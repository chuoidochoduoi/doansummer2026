package org.example.doansummer2026.service;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.journey.PatientJourneyResponse;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Method;
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

    @Mock
    private InvoiceRepository invoiceRepo;

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

    private Department paraclinicalDepartment(String name, String roomCode) {
        return Department.builder()
                .departmentId(UUID.randomUUID())
                .name(name)
                .roomCode(roomCode)
                .departmentType(DepartmentType.PARACLINICAL)
                .build();
    }

    private MedicalService medicalService(String name) {
        return MedicalService.builder()
                .serviceId(UUID.randomUUID())
                .name(name)
                .build();
    }

    private QueueTicket queue(QueueStatus status, LocalDateTime createdAt) {
        QueueTicket q = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(status)
                .queueNumber(1)
                .department(department("Phong kham", "P101"))
                .service(medicalService("Kham tong quat"))
                .build();
        q.setCreatedAt(createdAt);
        return q;
    }

    private QueueTicket paraclinicalQueue(QueueStatus status, LocalDateTime createdAt) {
        QueueTicket q = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(status)
                .queueNumber(1)
                .department(paraclinicalDepartment("Phong can lam sang", "CLS01"))
                .service(medicalService("Sieu am"))
                .build();
        q.setCreatedAt(createdAt);
        return q;
    }

    private TestRequest testRequest(TestRequestStatus status, LocalDateTime createdAt) {
        TestRequest t = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .status(status)
                .performingDepartment(department("Phong xet nghiem", "LAB01"))
                .service(medicalService("Xet nghiem mau"))
                .build();
        t.setCreatedAt(createdAt);
        return t;
    }

    private CustomerVisit visit(UUID visitId) {
        return CustomerVisit.builder()
                .visitId(visitId)
                .checkInTime(LocalDateTime.now().minusMinutes(30))
                .build();
    }

    private Invoice pendingInvoice(LocalDateTime createdAt) {
        Invoice invoice = mock(Invoice.class);
        when(invoice.getInvoiceId()).thenReturn(UUID.randomUUID());
        when(invoice.getStatus()).thenReturn(InvoiceStatus.PENDING);
        when(invoice.getCreatedAt()).thenReturn(createdAt);
        return invoice;
    }

    /**
     * PageResponse in the project is a custom type. This helper keeps the test
     * compatible with either record-style accessors (content/items) or bean-style
     * getters without coupling the whole test class to one accessor name.
     */
    @SuppressWarnings("unchecked")
    private List<PatientJourneyResponse> pageContent(PageResponse<PatientJourneyResponse> page) {
        for (String accessor : List.of("content", "items", "getContent", "getItems")) {
            try {
                Method method = page.getClass().getMethod(accessor);
                Object value = method.invoke(page);
                if (value instanceof List<?>) {
                    return (List<PatientJourneyResponse>) value;
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next common accessor.
            }
        }
        fail("Cannot read content/items from PageResponse");
        return List.of();
    }

    // =========================================================
    // HAS ACTIVE STEP
    // =========================================================

    @Test
    void hasActiveStep_ShouldReturnFalse_WhenJourneyHasNoSteps() {
        UUID visitId = UUID.randomUUID();

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());

        assertFalse(patientJourneyService.hasActiveStep(visitId));
    }

    @Test
    void hasActiveStep_ShouldReturnTrue_WhenWaitingQueueExists() {
        UUID visitId = UUID.randomUUID();
        QueueTicket active = queue(QueueStatus.WAITING, LocalDateTime.now());

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(active));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());

        assertTrue(patientJourneyService.hasActiveStep(visitId));
    }

    @Test
    void hasActiveStep_ShouldIgnoreBlockedDoneSkippedAndWaitingForTestQueues() {
        UUID visitId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(
                queue(QueueStatus.BLOCKED, now),
                queue(QueueStatus.DONE, now),
                queue(QueueStatus.SKIPPED, now),
                queue(QueueStatus.WAITING_FOR_TEST, now)
        ));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());

        assertFalse(patientJourneyService.hasActiveStep(visitId));
    }

    @Test
    void hasActiveStep_ShouldReturnTrue_WhenStandalonePendingTestExists() {
        UUID visitId = UUID.randomUUID();
        TestRequest test = testRequest(TestRequestStatus.PENDING, LocalDateTime.now());
        test.setQueueTicket(null);

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(test));

        assertTrue(patientJourneyService.hasActiveStep(visitId));
    }

    @Test
    void hasActiveStep_ShouldReturnTrue_WhenStandaloneTestInProgress() {
        UUID visitId = UUID.randomUUID();
        TestRequest test = testRequest(TestRequestStatus.IN_PROGRESS, LocalDateTime.now());
        test.setQueueTicket(null);

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(test));

        assertTrue(patientJourneyService.hasActiveStep(visitId));
    }

    @Test
    void hasActiveStep_ShouldIgnorePendingTest_WhenItHasQueueTicket() {
        UUID visitId = UUID.randomUUID();
        QueueTicket linkedQueue = queue(QueueStatus.DONE, LocalDateTime.now());
        TestRequest test = testRequest(TestRequestStatus.PENDING, LocalDateTime.now());
        test.setQueueTicket(linkedQueue);

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(linkedQueue));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(test));

        assertFalse(patientJourneyService.hasActiveStep(visitId));
    }

    @Test
    void hasActiveStep_ShouldIgnoreCompletedAndBlockedTests() {
        UUID visitId = UUID.randomUUID();

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(
                testRequest(TestRequestStatus.BLOCKED, LocalDateTime.now()),
                testRequest(TestRequestStatus.COMPLETED, LocalDateTime.now())
        ));

        assertFalse(patientJourneyService.hasActiveStep(visitId));
    }

    // =========================================================
    // ACTIVATE NEXT
    // =========================================================

    @Test
    void activateNext_ShouldDoNothing_WhenActiveStepAlreadyExists() {
        UUID visitId = UUID.randomUUID();
        QueueTicket active = queue(QueueStatus.WAITING, LocalDateTime.now());

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(active));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());

        patientJourneyService.activateNext(visitId);

        verify(queueRepo, never()).save(any());
        verify(testRepo, never()).save(any());
        verify(visitRepo, never()).save(any());
    }

    @Test
    void activateNext_ShouldActivateBlockedQueue_WhenOnlyQueueExists() {
        UUID visitId = UUID.randomUUID();
        QueueTicket blocked = queue(QueueStatus.BLOCKED, LocalDateTime.now().minusMinutes(10));

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(blocked));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(testRepo.findAllByQueueTicket_TicketId(blocked.getTicketId())).thenReturn(List.of());

        patientJourneyService.activateNext(visitId);

        assertEquals(QueueStatus.WAITING, blocked.getStatus());
        verify(queueRepo).save(blocked);
    }

    @Test
    void activateNext_ShouldActivateBlockedTestsSharingActivatedQueue() {
        UUID visitId = UUID.randomUUID();
        QueueTicket blockedQueue = queue(QueueStatus.BLOCKED, LocalDateTime.now().minusMinutes(20));

        TestRequest groupedBlocked = testRequest(TestRequestStatus.BLOCKED, LocalDateTime.now());
        groupedBlocked.setQueueTicket(blockedQueue);

        TestRequest groupedCompleted = testRequest(TestRequestStatus.COMPLETED, LocalDateTime.now());
        groupedCompleted.setQueueTicket(blockedQueue);

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(blockedQueue));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(testRepo.findAllByQueueTicket_TicketId(blockedQueue.getTicketId()))
                .thenReturn(List.of(groupedBlocked, groupedCompleted));

        patientJourneyService.activateNext(visitId);

        assertEquals(QueueStatus.WAITING, blockedQueue.getStatus());
        assertEquals(TestRequestStatus.PENDING, groupedBlocked.getStatus());
        assertEquals(TestRequestStatus.COMPLETED, groupedCompleted.getStatus());

        verify(testRepo).save(groupedBlocked);
        verify(testRepo, never()).save(groupedCompleted);
    }

    @Test
    void activateNext_ShouldPreferBlockedQueue_EvenWhenStandaloneTestWasCreatedEarlier() {
        UUID visitId = UUID.randomUUID();

        QueueTicket blockedQueue = queue(
                QueueStatus.BLOCKED,
                LocalDateTime.now().minusMinutes(10)
        );
        TestRequest olderStandaloneTest = testRequest(
                TestRequestStatus.BLOCKED,
                LocalDateTime.now().minusMinutes(20)
        );
        olderStandaloneTest.setQueueTicket(null);

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(blockedQueue));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(olderStandaloneTest));
        when(testRepo.findAllByQueueTicket_TicketId(blockedQueue.getTicketId())).thenReturn(List.of());

        patientJourneyService.activateNext(visitId);

        assertEquals(QueueStatus.WAITING, blockedQueue.getStatus());
        assertEquals(TestRequestStatus.BLOCKED, olderStandaloneTest.getStatus());
        verify(queueRepo).save(blockedQueue);
        verify(testRepo, never()).save(olderStandaloneTest);
    }

    @Test
    void activateNext_ShouldActivateStandaloneBlockedTest_WhenNoBlockedQueueExists() {
        UUID visitId = UUID.randomUUID();
        TestRequest test = testRequest(TestRequestStatus.BLOCKED, LocalDateTime.now().minusMinutes(20));
        test.setQueueTicket(null);

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(test));

        patientJourneyService.activateNext(visitId);

        assertEquals(TestRequestStatus.PENDING, test.getStatus());
        verify(testRepo).save(test);
    }

    @Test
    void activateNext_ShouldIgnoreBlockedTest_WhenItAlreadyHasQueueTicket() {
        UUID visitId = UUID.randomUUID();
        QueueTicket doneQueue = queue(QueueStatus.DONE, LocalDateTime.now());
        TestRequest groupedBlocked = testRequest(TestRequestStatus.BLOCKED, LocalDateTime.now());
        groupedBlocked.setQueueTicket(doneQueue);

        CustomerVisit visit = visit(visitId);

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(doneQueue));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(groupedBlocked));
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));

        patientJourneyService.activateNext(visitId);

        assertEquals(TestRequestStatus.BLOCKED, groupedBlocked.getStatus());
        assertEquals(VisitStatus.COMPLETED, visit.getStatus());
        verify(testRepo, never()).save(groupedBlocked);
        verify(visitRepo).save(visit);
    }

    @Test
    void activateNext_ShouldPickOldestBlockedQueue() {
        UUID visitId = UUID.randomUUID();
        QueueTicket oldQueue = queue(QueueStatus.BLOCKED, LocalDateTime.now().minusMinutes(30));
        QueueTicket newQueue = queue(QueueStatus.BLOCKED, LocalDateTime.now().minusMinutes(10));

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(newQueue, oldQueue));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(testRepo.findAllByQueueTicket_TicketId(oldQueue.getTicketId())).thenReturn(List.of());

        patientJourneyService.activateNext(visitId);

        assertEquals(QueueStatus.WAITING, oldQueue.getStatus());
        assertEquals(QueueStatus.BLOCKED, newQueue.getStatus());
    }

    @Test
    void activateNext_ShouldPickOldestStandaloneBlockedTest() {
        UUID visitId = UUID.randomUUID();

        TestRequest oldTest = testRequest(TestRequestStatus.BLOCKED, LocalDateTime.now().minusMinutes(30));
        oldTest.setQueueTicket(null);

        TestRequest newTest = testRequest(TestRequestStatus.BLOCKED, LocalDateTime.now().minusMinutes(10));
        newTest.setQueueTicket(null);

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(newTest, oldTest));

        patientJourneyService.activateNext(visitId);

        assertEquals(TestRequestStatus.PENDING, oldTest.getStatus());
        assertEquals(TestRequestStatus.BLOCKED, newTest.getStatus());
    }

    @Test
    void activateNext_ShouldCompleteVisit_WhenJourneyHasFinished() {
        UUID visitId = UUID.randomUUID();
        QueueTicket completedQueue = queue(QueueStatus.DONE, LocalDateTime.now());
        CustomerVisit visit = visit(visitId);

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(completedQueue));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));

        patientJourneyService.activateNext(visitId);

        assertEquals(VisitStatus.COMPLETED, visit.getStatus());
        assertNotNull(visit.getCheckOutTime());
        verify(visitRepo).save(visit);
    }

    @Test
    void activateNext_ShouldNotCompleteVisit_WhenGroupedTestIsStillPending() {
        UUID visitId = UUID.randomUUID();
        QueueTicket doneQueue = queue(QueueStatus.DONE, LocalDateTime.now());
        TestRequest pending = testRequest(TestRequestStatus.PENDING, LocalDateTime.now());
        pending.setQueueTicket(doneQueue);
        CustomerVisit visit = visit(visitId);

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(doneQueue));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(pending));
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));

        patientJourneyService.activateNext(visitId);

        assertNotEquals(VisitStatus.COMPLETED, visit.getStatus());
        assertNull(visit.getCheckOutTime());
        verify(visitRepo, never()).save(any());
    }

    @Test
    void activateNext_ShouldNotFail_WhenVisitDoesNotExistAndJourneyFinished() {
        UUID visitId = UUID.randomUUID();
        QueueTicket done = queue(QueueStatus.DONE, LocalDateTime.now());

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(done));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(visitRepo.findById(visitId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> patientJourneyService.activateNext(visitId));
        verify(visitRepo, never()).save(any());
    }

    @Test
    void activateNext_ShouldNotCompleteVisit_WhenJourneyHasNoSteps() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));

        patientJourneyService.activateNext(visitId);

        verify(visitRepo, never()).save(visit);
    }

    // =========================================================
    // GET / BUILD
    // =========================================================

    @Test
    void get_ShouldThrow_WhenVisitDoesNotExist() {
        UUID visitId = UUID.randomUUID();
        when(visitRepo.findById(visitId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientJourneyService.get(visitId));
    }

    @Test
    void get_ShouldBuildRegisteredCustomerJourney() {
        UUID visitId = UUID.randomUUID();

        Profile customer = Profile.builder()
                .profileId(UUID.randomUUID())
                .fullName("Nguyen Van A")
                .phone("0901111111")
                .build();

        CustomerVisit visit = visit(visitId);
        visit.setCustomer(customer);

        QueueTicket queue = queue(QueueStatus.WAITING, LocalDateTime.now().minusMinutes(5));

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(queue));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertNotNull(result);
        assertEquals("Nguyen Van A", result.patientName());
        assertEquals("0901111111", result.phone());
        assertFalse(result.guest());
        assertEquals(QueueStatus.WAITING.name(), result.currentStatus());
        assertEquals(1, result.steps().size());
    }

    @Test
    void get_ShouldUseGuestInformation_WhenCustomerIsNull() {
        UUID visitId = UUID.randomUUID();

        Appointment appointment = mock(Appointment.class);
        when(appointment.getGuestFullName()).thenReturn("Tran Van Guest");
        when(appointment.getGuestPhone()).thenReturn("0988888888");

        CustomerVisit visit = visit(visitId);
        visit.setAppointment(appointment);

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals("Tran Van Guest", result.patientName());
        assertEquals("0988888888", result.phone());
        assertTrue(result.guest());
        assertEquals("UNASSIGNED", result.currentStatus());
    }

    @Test
    void get_ShouldUseWalkInName_WhenCustomerAndAppointmentAreNull() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals("Khách vãng lai", result.patientName());
        assertNull(result.phone());
    }

    @Test
    void get_ShouldReturnCompleted_WhenAllStepsFinished() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        QueueTicket done = queue(QueueStatus.DONE, LocalDateTime.now());

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(done));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals("COMPLETED", result.currentStatus());
        assertEquals("Đã hoàn thành", result.currentStep());
    }

    @Test
    void get_ShouldReturnNextBlockedService() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        QueueTicket blocked = queue(QueueStatus.BLOCKED, LocalDateTime.now());
        blocked.getService().setName("Kham Noi");

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(blocked));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals("Kham Noi", result.nextStep());
        assertEquals("UNASSIGNED", result.currentStatus());
    }

    @Test
    void get_ShouldUseDefaultExaminationName_WhenQueueServiceMissing() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        QueueTicket queue = queue(QueueStatus.WAITING, LocalDateTime.now());
        queue.setService(null);

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(queue));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals("Khám bệnh", result.currentStep());
    }

    @Test
    void get_ShouldAddStandaloneParaclinicalTestAndResultWaitingStep() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        TestRequest test = testRequest(TestRequestStatus.PENDING, LocalDateTime.now().minusSeconds(1));
        test.setQueueTicket(null);

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(test));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals(2, result.steps().size());
        assertEquals("PARACLINICAL", result.steps().get(0).kind());
        assertTrue(result.steps().stream().anyMatch(s -> "RESULT_PENDING".equals(s.status())));
        assertEquals("Xet nghiem mau", result.currentStep());
    }

    @Test
    void get_ShouldUseDefaultParaclinicalName_WhenStandaloneTestHasNoService() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        TestRequest test = testRequest(TestRequestStatus.PENDING, LocalDateTime.now().minusSeconds(1));
        test.setQueueTicket(null);
        test.setService(null);

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(test));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals("Cận lâm sàng", result.currentStep());
    }

    @Test
    void get_ShouldGroupTestRequestsByQueueTicket() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        QueueTicket queue = queue(QueueStatus.WAITING, LocalDateTime.now());

        TestRequest first = testRequest(TestRequestStatus.PENDING, LocalDateTime.now());
        first.setQueueTicket(queue);
        first.setService(medicalService("Xet nghiem mau"));

        TestRequest second = testRequest(TestRequestStatus.PENDING, LocalDateTime.now());
        second.setQueueTicket(queue);
        second.setService(medicalService("Sieu am"));

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(first, second));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(queue));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals(1, result.steps().size());
        assertEquals("PARACLINICAL", result.steps().get(0).kind());
        assertTrue(result.steps().get(0).serviceName().contains("Xet nghiem mau"));
        assertTrue(result.steps().get(0).serviceName().contains("Sieu am"));
    }

    @Test
    void get_ShouldUseDefaultName_WhenGroupedTestServiceMissing() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        QueueTicket queue = queue(QueueStatus.WAITING, LocalDateTime.now());

        TestRequest test = testRequest(TestRequestStatus.PENDING, LocalDateTime.now());
        test.setQueueTicket(queue);
        test.setService(null);

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(test));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(queue));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals("Cận lâm sàng", result.steps().get(0).serviceName());
    }

    @Test
    void get_ShouldShowPaymentAsCurrentStep_WhenPendingInvoiceExists() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        Invoice pending = pendingInvoice(LocalDateTime.now().minusMinutes(2));

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(invoiceRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(pending));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals("PAYMENT_PENDING", result.currentStatus());
        assertEquals("Thanh toan dich vu", result.currentStep());
        assertEquals("Quay thu ngan (-)", result.currentRoom());
        assertEquals("PAYMENT", result.steps().get(0).kind());
    }

    @Test
    void get_ShouldShowResultPending_WhenGroupedTestStillProcessingAndNoPhysicalActiveQueue() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        QueueTicket doneQueue = queue(QueueStatus.DONE, LocalDateTime.now().minusMinutes(10));
        TestRequest groupedPending = testRequest(TestRequestStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        groupedPending.setQueueTicket(doneQueue);

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(groupedPending));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(doneQueue));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals("RESULT_PENDING", result.currentStatus());
        assertEquals("Dang cho ket qua can lam sang", result.currentStep());
        assertTrue(result.steps().stream().anyMatch(s -> "RESULT".equals(s.kind())));
    }

    @Test
    void get_ShouldNotAddResultPending_WhenPhysicalQueueIsWaiting() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        QueueTicket waitingQueue = queue(QueueStatus.WAITING, LocalDateTime.now().minusMinutes(10));
        TestRequest groupedPending = testRequest(TestRequestStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        groupedPending.setQueueTicket(waitingQueue);

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of(groupedPending));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(waitingQueue));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals("WAITING", result.currentStatus());
        assertFalse(result.steps().stream().anyMatch(s -> "RESULT_PENDING".equals(s.status())));
    }

    @Test
    void get_ShouldSortJourneyStepsByCreatedAt() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        QueueTicket later = queue(QueueStatus.BLOCKED, LocalDateTime.now().minusMinutes(5));
        QueueTicket earlier = queue(QueueStatus.WAITING, LocalDateTime.now().minusMinutes(20));

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(later, earlier));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals("QUEUE:" + earlier.getTicketId(), result.steps().get(0).id());
    }

    @Test
    void get_ShouldPutNullCreatedAtLast() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        QueueTicket withDate = queue(QueueStatus.WAITING, LocalDateTime.now());
        QueueTicket withoutDate = queue(QueueStatus.BLOCKED, LocalDateTime.now());
        withoutDate.setCreatedAt(null);

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(withoutDate, withDate));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals("QUEUE:" + withDate.getTicketId(), result.steps().get(0).id());
    }

    @Test
    void get_ShouldUseDash_WhenCurrentRoomCodeIsNull() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        QueueTicket queue = queue(QueueStatus.WAITING, LocalDateTime.now());
        queue.getDepartment().setRoomCode(null);

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(queue));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertTrue(result.currentRoom().contains("(-)"));
    }

    @Test
    void get_ShouldFlagLongWaiting_WhenWaitingAtLeast60Minutes() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        visit.setCheckInTime(LocalDateTime.now().minusMinutes(90));

        QueueTicket queue = queue(QueueStatus.WAITING, LocalDateTime.now());

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(queue));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertTrue(result.waitingMinutes() >= 60);
        assertTrue(result.warning());
    }

    @Test
    void get_ShouldSetWaitingZero_WhenCheckInTimeIsNull() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        visit.setCheckInTime(null);

        QueueTicket queue = queue(QueueStatus.WAITING, LocalDateTime.now());

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(queue));

        PatientJourneyResponse result = patientJourneyService.get(visitId);

        assertEquals(0, result.waitingMinutes());
    }

    // =========================================================
    // LIST - NOW PAGINATED
    // =========================================================

    @Test
    void list_ShouldReturnAll_WhenFiltersAreNull() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        when(visitRepo.findAll()).thenReturn(List.of(visit));

        PageResponse<PatientJourneyResponse> result = patientJourneyService.list(
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertEquals(1, pageContent(result).size());
    }

    @Test
    void list_ShouldFilterByPatientName() {
        UUID visitId = UUID.randomUUID();

        Profile customer = Profile.builder()
                .profileId(UUID.randomUUID())
                .fullName("Nguyen Van Cuong")
                .phone("0901234567")
                .build();

        CustomerVisit visit = visit(visitId);
        visit.setCustomer(customer);

        when(visitRepo.findAll()).thenReturn(List.of(visit));

        PageResponse<PatientJourneyResponse> found = patientJourneyService.list(
                "  CUONG ",
                null,
                PageRequest.of(0, 10)
        );

        PageResponse<PatientJourneyResponse> missing = patientJourneyService.list(
                "khong-co",
                null,
                PageRequest.of(0, 10)
        );

        assertEquals(1, pageContent(found).size());
        assertTrue(pageContent(missing).isEmpty());
    }

    @Test
    void list_ShouldFilterByPhone() {
        UUID visitId = UUID.randomUUID();

        Profile customer = Profile.builder()
                .profileId(UUID.randomUUID())
                .fullName("Patient")
                .phone("0987654321")
                .build();

        CustomerVisit visit = visit(visitId);
        visit.setCustomer(customer);

        when(visitRepo.findAll()).thenReturn(List.of(visit));

        PageResponse<PatientJourneyResponse> result = patientJourneyService.list(
                "987654",
                null,
                PageRequest.of(0, 10)
        );

        assertEquals(1, pageContent(result).size());
    }

    @Test
    void list_ShouldFilterByVisitCode() {
        UUID visitId = UUID.fromString("12345678-1111-2222-3333-444444444444");
        CustomerVisit visit = visit(visitId);

        when(visitRepo.findAll()).thenReturn(List.of(visit));

        PageResponse<PatientJourneyResponse> result = patientJourneyService.list(
                "VIS-12345678",
                null,
                PageRequest.of(0, 10)
        );

        assertEquals(1, pageContent(result).size());
    }

    @Test
    void list_ShouldFilterByCurrentStatus() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);
        QueueTicket waiting = queue(QueueStatus.WAITING, LocalDateTime.now());

        when(visitRepo.findAll()).thenReturn(List.of(visit));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(waiting));

        PageResponse<PatientJourneyResponse> waitingResult = patientJourneyService.list(
                null,
                "WAITING",
                PageRequest.of(0, 10)
        );

        PageResponse<PatientJourneyResponse> blockedResult = patientJourneyService.list(
                null,
                "BLOCKED",
                PageRequest.of(0, 10)
        );

        assertEquals(1, pageContent(waitingResult).size());
        assertTrue(pageContent(blockedResult).isEmpty());
    }

    @Test
    void list_ShouldSortLatestCheckInFirstAndNullLast() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID nullId = UUID.randomUUID();

        CustomerVisit first = visit(firstId);
        first.setCheckInTime(LocalDateTime.now().minusHours(2));

        CustomerVisit second = visit(secondId);
        second.setCheckInTime(LocalDateTime.now().minusMinutes(10));

        CustomerVisit noDate = visit(nullId);
        noDate.setCheckInTime(null);

        when(visitRepo.findAll()).thenReturn(List.of(first, noDate, second));

        PageResponse<PatientJourneyResponse> result = patientJourneyService.list(
                null,
                null,
                PageRequest.of(0, 10)
        );

        List<PatientJourneyResponse> content = pageContent(result);

        assertEquals(secondId, content.get(0).visitId());
        assertEquals(nullId, content.get(2).visitId());
    }

    @Test
    void list_ShouldPaginateFilteredResults() {
        CustomerVisit first = visit(UUID.randomUUID());
        CustomerVisit second = visit(UUID.randomUUID());
        CustomerVisit third = visit(UUID.randomUUID());

        first.setCheckInTime(LocalDateTime.now().minusMinutes(30));
        second.setCheckInTime(LocalDateTime.now().minusMinutes(20));
        third.setCheckInTime(LocalDateTime.now().minusMinutes(10));

        when(visitRepo.findAll()).thenReturn(List.of(first, second, third));

        PageResponse<PatientJourneyResponse> firstPage = patientJourneyService.list(
                null,
                null,
                PageRequest.of(0, 2)
        );
        PageResponse<PatientJourneyResponse> secondPage = patientJourneyService.list(
                null,
                null,
                PageRequest.of(1, 2)
        );

        assertEquals(2, pageContent(firstPage).size());
        assertEquals(1, pageContent(secondPage).size());
        assertEquals(third.getVisitId(), pageContent(firstPage).get(0).visitId());
        assertEquals(first.getVisitId(), pageContent(secondPage).get(0).visitId());
    }

    // =========================================================
    // ADVANCE BLOCKED STEP - NEW PUBLIC METHOD
    // =========================================================

    @Test
    void advanceBlockedStep_ShouldThrow_WhenVisitDoesNotExist() {
        UUID visitId = UUID.randomUUID();

        when(visitRepo.findById(visitId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> patientJourneyService.advanceBlockedStep(visitId)
        );
    }

    @Test
    void advanceBlockedStep_ShouldActivateOldestBlockedParaclinicalQueue_WhenNoPhysicalQueueActive() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        QueueTicket oldBlocked = paraclinicalQueue(
                QueueStatus.BLOCKED,
                LocalDateTime.now().minusMinutes(20)
        );
        QueueTicket newBlocked = paraclinicalQueue(
                QueueStatus.BLOCKED,
                LocalDateTime.now().minusMinutes(10)
        );

        TestRequest linkedBlocked = testRequest(TestRequestStatus.BLOCKED, LocalDateTime.now());
        linkedBlocked.setQueueTicket(oldBlocked);

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(queueRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(newBlocked, oldBlocked));
        when(testRepo.findAllByQueueTicket_TicketId(oldBlocked.getTicketId()))
                .thenReturn(List.of(linkedBlocked));

        PatientJourneyResponse result = patientJourneyService.advanceBlockedStep(visitId);

        assertNotNull(result);
        assertEquals(QueueStatus.WAITING, oldBlocked.getStatus());
        assertEquals(QueueStatus.BLOCKED, newBlocked.getStatus());
        assertEquals(TestRequestStatus.PENDING, linkedBlocked.getStatus());

        verify(queueRepo).save(oldBlocked);
        verify(testRepo).save(linkedBlocked);
    }

    @Test
    void advanceBlockedStep_ShouldDelegateToActivateNext_WhenPhysicalQueueAlreadyActive() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        QueueTicket active = queue(QueueStatus.WAITING, LocalDateTime.now());
        QueueTicket blockedParaclinical = paraclinicalQueue(
                QueueStatus.BLOCKED,
                LocalDateTime.now().minusMinutes(10)
        );

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(queueRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(active, blockedParaclinical));

        PatientJourneyResponse result = patientJourneyService.advanceBlockedStep(visitId);

        assertNotNull(result);
        assertEquals(QueueStatus.BLOCKED, blockedParaclinical.getStatus());
        verify(queueRepo, never()).save(blockedParaclinical);
    }

    // =========================================================
    // LIST FOR CUSTOMER
    // =========================================================

    @Test
    void listForCustomer_ShouldReturnEmpty_WhenCustomerHasNoVisits() {
        UUID profileId = UUID.randomUUID();

        when(visitRepo.findAllByCustomer_ProfileIdOrderByCheckInTimeDesc(profileId))
                .thenReturn(List.of());

        List<PatientJourneyResponse> result = patientJourneyService.listForCustomer(profileId);

        assertTrue(result.isEmpty());
    }

    @Test
    void listForCustomer_ShouldMapCustomerVisits() {
        UUID profileId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId);

        when(visitRepo.findAllByCustomer_ProfileIdOrderByCheckInTimeDesc(profileId))
                .thenReturn(List.of(visit));

        List<PatientJourneyResponse> result = patientJourneyService.listForCustomer(profileId);

        assertEquals(1, result.size());
        assertEquals(visitId, result.get(0).visitId());
    }
}