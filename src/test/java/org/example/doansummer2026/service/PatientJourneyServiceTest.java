package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.journey.PatientJourneyResponse;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientJourneyServiceTest {

    @Mock
    private CustomerVisitRepository visitRepo;

    @Mock
    private QueueTicketRepository queueRepo;

    @Mock
    private TestRequestRepository testRepo;

    @InjectMocks
    private PatientJourneyService patientJourneyService;


    // =========================================================
    // HELPERS
    // =========================================================

    private Department department(String name, String roomCode) {
        return Department.builder()
                .departmentId(UUID.randomUUID())
                .name(name)
                .roomCode(roomCode)
                .build();
    }

    private MedicalService medicalService(String name) {
        return MedicalService.builder()
                .serviceId(UUID.randomUUID())
                .name(name)
                .build();
    }

    private QueueTicket queue(
            QueueStatus status,
            LocalDateTime createdAt
    ) {

        QueueTicket q = QueueTicket.builder()
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

        q.setCreatedAt(createdAt);

        return q;
    }

    private TestRequest testRequest(
            TestRequestStatus status,
            LocalDateTime createdAt
    ) {

        TestRequest t = TestRequest.builder()
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

        t.setCreatedAt(createdAt);

        return t;
    }

    private CustomerVisit visit(UUID visitId) {

        return CustomerVisit.builder()
                .visitId(visitId)
                .checkInTime(
                        LocalDateTime.now()
                                .minusMinutes(30)
                )
                .build();
    }


    // =========================================================
    // HAS ACTIVE STEP
    // =========================================================

    @Test
    void hasActiveStep_ShouldReturnFalse_WhenJourneyHasNoSteps() {

        UUID visitId = UUID.randomUUID();

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        assertFalse(
                patientJourneyService
                        .hasActiveStep(visitId)
        );
    }


    @Test
    void hasActiveStep_ShouldReturnTrue_WhenWaitingQueueExists() {

        UUID visitId = UUID.randomUUID();

        QueueTicket queue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(queue));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        assertTrue(
                patientJourneyService
                        .hasActiveStep(visitId)
        );
    }


    @Test
    void hasActiveStep_ShouldIgnoreBlockedDoneSkippedAndWaitingForTestQueues() {

        UUID visitId = UUID.randomUUID();

        LocalDateTime now =
                LocalDateTime.now();

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(
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

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        assertFalse(
                patientJourneyService
                        .hasActiveStep(visitId)
        );
    }


    @Test
    void hasActiveStep_ShouldReturnTrue_WhenPendingTestExists() {

        UUID visitId = UUID.randomUUID();

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(
                        List.of(
                                testRequest(
                                        TestRequestStatus.PENDING,
                                        LocalDateTime.now()
                                )
                        )
                );

        assertTrue(
                patientJourneyService
                        .hasActiveStep(visitId)
        );
    }


    @Test
    void hasActiveStep_ShouldReturnTrue_WhenTestInProgress() {

        UUID visitId = UUID.randomUUID();

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(
                        List.of(
                                testRequest(
                                        TestRequestStatus.IN_PROGRESS,
                                        LocalDateTime.now()
                                )
                        )
                );

        assertTrue(
                patientJourneyService
                        .hasActiveStep(visitId)
        );
    }


    @Test
    void hasActiveStep_ShouldIgnoreCompletedAndBlockedTests() {

        UUID visitId = UUID.randomUUID();

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(
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
                        .hasActiveStep(visitId)
        );
    }


    // =========================================================
    // ACTIVATE NEXT - ACTIVE STEP ALREADY EXISTS
    // =========================================================

    @Test
    void activateNext_ShouldDoNothing_WhenActiveStepAlreadyExists() {

        UUID visitId = UUID.randomUUID();

        QueueTicket active =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(active));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        patientJourneyService.activateNext(visitId);

        verify(queueRepo, never())
                .save(any());

        verify(testRepo, never())
                .save(any());

        verify(visitRepo, never())
                .save(any());
    }


    // =========================================================
    // ACTIVATE NEXT - QUEUE ONLY
    // =========================================================

    @Test
    void activateNext_ShouldActivateBlockedQueue_WhenOnlyQueueExists() {

        UUID visitId = UUID.randomUUID();

        QueueTicket blocked =
                queue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(10)
                );

        /*
         * hasActiveStep + tìm blocked queue
         */
        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(blocked));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(
                testRepo.findAllByQueueTicket_TicketId(
                        blocked.getTicketId()
                )
        ).thenReturn(List.of());

        patientJourneyService.activateNext(visitId);

        assertEquals(
                QueueStatus.WAITING,
                blocked.getStatus()
        );

        verify(queueRepo)
                .save(blocked);
    }


    // =========================================================
    // ACTIVATE NEXT - QUEUE ALSO ACTIVATES GROUPED TESTS
    // =========================================================

    @Test
    void activateNext_ShouldActivateBlockedTestsSharingActivatedQueue() {

        UUID visitId = UUID.randomUUID();

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

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(
                        List.of(blockedQueue)
                );

        /*
         * Journey-wide tests:
         * để empty để queue chắc chắn là step được chọn.
         */
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

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

        patientJourneyService.activateNext(
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
                .save(groupedBlocked);

        verify(testRepo, never())
                .save(groupedCompleted);
    }


    // =========================================================
    // ACTIVATE NEXT - TEST EARLIER THAN QUEUE
    // =========================================================

    @Test
    void activateNext_ShouldActivateTest_WhenTestWasCreatedBeforeQueue() {

        UUID visitId = UUID.randomUUID();

        QueueTicket queue =
                queue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(10)
                );

        TestRequest test =
                testRequest(
                        TestRequestStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(20)
                );

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(
                        List.of(queue)
                );

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(
                        List.of(test)
                );

        patientJourneyService.activateNext(
                visitId
        );

        assertEquals(
                TestRequestStatus.PENDING,
                test.getStatus()
        );

        assertEquals(
                QueueStatus.BLOCKED,
                queue.getStatus()
        );

        verify(testRepo)
                .save(test);

        verify(queueRepo, never())
                .save(queue);
    }


    // =========================================================
    // ACTIVATE NEXT - QUEUE EARLIER THAN TEST
    // =========================================================

    @Test
    void activateNext_ShouldActivateQueue_WhenQueueWasCreatedBeforeTest() {

        UUID visitId = UUID.randomUUID();

        QueueTicket queue =
                queue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(30)
                );

        TestRequest test =
                testRequest(
                        TestRequestStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(10)
                );

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(
                        List.of(queue)
                );

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(
                        List.of(test)
                );

        when(
                testRepo.findAllByQueueTicket_TicketId(
                        queue.getTicketId()
                )
        ).thenReturn(List.of());

        patientJourneyService.activateNext(
                visitId
        );

        assertEquals(
                QueueStatus.WAITING,
                queue.getStatus()
        );

        assertEquals(
                TestRequestStatus.BLOCKED,
                test.getStatus()
        );
    }


    // =========================================================
    // ACTIVATE NEXT - SAME CREATED AT
    // queue wins because !test.createdAt.isBefore(queue.createdAt)
    // =========================================================

    @Test
    void activateNext_ShouldPreferQueue_WhenCreatedAtIsSame() {

        UUID visitId = UUID.randomUUID();

        LocalDateTime time =
                LocalDateTime.now();

        QueueTicket queue =
                queue(
                        QueueStatus.BLOCKED,
                        time
                );

        TestRequest test =
                testRequest(
                        TestRequestStatus.BLOCKED,
                        time
                );

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(queue));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of(test));

        when(
                testRepo.findAllByQueueTicket_TicketId(
                        queue.getTicketId()
                )
        ).thenReturn(List.of());

        patientJourneyService.activateNext(
                visitId
        );

        assertEquals(
                QueueStatus.WAITING,
                queue.getStatus()
        );
    }


    // =========================================================
    // ACTIVATE NEXT - PICK OLDEST BLOCKED QUEUE
    // =========================================================

    @Test
    void activateNext_ShouldPickOldestBlockedQueue() {

        UUID visitId = UUID.randomUUID();

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

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(
                        List.of(
                                newQueue,
                                oldQueue
                        )
                );

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(
                testRepo.findAllByQueueTicket_TicketId(
                        oldQueue.getTicketId()
                )
        ).thenReturn(List.of());

        patientJourneyService.activateNext(
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
    }


    // =========================================================
    // ACTIVATE NEXT - PICK OLDEST BLOCKED TEST
    // =========================================================

    @Test
    void activateNext_ShouldPickOldestBlockedTest() {

        UUID visitId = UUID.randomUUID();

        TestRequest oldTest =
                testRequest(
                        TestRequestStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(30)
                );

        TestRequest newTest =
                testRequest(
                        TestRequestStatus.BLOCKED,
                        LocalDateTime.now()
                                .minusMinutes(10)
                );

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(
                        List.of(
                                newTest,
                                oldTest
                        )
                );

        patientJourneyService.activateNext(
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


    // =========================================================
    // ACTIVATE NEXT - COMPLETE VISIT
    // =========================================================

    @Test
    void activateNext_ShouldCompleteVisit_WhenJourneyHasFinished() {

        UUID visitId = UUID.randomUUID();

        QueueTicket completedQueue =
                queue(
                        QueueStatus.DONE,
                        LocalDateTime.now()
                );

        CustomerVisit visit =
                visit(visitId);

        /*
         * Method gọi các repository nhiều lần:
         * tất cả đều thấy chỉ có step DONE.
         */
        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(
                        List.of(completedQueue)
                );

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        patientJourneyService.activateNext(
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


    // =========================================================
    // ACTIVATE NEXT - VISIT NOT FOUND
    // =========================================================

    @Test
    void activateNext_ShouldNotFail_WhenVisitDoesNotExistAndJourneyFinished() {

        UUID visitId = UUID.randomUUID();

        QueueTicket done =
                queue(
                        QueueStatus.DONE,
                        LocalDateTime.now()
                );

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(done));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(
                () -> patientJourneyService
                        .activateNext(visitId)
        );

        verify(visitRepo, never())
                .save(any());
    }


    // =========================================================
    // ACTIVATE NEXT - NO STEPS
    // visit must NOT be completed
    // =========================================================

    @Test
    void activateNext_ShouldNotCompleteVisit_WhenJourneyHasNoSteps() {

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        patientJourneyService.activateNext(
                visitId
        );

        verify(visitRepo, never())
                .save(visit);
    }


    // =========================================================
    // GET
    // =========================================================

    @Test
    void get_ShouldThrow_WhenVisitDoesNotExist() {

        UUID visitId = UUID.randomUUID();

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> patientJourneyService.get(
                        visitId
                )
        );
    }


    @Test
    void get_ShouldBuildRegisteredCustomerJourney() {

        UUID visitId = UUID.randomUUID();

        Profile customer =
                Profile.builder()
                        .profileId(UUID.randomUUID())
                        .fullName("Nguyen Van A")
                        .phone("0901111111")
                        .build();

        CustomerVisit visit =
                visit(visitId);

        visit.setCustomer(customer);

        QueueTicket queue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                                .minusMinutes(5)
                );

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(queue));

        PatientJourneyResponse result =
                patientJourneyService.get(
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


    // =========================================================
    // BUILD - GUEST
    // =========================================================

    @Test
    void get_ShouldUseGuestInformation_WhenCustomerIsNull() {

        UUID visitId = UUID.randomUUID();

        Appointment appointment =
                mock(Appointment.class);

        when(appointment.getGuestFullName())
                .thenReturn("Tran Van Guest");

        when(appointment.getGuestPhone())
                .thenReturn("0988888888");

        CustomerVisit visit =
                visit(visitId);

        visit.setAppointment(appointment);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        var result =
                patientJourneyService.get(
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


    // =========================================================
    // BUILD - WALK-IN WITHOUT APPOINTMENT
    // =========================================================

    @Test
    void get_ShouldUseWalkInName_WhenCustomerAndAppointmentAreNull() {

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        var result =
                patientJourneyService.get(
                        visitId
                );

        assertEquals(
                "Khách vãng lai",
                result.patientName()
        );

        assertNull(
                result.phone()
        );
    }


    // =========================================================
    // BUILD - FINISHED
    // =========================================================

    @Test
    void get_ShouldReturnCompleted_WhenAllStepsFinished() {

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket done =
                queue(
                        QueueStatus.DONE,
                        LocalDateTime.now()
                );

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(done));

        var result =
                patientJourneyService.get(
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


    // =========================================================
    // BUILD - NEXT BLOCKED STEP
    // =========================================================

    @Test
    void get_ShouldReturnNextBlockedService() {

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket blocked =
                queue(
                        QueueStatus.BLOCKED,
                        LocalDateTime.now()
                );

        blocked.getService()
                .setName("Kham Noi");

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(blocked));

        var result =
                patientJourneyService.get(
                        visitId
                );

        assertEquals(
                "Kham Noi",
                result.nextStep()
        );

        assertEquals(
                "UNASSIGNED",
                result.currentStatus()
        );
    }


    // =========================================================
    // BUILD - QUEUE WITHOUT SERVICE
    // =========================================================

    @Test
    void get_ShouldUseDefaultExaminationName_WhenQueueServiceMissing() {

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket queue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        queue.setService(null);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(queue));

        var result =
                patientJourneyService.get(
                        visitId
                );

        assertEquals(
                "Khám bệnh",
                result.currentStep()
        );
    }


    // =========================================================
    // BUILD - STANDALONE TEST
    // =========================================================

    @Test
    void get_ShouldAddStandaloneParaclinicalTest() {

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        TestRequest test =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                );

        test.setQueueTicket(null);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of(test));

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        var result =
                patientJourneyService.get(
                        visitId
                );

        assertEquals(
                1,
                result.steps().size()
        );

        assertEquals(
                "PARACLINICAL",
                result.steps().get(0).kind()
        );

        assertEquals(
                "Xet nghiem mau",
                result.currentStep()
        );
    }


    // =========================================================
    // BUILD - STANDALONE TEST WITHOUT SERVICE
    // =========================================================

    @Test
    void get_ShouldUseDefaultParaclinicalName_WhenStandaloneTestHasNoService() {

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        TestRequest test =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                );

        test.setQueueTicket(null);
        test.setService(null);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of(test));

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        var result =
                patientJourneyService.get(
                        visitId
                );

        assertEquals(
                "Cận lâm sàng",
                result.currentStep()
        );
    }


    // =========================================================
    // BUILD - GROUPED PARACLINICAL QUEUE
    // =========================================================

    @Test
    void get_ShouldGroupTestRequestsByQueueTicket() {

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket queue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        TestRequest first =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                );

        first.setQueueTicket(queue);

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

        second.setQueueTicket(queue);

        second.setService(
                medicalService(
                        "Sieu am"
                )
        );

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(
                        List.of(
                                first,
                                second
                        )
                );

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(
                        List.of(queue)
                );

        var result =
                patientJourneyService.get(
                        visitId
                );

        assertEquals(
                1,
                result.steps().size()
        );

        assertEquals(
                "PARACLINICAL",
                result.steps().get(0).kind()
        );

        assertTrue(
                result.steps()
                        .get(0)
                        .serviceName()
                        .contains("Xet nghiem mau")
        );

        assertTrue(
                result.steps()
                        .get(0)
                        .serviceName()
                        .contains("Sieu am")
        );
    }


    // =========================================================
    // BUILD - GROUPED TEST WITHOUT SERVICE
    // =========================================================

    @Test
    void get_ShouldUseDefaultName_WhenGroupedTestServiceMissing() {

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket queue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        TestRequest test =
                testRequest(
                        TestRequestStatus.PENDING,
                        LocalDateTime.now()
                );

        test.setQueueTicket(queue);
        test.setService(null);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of(test));

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(queue));

        var result =
                patientJourneyService.get(
                        visitId
                );

        assertEquals(
                "Cận lâm sàng",
                result.steps()
                        .get(0)
                        .serviceName()
        );
    }


    // =========================================================
    // BUILD - SORT STEPS BY STARTED AT
    // =========================================================

    @Test
    void get_ShouldSortJourneyStepsByCreatedAt() {

        UUID visitId = UUID.randomUUID();

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

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(
                        List.of(
                                later,
                                earlier
                        )
                );

        var result =
                patientJourneyService.get(
                        visitId
                );

        assertEquals(
                "QUEUE:" + earlier.getTicketId(),
                result.steps()
                        .get(0)
                        .id()
        );
    }


    // =========================================================
    // BUILD - NULL START TIME SORTED LAST
    // =========================================================

    @Test
    void get_ShouldPutNullCreatedAtLast() {

        UUID visitId = UUID.randomUUID();

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

        withoutDate.setCreatedAt(null);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(
                        List.of(
                                withoutDate,
                                withDate
                        )
                );

        var result =
                patientJourneyService.get(
                        visitId
                );

        assertEquals(
                "QUEUE:" + withDate.getTicketId(),
                result.steps()
                        .get(0)
                        .id()
        );
    }


    // =========================================================
    // BUILD - ROOM CODE NULL
    // =========================================================

    @Test
    void get_ShouldUseDash_WhenCurrentRoomCodeIsNull() {

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket queue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        queue.getDepartment()
                .setRoomCode(null);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(queue));

        var result =
                patientJourneyService.get(
                        visitId
                );

        assertTrue(
                result.currentRoom()
                        .contains("(-)")
        );
    }


    // =========================================================
    // BUILD - WAITING >= 60
    // =========================================================

    @Test
    void get_ShouldFlagLongWaiting_WhenWaitingAtLeast60Minutes() {

        UUID visitId = UUID.randomUUID();

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

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(queue));

        var result =
                patientJourneyService.get(
                        visitId
                );

        assertTrue(
                result.waitingMinutes() >= 60
        );

        assertTrue(
                result.warning()
        );
    }


    // =========================================================
    // BUILD - CHECK IN NULL
    // =========================================================

    @Test
    void get_ShouldSetWaitingZero_WhenCheckInTimeIsNull() {

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        visit.setCheckInTime(null);

        QueueTicket queue =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(queue));

        var result =
                patientJourneyService.get(
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

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        when(visitRepo.findAll())
                .thenReturn(List.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        var result =
                patientJourneyService.list(
                        null,
                        null
                );

        assertEquals(
                1,
                result.size()
        );
    }


    @Test
    void list_ShouldFilterByPatientName() {

        UUID visitId = UUID.randomUUID();

        Profile customer =
                Profile.builder()
                        .profileId(UUID.randomUUID())
                        .fullName("Nguyen Van Cuong")
                        .phone("0901234567")
                        .build();

        CustomerVisit visit =
                visit(visitId);

        visit.setCustomer(customer);

        when(visitRepo.findAll())
                .thenReturn(List.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        assertEquals(
                1,
                patientJourneyService.list(
                        "  CUONG ",
                        null
                ).size()
        );

        assertTrue(
                patientJourneyService.list(
                        "khong-co",
                        null
                ).isEmpty()
        );
    }


    @Test
    void list_ShouldFilterByPhone() {

        UUID visitId = UUID.randomUUID();

        Profile customer =
                Profile.builder()
                        .profileId(UUID.randomUUID())
                        .fullName("Patient")
                        .phone("0987654321")
                        .build();

        CustomerVisit visit =
                visit(visitId);

        visit.setCustomer(customer);

        when(visitRepo.findAll())
                .thenReturn(List.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        var result =
                patientJourneyService.list(
                        "987654",
                        null
                );

        assertEquals(
                1,
                result.size()
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

        when(visitRepo.findAll())
                .thenReturn(List.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        var result =
                patientJourneyService.list(
                        "VIS-12345678",
                        null
                );

        assertEquals(
                1,
                result.size()
        );
    }


    @Test
    void list_ShouldFilterByCurrentStatus() {

        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        QueueTicket waiting =
                queue(
                        QueueStatus.WAITING,
                        LocalDateTime.now()
                );

        when(visitRepo.findAll())
                .thenReturn(List.of(visit));

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(waiting));

        assertEquals(
                1,
                patientJourneyService.list(
                        null,
                        "WAITING"
                ).size()
        );

        assertTrue(
                patientJourneyService.list(
                        null,
                        "BLOCKED"
                ).isEmpty()
        );
    }


    // =========================================================
    // LIST - SORT CHECK IN DESC, NULL LAST
    // =========================================================

    @Test
    void list_ShouldSortLatestCheckInFirstAndNullLast() {

        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID nullId = UUID.randomUUID();

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

        noDate.setCheckInTime(null);

        when(visitRepo.findAll())
                .thenReturn(
                        List.of(
                                first,
                                noDate,
                                second
                        )
                );

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(any()))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(any()))
                .thenReturn(List.of());

        var result =
                patientJourneyService.list(
                        null,
                        null
                );

        assertEquals(
                secondId,
                result.get(0).visitId()
        );

        assertEquals(
                nullId,
                result.get(2).visitId()
        );
    }


    // =========================================================
    // LIST FOR CUSTOMER
    // =========================================================

    @Test
    void listForCustomer_ShouldReturnEmpty_WhenCustomerHasNoVisits() {

        UUID profileId = UUID.randomUUID();

        when(
                visitRepo
                        .findAllByCustomer_ProfileIdOrderByCheckInTimeDesc(
                                profileId
                        )
        ).thenReturn(List.of());

        var result =
                patientJourneyService.listForCustomer(
                        profileId
                );

        assertTrue(
                result.isEmpty()
        );
    }


    @Test
    void listForCustomer_ShouldMapCustomerVisits() {

        UUID profileId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                visit(visitId);

        when(
                visitRepo
                        .findAllByCustomer_ProfileIdOrderByCheckInTimeDesc(
                                profileId
                        )
        ).thenReturn(
                List.of(visit)
        );

        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        var result =
                patientJourneyService.listForCustomer(
                        profileId
                );

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                visitId,
                result.get(0).visitId()
        );
    }
}