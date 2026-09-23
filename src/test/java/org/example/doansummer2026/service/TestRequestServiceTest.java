package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestRequestServiceTest {
    @Mock TestRequestRepository repo;
    @Mock TestResultRepository resultRepo;
    @Mock MedicalRecordRepository recordRepo;
    @Mock CustomerVisitRepository visitRepo;
    @Mock MedicalServiceRepository serviceRepo;
    @Mock StaffInfoRepository staffRepo;
    @Mock QueueTicketRepository queueTicketRepo;
    @Mock DepartmentRepository departmentRepo;
    @Mock InvoiceItemRepository invoiceItemRepo;
    @Mock MedicalRecordService medicalRecordService;
    @Mock PatientJourneyService patientJourneyService;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock NotificationService notificationService;
    @Mock AuthService authService;
    @Mock ClinicalFormEngine clinicalFormEngine;
    @Mock FixedClinicalFormService fixedClinicalFormService;
    @Mock SameDayParaclinicalResultService sameDayParaclinicalResultService;
    @Mock StaffDutyService staffDutyService;
    @Mock MedicalServiceSelectionPolicyService serviceSelectionPolicyService;
    @InjectMocks TestRequestService service;

    @Test
    void getRejectsUnknownRequest() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.get(id));
    }

    @Test
    void startRequestsChangesOnlyPendingRows() {
        UUID queueId = UUID.randomUUID();
        TestRequest pending = request(TestRequestStatus.PENDING);
        TestRequest completed = request(TestRequestStatus.COMPLETED);
        when(repo.findAllByQueueTicket_TicketId(queueId)).thenReturn(List.of(pending, completed));

        service.startRequestsForQueue(queueId);

        assertEquals(TestRequestStatus.IN_PROGRESS, pending.getStatus());
        assertEquals(TestRequestStatus.COMPLETED, completed.getStatus());
        verify(repo).save(pending);
        verify(repo, never()).save(completed);
    }

    @Test
    void blockAndRestorePreserveTerminalRows() {
        UUID queueId = UUID.randomUUID();
        TestRequest pending = request(TestRequestStatus.PENDING);
        TestRequest completed = request(TestRequestStatus.COMPLETED);
        when(repo.findAllByQueueTicket_TicketId(queueId)).thenReturn(List.of(pending, completed));

        service.blockRequestsForQueue(queueId);
        assertEquals(TestRequestStatus.BLOCKED, pending.getStatus());
        assertEquals(TestRequestStatus.COMPLETED, completed.getStatus());

        service.restoreRequestsForQueue(queueId, false);
        assertEquals(TestRequestStatus.PENDING, pending.getStatus());
        assertEquals(TestRequestStatus.COMPLETED, completed.getStatus());
    }

    @Test
    void incompleteCheckDelegatesToRepository() {
        UUID recordId = UUID.randomUUID();
        when(repo.countByMedicalRecordAndStatusIn(eq(recordId), anyList())).thenReturn(2L);
        assertTrue(service.hasIncompleteRequestsForRecord(recordId));
        when(repo.countByMedicalRecordAndStatusIn(eq(recordId), anyList())).thenReturn(0L);
        assertFalse(service.hasIncompleteRequestsForRecord(recordId));
    }

    private TestRequest request(TestRequestStatus status) {
        return TestRequest.builder().testRequestId(UUID.randomUUID()).status(status).build();
    }
}
