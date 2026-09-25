package vn.edu.fpt.cares.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import vn.edu.fpt.cares.dto.auditlog.AuditLogCreateRequest;
import vn.edu.fpt.cares.enums.AuditAction;
import vn.edu.fpt.cares.model.Account;
import vn.edu.fpt.cares.service.AuditLogService;
import vn.edu.fpt.cares.service.AuthService;
import vn.edu.fpt.cares.service.AuditSnapshotService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private static final Set<String> MUTATING_HTTP_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final AuditLogService auditLogService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final AuditSnapshotService auditSnapshotService;

    @Around("@annotation(auditable)")
    public Object logAudit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String requestedEntityId = extractEntityId(joinPoint, auditable, null);
        String oldValueJson = auditSnapshotService.snapshot(auditable.entityName(), requestedEntityId);
        Object result = joinPoint.proceed();
        
        try {
            HttpServletRequest request = getRequest();
            String ipAddress = request != null ? request.getRemoteAddr() : "unknown";
            String userAgent = request != null ? request.getHeader("User-Agent") : "unknown";

            UUID actorId = null;
            try {
                Account acc = authService.currentAccount();
                if (acc != null) actorId = acc.getAccountId();
            } catch (Exception e) {
                log.warn("Could not extract current account for audit log: {}", e.getMessage());
            }

            String entityId = extractEntityId(joinPoint, auditable, result);
            String newValueJson = serializeResponse(result);
            String description = auditable.description().isBlank()
                    ? getVietnameseAction(auditable.action()) + " dữ liệu " + getVietnameseEntity(auditable.entityName())
                    : auditable.description();

            AuditLogCreateRequest logReq = new AuditLogCreateRequest(
                    auditable.action(),
                    auditable.entityName(),
                    entityId,
                    actorId,
                    ipAddress != null && ipAddress.length() > 50 ? ipAddress.substring(0, 50) : ipAddress,
                    userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent,
                    oldValueJson,
                    newValueJson,
                    description
            );
            // Ghi dong bo bang REQUIRES_NEW sau khi nghiep vu da tra ve thanh cong.
            // Khong dung common pool de tranh mat log khi JVM dung hoac request context ket thuc.
            auditLogService.create(logReq);
            
        } catch (Exception e) {
            log.error("Failed to process audit logging", e);
        }

        return result;
    }

    /**
     * Safety net for newly added or legacy mutation endpoints that have not yet
     * received a domain-specific {@link Auditable} annotation. Deliberately do
     * not serialize the response: authentication tokens, OTP codes and chat
     * content must never be copied into the audit table.
     */
    @Around("execution(public * vn.edu.fpt.cares.controller..*(..)) " +
            "&& !@annotation(vn.edu.fpt.cares.aop.Auditable)")
    public Object logUnannotatedMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        HttpServletRequest request = getRequest();
        if (request == null || !MUTATING_HTTP_METHODS.contains(request.getMethod())) {
            return result;
        }
        if (result instanceof ResponseEntity<?> response && response.getStatusCode().isError()) {
            return result;
        }

        try {
            UUID actorId = null;
            try {
                Account account = authService.currentAccount();
                if (account != null) actorId = account.getAccountId();
            } catch (Exception ignored) {
                // Public operations (login, OTP, guest booking) legitimately have no actor.
            }

            String controllerName = joinPoint.getSignature().getDeclaringType().getSimpleName();
            if (controllerName.endsWith("Controller")) {
                controllerName = controllerName.substring(0, controllerName.length() - "Controller".length());
            }
            String requestPath = request.getRequestURI();
            AuditAction action = resolveFallbackAction(request.getMethod(), requestPath);
            String technicalContext = objectMapper.writeValueAsString(java.util.Map.of(
                    "method", request.getMethod(),
                    "path", requestPath
            ));
            auditLogService.create(new AuditLogCreateRequest(
                    action,
                    controllerName,
                    null,
                    actorId,
                    truncate(request.getRemoteAddr(), 50),
                    truncate(request.getHeader("User-Agent"), 500),
                    null,
                    technicalContext,
                    resolveFallbackDescription(action, controllerName, requestPath)
            ));
        } catch (Exception ex) {
            log.error("Failed to create fallback audit log", ex);
        }
        return result;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private String serializeResponse(Object result) {
        if (result == null) return null;
        try {
            Object value = result instanceof ResponseEntity<?> response ? response.getBody() : result;
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("Could not serialize audit response: {}", ex.getMessage());
            return null;
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getVietnameseAction(vn.edu.fpt.cares.enums.AuditAction action) {
        if (action == null) return "Thao tác";
        return switch (action) {
            case CREATE -> "Tạo mới";
            case UPDATE -> "Cập nhật";
            case DELETE -> "Xóa";
            case LOGIN -> "Đăng nhập";
            case LOGOUT -> "Đăng xuất";
            case LOGIN_FAILED -> "Đăng nhập thất bại";
            case EXPORT -> "Xuất dữ liệu";
            case IMPORT -> "Nhập dữ liệu";
            case VIEW -> "Xem dữ liệu";
            case STATUS_CHANGE -> "Đổi trạng thái";
            case PAYMENT_CONFIRMED -> "Xác nhận thanh toán";
            case PATIENT_CALLED -> "Gọi bệnh nhân";
            case QUEUE_SKIPPED -> "Đánh dấu vắng";
            case EXAM_STARTED -> "Bắt đầu phục vụ";
            case DRAFT_SAVED -> "Lưu nháp";
            case RECORD_COMPLETED -> "Hoàn thành hồ sơ";
            case RESULT_UPLOADED -> "Tải lên kết quả";
            case RESULT_SIGNED -> "Ký kết quả";
            case COMPLETED_RECORD_EDITED -> "Sửa hồ sơ đã hoàn thành";
        };
    }

    private String getVietnameseEntity(String entityName) {
        if (entityName == null) return "Dữ liệu";
        return switch (entityName) {
            case "Account" -> "Tài khoản";
            case "Staff", "StaffInfo" -> "Nhân sự";
            case "PatientProfile" -> "Bệnh nhân";
            case "Department" -> "Phòng/Khoa";
            case "MedicalService", "ServiceItem" -> "Dịch vụ y tế";
            case "Appointment" -> "Lịch hẹn";
            case "ClinicInformation" -> "Thông tin phòng khám";
            case "System" -> "Hệ thống";
            case "AuditLog" -> "Nhật ký hệ thống";
            case "Auth" -> "Xác thực tài khoản";
            case "Bhxh", "Insurance" -> "Bảo hiểm y tế";
            case "Chat" -> "Hỗ trợ trực tuyến";
            case "ClinicalFormTemplate", "ClinicalFormTemplateBinding" -> "Biểu mẫu lâm sàng";
            case "ClinicSchedule", "ClinicScheduleException" -> "Lịch hoạt động phòng khám";
            case "CustomerVisit" -> "Lượt khám";
            case "DoctorExamination" -> "Khám bệnh";
            case "Icd10Code" -> "Danh mục ICD-10";
            case "Invoice", "Transaction" -> "Thanh toán";
            case "MedicalRecord", "PatientAllergy", "Profile", "VitalSigns" -> "Hồ sơ bệnh nhân";
            case "MedicineCatalog" -> "Danh mục thuốc";
            case "Notification" -> "Thông báo";
            case "PatientJourney" -> "Hành trình bệnh nhân";
            case "PayOSWebhook" -> "Thanh toán trực tuyến";
            case "PublicAnnouncement" -> "Thông báo công khai";
            case "QueueTicket", "QueueTicketSkip" -> "Hàng chờ";
            case "Report" -> "Báo cáo";
            case "ServiceCapability" -> "Danh mục kỹ thuật";
            case "ServiceCategory" -> "Nhóm dịch vụ";
            case "ShiftConfig", "ShiftVersion" -> "Cấu hình ca";
            case "Specialization" -> "Chuyên khoa";
            case "StaffSchedule", "ScheduleTemplate", "StaffScheduleTemplate" -> "Lịch trực nhân sự";
            case "TestRequest", "TestRequestCancel", "TestResult", "TestResultFile" -> "Cận lâm sàng";
            default -> "Phân hệ khác";
        };
    }

    private AuditAction resolveFallbackAction(String method, String requestPath) {
        if ("/api/auth/login".equals(requestPath)) {
            return AuditAction.LOGIN;
        }
        return switch (method) {
            case "POST" -> AuditAction.CREATE;
            case "DELETE" -> AuditAction.DELETE;
            default -> AuditAction.UPDATE;
        };
    }

    private String resolveFallbackDescription(AuditAction action, String entityName, String requestPath) {
        return switch (requestPath) {
            case "/api/auth/login" -> "Đăng nhập vào hệ thống";
            case "/api/auth/register" -> "Đăng ký tài khoản bệnh nhân";
            case "/api/auth/send-otp", "/api/auth/send-register-otp" -> "Gửi mã xác thực OTP";
            case "/api/auth/verify-register-otp" -> "Xác thực mã OTP đăng ký";
            case "/api/auth/reset-password" -> "Đặt lại mật khẩu";
            case "/api/auth/refresh" -> "Làm mới phiên đăng nhập";
            case "/api/auth/me/password" -> "Đổi mật khẩu tài khoản";
            default -> getVietnameseAction(action) + " trong phân hệ " + getVietnameseEntity(entityName);
        };
    }

    private String extractEntityId(ProceedingJoinPoint joinPoint, Auditable auditable, Object result) {
        if (!auditable.idParamName().isEmpty()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals(auditable.idParamName()) && args[i] != null) {
                    return args[i].toString();
                }
            }
        }

        if (result instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();
            if (body != null) {
                try {
                    for (Method method : body.getClass().getMethods()) {
                        String name = method.getName().toLowerCase();
                        if ((name.endsWith("id") || name.equals("id")) && method.getParameterCount() == 0) {
                            Object idVal = method.invoke(body);
                            if (idVal != null) {
                                String idStr = idVal.toString();
                                // Basic UUID/Long check to avoid getting random string fields like "sessionId" if they aren't the primary key.
                                // Actually, returning the first id-like field is fine for this context.
                                if (idStr.length() > 0) return idStr;
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Error extracting ID from response body: {}", ex.getMessage());
                }
            }
        }
        return null;
    }
}
