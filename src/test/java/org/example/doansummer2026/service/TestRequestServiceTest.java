package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.testRequest.TestRequestBatchCreateRequest;
import org.example.doansummer2026.dto.testRequest.TestRequestCancelRequest;
import org.example.doansummer2026.dto.testRequest.TestRequestCreateRequest;
import org.example.doansummer2026.dto.testRequest.TestRequestUpdateRequest;
import org.example.doansummer2026.dto.testResult.TestResultCreateRequest;
import org.example.doansummer2026.dto.testResult.TestResultUpdateRequest;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
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
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
@ExtendWith(MockitoExtension.class)
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

    @InjectMocks
    private TestRequestService testRequestService;


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

    @Test
    void delete_ShouldDelete_WhenRequestExists() {

        UUID id = UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(true);

        testRequestService.delete(id);

        verify(repo).deleteById(id);
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

        TestRequest testRequest =
                mock(TestRequest.class);

        TestRequestCancelRequest request =
                mock(TestRequestCancelRequest.class);

        when(testRequest.getStatus())
                .thenReturn(TestRequestStatus.COMPLETED);

        when(repo.findById(id))
                .thenReturn(Optional.of(testRequest));

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

        TestRequest testRequest =
                mock(TestRequest.class);

        TestRequestCancelRequest request =
                mock(TestRequestCancelRequest.class);

        when(testRequest.getStatus())
                .thenReturn(TestRequestStatus.CANCELLED);

        when(repo.findById(id))
                .thenReturn(Optional.of(testRequest));

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

        TestRequest testRequest =
                mock(TestRequest.class);

        TestRequestCancelRequest request =
                mock(TestRequestCancelRequest.class);

        when(testRequest.getStatus())
                .thenReturn(TestRequestStatus.PENDING);

        when(request.reason())
                .thenReturn("Benh nhan khong thuc hien");

        when(repo.findById(id))
                .thenReturn(Optional.of(testRequest));

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

        verify(testRequest)
                .setStatus(TestRequestStatus.CANCELLED);

        verify(testRequest)
                .setCancelReason("Benh nhan khong thuc hien");

        verify(repo)
                .save(testRequest);
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

        TestRequest t = mock(TestRequest.class);

        when(t.getStatus())
                .thenReturn(TestRequestStatus.COMPLETED);

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

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

        TestRequest t = mock(TestRequest.class);
        TestResult oldResult = mock(TestResult.class);

        when(t.getStatus())
                .thenReturn(TestRequestStatus.PENDING);

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

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

        TestRequest t = mock(TestRequest.class);

        when(t.getStatus())
                .thenReturn(TestRequestStatus.PENDING);

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

        when(resultRepo.findByTestRequest_TestRequestId(id))
                .thenReturn(Optional.empty());

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.performedById())
                .thenReturn(staffId);

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

        TestRequest t = TestRequest.builder()
                .testRequestId(id)
                .status(TestRequestStatus.PENDING)
                .build();

        StaffInfo staff = mock(StaffInfo.class);

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.performedById())
                .thenReturn(staffId);

        when(req.imageUrl())
                .thenReturn("/uploads/a.pdf");

        when(req.conclusion())
                .thenReturn("Binh thuong");

        when(req.sampleId())
                .thenReturn("SAMPLE-001");

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

        when(resultRepo.findByTestRequest_TestRequestId(id))
                .thenReturn(Optional.empty());

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(resultRepo.save(any(TestResult.class)))
                .thenAnswer(i -> i.getArgument(0));

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

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .build();

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

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

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.COMPLETED)
                        .build();

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

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

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .build();

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

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

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .build();

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

        when(req.sampleId())
                .thenReturn("NEW");

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

        when(resultRepo.findByTestRequest_TestRequestId(id))
                .thenReturn(Optional.of(r));

        when(resultRepo.save(r))
                .thenReturn(r);

        var result =
                testRequestService.updateResult(id, req);

        assertNotNull(result);

        assertEquals("new.pdf", r.getImageUrl());
        assertEquals("New conclusion", r.getConclusion());
        assertEquals("NEW", r.getSampleId());

        verify(resultRepo).save(r);
    }


// =========================================================
// UPDATE TEST REQUEST - SIMPLE STATUS
// =========================================================

    @Test
    void update_ShouldUpdateStatus_WhenStatusProvided() {

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

    @Test
    void update_ShouldSetQueueTestDone_WhenAllTestsCompleted() {

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

    @Test
    void update_ShouldSetQueueWaitingForTest_WhenSomeTestsRemain() {

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

    @Test
    void completeResult_ShouldRejectAlreadyCompletedRequest() {

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
                        mock(TestResultCreateRequest.class),
                        UUID.randomUUID()
                )
        );
    }


// =========================================================
// COMPLETE RESULT - MISSING CONCLUSION
// =========================================================

    @Test
    void completeResult_ShouldRejectBlankConclusion() {

        UUID id = UUID.randomUUID();
        UUID performedById = UUID.randomUUID();

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .build();

        StaffInfo performedBy =
                mock(StaffInfo.class);

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.performedById())
                .thenReturn(performedById);

        when(req.imageUrl())
                .thenReturn("result.pdf");

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(performedById))
                .thenReturn(Optional.of(performedBy));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.completeResult(
                        id,
                        req,
                        UUID.randomUUID()
                )
        );
    }


// =========================================================
// COMPLETE RESULT - INVALID PDF URL
// =========================================================

    @Test
    void completeResult_ShouldRejectNonPdfResult() {

        UUID id = UUID.randomUUID();
        UUID performedById = UUID.randomUUID();

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .build();

        StaffInfo performedBy =
                mock(StaffInfo.class);

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.performedById())
                .thenReturn(performedById);

        when(req.conclusion())
                .thenReturn("Binh thuong");

        when(req.imageUrl())
                .thenReturn("image.jpg");

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(performedById))
                .thenReturn(Optional.of(performedBy));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.completeResult(
                        id,
                        req,
                        UUID.randomUUID()
                )
        );
    }


// =========================================================
// COMPLETE RESULT - VERIFIER NOT FOUND
// =========================================================

    @Test
    void completeResult_ShouldThrow_WhenVerifierDoesNotExist() {

        UUID id = UUID.randomUUID();
        UUID performedById = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .build();

        StaffInfo performedBy =
                mock(StaffInfo.class);

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.performedById())
                .thenReturn(performedById);

        when(req.conclusion())
                .thenReturn("OK");

        when(req.imageUrl())
                .thenReturn("result.pdf");

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(performedById))
                .thenReturn(Optional.of(performedBy));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testRequestService.completeResult(
                        id,
                        req,
                        verifierId
                )
        );
    }


// =========================================================
// COMPLETE RESULT - VERIFIER IS NOT DOCTOR
// =========================================================

    @Test
    void completeResult_ShouldRejectVerifier_WhenNotDoctor() {

        UUID id = UUID.randomUUID();
        UUID performedById = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .build();

        StaffInfo performedBy =
                mock(StaffInfo.class);

        StaffInfo verifier =
                mock(StaffInfo.class);

        when(verifier.getSystemRole())
                .thenReturn(SystemRole.NURSE);

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.performedById())
                .thenReturn(performedById);

        when(req.conclusion())
                .thenReturn("OK");

        when(req.imageUrl())
                .thenReturn("result.pdf");

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(performedById))
                .thenReturn(Optional.of(performedBy));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.completeResult(
                        id,
                        req,
                        verifierId
                )
        );
    }


// =========================================================
// COMPLETE RESULT - DOCTOR NOT HEAD OF DEPARTMENT
// =========================================================

    @Test
    void completeResult_ShouldRejectDoctor_WhenNotDepartmentHead() {

        UUID id = UUID.randomUUID();
        UUID performedById = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        StaffInfo performedBy =
                mock(StaffInfo.class);

        StaffInfo verifier =
                mock(StaffInfo.class);

        StaffInfo headDoctor =
                mock(StaffInfo.class);

        when(verifier.getSystemRole())
                .thenReturn(SystemRole.DOCTOR);

        when(verifier.getStaffId())
                .thenReturn(verifierId);

        when(headDoctor.getStaffId())
                .thenReturn(UUID.randomUUID());

        Department dept =
                mock(Department.class);

        when(dept.getHeadDoctor())
                .thenReturn(headDoctor);

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .performingDepartment(dept)
                        .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.performedById())
                .thenReturn(performedById);

        when(req.conclusion())
                .thenReturn("OK");

        when(req.imageUrl())
                .thenReturn("result.pdf");

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(performedById))
                .thenReturn(Optional.of(performedBy));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.completeResult(
                        id,
                        req,
                        verifierId
                )
        );
    }


// =========================================================
// COMPLETE RESULT - SUCCESS
// =========================================================

    @Test
    void completeResult_ShouldCompleteRequest_WhenVerifierIsDepartmentHead() {

        UUID id = UUID.randomUUID();
        UUID performedById = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        StaffInfo performedBy =
                mock(StaffInfo.class);

        StaffInfo verifier =
                mock(StaffInfo.class);

        when(verifier.getSystemRole())
                .thenReturn(SystemRole.DOCTOR);

        when(verifier.getStaffId())
                .thenReturn(verifierId);

        Department dept =
                mock(Department.class);

        when(dept.getHeadDoctor())
                .thenReturn(verifier);

        TestRequest t =
                TestRequest.builder()
                        .testRequestId(id)
                        .status(TestRequestStatus.IN_PROGRESS)
                        .performingDepartment(dept)
                        .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.performedById())
                .thenReturn(performedById);

        when(req.conclusion())
                .thenReturn("Ket qua binh thuong");

        when(req.imageUrl())
                .thenReturn("/uploads/result.pdf");

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(performedById))
                .thenReturn(Optional.of(performedBy));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        when(resultRepo.save(any(TestResult.class)))
                .thenAnswer(i -> i.getArgument(0));

        when(repo.save(t))
                .thenReturn(t);

        var result =
                testRequestService.completeResult(
                        id,
                        req,
                        verifierId
                );

        assertNotNull(result);

        assertEquals(
                TestRequestStatus.COMPLETED,
                t.getStatus()
        );

        assertNotNull(
                t.getCompletedAt()
        );

        verify(resultRepo)
                .save(argThat(r ->
                        r.getVerifiedBy() == verifier
                                && r.getVerifiedAt() != null
                ));

        verify(repo).save(t);
    }


// =========================================================
// UPLOAD RESULT FILE
// =========================================================

    @Test
    void uploadResultFile_ShouldRejectNullFile() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.of(mock(TestRequest.class)));

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

        when(repo.findById(id))
                .thenReturn(Optional.of(mock(TestRequest.class)));

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

        when(repo.findById(id))
                .thenReturn(Optional.of(mock(TestRequest.class)));

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

        when(repo.findById(id))
                .thenReturn(Optional.of(mock(TestRequest.class)));

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

        when(repo.findById(id))
                .thenReturn(Optional.of(mock(TestRequest.class)));

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

        when(repo.findById(id))
                .thenReturn(Optional.of(mock(TestRequest.class)));

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

    @Test
    void createBatch_ShouldSkipServicesThatAlreadyExist() {

        UUID recordId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        UUID existingServiceId = UUID.randomUUID();

        MedicalService existingService =
                mock(MedicalService.class);

        when(existingService.getServiceId())
                .thenReturn(existingServiceId);

        TestRequest existingRequest =
                TestRequest.builder()
                        .service(existingService)
                        .build();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .testRequests(
                                new LinkedHashSet<>(
                                        List.of(existingRequest)
                                )
                        )
                        .build();

        StaffInfo staff =
                mock(StaffInfo.class);

        TestRequestBatchCreateRequest req =
                mock(TestRequestBatchCreateRequest.class);

        when(req.medicalRecordId())
                .thenReturn(recordId);

        when(req.requestedById())
                .thenReturn(staffId);

        when(req.serviceIds())
                .thenReturn(List.of(existingServiceId));

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(repo.saveAll(anyList()))
                .thenReturn(List.of());

        var result =
                testRequestService.createBatch(req);

        assertTrue(result.isEmpty());

        verify(serviceRepo, never())
                .findById(existingServiceId);

        verify(repo).saveAll(
                argThat(iterable -> !iterable.iterator().hasNext())
        );
    }
    // =========================================================
// COVERAGE BOOST - TEST REQUEST SERVICE
// createFromPaidInvoice / search / queue / notify / completeResult
// =========================================================


// =========================================================
// CREATE FROM PAID INVOICE - DUPLICATE
// =========================================================

    @Test
    void createFromPaidInvoice_ShouldReturnExisting_WhenInvoiceItemAlreadyHasRequest() {

        UUID invoiceItemId = UUID.randomUUID();

        TestRequest existing = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .status(TestRequestStatus.PENDING)
                .build();

        when(repo.findByInvoiceItem_ItemId(invoiceItemId))
                .thenReturn(List.of(existing));

        var result = testRequestService.createFromPaidInvoice(
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                null,
                "notes",
                invoiceItemId
        );

        assertNotNull(result);

        verifyNoInteractions(serviceRepo);
        verify(repo, never()).save(any(TestRequest.class));
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
        when(service.getDepartment()).thenReturn(dept);
        when(service.getName()).thenReturn("Xet nghiem mau");

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId()).thenReturn(visitId);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .build();

        QueueTicket existingQueue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.WAITING)
                .build();

        Profile nurseProfile = mock(Profile.class);
        when(nurseProfile.getProfileId()).thenReturn(UUID.randomUUID());

        StaffInfo nurse = mock(StaffInfo.class);
        when(nurse.getSystemRole()).thenReturn(SystemRole.NURSE);
        when(nurse.getProfile()).thenReturn(nurseProfile);

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(departmentRepo.findByIdForUpdate(deptId))
                .thenReturn(Optional.of(dept));

        when(
                queueTicketRepo
                        .findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(
                                eq(visitId),
                                eq(deptId),
                                anyList()
                        )
        ).thenReturn(Optional.of(existingQueue));

        when(staffRepo.findByDepartment_DepartmentId(deptId))
                .thenReturn(List.of(nurse));

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
                t.getMedicalRecord() == record
                        && t.getService() == service
                        && t.getRequestedBy() == headDoctor
                        && t.getQueueTicket() == existingQueue
                        && t.getStatus() == TestRequestStatus.PENDING
        ));

        verify(notificationService)
                .create(any());
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
        when(service.getDepartment()).thenReturn(dept);

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId()).thenReturn(visitId);

        MedicalRecord record = MedicalRecord.builder()
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

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(departmentRepo.findByIdForUpdate(deptId))
                .thenReturn(Optional.of(dept));

        when(
                queueTicketRepo
                        .findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(
                                eq(visitId),
                                eq(deptId),
                                anyList()
                        )
        ).thenReturn(Optional.of(queue));

        when(staffRepo.findByDepartment_DepartmentId(deptId))
                .thenReturn(List.of());

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
        when(service.getDepartment()).thenReturn(dept);

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId()).thenReturn(visitId);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .build();

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(departmentRepo.findByIdForUpdate(deptId))
                .thenReturn(Optional.of(dept));

        when(
                queueTicketRepo
                        .findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(
                                eq(visitId),
                                eq(deptId),
                                anyList()
                        )
        ).thenReturn(Optional.empty());

        when(
                queueTicketRepo.findMaxQueueNumberForDay(
                        eq(deptId),
                        any(LocalDate.class)
                )
        ).thenReturn(Optional.of(4));

        when(queueTicketRepo.save(any(QueueTicket.class)))
                .thenAnswer(i -> {
                    QueueTicket q = i.getArgument(0);
                    q.setTicketId(UUID.randomUUID());
                    return q;
                });

        when(staffRepo.findByDepartment_DepartmentId(deptId))
                .thenReturn(List.of());

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
        when(service.getDepartment()).thenReturn(dept);

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId()).thenReturn(visitId);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .build();

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

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

        verify(repo, never()).save(any());
    }


// =========================================================
// CREATE FROM PAID INVOICE - AUTO CREATE MEDICAL RECORD
// =========================================================

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

        when(recordRepo.findFirstByVisit_VisitIdOrderByCreatedAtDesc(visitId))
                .thenReturn(Optional.empty());

        when(medicalRecordService.create(any()))
                .thenReturn(createdResponse);

        when(recordRepo.findById(createdRecordId))
                .thenReturn(Optional.of(createdRecord));

        when(departmentRepo.findByIdForUpdate(deptId))
                .thenReturn(Optional.of(dept));

        when(
                queueTicketRepo
                        .findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(
                                eq(visitId),
                                eq(deptId),
                                anyList()
                        )
        ).thenReturn(Optional.of(queue));

        when(staffRepo.findByDepartment_DepartmentId(deptId))
                .thenReturn(List.of());

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
    void search_ShouldReactivateBlockedQueue_WhenGroupedTestIsInProgress() {

        UUID ticketId = UUID.randomUUID();

        QueueTicket queue = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.BLOCKED)
                .build();

        TestRequest displayed = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .queueTicket(queue)
                .status(TestRequestStatus.PENDING)
                .build();

        TestRequest groupedInProgress = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .queueTicket(queue)
                .status(TestRequestStatus.IN_PROGRESS)
                .build();

        TestRequest groupedBlocked = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .queueTicket(queue)
                .status(TestRequestStatus.BLOCKED)
                .build();

        var pageable = PageRequest.of(0, 10);

        when(
                repo.search(
                        null,
                        null,
                        null,
                        "",
                        null,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of(displayed))
        );

        when(repo.findAllByQueueTicket_TicketId(ticketId))
                .thenReturn(
                        List.of(
                                groupedInProgress,
                                groupedBlocked
                        )
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
                QueueStatus.IN_PROGRESS,
                queue.getStatus()
        );

        assertEquals(
                TestRequestStatus.PENDING,
                groupedBlocked.getStatus()
        );

        verify(queueTicketRepo)
                .save(queue);

        verify(repo)
                .save(groupedBlocked);
    }


// =========================================================
// SEARCH - BLOCKED QUEUE + ONLY PENDING
// =========================================================

    @Test
    void search_ShouldSetBlockedQueueWaiting_WhenOnlyPendingTestsAvailable() {

        UUID ticketId = UUID.randomUUID();

        QueueTicket queue = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.BLOCKED)
                .build();

        TestRequest displayed = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .queueTicket(queue)
                .status(TestRequestStatus.PENDING)
                .build();

        TestRequest groupedPending = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .queueTicket(queue)
                .status(TestRequestStatus.PENDING)
                .build();

        var pageable = PageRequest.of(0, 10);

        when(
                repo.search(
                        null,
                        null,
                        null,
                        "",
                        null,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of(displayed))
        );

        when(repo.findAllByQueueTicket_TicketId(ticketId))
                .thenReturn(List.of(groupedPending));

        testRequestService.search(
                null,
                null,
                null,
                null,
                null,
                pageable
        );

        assertEquals(
                QueueStatus.WAITING,
                queue.getStatus()
        );

        verify(queueTicketRepo)
                .save(queue);
    }


// =========================================================
// COMPLETE RESULT - EXISTING RESULT UPDATE PATH
// =========================================================

    @Test
    void completeResult_ShouldUpdateExistingResult() {

        UUID id = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        StaffInfo verifier = mock(StaffInfo.class);

        when(verifier.getStaffId()).thenReturn(verifierId);
        when(verifier.getSystemRole()).thenReturn(SystemRole.DOCTOR);

        Department dept = mock(Department.class);
        when(dept.getHeadDoctor()).thenReturn(verifier);

        TestResult existingResult = TestResult.builder()
                .imageUrl("old.pdf")
                .conclusion("Old")
                .sampleId("OLD")
                .build();

        TestRequest t = TestRequest.builder()
                .testRequestId(id)
                .status(TestRequestStatus.IN_PROGRESS)
                .performingDepartment(dept)
                .testResult(existingResult)
                .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.imageUrl())
                .thenReturn("new.pdf");

        when(req.conclusion())
                .thenReturn("New conclusion");

        when(req.sampleId())
                .thenReturn("NEW");

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        when(resultRepo.save(existingResult))
                .thenReturn(existingResult);

        when(repo.save(t))
                .thenReturn(t);

        var result =
                testRequestService.completeResult(
                        id,
                        req,
                        verifierId
                );

        assertNotNull(result);

        assertEquals("new.pdf", existingResult.getImageUrl());
        assertEquals("New conclusion", existingResult.getConclusion());
        assertEquals("NEW", existingResult.getSampleId());

        assertEquals(
                TestRequestStatus.COMPLETED,
                t.getStatus()
        );
    }


// =========================================================
// COMPLETE RESULT - VERIFIER ROLE NULL
// =========================================================

    @Test
    void completeResult_ShouldRejectVerifier_WhenSystemRoleIsNull() {

        UUID id = UUID.randomUUID();
        UUID performedById = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        StaffInfo performedBy = mock(StaffInfo.class);
        StaffInfo verifier = mock(StaffInfo.class);

        TestRequest t = TestRequest.builder()
                .testRequestId(id)
                .status(TestRequestStatus.IN_PROGRESS)
                .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.performedById()).thenReturn(performedById);
        when(req.conclusion()).thenReturn("OK");
        when(req.imageUrl()).thenReturn("result.pdf");

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(performedById))
                .thenReturn(Optional.of(performedBy));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.completeResult(
                        id,
                        req,
                        verifierId
                )
        );
    }


// =========================================================
// COMPLETE RESULT - DEPARTMENT NULL
// =========================================================

    @Test
    void completeResult_ShouldReject_WhenPerformingDepartmentIsNull() {

        UUID id = UUID.randomUUID();
        UUID performedById = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        StaffInfo performedBy = mock(StaffInfo.class);

        StaffInfo verifier = mock(StaffInfo.class);
        when(verifier.getSystemRole())
                .thenReturn(SystemRole.DOCTOR);

        TestRequest t = TestRequest.builder()
                .testRequestId(id)
                .status(TestRequestStatus.IN_PROGRESS)
                .build();

        TestResultCreateRequest req =
                mock(TestResultCreateRequest.class);

        when(req.performedById()).thenReturn(performedById);
        when(req.conclusion()).thenReturn("OK");
        when(req.imageUrl()).thenReturn("result.pdf");

        when(repo.findByIdWithResult(id))
                .thenReturn(Optional.of(t));

        when(staffRepo.findById(performedById))
                .thenReturn(Optional.of(performedBy));

        when(staffRepo.findById(verifierId))
                .thenReturn(Optional.of(verifier));

        assertThrows(
                BadRequestException.class,
                () -> testRequestService.completeResult(
                        id,
                        req,
                        verifierId
                )
        );
    }


// =========================================================
// COMPLETE RESULT - LAB QUEUE DONE
// =========================================================

    @Test
    void completeResult_ShouldCompleteLabQueue_WhenNoTestsRemain() {

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
                req,
                verifierId
        );

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

    @Test
    void completeResult_ShouldKeepLabQueueOpen_WhenTestsStillRemain() {

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
                req,
                verifierId
        );

        assertEquals(
                QueueStatus.IN_PROGRESS,
                labQueue.getStatus()
        );

        verify(queueTicketRepo, never())
                .save(labQueue);
    }
}