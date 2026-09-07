package org.example.doansummer2026.service;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.journey.PatientJourneyResponse;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.InvoiceItem;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.example.doansummer2026.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
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
    void activateNext_ShouldThrow_WhenVisitDoesNotExistEvenIfJourneyFinished() {

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
        assertThrows(org.example.doansummer2026.exception.BadRequestException.class,
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
    void skippedQueue_ShouldExposeSameDayPendingAndExpiredVariants() throws Exception {
        Method method = PatientJourneyService.class.getDeclaredMethod("skippedQueue", UUID.class, QueueTicket.class);
        method.setAccessible(true);
        UUID visitId = UUID.randomUUID();
        Department room = Department.builder().name("Phòng Nội").roomCode("INT-101").build();
        QueueTicket ticket = QueueTicket.builder().ticketId(UUID.randomUUID()).department(room).queueNumber(7)
                .workDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
                .status(QueueStatus.SKIPPED).build();

        var none = (org.example.doansummer2026.dto.journey.PatientQueueResponse) method.invoke(
                patientJourneyService, visitId, ticket);
        assertTrue(none.canRequestReturn());
        assertEquals("NONE", none.returnRequestStatus());
        assertEquals("Phòng Nội", none.roomName());
        assertEquals("INT-101", none.roomCode());

        var createdOnly = org.example.doansummer2026.model.Notification.builder()
                .status(org.example.doansummer2026.enums.NotificationStatus.PENDING).build();
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(5);
        createdOnly.setCreatedAt(createdAt);
        var sent = org.example.doansummer2026.model.Notification.builder()
                .status(org.example.doansummer2026.enums.NotificationStatus.PENDING)
                .sentAt(createdAt.minusMinutes(1)).build();
        when(notificationRepo.findAllByRelatedEntityAndRelatedEntityIdAndStatusOrderByCreatedAtAsc(
                eq(QueueReturnRequestService.RELATED_ENTITY), eq(ticket.getTicketId()),
                eq(org.example.doansummer2026.enums.NotificationStatus.PENDING)))
                .thenReturn(List.of(createdOnly, sent));
        var pending = (org.example.doansummer2026.dto.journey.PatientQueueResponse) method.invoke(
                patientJourneyService, visitId, ticket);
        assertFalse(pending.canRequestReturn());
        assertEquals("PENDING", pending.returnRequestStatus());
        assertEquals(sent.getSentAt(), pending.returnRequestedAt());

        ticket.setWorkDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1));
        ticket.setDepartment(null);
        var expired = (org.example.doansummer2026.dto.journey.PatientQueueResponse) method.invoke(
                patientJourneyService, visitId, ticket);
        assertEquals("EXPIRED", expired.returnRequestStatus());
        assertFalse(expired.canRequestReturn());
        assertNull(expired.roomName());
        assertNull(expired.roomCode());

        ticket.setWorkDate(null);
        assertEquals("EXPIRED", ((org.example.doansummer2026.dto.journey.PatientQueueResponse) method.invoke(
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

        invoice.setItems(null);
        List<PatientJourneyResponse.Step> noItems = new java.util.ArrayList<>();
        method.invoke(patientJourneyService, noItems, invoice, exam, 3, List.of(), new java.util.HashSet<UUID>());
        assertTrue(noItems.isEmpty());
    }
}
