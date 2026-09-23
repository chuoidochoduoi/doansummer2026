package org.example.doansummer2026.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.doansummer2026.dto.auditlog.AuditLogCreateRequest;
import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.service.AuditLogService;
import org.example.doansummer2026.service.AuditSnapshotService;
import org.example.doansummer2026.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock AuditLogService auditLogService;
    @Mock AuthService authService;
    @Mock AuditSnapshotService snapshotService;
    @Mock ProceedingJoinPoint joinPoint;
    @Mock MethodSignature signature;

    private AuditLogAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new AuditLogAspect(auditLogService, authService, new ObjectMapper(), snapshotService);
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void annotatedMutationSnapshotsProceedsAndWritesTruncatedAudit() throws Throwable {
        UUID entityId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Body body = new Body(entityId, "Kết quả");
        Auditable auditable = auditable(AuditAction.UPDATE, "MedicalRecord", "id", "");
        when(signature.getParameterNames()).thenReturn(new String[]{"id", "request"});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{entityId, "payload"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(body));
        when(snapshotService.snapshot("MedicalRecord", entityId.toString())).thenReturn("{\"old\":1}");
        when(authService.currentAccount()).thenReturn(Account.builder().accountId(actorId).build());
        request("PATCH", "/api/records/" + entityId, "1".repeat(55), "A".repeat(510));

        Object result = aspect.logAudit(joinPoint, auditable);

        assertInstanceOf(ResponseEntity.class, result);
        ArgumentCaptor<AuditLogCreateRequest> captor = ArgumentCaptor.forClass(AuditLogCreateRequest.class);
        verify(auditLogService).create(captor.capture());
        AuditLogCreateRequest logged = captor.getValue();
        assertAll(
                () -> assertEquals(AuditAction.UPDATE, logged.action()),
                () -> assertEquals("MedicalRecord", logged.entityName()),
                () -> assertEquals(entityId.toString(), logged.entityId()),
                () -> assertEquals(actorId, logged.actorAccountId()),
                () -> assertEquals(50, logged.ipAddress().length()),
                () -> assertEquals(500, logged.userAgent().length()),
                () -> assertEquals("{\"old\":1}", logged.oldValueJson()),
                () -> assertTrue(logged.newValueJson().contains("Kết quả")),
                () -> assertEquals("Cập nhật dữ liệu Hồ sơ bệnh nhân", logged.description()));
        verify(joinPoint).proceed();
    }

    @Test
    void annotatedMutationExtractsIdFromResponseAndUsesExplicitDescription() throws Throwable {
        UUID id = UUID.randomUUID();
        Auditable auditable = auditable(AuditAction.CREATE, "Appointment", "", "Tạo lịch thành công");
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(new Body(id, "Lịch")));
        when(authService.currentAccount()).thenReturn(null);

        aspect.logAudit(joinPoint, auditable);

        ArgumentCaptor<AuditLogCreateRequest> captor = ArgumentCaptor.forClass(AuditLogCreateRequest.class);
        verify(auditLogService).create(captor.capture());
        assertEquals(id.toString(), captor.getValue().entityId());
        assertNull(captor.getValue().actorAccountId());
        assertEquals("unknown", captor.getValue().ipAddress());
        assertEquals("Tạo lịch thành công", captor.getValue().description());
    }

    @Test
    void annotatedMutationStillReturnsWhenAuthenticationOrAuditWritingFails() throws Throwable {
        Auditable auditable = auditable(AuditAction.DELETE, "Department", "", "");
        when(joinPoint.proceed()).thenReturn("đã xóa");
        when(authService.currentAccount()).thenThrow(new IllegalStateException("public"));
        doThrow(new IllegalStateException("audit unavailable")).when(auditLogService).create(any());

        assertEquals("đã xóa", aspect.logAudit(joinPoint, auditable));
        verify(auditLogService).create(any());
    }

    @Test
    void annotatedMutationDoesNotAuditFailedBusinessCall() throws Throwable {
        Auditable auditable = auditable(AuditAction.CREATE, "Invoice", "", "");
        when(snapshotService.snapshot("Invoice", null)).thenReturn(null);
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("nghiệp vụ lỗi"));

        assertThrows(IllegalArgumentException.class, () -> aspect.logAudit(joinPoint, auditable));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void unannotatedIgnoresNoRequestGetAndErrorResponse() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");
        assertEquals("ok", aspect.logUnannotatedMutation(joinPoint));

        request("GET", "/api/items", null, null);
        assertEquals("ok", aspect.logUnannotatedMutation(joinPoint));

        request("POST", "/api/items", null, null);
        when(joinPoint.proceed()).thenReturn(ResponseEntity.badRequest().body("lỗi"));
        assertEquals(400, ((ResponseEntity<?>) aspect.logUnannotatedMutation(joinPoint)).getStatusCode().value());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void unannotatedMutationWritesOnlyTechnicalContextAndStripsControllerSuffix() throws Throwable {
        UUID actorId = UUID.randomUUID();
        request("POST", "/api/items", "127.0.0.1", "JUnit");
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok("secret response"));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn((Class) DummyController.class);
        when(authService.currentAccount()).thenReturn(Account.builder().accountId(actorId).build());

        aspect.logUnannotatedMutation(joinPoint);

        ArgumentCaptor<AuditLogCreateRequest> captor = ArgumentCaptor.forClass(AuditLogCreateRequest.class);
        verify(auditLogService).create(captor.capture());
        AuditLogCreateRequest logged = captor.getValue();
        assertAll(
                () -> assertEquals(AuditAction.CREATE, logged.action()),
                () -> assertEquals("Dummy", logged.entityName()),
                () -> assertEquals(actorId, logged.actorAccountId()),
                () -> assertNull(logged.oldValueJson()),
                () -> assertTrue(logged.newValueJson().contains("POST")),
                () -> assertTrue(logged.newValueJson().contains("/api/items")),
                () -> assertFalse(logged.newValueJson().contains("secret response")));
    }

    @Test
    void unannotatedPublicDeleteSurvivesMissingActorAndAuditFailure() throws Throwable {
        request("DELETE", "/api/public/item/1", null, null);
        when(joinPoint.proceed()).thenReturn("deleted");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn((Class) PlainHandler.class);
        when(authService.currentAccount()).thenThrow(new IllegalStateException("anonymous"));
        doThrow(new IllegalStateException("log unavailable")).when(auditLogService).create(any());

        assertEquals("deleted", aspect.logUnannotatedMutation(joinPoint));
    }

    @Test
    void fallbackMappingsCoverEveryActionEntityAndAuthenticationDescription() {
        assertEquals("Thao tác", invoke("getVietnameseAction", (Object) null));
        for (AuditAction action : AuditAction.values()) {
            String label = invoke("getVietnameseAction", action);
            assertNotNull(label, action.name());
            assertFalse(label.isBlank(), action.name());
        }

        Map<String, String> entities = new LinkedHashMap<>();
        entities.put(null, "Dữ liệu");
        entities.put("Account", "Tài khoản");
        entities.put("Staff", "Nhân sự");
        entities.put("StaffInfo", "Nhân sự");
        entities.put("PatientProfile", "Bệnh nhân");
        entities.put("Department", "Phòng/Khoa");
        entities.put("MedicalService", "Dịch vụ y tế");
        entities.put("ServiceItem", "Dịch vụ y tế");
        entities.put("Appointment", "Lịch hẹn");
        entities.put("ClinicInformation", "Thông tin phòng khám");
        entities.put("System", "Hệ thống");
        entities.put("AuditLog", "Nhật ký hệ thống");
        entities.put("Auth", "Xác thực tài khoản");
        entities.put("Bhxh", "Bảo hiểm y tế");
        entities.put("Insurance", "Bảo hiểm y tế");
        entities.put("Chat", "Hỗ trợ trực tuyến");
        entities.put("ClinicalFormTemplate", "Biểu mẫu lâm sàng");
        entities.put("ClinicalFormTemplateBinding", "Biểu mẫu lâm sàng");
        entities.put("ClinicSchedule", "Lịch hoạt động phòng khám");
        entities.put("ClinicScheduleException", "Lịch hoạt động phòng khám");
        entities.put("CustomerVisit", "Lượt khám");
        entities.put("DoctorExamination", "Khám bệnh");
        entities.put("Icd10Code", "Danh mục ICD-10");
        entities.put("Invoice", "Thanh toán");
        entities.put("Transaction", "Thanh toán");
        entities.put("MedicalRecord", "Hồ sơ bệnh nhân");
        entities.put("PatientAllergy", "Hồ sơ bệnh nhân");
        entities.put("Profile", "Hồ sơ bệnh nhân");
        entities.put("VitalSigns", "Hồ sơ bệnh nhân");
        entities.put("MedicineCatalog", "Danh mục thuốc");
        entities.put("Notification", "Thông báo");
        entities.put("PatientJourney", "Hành trình bệnh nhân");
        entities.put("PayOSWebhook", "Thanh toán trực tuyến");
        entities.put("PublicAnnouncement", "Thông báo công khai");
        entities.put("QueueTicket", "Hàng chờ");
        entities.put("QueueTicketSkip", "Hàng chờ");
        entities.put("Report", "Báo cáo");
        entities.put("ServiceCapability", "Danh mục kỹ thuật");
        entities.put("ServiceCategory", "Nhóm dịch vụ");
        entities.put("ShiftConfig", "Cấu hình ca");
        entities.put("ShiftVersion", "Cấu hình ca");
        entities.put("Specialization", "Chuyên khoa");
        entities.put("StaffSchedule", "Lịch trực nhân sự");
        entities.put("ScheduleTemplate", "Lịch trực nhân sự");
        entities.put("StaffScheduleTemplate", "Lịch trực nhân sự");
        entities.put("TestRequest", "Cận lâm sàng");
        entities.put("TestRequestCancel", "Cận lâm sàng");
        entities.put("TestResult", "Cận lâm sàng");
        entities.put("TestResultFile", "Cận lâm sàng");
        entities.put("UnknownModule", "Phân hệ khác");
        entities.forEach((name, expected) -> assertEquals(expected,
                invoke("getVietnameseEntity", name), String.valueOf(name)));

        assertEquals(AuditAction.LOGIN, invoke("resolveFallbackAction", "POST", "/api/auth/login"));
        assertEquals(AuditAction.CREATE, invoke("resolveFallbackAction", "POST", "/api/items"));
        assertEquals(AuditAction.DELETE, invoke("resolveFallbackAction", "DELETE", "/api/items/1"));
        assertEquals(AuditAction.UPDATE, invoke("resolveFallbackAction", "PATCH", "/api/items/1"));

        Map<String, String> descriptions = Map.of(
                "/api/auth/login", "Đăng nhập vào hệ thống",
                "/api/auth/register", "Đăng ký tài khoản bệnh nhân",
                "/api/auth/send-otp", "Gửi mã xác thực OTP",
                "/api/auth/send-register-otp", "Gửi mã xác thực OTP",
                "/api/auth/verify-register-otp", "Xác thực mã OTP đăng ký",
                "/api/auth/reset-password", "Đặt lại mật khẩu",
                "/api/auth/refresh", "Làm mới phiên đăng nhập",
                "/api/auth/me/password", "Đổi mật khẩu tài khoản");
        descriptions.forEach((path, expected) -> assertEquals(expected,
                invoke("resolveFallbackDescription", AuditAction.UPDATE, "Account", path), path));
        assertEquals("Cập nhật trong phân hệ Tài khoản",
                invoke("resolveFallbackDescription", AuditAction.UPDATE, "Account", "/api/accounts/1"));
    }

    @Test
    void serializationFailureReturnsNullWithoutBreakingAudit() throws Throwable {
        ObjectMapper mapper = mock(ObjectMapper.class);
        when(mapper.writeValueAsString(any())).thenThrow(new IllegalStateException("json error"));
        AuditLogAspect failingAspect = new AuditLogAspect(auditLogService, authService, mapper, snapshotService);
        Auditable auditable = auditable(AuditAction.CREATE, "System", "", "");
        when(joinPoint.proceed()).thenReturn(new Body(UUID.randomUUID(), "value"));

        assertNotNull(failingAspect.logAudit(joinPoint, auditable));
        ArgumentCaptor<AuditLogCreateRequest> captor = ArgumentCaptor.forClass(AuditLogCreateRequest.class);
        verify(auditLogService).create(captor.capture());
        assertNull(captor.getValue().newValueJson());
    }

    private Auditable auditable(AuditAction action, String entity, String idParam, String description) {
        Auditable value = mock(Auditable.class);
        lenient().when(value.action()).thenReturn(action);
        lenient().when(value.entityName()).thenReturn(entity);
        lenient().when(value.idParamName()).thenReturn(idParam);
        lenient().when(value.description()).thenReturn(description);
        return value;
    }

    private void request(String method, String uri, String remoteAddress, String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        if (remoteAddress != null) request.setRemoteAddr(remoteAddress);
        if (userAgent != null) request.addHeader("User-Agent", userAgent);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(String method, Object... args) {
        return (T) ReflectionTestUtils.invokeMethod(aspect, method, args);
    }

    private record Body(UUID itemId, String name) {}
    private static class DummyController {}
    private static class PlainHandler {}
}
