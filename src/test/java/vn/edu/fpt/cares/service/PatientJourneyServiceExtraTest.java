package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.dto.journey.PatientJourneyResponse;
import vn.edu.fpt.cares.dto.journey.PatientQueueResponse;
import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.enums.InvoiceStatus;
import vn.edu.fpt.cares.enums.QueueStatus;
import vn.edu.fpt.cares.enums.TestRequestStatus;
import vn.edu.fpt.cares.enums.VisitStatus;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PatientJourneyServiceExtraTest {

    @Mock private CustomerVisitRepository visitRepo;
    @Mock private QueueTicketRepository queueRepo;
    @Mock private TestRequestRepository testRepo;
    @Mock private InvoiceRepository invoiceRepo;
    @Mock private MedicalRecordRepository recordRepo;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private QueuePriorityService queuePriorityService;

    @InjectMocks
    private PatientJourneyService service;

    private String toVisitCode(UUID id) {
        if (id == null) return null;
        return "VIS-" + id.toString().substring(0, 8).toUpperCase();
    }

    @Test
    void lookupGuest_InvalidCode_Throws() {
        assertThrows(ResourceNotFoundException.class, () -> service.lookupGuest("INVALID", "0901234567"));
        assertThrows(ResourceNotFoundException.class, () -> service.lookupGuest(null, "0901234567"));
        assertThrows(ResourceNotFoundException.class, () -> service.lookupGuest("VIS-12345678", "   "));
    }

    @Test
    void lookupGuest_NotFound_Throws() {
        when(visitRepo.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc("0901234567"))
                .thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> service.lookupGuest("VIS-12345678", "0901234567"));
    }

    @Test
    void lookupGuest_Found_Returns() {
        UUID id = UUID.randomUUID();
        String code = toVisitCode(id);
        CustomerVisit visit = CustomerVisit.builder().visitId(id).build();
        when(visitRepo.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc("0901234567"))
                .thenReturn(List.of(visit));
        
        // Mock the internal build() method dependencies
        when(invoiceRepo.findAllByVisit_VisitId(id)).thenReturn(List.of());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(id)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of());
        
        PatientJourneyResponse res = service.lookupGuest(code, "0901234567");
        assertNotNull(res);
        assertEquals(id, res.visitId());
    }

    @Test
    void lookupGuestQueue_Found_Returns() {
        UUID id = UUID.randomUUID();
        String code = toVisitCode(id);
        CustomerVisit visit = CustomerVisit.builder().visitId(id).status(VisitStatus.CANCELLED).build();
        when(visitRepo.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc("0901234567"))
                .thenReturn(List.of(visit));
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of());
        PatientQueueResponse res = service.lookupGuestQueue(code, "0901234567");
        assertNotNull(res);
        assertEquals(id, res.visitId());
    }

    @Test
    void advanceBlockedStep_NotFound_Throws() {
        UUID id = UUID.randomUUID();
        when(visitRepo.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.advanceBlockedStep(id));
    }

    @Test
    void advanceBlockedStep_Skipped_Throws() {
        UUID id = UUID.randomUUID();
        when(visitRepo.findById(id)).thenReturn(Optional.of(new CustomerVisit()));
        QueueTicket queue = QueueTicket.builder().status(QueueStatus.SKIPPED).build();
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of(queue));
        assertThrows(ConflictException.class, () -> service.advanceBlockedStep(id));
    }

    @Test
    void advanceBlockedStep_ParaclinicalBlocked_Reactivates() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(id).build();
        when(visitRepo.findById(id)).thenReturn(Optional.of(visit));
        
        QueueTicket blockedPara = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.BLOCKED)
                .department(Department.builder().departmentType(DepartmentType.PARACLINICAL).build())
                .build();
        blockedPara.setCreatedAt(LocalDateTime.now());
                
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of(blockedPara));
        
        TestRequest blockedReq = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .status(TestRequestStatus.BLOCKED)
                .build();
                
        when(testRepo.findAllByQueueTicket_TicketId(blockedPara.getTicketId())).thenReturn(List.of(blockedReq));
        when(invoiceRepo.findAllByVisit_VisitId(id)).thenReturn(List.of());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(id)).thenReturn(List.of());
        
        service.advanceBlockedStep(id);
        
        assertEquals(QueueStatus.WAITING, blockedPara.getStatus());
        assertEquals(TestRequestStatus.PENDING, blockedReq.getStatus());
        verify(queueRepo).save(blockedPara);
        verify(testRepo).save(blockedReq);
    }
    
    @Test
    void queueForCustomer_NotAuthorized_Throws() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder()
                .customer(Profile.builder().profileId(UUID.randomUUID()).build())
                .build();
        when(visitRepo.findById(id)).thenReturn(Optional.of(visit));
        
        assertThrows(AccessDeniedException.class, () -> service.queueForCustomer(id, Set.of(otherId)));
    }
    
    @Test
    void queueForCustomer_Authorized_Returns() {
        UUID id = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder()
                .visitId(id)
                .status(VisitStatus.COMPLETED)
                .customer(Profile.builder().profileId(profileId).build())
                .build();
        when(visitRepo.findById(id)).thenReturn(Optional.of(visit));
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of());
        
        PatientQueueResponse res = service.queueForCustomer(id, Set.of(profileId));
        assertNotNull(res);
        assertEquals(id, res.visitId());
    }

    @Test
    void list_VariousFilters_ReturnsFilteredPage() {
        CustomerVisit v1 = CustomerVisit.builder().visitId(UUID.randomUUID()).checkInTime(LocalDateTime.now()).build();
        CustomerVisit v2 = CustomerVisit.builder().visitId(UUID.randomUUID()).checkInTime(LocalDateTime.now().minusDays(2)).status(VisitStatus.COMPLETED).build();
        when(visitRepo.findAll()).thenReturn(List.of(v1, v2));
        
        when(invoiceRepo.findAllByVisit_VisitId(any())).thenReturn(List.of());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(any())).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(any())).thenReturn(List.of());
        
        // Scope TODAY
        vn.edu.fpt.cares.common.PageResponse<PatientJourneyResponse> page1 = service.list("   ", "  ", "TODAY", org.springframework.data.domain.PageRequest.of(0, 10));
        assertNotNull(page1);
        
        // Scope OVERDUE
        vn.edu.fpt.cares.common.PageResponse<PatientJourneyResponse> page2 = service.list(null, null, "OVERDUE", org.springframework.data.domain.PageRequest.of(0, 10));
        assertNotNull(page2);
        
        // Scope Invalid
        assertThrows(vn.edu.fpt.cares.exception.BadRequestException.class, () -> service.list(null, null, "INVALID", org.springframework.data.domain.PageRequest.of(0, 10)));
        
        // Specific search and status
        vn.edu.fpt.cares.common.PageResponse<PatientJourneyResponse> page3 = service.list("Khách", "UNASSIGNED", "ALL", org.springframework.data.domain.PageRequest.of(0, 10));
        assertNotNull(page3);
    }
    
    @Test
    void buildPatientQueue_Skipped_ReturnsSkipped() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(id).status(VisitStatus.IN_PROGRESS).build();
        QueueTicket skipped = QueueTicket.builder().status(QueueStatus.SKIPPED).workDate(java.time.LocalDate.now()).build();
        skipped.setCreatedAt(LocalDateTime.now());
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of(skipped));
        
        when(visitRepo.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc("123"))
            .thenReturn(List.of(visit));
        
        PatientQueueResponse res = service.lookupGuestQueue(toVisitCode(id), "123");
        assertEquals(QueueStatus.SKIPPED.name(), res.currentStatus());
    }

    @Test
    void buildPatientQueue_CancelledOrCompleted_ReturnsStatus() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(id).status(VisitStatus.CANCELLED).build();
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of());
        when(visitRepo.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc("123"))
            .thenReturn(List.of(visit));
        
        PatientQueueResponse res = service.lookupGuestQueue(toVisitCode(id), "123");
        assertEquals("CANCELLED", res.currentStatus());
    }

    @Test
    void buildPatientQueue_WaitingQueue_CalculatesPosition() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(id).status(VisitStatus.IN_PROGRESS).build();
        Department dept = Department.builder().departmentId(UUID.randomUUID()).departmentType(DepartmentType.EXAMINATION).build();
        MedicalService mockService = MedicalService.builder().name("Khám").build();
        QueueTicket myTicket = QueueTicket.builder()
            .ticketId(UUID.randomUUID())
            .visit(visit)
            .status(QueueStatus.WAITING)
            .department(dept)
            .service(mockService)
            .workDate(java.time.LocalDate.now())
            .queueNumber(2)
            .build();
        myTicket.setCreatedAt(java.time.LocalDateTime.now());
            
        QueueTicket otherTicket = QueueTicket.builder()
            .ticketId(UUID.randomUUID())
            .visit(CustomerVisit.builder().status(VisitStatus.IN_PROGRESS).build())
            .status(QueueStatus.IN_PROGRESS)
            .department(dept)
            .service(mockService)
            .workDate(java.time.LocalDate.now())
            .queueNumber(1)
            .build();
        otherTicket.setCreatedAt(java.time.LocalDateTime.now());

        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of(myTicket));
        when(visitRepo.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc("123"))
            .thenReturn(List.of(visit));
        when(invoiceRepo.findAllByVisit_VisitId(id)).thenReturn(List.of());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(id)).thenReturn(List.of());
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of(myTicket));
        
        org.springframework.data.domain.Page<QueueTicket> page = new org.springframework.data.domain.PageImpl<>(List.of(otherTicket, myTicket));
        when(queueRepo.findWaitingPrioritized(eq(dept.getDepartmentId()), any(), any(), any()))
            .thenReturn(page);
            
        QueuePriorityService.RankedTicket rt1 = new QueuePriorityService.RankedTicket(otherTicket, null, true, new QueuePriorityService.PriorityInfo("REGULAR", "Khách", null, false));
        QueuePriorityService.RankedTicket rt2 = new QueuePriorityService.RankedTicket(myTicket, 1, false, new QueuePriorityService.PriorityInfo("REGULAR", "Khách", null, false));
        when(queuePriorityService.rank(any())).thenReturn(List.of(rt1, rt2));
        
        PatientQueueResponse res = service.lookupGuestQueue(toVisitCode(id), "123");
        assertEquals("WAITING", res.currentStatus());
        assertEquals(1, res.waitingPosition());
    }

    @Test
    void build_WithClinicalCycle_WorksWithoutCrash() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(id).status(VisitStatus.IN_PROGRESS).build();
        Department examDept = Department.builder().departmentId(UUID.randomUUID()).departmentType(DepartmentType.EXAMINATION).name("Kham").build();
        QueueTicket examQueue = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).status(QueueStatus.TEST_DONE).department(examDept).queueNumber(1).workDate(java.time.LocalDate.now()).build();
        examQueue.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        
        MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID()).visit(visit).queueTicket(examQueue).build();
        Invoice invoice = Invoice.builder().invoiceId(UUID.randomUUID()).visit(visit).medicalRecord(record).status(InvoiceStatus.PAID).build();
        invoice.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        
        InvoiceItem item = InvoiceItem.builder().invoice(invoice).build();
        TestRequest testReq = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record).status(TestRequestStatus.COMPLETED).invoiceItem(item).build();
        testReq.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of(examQueue));
        when(invoiceRepo.findAllByVisit_VisitId(id)).thenReturn(List.of(invoice));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(id)).thenReturn(List.of(testReq));
        when(visitRepo.findById(id)).thenReturn(java.util.Optional.of(visit));
        
        PatientJourneyResponse res = service.get(id);
        
        assertNotNull(res);
    }
    @Test
    void resolveGuestVisit_InvalidFormat_ThrowsException() {
        assertThrows(vn.edu.fpt.cares.exception.ResourceNotFoundException.class, () -> {
            service.lookupGuestQueue("VIS-123", "123");
        });
        assertThrows(vn.edu.fpt.cares.exception.ResourceNotFoundException.class, () -> {
            service.lookupGuestQueue("VIS-ABCDEFGH", "");
        });
        assertThrows(vn.edu.fpt.cares.exception.ResourceNotFoundException.class, () -> {
            service.lookupGuestQueue(null, "123");
        });
    }

    @Test
    void resolveGuestVisit_NotFound_ThrowsException() {
        when(visitRepo.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc("0901234567"))
            .thenReturn(List.of());
        assertThrows(vn.edu.fpt.cares.exception.ResourceNotFoundException.class, () -> {
            service.lookupGuestQueue("VIS-12345678", "0901234567");
        });
    }

    @Test
    void resolveGuestVisit_NotMatchCode_ThrowsException() {
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        when(visitRepo.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc("0901234567"))
            .thenReturn(List.of(visit));
        assertThrows(vn.edu.fpt.cares.exception.ResourceNotFoundException.class, () -> {
            service.lookupGuestQueue("VIS-12345678", "0901234567");
        });
    }
    @Test
    void advanceBlockedStep_NotFound_ThrowsException() {
        UUID id = UUID.randomUUID();
        when(visitRepo.findById(id)).thenReturn(java.util.Optional.empty());
        assertThrows(vn.edu.fpt.cares.exception.ResourceNotFoundException.class, () -> service.advanceBlockedStep(id));
    }

    @Test
    void advanceBlockedStep_HasSkippedQueue_ReturnsGet() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(id).build();
        when(visitRepo.findById(id)).thenReturn(java.util.Optional.of(visit));
        
        QueueTicket skipped = QueueTicket.builder().status(QueueStatus.SKIPPED).build();
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of(skipped));
        
        assertThrows(vn.edu.fpt.cares.exception.ConflictException.class, () -> service.advanceBlockedStep(id));
    }

    @Test
    void advanceBlockedStep_NoPhysicalActive_HasBlockedParaclinical_ActivatesTests() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(id).build();
        when(visitRepo.findById(id)).thenReturn(java.util.Optional.of(visit));
        
        Department paraDept = Department.builder().departmentId(UUID.randomUUID()).departmentType(DepartmentType.PARACLINICAL).build();
        QueueTicket blocked = QueueTicket.builder().ticketId(UUID.randomUUID()).status(QueueStatus.BLOCKED).department(paraDept).build();
        blocked.setCreatedAt(java.time.LocalDateTime.now());
        blocked.setQueueNumber(1);
        QueueTicket noDept = QueueTicket.builder().status(QueueStatus.BLOCKED).build();
        noDept.setCreatedAt(java.time.LocalDateTime.now());
        noDept.setQueueNumber(1);
        QueueTicket noType = QueueTicket.builder().status(QueueStatus.BLOCKED).department(Department.builder().build()).build();
        noType.setCreatedAt(java.time.LocalDateTime.now());
        noType.setQueueNumber(1);
        QueueTicket examDept = QueueTicket.builder().status(QueueStatus.BLOCKED).department(Department.builder().departmentType(DepartmentType.EXAMINATION).build()).build();
        examDept.setCreatedAt(java.time.LocalDateTime.now());
        examDept.setQueueNumber(1);
        
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of(blocked, noDept, noType, examDept));
        
        TestRequest blockedReq = TestRequest.builder().status(TestRequestStatus.BLOCKED).build();
        TestRequest pendingReq = TestRequest.builder().status(TestRequestStatus.PENDING).build();
        when(testRepo.findAllByQueueTicket_TicketId(blocked.getTicketId())).thenReturn(List.of(blockedReq, pendingReq));
        
        service.advanceBlockedStep(id);
        
        assertEquals(QueueStatus.WAITING, blocked.getStatus());
        assertEquals(TestRequestStatus.PENDING, blockedReq.getStatus());
        verify(queueRepo, times(1)).save(blocked);
        verify(testRepo, times(1)).save(blockedReq);
    }

    @Test
    void advanceBlockedStep_HasPhysicalActive_CallsActivateNext() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(id).build();
        when(visitRepo.findById(id)).thenReturn(java.util.Optional.of(visit));
        when(visitRepo.findByIdForUpdate(id)).thenReturn(java.util.Optional.of(visit));
        
        QueueTicket active = QueueTicket.builder().status(QueueStatus.IN_PROGRESS).build();
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of(active));
        
        service.advanceBlockedStep(id);
        verify(queueRepo, never()).save(any(QueueTicket.class));
    }
    @Test
    void activateNext_TestNotNullAndQueueNull_SetsPending() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(id).build();
        when(visitRepo.findByIdForUpdate(id)).thenReturn(java.util.Optional.of(visit));
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of());
        
        TestRequest test = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .status(TestRequestStatus.BLOCKED)
                .build();
        test.setCreatedAt(LocalDateTime.now());
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(id)).thenReturn(List.of(test));
        
        service.activateNext(id);
        
        assertEquals(TestRequestStatus.PENDING, test.getStatus());
        verify(testRepo).save(test);
    }

    @Test
    void activateNext_ExaminationWaitingForTest_NoOtherQueues_Returns() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(id).build();
        when(visitRepo.findByIdForUpdate(id)).thenReturn(java.util.Optional.of(visit));
        
        Department examDept = Department.builder().departmentType(DepartmentType.EXAMINATION).build();
        QueueTicket waitingExam = QueueTicket.builder()
                .status(QueueStatus.WAITING_FOR_TEST)
                .department(examDept)
                .build();
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of(waitingExam));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(id)).thenReturn(List.of());
        
        service.activateNext(id);
        
        verify(queueRepo, never()).save(any(QueueTicket.class));
        verify(testRepo, never()).save(any(TestRequest.class));
        verify(visitRepo, never()).save(any(CustomerVisit.class));
    }

    @Test
    void activateNext_NoQueuesOrTests_CompletesVisit() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(id).status(VisitStatus.IN_PROGRESS).build();
        when(visitRepo.findByIdForUpdate(id)).thenReturn(java.util.Optional.of(visit));
        
        QueueTicket completedQueue = QueueTicket.builder().status(QueueStatus.DONE).build();
        when(queueRepo.findAllByVisit_VisitId(id)).thenReturn(List.of(completedQueue));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(id)).thenReturn(List.of());
        when(visitRepo.findById(id)).thenReturn(java.util.Optional.of(visit));
        
        service.activateNext(id);
        
        assertEquals(VisitStatus.COMPLETED, visit.getStatus());
        assertNotNull(visit.getCheckOutTime());
        verify(visitRepo).save(visit);
    }
}
