package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.journey.PatientJourneyResponse;
import vn.edu.fpt.cares.dto.journey.PatientQueueResponse;
import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.enums.InvoiceStatus;
import vn.edu.fpt.cares.enums.QueueStatus;
import vn.edu.fpt.cares.enums.TestRequestStatus;
import vn.edu.fpt.cares.enums.VisitStatus;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.Account;
import vn.edu.fpt.cares.model.Appointment;
import vn.edu.fpt.cares.model.CustomerVisit;
import vn.edu.fpt.cares.model.Department;
import vn.edu.fpt.cares.model.Invoice;
import vn.edu.fpt.cares.model.InvoiceItem;
import vn.edu.fpt.cares.model.MedicalService;
import vn.edu.fpt.cares.model.MedicalRecord;
import vn.edu.fpt.cares.model.Profile;
import vn.edu.fpt.cares.model.QueueTicket;
import vn.edu.fpt.cares.model.TestRequest;
import vn.edu.fpt.cares.repository.CustomerVisitRepository;
import vn.edu.fpt.cares.repository.InvoiceRepository;
import vn.edu.fpt.cares.repository.MedicalRecordRepository;
import vn.edu.fpt.cares.repository.QueueTicketRepository;
import vn.edu.fpt.cares.repository.TestRequestRepository;
import vn.edu.fpt.cares.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientJourneyServiceTest {

    @Mock
    private CustomerVisitRepository visitRepo;

    @Mock
    private QueueTicketRepository queueRepo;

    @Mock
    private TestRequestRepository testRepo;

    @Mock
    private InvoiceRepository invoiceRepo;

    @Mock
    private MedicalRecordRepository recordRepo;

    @Mock
    private NotificationRepository notificationRepo;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private QueuePriorityService queuePriorityService;

    @InjectMocks
    private PatientJourneyService patientJourneyService;


    // =========================================================
    // HELPERS
    // =========================================================

    private Department department(
            String name,
            String roomCode
    ) {
        return Department.builder()
                .departmentId(UUID.randomUUID())
                .name(name)
                .roomCode(roomCode)
                .build();
    }


    private Department paraclinicalDepartment(
            String name,
            String roomCode
    ) {
        return Department.builder()
                .departmentId(UUID.randomUUID())
                .name(name)
                .roomCode(roomCode)
                .departmentType(DepartmentType.PARACLINICAL)
                .build();
    }


    private MedicalService medicalService(
            String name
    ) {
        return MedicalService.builder()
                .serviceId(UUID.randomUUID())
                .name(name)
                .build();
    }


    private QueueTicket queue(
            QueueStatus status,
            LocalDateTime createdAt
    ) {
        QueueTicket queue =
                QueueTicket.builder()
                        .ticketId(UUID.randomUUID())
                        .status(status)
                        .queueNumber(1)
                        .department(
                                department(
                                        "Phong kham",
                                        "P101"
                                )
                        )
                        .service(
                                medicalService(
                                        "Kham tong quat"
                                )
                        )
                        .build();

        queue.setCreatedAt(createdAt);

        return queue;
    }


    private QueueTicket paraclinicalQueue(
            QueueStatus status,
            LocalDateTime createdAt
    ) {
        QueueTicket queue =
                QueueTicket.builder()
                        .ticketId(UUID.randomUUID())
                        .status(status)
                        .queueNumber(1)
                        .department(
                                paraclinicalDepartment(
                                        "Phong can lam sang",
                                        "CLS01"
                                )
                        )
                        .service(
                                medicalService(
                                        "Sieu am"
                                )
                        )
                        .build();

        queue.setCreatedAt(createdAt);

        return queue;
    }


    private TestRequest testRequest(
            TestRequestStatus status,
            LocalDateTime createdAt
    ) {
        TestRequest request =
                TestRequest.builder()
                        .testRequestId(UUID.randomUUID())
                        .status(status)
                        .performingDepartment(
                                department(
                                        "Phong xet nghiem",
                                        "LAB01"
                                )
                        )
                        .service(
                                medicalService(
                                        "Xet nghiem mau"
                                )
                        )
                        .build();

        request.setCreatedAt(createdAt);

        return request;
    }


    private CustomerVisit visit(
            UUID visitId
    ) {
        return CustomerVisit.builder()
                .visitId(visitId)
                .checkInTime(
                        LocalDateTime.now()
                                .minusMinutes(30)
                )
                .build();
    }


    private Invoice pendingInvoice(
            LocalDateTime createdAt
    ) {
        Invoice invoice =
                mock(Invoice.class);

        when(invoice.getInvoiceId())
                .thenReturn(
                        UUID.randomUUID()
                );

        when(invoice.getStatus())
                .thenReturn(
                        InvoiceStatus.PENDING
                );

        when(invoice.getCreatedAt())
                .thenReturn(createdAt);

        return invoice;
    }


    @SuppressWarnings("unchecked")
    private List<PatientJourneyResponse> pageContent(
            PageResponse<PatientJourneyResponse> page
    ) {
        for (
                String accessor :
                List.of(
                        "content",
                        "items",
                        "getContent",
                        "getItems"
                )
        ) {
            try {
                Method method =
                        page.getClass()
                                .getMethod(accessor);

                Object value =
                        method.invoke(page);

                if (value instanceof List<?>) {
                    return (List<PatientJourneyResponse>) value;
                }

            } catch (ReflectiveOperationException ignored) {
            }
        }

        fail(
                "Cannot read content/items from PageResponse"
        );

        return List.of();
    }


    // =========================================================
    // HAS ACTIVE STEP
    // =========================================================

    @Test
    void hasActiveStep_ShouldReturnFalse_WhenJourneyHasNoSteps() {

        UUID visitId =
                UUID.randomUUID();

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        assertFalse(
                patientJourneyService
                        .hasActiveStep(
                                visitId
                        )
        );
    }


    @Test
    void hasActiveStep_ShouldReturnTrue_WhenWaitingQueueExists() {

        UUID visitId =
                UUID.randomUUID();

        QueueTicket active =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(active)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        assertTrue(
                patientJourneyService
                        .hasActiveStep(
                                visitId
                        )
        );
    }


    @Test
    void hasActiveStep_ShouldIgnoreBlockedDoneSkippedAndWaitingForTestQueues() {

        UUID visitId =
                UUID.randomUUID();

        LocalDateTime now =
                LocalDateTime.now();

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(
                        queue(
                                QueueStatus.BLOCKED,
                                now
                        ),
                        queue(
                                QueueStatus.DONE,
                                now
                        ),
                        queue(
                                QueueStatus.SKIPPED,
                                now
                        ),
                        queue(
                                QueueStatus.WAITING_FOR_TEST,
                                now
                        )
                )
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        assertFalse(
                patientJourneyService
                        .hasActiveStep(
                                visitId
                        )
        );
    }


    @Test
    void hasActiveStep_ShouldReturnTrue_WhenStandalonePendingTestExists() {

        UUID visitId =
                UUID.randomUUID();

        TestRequest test =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                );

        test.setQueueTicket(null);

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(test)
        );

        assertTrue(
                patientJourneyService
                        .hasActiveStep(
                                visitId
                        )
        );
    }


    @Test
    void hasActiveStep_ShouldReturnTrue_WhenStandaloneTestInProgress() {

        UUID visitId =
                UUID.randomUUID();

        TestRequest test =
                testRequest(
                        TestRequestStatus.IN_PROGRESS,
                        LocalDateTime.now()
                );

        test.setQueueTicket(null);

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(test)
        );

        assertTrue(
                patientJourneyService
                        .hasActiveStep(
                                visitId
                        )
        );
    }


    @Test
    void hasActiveStep_ShouldIgnorePendingTest_WhenItHasQueueTicket() {

        UUID visitId =
                UUID.randomUUID();

        QueueTicket linkedQueue =
                queue(
                        QueueStatus.DONE,
                        LocalDateTime.now()
                );

        TestRequest test =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                );

        test.setQueueTicket(
                linkedQueue
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(linkedQueue)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(test)
        );

        assertFalse(
                patientJourneyService
                        .hasActiveStep(
                                visitId
                        )
        );
    }


    @Test
    void hasActiveStep_ShouldIgnoreCompletedAndBlockedTests() {

        UUID visitId =
                UUID.randomUUID();

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(
                        testRequest(
                                TestRequestStatus.BLOCKED,
                                LocalDateTime.now()
                        ),
                        testRequest(
                                TestRequestStatus.COMPLETED,
                                LocalDateTime.now()
                        )
                )
        );

        assertFalse(
                patientJourneyService
                        .hasActiveStep(
                                visitId
                        )
        );
    }


    // =========================================================
    // ACTIVATE NEXT
    // =========================================================

    @Test
    void activateNext_ShouldThrow_WhenVisitDoesNotExist() {

        UUID visitId =
                UUID.randomUUID();

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        patientJourneyService
                                .activateNext(
                                        visitId
                                )
        );

        verifyNoInteractions(
                queueRepo
        );

        verifyNoInteractions(
                testRepo
        );
    }


    @Test
    void activateNext_ShouldDoNothing_WhenActiveStepAlreadyExists() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket active =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(active)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        patientJourneyService
                .activateNext(
                        visitId
                );

        verify(
                queueRepo,
                never()
        ).save(
                any()
        );

        verify(
                testRepo,
                never()
        ).save(
                any()
        );

        verify(
                visitRepo,
                never()
        ).save(
                any()
        );
    }


    @Test
    void activateNext_ShouldActivateBlockedQueue_WhenOnlyQueueExists() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket blocked =
                queue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(10)
                );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(blocked)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                testRepo.findAllByQueueTicket_TicketId(
                        blocked.getTicketId()
                )
        ).thenReturn(
                List.of()
        );

        patientJourneyService
                .activateNext(
                        visitId
                );

        assertEquals(
                QueueStatus.WAITING,
                blocked.getStatus()
        );

        verify(queueRepo)
                .save(blocked);
    }


    @Test
    void activateNext_ShouldActivateBlockedTestsSharingActivatedQueue() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket blockedQueue =
                queue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(20)
                );

        TestRequest groupedBlocked =
                testRequest(
                        TestRequestStatus.BLOCKED,
                        LocalDateTime.now()
                );

        groupedBlocked.setQueueTicket(
                blockedQueue
        );

        TestRequest groupedCompleted =
                testRequest(
                        TestRequestStatus.COMPLETED,
                        LocalDateTime.now()
                );

        groupedCompleted.setQueueTicket(
                blockedQueue
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(blockedQueue)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                testRepo.findAllByQueueTicket_TicketId(
                        blockedQueue.getTicketId()
                )
        ).thenReturn(
                List.of(
                        groupedBlocked,
                        groupedCompleted
                )
        );

        patientJourneyService
                .activateNext(
                        visitId
                );

        assertEquals(
                QueueStatus.WAITING,
                blockedQueue.getStatus()
        );

        assertEquals(
                TestRequestStatus.PENDING,
                groupedBlocked.getStatus()
        );

        assertEquals(
                TestRequestStatus.COMPLETED,
                groupedCompleted.getStatus()
        );

        verify(testRepo)
                .save(
                        groupedBlocked
                );

        verify(
                testRepo,
                never()
        ).save(
                groupedCompleted
        );
    }


    @Test
    void activateNext_ShouldPreferBlockedQueue_EvenWhenStandaloneTestWasCreatedEarlier() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket blockedQueue =
                queue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(10)
                );

        TestRequest olderStandaloneTest =
                testRequest(
                        TestRequestStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(20)
                );

        olderStandaloneTest.setQueueTicket(
                null
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(blockedQueue)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(
                        olderStandaloneTest
                )
        );

        when(
                testRepo.findAllByQueueTicket_TicketId(
                        blockedQueue.getTicketId()
                )
        ).thenReturn(
                List.of()
        );

        patientJourneyService
                .activateNext(
                        visitId
                );

        assertEquals(
                QueueStatus.WAITING,
                blockedQueue.getStatus()
        );

        assertEquals(
                TestRequestStatus.BLOCKED,
                olderStandaloneTest.getStatus()
        );

        verify(queueRepo)
                .save(
                        blockedQueue
                );

        verify(
                testRepo,
                never()
        ).save(
                olderStandaloneTest
        );
    }


    @Test
    void activateNext_ShouldActivateStandaloneBlockedTest_WhenNoBlockedQueueExists() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        TestRequest test =
                testRequest(
                        TestRequestStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(20)
                );

        test.setQueueTicket(
                null
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(test)
        );

        patientJourneyService
                .activateNext(
                        visitId
                );

        assertEquals(
                TestRequestStatus.PENDING,
                test.getStatus()
        );

        verify(testRepo)
                .save(test);
    }


    @Test
    void activateNext_ShouldIgnoreBlockedTest_WhenItAlreadyHasQueueTicket() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket doneQueue =
                queue(
                        QueueStatus.DONE,
                        LocalDateTime.now()
                );

        TestRequest groupedBlocked =
                testRequest(
                        TestRequestStatus.BLOCKED,
                        LocalDateTime.now()
                );

        groupedBlocked.setQueueTicket(
                doneQueue
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(doneQueue)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(groupedBlocked)
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        patientJourneyService
                .activateNext(
                        visitId
                );

        assertEquals(
                TestRequestStatus.BLOCKED,
                groupedBlocked.getStatus()
        );

        assertEquals(
                VisitStatus.COMPLETED,
                visit.getStatus()
        );

        verify(
                testRepo,
                never()
        ).save(
                groupedBlocked
        );

        verify(visitRepo)
                .save(visit);
    }


    @Test
    void activateNext_ShouldPickOldestBlockedQueue() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket oldQueue =
                queue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(30)
                );

        QueueTicket newQueue =
                queue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(10)
                );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(
                        newQueue,
                        oldQueue
                )
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                testRepo.findAllByQueueTicket_TicketId(
                        oldQueue.getTicketId()
                )
        ).thenReturn(
                List.of()
        );

        patientJourneyService
                .activateNext(
                        visitId
                );

        assertEquals(
                QueueStatus.WAITING,
                oldQueue.getStatus()
        );

        assertEquals(
                QueueStatus.BLOCKED,
                newQueue.getStatus()
        );

        verify(queueRepo)
                .save(
                        oldQueue
                );
    }


    @Test
    void activateNext_ShouldPickOldestStandaloneBlockedTest() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        TestRequest oldTest =
                testRequest(
                        TestRequestStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(30)
                );

        oldTest.setQueueTicket(
                null
        );

        TestRequest newTest =
                testRequest(
                        TestRequestStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(10)
                );

        newTest.setQueueTicket(
                null
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(
                        newTest,
                        oldTest
                )
        );

        patientJourneyService
                .activateNext(
                        visitId
                );

        assertEquals(
                TestRequestStatus.PENDING,
                oldTest.getStatus()
        );

        assertEquals(
                TestRequestStatus.BLOCKED,
                newTest.getStatus()
        );
    }


    @Test
    void activateNext_ShouldCompleteVisit_WhenJourneyHasFinished() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket completedQueue =
                queue(
                        QueueStatus.DONE,
                        LocalDateTime.now()
                );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(completedQueue)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        patientJourneyService
                .activateNext(
                        visitId
                );

        assertEquals(
                VisitStatus.COMPLETED,
                visit.getStatus()
        );

        assertNotNull(
                visit.getCheckOutTime()
        );

        verify(visitRepo)
                .save(visit);
    }


    @Test
    void activateNext_ShouldNotCompleteVisit_WhenGroupedTestIsStillPending() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket doneQueue =
                queue(
                        QueueStatus.DONE,
                        LocalDateTime.now()
                );

        TestRequest pending =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                );

        pending.setQueueTicket(
                doneQueue
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(doneQueue)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(pending)
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        patientJourneyService
                .activateNext(
                        visitId
                );

        assertNotEquals(
                VisitStatus.COMPLETED,
                visit.getStatus()
        );

        assertNull(
                visit.getCheckOutTime()
        );

        verify(
                visitRepo,
                never()
        ).save(
                any()
        );
    }


    @Test
    void activateNext_ShouldNotCompleteVisit_WhenJourneyHasNoSteps() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        patientJourneyService
                .activateNext(
                        visitId
                );

        verify(
                visitRepo,
                never()
        ).save(
                visit
        );

        assertNotEquals(
                VisitStatus.COMPLETED,
                visit.getStatus()
        );
    }


    // =========================================================
    // GET / BUILD
    // =========================================================

    @Test
    void get_ShouldThrow_WhenVisitDoesNotExist() {

        UUID visitId =
                UUID.randomUUID();

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        patientJourneyService
                                .get(
                                        visitId
                                )
        );
    }


    @Test
    void get_ShouldBuildRegisteredCustomerJourney() {

        UUID visitId =
                UUID.randomUUID();

        Profile customer =
                mock(Profile.class);

        Account account =
                mock(Account.class);

        when(customer.getFullName())
                .thenReturn(
                        "Nguyen Van A"
                );

        when(customer.getPhone())
                .thenReturn(
                        "0901111111"
                );

        when(customer.getAccount())
                .thenReturn(account);

        CustomerVisit visit =
                visit(visitId);

        visit.setCustomer(
                customer
        );

        QueueTicket queue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                                .minusMinutes(5)
                );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(queue)
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertNotNull(result);

        assertEquals(
                "Nguyen Van A",
                result.patientName()
        );

        assertEquals(
                "0901111111",
                result.phone()
        );

        assertFalse(
                result.guest()
        );

        assertEquals(
                QueueStatus.WAITING.name(),
                result.currentStatus()
        );

        assertEquals(
                1,
                result.steps().size()
        );
    }


    @Test
    void get_ShouldUseGuestInformation_WhenCustomerIsNull() {

        UUID visitId =
                UUID.randomUUID();

        Appointment appointment =
                mock(Appointment.class);

        when(
                appointment.getGuestFullName()
        ).thenReturn(
                "Tran Van Guest"
        );

        when(
                appointment.getGuestPhone()
        ).thenReturn(
                "0988888888"
        );

        CustomerVisit visit =
                visit(visitId);

        visit.setAppointment(
                appointment
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                "Tran Van Guest",
                result.patientName()
        );

        assertEquals(
                "0988888888",
                result.phone()
        );

        assertTrue(
                result.guest()
        );

        assertEquals(
                "UNASSIGNED",
                result.currentStatus()
        );
    }


    @Test
    void get_ShouldUseWalkInName_WhenCustomerAndAppointmentAreNull() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                "Khách vãng lai",
                result.patientName()
        );

        assertNull(
                result.phone()
        );

        assertTrue(
                result.guest()
        );
    }


    @Test
    void get_ShouldReturnCompleted_WhenAllStepsFinished() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket done =
                queue(
                        QueueStatus.DONE,
                        LocalDateTime.now()
                );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(done)
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                "COMPLETED",
                result.currentStatus()
        );

        assertEquals(
                "Đã hoàn thành",
                result.currentStep()
        );
    }


    @Test
    void get_ShouldReturnNextBlockedService() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket blocked =
                queue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                );

        blocked.getService()
                .setName(
                        "Kham Noi"
                );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(blocked)
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                "-",
                result.nextStep()
        );

        assertEquals(
                "BLOCKED",
                result.currentStatus()
        );

        assertEquals(
                "Kham Noi",
                result.currentStep()
        );

        assertEquals(
                "QUEUE:" + blocked.getTicketId(),
                result.currentStepId()
        );
    }


    @Test
    void get_ShouldUseDefaultExaminationName_WhenQueueServiceMissing() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket queue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        queue.setService(null);

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(queue)
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                "Khám bệnh",
                result.currentStep()
        );
    }


    @Test
    void get_ShouldAddStandaloneParaclinicalTestAndResultWaitingStep() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        TestRequest test =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                                .minusSeconds(1)
                );

        test.setQueueTicket(
                null
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(test)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                2,
                result.steps().size()
        );

        assertEquals(
                "PARACLINICAL",
                result.steps()
                        .get(0)
                        .kind()
        );

        assertTrue(
                result.steps()
                        .stream()
                        .anyMatch(
                                step ->
                                        "RESULT_PENDING"
                                                .equals(
                                                        step.status()
                                                )
                        )
        );

        assertEquals(
                "RESULT_PENDING",
                result.currentStatus()
        );

        assertEquals(
                "Đang chờ kết quả cận lâm sàng",
                result.currentStep()
        );
    }


    @Test
    void get_ShouldUseDefaultParaclinicalName_WhenStandaloneTestHasNoService() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        TestRequest test =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                                .minusSeconds(1)
                );

        test.setQueueTicket(
                null
        );

        test.setService(
                null
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(test)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                "Cận lâm sàng",
                result.steps()
                        .stream()
                        .filter(
                                step ->
                                        "PARACLINICAL"
                                                .equals(
                                                        step.kind()
                                                )
                        )
                        .findFirst()
                        .orElseThrow()
                        .serviceName()
        );

        assertEquals(
                "RESULT_PENDING",
                result.currentStatus()
        );

        assertEquals(
                "Đang chờ kết quả cận lâm sàng",
                result.currentStep()
        );
    }


    @Test
    void get_ShouldGroupTestRequestsByQueueTicket() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket queue =
                paraclinicalQueue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        TestRequest first =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                );

        first.setQueueTicket(
                queue
        );

        first.setService(
                medicalService(
                        "Xet nghiem mau"
                )
        );

        TestRequest second =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                );

        second.setQueueTicket(
                queue
        );

        second.setService(
                medicalService(
                        "Sieu am"
                )
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(
                        first,
                        second
                )
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(queue)
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                1,
                result.steps().size()
        );

        assertEquals(
                "PARACLINICAL",
                result.steps()
                        .get(0)
                        .kind()
        );

        assertTrue(
                result.steps()
                        .get(0)
                        .serviceName()
                        .contains(
                                "Xet nghiem mau"
                        )
        );

        assertTrue(
                result.steps()
                        .get(0)
                        .serviceName()
                        .contains(
                                "Sieu am"
                        )
        );
    }


    @Test
    void get_ShouldUseDefaultName_WhenGroupedTestServiceMissing() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket queue =
                paraclinicalQueue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        TestRequest test =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                );

        test.setQueueTicket(
                queue
        );

        test.setService(
                null
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(test)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(queue)
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                "Cận lâm sàng",
                result.steps()
                        .get(0)
                        .serviceName()
        );
    }


    @Test
    void get_ShouldShowPaymentAsCurrentStep_WhenPendingInvoiceExists() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        Invoice pending =
                pendingInvoice(
                        LocalDateTime.now()
                                .minusMinutes(2)
                );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                invoiceRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(pending)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                "PAYMENT_PENDING",
                result.currentStatus()
        );

        assertEquals(
                "Thanh toán dịch vụ ban đầu",
                result.currentStep()
        );

        assertEquals(
                "Quầy thu ngân (-)",
                result.currentRoom()
        );

        assertEquals(
                "PAYMENT",
                result.steps()
                        .get(0)
                        .kind()
        );
    }


    @Test
    void get_ShouldShowResultPending_WhenGroupedTestStillProcessingAndNoPhysicalActiveQueue() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket doneQueue =
                queue(
                        QueueStatus.DONE,
                        LocalDateTime.now()
                                .minusMinutes(10)
                );

        TestRequest groupedPending =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                                .minusMinutes(5)
                );

        groupedPending.setQueueTicket(
                doneQueue
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(groupedPending)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(doneQueue)
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                "RESULT_PENDING",
                result.currentStatus()
        );

        assertEquals(
                "Đang chờ kết quả cận lâm sàng",
                result.currentStep()
        );

        assertTrue(
                result.steps()
                        .stream()
                        .anyMatch(
                                step ->
                                        "RESULT"
                                                .equals(
                                                        step.kind()
                                                )
                        )
        );
    }


    @Test
    void get_ShouldNotAddResultPending_WhenPhysicalQueueIsWaiting() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket waitingQueue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                                .minusMinutes(10)
                );

        TestRequest groupedPending =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                                .minusMinutes(5)
                );

        groupedPending.setQueueTicket(
                waitingQueue
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(groupedPending)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(waitingQueue)
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                "WAITING",
                result.currentStatus()
        );

        assertFalse(
                result.steps()
                        .stream()
                        .anyMatch(
                                step ->
                                        "RESULT_PENDING"
                                                .equals(
                                                        step.status()
                                                )
                        )
        );
    }


    @Test
    void get_ShouldSortJourneyStepsByCreatedAt() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket later =
                queue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(5)
                );

        QueueTicket earlier =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                                .minusMinutes(20)
                );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(
                        later,
                        earlier
                )
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                "QUEUE:"
                        + earlier.getTicketId(),
                result.steps()
                        .get(0)
                        .id()
        );
    }


    @Test
    void get_ShouldPutNullCreatedAtLast() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket withDate =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        QueueTicket withoutDate =
                queue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                );

        withoutDate.setCreatedAt(
                null
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(
                        withoutDate,
                        withDate
                )
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                "QUEUE:"
                        + withDate.getTicketId(),
                result.steps()
                        .get(0)
                        .id()
        );
    }


    @Test
    void get_ShouldUseDash_WhenCurrentRoomCodeIsNull() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket queue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        queue.getDepartment()
                .setRoomCode(
                        null
                );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(queue)
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertTrue(
                result.currentRoom()
                        .contains(
                                "(-)"
                        )
        );
    }


    @Test
    void get_ShouldFlagLongWaiting_WhenWaitingAtLeast60Minutes() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        visit.setCheckInTime(
                LocalDateTime.now()
                        .minusMinutes(90)
        );

        QueueTicket queue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(queue)
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertTrue(
                result.waitingMinutes()
                        >= 60
        );

        assertTrue(
                result.warning()
        );
    }


    @Test
    void get_ShouldSetWaitingZero_WhenCheckInTimeIsNull() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        visit.setCheckInTime(
                null
        );

        QueueTicket queue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(queue)
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .get(
                                visitId
                        );

        assertEquals(
                0,
                result.waitingMinutes()
        );
    }


    // =========================================================
    // LIST
    // =========================================================

    @Test
    void list_ShouldReturnAll_WhenFiltersAreNull() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        when(
                visitRepo.findAll()
        ).thenReturn(
                List.of(visit)
        );

        PageResponse<PatientJourneyResponse> result =
                patientJourneyService.list(
                        null,
                        null,
                        PageRequest.of(
                                0,
                                10
                        )
                );

        assertEquals(
                1,
                pageContent(result)
                        .size()
        );
    }


    @Test
    void list_ShouldFilterByPatientName() {

        UUID visitId =
                UUID.randomUUID();

        Profile customer =
                Profile.builder()
                        .profileId(
                                UUID.randomUUID()
                        )
                        .fullName(
                                "Nguyen Van Cuong"
                        )
                        .phone(
                                "0901234567"
                        )
                        .build();

        CustomerVisit visit =
                visit(visitId);

        visit.setCustomer(
                customer
        );

        when(
                visitRepo.findAll()
        ).thenReturn(
                List.of(visit)
        );

        PageResponse<PatientJourneyResponse> found =
                patientJourneyService.list(
                        "  CUONG ",
                        null,
                        PageRequest.of(
                                0,
                                10
                        )
                );

        PageResponse<PatientJourneyResponse> missing =
                patientJourneyService.list(
                        "khong-co",
                        null,
                        PageRequest.of(
                                0,
                                10
                        )
                );

        assertEquals(
                1,
                pageContent(found)
                        .size()
        );

        assertTrue(
                pageContent(missing)
                        .isEmpty()
        );
    }


    @Test
    void list_ShouldFilterByPhone() {

        UUID visitId =
                UUID.randomUUID();

        Profile customer =
                Profile.builder()
                        .profileId(
                                UUID.randomUUID()
                        )
                        .fullName(
                                "Patient"
                        )
                        .phone(
                                "0987654321"
                        )
                        .build();

        CustomerVisit visit =
                visit(visitId);

        visit.setCustomer(
                customer
        );

        when(
                visitRepo.findAll()
        ).thenReturn(
                List.of(visit)
        );

        PageResponse<PatientJourneyResponse> result =
                patientJourneyService.list(
                        "987654",
                        null,
                        PageRequest.of(
                                0,
                                10
                        )
                );

        assertEquals(
                1,
                pageContent(result)
                        .size()
        );
    }


    @Test
    void list_ShouldFilterByVisitCode() {

        UUID visitId =
                UUID.fromString(
                        "12345678-1111-2222-3333-444444444444"
                );

        CustomerVisit visit =
                visit(visitId);

        when(
                visitRepo.findAll()
        ).thenReturn(
                List.of(visit)
        );

        PageResponse<PatientJourneyResponse> result =
                patientJourneyService.list(
                        "VIS-12345678",
                        null,
                        PageRequest.of(
                                0,
                                10
                        )
                );

        assertEquals(
                1,
                pageContent(result)
                        .size()
        );
    }


    @Test
    void list_ShouldFilterByCurrentStatus() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket waiting =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        when(
                visitRepo.findAll()
        ).thenReturn(
                List.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(waiting)
        );

        PageResponse<PatientJourneyResponse> waitingResult =
                patientJourneyService.list(
                        null,
                        "WAITING",
                        PageRequest.of(
                                0,
                                10
                        )
                );

        PageResponse<PatientJourneyResponse> blockedResult =
                patientJourneyService.list(
                        null,
                        "BLOCKED",
                        PageRequest.of(
                                0,
                                10
                        )
                );

        assertEquals(
                1,
                pageContent(waitingResult)
                        .size()
        );

        assertTrue(
                pageContent(blockedResult)
                        .isEmpty()
        );
    }


    @Test
    void list_ShouldSortLatestCheckInFirstAndNullLast() {

        UUID firstId =
                UUID.randomUUID();

        UUID secondId =
                UUID.randomUUID();

        UUID nullId =
                UUID.randomUUID();

        CustomerVisit first =
                visit(firstId);

        first.setCheckInTime(
                LocalDateTime.now()
                        .minusHours(2)
        );

        CustomerVisit second =
                visit(secondId);

        second.setCheckInTime(
                LocalDateTime.now()
                        .minusMinutes(10)
        );

        CustomerVisit noDate =
                visit(nullId);

        noDate.setCheckInTime(
                null
        );

        when(
                visitRepo.findAll()
        ).thenReturn(
                List.of(
                        first,
                        noDate,
                        second
                )
        );

        PageResponse<PatientJourneyResponse> result =
                patientJourneyService.list(
                        null,
                        null,
                        PageRequest.of(
                                0,
                                10
                        )
                );

        List<PatientJourneyResponse> content =
                pageContent(result);

        assertEquals(
                secondId,
                content.get(0)
                        .visitId()
        );

        assertEquals(
                nullId,
                content.get(2)
                        .visitId()
        );
    }


    @Test
    void list_ShouldPaginateFilteredResults() {

        CustomerVisit first =
                visit(
                        UUID.randomUUID()
                );

        CustomerVisit second =
                visit(
                        UUID.randomUUID()
                );

        CustomerVisit third =
                visit(
                        UUID.randomUUID()
                );

        first.setCheckInTime(
                LocalDateTime.now()
                        .minusMinutes(30)
        );

        second.setCheckInTime(
                LocalDateTime.now()
                        .minusMinutes(20)
        );

        third.setCheckInTime(
                LocalDateTime.now()
                        .minusMinutes(10)
        );

        when(
                visitRepo.findAll()
        ).thenReturn(
                List.of(
                        first,
                        second,
                        third
                )
        );

        PageResponse<PatientJourneyResponse> firstPage =
                patientJourneyService.list(
                        null,
                        null,
                        PageRequest.of(
                                0,
                                2
                        )
                );

        PageResponse<PatientJourneyResponse> secondPage =
                patientJourneyService.list(
                        null,
                        null,
                        PageRequest.of(
                                1,
                                2
                        )
                );

        assertEquals(
                2,
                pageContent(firstPage)
                        .size()
        );

        assertEquals(
                1,
                pageContent(secondPage)
                        .size()
        );

        assertEquals(
                third.getVisitId(),
                pageContent(firstPage)
                        .get(0)
                        .visitId()
        );

        assertEquals(
                first.getVisitId(),
                pageContent(secondPage)
                        .get(0)
                        .visitId()
        );
    }

    @Test
    void activateNext_ShouldPauseWhileAnyTicketIsMarkedAbsent() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        QueueTicket skipped = queue(QueueStatus.SKIPPED, LocalDateTime.now());
        QueueTicket blocked = queue(QueueStatus.BLOCKED, LocalDateTime.now().plusMinutes(1));
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(skipped, blocked));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());

        patientJourneyService.activateNext(visitId);

        assertEquals(QueueStatus.BLOCKED, blocked.getStatus());
        verify(queueRepo, never()).save(any());
        verify(visitRepo, never()).save(any());
    }

    @Test
    void activateNext_ShouldOpenLinkedParaclinicalRoomBeforeOtherBlockedSteps() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        Department examinationDepartment = department("Phòng khám Nội", "INT-103");
        examinationDepartment.setDepartmentType(DepartmentType.EXAMINATION);
        QueueTicket suspended = queue(QueueStatus.WAITING_FOR_TEST, LocalDateTime.now().minusMinutes(20));
        suspended.setDepartment(examinationDepartment);
        QueueTicket nextExamination = queue(QueueStatus.BLOCKED, LocalDateTime.now().minusMinutes(15));
        nextExamination.setDepartment(examinationDepartment);
        QueueTicket unrelatedLab = paraclinicalQueue(QueueStatus.BLOCKED, LocalDateTime.now().minusMinutes(10));
        QueueTicket linkedLab = paraclinicalQueue(QueueStatus.BLOCKED, LocalDateTime.now());
        MedicalRecord sourceRecord = MedicalRecord.builder().recordId(UUID.randomUUID())
                .queueTicket(suspended).visit(visit).build();
        TestRequest linkedRequest = TestRequest.builder().testRequestId(UUID.randomUUID())
                .medicalRecord(sourceRecord).queueTicket(linkedLab)
                .status(TestRequestStatus.BLOCKED).build();
        linkedRequest.setCreatedAt(LocalDateTime.now());
        List<QueueTicket> queues = List.of(suspended, nextExamination, unrelatedLab, linkedLab);
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(queues);
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(linkedRequest));
        when(testRepo.findAllByQueueTicket_TicketId(linkedLab.getTicketId()))
                .thenReturn(List.of(linkedRequest));

        patientJourneyService.activateNext(visitId);

        assertEquals(QueueStatus.WAITING, linkedLab.getStatus());
        assertEquals(TestRequestStatus.PENDING, linkedRequest.getStatus());
        assertEquals(QueueStatus.BLOCKED, unrelatedLab.getStatus());
        assertEquals(QueueStatus.BLOCKED, nextExamination.getStatus());
        verify(queueRepo).save(linkedLab);
        verify(testRepo).save(linkedRequest);
    }

    @Test
    void activateNext_ShouldKeepLaterExaminationBlockedWhileSourceWaitsForResults() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        Department examinationDepartment = department("Phòng khám Nội", "INT-103");
        examinationDepartment.setDepartmentType(DepartmentType.EXAMINATION);
        QueueTicket suspended = queue(QueueStatus.WAITING_FOR_TEST, LocalDateTime.now().minusMinutes(10));
        suspended.setDepartment(examinationDepartment);
        QueueTicket laterExamination = queue(QueueStatus.BLOCKED, LocalDateTime.now());
        laterExamination.setDepartment(examinationDepartment);
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(suspended, laterExamination));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());

        patientJourneyService.activateNext(visitId);

        assertEquals(QueueStatus.BLOCKED, laterExamination.getStatus());
        verify(queueRepo, never()).save(any());
        verify(visitRepo, never()).save(any());
    }

    @Test
    void refreshWaitingExaminations_ShouldReleaseOnlyRecordWhoseTestsFinished() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        Department examinationDepartment = department("Phòng khám", "INT-103");
        examinationDepartment.setDepartmentType(DepartmentType.EXAMINATION);
        QueueTicket finishedSource = queue(QueueStatus.WAITING_FOR_TEST, LocalDateTime.now().minusHours(1));
        finishedSource.setDepartment(examinationDepartment);
        QueueTicket pendingSource = queue(QueueStatus.WAITING_FOR_TEST, LocalDateTime.now().minusMinutes(30));
        pendingSource.setDepartment(examinationDepartment);
        MedicalRecord finishedRecord = MedicalRecord.builder().recordId(UUID.randomUUID())
                .visit(visit).queueTicket(finishedSource).build();
        MedicalRecord pendingRecord = MedicalRecord.builder().recordId(UUID.randomUUID())
                .visit(visit).queueTicket(pendingSource).build();
        TestRequest completed = TestRequest.builder().testRequestId(UUID.randomUUID())
                .medicalRecord(finishedRecord).status(TestRequestStatus.COMPLETED).build();
        TestRequest pending = TestRequest.builder().testRequestId(UUID.randomUUID())
                .medicalRecord(pendingRecord).status(TestRequestStatus.PENDING).build();
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(finishedSource, pendingSource));
        when(recordRepo.findByQueueTicket_TicketId(finishedSource.getTicketId()))
                .thenReturn(Optional.of(finishedRecord));
        when(recordRepo.findByQueueTicket_TicketId(pendingSource.getTicketId()))
                .thenReturn(Optional.of(pendingRecord));
        when(invoiceRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of(completed, pending));

        patientJourneyService.refreshWaitingExaminationsAfterTestCompletion(visitId);

        assertEquals(QueueStatus.TEST_DONE, finishedSource.getStatus());
        assertEquals(QueueStatus.WAITING_FOR_TEST, pendingSource.getStatus());
        verify(queueRepo).save(finishedSource);
        verify(queueRepo, never()).save(pendingSource);
    }

    @Test
    void publishQueueActivated_ShouldNotifyLabChannelsWithoutBreakingWorkflowOnRealtimeFailure() {
        assertDoesNotThrow(() -> invokePublishQueueActivated(null));
        QueueTicket noDepartment = QueueTicket.builder().ticketId(UUID.randomUUID()).build();
        assertDoesNotThrow(() -> invokePublishQueueActivated(noDepartment));

        Department laboratory = paraclinicalDepartment("Xét nghiệm", "LAB-201");
        QueueTicket queue = QueueTicket.builder().ticketId(UUID.randomUUID())
                .department(laboratory).build();
        invokePublishQueueActivated(queue);
        verify(messagingTemplate).convertAndSend(
                "/topic/department-" + laboratory.getDepartmentId() + "-queue", "QUEUE_UPDATED");
        verify(messagingTemplate).convertAndSend("/topic/queue-display", "QUEUE_UPDATED");
        verify(messagingTemplate).convertAndSend(
                "/topic/department-" + laboratory.getDepartmentId() + "-lab-queue", "LAB_UPDATED");

        reset(messagingTemplate);
        doThrow(new IllegalStateException("realtime unavailable")).when(messagingTemplate)
                .convertAndSend(anyString(), anyString());
        assertDoesNotThrow(() -> invokePublishQueueActivated(queue));
    }

    private void invokePublishQueueActivated(QueueTicket queue) {
        try {
            Method method = PatientJourneyService.class
                    .getDeclaredMethod("publishQueueActivated", QueueTicket.class);
            method.setAccessible(true);
            method.invoke(patientJourneyService, queue);
        } catch (java.lang.reflect.InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    void list_ShouldSeparateTodayAndUnfinishedOverdueJourneys() {
        CustomerVisit today = visit(UUID.randomUUID());
        CustomerVisit overdue = visit(UUID.randomUUID());
        CustomerVisit completedHistory = visit(UUID.randomUUID());
        java.time.LocalDate clinicDate = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        today.setCheckInTime(clinicDate.atTime(12, 0));
        overdue.setCheckInTime(clinicDate.minusDays(3).atTime(12, 0));
        completedHistory.setCheckInTime(clinicDate.minusDays(4).atTime(12, 0));

        QueueTicket active = queue(QueueStatus.IN_PROGRESS, overdue.getCheckInTime());
        QueueTicket done = queue(QueueStatus.DONE, completedHistory.getCheckInTime());
        when(visitRepo.findAll()).thenReturn(List.of(today, overdue, completedHistory));
        when(queueRepo.findAllByVisit_VisitId(today.getVisitId())).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(overdue.getVisitId())).thenReturn(List.of(active));
        when(queueRepo.findAllByVisit_VisitId(completedHistory.getVisitId())).thenReturn(List.of(done));

        var todayPage = patientJourneyService.list(null, null, "TODAY", PageRequest.of(0, 10));
        var overduePage = patientJourneyService.list(null, null, "OVERDUE", PageRequest.of(0, 10));
        var allPage = patientJourneyService.list(null, null, "ALL", PageRequest.of(0, 10));

        assertEquals(List.of(today.getVisitId()), pageContent(todayPage).stream()
                .map(PatientJourneyResponse::visitId).toList());
        assertEquals(List.of(overdue.getVisitId()), pageContent(overduePage).stream()
                .map(PatientJourneyResponse::visitId).toList());
        assertEquals(3, pageContent(allPage).size());
    }

    @Test
    void list_ShouldRejectUnknownScope() {
        assertThrows(vn.edu.fpt.cares.exception.BadRequestException.class,
                () -> patientJourneyService.list(null, null, "YESTERDAY", PageRequest.of(0, 10)));
    }


    // =========================================================
    // ADVANCE BLOCKED STEP
    // =========================================================

    @Test
    void advanceBlockedStep_ShouldThrow_WhenVisitDoesNotExist() {

        UUID visitId =
                UUID.randomUUID();

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        patientJourneyService
                                .advanceBlockedStep(
                                        visitId
                                )
        );
    }


    @Test
    void advanceBlockedStep_ShouldActivateOldestBlockedParaclinicalQueue_WhenNoPhysicalQueueActive() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket oldBlocked =
                paraclinicalQueue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(20)
                );

        QueueTicket newBlocked =
                paraclinicalQueue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(10)
                );

        TestRequest linkedBlocked =
                testRequest(
                        TestRequestStatus.BLOCKED,
                        LocalDateTime.now()
                );

        linkedBlocked.setQueueTicket(
                oldBlocked
        );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(
                        newBlocked,
                        oldBlocked
                )
        );

        when(
                testRepo.findAllByQueueTicket_TicketId(
                        oldBlocked.getTicketId()
                )
        ).thenReturn(
                List.of(
                        linkedBlocked
                )
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .advanceBlockedStep(
                                visitId
                        );

        assertNotNull(result);

        assertEquals(
                QueueStatus.WAITING,
                oldBlocked.getStatus()
        );

        assertEquals(
                QueueStatus.BLOCKED,
                newBlocked.getStatus()
        );

        assertEquals(
                TestRequestStatus.PENDING,
                linkedBlocked.getStatus()
        );

        verify(queueRepo)
                .save(
                        oldBlocked
                );

        verify(testRepo)
                .save(
                        linkedBlocked
                );
    }


    @Test
    void advanceBlockedStep_ShouldDelegateToActivateNext_WhenPhysicalQueueAlreadyActive() {

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket active =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        QueueTicket blockedParaclinical =
                paraclinicalQueue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(10)
                );

        when(
                visitRepo.findById(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueRepo.findAllByVisit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of(
                        active,
                        blockedParaclinical
                )
        );

        when(
                testRepo.findAllByMedicalRecord_Visit_VisitId(
                        visitId
                )
        ).thenReturn(
                List.of()
        );

        PatientJourneyResponse result =
                patientJourneyService
                        .advanceBlockedStep(
                                visitId
                        );

        assertNotNull(result);

        assertEquals(
                QueueStatus.BLOCKED,
                blockedParaclinical.getStatus()
        );

        verify(
                queueRepo,
                never()
        ).save(
                blockedParaclinical
        );
    }


    // =========================================================
    // LIST FOR CUSTOMER
    // =========================================================

    @Test
    void listForCustomer_ShouldReturnEmpty_WhenCustomerHasNoVisits() {

        UUID profileId =
                UUID.randomUUID();

        when(
                visitRepo.findAllByCustomer_ProfileIdOrderByCheckInTimeDesc(
                        profileId
                )
        ).thenReturn(
                List.of()
        );

        List<PatientJourneyResponse> result =
                patientJourneyService
                        .listForCustomer(
                                profileId
                        );

        assertTrue(
                result.isEmpty()
        );
    }


    @Test
    void listForCustomer_ShouldMapCustomerVisits() {

        UUID profileId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        when(
                visitRepo.findAllByCustomer_ProfileIdOrderByCheckInTimeDesc(
                        profileId
                )
        ).thenReturn(
                List.of(visit)
        );

        List<PatientJourneyResponse> result =
                patientJourneyService
                        .listForCustomer(
                                profileId
                        );

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                visitId,
                result.get(0)
                        .visitId()
        );
    }

    @Test
    void skippedQueue_ShouldHideCustomerReturnActionAndExposeExpiredVariant() throws Exception {
        Method method = PatientJourneyService.class.getDeclaredMethod("skippedQueue", UUID.class, QueueTicket.class);
        method.setAccessible(true);
        UUID visitId = UUID.randomUUID();
        Department room = Department.builder().name("Phòng Nội").roomCode("INT-101").build();
        QueueTicket ticket = QueueTicket.builder().ticketId(UUID.randomUUID()).department(room).queueNumber(7)
                .workDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
                .status(QueueStatus.SKIPPED).build();

        var none = (vn.edu.fpt.cares.dto.journey.PatientQueueResponse) method.invoke(
                patientJourneyService, visitId, ticket);
        assertFalse(none.canRequestReturn());
        assertEquals("NONE", none.returnRequestStatus());
        assertEquals("Phòng Nội", none.roomName());
        assertEquals("INT-101", none.roomCode());

        assertNull(none.returnRequestedAt());

        ticket.setWorkDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1));
        ticket.setDepartment(null);
        var expired = (vn.edu.fpt.cares.dto.journey.PatientQueueResponse) method.invoke(
                patientJourneyService, visitId, ticket);
        assertEquals("EXPIRED", expired.returnRequestStatus());
        assertFalse(expired.canRequestReturn());
        assertNull(expired.roomName());
        assertNull(expired.roomCode());

        ticket.setWorkDate(null);
        assertEquals("EXPIRED", ((vn.edu.fpt.cares.dto.journey.PatientQueueResponse) method.invoke(
                patientJourneyService, visitId, ticket)).returnRequestStatus());
    }

    @Test
    void journeyStatusPriority_ShouldCoverEveryQueueStatusGroup() throws Exception {
        Method method = PatientJourneyService.class.getDeclaredMethod("journeyStatusPriority", QueueStatus.class);
        method.setAccessible(true);
        assertEquals(0, method.invoke(patientJourneyService, QueueStatus.IN_PROGRESS));
        assertEquals(1, method.invoke(patientJourneyService, QueueStatus.CALLED));
        assertEquals(2, method.invoke(patientJourneyService, QueueStatus.WAITING));
        assertEquals(2, method.invoke(patientJourneyService, QueueStatus.TEST_DONE));
        assertEquals(2, method.invoke(patientJourneyService, QueueStatus.WAITING_FOR_TEST));
        assertEquals(3, method.invoke(patientJourneyService, QueueStatus.BLOCKED));
        assertEquals(4, method.invoke(patientJourneyService, QueueStatus.DONE));
        assertEquals(5, method.invoke(patientJourneyService, QueueStatus.SKIPPED));
    }

    @Test
    void currentQueue_ShouldResolveDirectEncodedLegacyAndInvalidSteps() throws Exception {
        Method method = PatientJourneyService.class.getDeclaredMethod("currentQueue",
                PatientJourneyResponse.Step.class, List.class);
        method.setAccessible(true);
        UUID id = UUID.randomUUID();
        QueueTicket ticket = QueueTicket.builder().ticketId(id).build();
        List<QueueTicket> tickets = List.of(ticket);

        assertTrue(((Optional<?>) method.invoke(patientJourneyService, null, tickets)).isEmpty());
        assertSame(ticket, ((Optional<?>) method.invoke(patientJourneyService, journeyStep("ANY", id), tickets)).orElseThrow());
        assertSame(ticket, ((Optional<?>) method.invoke(patientJourneyService, journeyStep("QUEUE:" + id, null), tickets)).orElseThrow());
        assertSame(ticket, ((Optional<?>) method.invoke(patientJourneyService,
                journeyStep("RETURN:" + id + ":CYCLE:2", null), tickets)).orElseThrow());
        assertTrue(((Optional<?>) method.invoke(patientJourneyService, journeyStep(null, null), tickets)).isEmpty());
        assertTrue(((Optional<?>) method.invoke(patientJourneyService, journeyStep("PAYMENT:" + id, null), tickets)).isEmpty());
        assertTrue(((Optional<?>) method.invoke(patientJourneyService, journeyStep("QUEUE:not-a-uuid", null), tickets)).isEmpty());
        assertTrue(((Optional<?>) method.invoke(patientJourneyService,
                journeyStep("QUEUE:" + UUID.randomUUID(), null), tickets)).isEmpty());
    }

    private PatientJourneyResponse.Step journeyStep(String id, UUID queueTicketId) {
        return new PatientJourneyResponse.Step(id, "EXAMINATION", "Khám", null, null, null,
                QueueStatus.WAITING.name(), null, null, List.of(), 0, 0,
                "EXAMINATION", null, queueTicketId, null);
    }

    @Test
    void hasOutstandingTestsForExamination_ShouldCoverMissingCompletedCancelledAndPendingBatches() {
        UUID ticketId = UUID.randomUUID();
        assertFalse(patientJourneyService.hasOutstandingTestsForExamination(ticketId));

        QueueTicket examination = QueueTicket.builder().ticketId(ticketId).build();
        MedicalRecord missingVisit = MedicalRecord.builder().queueTicket(examination).build();
        when(recordRepo.findByQueueTicket_TicketId(ticketId)).thenReturn(Optional.of(missingVisit));
        assertFalse(patientJourneyService.hasOutstandingTestsForExamination(ticketId));

        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID()).visit(visit)
                .queueTicket(examination).build();
        when(recordRepo.findByQueueTicket_TicketId(ticketId)).thenReturn(Optional.of(record));
        Invoice cancelledInvoice = Invoice.builder().invoiceId(UUID.randomUUID()).medicalRecord(record)
                .status(InvoiceStatus.CANCELLED).build();
        when(invoiceRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(cancelledInvoice));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId())).thenReturn(List.of());
        assertFalse(patientJourneyService.hasOutstandingTestsForExamination(ticketId));

        TestRequest completed = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record)
                .status(TestRequestStatus.COMPLETED).build();
        TestRequest cancelled = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record)
                .status(TestRequestStatus.CANCELLED).build();
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId()))
                .thenReturn(List.of(completed, cancelled));
        assertFalse(patientJourneyService.hasOutstandingTestsForExamination(ticketId));

        TestRequest pending = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record)
                .status(TestRequestStatus.PENDING).build();
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId()))
                .thenReturn(List.of(completed, pending));
        assertTrue(patientJourneyService.hasOutstandingTestsForExamination(ticketId));

        Invoice initialPaid = Invoice.builder().invoiceId(UUID.randomUUID()).visit(visit)
                .status(InvoiceStatus.PAID).build();
        initialPaid.setCreatedAt(LocalDateTime.now().minusHours(2));
        Invoice clinicalPaid = Invoice.builder().invoiceId(UUID.randomUUID()).visit(visit)
                .medicalRecord(record).status(InvoiceStatus.PAID).build();
        clinicalPaid.setCreatedAt(LocalDateTime.now().minusHours(1));
        MedicalRecord standalone = MedicalRecord.builder().recordId(UUID.randomUUID()).visit(visit).build();
        TestRequest carried = TestRequest.builder().testRequestId(UUID.randomUUID())
                .medicalRecord(standalone)
                .invoiceItem(InvoiceItem.builder().itemId(UUID.randomUUID()).invoice(initialPaid).build())
                .status(TestRequestStatus.PENDING).build();
        carried.setCreatedAt(LocalDateTime.now().minusMinutes(90));
        when(invoiceRepo.findAllByVisit_VisitId(visit.getVisitId()))
                .thenReturn(List.of(initialPaid, clinicalPaid));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId()))
                .thenReturn(List.of(carried));

        assertTrue(patientJourneyService.hasOutstandingTestsForExamination(ticketId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void carriedPrebookedTests_ShouldApplyEveryEligibilityGuard() throws Exception {
        Method method = PatientJourneyService.class.getDeclaredMethod("carriedPrebookedTests",
                CustomerVisit.class, Invoice.class, List.class, List.class);
        method.setAccessible(true);
        LocalDateTime orderedAt = LocalDateTime.of(2026, 9, 7, 10, 0);
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        QueueTicket examTicket = QueueTicket.builder().ticketId(UUID.randomUUID()).build();
        MedicalRecord examRecord = MedicalRecord.builder().recordId(UUID.randomUUID()).visit(visit)
                .queueTicket(examTicket).build();
        Invoice clinical = Invoice.builder().invoiceId(UUID.randomUUID()).medicalRecord(examRecord)
                .status(InvoiceStatus.PAID).build();
        clinical.setCreatedAt(orderedAt);
        Invoice initial = Invoice.builder().invoiceId(UUID.randomUUID()).status(InvoiceStatus.PAID).build();
        initial.setCreatedAt(orderedAt.minusHours(1));
        Invoice pending = Invoice.builder().invoiceId(UUID.randomUUID()).status(InvoiceStatus.PENDING).build();
        Invoice attached = Invoice.builder().invoiceId(UUID.randomUUID()).status(InvoiceStatus.PAID)
                .medicalRecord(examRecord).build();

        java.util.function.BiFunction<Invoice, TestRequestStatus, TestRequest> standalone = (invoice, status) -> {
            InvoiceItem item = invoice == null ? null : InvoiceItem.builder().invoice(invoice).build();
            TestRequest test = TestRequest.builder().testRequestId(UUID.randomUUID()).status(status)
                    .medicalRecord(MedicalRecord.builder().visit(visit).build()).invoiceItem(item).build();
            test.setCreatedAt(orderedAt.minusMinutes(10));
            return test;
        };
        TestRequest eligible = standalone.apply(initial, TestRequestStatus.PENDING);
        TestRequest cancelled = standalone.apply(initial, TestRequestStatus.CANCELLED);
        TestRequest noRecord = standalone.apply(initial, TestRequestStatus.PENDING); noRecord.setMedicalRecord(null);
        TestRequest examinationOwned = standalone.apply(initial, TestRequestStatus.PENDING);
        examinationOwned.getMedicalRecord().setQueueTicket(examTicket);
        TestRequest noInvoice = standalone.apply(null, TestRequestStatus.PENDING);
        TestRequest unknownInvoice = standalone.apply(Invoice.builder().invoiceId(UUID.randomUUID())
                .status(InvoiceStatus.PAID).build(), TestRequestStatus.PENDING);
        TestRequest clinicalSource = standalone.apply(attached, TestRequestStatus.PENDING);
        TestRequest unpaid = standalone.apply(pending, TestRequestStatus.PENDING);
        TestRequest createdAfter = standalone.apply(initial, TestRequestStatus.PENDING);
        createdAfter.setCreatedAt(orderedAt.plusSeconds(1));
        TestRequest completedBefore = standalone.apply(initial, TestRequestStatus.COMPLETED);
        completedBefore.setCompletedAt(orderedAt.minusSeconds(1));
        TestRequest completedAtOrder = standalone.apply(initial, TestRequestStatus.COMPLETED);
        completedAtOrder.setCompletedAt(orderedAt);

        List<Invoice> invoices = List.of(initial, pending, attached, clinical);
        List<TestRequest> candidates = List.of(eligible, cancelled, noRecord, examinationOwned, noInvoice,
                unknownInvoice, clinicalSource, unpaid, createdAfter, completedBefore, completedAtOrder);
        List<TestRequest> result = (List<TestRequest>) method.invoke(patientJourneyService,
                visit, clinical, invoices, candidates);
        assertEquals(List.of(eligible, completedAtOrder), result);

        Invoice earlierClinical = Invoice.builder().invoiceId(UUID.randomUUID()).medicalRecord(examRecord)
                .status(InvoiceStatus.PAID).build();
        earlierClinical.setCreatedAt(orderedAt.minusMinutes(2));
        assertTrue(((List<TestRequest>) method.invoke(patientJourneyService, visit, clinical,
                List.of(earlierClinical, clinical, initial), List.of(eligible))).isEmpty());

        clinical.setCreatedAt(null);
        eligible.setCreatedAt(null);
        eligible.setCompletedAt(orderedAt.minusDays(1));
        assertEquals(List.of(eligible), method.invoke(patientJourneyService, visit, clinical,
                List.of(initial, clinical), List.of(eligible)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void carriedPrebookedTestsForLegacyCycleFiltersInvoiceAndRequestStates() throws Exception {
        Method method = PatientJourneyService.class.getDeclaredMethod("carriedPrebookedTestsForLegacyCycle",
                List.class, List.class, List.class);
        method.setAccessible(true);
        assertEquals(List.of(), method.invoke(patientJourneyService, List.of(), List.of(), List.of()));

        Invoice paidInitial = Invoice.builder().invoiceId(UUID.randomUUID()).status(InvoiceStatus.PAID).build();
        Invoice unpaid = Invoice.builder().invoiceId(UUID.randomUUID()).status(InvoiceStatus.PENDING).build();
        Invoice clinical = Invoice.builder().invoiceId(UUID.randomUUID()).status(InvoiceStatus.PAID)
                .medicalRecord(MedicalRecord.builder().recordId(UUID.randomUUID()).build()).build();
        Invoice withoutId = Invoice.builder().status(InvoiceStatus.PAID).build();
        java.util.function.BiFunction<Invoice, TestRequestStatus, TestRequest> standalone = (invoice, status) ->
                TestRequest.builder().testRequestId(UUID.randomUUID()).status(status)
                        .medicalRecord(MedicalRecord.builder().recordId(UUID.randomUUID()).build())
                        .invoiceItem(invoice == null ? null : InvoiceItem.builder().invoice(invoice).build()).build();

        TestRequest linkedUnknown = standalone.apply(
                Invoice.builder().invoiceId(UUID.randomUUID()).status(InvoiceStatus.PAID).build(),
                TestRequestStatus.PENDING);
        TestRequest linkedUnpaid = standalone.apply(unpaid, TestRequestStatus.PENDING);
        TestRequest linkedClinical = standalone.apply(clinical, TestRequestStatus.PENDING);
        assertEquals(List.of(), method.invoke(patientJourneyService,
                List.of(linkedUnknown, linkedUnpaid, linkedClinical),
                List.of(withoutId, unpaid, clinical), List.of()));

        TestRequest linkedPaid = standalone.apply(paidInitial, TestRequestStatus.PENDING);
        TestRequest eligible = standalone.apply(paidInitial, TestRequestStatus.IN_PROGRESS);
        TestRequest cancelled = standalone.apply(paidInitial, TestRequestStatus.CANCELLED);
        TestRequest completed = standalone.apply(paidInitial, TestRequestStatus.COMPLETED);
        TestRequest noRecord = standalone.apply(paidInitial, TestRequestStatus.PENDING);
        noRecord.setMedicalRecord(null);
        TestRequest examinationOwned = standalone.apply(paidInitial, TestRequestStatus.PENDING);
        examinationOwned.getMedicalRecord().setQueueTicket(QueueTicket.builder().ticketId(UUID.randomUUID()).build());
        TestRequest anotherInvoice = standalone.apply(unpaid, TestRequestStatus.PENDING);
        TestRequest noInvoice = standalone.apply(null, TestRequestStatus.PENDING);

        List<TestRequest> result = (List<TestRequest>) method.invoke(patientJourneyService,
                List.of(linkedPaid), List.of(withoutId, paidInitial, unpaid, clinical),
                List.of(eligible, cancelled, completed, noRecord, examinationOwned, anotherInvoice, noInvoice));

        assertEquals(List.of(eligible), result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void addCycleParaclinicalSteps_ShouldCoverQueuedUnqueuedAndPlannedFallbacks() throws Exception {
        Method method = PatientJourneyService.class.getDeclaredMethod("addCycleParaclinicalSteps",
                List.class, Invoice.class, QueueTicket.class, int.class, List.class, java.util.Set.class);
        method.setAccessible(true);
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        Department lab = Department.builder().departmentId(UUID.randomUUID()).name("Sinh hóa")
                .roomCode("LAB-202").build();
        MedicalService glucose = MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("LAB-GLU")
                .name("Đường huyết").department(lab).build();
        QueueTicket exam = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).build();
        QueueTicket labQueue = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).department(lab)
                .status(QueueStatus.WAITING).queueNumber(3).build();
        TestRequest queued = TestRequest.builder().testRequestId(UUID.randomUUID()).queueTicket(labQueue)
                .service(glucose).performingDepartment(lab).status(TestRequestStatus.PENDING).build();
        TestRequest unqueued = TestRequest.builder().testRequestId(UUID.randomUUID())
                .status(TestRequestStatus.COMPLETED).build();
        Invoice invoice = Invoice.builder().invoiceId(UUID.randomUUID()).status(InvoiceStatus.PAID).build();
        List<PatientJourneyResponse.Step> steps = new java.util.ArrayList<>();

        method.invoke(patientJourneyService, steps, invoice, exam, 1,
                List.of(queued, unqueued), new java.util.HashSet<UUID>());
        assertEquals(2, steps.size());
        assertEquals("Cận lâm sàng", steps.get(1).serviceName());
        assertNull(steps.get(1).roomName());

        MedicalService rbc = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("AN-CBC-RBC").name("Số lượng hồng cầu (RBC)").department(lab).build();
        MedicalService hgb = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("AN-CBC-HGB").name("Huyết sắc tố (HGB)").department(lab).build();
        TestRequest unqueuedRbc = TestRequest.builder().testRequestId(UUID.randomUUID()).service(rbc)
                .performingDepartment(lab).status(TestRequestStatus.PENDING).build();
        TestRequest unqueuedHgb = TestRequest.builder().testRequestId(UUID.randomUUID()).service(hgb)
                .performingDepartment(lab).status(TestRequestStatus.PENDING).build();
        List<PatientJourneyResponse.Step> groupedUnqueued = new java.util.ArrayList<>();
        method.invoke(patientJourneyService, groupedUnqueued, invoice, exam, 1,
                List.of(unqueuedRbc, unqueuedHgb), new java.util.HashSet<UUID>());
        assertEquals(1, groupedUnqueued.size());
        assertEquals("Công thức máu (2 chỉ số)", groupedUnqueued.get(0).serviceName());
        assertEquals(1, groupedUnqueued.get(0).services().size());

        List<PatientJourneyResponse.Step> duplicateSteps = new java.util.ArrayList<>();
        method.invoke(patientJourneyService, duplicateSteps, invoice, exam, 1,
                List.of(queued), new java.util.HashSet<>(List.of(labQueue.getTicketId())));
        assertTrue(duplicateSteps.isEmpty());

        InvoiceItem assigned = InvoiceItem.builder().service(glucose).serviceCodeSnapshot("LAB-GLU")
                .serviceSnapshot("Đường huyết").build();
        InvoiceItem unassigned = InvoiceItem.builder().service(null).serviceCodeSnapshot("IMG-X")
                .serviceSnapshot("Chẩn đoán hình ảnh").build();
        invoice.setItems(List.of(assigned, unassigned));
        List<PatientJourneyResponse.Step> planned = new java.util.ArrayList<>();
        method.invoke(patientJourneyService, planned, invoice, exam, 2, List.of(), new java.util.HashSet<UUID>());
        assertEquals(2, planned.size());
        assertTrue(planned.stream().anyMatch(step -> "Phân phòng sau thanh toán".equals(step.roomName())));
        assertTrue(planned.stream().anyMatch(step -> "Sinh hóa".equals(step.roomName())));

        InvoiceItem plannedRbc = InvoiceItem.builder().service(rbc).serviceCodeSnapshot("AN-CBC-RBC")
                .serviceSnapshot("Số lượng hồng cầu (RBC)").build();
        InvoiceItem plannedHgb = InvoiceItem.builder().service(hgb).serviceCodeSnapshot("AN-CBC-HGB")
                .serviceSnapshot("Huyết sắc tố (HGB)").build();
        invoice.setItems(List.of(plannedRbc, plannedHgb));
        List<PatientJourneyResponse.Step> groupedPlanned = new java.util.ArrayList<>();
        method.invoke(patientJourneyService, groupedPlanned, invoice, exam, 2,
                List.of(), new java.util.HashSet<UUID>());
        assertEquals(1, groupedPlanned.size());
        assertEquals("Công thức máu (2 chỉ số)", groupedPlanned.get(0).serviceName());
        assertEquals(1, groupedPlanned.get(0).services().size());

        invoice.setItems(null);
        List<PatientJourneyResponse.Step> noItems = new java.util.ArrayList<>();
        method.invoke(patientJourneyService, noItems, invoice, exam, 3, List.of(), new java.util.HashSet<UUID>());
        assertTrue(noItems.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void groupedServiceProgress_ShouldPresentAnalytesAsOnePanel() throws Exception {
        Method method = PatientJourneyService.class.getDeclaredMethod("groupedServiceProgress", List.class);
        method.setAccessible(true);
        MedicalService rbc = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("AN-CBC-RBC").name("Số lượng hồng cầu (RBC)").build();
        MedicalService hgb = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("AN-CBC-HGB").name("Huyết sắc tố (HGB)").build();
        TestRequest first = TestRequest.builder().testRequestId(UUID.randomUUID()).service(rbc)
                .status(TestRequestStatus.COMPLETED).build();
        TestRequest second = TestRequest.builder().testRequestId(UUID.randomUUID()).service(hgb)
                .status(TestRequestStatus.PENDING).build();

        List<PatientJourneyResponse.ServiceProgress> progress =
                (List<PatientJourneyResponse.ServiceProgress>) method.invoke(patientJourneyService,
                        List.of(first, second));

        assertEquals(1, progress.size());
        assertEquals("LAB-001", progress.get(0).serviceCode());
        assertEquals("Công thức máu (2 chỉ số)", progress.get(0).serviceName());
        assertEquals("PENDING", progress.get(0).status());

        second.setStatus(TestRequestStatus.COMPLETED);
        progress = (List<PatientJourneyResponse.ServiceProgress>) method.invoke(patientJourneyService,
                List.of(first, second));
        assertEquals("COMPLETED", progress.get(0).status());
    }

    @Test
    void queueStep_ShouldPresentAnalytesInTheSameQueueAsOnePanel() throws Exception {
        Method method = PatientJourneyService.class.getDeclaredMethod("queueStep",
                QueueTicket.class, List.class, String.class, String.class);
        method.setAccessible(true);
        Department lab = Department.builder().departmentId(UUID.randomUUID())
                .name("Xét nghiệm huyết học").roomCode("LAB-201").build();
        QueueTicket queue = QueueTicket.builder().ticketId(UUID.randomUUID()).department(lab)
                .status(QueueStatus.WAITING).queueNumber(5).build();
        MedicalService rbc = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("AN-CBC-RBC").name("Số lượng hồng cầu (RBC)").department(lab).build();
        MedicalService hgb = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("AN-CBC-HGB").name("Huyết sắc tố (HGB)").department(lab).build();
        TestRequest first = TestRequest.builder().testRequestId(UUID.randomUUID()).queueTicket(queue)
                .service(rbc).status(TestRequestStatus.PENDING).build();
        TestRequest second = TestRequest.builder().testRequestId(UUID.randomUUID()).queueTicket(queue)
                .service(hgb).status(TestRequestStatus.PENDING).build();

        PatientJourneyResponse.Step step = (PatientJourneyResponse.Step) method.invoke(
                patientJourneyService, queue, List.of(first, second),
                "PARACLINICAL", QueueStatus.WAITING.name());

        assertEquals("Công thức máu (2 chỉ số)", step.serviceName());
        assertEquals(1, step.services().size());
        assertEquals("LAB-001", step.services().get(0).serviceCode());
        assertEquals(1, step.totalServices());
    }

    @Test
    void scopeFilteringCoversNullTodayOverdueTerminalAndMissingCheckIn() throws Exception {
        Method normalize = PatientJourneyService.class.getDeclaredMethod("normalizeScope", String.class);
        normalize.setAccessible(true);
        assertEquals("ALL", normalize.invoke(patientJourneyService, new Object[]{null}));
        assertEquals("ALL", normalize.invoke(patientJourneyService, "  "));
        assertEquals("TODAY", normalize.invoke(patientJourneyService, " today "));
        assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> normalize.invoke(patientJourneyService, "future"));

        Method matches = PatientJourneyService.class.getDeclaredMethod(
                "matchesScope", PatientJourneyResponse.class, String.class);
        matches.setAccessible(true);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        PatientJourneyResponse current = journeyAt(today.atTime(8, 0), "WAITING");
        PatientJourneyResponse overdue = journeyAt(today.minusDays(2).atTime(8, 0), "WAITING");
        PatientJourneyResponse completed = journeyAt(today.minusDays(2).atTime(8, 0), "COMPLETED");
        PatientJourneyResponse undatedActive = journeyAt(null, "WAITING");
        PatientJourneyResponse undatedTerminal = journeyAt(null, "CANCELLED");
        assertTrue((boolean) matches.invoke(patientJourneyService, current, "ALL"));
        assertTrue((boolean) matches.invoke(patientJourneyService, current, "TODAY"));
        assertFalse((boolean) matches.invoke(patientJourneyService, current, "OVERDUE"));
        assertTrue((boolean) matches.invoke(patientJourneyService, overdue, "OVERDUE"));
        assertFalse((boolean) matches.invoke(patientJourneyService, completed, "OVERDUE"));
        assertTrue((boolean) matches.invoke(patientJourneyService, undatedActive, "OVERDUE"));
        assertFalse((boolean) matches.invoke(patientJourneyService, undatedTerminal, "OVERDUE"));
        assertFalse((boolean) matches.invoke(patientJourneyService, undatedActive, "TODAY"));
    }

    @Test
    void rankCurrentTicketCoversMissingInputsUnavailableRankingAndMatchingTicket() throws Exception {
        Method method = PatientJourneyService.class.getDeclaredMethod("rankCurrentTicket", QueueTicket.class);
        method.setAccessible(true);
        assertNull(method.invoke(patientJourneyService, new Object[]{null}));

        QueueTicket ticket = QueueTicket.builder().ticketId(UUID.randomUUID()).build();
        assertNull(method.invoke(patientJourneyService, ticket));
        ticket.setDepartment(Department.builder().departmentId(UUID.randomUUID()).build());
        assertNull(method.invoke(patientJourneyService, ticket));
        ticket.setWorkDate(LocalDate.now());

        when(queueRepo.findWaitingPrioritized(eq(ticket.getDepartment().getDepartmentId()), eq(ticket.getWorkDate()),
                anyList(), any())).thenReturn(null);
        assertNull(method.invoke(patientJourneyService, ticket));

        when(queueRepo.findWaitingPrioritized(eq(ticket.getDepartment().getDepartmentId()), eq(ticket.getWorkDate()),
                anyList(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(ticket)));
        when(queuePriorityService.rank(anyList())).thenReturn(null);
        assertNull(method.invoke(patientJourneyService, ticket));

        QueueTicket other = QueueTicket.builder().ticketId(UUID.randomUUID()).build();
        QueuePriorityService.PriorityInfo priority = new QueuePriorityService.PriorityInfo(
                QueuePriorityService.REGULAR, "Khách trực tiếp", null, false);
        when(queuePriorityService.rank(anyList())).thenReturn(List.of(
                new QueuePriorityService.RankedTicket(other, 1, true, priority)));
        assertNull(method.invoke(patientJourneyService, ticket));

        QueuePriorityService.RankedTicket expected = new QueuePriorityService.RankedTicket(ticket, 2, true, priority);
        when(queuePriorityService.rank(anyList())).thenReturn(List.of(expected));
        assertSame(expected, method.invoke(patientJourneyService, ticket));
    }

    @Test
    void physicalCurrentPrefersProgressThenCalledThenOldestWaitingAndHandlesEmpty() throws Exception {
        Method method = PatientJourneyService.class.getDeclaredMethod("physicalCurrent", List.class);
        method.setAccessible(true);
        assertNull(method.invoke(patientJourneyService, List.of()));
        PatientJourneyResponse.Step done = journeyStepWithStatus("DONE", null, QueueStatus.DONE.name());
        assertNull(method.invoke(patientJourneyService, List.of(done)));

        LocalDateTime now = LocalDateTime.now();
        PatientJourneyResponse.Step waitingLate = journeyStepWithStatus("W2", now, QueueStatus.WAITING.name());
        PatientJourneyResponse.Step waitingEarly = journeyStepWithStatus("W1", now.minusMinutes(5), QueueStatus.WAITING.name());
        assertSame(waitingEarly, method.invoke(patientJourneyService, List.of(waitingLate, waitingEarly)));

        PatientJourneyResponse.Step called = journeyStepWithStatus("C", null, QueueStatus.CALLED.name());
        assertSame(called, method.invoke(patientJourneyService, List.of(waitingEarly, called)));
        PatientJourneyResponse.Step progress = journeyStepWithStatus("P", now.plusMinutes(5), QueueStatus.IN_PROGRESS.name());
        assertSame(progress, method.invoke(patientJourneyService, List.of(called, progress, waitingEarly)));
    }

    @Test
    void emptyAndSkippedPatientQueuesCoverMissingRoomTodayAndExpiredTicket() throws Exception {
        Method empty = PatientJourneyService.class.getDeclaredMethod("emptyQueue", UUID.class, String.class);
        Method skipped = PatientJourneyService.class.getDeclaredMethod("skippedQueue", UUID.class, QueueTicket.class);
        empty.setAccessible(true);
        skipped.setAccessible(true);
        UUID visitId = UUID.randomUUID();
        var emptyResponse = (vn.edu.fpt.cares.dto.journey.PatientQueueResponse)
                empty.invoke(patientJourneyService, visitId, "COMPLETED");
        assertEquals("COMPLETED", emptyResponse.currentStatus());
        assertTrue(emptyResponse.waiting().isEmpty());

        QueueTicket expired = QueueTicket.builder().ticketId(UUID.randomUUID()).status(QueueStatus.SKIPPED)
                .queueNumber(12).workDate(null).build();
        var expiredResponse = (vn.edu.fpt.cares.dto.journey.PatientQueueResponse)
                skipped.invoke(patientJourneyService, visitId, expired);
        assertEquals("EXPIRED", expiredResponse.returnRequestStatus());
        assertNull(expiredResponse.roomName());

        Department room = Department.builder().departmentId(UUID.randomUUID()).name("Phòng Nội")
                .roomCode("INT-103").build();
        expired.setDepartment(room);
        expired.setWorkDate(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        var todayResponse = (vn.edu.fpt.cares.dto.journey.PatientQueueResponse)
                skipped.invoke(patientJourneyService, visitId, expired);
        assertEquals("NONE", todayResponse.returnRequestStatus());
        assertEquals("Phòng Nội", todayResponse.roomName());
        assertEquals("INT-103", todayResponse.roomCode());
    }

    private PatientJourneyResponse.Step journeyStepWithStatus(
            String id, LocalDateTime startedAt, String status) {
        return new PatientJourneyResponse.Step(id, "EXAMINATION", "Khám", null, null, null,
                status, startedAt, null, List.of(), 0, 0,
                "EXAMINATION", null, null, null);
    }

    private PatientJourneyResponse journeyAt(LocalDateTime checkedIn, String status) {
        return new PatientJourneyResponse(UUID.randomUUID(), "VIS", "Bệnh nhân", "0900000000", false,
                "Bước", "Phòng", status, "-", null, null, checkedIn, 0, false, List.of(),
                null, null, null, null, null, null);
    }

    @Test
    void queueForCustomer_ShouldRejectMissingVisitBeforeReadingJourney() {
        UUID visitId = UUID.randomUUID();
        when(visitRepo.findById(visitId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> patientJourneyService.queueForCustomer(visitId, List.of(UUID.randomUUID())));
        verifyNoInteractions(queueRepo, testRepo, invoiceRepo, recordRepo, queuePriorityService);
    }

    @Test
    void queueForCustomer_ShouldRejectVisitWithoutRegisteredCustomer() {
        UUID visitId = UUID.randomUUID();
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(
                CustomerVisit.builder().visitId(visitId).build()));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> patientJourneyService.queueForCustomer(visitId, List.of(UUID.randomUUID())));
        verifyNoInteractions(queueRepo, testRepo, invoiceRepo, recordRepo, queuePriorityService);
    }

    @Test
    void aggregateTestStatus_ShouldCoverAllTerminalAndMixedBranches() {
        java.util.function.Function<TestRequestStatus, TestRequest> request = status ->
                TestRequest.builder().status(status).build();
        assertEquals("COMPLETED", ReflectionTestUtils.invokeMethod(patientJourneyService,
                "aggregateTestStatus", List.of(request.apply(TestRequestStatus.COMPLETED))));
        assertEquals("IN_PROGRESS", ReflectionTestUtils.invokeMethod(patientJourneyService,
                "aggregateTestStatus", List.of(request.apply(TestRequestStatus.CANCELLED),
                        request.apply(TestRequestStatus.IN_PROGRESS))));
        assertEquals("BLOCKED", ReflectionTestUtils.invokeMethod(patientJourneyService,
                "aggregateTestStatus", List.of(request.apply(TestRequestStatus.BLOCKED),
                        request.apply(TestRequestStatus.BLOCKED))));
        assertEquals("PENDING", ReflectionTestUtils.invokeMethod(patientJourneyService,
                "aggregateTestStatus", List.of(request.apply(TestRequestStatus.CANCELLED),
                        request.apply(TestRequestStatus.PENDING))));
        assertEquals("CANCELLED", ReflectionTestUtils.invokeMethod(patientJourneyService,
                "aggregateTestStatus", List.of(request.apply(TestRequestStatus.CANCELLED))));
    }

    @Test
    void returnStep_ShouldCoverInvoiceTestCycleAndExaminationStatusMatrix() {
        QueueTicket examination = QueueTicket.builder().ticketId(UUID.randomUUID())
                .status(QueueStatus.WAITING).queueNumber(3).build();
        Invoice pending = Invoice.builder().invoiceId(UUID.randomUUID()).status(InvoiceStatus.PENDING).build();
        Invoice cancelled = Invoice.builder().invoiceId(UUID.randomUUID()).status(InvoiceStatus.CANCELLED).build();
        TestRequest pendingTest = TestRequest.builder().status(TestRequestStatus.PENDING).build();
        TestRequest completed = TestRequest.builder().status(TestRequestStatus.COMPLETED).build();

        PatientJourneyResponse.Step step = ReflectionTestUtils.invokeMethod(patientJourneyService,
                "returnStep", examination, pending, 1, List.of(), true);
        assertEquals("BLOCKED", step.status());
        step = ReflectionTestUtils.invokeMethod(patientJourneyService,
                "returnStep", examination, cancelled, 1, List.of(completed), true);
        assertEquals("CANCELLED", step.status());
        step = ReflectionTestUtils.invokeMethod(patientJourneyService,
                "returnStep", examination, null, 1, List.of(pendingTest), true);
        assertEquals("BLOCKED", step.status());
        step = ReflectionTestUtils.invokeMethod(patientJourneyService,
                "returnStep", examination, null, 2, List.of(completed), false);
        assertEquals("DONE", step.status());
        examination.setStatus(QueueStatus.CALLED);
        step = ReflectionTestUtils.invokeMethod(patientJourneyService,
                "returnStep", examination, null, 2, List.of(completed), true);
        assertEquals("CALLED", step.status());
        examination.setStatus(QueueStatus.WAITING);
        step = ReflectionTestUtils.invokeMethod(patientJourneyService,
                "returnStep", examination, null, 2, List.of(completed), true);
        assertEquals("TEST_DONE", step.status());
    }

    @Test
    void examinationGroupStep_ShouldCoverGroupingFallbacksAndParaclinicalHandoff() {
        QueueTicket missingService = QueueTicket.builder().ticketId(UUID.randomUUID())
                .status(QueueStatus.DONE).queueNumber(5).build();
        QueueTicket waiting = QueueTicket.builder().ticketId(UUID.randomUUID())
                .status(QueueStatus.WAITING_FOR_TEST).queueNumber(2)
                .service(MedicalService.builder().serviceId(UUID.randomUUID())
                        .serviceCode("EX-01").name("Khám Nội").build())
                .department(Department.builder().name("Nội").roomCode("INT-1").build()).build();
        LocalDateTime completedAt = LocalDateTime.now().minusMinutes(1);
        missingService.setCompletedAt(completedAt);

        PatientJourneyResponse.Step grouped = ReflectionTestUtils.invokeMethod(patientJourneyService,
                "examinationGroupStep", List.of(missingService, waiting), waiting, false);
        assertEquals("DONE", grouped.status());
        assertEquals(2, grouped.totalServices());
        assertEquals(2, grouped.completedServices());
        assertTrue(grouped.serviceName().startsWith("2 dịch vụ:"));
        assertEquals(2, grouped.queueNumber());

        waiting.setStatus(QueueStatus.WAITING);
        PatientJourneyResponse.Step active = ReflectionTestUtils.invokeMethod(patientJourneyService,
                "examinationGroupStep", List.of(waiting), waiting, false);
        assertEquals("WAITING", active.status());
        assertEquals(0, active.completedServices());
        assertNull(active.completedAt());

        PatientJourneyResponse.Step forced = ReflectionTestUtils.invokeMethod(patientJourneyService,
                "examinationGroupStep", List.of(waiting), waiting, true);
        assertEquals("DONE", forced.status());
        assertEquals(1, forced.completedServices());
    }

    @Test
    void journeyServiceNameAndServiceProgress_ShouldCoverEmptySingleDistinctAndMissingService() {
        assertEquals("Cận lâm sàng", ReflectionTestUtils.invokeMethod(patientJourneyService,
                "journeyServiceName", List.of()));
        PatientJourneyResponse.ServiceProgress one = new PatientJourneyResponse.ServiceProgress(
                UUID.randomUUID(), "LAB-1", "Xét nghiệm máu", "PENDING");
        assertEquals("Xét nghiệm máu", ReflectionTestUtils.invokeMethod(patientJourneyService,
                "journeyServiceName", List.of(one)));
        PatientJourneyResponse.ServiceProgress duplicate = new PatientJourneyResponse.ServiceProgress(
                UUID.randomUUID(), "LAB-2", "Xét nghiệm máu", "COMPLETED");
        assertEquals("2 dịch vụ: Xét nghiệm máu", ReflectionTestUtils.invokeMethod(patientJourneyService,
                "journeyServiceName", List.of(one, duplicate)));

        TestRequest missing = TestRequest.builder().status(TestRequestStatus.CANCELLED).build();
        PatientJourneyResponse.ServiceProgress fallback = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "serviceProgress", missing);
        assertNull(fallback.serviceId());
        assertEquals("Cận lâm sàng", fallback.serviceName());
        TestRequest configured = TestRequest.builder().status(TestRequestStatus.COMPLETED)
                .service(MedicalService.builder().serviceId(UUID.randomUUID())
                        .serviceCode("LAB-X").name("Sinh hóa").build()).build();
        PatientJourneyResponse.ServiceProgress mapped = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "serviceProgress", configured);
        assertEquals("LAB-X", mapped.serviceCode());
        assertEquals("COMPLETED", mapped.status());
    }

    @Test
    void standaloneGroupingKeys_ShouldCoverInvoiceDepartmentAndFallbackCombinations() {
        UUID invoiceId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        TestRequest fullyLinked = TestRequest.builder().testRequestId(UUID.randomUUID())
                .invoiceItem(InvoiceItem.builder().invoice(Invoice.builder().invoiceId(invoiceId).build()).build())
                .performingDepartment(Department.builder().departmentId(departmentId).build()).build();
        assertEquals(invoiceId + ":" + departmentId, ReflectionTestUtils.invokeMethod(
                patientJourneyService, "standaloneTestGroupKey", fullyLinked));

        TestRequest noInvoice = TestRequest.builder().testRequestId(UUID.randomUUID())
                .performingDepartment(Department.builder().build()).build();
        assertEquals("NO_INVOICE:UNASSIGNED", ReflectionTestUtils.invokeMethod(
                patientJourneyService, "standaloneTestGroupKey", noInvoice));
        assertEquals("UNASSIGNED", ReflectionTestUtils.invokeMethod(
                patientJourneyService, "performingDepartmentKey", TestRequest.builder().build()));
        assertEquals("UNASSIGNED", ReflectionTestUtils.invokeMethod(
                patientJourneyService, "performingDepartmentKey", noInvoice));
    }

    @Test
    void queueStep_ShouldCoverNullEmptyServiceDepartmentAndCompletedProgressBranches() {
        UUID ticketId = UUID.randomUUID();
        QueueTicket empty = QueueTicket.builder().ticketId(ticketId).queueNumber(9)
                .status(QueueStatus.WAITING).build();
        empty.setCreatedAt(LocalDateTime.now());
        PatientJourneyResponse.Step fallback = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "queueStep", empty, null, "EXAMINATION", "WAITING");
        assertEquals("Khám bệnh", fallback.serviceName());
        assertTrue(fallback.services().isEmpty());
        assertNull(fallback.roomName());

        Department room = Department.builder().departmentId(UUID.randomUUID())
                .name("Phòng Nội").roomCode("INT-101").build();
        MedicalService service = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("EX-01").name("Khám Nội").build();
        QueueTicket configured = QueueTicket.builder().ticketId(UUID.randomUUID()).queueNumber(10)
                .status(QueueStatus.DONE).department(room).service(service)
                .completedAt(LocalDateTime.now()).build();
        configured.setCreatedAt(LocalDateTime.now());
        PatientJourneyResponse.Step single = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "queueStep", configured, List.of(), "EXAMINATION", "DONE");
        assertEquals("Khám Nội", single.serviceName());
        assertEquals(1, single.completedServices());
        assertEquals("Phòng Nội", single.roomName());

        TestRequest completed = TestRequest.builder().testRequestId(UUID.randomUUID())
                .service(MedicalService.builder().serviceId(UUID.randomUUID())
                        .serviceCode("LAB-X").name("Xét nghiệm X").build())
                .status(TestRequestStatus.COMPLETED).build();
        completed.setCreatedAt(LocalDateTime.now());
        PatientJourneyResponse.Step withRequest = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "queueStep", configured, List.of(completed),
                "PARACLINICAL", "DONE");
        assertEquals(1, withRequest.completedServices());
        assertEquals("Phòng Nội", withRequest.roomName());
    }

    @Test
    void buildPatientQueue_ShouldCoverSkippedCancelledAndCompletedEarlyExitPaths() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId)
                .status(vn.edu.fpt.cares.enums.VisitStatus.CHECKED_IN).build();
        QueueTicket lateSkipped = QueueTicket.builder().ticketId(UUID.randomUUID())
                .status(QueueStatus.SKIPPED).build();
        lateSkipped.setCreatedAt(LocalDateTime.now());
        QueueTicket earlySkipped = QueueTicket.builder().ticketId(UUID.randomUUID())
                .status(QueueStatus.SKIPPED).build();
        earlySkipped.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(lateSkipped, earlySkipped));
        PatientQueueResponse skipped = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "buildPatientQueue", visit);
        assertEquals("SKIPPED", skipped.currentStatus());

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        visit.setStatus(vn.edu.fpt.cares.enums.VisitStatus.CANCELLED);
        PatientQueueResponse cancelled = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "buildPatientQueue", visit);
        assertEquals("CANCELLED", cancelled.currentStatus());

        visit.setStatus(vn.edu.fpt.cares.enums.VisitStatus.COMPLETED);
        PatientQueueResponse completed = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "buildPatientQueue", visit);
        assertEquals("COMPLETED", completed.currentStatus());
    }

    @Test
    void buildPatientQueue_ShouldHandleNoActiveStepTicketAndMissingOwnedTicket() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        visit.setStatus(VisitStatus.CHECKED_IN);

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        PatientQueueResponse noActive = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "buildPatientQueue", visit);
        assertNotNull(noActive);

        reset(queueRepo);
        Department clinic = Department.builder().departmentId(UUID.randomUUID())
                .departmentType(DepartmentType.EXAMINATION).name("Phòng Nội").build();
        Department lab = paraclinicalDepartment("Phòng xét nghiệm", "LAB-1");
        QueueTicket withoutId = QueueTicket.builder().visit(visit).department(lab)
                .service(medicalService("Đường huyết")).workDate(LocalDate.now())
                .queueNumber(1).status(QueueStatus.TEST_DONE).build();
        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(), List.of(withoutId));
        PatientQueueResponse noTicketId = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "buildPatientQueue", visit);
        assertEquals("TEST_DONE", noTicketId.currentStatus());

        reset(queueRepo);
        QueueTicket ticket = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit)
                .department(lab).service(medicalService("Đường huyết"))
                .workDate(LocalDate.now()).queueNumber(2).status(QueueStatus.TEST_DONE).build();
        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(), List.of(ticket), List.of());
        PatientQueueResponse missingOwned = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "buildPatientQueue", visit);
        assertEquals("TEST_DONE", missingOwned.currentStatus());
    }

    @Test
    void get_ShouldBuildCompleteClinicalCycleWithCancelledSharedPrebookedAndLegacyTests() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        visit.setStatus(VisitStatus.CHECKED_IN);

        Department clinic = Department.builder().departmentId(UUID.randomUUID())
                .departmentType(DepartmentType.EXAMINATION).name("Phòng Nội").roomCode("INT-1").build();
        Department lab = paraclinicalDepartment("Xét nghiệm", "LAB-1");
        QueueTicket exam = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit)
                .department(clinic).service(medicalService("Khám Nội"))
                .workDate(LocalDate.now()).queueNumber(1).status(QueueStatus.DONE).build();
        exam.setCreatedAt(LocalDateTime.now().minusHours(2));
        MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID())
                .visit(visit).queueTicket(exam).build();

        Invoice initial = Invoice.builder().invoiceId(UUID.randomUUID()).visit(visit)
                .status(InvoiceStatus.PAID).build();
        initial.setCreatedAt(LocalDateTime.now().minusHours(3));
        Invoice clinical = Invoice.builder().invoiceId(UUID.randomUUID()).visit(visit)
                .medicalRecord(record).status(InvoiceStatus.PAID).build();
        clinical.setCreatedAt(LocalDateTime.now().minusHours(1));
        Invoice cancelled = Invoice.builder().invoiceId(UUID.randomUUID()).visit(visit)
                .medicalRecord(record).status(InvoiceStatus.CANCELLED).build();
        cancelled.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        QueueTicket labQueue = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit)
                .department(lab).service(medicalService("Công thức máu"))
                .workDate(LocalDate.now()).queueNumber(2).status(QueueStatus.WAITING).build();
        labQueue.setCreatedAt(LocalDateTime.now().minusMinutes(45));

        java.util.function.BiFunction<Invoice, MedicalRecord, InvoiceItem> item = (invoice, owner) ->
                InvoiceItem.builder().itemId(UUID.randomUUID()).invoice(invoice).build();
        TestRequest direct = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record)
                .invoiceItem(item.apply(clinical, record)).queueTicket(labQueue)
                .performingDepartment(lab).service(medicalService("RBC"))
                .status(TestRequestStatus.PENDING).build();
        direct.setCreatedAt(LocalDateTime.now().minusMinutes(50));

        MedicalRecord otherRecord = MedicalRecord.builder().recordId(UUID.randomUUID()).visit(visit).build();
        TestRequest shared = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(otherRecord)
                .invoiceItem(item.apply(initial, otherRecord)).queueTicket(labQueue)
                .performingDepartment(lab).service(medicalService("HGB"))
                .status(TestRequestStatus.PENDING).build();
        shared.setCreatedAt(LocalDateTime.now().minusMinutes(49));

        TestRequest prebooked = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(otherRecord)
                .invoiceItem(item.apply(initial, otherRecord)).performingDepartment(lab)
                .service(medicalService("Đường huyết")).status(TestRequestStatus.IN_PROGRESS).build();
        prebooked.setCreatedAt(LocalDateTime.now().minusHours(2));

        TestRequest legacy = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record)
                .invoiceItem(item.apply(initial, record)).performingDepartment(lab)
                .service(medicalService("AST")).status(TestRequestStatus.PENDING).build();
        legacy.setCreatedAt(LocalDateTime.now().minusMinutes(40));
        TestRequest legacyCompanion = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(otherRecord)
                .invoiceItem(item.apply(initial, otherRecord)).performingDepartment(lab)
                .service(medicalService("ALT")).status(TestRequestStatus.PENDING).build();
        legacyCompanion.setCreatedAt(LocalDateTime.now().minusMinutes(39));

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(invoiceRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(initial, clinical, cancelled));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of(direct, shared, prebooked, legacy, legacyCompanion));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(exam, labQueue));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertFalse(result.steps().isEmpty());
        assertTrue(result.steps().stream().anyMatch(step -> "CANCELLED".equals(step.status())));
        assertTrue(result.steps().stream().anyMatch(step -> "PARACLINICAL".equals(step.kind())));
    }

    @Test
    void groupingHelpers_ShouldCoverWholePanelAnalyteSnapshotAndMissingServiceBranches() {
        MedicalService wholePanel = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("LAB-001").name("Công thức máu").build();
        MedicalService analyte = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("AN-CBC-RBC").name("RBC").build();
        MedicalService ordinary = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("IMG-X").name("Chụp X").build();
        TestRequest panelRequest = TestRequest.builder().testRequestId(UUID.randomUUID())
                .service(wholePanel).status(TestRequestStatus.COMPLETED).build();
        TestRequest analyteRequest = TestRequest.builder().testRequestId(UUID.randomUUID())
                .service(analyte).status(TestRequestStatus.PENDING).build();
        TestRequest ordinaryRequest = TestRequest.builder().testRequestId(UUID.randomUUID())
                .service(ordinary).status(TestRequestStatus.BLOCKED).build();
        TestRequest missingService = TestRequest.builder().testRequestId(UUID.randomUUID())
                .status(TestRequestStatus.CANCELLED).build();

        @SuppressWarnings("unchecked")
        List<PatientJourneyResponse.ServiceProgress> actual = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "groupedServiceProgress",
                List.of(panelRequest, analyteRequest, ordinaryRequest, missingService));
        assertEquals(3, actual.size());
        assertTrue(actual.stream().anyMatch(item -> "LAB-001".equals(item.serviceCode())
                && "Công thức máu".equals(item.serviceName())));
        assertTrue(actual.stream().anyMatch(item -> "IMG-X".equals(item.serviceCode())));

        InvoiceItem panelItem = InvoiceItem.builder().itemId(UUID.randomUUID()).service(wholePanel)
                .serviceCodeSnapshot("LAB-001").serviceSnapshot("Công thức máu").build();
        InvoiceItem analyteSnapshot = InvoiceItem.builder().itemId(UUID.randomUUID())
                .serviceCodeSnapshot("AN-CBC-RBC").serviceSnapshot("RBC").build();
        InvoiceItem ordinarySnapshot = InvoiceItem.builder().itemId(UUID.randomUUID())
                .serviceCodeSnapshot("IMG-X").serviceSnapshot("Chụp X").build();
        @SuppressWarnings("unchecked")
        List<PatientJourneyResponse.ServiceProgress> planned = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "groupedPlannedServiceProgress",
                List.of(panelItem, analyteSnapshot, ordinarySnapshot));
        assertEquals(2, planned.size());
        assertTrue(planned.stream().allMatch(item -> "BLOCKED".equals(item.status())));
        assertTrue(planned.stream().anyMatch(item -> "LAB-001".equals(item.serviceCode())
                && "Công thức máu".equals(item.serviceName())));
    }

    @Test
    void groupingHelpers_ShouldCoverCompleteAnalytePanelAndNullIdentifiers() {
        var panel = LaboratoryAnalyteCatalog.panel("LAB-001").orElseThrow();
        List<TestRequest> requests = new java.util.ArrayList<>();
        List<InvoiceItem> items = new java.util.ArrayList<>();
        for (LaboratoryAnalyteCatalog.Analyte analyte : panel.analytes()) {
            String analyteCode = analyte.serviceCode();
            MedicalService service = MedicalService.builder().serviceId(UUID.randomUUID())
                    .serviceCode(analyteCode).name(analyteCode).build();
            requests.add(TestRequest.builder().testRequestId(UUID.randomUUID()).service(service)
                    .status(TestRequestStatus.COMPLETED).build());
            items.add(InvoiceItem.builder().itemId(UUID.randomUUID()).service(service)
                    .serviceCodeSnapshot(analyteCode).serviceSnapshot(analyteCode).build());
        }
        @SuppressWarnings("unchecked")
        List<PatientJourneyResponse.ServiceProgress> grouped = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "groupedServiceProgress", requests);
        @SuppressWarnings("unchecked")
        List<PatientJourneyResponse.ServiceProgress> planned = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "groupedPlannedServiceProgress", items);
        assertEquals(panel.name(), grouped.get(0).serviceName());
        assertEquals(panel.name(), planned.get(0).serviceName());

        MedicalService noId = MedicalService.builder().serviceCode("CUSTOM").name("Tùy chỉnh").build();
        TestRequest requestWithoutId = TestRequest.builder().testRequestId(UUID.randomUUID())
                .service(noId).status(TestRequestStatus.CANCELLED).build();
        InvoiceItem itemWithoutId = InvoiceItem.builder().itemId(UUID.randomUUID()).service(noId)
                .serviceCodeSnapshot("CUSTOM").serviceSnapshot("Tùy chỉnh").build();
        assertEquals(1, ((List<?>) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "groupedServiceProgress", List.of(requestWithoutId))).size());
        assertEquals(1, ((List<?>) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "groupedPlannedServiceProgress", List.of(itemWithoutId))).size());
    }

    @Test
    void cycleAndReturnSteps_ShouldCoverLegacyInvoiceRoomAndFallbackMetadata() {
        UUID ticketId = UUID.randomUUID();
        QueueTicket queue = QueueTicket.builder().ticketId(ticketId).queueNumber(7)
                .status(QueueStatus.DONE).build();
        TestRequest done = TestRequest.builder().testRequestId(UUID.randomUUID())
                .status(TestRequestStatus.COMPLETED).build();
        done.setCreatedAt(LocalDateTime.now());
        PatientJourneyResponse.Step legacy = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "cycleQueueStep", queue, List.of(done), null, 3);
        assertTrue(legacy.id().contains("LEGACY-" + ticketId));
        assertNull(legacy.roomName());
        assertEquals(1, legacy.completedServices());
        assertNull(legacy.invoiceId());

        Department room = Department.builder().departmentId(UUID.randomUUID())
                .name("Phòng xét nghiệm").roomCode("LAB-1").build();
        queue.setDepartment(room);
        Invoice invoice = Invoice.builder().invoiceId(UUID.randomUUID()).build();
        TestRequest pending = TestRequest.builder().testRequestId(UUID.randomUUID())
                .status(TestRequestStatus.PENDING).build();
        pending.setCreatedAt(LocalDateTime.now().plusSeconds(1));
        PatientJourneyResponse.Step linked = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "cycleQueueStep", queue, List.of(done, pending), invoice, 4);
        assertEquals("Phòng xét nghiệm", linked.roomName());
        assertEquals(invoice.getInvoiceId(), linked.invoiceId());
        assertEquals(1, linked.completedServices());

        queue.setService(null);
        PatientJourneyResponse.Step returned = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "returnStep", queue, invoice, 4, List.of(done), true);
        assertTrue(returned.serviceName().endsWith("· bác sĩ"));
        assertEquals("Phòng xét nghiệm", returned.roomName());
    }

    @Test
    void workflowOrder_ShouldCoverMissingRoomServicePriorityCodeAndCreatedDate() {
        @SuppressWarnings("unchecked")
        java.util.Comparator<QueueTicket> comparator = ReflectionTestUtils.invokeMethod(
                patientJourneyService, "workflowOrder");
        Department examination = Department.builder().departmentType(DepartmentType.EXAMINATION).build();
        Department lab = Department.builder().departmentType(DepartmentType.LABORATORY).build();
        MedicalService high = MedicalService.builder().serviceCode("B").workflowPriority(10).build();
        MedicalService low = MedicalService.builder().serviceCode("A").workflowPriority(null).build();
        QueueTicket exam = QueueTicket.builder().department(examination).service(low).build();
        QueueTicket para = QueueTicket.builder().department(lab).service(high).build();
        QueueTicket missing = QueueTicket.builder().build();
        exam.setCreatedAt(LocalDateTime.now());
        para.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        assertTrue(comparator.compare(exam, para) < 0);
        assertTrue(comparator.compare(para, missing) < 0);
        assertNotEquals(0, comparator.compare(exam, missing));
        assertEquals(0, comparator.compare(missing, QueueTicket.builder().build()));
    }

    @Test
    void addLegacyParaclinicalSteps_ShouldGroupQueuedAndQueueLessRequestsWithoutDuplicates() {
        QueueTicket examination = QueueTicket.builder().ticketId(UUID.randomUUID()).build();
        Department lab = Department.builder().departmentId(UUID.randomUUID())
                .name("Xét nghiệm").roomCode("LAB-1").build();
        QueueTicket labQueue = QueueTicket.builder().ticketId(UUID.randomUUID())
                .department(lab).status(QueueStatus.WAITING).queueNumber(8).build();
        TestRequest queued = TestRequest.builder().testRequestId(UUID.randomUUID())
                .queueTicket(labQueue).performingDepartment(lab)
                .status(TestRequestStatus.PENDING).build();
        TestRequest withoutQueue = TestRequest.builder().testRequestId(UUID.randomUUID())
                .performingDepartment(lab).status(TestRequestStatus.COMPLETED).build();
        queued.setCreatedAt(LocalDateTime.now());
        withoutQueue.setCreatedAt(LocalDateTime.now());
        List<PatientJourneyResponse.Step> steps = new java.util.ArrayList<>();
        java.util.Set<UUID> added = new java.util.HashSet<>();

        ReflectionTestUtils.invokeMethod(patientJourneyService, "addLegacyParaclinicalSteps",
                steps, examination, 2, List.of(queued, withoutQueue), added);
        assertEquals(2, steps.size());
        assertTrue(added.contains(labQueue.getTicketId()));
        assertTrue(steps.stream().anyMatch(step -> step.id().startsWith("LEGACY:")));

        List<PatientJourneyResponse.Step> duplicate = new java.util.ArrayList<>();
        ReflectionTestUtils.invokeMethod(patientJourneyService, "addLegacyParaclinicalSteps",
                duplicate, examination, 3, List.of(queued), added);
        assertTrue(duplicate.isEmpty());
    }

    @Test
    void paymentStep_ShouldCoverPaidCancelledAndPendingPresentation() {
        for (InvoiceStatus status : InvoiceStatus.values()) {
            Invoice invoice = Invoice.builder().invoiceId(UUID.randomUUID()).status(status).build();
            invoice.setCreatedAt(LocalDateTime.now().minusMinutes(2));
            invoice.setUpdatedAt(LocalDateTime.now());
            PatientJourneyResponse.Step step = ReflectionTestUtils.invokeMethod(
                    patientJourneyService, "paymentStep", invoice, "Thanh toán", "PAYMENT", 1);
            if (status == InvoiceStatus.PAID) {
                assertEquals("DONE", step.status());
                assertEquals(1, step.completedServices());
                assertNotNull(step.completedAt());
            } else if (status == InvoiceStatus.CANCELLED) {
                assertEquals("CANCELLED", step.status());
                assertNotNull(step.completedAt());
            } else {
                assertEquals("PAYMENT_PENDING", step.status());
                assertNull(step.completedAt());
            }
        }
    }

    @Test
    void relationshipPredicates_ShouldCoverEachMissingLinkAndMatchingLink() {
        UUID ticketId = UUID.randomUUID();
        QueueTicket examination = QueueTicket.builder().ticketId(ticketId).build();
        QueueTicket other = QueueTicket.builder().ticketId(UUID.randomUUID()).build();

        Invoice invoice = Invoice.builder().build();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "belongsToExamination", invoice, examination));
        invoice.setMedicalRecord(MedicalRecord.builder().build());
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "belongsToExamination", invoice, examination));
        invoice.getMedicalRecord().setQueueTicket(other);
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "belongsToExamination", invoice, examination));
        invoice.getMedicalRecord().setQueueTicket(examination);
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "belongsToExamination", invoice, examination));

        TestRequest test = TestRequest.builder().build();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "belongsToExamination", test, examination));
        test.setMedicalRecord(MedicalRecord.builder().build());
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "belongsToExamination", test, examination));
        test.getMedicalRecord().setQueueTicket(other);
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "belongsToExamination", test, examination));
        test.getMedicalRecord().setQueueTicket(examination);
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "belongsToExamination", test, examination));

        assertTrue(((Optional<?>) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "invoiceIdOf", (Object) null)).isEmpty());
        assertTrue(((Optional<?>) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "invoiceIdOf", TestRequest.builder().build())).isEmpty());
        assertTrue(((Optional<?>) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "invoiceIdOf", TestRequest.builder().invoiceItem(InvoiceItem.builder().build()).build())).isEmpty());
        UUID invoiceId = UUID.randomUUID();
        TestRequest linked = TestRequest.builder().invoiceItem(InvoiceItem.builder()
                .invoice(Invoice.builder().invoiceId(invoiceId).build()).build()).build();
        assertEquals(invoiceId, ((Optional<?>) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "invoiceIdOf", linked)).orElseThrow());

        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "isExaminationQueue", QueueTicket.builder().build()));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "isExaminationQueue", QueueTicket.builder().department(
                        Department.builder().departmentType(DepartmentType.LABORATORY).build()).build()));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(patientJourneyService,
                "isExaminationQueue", QueueTicket.builder().department(
                        Department.builder().departmentType(DepartmentType.EXAMINATION).build()).build()));
    }
}
