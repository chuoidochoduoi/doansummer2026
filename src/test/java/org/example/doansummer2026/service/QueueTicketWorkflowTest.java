package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.*;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueTicketWorkflowTest {
    @Mock QueueTicketRepository repo;
    @Mock CustomerVisitRepository visitRepo;
    @Mock DepartmentRepository departmentRepo;
    @Mock StaffDutyService staffDutyService;
    @Mock TestRequestService testRequestService;
    @Mock TestRequestRepository testRequestRepository;
    @Mock PatientJourneyService patientJourneyService;
    @Mock QueuePriorityService queuePriorityService;
    @Mock MedicalRecordRepository recordRepo;
    @Mock MedicalRecordService medicalRecordService;
    @InjectMocks QueueTicketService service;

    private QueueTicket ticket(QueueStatus status) {
        return QueueTicket.builder().ticketId(UUID.randomUUID()).status(status)
                .department(Department.builder().departmentId(UUID.randomUUID()).departmentType(DepartmentType.PARACLINICAL).build())
                .workDate(LocalDate.of(2026,9,4)).build();
    }

    @ParameterizedTest @EnumSource(value=QueueStatus.class,names="CALLED",mode=EnumSource.Mode.EXCLUDE)
    void skipOnlyAcceptsCalledTickets(QueueStatus status) {
        var q=ticket(status); when(repo.findByIdForUpdate(q.getTicketId())).thenReturn(Optional.of(q));
        assertThrows(BadRequestException.class, () -> service.skip(q.getTicketId()));
        assertEquals(status,q.getStatus()); verify(repo,never()).save(any());
        verifyNoInteractions(testRequestService,patientJourneyService);
    }

    @ParameterizedTest @EnumSource(value=QueueStatus.class,names="IN_PROGRESS",mode=EnumSource.Mode.EXCLUDE)
    void finishLabOnlyAcceptsInProgressTickets(QueueStatus status) {
        var q=ticket(status); when(repo.findByIdForUpdate(q.getTicketId())).thenReturn(Optional.of(q));
        assertThrows(BadRequestException.class, () -> service.finishParaclinicalQueue(q.getTicketId()));
        verify(repo,never()).save(any()); verifyNoInteractions(testRequestRepository,patientJourneyService);
    }

    @ParameterizedTest @ValueSource(longs={1,3})
    void labCannotFinishWhileAnyRequestsRemain(long remaining) {
        var q=ticket(QueueStatus.IN_PROGRESS); when(repo.findByIdForUpdate(q.getTicketId())).thenReturn(Optional.of(q));
        when(testRequestRepository.countByQueueTicket_TicketIdAndStatusIn(q.getTicketId(),List.of(TestRequestStatus.PENDING,TestRequestStatus.IN_PROGRESS,TestRequestStatus.BLOCKED))).thenReturn(remaining);
        assertThrows(ConflictException.class, () -> service.finishParaclinicalQueue(q.getTicketId()));
        assertEquals(QueueStatus.IN_PROGRESS,q.getStatus()); verify(repo,never()).save(any()); verifyNoInteractions(patientJourneyService);
    }

    @ParameterizedTest @EnumSource(value=QueueStatus.class,names="SKIPPED",mode=EnumSource.Mode.EXCLUDE)
    void returnRequiresSkippedTicket(QueueStatus status) {
        var q=ticket(status); when(repo.findByIdForUpdate(q.getTicketId())).thenReturn(Optional.of(q));
        assertThrows(BadRequestException.class, () -> service.confirmReturnToQueue(q.getTicketId()));
        verify(repo,never()).save(any()); verifyNoInteractions(testRequestService,patientJourneyService);
    }

    @ParameterizedTest @ValueSource(ints={-1,1})
    void skippedTicketsCannotBeRestoredOutsideTheirWorkDate(int offset) {
        LocalDate today=LocalDate.of(2026,9,4); var q=ticket(QueueStatus.SKIPPED); q.setWorkDate(today.plusDays(offset));
        when(repo.findByIdForUpdate(q.getTicketId())).thenReturn(Optional.of(q));
        try(var dates=mockStatic(LocalDate.class,CALLS_REAL_METHODS)) {
            dates.when(() -> LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))).thenReturn(today);
            assertThrows(BadRequestException.class, () -> service.confirmReturnToQueue(q.getTicketId()));
        }
        assertEquals(QueueStatus.SKIPPED,q.getStatus()); verify(repo,never()).save(any());
        verifyNoInteractions(testRequestService,patientJourneyService);
    }

    @Test void yesterdayCompletedTestsCanBeCalledThenStartedToCloseOriginalRecord() {
        LocalDate today=LocalDate.of(2026,9,4);
        LocalDateTime callTime=today.atTime(9,0);
        var q=ticket(QueueStatus.TEST_DONE); q.setWorkDate(today.minusDays(1));
        q.getDepartment().setDepartmentType(DepartmentType.EXAMINATION);
        var visit=CustomerVisit.builder().visitId(UUID.randomUUID()).build(); q.setVisit(visit);
        var doctor=StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(SystemRole.DOCTOR).build();
        var record=MedicalRecord.builder().recordId(UUID.randomUUID()).visit(visit).queueTicket(q).doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS).build();
        when(repo.findByIdForUpdate(q.getTicketId())).thenReturn(Optional.of(q));
        when(departmentRepo.findByIdForUpdate(q.getDepartment().getDepartmentId())).thenReturn(Optional.of(q.getDepartment()));
        when(repo.findWaitingPrioritized(eq(q.getDepartment().getDepartmentId()),eq(q.getWorkDate()),anyList(),any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(q)));
        when(queuePriorityService.rank(List.of(q))).thenReturn(List.of(new QueuePriorityService.RankedTicket(q,1,true,null)));
        when(repo.save(q)).thenReturn(q);
        lenient().when(staffDutyService.requireCurrentStaffOnDuty(q.getDepartment(),true)).thenReturn(doctor);
        lenient().when(recordRepo.findByQueueTicket_TicketId(q.getTicketId())).thenReturn(Optional.of(record));
        lenient().when(medicalRecordService.inheritFirstVisitVitalSigns(record)).thenReturn(record);
        try(var dates=mockStatic(LocalDate.class,CALLS_REAL_METHODS);
            var dateTimes=mockStatic(LocalDateTime.class,CALLS_REAL_METHODS)) {
            dates.when(() -> LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))).thenReturn(today);
            dateTimes.when(LocalDateTime::now).thenReturn(callTime);
            service.call(q.getTicketId());
            assertEquals(QueueStatus.CALLED,q.getStatus());
            assertEquals(callTime,q.getCalledAt());
            assertDoesNotThrow(() -> service.startExam(q.getTicketId()));
            assertEquals(QueueStatus.IN_PROGRESS,q.getStatus());
        }
    }


    @Test void deletingExistingTicketIsForbiddenToPreserveHistory() {
        var q=ticket(QueueStatus.DONE); when(repo.findById(q.getTicketId())).thenReturn(Optional.of(q));
        assertThrows(ConflictException.class, () -> service.delete(q.getTicketId()));
        verify(repo,never()).deleteById(any());
    }
}
