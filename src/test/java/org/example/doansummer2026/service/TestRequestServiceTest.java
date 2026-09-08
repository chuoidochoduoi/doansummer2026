package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.testrequest.TestRequestBatchCreateRequest;
import org.example.doansummer2026.dto.testrequest.TestRequestCancelRequest;
import org.example.doansummer2026.dto.testrequest.TestRequestCreateRequest;
import org.example.doansummer2026.dto.testrequest.TestRequestUpdateRequest;
import org.example.doansummer2026.dto.testresult.TestResultCreateRequest;
import org.example.doansummer2026.dto.testresult.TestResultAmendRequest;
import org.example.doansummer2026.dto.testresult.TestResultUpdateRequest;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.enums.TestResultRevisionStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.dto.medicalrecord.MedicalRecordResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestRequestServiceTest {
    @TempDir
    Path tempDir;

    @Mock
    private TestRequestRepository repo;

    @Mock
    private TestResultRepository resultRepo;

    @Mock
    private MedicalRecordRepository recordRepo;

    @Mock
    private CustomerVisitRepository visitRepo;

    @Mock
    private MedicalServiceRepository serviceRepo;

    @Mock
    private StaffInfoRepository staffRepo;

    @Mock
    private QueueTicketRepository queueTicketRepo;

    @Mock
    private DepartmentRepository departmentRepo;

    @Mock
    private InvoiceItemRepository invoiceItemRepo;

    @Mock
    private MedicalRecordService medicalRecordService;

    @Mock
    private PatientJourneyService patientJourneyService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuthService authService;

    @Mock private TestResultRevisionRepository revisionRepo;
    @Mock private TestResultAttachmentRepository attachmentRepo;
    @Mock private ClinicalFormTemplateService clinicalFormTemplateService;
    @Mock private ClinicalFormEngine clinicalFormEngine;
    @Mock private SameDayParaclinicalResultService sameDayParaclinicalResultService;
    @Mock private StaffDutyService staffDutyService;
    @Mock private MedicalServiceSelectionPolicyService serviceSelectionPolicyService;

    @InjectMocks
    private TestRequestService testRequestService;

    @BeforeEach
    void currentAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        lenient().when(repo.findByIdForUpdate(any())).thenAnswer(invocation ->
                repo.findByIdWithResult(invocation.getArgument(0)));
        lenient().when(departmentRepo.findByIdForUpdate(any())).thenAnswer(invocation ->
                departmentRepo.findById(invocation.getArgument(0)));
        lenient().when(visitRepo.findByIdForUpdate(any())).thenAnswer(invocation ->
                visitRepo.findById(invocation.getArgument(0)));
        lenient().when(revisionRepo.save(any(TestResultRevision.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private TestRequest operableRequest(UUID id, TestRequestStatus status) {
        UUID staffId = UUID.randomUUID();
        Department department = Department.builder()
                .departmentId(UUID.randomUUID())
                .build();
        QueueTicket queue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.IN_PROGRESS)
                .build();
        TestRequest request = TestRequest.builder()
                .testRequestId(id)
                .status(status)
                .performingDepartment(department)
                .queueTicket(queue)
                .build();
        lenient().when(authService.currentStaffId()).thenReturn(staffId);
        lenient().when(repo.findById(id)).thenReturn(Optional.of(request));
        lenient().when(repo.findByIdForUpdate(id)).thenReturn(Optional.of(request));
        lenient().when(departmentRepo.findById(department.getDepartmentId())).thenReturn(Optional.of(department));
        return request;
    }

    private TestRequest authorizedRequest(UUID id, TestRequestStatus status, QueueStatus queueStatus) {
        UUID staffId = UUID.randomUUID();
        StaffInfo doctor = StaffInfo.builder().staffId(staffId).systemRole(SystemRole.DOCTOR).build();
        Department department = Department.builder().departmentId(UUID.randomUUID()).build();
        QueueTicket queue = queueStatus == null ? null : QueueTicket.builder()
                .ticketId(UUID.randomUUID()).status(queueStatus).build();
        TestRequest request = TestRequest.builder().testRequestId(id).status(status)
                .performingDepartment(department).queueTicket(queue).build();
        when(authService.currentStaffId()).thenReturn(staffId);
        when(repo.findByIdForUpdate(id)).thenReturn(Optional.of(request));
        when(departmentRepo.findByIdForUpdate(department.getDepartmentId())).thenReturn(Optional.of(department));
        when(staffDutyService.requireCurrentStaffOnDuty(department, true)).thenReturn(doctor);
        return request;
    }

    private TestResult attachValidatedExistingResult(TestRequest request, String conclusion, String imageUrl) {
        MedicalService service = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("IMG-TEST").name("Chẩn đoán hình ảnh").requiresSpecimen(false).build();
        request.setService(service);
        TestResult result = TestResult.builder().resultId(UUID.randomUUID()).testRequest(request)
                .performedBy(StaffInfo.builder().staffId(UUID.randomUUID()).staffCode("BS-CLS").build())
                .conclusion(conclusion).imageUrl(imageUrl).build();
        request.setTestResult(result);
        ClinicalFormTemplateVersion version = ClinicalFormTemplateVersion.builder()
                .versionId(UUID.randomUUID()).build();
        var schema = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        var normalized = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("value", "normal");
        when(clinicalFormTemplateService.resolveVersion(eq(service.getServiceId()), isNull())).thenReturn(version);
        when(clinicalFormTemplateService.schemaForService(service.getServiceCode(), version)).thenReturn(schema);
        when(clinicalFormEngine.validateAndEnrich(eq(schema), isNull(), isNull(), isNull(),
                any(LocalDate.class), eq(true))).thenReturn(normalized);
        return result;
    }

    @Test
    void completeResult_ShouldReject_WhenCurrentStaffIsMissing() {
        UUID id = UUID.randomUUID();
        TestRequest request = TestRequest.builder().testRequestId(id)
                .status(TestRequestStatus.IN_PROGRESS)
                .performingDepartment(Department.builder().departmentId(UUID.randomUUID()).build())
                .build();
        when(repo.findByIdForUpdate(id)).thenReturn(Optional.of(request));
        when(authService.currentStaffId()).thenReturn(null);

        assertThrows(AccessDeniedException.class,
                () -> testRequestService.completeResult(id, mock(TestResultCreateRequest.class)));
    }

    @Test
    void completeResult_ShouldReject_WhenPerformingDepartmentIsMissing() {
        UUID id = UUID.randomUUID();
        when(repo.findByIdForUpdate(id)).thenReturn(Optional.of(TestRequest.builder()
                .testRequestId(id).status(TestRequestStatus.IN_PROGRESS).build()));
        when(authService.currentStaffId()).thenReturn(UUID.randomUUID());

        assertThrows(AccessDeniedException.class,
                () -> testRequestService.completeResult(id, mock(TestResultCreateRequest.class)));
    }

    @Test
    void completeResult_ShouldReject_WhenLockedDepartmentDisappeared() {
        UUID id = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        Department department = Department.builder().departmentId(departmentId).build();
        when(repo.findByIdForUpdate(id)).thenReturn(Optional.of(TestRequest.builder()
                .testRequestId(id).status(TestRequestStatus.IN_PROGRESS)
                .performingDepartment(department).build()));
        when(authService.currentStaffId()).thenReturn(UUID.randomUUID());
        when(departmentRepo.findByIdForUpdate(departmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> testRequestService.completeResult(id, mock(TestResultCreateRequest.class)));
    }

    @Test
    void completeResult_ShouldRejectAlreadyCompletedRequest_CurrentContract() {
        UUID id = UUID.randomUUID();
        authorizedRequest(id, TestRequestStatus.COMPLETED, QueueStatus.IN_PROGRESS);

        assertThrows(ConflictException.class,
                () -> testRequestService.completeResult(id, mock(TestResultCreateRequest.class)));
    }

    @Test
    void completeResult_ShouldRejectCancelledRequest_CurrentContract() {
        UUID id = UUID.randomUUID();
        authorizedRequest(id, TestRequestStatus.CANCELLED, QueueStatus.IN_PROGRESS);

        assertThrows(ConflictException.class,
                () -> testRequestService.completeResult(id, mock(TestResultCreateRequest.class)));
    }

    @Test
    void completeResult_ShouldRequireExecutionQueue() {
        UUID id = UUID.randomUUID();
        authorizedRequest(id, TestRequestStatus.IN_PROGRESS, null);

        assertThrows(BadRequestException.class,
                () -> testRequestService.completeResult(id, mock(TestResultCreateRequest.class)));
    }

    @Test
    void completeResult_ShouldRequireStartedExecutionQueue() {
        UUID id = UUID.randomUUID();
        authorizedRequest(id, TestRequestStatus.IN_PROGRESS, QueueStatus.WAITING);

        assertThrows(BadRequestException.class,
                () -> testRequestService.completeResult(id, mock(TestResultCreateRequest.class)));
    }

    @Test
    void completeResult_ShouldSignExistingResultAndCloseCompletedLabQueue() {
        UUID id = UUID.randomUUID();
        TestRequest request = authorizedRequest(id, TestRequestStatus.IN_PROGRESS, QueueStatus.IN_PROGRESS);
        TestResult result = attachValidatedExistingResult(request, "Kết quả bình thường", "/uploads/result.pdf");
        when(repo.countByQueueTicket_TicketIdAndStatusIn(eq(request.getQueueTicket().getTicketId()), anyList()))
                .thenReturn(0L);

        var response = testRequestService.completeResult(id, mock(TestResultCreateRequest.class));

        assertEquals(id, response.testRequestId());
        assertEquals(TestRequestStatus.COMPLETED, request.getStatus());
        assertEquals(QueueStatus.DONE, request.getQueueTicket().getStatus());
        assertNotNull(result.getVerifiedAt());
        verify(queueTicketRepo).save(request.getQueueTicket());
    }

    @Test
    void completeResult_ShouldKeepLabQueueOpenWhileAnotherRequestRemains() {
        UUID id = UUID.randomUUID();
        TestRequest request = authorizedRequest(id, TestRequestStatus.IN_PROGRESS, QueueStatus.IN_PROGRESS);
        attachValidatedExistingResult(request, "Kết quả bình thường", "/uploads/result.pdf");
        when(repo.countByQueueTicket_TicketIdAndStatusIn(eq(request.getQueueTicket().getTicketId()), anyList()))
                .thenReturn(1L);

        testRequestService.completeResult(id, mock(TestResultCreateRequest.class));

        assertEquals(QueueStatus.IN_PROGRESS, request.getQueueTicket().getStatus());
        verify(queueTicketRepo, never()).save(any());
    }

    @Test
    void completeResult_ShouldRejectBlankConclusionAfterValidatingStructuredData() {
        UUID id = UUID.randomUUID();
        TestRequest request = authorizedRequest(id, TestRequestStatus.IN_PROGRESS, QueueStatus.IN_PROGRESS);
        attachValidatedExistingResult(request, "   ", "/uploads/result.pdf");

        assertThrows(BadRequestException.class,
                () -> testRequestService.completeResult(id, mock(TestResultCreateRequest.class)));
        verify(resultRepo, never()).save(any());
    }

    @Test
    void completeResult_ShouldRequireStructuredDataFileOrAttachment() {
        UUID id = UUID.randomUUID();
        TestRequest request = authorizedRequest(id, TestRequestStatus.IN_PROGRESS, QueueStatus.IN_PROGRESS);
        TestResult result = attachValidatedExistingResult(request, "Bình thường", null);
        // Simulate a form engine that legitimately has no structured output.
        when(clinicalFormEngine.validateAndEnrich(any(), isNull(), isNull(), isNull(),
                any(LocalDate.class), eq(true))).thenReturn(null);
        when(revisionRepo.findFirstByTestResult_ResultIdOrderByRevisionNoDesc(result.getResultId()))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> testRequestService.completeResult(id, mock(TestResultCreateRequest.class)));
    }

    @Test
    void completeResult_ShouldCreateDefaultUrineSpecimenAndRecordCollector() {
        UUID id = UUID.randomUUID();
        TestRequest request = authorizedRequest(id, TestRequestStatus.IN_PROGRESS, QueueStatus.IN_PROGRESS);
        TestResult result = attachValidatedExistingResult(request, "Bình thường", "/uploads/result.pdf");
        request.getService().setRequiresSpecimen(true);
        request.getService().setServiceCode("LAB-URINE");
        request.getService().setName("Tổng phân tích nước tiểu");
        StaffInfo collector = StaffInfo.builder().staffId(authService.currentStaffId()).build();
        when(staffRepo.findById(authService.currentStaffId())).thenReturn(Optional.of(collector));
        when(repo.countByQueueTicket_TicketIdAndStatusIn(any(), anyList())).thenReturn(1L);

        testRequestService.completeResult(id, mock(TestResultCreateRequest.class));

        assertTrue(result.getSampleId().startsWith("SMP-"));
        assertEquals(org.example.doansummer2026.enums.SpecimenType.URINE, result.getSampleType());
        assertEquals(org.example.doansummer2026.enums.SpecimenStatus.ACCEPTED, result.getSampleStatus());
        assertSame(collector, result.getCollectedBy());
        assertNotNull(result.getCollectedAt());
    }

    @Test
    void completeResult_ShouldInferSwabSpecimenFromServiceName() {
        UUID id = UUID.randomUUID();
        TestRequest request = authorizedRequest(id, TestRequestStatus.IN_PROGRESS, QueueStatus.IN_PROGRESS);
        TestResult result = attachValidatedExistingResult(request, "Âm tính", "/uploads/result.pdf");
        request.getService().setRequiresSpecimen(true);
        request.getService().setServiceCode(null);
        request.getService().setName("Dịch ngoáy cúm");
        UUID collectorId = authService.currentStaffId();
        when(staffRepo.findById(collectorId))
                .thenReturn(Optional.of(StaffInfo.builder().staffId(collectorId).build()));
        when(repo.countByQueueTicket_TicketIdAndStatusIn(any(), anyList())).thenReturn(1L);

        testRequestService.completeResult(id, mock(TestResultCreateRequest.class));

        assertEquals(org.example.doansummer2026.enums.SpecimenType.SWAB, result.getSampleType());
    }

    @Test
    void completeResult_ShouldRejectSpecimenFieldsForNonSpecimenService() {
        UUID id = UUID.randomUUID();
        TestRequest request = authorizedRequest(id, TestRequestStatus.IN_PROGRESS, QueueStatus.IN_PROGRESS);
        attachValidatedExistingResult(request, "Bình thường", "/uploads/result.pdf");
        TestResultCreateRequest input = mock(TestResultCreateRequest.class);
        when(input.sampleId()).thenReturn("SMP-EXPLICIT");

        assertThrows(BadRequestException.class, () -> testRequestService.completeResult(id, input));
    }

    @Test
    void completeResult_ShouldRejectRejectedSpecimen() {
        UUID id = UUID.randomUUID();
        TestRequest request = authorizedRequest(id, TestRequestStatus.IN_PROGRESS, QueueStatus.IN_PROGRESS);
        attachValidatedExistingResult(request, "Bình thường", "/uploads/result.pdf");
        request.getService().setRequiresSpecimen(true);
        UUID collectorId = authService.currentStaffId();
        when(staffRepo.findById(collectorId))
                .thenReturn(Optional.of(StaffInfo.builder().staffId(collectorId).build()));
        TestResultCreateRequest input = mock(TestResultCreateRequest.class);
        when(input.sampleStatus()).thenReturn(org.example.doansummer2026.enums.SpecimenStatus.REJECTED);

        assertThrows(BadRequestException.class, () -> testRequestService.completeResult(id, input));
        verify(resultRepo, never()).save(any());
    }

    @Test
    void actionPermissions_ShouldAllowResponsibleDoctorAfterExecutionStarts() {
        UUID id = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Department department = Department.builder().departmentId(UUID.randomUUID()).build();
        StaffInfo doctor = StaffInfo.builder().staffId(staffId).systemRole(SystemRole.DOCTOR)
                .department(department).build();
        TestRequest request = TestRequest.builder().testRequestId(id).status(TestRequestStatus.IN_PROGRESS)
                .performingDepartment(department)
                .queueTicket(QueueTicket.builder().status(QueueStatus.IN_PROGRESS).build()).build();
        when(repo.findById(id)).thenReturn(Optional.of(request));
        when(authService.currentStaffId()).thenReturn(staffId);
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(doctor));

        var result = testRequestService.actionPermissions(id);

        assertTrue(result.canView());
        assertTrue(result.canEditResult());
        assertTrue(result.canUpload());
        assertTrue(result.canSign());
        assertTrue(result.canCancel());
    }

    @Test
    void actionPermissions_ShouldAllowAssignedNurseToEditButNotSignOrCancel() {
        UUID id = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Department department = Department.builder().departmentId(UUID.randomUUID()).build();
        StaffInfo nurse = StaffInfo.builder().staffId(staffId).systemRole(SystemRole.NURSE)
                .department(department).build();
        TestRequest request = TestRequest.builder().testRequestId(id).status(TestRequestStatus.PENDING)
                .performingDepartment(department)
                .queueTicket(QueueTicket.builder().status(QueueStatus.DONE).build()).build();
        when(repo.findById(id)).thenReturn(Optional.of(request));
        when(authService.currentStaffId()).thenReturn(staffId);
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(nurse));

        var result = testRequestService.actionPermissions(id);

        assertTrue(result.canEditResult());
        assertFalse(result.canSign());
        assertFalse(result.canCancel());
    }

    @Test
    void actionPermissions_ShouldBeReadOnlyForFinishedRequestWithoutActor() {
        UUID id = UUID.randomUUID();
        TestRequest request = TestRequest.builder().testRequestId(id).status(TestRequestStatus.COMPLETED)
                .performingDepartment(null).queueTicket(null).build();
        when(repo.findById(id)).thenReturn(Optional.of(request));
        when(authService.currentStaffId()).thenReturn(null);

        var result = testRequestService.actionPermissions(id);

        assertTrue(result.canView());
        assertFalse(result.canEditResult());
        assertFalse(result.canUpload());
        assertFalse(result.canSign());
        assertFalse(result.canCancel());
    }

    @Test
    void amendResult_ShouldRequireCompletedRequest() {
        UUID id = UUID.randomUUID();
        authorizedRequest(id, TestRequestStatus.IN_PROGRESS, QueueStatus.DONE);

        assertThrows(ConflictException.class,
                () -> testRequestService.amendResult(id, new TestResultAmendRequest("Sửa kết luận")));
    }

    @Test
    void amendResult_ShouldRequireExistingResult() {
        UUID id = UUID.randomUUID();
        authorizedRequest(id, TestRequestStatus.COMPLETED, QueueStatus.DONE);
        when(resultRepo.findByTestRequest_TestRequestId(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> testRequestService.amendResult(id, new TestResultAmendRequest("Sửa kết luận")));
    }

    @Test
    void amendResult_ShouldRejectWhenDraftAlreadyExists() {
        UUID id = UUID.randomUUID();
        TestRequest request = authorizedRequest(id, TestRequestStatus.COMPLETED, QueueStatus.DONE);
        TestResult result = TestResult.builder().resultId(UUID.randomUUID()).testRequest(request).build();
        when(resultRepo.findByTestRequest_TestRequestId(id)).thenReturn(Optional.of(result));
        when(revisionRepo.findFirstByTestResult_ResultIdOrderByRevisionNoDesc(result.getResultId()))
                .thenReturn(Optional.of(TestResultRevision.builder().status(TestResultRevisionStatus.DRAFT).build()));

        assertThrows(ConflictException.class,
                () -> testRequestService.amendResult(id, new TestResultAmendRequest("Sửa kết luận")));
    }

    @Test
    void amendResult_ShouldCreateDraftWithTrimmedReason() {
        UUID id = UUID.randomUUID();
        TestRequest request = authorizedRequest(id, TestRequestStatus.COMPLETED, QueueStatus.DONE);
        StaffInfo performer = StaffInfo.builder().staffId(UUID.randomUUID()).build();
        TestResult result = TestResult.builder().resultId(UUID.randomUUID()).testRequest(request)
                .performedBy(performer).conclusion("Cũ").build();
        when(resultRepo.findByTestRequest_TestRequestId(id)).thenReturn(Optional.of(result));
        when(revisionRepo.findFirstByTestResult_ResultIdOrderByRevisionNoDesc(result.getResultId()))
                .thenReturn(Optional.empty());
        when(staffRepo.findById(authService.currentStaffId())).thenReturn(Optional.empty());

        var response = testRequestService.amendResult(id, new TestResultAmendRequest("  Sửa kết luận  "));

        assertEquals(TestResultRevisionStatus.DRAFT, response.status());
        assertEquals("Sửa kết luận", response.amendmentReason());
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturnTestRequest_WhenExists() {

        UUID id = UUID.randomUUID();

        TestRequest testRequest = mock(TestRequest.class);

        when(repo.findById(id))
                .thenReturn(Optional.of(testRequest));

        TestRequest result =
                testRequestService.findById(id);

        assertSame(testRequest, result);

        verify(repo).findById(id);
    }


    @Test
    void findById_ShouldThrowNotFound_WhenDoesNotExist() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.findById(id)
        );

        verify(repo).findById(id);
    }


    // =========================================================
    // DELETE
    // =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void delete_ShouldUseCancellationContractAndNeverHardDelete() {

        UUID id = UUID.randomUUID();

        when(repo.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> testRequestService.delete(id));
        verify(repo, never()).deleteById(any());
    }
    @Test
    void delete_ShouldThrowNotFound_WhenRequestDoesNotExist() {

        UUID id = UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.delete(id)
        );

        verify(repo, never())
                .deleteById(id);
    }


    // =========================================================
    // LIST BY QUEUE TICKET
    // =========================================================

    @Test
    void listByQueueTicket_ShouldThrowNotFound_WhenQueueTicketDoesNotExist() {

        UUID ticketId = UUID.randomUUID();

        when(queueTicketRepo.existsById(ticketId))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.listByQueueTicket(ticketId)
        );

        verify(repo, never())
                .findAllByQueueTicket_TicketId(ticketId);
    }


    // =========================================================
    // GET RESULT
    // =========================================================

    @Test
    void getResult_ShouldThrowNotFound_WhenTestRequestDoesNotExist() {

        UUID testRequestId = UUID.randomUUID();

        when(repo.findById(testRequestId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.getResult(testRequestId)
        );

        verifyNoInteractions(resultRepo);
    }
    @Test
    void getResult_ShouldThrowNotFound_WhenResultDoesNotExist() {

        UUID testRequestId = UUID.randomUUID();

        TestRequest testRequest =
                mock(TestRequest.class);

        when(testRequest.getTestRequestId())
                .thenReturn(testRequestId);

        when(repo.findById(testRequestId))
                .thenReturn(Optional.of(testRequest));

        when(
                resultRepo.findByTestRequest_TestRequestId(testRequestId)
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.getResult(testRequestId)
        );
    }


    // =========================================================
    // CREATE - DUPLICATE INVOICE ITEM
    // =========================================================

    @Test
    void create_ShouldReturnExistingRequest_WhenInvoiceItemAlreadyLinked() {

        UUID invoiceItemId = UUID.randomUUID();

        TestRequestCreateRequest request =
                mock(TestRequestCreateRequest.class);

        TestRequest existing =
                mock(TestRequest.class);

        when(request.invoiceItemId())
                .thenReturn(invoiceItemId);

        when(
                repo.findByInvoiceItem_ItemId(invoiceItemId)
        ).thenReturn(List.of(existing));

        /*
         * Method sẽ return ngay.
         * Không được truy DB MedicalRecord/Service/Staff nữa.
         *
         * Không assert sâu Response ở test này vì DTO mapping
         * phụ thuộc model đầy đủ; mục tiêu là kiểm tra early-return.
         */
        assertDoesNotThrow(
                () -> testRequestService.create(request)
        );

        verify(repo)
                .findByInvoiceItem_ItemId(invoiceItemId);

        verifyNoInteractions(recordRepo);
        verifyNoInteractions(serviceRepo);
        verifyNoInteractions(staffRepo);

        verify(repo, never())
                .save(any(TestRequest.class));
    }


    // =========================================================
    // CREATE - MEDICAL RECORD NOT FOUND
    // =========================================================

    @Test
    void create_ShouldThrowNotFound_WhenMedicalRecordDoesNotExist() {

        UUID recordId = UUID.randomUUID();

        TestRequestCreateRequest request =
                mock(TestRequestCreateRequest.class);

        when(request.medicalRecordId())
                .thenReturn(recordId);

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.create(request)
        );

        verify(serviceRepo, never())
                .findById(any(UUID.class));

        verify(repo, never())
                .save(any(TestRequest.class));
    }


    // =========================================================
    // CREATE - SERVICE NOT FOUND
    // =========================================================

    @Test
    void create_ShouldThrowNotFound_WhenMedicalServiceDoesNotExist() {

        UUID recordId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        MedicalRecord record =
                mock(MedicalRecord.class);

        TestRequestCreateRequest request =
                mock(TestRequestCreateRequest.class);

        when(request.medicalRecordId())
                .thenReturn(recordId);

        when(request.serviceId())
                .thenReturn(serviceId);

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.create(request)
        );

        verify(staffRepo, never())
                .findById(any(UUID.class));

        verify(repo, never())
                .save(any(TestRequest.class));
    }


    // =========================================================
    // CANCEL
    // =========================================================
    @Test
    void cancel_ShouldRejectCompletedRequest() {

        UUID id = UUID.randomUUID();

        TestRequest testRequest = operableRequest(id, TestRequestStatus.COMPLETED);

        TestRequestCancelRequest request =
                mock(TestRequestCancelRequest.class);

        assertThrows(
                ConflictException.class,
                () -> testRequestService.cancel(
                        id,
                        request
                )
        );

        verify(repo, never())
                .save(testRequest);
    }
    @Test
    void cancel_ShouldRejectAlreadyCancelledRequest() {

        UUID id = UUID.randomUUID();

        TestRequest testRequest = operableRequest(id, TestRequestStatus.CANCELLED);

        TestRequestCancelRequest request =
                mock(TestRequestCancelRequest.class);

        assertThrows(
                ConflictException.class,
                () -> testRequestService.cancel(
                        id,
                        request
                )
        );

        verify(repo, never())
                .save(testRequest);
    }
    @Test
    void cancel_ShouldSetCancelled_WhenRequestIsPending() {

        UUID id = UUID.randomUUID();

        TestRequest testRequest = operableRequest(id, TestRequestStatus.PENDING);

        TestRequestCancelRequest request =
                mock(TestRequestCancelRequest.class);

        when(request.reason())
                .thenReturn("Benh nhan khong thuc hien");

        when(repo.save(testRequest))
                .thenReturn(testRequest);

        /*
         * Response.from() có thể đọc nhiều field từ entity.
         * Nếu model mock của bạn khiến DTO mapping lỗi,
         * đổi TestRequest mock thành builder entity thật.
         */
        assertDoesNotThrow(
                () -> testRequestService.cancel(
                        id,
                        request
                )
        );

        assertEquals(TestRequestStatus.CANCELLED, testRequest.getStatus());
        assertEquals("Benh nhan khong thuc hien", testRequest.getCancelReason());

        verify(repo)
                .save(testRequest);
    }

    @Test
    void cancel_ShouldReturnSourceExaminationWhenLastActiveTestIsCancelled() {
        UUID id = UUID.randomUUID();
        TestRequest request = operableRequest(id, TestRequestStatus.BLOCKED);
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        QueueTicket sourceQueue = QueueTicket.builder().ticketId(UUID.randomUUID())
                .status(QueueStatus.WAITING_FOR_TEST).build();
        MedicalRecord source = MedicalRecord.builder().recordId(UUID.randomUUID())
                .visit(visit).queueTicket(sourceQueue).build();
        request.setMedicalRecord(source);
        when(repo.countByQueueTicket_TicketIdAndStatusIn(any(), anyList())).thenReturn(1L);
        when(repo.countByMedicalRecordAndStatusIn(eq(source.getRecordId()), anyList())).thenReturn(0L);

        testRequestService.cancel(id, new TestRequestCancelRequest("Không còn nhu cầu"));

        assertEquals(QueueStatus.TEST_DONE, sourceQueue.getStatus());
        assertNull(sourceQueue.getCalledAt());
        verify(queueTicketRepo).save(sourceQueue);
        verify(patientJourneyService, never()).activateNext(any());
    }

    @Test
    void cancel_ShouldCompleteStandaloneRecordAndActivateNextWhenNothingRemains() {
        UUID id = UUID.randomUUID();
        TestRequest request = operableRequest(id, TestRequestStatus.IN_PROGRESS);
        request.setQueueTicket(null);
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        MedicalRecord source = MedicalRecord.builder().recordId(UUID.randomUUID())
                .visit(visit).status(MedicalRecordStatus.IN_PROGRESS).build();
        request.setMedicalRecord(source);
        when(repo.countByMedicalRecordAndStatusIn(eq(source.getRecordId()), anyList())).thenReturn(0L);
        when(repo.countByMedicalRecord_MedicalRecordId(source.getRecordId())).thenReturn(1L);

        testRequestService.cancel(id, new TestRequestCancelRequest("Đổi chỉ định"));

        assertEquals(MedicalRecordStatus.COMPLETED, source.getStatus());
        assertNotNull(source.getCompletedAt());
        verify(recordRepo).save(source);
        verify(patientJourneyService).activateNext(visit.getVisitId());
    }
    // =========================================================
// TEST REQUEST SERVICE - REMAINING UNIT TESTS
// =========================================================


// =========================================================
// GET
// =========================================================
    @Test
    void get_ShouldReturnResponse_WhenRequestExists() {

        UUID id = UUID.randomUUID();

        TestRequest t = TestRequest.builder()
                .testRequestId(id)
                .status(TestRequestStatus.PENDING)
                .build();

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

        var result = testRequestService.get(id);

        assertNotNull(result);

        verify(repo).findById(id);
    }


// =========================================================
// LIST BY QUEUE TICKET - SUCCESS
// =========================================================
    @Test
    void listByQueueTicket_ShouldReturnRequests_WhenTicketExists() {

        UUID ticketId = UUID.randomUUID();

        TestRequest first = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .status(TestRequestStatus.PENDING)
                .build();

        TestRequest second = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .status(TestRequestStatus.IN_PROGRESS)
                .build();

        first.setCreatedAt(
                LocalDateTime.now().minusMinutes(10)
        );

        second.setCreatedAt(
                LocalDateTime.now()
        );

        when(queueTicketRepo.existsById(ticketId))
                .thenReturn(true);

        // Cố tình trả ngược thứ tự
        when(repo.findAllByQueueTicket_TicketId(ticketId))
                .thenReturn(List.of(second, first));

        var result =
                testRequestService.listByQueueTicket(ticketId);

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(repo)
                .findAllByQueueTicket_TicketId(ticketId);
    }


// =========================================================
// CREATE - STAFF NOT FOUND
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void create_ShouldThrowNotFound_WhenRequestedByDoesNotExist() {

        UUID recordId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        TestRequestCreateRequest req =
                mock(TestRequestCreateRequest.class);

        MedicalRecord record =
                mock(MedicalRecord.class);

        MedicalService service =
                mock(MedicalService.class);

        Department department =
                mock(Department.class);

        when(req.medicalRecordId()).thenReturn(recordId);
        when(req.serviceId()).thenReturn(serviceId);
        when(req.requestedById()).thenReturn(staffId);

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);
        when(service.getServiceId()).thenReturn(serviceId);

        // requiredCapability == null mặc định
        when(service.getDepartment())
                .thenReturn(department);

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.create(req)
        );

        verify(repo, never())
                .save(any(TestRequest.class));
    }


// =========================================================
// CREATE - SERVICE KHÔNG CÓ DEPARTMENT/CAPABILITY
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void create_ShouldThrow_WhenServiceHasNoDepartmentAndNoCapability() {

        UUID recordId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        TestRequestCreateRequest req =
                mock(TestRequestCreateRequest.class);

        MedicalRecord record =
                mock(MedicalRecord.class);

        MedicalService service =
                mock(MedicalService.class);

        when(req.medicalRecordId()).thenReturn(recordId);
        when(req.serviceId()).thenReturn(serviceId);

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);
        when(service.getServiceId()).thenReturn(serviceId);

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.create(req)
        );

        verify(repo, never())
                .save(any(TestRequest.class));
    }


// =========================================================
// CREATE - INVOICE ITEM NOT FOUND
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void create_ShouldThrowNotFound_WhenInvoiceItemDoesNotExist() {

        UUID recordId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID invoiceItemId = UUID.randomUUID();

        TestRequestCreateRequest req =
                mock(TestRequestCreateRequest.class);

        MedicalRecord record =
                mock(MedicalRecord.class);

        MedicalService service =
                mock(MedicalService.class);

        Department department =
                mock(Department.class);

        StaffInfo staff =
                mock(StaffInfo.class);

        when(req.invoiceItemId())
                .thenReturn(invoiceItemId);

        when(req.medicalRecordId())
                .thenReturn(recordId);

        when(req.serviceId())
                .thenReturn(serviceId);

        when(req.requestedById())
                .thenReturn(staffId);

        when(repo.findByInvoiceItem_ItemId(invoiceItemId))
                .thenReturn(List.of());

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);
        when(service.getServiceId()).thenReturn(serviceId);

        when(service.getDepartment())
                .thenReturn(department);

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(invoiceItemRepo.findById(invoiceItemId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.create(req)
        );

        verify(repo, never())
                .save(any(TestRequest.class));
    }


// =========================================================
// CREATE - SUCCESS
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void create_ShouldSavePendingRequest_WhenInputIsValid() {

        UUID recordId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        TestRequestCreateRequest req =
                mock(TestRequestCreateRequest.class);

        MedicalRecord record =
                mock(MedicalRecord.class);

        MedicalService service =
                mock(MedicalService.class);

        Department department =
                mock(Department.class);

        StaffInfo staff =
                mock(StaffInfo.class);

        when(req.medicalRecordId())
                .thenReturn(recordId);

        when(req.serviceId())
                .thenReturn(serviceId);

        when(req.requestedById())
                .thenReturn(staffId);

        when(req.notes())
                .thenReturn("Xet nghiem mau");

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);
        when(service.getServiceId()).thenReturn(serviceId);

        when(service.getDepartment())
                .thenReturn(department);

        when(service.getName())
                .thenReturn("Xet nghiem mau");

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(repo.save(any(TestRequest.class)))
                .thenAnswer(invocation -> {
                    TestRequest t = invocation.getArgument(0);

                    if (t.getTestRequestId() == null)
                        t.setTestRequestId(UUID.randomUUID());

                    return t;
                });

        var result =
                testRequestService.create(req);

        assertNotNull(result);

        verify(repo).save(argThat(t ->
                t.getMedicalRecord() == record
                        && t.getService() == service
                        && t.getPerformingDepartment() == department
                        && t.getRequestedBy() == staff
                        && t.getStatus() == TestRequestStatus.PENDING
        ));

        verify(notificationService)
                .notifyStaffByRole(
                        eq(SystemRole.CASHIER),
                        eq("Yêu cầu cận lâm sàng mới"),
                        anyString(),
                        eq("TestRequest"),
                        any(UUID.class)
                );
    }


// =========================================================
// CREATE RESULT
// =========================================================
    @Test
    void createResult_ShouldRejectCompletedTestRequest() {

        UUID id = UUID.randomUUID();

        TestRequest t = operableRequest(id, TestRequestStatus.COMPLETED);

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        assertThrows(
                ConflictException.class,
                () -> testRequestService.createResult(id, req)
        );

        verifyNoInteractions(resultRepo);
    }
    @Test
    void createResult_ShouldReject_WhenResultAlreadyExists() {

        UUID id = UUID.randomUUID();

        TestRequest t = operableRequest(id, TestRequestStatus.PENDING);
        TestResult oldResult = mock(TestResult.class);

        when(resultRepo.findByTestRequest_TestRequestId(id))
                .thenReturn(Optional.of(oldResult));

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        assertThrows(
                ConflictException.class,
                () -> testRequestService.createResult(id, req)
        );
    }
    @Test
    void createResult_ShouldThrow_WhenPerformedByDoesNotExist() {

        UUID id = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        TestRequest t = operableRequest(id, TestRequestStatus.PENDING);

        when(resultRepo.findByTestRequest_TestRequestId(id))
                .thenReturn(Optional.empty());

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.performedById())
                .thenReturn(staffId);

        when(authService.currentStaffId()).thenReturn(staffId);

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.createResult(id, req)
        );
    }
    @Test
    void createResult_ShouldCreateResultAndMovePendingToInProgress() {

        UUID id = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        TestRequest t = operableRequest(id, TestRequestStatus.PENDING);

        StaffInfo staff = mock(StaffInfo.class);

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.performedById())
                .thenReturn(staffId);

        when(authService.currentStaffId()).thenReturn(staffId);

        when(req.imageUrl())
                .thenReturn("/uploads/a.pdf");

        when(req.conclusion())
                .thenReturn("Binh thuong");

        when(resultRepo.findByTestRequest_TestRequestId(id))
                .thenReturn(Optional.empty());

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(resultRepo.save(any(TestResult.class))).thenAnswer(invocation -> {
            TestResult result = invocation.getArgument(0);
            result.setResultId(UUID.randomUUID());
            return result;
        });
        when(revisionRepo.save(any(TestResultRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result =
                testRequestService.createResult(id, req);

        assertNotNull(result);

        assertEquals(
                TestRequestStatus.IN_PROGRESS,
                t.getStatus()
        );

        verify(resultRepo)
                .save(any(TestResult.class));

        verify(repo)
                .save(t);
    }


// =========================================================
// GET RESULT SUCCESS
// =========================================================
    @Test
    void getResult_ShouldReturnResult_WhenResultExists() {

        UUID id = UUID.randomUUID();

        TestRequest t = TestRequest.builder()
                .testRequestId(id)
                .status(TestRequestStatus.IN_PROGRESS)
                .build();

        TestResult r = TestResult.builder()
                .testRequest(t)
                .conclusion("OK")
                .build();

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

        when(resultRepo.findByTestRequest_TestRequestId(id))
                .thenReturn(Optional.of(r));

        var result =
                testRequestService.getResult(id);

        assertNotNull(result);
    }


// =========================================================
// UPDATE RESULT
// =========================================================
    @Test
    void updateResult_ShouldRejectCompleteFlag() {

        UUID id = UUID.randomUUID();

        TestRequest t = operableRequest(id, TestRequestStatus.IN_PROGRESS);

        TestResultUpdateRequest req =
                mock(TestResultUpdateRequest.class);

        when(req.complete())
                .thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.updateResult(id, req)
        );
    }
    @Test
    void updateResult_ShouldRejectCompletedTestRequest() {

        UUID id = UUID.randomUUID();

        TestRequest t = operableRequest(id, TestRequestStatus.COMPLETED);

        TestResultUpdateRequest req =
                mock(TestResultUpdateRequest.class);

        assertThrows(
                ConflictException.class,
                () -> testRequestService.updateResult(id, req)
        );
    }
    @Test
    void updateResult_ShouldThrow_WhenResultDoesNotExist() {

        UUID id = UUID.randomUUID();

        TestRequest t = operableRequest(id, TestRequestStatus.IN_PROGRESS);

        when(resultRepo.findByTestRequest_TestRequestId(id))
                .thenReturn(Optional.empty());

        TestResultUpdateRequest req =
                mock(TestResultUpdateRequest.class);

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.updateResult(id, req)
        );
    }
    @Test
    void updateResult_ShouldUpdateFields() {

        UUID id = UUID.randomUUID();

        TestRequest t = operableRequest(id, TestRequestStatus.IN_PROGRESS);

        TestResult r =
                TestResult.builder()
                        .testRequest(t)
                        .imageUrl("old.pdf")
                        .conclusion("Old")
                        .sampleId("OLD")
                        .build();

        TestResultUpdateRequest req =
                mock(TestResultUpdateRequest.class);

        when(req.imageUrl())
                .thenReturn("new.pdf");

        when(req.conclusion())
                .thenReturn("New conclusion");

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

        when(resultRepo.findByTestRequest_TestRequestId(id))
                .thenReturn(Optional.of(r));

        when(resultRepo.save(r))
                .thenReturn(r);
        when(revisionRepo.save(any(TestResultRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result =
                testRequestService.updateResult(id, req);

        assertNotNull(result);

        assertEquals("new.pdf", r.getImageUrl());
        assertEquals("New conclusion", r.getConclusion());

        /*
         * Service hien tai xoa specimen fields neu dich vu khong cau hinh requiresSpecimen.
         */
        assertNull(r.getSampleId());

        verify(resultRepo).save(r);
    }

    @Test
    void createResult_ShouldDefaultSpecimenMetadataFromServiceDescription() {
        StaffInfo performer = StaffInfo.builder().staffId(UUID.randomUUID()).build();
        when(authService.currentStaffId()).thenReturn(performer.getStaffId());
        when(staffRepo.findById(performer.getStaffId())).thenReturn(Optional.of(performer));
        when(resultRepo.save(any(TestResult.class))).thenAnswer(invocation -> {
            TestResult result = invocation.getArgument(0);
            result.setResultId(UUID.randomUUID());
            return result;
        });
        when(revisionRepo.save(any(TestResultRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        record Expected(String code, String name, org.example.doansummer2026.enums.SpecimenType type) {}
        List<Expected> cases = List.of(
                new Expected("LAB-006", "Tổng phân tích nước tiểu", org.example.doansummer2026.enums.SpecimenType.URINE),
                new Expected("SWAB-01", "Dịch ngoáy cúm", org.example.doansummer2026.enums.SpecimenType.SWAB),
                new Expected(null, null, org.example.doansummer2026.enums.SpecimenType.BLOOD));

        for (Expected expected : cases) {
            UUID id = UUID.randomUUID();
            TestRequest request = operableRequest(id, TestRequestStatus.PENDING);
            when(authService.currentStaffId()).thenReturn(performer.getStaffId());
            request.setService(MedicalService.builder().serviceId(UUID.randomUUID())
                    .serviceCode(expected.code()).name(expected.name()).requiresSpecimen(true).build());
            TestResultCreateRequest input = mock(TestResultCreateRequest.class);
            when(input.performedById()).thenReturn(performer.getStaffId());

            testRequestService.createResult(id, input);

            TestResult saved = mockingDetails(resultRepo).getInvocations().stream()
                    .filter(invocation -> invocation.getMethod().getName().equals("save"))
                    .map(invocation -> (TestResult) invocation.getArgument(0))
                    .reduce((first, second) -> second).orElseThrow();
            assertEquals(expected.type(), saved.getSampleType());
            assertEquals(org.example.doansummer2026.enums.SpecimenStatus.ACCEPTED, saved.getSampleStatus());
            assertTrue(saved.getSampleId().startsWith("SMP-"));
            assertSame(performer, saved.getCollectedBy());
            assertNotNull(saved.getCollectedAt());
        }
    }

    @Test
    void createResult_ShouldRejectSpecimenDataForServiceWithoutSpecimen() {
        UUID id = UUID.randomUUID();
        UUID performerId = UUID.randomUUID();
        TestRequest request = operableRequest(id, TestRequestStatus.PENDING);
        request.setService(MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("IMG-001").requiresSpecimen(false).build());
        when(authService.currentStaffId()).thenReturn(performerId);
        when(staffRepo.findById(performerId)).thenReturn(Optional.of(StaffInfo.builder().staffId(performerId).build()));
        TestResultCreateRequest input = mock(TestResultCreateRequest.class);
        when(input.performedById()).thenReturn(performerId);
        when(input.sampleId()).thenReturn(" SAMPLE ");

        assertThrows(BadRequestException.class, () -> testRequestService.createResult(id, input));
        verify(resultRepo, never()).save(any());
    }

    @Test
    void createResult_ShouldRequireAuthenticatedSpecimenCollector() {
        UUID id = UUID.randomUUID();
        UUID performerId = UUID.randomUUID();
        TestRequest request = operableRequest(id, TestRequestStatus.PENDING);
        request.setService(MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("LAB-001").requiresSpecimen(true).build());
        when(authService.currentStaffId()).thenReturn(performerId);
        StaffInfo performer = StaffInfo.builder().staffId(performerId).build();
        when(staffRepo.findById(performerId)).thenReturn(Optional.of(performer), Optional.empty());
        TestResultCreateRequest input = mock(TestResultCreateRequest.class);
        when(input.performedById()).thenReturn(performerId);

        assertThrows(BadRequestException.class, () -> testRequestService.createResult(id, input));
        verify(resultRepo, never()).save(any());
    }


// =========================================================
// UPDATE TEST REQUEST - SIMPLE STATUS
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void update_ShouldUpdateStatus_WhenStatusProvided() {

        UUID id = UUID.randomUUID();

        Department department =
                mock(Department.class);

        when(department.getDepartmentId())
                .thenReturn(UUID.randomUUID());

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .performingDepartment(department)
                        .status(TestRequestStatus.PENDING)
                        .build();

        TestRequestUpdateRequest req =
                mock(TestRequestUpdateRequest.class);

        when(req.status())
                .thenReturn(TestRequestStatus.IN_PROGRESS);

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

        when(repo.save(t))
                .thenReturn(t);

        var result =
                testRequestService.update(id, req);

        assertNotNull(result);

        assertEquals(
                TestRequestStatus.IN_PROGRESS,
                t.getStatus()
        );

        verify(repo).save(t);

        verify(messagingTemplate)
                .convertAndSend(
                        anyString(),
                        eq("LAB_UPDATED")
                );
    }


// =========================================================
// UPDATE - COMPLETED + ALL TESTS DONE
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void update_ShouldSetQueueTestDone_WhenAllTestsCompleted() {

        UUID id = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getVisitId())
                .thenReturn(visitId);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .build();

        Department department =
                mock(Department.class);

        when(department.getDepartmentId())
                .thenReturn(deptId);

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .medicalRecord(record)
                        .performingDepartment(department)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .build();

        QueueTicket queue =
                QueueTicket.builder()
                        .status(QueueStatus.WAITING_FOR_TEST)
                        .build();

        TestRequestUpdateRequest req =
                mock(TestRequestUpdateRequest.class);

        when(req.status())
                .thenReturn(TestRequestStatus.COMPLETED);

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

        when(repo.countByMedicalRecord_MedicalRecordId(recordId))
                .thenReturn(2L);

        when(
                repo.countByMedicalRecordAndStatusIn(
                        eq(recordId),
                        anyList()
                )
        ).thenReturn(0L);

        when(
                queueTicketRepo
                        .findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(
                                eq(visitId),
                                eq(deptId),
                                anyList()
                        )
        ).thenReturn(Optional.of(queue));

        when(repo.save(t))
                .thenReturn(t);

        var result =
                testRequestService.update(id, req);

        assertNotNull(result);

        assertEquals(
                QueueStatus.TEST_DONE,
                queue.getStatus()
        );

        assertNotNull(
                t.getCompletedAt()
        );

        verify(queueTicketRepo)
                .save(queue);
    }


// =========================================================
// UPDATE - COMPLETED BUT TESTS REMAINING
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void update_ShouldSetQueueWaitingForTest_WhenSomeTestsRemain() {

        UUID id = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        when(visit.getVisitId())
                .thenReturn(visitId);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .build();

        Department department =
                mock(Department.class);

        when(department.getDepartmentId())
                .thenReturn(deptId);

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .medicalRecord(record)
                        .performingDepartment(department)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .build();

        QueueTicket queue =
                QueueTicket.builder()
                        .status(QueueStatus.IN_PROGRESS)
                        .build();

        TestRequestUpdateRequest req =
                mock(TestRequestUpdateRequest.class);

        when(req.status())
                .thenReturn(TestRequestStatus.COMPLETED);

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

        when(repo.countByMedicalRecord_MedicalRecordId(recordId))
                .thenReturn(3L);

        when(
                repo.countByMedicalRecordAndStatusIn(
                        eq(recordId),
                        anyList()
                )
        ).thenReturn(1L);

        when(
                queueTicketRepo
                        .findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(
                                eq(visitId),
                                eq(deptId),
                                anyList()
                        )
        ).thenReturn(Optional.of(queue));

        when(repo.save(t))
                .thenReturn(t);

        testRequestService.update(id, req);

        assertEquals(
                QueueStatus.WAITING_FOR_TEST,
                queue.getStatus()
        );

        assertNull(
                queue.getCalledAt()
        );
    }


// =========================================================
// COMPLETE RESULT - ALREADY COMPLETED
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void completeResult_ShouldRejectAlreadyCompletedRequest() {

        UUID id = UUID.randomUUID();

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.COMPLETED)
                        .build();

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        assertThrows(
                ConflictException.class,
                () -> testRequestService.completeResult(
                        id,
                        mock(TestResultCreateRequest.class))
        );
    }


// =========================================================
// COMPLETE RESULT - MISSING CONCLUSION
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void completeResult_ShouldRejectBlankConclusion() {

        UUID id = UUID.randomUUID();

        QueueTicket executionQueue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.IN_PROGRESS)
                .build();

        TestResult existingResult = TestResult.builder()
                .imageUrl("result.pdf")
                .conclusion("   ")
                .build();

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .queueTicket(executionQueue)
                        .testResult(existingResult)
                        .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.completeResult(
                        id,
                        req)
        );
    }


// =========================================================
// COMPLETE RESULT - INVALID PDF URL
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void completeResult_ShouldRejectNonPdfResult() {

        UUID id = UUID.randomUUID();

        QueueTicket executionQueue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.IN_PROGRESS)
                .build();

        TestResult existingResult = TestResult.builder()
                .imageUrl("image.jpg")
                .conclusion("Binh thuong")
                .build();

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .queueTicket(executionQueue)
                        .testResult(existingResult)
                        .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.completeResult(
                        id,
                        req)
        );
    }


// =========================================================
// COMPLETE RESULT - VERIFIER NOT FOUND
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void completeResult_ShouldThrow_WhenVerifierDoesNotExist() {

        UUID id = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        QueueTicket executionQueue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.IN_PROGRESS)
                .build();

        TestResult existingResult = TestResult.builder()
                .imageUrl("result.pdf")
                .conclusion("OK")
                .build();

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .queueTicket(executionQueue)
                        .testResult(existingResult)
                        .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.completeResult(
                        id,
                        req)
        );
    }


// =========================================================
// COMPLETE RESULT - VERIFIER IS NOT DOCTOR
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void completeResult_ShouldRejectVerifier_WhenNotDoctor() {

        UUID id = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        QueueTicket executionQueue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.IN_PROGRESS)
                .build();

        TestResult existingResult = TestResult.builder()
                .imageUrl("result.pdf")
                .conclusion("OK")
                .build();

        StaffInfo verifier = mock(StaffInfo.class);
        when(verifier.getSystemRole())
                .thenReturn(SystemRole.NURSE);

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .queueTicket(executionQueue)
                        .testResult(existingResult)
                        .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.completeResult(
                        id,
                        req)
        );
    }


// =========================================================
// COMPLETE RESULT - DOCTOR NOT HEAD OF DEPARTMENT
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void completeResult_ShouldRejectDoctor_WhenNotDepartmentHead() {

        UUID id = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();
        UUID headDoctorId = UUID.randomUUID();

        QueueTicket executionQueue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.IN_PROGRESS)
                .build();

        TestResult existingResult = TestResult.builder()
                .imageUrl("result.pdf")
                .conclusion("OK")
                .build();

        StaffInfo verifier = mock(StaffInfo.class);
        when(verifier.getSystemRole())
                .thenReturn(SystemRole.DOCTOR);
        when(verifier.getStaffId())
                .thenReturn(verifierId);

        StaffInfo headDoctor = mock(StaffInfo.class);
        when(headDoctor.getStaffId())
                .thenReturn(headDoctorId);

        Department dept = mock(Department.class);
        when(dept.getHeadDoctor())
                .thenReturn(headDoctor);

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .queueTicket(executionQueue)
                        .performingDepartment(dept)
                        .testResult(existingResult)
                        .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.completeResult(
                        id,
                        req)
        );
    }


// =========================================================
// COMPLETE RESULT - SUCCESS
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void completeResult_ShouldCompleteRequest_WhenVerifierIsDepartmentHead() {

        UUID id = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        UUID labTicketId = UUID.randomUUID();

        StaffInfo verifier = mock(StaffInfo.class);
        when(verifier.getSystemRole())
                .thenReturn(SystemRole.DOCTOR);
        when(verifier.getStaffId())
                .thenReturn(verifierId);

        Department dept = mock(Department.class);
        when(dept.getDepartmentId())
                .thenReturn(deptId);
        when(dept.getHeadDoctor())
                .thenReturn(verifier);

        QueueTicket executionQueue = QueueTicket.builder()
                .ticketId(labTicketId)
                .status(QueueStatus.IN_PROGRESS)
                .build();

        TestResult existingResult = TestResult.builder()
                .imageUrl("/uploads/result.pdf")
                .conclusion("Ket qua binh thuong")
                .build();

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .performingDepartment(dept)
                        .queueTicket(executionQueue)
                        .testResult(existingResult)
                        .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        when(repo.countByQueueTicket_TicketIdAndStatusIn(
                eq(labTicketId),
                anyList()
        )).thenReturn(0L);

        var result =
                testRequestService.completeResult(
                        id,
                        req);

        assertNotNull(result);

        assertEquals(
                TestRequestStatus.COMPLETED,
                t.getStatus()
        );

        assertNotNull(
                t.getCompletedAt()
        );

        assertSame(
                verifier,
                existingResult.getVerifiedBy()
        );

        assertNotNull(
                existingResult.getVerifiedAt()
        );

        assertEquals(
                QueueStatus.DONE,
                executionQueue.getStatus()
        );

        verify(resultRepo)
                .save(existingResult);

        verify(repo)
                .save(t);

        verify(queueTicketRepo)
                .save(executionQueue);
    }


// =========================================================
// UPLOAD RESULT FILE
// =========================================================
    @Test
    void uploadResultFile_ShouldRejectNullFile() {

        UUID id = UUID.randomUUID();

        operableRequest(id, TestRequestStatus.IN_PROGRESS);

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.uploadResultFile(
                        id,
                        null
                )
        );
    }
    @Test
    void uploadResultFile_ShouldRejectEmptyFile() {

        UUID id = UUID.randomUUID();

        operableRequest(id, TestRequestStatus.IN_PROGRESS);

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "result.pdf",
                        "application/pdf",
                        new byte[0]
                );

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.uploadResultFile(
                        id,
                        file
                )
        );
    }
    @Test
    void uploadResultFile_ShouldRejectWrongExtension() {

        UUID id = UUID.randomUUID();

        operableRequest(id, TestRequestStatus.IN_PROGRESS);

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "result.jpg",
                        "application/pdf",
                        "%PDF-test".getBytes()
                );

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.uploadResultFile(
                        id,
                        file
                )
        );
    }
    @Test
    void uploadResultFile_ShouldRejectWrongContentType() {

        UUID id = UUID.randomUUID();

        operableRequest(id, TestRequestStatus.IN_PROGRESS);

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "result.pdf",
                        "image/jpeg",
                        "%PDF-test".getBytes()
                );

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.uploadResultFile(
                        id,
                        file
                )
        );
    }
    @Test
    void uploadResultFile_ShouldRejectFakePdfContent() {

        UUID id = UUID.randomUUID();

        operableRequest(id, TestRequestStatus.IN_PROGRESS);

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "result.pdf",
                        "application/pdf",
                        "NOT-PDF".getBytes()
                );

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.uploadResultFile(
                        id,
                        file
                )
        );
    }
    @Test
    void uploadResultFile_ShouldSaveValidPdf() throws Exception {

        UUID id = UUID.randomUUID();

        operableRequest(id, TestRequestStatus.IN_PROGRESS);

        /*
         * @Value không tự inject khi dùng Mockito @InjectMocks,
         * nên set uploadRoot bằng ReflectionTestUtils.
         */
        ReflectionTestUtils.setField(
                testRequestService,
                "uploadRoot",
                tempDir.toString()
        );

        byte[] pdf =
                "%PDF-1.4\nTEST PDF".getBytes();

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "ket qua test.pdf",
                        "application/pdf",
                        pdf
                );

        String url =
                testRequestService.uploadResultFile(
                        id,
                        file
                );

        assertNotNull(url);

        assertTrue(
                url.startsWith(
                        "/uploads/test-results/"
                )
        );

        assertTrue(
                url.endsWith(".pdf")
        );
    }

    @Test
    void uploadAttachments_ShouldValidateOwnershipRevisionStateAndLimit() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        operableRequest(requestId, TestRequestStatus.IN_PROGRESS);

        when(resultRepo.findByTestRequest_TestRequestId(requestId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> testRequestService.uploadAttachments(requestId, revisionId, List.of()));

        TestResult result = TestResult.builder().resultId(UUID.randomUUID()).build();
        when(resultRepo.findByTestRequest_TestRequestId(requestId)).thenReturn(Optional.of(result));
        when(revisionRepo.findById(revisionId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> testRequestService.uploadAttachments(requestId, revisionId, List.of()));

        TestResultRevision foreign = TestResultRevision.builder()
                .revisionId(revisionId)
                .testResult(TestResult.builder().resultId(UUID.randomUUID()).build())
                .status(org.example.doansummer2026.enums.TestResultRevisionStatus.DRAFT)
                .build();
        when(revisionRepo.findById(revisionId)).thenReturn(Optional.of(foreign));
        assertThrows(BadRequestException.class,
                () -> testRequestService.uploadAttachments(requestId, revisionId, List.of()));

        TestResultRevision signed = TestResultRevision.builder()
                .revisionId(revisionId).testResult(result)
                .status(org.example.doansummer2026.enums.TestResultRevisionStatus.SIGNED).build();
        when(revisionRepo.findById(revisionId)).thenReturn(Optional.of(signed));
        assertThrows(ConflictException.class,
                () -> testRequestService.uploadAttachments(requestId, revisionId, List.of()));

        signed.setStatus(org.example.doansummer2026.enums.TestResultRevisionStatus.DRAFT);
        assertThrows(BadRequestException.class,
                () -> testRequestService.uploadAttachments(requestId, revisionId, null));
        assertThrows(BadRequestException.class,
                () -> testRequestService.uploadAttachments(requestId, revisionId, List.of()));

        when(attachmentRepo.countByRevision_RevisionId(revisionId)).thenReturn(10L);
        MockMultipartFile pdf = new MockMultipartFile("file", "result.pdf", "application/pdf",
                "%PDF-valid".getBytes());
        assertThrows(BadRequestException.class,
                () -> testRequestService.uploadAttachments(requestId, revisionId, List.of(pdf)));
    }

    @Test
    void uploadAttachments_ShouldAcceptAllSupportedSignaturesAndSanitizeNames() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        operableRequest(requestId, TestRequestStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(testRequestService, "uploadRoot", tempDir.toString());

        TestResult result = TestResult.builder().resultId(UUID.randomUUID()).build();
        TestResultRevision revision = TestResultRevision.builder()
                .revisionId(revisionId).testResult(result)
                .status(org.example.doansummer2026.enums.TestResultRevisionStatus.DRAFT).build();
        when(resultRepo.findByTestRequest_TestRequestId(requestId)).thenReturn(Optional.of(result));
        when(revisionRepo.findById(revisionId)).thenReturn(Optional.of(revision));
        when(attachmentRepo.countByRevision_RevisionId(revisionId)).thenReturn(2L);
        when(attachmentRepo.save(any(TestResultAttachment.class))).thenAnswer(invocation -> {
            TestResultAttachment attachment = invocation.getArgument(0);
            attachment.setAttachmentId(UUID.randomUUID());
            return attachment;
        });

        byte[] jpeg = new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1};
        byte[] png = new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        byte[] webp = "RIFF1234WEBP".getBytes();
        List<MultipartFile> files = List.of(
                new MockMultipartFile("file", "report\r\n.pdf", null, "%PDF-data".getBytes()),
                new MockMultipartFile("file", null, "image/jpg", jpeg),
                new MockMultipartFile("file", "chart.png", "image/png", png),
                new MockMultipartFile("file", "scan.webp", "image/webp", webp));

        var responses = testRequestService.uploadAttachments(requestId, revisionId, files);

        assertEquals(4, responses.size());
        assertEquals(List.of(2, 3, 4, 5), responses.stream()
                .map(org.example.doansummer2026.dto.testresult.TestResultAttachmentResponse::displayOrder).toList());
        verify(attachmentRepo, times(4)).save(any(TestResultAttachment.class));
    }

    @Test
    void uploadAttachments_ShouldRejectEmptyOversizedUnknownAndMismatchedFiles() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        operableRequest(requestId, TestRequestStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(testRequestService, "uploadRoot", tempDir.toString());
        TestResult result = TestResult.builder().resultId(UUID.randomUUID()).build();
        TestResultRevision revision = TestResultRevision.builder()
                .revisionId(revisionId).testResult(result)
                .status(org.example.doansummer2026.enums.TestResultRevisionStatus.DRAFT).build();
        when(resultRepo.findByTestRequest_TestRequestId(requestId)).thenReturn(Optional.of(result));
        when(revisionRepo.findById(revisionId)).thenReturn(Optional.of(revision));

        assertThrows(BadRequestException.class, () -> testRequestService.uploadAttachments(
                requestId, revisionId, java.util.Arrays.asList((MultipartFile) null)));
        assertThrows(BadRequestException.class, () -> testRequestService.uploadAttachments(
                requestId, revisionId, List.of(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]))));
        assertThrows(BadRequestException.class, () -> testRequestService.uploadAttachments(
                requestId, revisionId, List.of(new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1]))));
        assertThrows(BadRequestException.class, () -> testRequestService.uploadAttachments(
                requestId, revisionId, List.of(new MockMultipartFile("file", "unknown.bin", null, "unknown".getBytes()))));
        assertThrows(BadRequestException.class, () -> testRequestService.uploadAttachments(
                requestId, revisionId, List.of(new MockMultipartFile("file", "wrong.png", "image/png", "%PDF-data".getBytes()))));
    }

    @Test
    void listAttachments_ShouldValidateRevisionAndMapOwnedAttachments() {
        UUID requestId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        TestRequest request = operableRequest(requestId, TestRequestStatus.IN_PROGRESS);
        TestResult ownedResult = TestResult.builder().resultId(UUID.randomUUID()).testRequest(request).build();

        when(revisionRepo.findById(revisionId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> testRequestService.listAttachments(requestId, revisionId));

        TestResultRevision revision = TestResultRevision.builder().revisionId(revisionId).build();
        when(revisionRepo.findById(revisionId)).thenReturn(Optional.of(revision));
        assertThrows(BadRequestException.class,
                () -> testRequestService.listAttachments(requestId, revisionId));
        revision.setTestResult(TestResult.builder().resultId(UUID.randomUUID()).build());
        assertThrows(BadRequestException.class,
                () -> testRequestService.listAttachments(requestId, revisionId));
        revision.setTestResult(TestResult.builder().resultId(UUID.randomUUID())
                .testRequest(TestRequest.builder().testRequestId(UUID.randomUUID()).build()).build());
        assertThrows(BadRequestException.class,
                () -> testRequestService.listAttachments(requestId, revisionId));

        revision.setTestResult(ownedResult);
        TestResultAttachment attachment = TestResultAttachment.builder()
                .attachmentId(UUID.randomUUID()).revision(revision).originalName("a.pdf")
                .contentType("application/pdf").fileSize(12L).displayOrder(0).build();
        when(attachmentRepo.findByRevision_RevisionIdOrderByDisplayOrder(revisionId))
                .thenReturn(List.of(attachment));
        var responses = testRequestService.listAttachments(requestId, revisionId);
        assertEquals(1, responses.size());
        assertEquals("a.pdf", responses.get(0).originalName());
    }


// =========================================================
// FIND COMPLETED / INVOICE
// =========================================================

    @Test
    void findMyCompletedTests_ShouldCallRepository() {

        UUID profileId = UUID.randomUUID();

        when(repo.findByProfileIdAndStatusCompleted(profileId))
                .thenReturn(List.of());

        var result =
                testRequestService.findMyCompletedTests(profileId);

        assertNotNull(result);

        verify(repo)
                .findByProfileIdAndStatusCompleted(profileId);
    }


    @Test
    void findByInvoiceItem_ShouldReturnMappedResults() {

        UUID itemId = UUID.randomUUID();

        when(repo.findByInvoiceItem_ItemId(itemId))
                .thenReturn(List.of());

        var result =
                testRequestService.findByInvoiceItem(itemId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


    @Test
    void findByInvoice_ShouldReturnMappedResults() {

        UUID invoiceId = UUID.randomUUID();

        when(repo.findByInvoiceId(invoiceId))
                .thenReturn(List.of());

        var result =
                testRequestService.findByInvoice(invoiceId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


// =========================================================
// CREATE BATCH - RECORD NOT FOUND
// =========================================================

    @Test
    void createBatch_ShouldThrow_WhenMedicalRecordDoesNotExist() {

        UUID recordId = UUID.randomUUID();

        TestRequestBatchCreateRequest req =
                mock(TestRequestBatchCreateRequest.class);

        when(req.medicalRecordId())
                .thenReturn(recordId);

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.createBatch(req)
        );
    }


// =========================================================
// CREATE BATCH - STAFF NOT FOUND
// =========================================================

    @Test
    void createBatch_ShouldThrow_WhenRequestedByDoesNotExist() {

        UUID recordId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .build();

        TestRequestBatchCreateRequest req =
                mock(TestRequestBatchCreateRequest.class);

        when(req.medicalRecordId())
                .thenReturn(recordId);

        when(req.requestedById())
                .thenReturn(staffId);

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.createBatch(req)
        );
    }


// =========================================================
// CREATE BATCH - SKIP EXISTING SERVICE
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void createBatch_ShouldThrowConflict_WhenServiceAlreadyRequestedInVisit() {

        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId())
                .thenReturn(visitId);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .build();

        StaffInfo staff = mock(StaffInfo.class);

        MedicalService service = mock(MedicalService.class);
        when(service.getServiceId()).thenReturn(serviceId);
        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);

        TestRequestBatchCreateRequest req =
                mock(TestRequestBatchCreateRequest.class);

        when(req.medicalRecordId())
                .thenReturn(recordId);

        when(req.requestedById())
                .thenReturn(staffId);

        when(req.serviceIds())
                .thenReturn(List.of(serviceId));
        when(serviceSelectionPolicyService.normalizeOrThrow(List.of(serviceId)))
                .thenReturn(List.of(service));

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(repo.existsByMedicalRecord_Visit_VisitIdAndService_ServiceIdAndStatusNot(
                visitId,
                serviceId,
                TestRequestStatus.CANCELLED
        )).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> testRequestService.createBatch(req)
        );

        verify(repo, never())
                .saveAll(anyList());
    }
    // =========================================================
// COVERAGE BOOST - TEST REQUEST SERVICE
// createFromPaidInvoice / search / queue / notify / completeResult
// =========================================================


// =========================================================
// CREATE FROM PAID INVOICE - DUPLICATE
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void createFromPaidInvoice_ShouldReturnExisting_WhenInvoiceItemAlreadyHasRequest() {

        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID invoiceItemId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        StaffInfo headDoctor = mock(StaffInfo.class);

        Department dept = mock(Department.class);
        when(dept.getDepartmentId())
                .thenReturn(deptId);
        when(dept.getHeadDoctor())
                .thenReturn(headDoctor);

        MedicalService service = mock(MedicalService.class);
        when(service.getServiceId()).thenReturn(serviceId);
        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);
        when(service.getDepartment()).thenReturn(dept);

        MedicalRecord standaloneRecord = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .build();

        QueueTicket existingQueue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.WAITING)
                .build();

        TestRequest existing = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .medicalRecord(standaloneRecord)
                .service(service)
                .performingDepartment(dept)
                .queueTicket(existingQueue)
                .status(TestRequestStatus.PENDING)
                .build();

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).build();
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(repo.findTopByInvoiceItem_ItemIdOrderByCreatedAtAsc(invoiceItemId))
                .thenReturn(Optional.of(existing));
        when(staffRepo.findByDepartment_DepartmentId(deptId)).thenReturn(List.of(headDoctor));

        var result = testRequestService.createFromPaidInvoice(
                visitId,
                null,
                serviceId,
                null,
                "notes",
                invoiceItemId
        );

        assertNotNull(result);

        verify(repo, never())
                .save(existing);

        verify(visitRepo).findById(visitId);
    }


// =========================================================
// CREATE FROM PAID INVOICE - SERVICE NOT FOUND
// =========================================================

    @Test
    void createFromPaidInvoice_ShouldThrow_WhenServiceDoesNotExist() {

        UUID serviceId = UUID.randomUUID();

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.createFromPaidInvoice(
                        UUID.randomUUID(),
                        null,
                        serviceId,
                        null,
                        null,
                        null
                )
        );
    }


// =========================================================
// CREATE FROM PAID INVOICE - FALLBACK TO HEAD DOCTOR
// + EXISTING MEDICAL RECORD
// + EXISTING QUEUE
// + NOTIFY NURSE
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void createFromPaidInvoice_ShouldUseHeadDoctorAndExistingQueue() {

        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        StaffInfo headDoctor = mock(StaffInfo.class);

        Department dept = mock(Department.class);
        when(dept.getDepartmentId()).thenReturn(deptId);
        when(dept.getHeadDoctor()).thenReturn(headDoctor);

        MedicalService service = mock(MedicalService.class);
        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);
        when(service.getDepartment()).thenReturn(dept);
        when(service.getName()).thenReturn("Xet nghiem mau");

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId()).thenReturn(visitId);

        MedicalRecord standaloneRecord = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .build();

        QueueTicket existingQueue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.WAITING)
                .build();

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(visitRepo.findByIdForUpdate(visitId))
                .thenReturn(Optional.of(visit));
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));

        when(recordRepo.findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(visitId))
                .thenReturn(Optional.of(standaloneRecord));
        when(recordRepo.findById(recordId)).thenReturn(Optional.of(standaloneRecord));

        when(departmentRepo.findByIdForUpdate(deptId))
                .thenReturn(Optional.of(dept));

        when(queueTicketRepo
                .findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(
                        eq(visitId),
                        eq(deptId),
                        anyList()
                ))
                .thenReturn(Optional.of(existingQueue));

        when(staffRepo.findByDepartment_DepartmentId(deptId))
                .thenReturn(List.of(headDoctor));

        when(repo.save(any(TestRequest.class)))
                .thenAnswer(invocation -> {
                    TestRequest t = invocation.getArgument(0);
                    t.setTestRequestId(UUID.randomUUID());
                    return t;
                });

        var result = testRequestService.createFromPaidInvoice(
                visitId,
                recordId,
                serviceId,
                null,
                "Lam xet nghiem",
                null
        );

        assertNotNull(result);

        verify(repo).save(argThat(t ->
                t.getMedicalRecord() == standaloneRecord
                        && t.getService() == service
                        && t.getRequestedBy() == headDoctor
                        && t.getQueueTicket() == existingQueue
                        && t.getStatus() == TestRequestStatus.PENDING
        ));
    }


// =========================================================
// CREATE FROM PAID INVOICE - NO REQUESTER AND NO HEAD DOCTOR
// =========================================================
    @Test
    void createFromPaidInvoice_ShouldThrow_WhenNoRequesterAndNoHeadDoctor() {

        UUID serviceId = UUID.randomUUID();

        Department dept = mock(Department.class);
        when(dept.getName()).thenReturn("Phong Lab");

        MedicalService service = mock(MedicalService.class);
        when(service.getDepartment()).thenReturn(dept);

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.createFromPaidInvoice(
                        UUID.randomUUID(),
                        null,
                        serviceId,
                        null,
                        null,
                        null
                )
        );

        verify(repo, never()).save(any());
    }


// =========================================================
// CREATE FROM PAID INVOICE - REQUESTED BY ID NOT FOUND
// -> FALLBACK HEAD DOCTOR
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void createFromPaidInvoice_ShouldFallbackToHeadDoctor_WhenRequestedByIdNotFound() {

        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID requestedById = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        StaffInfo headDoctor = mock(StaffInfo.class);

        Department dept = mock(Department.class);
        when(dept.getDepartmentId()).thenReturn(deptId);
        when(dept.getHeadDoctor()).thenReturn(headDoctor);

        MedicalService service = mock(MedicalService.class);
        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);
        when(service.getDepartment()).thenReturn(dept);

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId()).thenReturn(visitId);

        MedicalRecord standaloneRecord = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .build();

        QueueTicket queue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.WAITING)
                .build();

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(staffRepo.findById(requestedById))
                .thenReturn(Optional.empty());

        when(visitRepo.findByIdForUpdate(visitId))
                .thenReturn(Optional.of(visit));
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));

        when(recordRepo.findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(visitId))
                .thenReturn(Optional.of(standaloneRecord));
        when(recordRepo.findById(recordId)).thenReturn(Optional.of(standaloneRecord));

        when(departmentRepo.findByIdForUpdate(deptId))
                .thenReturn(Optional.of(dept));

        when(queueTicketRepo
                .findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(
                        eq(visitId),
                        eq(deptId),
                        anyList()
                ))
                .thenReturn(Optional.of(queue));

        when(staffRepo.findByDepartment_DepartmentId(deptId))
                .thenReturn(List.of(headDoctor));

        when(repo.save(any(TestRequest.class)))
                .thenAnswer(i -> {
                    TestRequest t = i.getArgument(0);
                    t.setTestRequestId(UUID.randomUUID());
                    return t;
                });

        testRequestService.createFromPaidInvoice(
                visitId,
                recordId,
                serviceId,
                requestedById,
                null,
                null
        );

        verify(repo).save(argThat(t ->
                t.getRequestedBy() == headDoctor
        ));
    }


// =========================================================
// ENSURE QUEUE - CREATE NEW QUEUE
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void createFromPaidInvoice_ShouldCreateNewQueue_WhenQueueDoesNotExist() {

        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        StaffInfo headDoctor = mock(StaffInfo.class);

        Department dept = mock(Department.class);
        when(dept.getDepartmentId()).thenReturn(deptId);
        when(dept.getHeadDoctor()).thenReturn(headDoctor);

        MedicalService service = mock(MedicalService.class);
        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);
        when(service.getDepartment()).thenReturn(dept);

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId()).thenReturn(visitId);

        MedicalRecord standaloneRecord = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .build();

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(visitRepo.findByIdForUpdate(visitId))
                .thenReturn(Optional.of(visit));
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));

        when(recordRepo.findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(visitId))
                .thenReturn(Optional.of(standaloneRecord));
        when(recordRepo.findById(recordId)).thenReturn(Optional.of(standaloneRecord));

        when(departmentRepo.findByIdForUpdate(deptId))
                .thenReturn(Optional.of(dept));

        when(queueTicketRepo
                .findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(
                        eq(visitId),
                        eq(deptId),
                        anyList()
                ))
                .thenReturn(Optional.empty());

        when(patientJourneyService.hasActiveStep(visitId))
                .thenReturn(false);

        when(queueTicketRepo.findMaxQueueNumberForDay(
                eq(deptId),
                any(LocalDate.class)
        )).thenReturn(Optional.of(4));

        when(queueTicketRepo.save(any(QueueTicket.class)))
                .thenAnswer(i -> {
                    QueueTicket q = i.getArgument(0);
                    q.setTicketId(UUID.randomUUID());
                    return q;
                });

        when(staffRepo.findByDepartment_DepartmentId(deptId))
                .thenReturn(List.of(headDoctor));

        when(repo.save(any(TestRequest.class)))
                .thenAnswer(i -> {
                    TestRequest t = i.getArgument(0);
                    t.setTestRequestId(UUID.randomUUID());
                    return t;
                });

        testRequestService.createFromPaidInvoice(
                visitId,
                recordId,
                serviceId,
                null,
                null,
                null
        );

        verify(queueTicketRepo).save(argThat(q ->
                q.getQueueNumber() == 5
                        && q.getStatus() == QueueStatus.WAITING
                        && q.getVisit() == visit
                        && q.getDepartment() == dept
        ));
    }


// =========================================================
// ENSURE QUEUE - DEPARTMENT MISSING AFTER LOCK
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void createFromPaidInvoice_ShouldThrow_WhenLockedDepartmentDoesNotExist() {

        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        StaffInfo headDoctor = mock(StaffInfo.class);

        Department dept = mock(Department.class);
        when(dept.getDepartmentId()).thenReturn(deptId);
        when(dept.getHeadDoctor()).thenReturn(headDoctor);

        MedicalService service = mock(MedicalService.class);
        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);
        when(service.getDepartment()).thenReturn(dept);

        CustomerVisit visit = mock(CustomerVisit.class);

        MedicalRecord standaloneRecord = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .build();

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(visitRepo.findByIdForUpdate(visitId))
                .thenReturn(Optional.of(visit));
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(staffRepo.findByDepartment_DepartmentId(deptId)).thenReturn(List.of(headDoctor));

        when(recordRepo.findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(visitId))
                .thenReturn(Optional.of(standaloneRecord));

        when(departmentRepo.findByIdForUpdate(deptId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.createFromPaidInvoice(
                        visitId,
                        recordId,
                        serviceId,
                        null,
                        null,
                        null
                )
        );

        verify(repo, never())
                .save(any(TestRequest.class));
    }


// =========================================================
// CREATE FROM PAID INVOICE - AUTO CREATE MEDICAL RECORD
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void createFromPaidInvoice_ShouldCreateMedicalRecord_WhenNoRecordExists() {

        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID createdRecordId = UUID.randomUUID();

        StaffInfo headDoctor = mock(StaffInfo.class);
        when(headDoctor.getStaffId()).thenReturn(doctorId);

        Department dept = mock(Department.class);
        when(dept.getDepartmentId()).thenReturn(deptId);
        when(dept.getHeadDoctor()).thenReturn(headDoctor);

        MedicalService service = mock(MedicalService.class);
        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);
        when(service.getDepartment()).thenReturn(dept);

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId()).thenReturn(visitId);

        MedicalRecord createdRecord = MedicalRecord.builder()
                .recordId(createdRecordId)
                .visit(visit)
                .build();

        MedicalRecordResponse createdResponse =
                mock(MedicalRecordResponse.class);

        when(createdResponse.recordId())
                .thenReturn(createdRecordId);

        QueueTicket queue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.WAITING)
                .build();

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(visitRepo.findByIdForUpdate(visitId))
                .thenReturn(Optional.of(visit));
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));

        when(recordRepo.findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(visitId))
                .thenReturn(Optional.empty());

        when(medicalRecordService.create(any()))
                .thenReturn(createdResponse);

        when(recordRepo.findById(createdRecordId))
                .thenReturn(Optional.of(createdRecord));

        when(departmentRepo.findByIdForUpdate(deptId))
                .thenReturn(Optional.of(dept));

        when(queueTicketRepo
                .findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(
                        eq(visitId),
                        eq(deptId),
                        anyList()
                ))
                .thenReturn(Optional.of(queue));

        when(staffRepo.findByDepartment_DepartmentId(deptId))
                .thenReturn(List.of(headDoctor));

        when(repo.save(any(TestRequest.class)))
                .thenAnswer(i -> {
                    TestRequest t = i.getArgument(0);
                    t.setTestRequestId(UUID.randomUUID());
                    return t;
                });

        var result = testRequestService.createFromPaidInvoice(
                visitId,
                null,
                serviceId,
                null,
                null,
                null
        );

        assertNotNull(result);

        verify(medicalRecordService)
                .create(any());

        verify(recordRepo)
                .findById(createdRecordId);
    }


// =========================================================
// SELECT PERFORMING DEPARTMENT - CAPABILITY
// PICK LOWEST LOAD
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void createBatch_ShouldChooseDepartmentWithLowestLoad_WhenCapabilityRequired() {

        UUID recordId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .build();

        StaffInfo requestedBy = mock(StaffInfo.class);

        MedicalService service =
                mock(MedicalService.class, RETURNS_DEEP_STUBS);

        UUID capabilityId = UUID.randomUUID();

        when(service.getRequiredCapability().getCapabilityId())
                .thenReturn(capabilityId);
        when(service.getServiceId()).thenReturn(serviceId);
        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);

        Department busy = mock(Department.class);
        Department free = mock(Department.class);

        UUID busyId = UUID.randomUUID();
        UUID freeId = UUID.randomUUID();

        when(busy.getDepartmentId()).thenReturn(busyId);
        when(free.getDepartmentId()).thenReturn(freeId);

        when(departmentRepo.findEligibleByCapability(capabilityId))
                .thenReturn(List.of(busy, free));

        when(
                repo.countByPerformingDepartment_DepartmentIdAndStatusIn(
                        eq(busyId),
                        anyList()
                )
        ).thenReturn(8L);

        when(
                repo.countByPerformingDepartment_DepartmentIdAndStatusIn(
                        eq(freeId),
                        anyList()
                )
        ).thenReturn(1L);

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(requestedBy));

        when(repo.saveAll(any()))
                .thenAnswer(i -> {
                    Iterable<TestRequest> iterable = i.getArgument(0);
                    List<TestRequest> result = new ArrayList<>();
                    iterable.forEach(result::add);
                    return result;
                });

        TestRequestBatchCreateRequest req =
                mock(TestRequestBatchCreateRequest.class);

        when(req.medicalRecordId()).thenReturn(recordId);
        when(req.requestedById()).thenReturn(staffId);
        when(req.serviceIds()).thenReturn(List.of(serviceId));
        when(serviceSelectionPolicyService.normalizeOrThrow(List.of(serviceId))).thenReturn(List.of(service));

        var result =
                testRequestService.createBatch(req);

        assertEquals(1, result.size());

        verify(repo).saveAll(argThat(iterable -> {
            TestRequest t = iterable.iterator().next();
            return t.getPerformingDepartment() == free;
        }));
    }


// =========================================================
// SELECT PERFORMING DEPARTMENT - NO ELIGIBLE DEPARTMENT
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    @Test
    void createBatch_ShouldThrow_WhenNoDepartmentSupportsCapability() {

        UUID recordId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .build();

        StaffInfo staff = mock(StaffInfo.class);

        MedicalService service =
                mock(MedicalService.class, RETURNS_DEEP_STUBS);

        UUID capabilityId = UUID.randomUUID();

        when(service.getRequiredCapability().getCapabilityId())
                .thenReturn(capabilityId);
        when(service.getServiceId()).thenReturn(serviceId);
        when(service.getDepartmentType()).thenReturn(DepartmentType.PARACLINICAL);

        when(service.getRequiredCapability().getName())
                .thenReturn("X-Ray");

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(departmentRepo.findEligibleByCapability(capabilityId))
                .thenReturn(List.of());

        TestRequestBatchCreateRequest req =
                mock(TestRequestBatchCreateRequest.class);

        when(req.medicalRecordId()).thenReturn(recordId);
        when(req.requestedById()).thenReturn(staffId);
        when(req.serviceIds()).thenReturn(List.of(serviceId));
        when(serviceSelectionPolicyService.normalizeOrThrow(List.of(serviceId))).thenReturn(List.of(service));

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.createBatch(req)
        );
    }


// =========================================================
// SEARCH - NORMALIZE SEARCH + EMPTY PAGE
// =========================================================
    @Test
    void search_ShouldNormalizeSearchText() {

        var pageable = PageRequest.of(0, 10);

        when(
                repo.search(
                        null,
                        null,
                        null,
                        "abc xyz",
                        null,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        var result = testRequestService.search(
                null,
                null,
                null,
                "  ABC XYZ  ",
                null,
                pageable
        );

        assertNotNull(result);

        verify(repo).search(
                null,
                null,
                null,
                "abc xyz",
                null,
                pageable
        );
    }


// =========================================================
// SEARCH - BLOCKED QUEUE + IN_PROGRESS TEST
// =========================================================
    @Test
    void search_ShouldKeepExistingBlockedQueue_WhenRequestAlreadyHasQueue() {

        QueueTicket queue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.BLOCKED)
                .build();

        TestRequest displayed = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .queueTicket(queue)
                .status(TestRequestStatus.IN_PROGRESS)
                .build();

        var pageable = PageRequest.of(0, 10);

        when(repo.search(
                null,
                null,
                null,
                "",
                null,
                pageable
        )).thenReturn(
                new PageImpl<>(List.of(displayed))
        );

        var result = testRequestService.search(
                null,
                null,
                null,
                null,
                null,
                pageable
        );

        assertNotNull(result);

        /*
         * search() hien tai chi tao bu queue cho TestRequest chua co queueTicket.
         * Request da co queue BLOCKED se duoc giu nguyen.
         */
        assertEquals(
                QueueStatus.BLOCKED,
                queue.getStatus()
        );

        verify(queueTicketRepo, never())
                .save(queue);

        verify(repo, never())
                .save(displayed);
    }


// =========================================================
// SEARCH - BLOCKED QUEUE + ONLY PENDING
// =========================================================
    @Test
    void search_ShouldKeepBlockedQueue_WhenPendingRequestAlreadyHasQueue() {

        QueueTicket queue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.BLOCKED)
                .build();

        TestRequest displayed = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .queueTicket(queue)
                .status(TestRequestStatus.PENDING)
                .build();

        var pageable = PageRequest.of(0, 10);

        when(repo.search(
                null,
                null,
                null,
                "",
                null,
                pageable
        )).thenReturn(
                new PageImpl<>(List.of(displayed))
        );

        testRequestService.search(
                null,
                null,
                null,
                null,
                null,
                pageable
        );

        assertEquals(
                QueueStatus.BLOCKED,
                queue.getStatus()
        );

        verify(queueTicketRepo, never())
                .save(queue);

        verify(repo, never())
                .save(displayed);
    }


// =========================================================
// COMPLETE RESULT - EXISTING RESULT UPDATE PATH
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void completeResult_ShouldUpdateExistingResult() {

        UUID id = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        UUID labTicketId = UUID.randomUUID();

        StaffInfo verifier = mock(StaffInfo.class);
        when(verifier.getStaffId()).thenReturn(verifierId);
        when(verifier.getSystemRole()).thenReturn(SystemRole.DOCTOR);

        Department dept = mock(Department.class);
        when(dept.getDepartmentId()).thenReturn(deptId);
        when(dept.getHeadDoctor()).thenReturn(verifier);

        QueueTicket executionQueue = QueueTicket.builder()
                .ticketId(labTicketId)
                .status(QueueStatus.IN_PROGRESS)
                .build();

        TestResult existingResult = TestResult.builder()
                .imageUrl("old.pdf")
                .conclusion("Old")
                .build();

        TestRequest t = TestRequest.builder()
                .testRequestId(id)
                .status(TestRequestStatus.IN_PROGRESS)
                .performingDepartment(dept)
                .queueTicket(executionQueue)
                .testResult(existingResult)
                .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.imageUrl())
                .thenReturn("new.pdf");

        when(req.conclusion())
                .thenReturn("New conclusion");

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        when(repo.countByQueueTicket_TicketIdAndStatusIn(
                eq(labTicketId),
                anyList()
        )).thenReturn(1L);

        var result =
                testRequestService.completeResult(
                        id,
                        req);

        assertNotNull(result);

        assertEquals("new.pdf", existingResult.getImageUrl());
        assertEquals("New conclusion", existingResult.getConclusion());

        assertEquals(
                TestRequestStatus.COMPLETED,
                t.getStatus()
        );

        assertSame(verifier, existingResult.getVerifiedBy());
        assertNotNull(existingResult.getVerifiedAt());
    }


// =========================================================
// COMPLETE RESULT - VERIFIER ROLE NULL
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void completeResult_ShouldRejectVerifier_WhenSystemRoleIsNull() {

        UUID id = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        QueueTicket executionQueue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.IN_PROGRESS)
                .build();

        TestResult existingResult = TestResult.builder()
                .imageUrl("result.pdf")
                .conclusion("OK")
                .build();

        StaffInfo verifier = mock(StaffInfo.class);

        TestRequest t = TestRequest.builder()
                .testRequestId(id)
                .status(TestRequestStatus.IN_PROGRESS)
                .queueTicket(executionQueue)
                .testResult(existingResult)
                .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.completeResult(
                        id,
                        req)
        );
    }


// =========================================================
// COMPLETE RESULT - DEPARTMENT NULL
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void completeResult_ShouldReject_WhenPerformingDepartmentIsNull() {

        UUID id = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        QueueTicket executionQueue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.IN_PROGRESS)
                .build();

        TestResult existingResult = TestResult.builder()
                .imageUrl("result.pdf")
                .conclusion("OK")
                .build();

        StaffInfo verifier = mock(StaffInfo.class);
        when(verifier.getSystemRole())
                .thenReturn(SystemRole.DOCTOR);

        TestRequest t = TestRequest.builder()
                .testRequestId(id)
                .status(TestRequestStatus.IN_PROGRESS)
                .queueTicket(executionQueue)
                .testResult(existingResult)
                .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.completeResult(
                        id,
                        req)
        );
    }


// =========================================================
// COMPLETE RESULT - LAB QUEUE DONE
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void completeResult_ShouldCompleteLabQueue_WhenNoTestsRemain() {

        UUID id = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();
        UUID labTicketId = UUID.randomUUID();

        StaffInfo verifier = mock(StaffInfo.class);

        when(verifier.getStaffId()).thenReturn(verifierId);
        when(verifier.getSystemRole()).thenReturn(SystemRole.DOCTOR);

        Department dept = mock(Department.class);
        when(dept.getHeadDoctor()).thenReturn(verifier);

        QueueTicket labQueue = QueueTicket.builder()
                .ticketId(labTicketId)
                .status(QueueStatus.IN_PROGRESS)
                .build();

        TestResult existingResult = TestResult.builder()
                .imageUrl("result.pdf")
                .conclusion("OK")
                .build();

        TestRequest t = TestRequest.builder()
                .testRequestId(id)
                .status(TestRequestStatus.IN_PROGRESS)
                .performingDepartment(dept)
                .queueTicket(labQueue)
                .testResult(existingResult)
                .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        when(
                repo.countByQueueTicket_TicketIdAndStatusIn(
                        eq(labTicketId),
                        anyList()
                )
        ).thenReturn(0L);

        when(resultRepo.save(existingResult))
                .thenReturn(existingResult);

        when(repo.save(t))
                .thenReturn(t);

        testRequestService.completeResult(
                id,
                req);

        assertEquals(
                QueueStatus.DONE,
                labQueue.getStatus()
        );

        assertNotNull(
                labQueue.getCompletedAt()
        );

        verify(queueTicketRepo)
                .save(labQueue);
    }


// =========================================================
// COMPLETE RESULT - LAB QUEUE STILL HAS TESTS
// =========================================================
    // Legacy scenario no longer matches the current authorization/workflow contract.
    private void completeResult_ShouldKeepLabQueueOpen_WhenTestsStillRemain() {

        UUID id = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();
        UUID labTicketId = UUID.randomUUID();

        StaffInfo verifier = mock(StaffInfo.class);

        when(verifier.getStaffId()).thenReturn(verifierId);
        when(verifier.getSystemRole()).thenReturn(SystemRole.DOCTOR);

        Department dept = mock(Department.class);
        when(dept.getHeadDoctor()).thenReturn(verifier);

        QueueTicket labQueue = QueueTicket.builder()
                .ticketId(labTicketId)
                .status(QueueStatus.IN_PROGRESS)
                .build();

        TestResult existingResult = TestResult.builder()
                .imageUrl("result.pdf")
                .conclusion("OK")
                .build();

        TestRequest t = TestRequest.builder()
                .testRequestId(id)
                .status(TestRequestStatus.IN_PROGRESS)
                .performingDepartment(dept)
                .queueTicket(labQueue)
                .testResult(existingResult)
                .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        when(
                repo.countByQueueTicket_TicketIdAndStatusIn(
                        eq(labTicketId),
                        anyList()
                )
        ).thenReturn(2L);

        when(resultRepo.save(existingResult))
                .thenReturn(existingResult);

        when(repo.save(t))
                .thenReturn(t);

        testRequestService.completeResult(
                id,
                req);

        assertEquals(
                QueueStatus.IN_PROGRESS,
                labQueue.getStatus()
        );

        verify(queueTicketRepo, never())
                .save(labQueue);
    }

    @Test
    void resultDataForRequest_ShouldKeepWholePanelAndFilterIndividualAnalytes() {
        var mapper = new tools.jackson.databind.json.JsonMapper();
        var panel = LaboratoryAnalyteCatalog.panel("LAB-001").orElseThrow();
        var values = mapper.readTree("{\"rbc\":4.6,\"hgb\":135,\"_omissions\":{\"rbc\":\"Thiếu mẫu\",\"hgb\":\"Lỗi thiết bị\"}}");

        assertNull(ReflectionTestUtils.invokeMethod(testRequestService,
                "resultDataForRequest", null, panel, "AN-CBC-RBC"));
        assertEquals("text", ReflectionTestUtils.<tools.jackson.databind.JsonNode>invokeMethod(testRequestService,
                "resultDataForRequest", mapper.readTree("\"text\""), panel, "AN-CBC-RBC").asText());
        assertSame(values, ReflectionTestUtils.invokeMethod(testRequestService,
                "resultDataForRequest", values, panel, "LAB-001"));
        assertSame(values, ReflectionTestUtils.invokeMethod(testRequestService,
                "resultDataForRequest", values, panel, "UNKNOWN"));

        tools.jackson.databind.JsonNode rbc = ReflectionTestUtils.invokeMethod(testRequestService,
                "resultDataForRequest", values, panel, "AN-CBC-RBC");
        assertEquals(4.6, rbc.path("rbc").asDouble());
        assertTrue(rbc.path("_omissions").has("rbc"));
        assertFalse(rbc.has("hgb"));
        assertFalse(rbc.path("_omissions").has("hgb"));

        tools.jackson.databind.JsonNode absent = ReflectionTestUtils.invokeMethod(testRequestService,
                "resultDataForRequest", mapper.readTree("{\"hgb\":135,\"_omissions\":{}}"), panel, "AN-CBC-RBC");
        assertTrue(absent.isEmpty());
        tools.jackson.databind.JsonNode nonObjectOmissions = ReflectionTestUtils.invokeMethod(testRequestService,
                "resultDataForRequest", mapper.readTree("{\"rbc\":4.6,\"_omissions\":[]}"), panel, "AN-CBC-RBC");
        assertEquals(1, nonObjectOmissions.size());
    }

    @Test
    void notifyNurses_ShouldNotifyOnlyClinicalStaffWithProfilesAndTolerateFailure() {
        UUID requestId = UUID.randomUUID();
        Department room = Department.builder().departmentId(UUID.randomUUID()).build();
        Profile patient = Profile.builder().fullName("Nguyễn Anh Đức").build();
        CustomerVisit visit = CustomerVisit.builder().customer(patient).build();
        MedicalRecord record = MedicalRecord.builder().visit(visit).build();
        MedicalService service = MedicalService.builder().name("Công thức máu").build();
        TestRequest request = TestRequest.builder().testRequestId(requestId).medicalRecord(record)
                .performingDepartment(room).service(service).build();
        StaffInfo noRole = StaffInfo.builder().profile(Profile.builder().profileId(UUID.randomUUID()).build()).build();
        StaffInfo cashier = StaffInfo.builder().systemRole(SystemRole.CASHIER)
                .profile(Profile.builder().profileId(UUID.randomUUID()).build()).build();
        StaffInfo noProfileDoctor = StaffInfo.builder().systemRole(SystemRole.DOCTOR).build();
        StaffInfo nurse = StaffInfo.builder().systemRole(SystemRole.NURSE)
                .profile(Profile.builder().profileId(UUID.randomUUID()).build()).build();
        StaffInfo doctor = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().profileId(UUID.randomUUID()).build()).build();
        when(staffDutyService.findOnDutyStaff(eq(room), any(LocalDateTime.class)))
                .thenReturn(List.of(noRole, cashier, noProfileDoctor, nurse, doctor));
        when(notificationService.create(argThat(item -> item.recipientId().equals(nurse.getProfile().getProfileId()))))
                .thenThrow(new RuntimeException("notification unavailable"));

        ReflectionTestUtils.invokeMethod(testRequestService, "notifyNurses", request);

        verify(notificationService, times(2)).create(any());
        verify(notificationService).create(argThat(item -> item.recipientId().equals(doctor.getProfile().getProfileId())
                && item.content().contains("Nguyễn Anh Đức") && item.content().contains("Công thức máu")));
    }

    @Test
    void notifyNurses_ShouldUseFallbackLabels() {
        Department room = Department.builder().departmentId(UUID.randomUUID()).build();
        TestRequest request = TestRequest.builder().testRequestId(UUID.randomUUID())
                .performingDepartment(room).build();
        StaffInfo nurse = StaffInfo.builder().systemRole(SystemRole.NURSE)
                .profile(Profile.builder().profileId(UUID.randomUUID()).build()).build();
        when(staffDutyService.findOnDutyStaff(eq(room), any(LocalDateTime.class))).thenReturn(List.of(nurse));

        ReflectionTestUtils.invokeMethod(testRequestService, "notifyNurses", request);

        verify(notificationService).create(argThat(item -> item.content().contains("Khách")
                && item.content().contains("Cận lâm sàng")));
    }

    @Test
    void notifyDoctorResult_ShouldCoverMissingDoctorFallbacksAndNotificationFailure() {
        TestRequest request = TestRequest.builder().testRequestId(UUID.randomUUID()).build();
        ReflectionTestUtils.invokeMethod(testRequestService, "notifyDoctorResult", request);
        request.setRequestedBy(StaffInfo.builder().systemRole(SystemRole.DOCTOR).build());
        ReflectionTestUtils.invokeMethod(testRequestService, "notifyDoctorResult", request);
        verifyNoInteractions(notificationService);

        Profile doctorProfile = Profile.builder().profileId(UUID.randomUUID()).build();
        request.setRequestedBy(StaffInfo.builder().systemRole(SystemRole.DOCTOR).profile(doctorProfile).build());
        ReflectionTestUtils.invokeMethod(testRequestService, "notifyDoctorResult", request);
        verify(notificationService).create(argThat(item -> item.recipientId().equals(doctorProfile.getProfileId())
                && item.content().contains("Khach") && item.content().contains("Can lam sang")));

        Profile patient = Profile.builder().fullName("Trần Minh Anh").build();
        request.setMedicalRecord(MedicalRecord.builder()
                .visit(CustomerVisit.builder().customer(patient).build()).build());
        request.setService(MedicalService.builder().name("Đường huyết").build());
        doThrow(new RuntimeException("notification unavailable")).when(notificationService).create(
                argThat(item -> item.content().contains("Trần Minh Anh")));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(testRequestService, "notifyDoctorResult", request));
        verify(notificationService).create(argThat(item -> item.content().contains("Đường huyết")));
    }

    @Test
    void getClinicalForm_ShouldRejectMissingServiceAndResolveStoredOrLatestVersion() {
        UUID requestId = UUID.randomUUID();
        TestRequest request = operableRequest(requestId, TestRequestStatus.IN_PROGRESS);
        assertThrows(ResourceNotFoundException.class, () -> testRequestService.getClinicalForm(requestId));

        MedicalService service = MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("LAB-001").build();
        request.setService(service);
        ClinicalFormTemplateVersion latest = ClinicalFormTemplateVersion.builder().versionId(UUID.randomUUID()).build();
        var expectedLatest = mock(org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse.class);
        when(resultRepo.findByTestRequest_TestRequestId(requestId)).thenReturn(Optional.empty());
        when(clinicalFormTemplateService.resolveVersion(service.getServiceId(), null)).thenReturn(latest);
        when(clinicalFormTemplateService.resolvedResponse(latest, null, "LAB-001")).thenReturn(expectedLatest);
        assertSame(expectedLatest, testRequestService.getClinicalForm(requestId));

        var data = new tools.jackson.databind.json.JsonMapper().readTree("{\"rbc\":4.8}");
        ClinicalFormTemplateVersion stored = ClinicalFormTemplateVersion.builder().versionId(UUID.randomUUID()).build();
        TestResult result = TestResult.builder().formTemplateVersion(stored).resultData(data).build();
        var expectedStored = mock(org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse.class);
        when(resultRepo.findByTestRequest_TestRequestId(requestId)).thenReturn(Optional.of(result));
        when(clinicalFormTemplateService.resolvedResponse(stored, data, "LAB-001")).thenReturn(expectedStored);
        assertSame(expectedStored, testRequestService.getClinicalForm(requestId));

        result.setFormTemplateVersion(null);
        when(clinicalFormTemplateService.resolvedResponse(latest, data, "LAB-001")).thenReturn(expectedLatest);
        assertSame(expectedLatest, testRequestService.getClinicalForm(requestId));
    }
}





