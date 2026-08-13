package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.dto.queueTicket.QueueTicketCreateRequest;
import org.example.doansummer2026.dto.queueTicket.QueueTicketUpdateRequest;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.example.doansummer2026.service.interfaces.InvoiceServiceInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.dto.medicalRecord.PrescriptionItemCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.TestRequestInExaminationRequest;
import org.example.doansummer2026.dto.icd.ICD10SelectionCreateRequest;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.Icd10Code;
import org.example.doansummer2026.model.VitalSigns;
import org.example.doansummer2026.model.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@ExtendWith(MockitoExtension.class)
class QueueTicketServiceTest {

    @Mock
    private QueueTicketRepository repo;

    @Mock
    private CustomerVisitRepository visitRepo;

    @Mock
    private DepartmentRepository departmentRepo;

    @Mock
    private MedicalServiceRepository serviceRepo;

    @Mock
    private MedicalRecordRepository recordRepo;

    @Mock
    private StaffInfoRepository staffRepo;

    @Mock
    private Icd10CodeRepository icd10Repo;

    @Mock
    private TestRequestService testRequestService;

    @Mock
    private PatientJourneyService patientJourneyService;

    @Mock
    private MedicalRecordService medicalRecordService;

    @Mock
    private InvoiceRepository invoiceRepo;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationService notificationService;

    @Mock
    private InvoiceServiceInterface invoiceService;

    @InjectMocks
    private QueueTicketService queueTicketService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(
                queueTicketService,
                "invoiceService",
                invoiceService
        );
    }
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Department department(UUID id) {
        return Department.builder()
                .departmentId(id)
                .build();
    }

    private QueueTicket ticket(
            UUID ticketId,
            QueueStatus status,
            CustomerVisit visit,
            Department department
    ) {
        return QueueTicket.builder()
                .ticketId(ticketId)
                .status(status)
                .visit(visit)
                .department(department)
                .workDate(LocalDate.now())
                .build();
    }

    private void setStaffPrincipal(UUID staffId, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                Map.of("staffId", staffId.toString()),
                null,
                List.of(new SimpleGrantedAuthority(role))
        );

        SecurityContextHolder.getContext()
                .setAuthentication(auth);
    }

    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenExists() {

        UUID id = UUID.randomUUID();
        QueueTicket q = mock(QueueTicket.class);

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        assertSame(
                q,
                queueTicketService.findById(id)
        );
    }

    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> queueTicketService.findById(id)
        );
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void create_ShouldThrow_WhenVisitMissing() {

        UUID visitId = UUID.randomUUID();

        QueueTicketCreateRequest req =
                mock(QueueTicketCreateRequest.class);

        when(req.visitId()).thenReturn(visitId);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> queueTicketService.create(req)
        );
    }

    @Test
    void create_ShouldReturnExisting_WhenQueueAlreadyExists() {

        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        QueueTicketCreateRequest req =
                mock(QueueTicketCreateRequest.class);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        QueueTicket existing =
                QueueTicket.builder()
                        .ticketId(UUID.randomUUID())
                        .status(QueueStatus.WAITING)
                        .build();

        when(req.visitId()).thenReturn(visitId);
        when(req.serviceId()).thenReturn(serviceId);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(
                repo.findByVisit_VisitIdAndService_ServiceId(
                        visitId,
                        serviceId
                )
        ).thenReturn(Optional.of(existing));

        assertNotNull(
                queueTicketService.create(req)
        );

        verify(repo, never())
                .save(any());
    }

    @Test
    void create_ShouldThrow_WhenDepartmentMissing() {

        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        QueueTicketCreateRequest req =
                mock(QueueTicketCreateRequest.class);

        when(req.visitId()).thenReturn(visitId);
        when(req.serviceId()).thenReturn(serviceId);
        when(req.departmentId()).thenReturn(deptId);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(mock(CustomerVisit.class)));

        when(
                repo.findByVisit_VisitIdAndService_ServiceId(
                        visitId,
                        serviceId
                )
        ).thenReturn(Optional.empty());

        when(departmentRepo.findByIdForUpdate(deptId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> queueTicketService.create(req)
        );
    }

    @Test
    void create_ShouldThrow_WhenServiceMissing() {

        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        QueueTicketCreateRequest req =
                mock(QueueTicketCreateRequest.class);

        Department dept =
                department(deptId);

        when(req.visitId()).thenReturn(visitId);
        when(req.serviceId()).thenReturn(serviceId);
        when(req.departmentId()).thenReturn(deptId);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(mock(CustomerVisit.class)));

        when(
                repo.findByVisit_VisitIdAndService_ServiceId(
                        visitId,
                        serviceId
                )
        ).thenReturn(Optional.empty());

        when(departmentRepo.findByIdForUpdate(deptId))
                .thenReturn(Optional.of(dept));

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> queueTicketService.create(req)
        );
    }

    @Test
    void create_ShouldCreateWaitingQueueWithNextNumber() {

        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        QueueTicketCreateRequest req =
                mock(QueueTicketCreateRequest.class);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        Department dept =
                department(deptId);

        dept.setDepartmentType(DepartmentType.LABORATORY);

        MedicalService service =
                mock(MedicalService.class);

        when(req.visitId()).thenReturn(visitId);
        when(req.serviceId()).thenReturn(serviceId);
        when(req.departmentId()).thenReturn(deptId);
        when(req.workDate()).thenReturn(LocalDate.now());

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(
                repo.findByVisit_VisitIdAndService_ServiceId(
                        visitId,
                        serviceId
                )
        ).thenReturn(Optional.empty());

        when(departmentRepo.findByIdForUpdate(deptId))
                .thenReturn(Optional.of(dept));

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(
                repo.findMaxQueueNumberForDay(
                        deptId,
                        LocalDate.now()
                )
        ).thenReturn(Optional.of(4));

        when(repo.save(any(QueueTicket.class)))
                .thenAnswer(i -> {
                    QueueTicket q = i.getArgument(0);
                    q.setTicketId(UUID.randomUUID());
                    return q;
                });

        // updateDepartmentStatus
        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.of(dept));

        when(repo.countActiveTicketsByDepartment(deptId))
                .thenReturn(1L);

        var result =
                queueTicketService.create(req);

        assertNotNull(result);

        verify(repo).save(argThat(q ->
                q.getVisit() == visit
                        && q.getDepartment() == dept
                        && q.getService() == service
                        && q.getQueueNumber() == 5
                        && q.getStatus() == QueueStatus.WAITING
        ));
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void update_ShouldRejectInProgress_FromInvalidStatus() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.WAITING,
                        null,
                        department(deptId)
                );

        QueueTicketUpdateRequest req =
                mock(QueueTicketUpdateRequest.class);

        when(req.status())
                .thenReturn(QueueStatus.IN_PROGRESS);

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.update(id, req)
        );
    }

    @Test
    void update_ShouldRejectInProgress_WhenRoomAlreadyBusy() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                department(deptId);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.CALLED,
                        null,
                        dept
                );

        QueueTicketUpdateRequest req =
                mock(QueueTicketUpdateRequest.class);

        when(req.status())
                .thenReturn(QueueStatus.IN_PROGRESS);

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.countInprogressByDepartment(deptId))
                .thenReturn(1L);

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.update(id, req)
        );
    }

    @Test
    void update_ShouldSetCalledAt_WhenCalled() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                department(deptId);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.WAITING,
                        null,
                        dept
                );

        QueueTicketUpdateRequest req =
                mock(QueueTicketUpdateRequest.class);

        when(req.status())
                .thenReturn(QueueStatus.CALLED);

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.update(id, req);

        assertEquals(
                QueueStatus.CALLED,
                q.getStatus()
        );

        assertNotNull(
                q.getCalledAt()
        );
    }

    @Test
    void update_ShouldSetCompletedAt_WhenDone() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                department(deptId);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.IN_PROGRESS,
                        null,
                        dept
                );

        QueueTicketUpdateRequest req =
                mock(QueueTicketUpdateRequest.class);

        when(req.status())
                .thenReturn(QueueStatus.DONE);

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.update(id, req);

        assertEquals(
                QueueStatus.DONE,
                q.getStatus()
        );

        assertNotNull(
                q.getCompletedAt()
        );
    }

    // =========================================================
    // CALL
    // =========================================================

    @Test
    void call_ShouldRejectInvalidStatus() {

        UUID id = UUID.randomUUID();

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.IN_PROGRESS,
                        null,
                        department(UUID.randomUUID())
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.call(id)
        );
    }

    @Test
    void call_ShouldMoveWaitingToCalled() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                department(deptId);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.WAITING,
                        null,
                        dept
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        var result =
                queueTicketService.call(id);

        assertNotNull(result);

        assertEquals(
                QueueStatus.CALLED,
                q.getStatus()
        );

        assertNotNull(
                q.getCalledAt()
        );
    }

    // =========================================================
    // START EXAM
    // =========================================================

    @Test
    void startExam_ShouldRejectInvalidQueueStatus() {

        UUID id = UUID.randomUUID();

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.WAITING,
                        null,
                        department(UUID.randomUUID())
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.startExam(id)
        );
    }

    @Test
    void startExam_ShouldReject_WhenDepartmentAlreadyHasPatient() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                department(deptId);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.CALLED,
                        null,
                        dept
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.countInprogressByDepartment(deptId))
                .thenReturn(1L);

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.startExam(id)
        );
    }

    @Test
    void startExam_ShouldReject_WhenNoStaffIdInPrincipal() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                department(deptId);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.CALLED,
                        null,
                        dept
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.countInprogressByDepartment(deptId))
                .thenReturn(0L);

        var auth =
                new UsernamePasswordAuthenticationToken(
                        "user",
                        null,
                        List.of()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(auth);

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.startExam(id)
        );
    }

    @Test
    void startExam_ShouldRejectNurse_WhenNoHeadDoctor() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();

        Department dept =
                department(deptId);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.CALLED,
                        mock(CustomerVisit.class),
                        dept
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.countInprogressByDepartment(deptId))
                .thenReturn(0L);

        setStaffPrincipal(
                nurseId,
                "ROLE_NURSE"
        );

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.startExam(id)
        );
    }

    @Test
    void startExam_ShouldCreateRecordAndSetInProgress_ForDoctor() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        Department dept =
                department(deptId);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.CALLED,
                        visit,
                        dept
                );

        StaffInfo doctor =
                mock(StaffInfo.class);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(UUID.randomUUID())
                        .visit(visit)
                        .queueTicket(q)
                        .doctor(doctor)
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .build();

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.countInprogressByDepartment(deptId))
                .thenReturn(0L);

        setStaffPrincipal(
                doctorId,
                "ROLE_DOCTOR"
        );

        when(recordRepo.findByQueueTicket_TicketId(id))
                .thenReturn(Optional.empty());

        when(staffRepo.findById(doctorId))
                .thenReturn(Optional.of(doctor));

        when(recordRepo.save(any(MedicalRecord.class)))
                .thenReturn(record);

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        var result =
                queueTicketService.startExam(id);

        assertNotNull(result);

        assertEquals(
                QueueStatus.IN_PROGRESS,
                q.getStatus()
        );

        verify(recordRepo)
                .save(any(MedicalRecord.class));
    }

    // =========================================================
    // COMPLETE SIMPLE
    // =========================================================

    @Test
    void complete_ShouldSetDoneAndActivateNext() {

        UUID id = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getVisitId())
                .thenReturn(visitId);

        Department dept =
                department(deptId);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.IN_PROGRESS,
                        visit,
                        dept
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.complete(id);

        assertEquals(
                QueueStatus.DONE,
                q.getStatus()
        );

        assertNotNull(
                q.getCompletedAt()
        );

        verify(patientJourneyService)
                .activateNext(visitId);
    }

    // =========================================================
    // SKIP / RETURN
    // =========================================================

    @Test
    void skip_ShouldSetSkipped() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                department(deptId);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.WAITING,
                        null,
                        dept
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.skip(id);

        assertEquals(
                QueueStatus.SKIPPED,
                q.getStatus()
        );
    }

    @Test
    void returnToQueue_ShouldReject_WhenNotSkipped() {

        UUID id = UUID.randomUUID();

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.WAITING,
                        null,
                        department(UUID.randomUUID())
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.returnToQueue(id)
        );
    }

    @Test
    void returnToQueue_ShouldReject_WhenDifferentWorkDate() {

        UUID id = UUID.randomUUID();

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.SKIPPED,
                        null,
                        department(UUID.randomUUID())
                );

        q.setWorkDate(
                LocalDate.now().minusDays(1)
        );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.returnToQueue(id)
        );
    }

    @Test
    void returnToQueue_ShouldRestoreWaiting() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                department(deptId);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.SKIPPED,
                        null,
                        dept
                );

        q.setCalledAt(
                java.time.LocalDateTime.now()
        );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.returnToQueue(id);

        assertEquals(
                QueueStatus.WAITING,
                q.getStatus()
        );

        assertNull(
                q.getCalledAt()
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> queueTicketService.delete(id)
        );
    }

    @Test
    void delete_ShouldDeleteAndUpdateDepartment() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                department(deptId);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.WAITING,
                        null,
                        dept
                );

        when(repo.existsById(id))
                .thenReturn(true);

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.delete(id);

        verify(repo)
                .deleteById(id);
    }

    // =========================================================
    // IN-PROGRESS BY DEPARTMENT
    // =========================================================

    @Test
    void getInprogressByDepartment_ShouldReturnNull_WhenNone() {

        UUID deptId = UUID.randomUUID();

        when(
                repo.findTopByDepartment_DepartmentIdAndStatusOrderByCreatedAtAsc(
                        deptId,
                        QueueStatus.IN_PROGRESS
                )
        ).thenReturn(Optional.empty());

        assertNull(
                queueTicketService
                        .getInprogressByDepartment(deptId)
        );
    }

    // =========================================================
    // WAITING BY DEPARTMENT - FOUR BRANCHES
    // =========================================================

    @Test
    void getWaitingByDepartment_ShouldUseStatusAndDateQuery() {

        UUID deptId = UUID.randomUUID();

        LocalDate date = LocalDate.now();

        var pageable =
                PageRequest.of(0, 10);

        when(
                repo.findByDepartment_DepartmentIdAndWorkDateAndStatus(
                        deptId,
                        date,
                        QueueStatus.WAITING,
                        pageable
                )
        ).thenReturn(new PageImpl<>(List.of()));

        assertNotNull(
                queueTicketService.getWaitingByDepartment(
                        deptId,
                        date,
                        QueueStatus.WAITING,
                        pageable
                )
        );
    }

    @Test
    void getWaitingByDepartment_ShouldUseStatusOnlyQuery() {

        UUID deptId = UUID.randomUUID();

        var pageable =
                PageRequest.of(0, 10);

        when(
                repo.findByDepartment_DepartmentIdAndStatus(
                        deptId,
                        QueueStatus.WAITING,
                        pageable
                )
        ).thenReturn(new PageImpl<>(List.of()));

        assertNotNull(
                queueTicketService.getWaitingByDepartment(
                        deptId,
                        null,
                        QueueStatus.WAITING,
                        pageable
                )
        );
    }

    @Test
    void getWaitingByDepartment_ShouldUsePrioritizedQuery_WhenDateSpecified() {

        UUID deptId = UUID.randomUUID();

        LocalDate date = LocalDate.now();

        var pageable =
                PageRequest.of(0, 10);

        when(
                repo.findWaitingPrioritized(
                        eq(deptId),
                        eq(date),
                        anyList(),
                        eq(pageable)
                )
        ).thenReturn(new PageImpl<>(List.of()));

        assertNotNull(
                queueTicketService.getWaitingByDepartment(
                        deptId,
                        date,
                        null,
                        pageable
                )
        );
    }

    @Test
    void getWaitingByDepartment_ShouldUseStatusIn_WhenNoStatusOrDate() {

        UUID deptId = UUID.randomUUID();

        var pageable =
                PageRequest.of(0, 10);

        when(
                repo.findByDepartment_DepartmentIdAndStatusIn(
                        eq(deptId),
                        anyList(),
                        eq(pageable)
                )
        ).thenReturn(new PageImpl<>(List.of()));

        assertNotNull(
                queueTicketService.getWaitingByDepartment(
                        deptId,
                        null,
                        null,
                        pageable
                )
        );
    }

    // =========================================================
    // COUNTERS
    // =========================================================

    @Test
    void countWaitingForTest_ShouldDelegate() {

        UUID deptId = UUID.randomUUID();

        when(
                repo.countWaitingForTestByDepartment(deptId)
        ).thenReturn(5L);

        assertEquals(
                5L,
                queueTicketService
                        .countWaitingForTestByDepartment(deptId)
        );
    }

    @Test
    void countTestDone_ShouldDelegate() {

        UUID deptId = UUID.randomUUID();

        when(
                repo.countTestDoneByDepartment(deptId)
        ).thenReturn(3L);

        assertEquals(
                3L,
                queueTicketService
                        .countTestDoneByDepartment(deptId)
        );
    }

    // =========================================================
    // MARK TEST DONE
    // =========================================================

    @Test
    void markTestDone_ShouldThrow_WhenInvalidStatus() {

        UUID id = UUID.randomUUID();

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.WAITING,
                        null,
                        department(UUID.randomUUID())
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.markTestDone(id)
        );
    }

    @Test
    void markTestDone_ShouldChangeWaitingForTestToTestDone() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                department(deptId);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.WAITING_FOR_TEST,
                        null,
                        dept
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.markTestDone(id);

        assertEquals(
                QueueStatus.TEST_DONE,
                q.getStatus()
        );
    }

    // =========================================================
    // UPDATE DEPARTMENT STATUS INDIRECT COVERAGE
    // =========================================================

    @Test
    void complete_ShouldSetDepartmentInSession_WhenActiveTicketsRemain() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                mock(Department.class);

        when(dept.getDepartmentId())
                .thenReturn(deptId);

        when(dept.getStatus())
                .thenReturn(DepartmentStatus.AVAILABLE);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.IN_PROGRESS,
                        null,
                        dept
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.of(dept));

        when(repo.countActiveTicketsByDepartment(deptId))
                .thenReturn(1L);

        queueTicketService.complete(id);

        verify(dept)
                .setStatus(DepartmentStatus.IN_SESSION);

        verify(departmentRepo)
                .save(dept);
    }

    @Test
    void complete_ShouldSetDepartmentAvailable_WhenNoActiveTicketsRemain() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                mock(Department.class);

        when(dept.getDepartmentId())
                .thenReturn(deptId);

        when(dept.getStatus())
                .thenReturn(DepartmentStatus.IN_SESSION);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.IN_PROGRESS,
                        null,
                        dept
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.of(dept));

        when(repo.countActiveTicketsByDepartment(deptId))
                .thenReturn(0L);

        queueTicketService.complete(id);

        verify(dept)
                .setStatus(DepartmentStatus.AVAILABLE);

        verify(departmentRepo)
                .save(dept);
    }

    @Test
    void complete_ShouldNotChangeDepartment_WhenMaintenance() {

        UUID id = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        Department dept =
                mock(Department.class);

        when(dept.getDepartmentId())
                .thenReturn(deptId);

        when(dept.getStatus())
                .thenReturn(DepartmentStatus.MAINTENANCE);

        QueueTicket q =
                ticket(
                        id,
                        QueueStatus.IN_PROGRESS,
                        null,
                        dept
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.of(dept));

        queueTicketService.complete(id);

        verify(departmentRepo, never())
                .save(dept);
    }
    // =========================================================
// QUEUE TICKET SERVICE - COVER CÁC VÙNG ĐỎ CÒN LẠI
// search / get / notifyDoctors / getAllInprogress
// getMedicalRecord / getMedicalRecordByQueueTicket
// =========================================================


// =========================================================
// SEARCH
// =========================================================

    @Test
    void search_ShouldReturnEmptyPage_WhenNoQueueTicketFound() {

        UUID departmentId = UUID.randomUUID();
        LocalDate workDate = LocalDate.now();

        var pageable = PageRequest.of(0, 10);

        when(
                repo.search(
                        departmentId,
                        workDate,
                        QueueStatus.WAITING,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        var result = queueTicketService.search(
                departmentId,
                workDate,
                QueueStatus.WAITING,
                pageable
        );

        assertNotNull(result);

        verify(repo).search(
                departmentId,
                workDate,
                QueueStatus.WAITING,
                pageable
        );
    }


    @Test
    void search_ShouldMapQueueAndLoadRecordId() {

        UUID departmentId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        var pageable = PageRequest.of(0, 10);

        /*
         * Dùng deep mock để QueueTicketResponse.from(...)
         * có thể đọc các nested getter mà không NPE.
         */
        QueueTicket q =
                mock(QueueTicket.class, RETURNS_DEEP_STUBS);

        when(q.getTicketId())
                .thenReturn(ticketId);

        when(q.getStatus())
                .thenReturn(QueueStatus.WAITING);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .build();

        when(
                repo.search(
                        departmentId,
                        null,
                        null,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of(q))
        );

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        var result =
                queueTicketService.search(
                        departmentId,
                        null,
                        null,
                        pageable
                );

        assertNotNull(result);

        verify(recordRepo)
                .findByQueueTicket_TicketId(ticketId);
    }


// =========================================================
// GET
// =========================================================

    @Test
    void get_ShouldReturnResponse_WhenQueueExists() {

        UUID id = UUID.randomUUID();

        QueueTicket q =
                mock(QueueTicket.class, RETURNS_DEEP_STUBS);

        when(q.getTicketId())
                .thenReturn(id);

        when(q.getStatus())
                .thenReturn(QueueStatus.WAITING);

        when(repo.findById(id))
                .thenReturn(Optional.of(q));

        var result =
                queueTicketService.get(id);

        assertNotNull(result);

        verify(repo).findById(id);
    }


    @Test
    void get_ShouldThrow_WhenQueueDoesNotExist() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> queueTicketService.get(id)
        );
    }

// =========================================================
// NOTIFY DOCTORS
// =========================================================

    @Test
    void notifyDoctors_ShouldCreateNotification_ForHeadDoctor() {

        UUID ticketId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID doctorProfileId = UUID.randomUUID();

        Profile customer = mock(Profile.class);

        when(customer.getFullName())
                .thenReturn("Nguyen Van A");

        CustomerVisit visit = mock(CustomerVisit.class);

        when(visit.getCustomer())
                .thenReturn(customer);

        Profile doctorProfile = mock(Profile.class);

        when(doctorProfile.getProfileId())
                .thenReturn(doctorProfileId);

        StaffInfo headDoctor = mock(StaffInfo.class);

        when(headDoctor.getProfile())
                .thenReturn(doctorProfile);

        Department department =
                Department.builder()
                        .departmentId(departmentId)
                        .name("Phong Kham 01")
                        .departmentType(DepartmentType.EXAMINATION)
                        .status(DepartmentStatus.AVAILABLE)
                        .headDoctor(headDoctor)
                        .build();

        QueueTicket ticket =
                QueueTicket.builder()
                        .ticketId(ticketId)
                        .visit(visit)
                        .department(department)
                        .status(QueueStatus.WAITING)
                        .workDate(LocalDate.now())
                        .queueNumber(1)
                        .build();

        ReflectionTestUtils.invokeMethod(
                queueTicketService,
                "notifyDoctors",
                ticket
        );

        verify(notificationService)
                .create(
                        argThat(notification ->
                                doctorProfileId.equals(
                                        notification.recipientId()
                                )
                                        && notification.notificationType()
                                        == NotificationType.GENERAL

                                        && notification.channel()
                                        == NotificationChannel.IN_APP

                                        && "Benh nhan moi".equals(
                                        notification.title()
                                )

                                        && notification.content() != null

                                        && notification.content()
                                        .contains("Nguyen Van A")

                                        && notification.content()
                                        .contains("Phong Kham 01")

                                        && "QueueTicket".equals(
                                        notification.relatedEntity()
                                )

                                        && ticketId.equals(
                                        notification.relatedEntityId()
                                )
                        )
                );
    }


    @Test
    void notifyDoctors_ShouldUseGuestName_WhenVisitHasNoCustomer() {

        UUID ticketId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID doctorProfileId = UUID.randomUUID();

        Appointment appointment =
                mock(Appointment.class);

        when(appointment.getGuestFullName())
                .thenReturn("Tran Van Guest");

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getAppointment())
                .thenReturn(appointment);

        Profile doctorProfile =
                mock(Profile.class);

        when(doctorProfile.getProfileId())
                .thenReturn(doctorProfileId);

        StaffInfo headDoctor =
                mock(StaffInfo.class);

        when(headDoctor.getProfile())
                .thenReturn(doctorProfile);

        Department department =
                Department.builder()
                        .departmentId(departmentId)
                        .name("Phong Kham Guest")
                        .departmentType(DepartmentType.EXAMINATION)
                        .status(DepartmentStatus.AVAILABLE)
                        .headDoctor(headDoctor)
                        .build();

        QueueTicket ticket =
                QueueTicket.builder()
                        .ticketId(ticketId)
                        .visit(visit)
                        .department(department)
                        .status(QueueStatus.WAITING)
                        .workDate(LocalDate.now())
                        .queueNumber(1)
                        .build();

        ReflectionTestUtils.invokeMethod(
                queueTicketService,
                "notifyDoctors",
                ticket
        );

        verify(notificationService)
                .create(
                        argThat(notification ->
                                doctorProfileId.equals(
                                        notification.recipientId()
                                )
                                        && notification.content() != null

                                        && notification.content()
                                        .contains("Tran Van Guest")

                                        && notification.content()
                                        .contains("Phong Kham Guest")

                                        && "QueueTicket".equals(
                                        notification.relatedEntity()
                                )

                                        && ticketId.equals(
                                        notification.relatedEntityId()
                                )
                        )
                );
    }


    @Test
    void notifyDoctors_ShouldNotCreateNotification_WhenDepartmentHasNoHeadDoctor() {

        UUID ticketId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        CustomerVisit visit =
                mock(CustomerVisit.class);

        Department department =
                Department.builder()
                        .departmentId(departmentId)
                        .name("Phong Kham")
                        .departmentType(DepartmentType.EXAMINATION)
                        .status(DepartmentStatus.AVAILABLE)
                        .headDoctor(null)
                        .build();

        QueueTicket ticket =
                QueueTicket.builder()
                        .ticketId(ticketId)
                        .visit(visit)
                        .department(department)
                        .status(QueueStatus.WAITING)
                        .workDate(LocalDate.now())
                        .queueNumber(1)
                        .build();

        ReflectionTestUtils.invokeMethod(
                queueTicketService,
                "notifyDoctors",
                ticket
        );

        verifyNoInteractions(notificationService);
    }


    @Test
    void notifyDoctors_ShouldNotCreateNotification_WhenHeadDoctorHasNoProfile() {

        UUID ticketId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        StaffInfo headDoctor =
                mock(StaffInfo.class);

        Department department =
                Department.builder()
                        .departmentId(departmentId)
                        .name("Phong Kham")
                        .departmentType(DepartmentType.EXAMINATION)
                        .status(DepartmentStatus.AVAILABLE)
                        .headDoctor(headDoctor)
                        .build();

        QueueTicket ticket =
                QueueTicket.builder()
                        .ticketId(ticketId)
                        .visit(mock(CustomerVisit.class))
                        .department(department)
                        .status(QueueStatus.WAITING)
                        .workDate(LocalDate.now())
                        .queueNumber(1)
                        .build();

        ReflectionTestUtils.invokeMethod(queueTicketService, "notifyDoctors", ticket);
        verifyNoInteractions(notificationService);
    }


// =========================================================
// GET ALL IN-PROGRESS
// =========================================================

    @Test
    void getAllInprogress_ShouldReturnEmptyPage() {

        var pageable =
                PageRequest.of(0, 10);

        when(
                repo.findAllByStatus(
                        QueueStatus.IN_PROGRESS,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        var result =
                queueTicketService.getAllInprogress(
                        pageable
                );

        assertNotNull(result);

        verify(repo)
                .findAllByStatus(
                        QueueStatus.IN_PROGRESS,
                        pageable
                );
    }


    @Test
    void getAllInprogress_ShouldLoadRecordIdAndWaitingCount() {

        UUID ticketId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        var pageable =
                PageRequest.of(0, 10);

        Department department =
                Department.builder()
                        .departmentId(departmentId)
                        .build();

        QueueTicket q =
                QueueTicket.builder()
                        .ticketId(ticketId)
                        .department(department)
                        .status(QueueStatus.IN_PROGRESS)
                        .workDate(LocalDate.now())
                        .queueNumber(1)
                        .build();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .build();

        when(
                repo.findAllByStatus(
                        QueueStatus.IN_PROGRESS,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of(q))
        );

        /*
         * getRecordId(q)
         */
        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        /*
         * getWaitingCount(q)
         */
        when(
                repo.countWaitingByDepartment(departmentId)
        ).thenReturn(4L);

        var result =
                queueTicketService.getAllInprogress(
                        pageable
                );

        assertNotNull(result);

        verify(recordRepo)
                .findByQueueTicket_TicketId(ticketId);

        verify(repo)
                .countWaitingByDepartment(departmentId);
    }


// =========================================================
// GET IN-PROGRESS - RECORD NOT FOUND
//
// Đây cover:
// getMedicalRecordByQueueTicket()
// -> repository trả empty
// -> return null
// =========================================================

    @Test
    void getInprogressByDepartment_ShouldReturnTicketWithoutRecord_WhenRecordMissing() {

        UUID departmentId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        QueueTicket q =
                mock(QueueTicket.class, RETURNS_DEEP_STUBS);

        when(q.getTicketId())
                .thenReturn(ticketId);

        when(q.getStatus())
                .thenReturn(QueueStatus.IN_PROGRESS);

        when(
                repo.findTopByDepartment_DepartmentIdAndStatusOrderByCreatedAtAsc(
                        departmentId,
                        QueueStatus.IN_PROGRESS
                )
        ).thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.empty());

        var result =
                queueTicketService
                        .getInprogressByDepartment(
                                departmentId
                        );

        assertNotNull(result);

        verify(recordRepo)
                .findByQueueTicket_TicketId(ticketId);
    }


// =========================================================
// GET IN-PROGRESS - RECORD EXISTS
//
// Đây cover success của getMedicalRecordByQueueTicket()
// =========================================================

    @Test
    void getInprogressByDepartment_ShouldReturnMedicalRecord_WhenRecordExists() {

        UUID departmentId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        QueueTicket q =
                mock(QueueTicket.class, RETURNS_DEEP_STUBS);

        when(q.getTicketId())
                .thenReturn(ticketId);

        when(q.getStatus())
                .thenReturn(QueueStatus.IN_PROGRESS);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .build();

        when(
                repo.findTopByDepartment_DepartmentIdAndStatusOrderByCreatedAtAsc(
                        departmentId,
                        QueueStatus.IN_PROGRESS
                )
        ).thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        var result =
                queueTicketService
                        .getInprogressByDepartment(
                                departmentId
                        );

        assertNotNull(result);

        verify(recordRepo)
                .findByQueueTicket_TicketId(ticketId);
    }


// =========================================================
// PRIVATE getMedicalRecord(UUID)
//
// QUAN TRỌNG:
// Method này hiện không được bất kỳ public method nào sử dụng.
// Chỉ test reflection nếu mục tiêu của bạn là JaCoCo coverage.
// =========================================================

    @Test
    void getMedicalRecord_ShouldReturnNull_WhenVisitIdIsNull() {

        MedicalRecordResponse result =
                ReflectionTestUtils.invokeMethod(
                        queueTicketService,
                        "getMedicalRecord",
                        (UUID) null
                );

        assertNull(result);

        verifyNoInteractions(recordRepo);
    }


    @Test
    void getMedicalRecord_ShouldReturnNull_WhenRecordDoesNotExist() {

        UUID visitId = UUID.randomUUID();

        when(
                recordRepo
                        .findFirstByVisit_VisitIdOrderByCreatedAtDesc(
                                visitId
                        )
        ).thenReturn(Optional.empty());

        MedicalRecordResponse result =
                ReflectionTestUtils.invokeMethod(
                        queueTicketService,
                        "getMedicalRecord",
                        visitId
                );

        assertNull(result);

        verify(recordRepo)
                .findFirstByVisit_VisitIdOrderByCreatedAtDesc(
                        visitId
                );
    }


    @Test
    void getMedicalRecord_ShouldReturnResponse_WhenRecordExists() {

        UUID visitId = UUID.randomUUID();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(UUID.randomUUID())
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .build();

        when(
                recordRepo
                        .findFirstByVisit_VisitIdOrderByCreatedAtDesc(
                                visitId
                        )
        ).thenReturn(Optional.of(record));

        MedicalRecordResponse result =
                ReflectionTestUtils.invokeMethod(
                        queueTicketService,
                        "getMedicalRecord",
                        visitId
                );

        assertNotNull(result);
    }
    @Test
    void getMedicalRecordByQueueTicket_ShouldReturnNull_WhenTicketIdNull() {

        MedicalRecordResponse result =
                ReflectionTestUtils.invokeMethod(
                        queueTicketService,
                        "getMedicalRecordByQueueTicket",
                        (UUID) null
                );

        assertNull(result);
    }
    // =========================================================
// QUEUE TICKET SERVICE - COVERAGE BOOST PART 2
// completeAndReturnRecord / facade / resolve / update fields
// =========================================================


// =========================================================
// COMPLETE AND RETURN RECORD - INVALID QUEUE STATUS
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldReject_WhenQueueNotInProgress() {

        UUID ticketId = UUID.randomUUID();

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.WAITING)
                .build();

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.completeAndReturnRecord(ticketId)
        );
    }


// =========================================================
// COMPLETE AND RETURN RECORD - NO VISIT
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldReject_WhenVisitMissing() {

        UUID ticketId = UUID.randomUUID();

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .build();

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.completeAndReturnRecord(ticketId)
        );
    }


// =========================================================
// COMPLETE AND RETURN RECORD - RECORD NOT FOUND
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldThrow_WhenRecordMissing() {

        UUID ticketId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .build();

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> queueTicketService.completeAndReturnRecord(ticketId)
        );
    }


// =========================================================
// COMPLETE AND RETURN RECORD - USER NOT DOCTOR/OWNER
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldReject_WhenCurrentUserIsNotRecordDoctor() {

        UUID ticketId = UUID.randomUUID();
        UUID currentStaffId = UUID.randomUUID();
        UUID ownerDoctorId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        StaffInfo doctor = mock(StaffInfo.class);
        when(doctor.getStaffId())
                .thenReturn(ownerDoctorId);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .build();

        setStaffPrincipal(
                currentStaffId,
                "ROLE_DOCTOR"
        );

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.completeAndReturnRecord(ticketId)
        );
    }


// =========================================================
// COMPLETE AND RETURN RECORD - ADMIN CAN COMPLETE
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldAllowAdmin() {

        UUID ticketId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId()).thenReturn(visitId);

        Department dept = Department.builder()
                .departmentId(deptId)
                .status(DepartmentStatus.AVAILABLE)
                .build();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .status(MedicalRecordStatus.IN_PROGRESS)
                .diagnosis("Cam cum")
                .build();

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .department(dept)
                .build();

        var auth =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(auth);

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        when(invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()))
                .thenReturn(List.of());

        when(recordRepo.save(record))
                .thenReturn(record);

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        var result =
                queueTicketService.completeAndReturnRecord(ticketId);

        assertNotNull(result);

        assertEquals(
                MedicalRecordStatus.COMPLETED,
                record.getStatus()
        );

        assertEquals(
                QueueStatus.DONE,
                q.getStatus()
        );

        verify(patientJourneyService)
                .activateNext(visitId);
    }


// =========================================================
// COMPLETE - VERSION CONFLICT
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldThrowConflict_WhenVersionMismatch() {

        UUID ticketId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        StaffInfo doctor = mock(StaffInfo.class);
        when(doctor.getStaffId()).thenReturn(staffId);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .version(5L)
                .build();

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .build();

        MedicalRecordUpdateRequest req =
                mock(MedicalRecordUpdateRequest.class);

        when(req.version())
                .thenReturn(4L);

        setStaffPrincipal(staffId, "ROLE_DOCTOR");

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        assertThrows(
                ConflictException.class,
                () -> queueTicketService.completeAndReturnRecord(
                        ticketId,
                        req
                )
        );
    }


// =========================================================
// COMPLETE - UNPAID INVOICE
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldReject_WhenPendingInvoiceExists() {

        UUID ticketId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        StaffInfo doctor = mock(StaffInfo.class);
        when(doctor.getStaffId()).thenReturn(staffId);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .diagnosis("Viem hong")
                .build();

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .build();

        Invoice invoice = mock(Invoice.class);
        when(invoice.getStatus())
                .thenReturn(InvoiceStatus.PENDING);

        setStaffPrincipal(staffId, "ROLE_DOCTOR");

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        when(invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()))
                .thenReturn(List.of(invoice));

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.completeAndReturnRecord(ticketId)
        );
    }


// =========================================================
// COMPLETE - NO DIAGNOSIS / CONCLUSION / ICD
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldReject_WhenNoDiagnosisConclusionOrIcd() {

        UUID ticketId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        StaffInfo doctor = mock(StaffInfo.class);
        when(doctor.getStaffId()).thenReturn(staffId);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .build();

        setStaffPrincipal(staffId, "ROLE_DOCTOR");

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        when(invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()))
                .thenReturn(List.of());

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.completeAndReturnRecord(ticketId)
        );
    }


// =========================================================
// COMPLETE - SUCCESS WITH DIAGNOSIS
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldCompleteRecordAndQueue_WhenNoTests() {

        UUID ticketId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId()).thenReturn(visitId);

        StaffInfo doctor = mock(StaffInfo.class);
        when(doctor.getStaffId()).thenReturn(staffId);

        Department dept = Department.builder()
                .departmentId(deptId)
                .status(DepartmentStatus.AVAILABLE)
                .build();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .diagnosis("Viem amidan")
                .build();

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .department(dept)
                .build();

        StaffInfo confirmer = mock(StaffInfo.class);

        setStaffPrincipal(staffId, "ROLE_DOCTOR");

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        when(invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()))
                .thenReturn(List.of());

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(confirmer));

        when(recordRepo.save(record))
                .thenReturn(record);

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        var result =
                queueTicketService.completeAndReturnRecord(ticketId);

        assertNotNull(result);

        assertEquals(
                MedicalRecordStatus.COMPLETED,
                record.getStatus()
        );

        assertNotNull(record.getCompletedAt());

        assertSame(
                confirmer,
                record.getDoctorConfirmedBy()
        );

        assertNotNull(
                record.getDoctorConfirmedAt()
        );

        assertEquals(
                QueueStatus.DONE,
                q.getStatus()
        );

        assertNotNull(q.getCompletedAt());

        verify(patientJourneyService)
                .activateNext(visitId);
    }


// =========================================================
// COMPLETE - HAS TEST REQUEST BUT NO DOCTOR
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldRejectTestCreation_WhenDoctorMissing() {

        UUID ticketId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .build();

        TestRequestInExaminationRequest testReq =
                mock(TestRequestInExaminationRequest.class);

        MedicalRecordUpdateRequest req =
                mock(MedicalRecordUpdateRequest.class);

        when(req.testRequests())
                .thenReturn(List.of(testReq));

        var auth =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(auth);

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));
        when(recordRepo.save(record))
                .thenReturn(record);
        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.completeAndReturnRecord(
                        ticketId,
                        req
                )
        );
    }


// =========================================================
// COMPLETE - TEST SERVICE NOT FOUND
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldThrow_WhenRequestedTestServiceMissing() {

        UUID ticketId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        StaffInfo doctor = mock(StaffInfo.class);
        when(doctor.getStaffId()).thenReturn(staffId);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .build();

        TestRequestInExaminationRequest testReq =
                mock(TestRequestInExaminationRequest.class);

        when(testReq.serviceId())
                .thenReturn(serviceId);

        MedicalRecordUpdateRequest req =
                mock(MedicalRecordUpdateRequest.class);

        when(req.testRequests())
                .thenReturn(List.of(testReq));

        setStaffPrincipal(staffId, "ROLE_DOCTOR");

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));
        when(recordRepo.save(record))
                .thenReturn(record);
        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> queueTicketService.completeAndReturnRecord(
                        ticketId,
                        req
                )
        );
    }


// =========================================================
// COMPLETE - TEST REQUEST -> CREATE INVOICE
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldCreateInvoiceAndWaitForTests() {

        UUID ticketId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        Profile customer = mock(Profile.class);
        when(customer.getProfileId())
                .thenReturn(profileId);

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId())
                .thenReturn(visitId);
        when(visit.getCustomer())
                .thenReturn(customer);

        StaffInfo doctor = mock(StaffInfo.class);
        when(doctor.getStaffId())
                .thenReturn(staffId);

        Department dept = Department.builder()
                .departmentId(deptId)
                .status(DepartmentStatus.AVAILABLE)
                .build();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .department(dept)
                .build();

        TestRequestInExaminationRequest testReq =
                mock(TestRequestInExaminationRequest.class);

        when(testReq.serviceId())
                .thenReturn(serviceId);

        when(testReq.notes())
                .thenReturn("Lam xet nghiem");

        MedicalService service =
                mock(MedicalService.class);

        when(service.getServiceId())
                .thenReturn(serviceId);

        when(service.getName())
                .thenReturn("Xet nghiem mau");

        when(service.getServiceCode())
                .thenReturn("XN01");

        when(service.getPrice())
                .thenReturn(new BigDecimal("150000"));

        MedicalRecordUpdateRequest req =
                mock(MedicalRecordUpdateRequest.class);

        when(req.testRequests())
                .thenReturn(List.of(testReq));

        setStaffPrincipal(staffId, "ROLE_DOCTOR");

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        when(recordRepo.save(record))
                .thenReturn(record);
        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        var result =
                queueTicketService.completeAndReturnRecord(
                        ticketId,
                        req
                );

        assertNotNull(result);

        assertEquals(
                QueueStatus.WAITING_FOR_TEST,
                q.getStatus()
        );

        assertNull(q.getCalledAt());

        verify(invoiceService)
                .create(argThat(invoiceRequest ->
                        profileId.equals(invoiceRequest.customerId())
                                && visitId.equals(invoiceRequest.visitId())
                                && record.getRecordId().equals(invoiceRequest.medicalRecordId())
                                && staffId.equals(invoiceRequest.issuedById())
                                && invoiceRequest.items() != null
                                && invoiceRequest.items().size() == 1
                ));
        verify(patientJourneyService, never())
                .activateNext(any(UUID.class));
    }


// =========================================================
// LOAD EXAMINATION - ID IS MEDICAL RECORD
// =========================================================

    @Test
    void loadExamination_ShouldLoadDirectlyByRecordId() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        var result =
                queueTicketService.loadExamination(recordId);

        assertNotNull(result);

        verify(repo, never())
                .findById(recordId);
    }


// =========================================================
// LOAD EXAMINATION - ID IS QUEUE TICKET
// =========================================================

    @Test
    void loadExamination_ShouldResolveRecordFromQueueTicket() {

        UUID ticketId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        QueueTicket ticket =
                QueueTicket.builder()
                        .ticketId(ticketId)
                        .build();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .build();

        when(recordRepo.findById(ticketId))
                .thenReturn(Optional.empty());

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        var result =
                queueTicketService.loadExamination(ticketId);

        assertNotNull(result);
    }


// =========================================================
// LOAD EXAMINATION - QUEUE WITHOUT RECORD
// =========================================================

    @Test
    void loadExamination_ShouldThrow_WhenQueueHasNoRecord() {

        UUID ticketId = UUID.randomUUID();

        QueueTicket ticket =
                QueueTicket.builder()
                        .ticketId(ticketId)
                        .build();

        when(recordRepo.findById(ticketId))
                .thenReturn(Optional.empty());

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> queueTicketService.loadExamination(ticketId)
        );
    }


// =========================================================
// LOAD EXAMINATION - NOTHING FOUND
// =========================================================

    @Test
    void loadExamination_ShouldThrow_WhenNeitherRecordNorQueueExists() {

        UUID id = UUID.randomUUID();

        when(recordRepo.findById(id))
                .thenReturn(Optional.empty());

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> queueTicketService.loadExamination(id)
        );
    }


// =========================================================
// SAVE EXAMINATION DRAFT
// =========================================================

    @Test
    void saveExaminationDraft_ShouldDelegateToMedicalRecordService() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .build();

        MedicalRecordUpdateRequest req =
                mock(MedicalRecordUpdateRequest.class);

        MedicalRecordResponse response =
                mock(MedicalRecordResponse.class);

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordService.saveDraft(recordId, req))
                .thenReturn(response);

        var result =
                queueTicketService.saveExaminationDraft(
                        recordId,
                        req
                );

        assertSame(response, result);
    }


// =========================================================
// COMPLETE EXAMINATION - ID ALREADY QUEUE
// =========================================================

    @Test
    void completeExamination_ShouldUseQueueIdDirectly() {

        UUID ticketId = UUID.randomUUID();

        QueueTicket q =
                QueueTicket.builder()
                        .ticketId(ticketId)
                        .status(QueueStatus.WAITING)
                        .build();

        when(repo.existsById(ticketId))
                .thenReturn(true);

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.completeExamination(
                        ticketId,
                        null
                )
        );

        verify(recordRepo, never())
                .findById(ticketId);
    }


// =========================================================
// COMPLETE EXAMINATION - RESOLVE RECORD -> QUEUE
// =========================================================

    @Test
    void completeExamination_ShouldResolveTicketFromMedicalRecord() {

        UUID recordId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        QueueTicket ticket =
                QueueTicket.builder()
                        .ticketId(ticketId)
                        .status(QueueStatus.WAITING)
                        .build();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .queueTicket(ticket)
                        .build();

        when(repo.existsById(recordId))
                .thenReturn(false);

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        assertThrows(
                BadRequestException.class,
                () -> queueTicketService.completeExamination(
                        recordId,
                        null
                )
        );
    }


// =========================================================
// COMPLETE EXAMINATION - RECORD HAS NO TICKET
// =========================================================

    @Test
    void completeExamination_ShouldThrow_WhenRecordHasNoQueueTicket() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .build();

        when(repo.existsById(recordId))
                .thenReturn(false);

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        assertThrows(
                ResourceNotFoundException.class,
                () -> queueTicketService.completeExamination(
                        recordId,
                        null
                )
        );
    }


// =========================================================
// UPDATE MEDICAL RECORD FIELDS
// test thông qua completeAndReturnRecord(req)
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldUpdateBasicMedicalRecordFields() {

        UUID ticketId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        CustomerVisit visit =
                mock(CustomerVisit.class);

        StaffInfo doctor =
                mock(StaffInfo.class);

        when(doctor.getStaffId())
                .thenReturn(staffId);

        Department dept = Department.builder()
                .departmentId(deptId)
                .status(DepartmentStatus.AVAILABLE)
                .build();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .version(0L)
                .build();

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .department(dept)
                .build();

        MedicalRecordUpdateRequest req =
                mock(MedicalRecordUpdateRequest.class);

        when(req.version()).thenReturn(0L);
        when(req.chiefComplaint()).thenReturn("Dau dau");
        when(req.clinicalFindings()).thenReturn("Sot");
        when(req.diagnosis()).thenReturn("Viem hong");
        when(req.prescriptionNote()).thenReturn("Uong thuoc");
        when(req.conclusion()).thenReturn("On dinh");
        when(req.patientInstruction()).thenReturn("Nghi ngoi");

        setStaffPrincipal(staffId, "ROLE_DOCTOR");

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        when(recordRepo.save(record))
                .thenReturn(record);

        when(invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()))
                .thenReturn(List.of());

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.completeAndReturnRecord(
                ticketId,
                req
        );

        assertEquals("Dau dau", record.getChiefComplaint());
        assertEquals("Sot", record.getClinicalFindings());
        assertEquals("Viem hong", record.getDiagnosis());
        assertEquals("Uong thuoc", record.getPrescriptionNote());
        assertEquals("On dinh", record.getConclusion());
        assertEquals("Nghi ngoi", record.getPatientInstruction());
    }


// =========================================================
// UPDATE MEDICAL RECORD - PRESCRIPTION
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldReplacePrescriptionItems() {

        UUID ticketId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        StaffInfo doctor = mock(StaffInfo.class);
        when(doctor.getStaffId()).thenReturn(staffId);

        Department dept = Department.builder()
                .departmentId(deptId)
                .status(DepartmentStatus.AVAILABLE)
                .build();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .version(0L)
                .build();

        PrescriptionItemCreateRequest item =
                mock(PrescriptionItemCreateRequest.class);

        when(item.medicineName())
                .thenReturn("Paracetamol");

        when(item.quantity())
                .thenReturn(10);

        MedicalRecordUpdateRequest req =
                mock(MedicalRecordUpdateRequest.class);

        when(req.version()).thenReturn(0L);
        when(req.diagnosis()).thenReturn("Cam");
        when(req.prescriptionItems())
                .thenReturn(List.of(item));

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .department(dept)
                .build();

        setStaffPrincipal(staffId, "ROLE_DOCTOR");

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        when(recordRepo.save(record))
                .thenReturn(record);

        when(invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()))
                .thenReturn(List.of());

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.completeAndReturnRecord(
                ticketId,
                req
        );

        assertEquals(
                1,
                record.getPrescriptionItems().size()
        );

        var savedItem =
                record.getPrescriptionItems()
                        .iterator()
                        .next();

        assertEquals(
                "Paracetamol",
                savedItem.getMedicineName()
        );

        assertSame(
                record,
                savedItem.getMedicalRecord()
        );
    }


// =========================================================
// UPDATE MEDICAL RECORD - ICD WITH NAME PROVIDED
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldAddIcd_WhenCodeNameProvided() {

        UUID ticketId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        StaffInfo doctor = mock(StaffInfo.class);
        when(doctor.getStaffId()).thenReturn(staffId);

        Department dept = Department.builder()
                .departmentId(deptId)
                .status(DepartmentStatus.AVAILABLE)
                .build();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .version(0L)
                .build();

        ICD10SelectionCreateRequest icd =
                mock(ICD10SelectionCreateRequest.class);

        when(icd.code()).thenReturn("J02.9");
        when(icd.codeName()).thenReturn("Viem hong");

        MedicalRecordUpdateRequest req =
                mock(MedicalRecordUpdateRequest.class);

        when(req.version()).thenReturn(0L);
        when(req.icdSelections())
                .thenReturn(List.of(icd));

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .department(dept)
                .build();

        setStaffPrincipal(staffId, "ROLE_DOCTOR");

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        when(recordRepo.save(record))
                .thenReturn(record);

        when(invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()))
                .thenReturn(List.of());

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.completeAndReturnRecord(
                ticketId,
                req
        );

        assertEquals(
                1,
                record.getIcdSelections().size()
        );

        var saved =
                record.getIcdSelections()
                        .iterator()
                        .next();

        assertEquals("J02.9", saved.getCode());
        assertEquals("Viem hong", saved.getCodeName());

        verifyNoInteractions(icd10Repo);
    }


// =========================================================
// ICD LOOKUP FALLBACK
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldLookupIcdName_WhenCodeNameMissing() {

        UUID ticketId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        StaffInfo doctor = mock(StaffInfo.class);
        when(doctor.getStaffId()).thenReturn(staffId);

        Department dept = Department.builder()
                .departmentId(deptId)
                .status(DepartmentStatus.AVAILABLE)
                .build();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .version(0L)
                .build();

        ICD10SelectionCreateRequest icd =
                mock(ICD10SelectionCreateRequest.class);

        when(icd.code())
                .thenReturn("J02.9");

        Icd10Code code =
                mock(Icd10Code.class);

        when(code.getName())
                .thenReturn("Viem hong cap");

        when(icd10Repo.findById("J02.9"))
                .thenReturn(Optional.of(code));

        MedicalRecordUpdateRequest req =
                mock(MedicalRecordUpdateRequest.class);

        when(req.version()).thenReturn(0L);
        when(req.icdSelections())
                .thenReturn(List.of(icd));

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .department(dept)
                .build();

        setStaffPrincipal(staffId, "ROLE_DOCTOR");

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        when(recordRepo.save(record))
                .thenReturn(record);

        when(invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()))
                .thenReturn(List.of());

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.completeAndReturnRecord(
                ticketId,
                req
        );

        assertEquals(
                "Viem hong cap",
                record.getIcdSelections()
                        .iterator()
                        .next()
                        .getCodeName()
        );
    }


// =========================================================
// VITAL SIGNS - CREATE NEW
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldCreateVitalSigns_WhenMissing() {

        UUID ticketId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        StaffInfo doctor = mock(StaffInfo.class);
        when(doctor.getStaffId()).thenReturn(staffId);

        Department dept = Department.builder()
                .departmentId(deptId)
                .status(DepartmentStatus.AVAILABLE)
                .build();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .version(0L)
                .build();

        MedicalRecordUpdateRequest req =
                mock(MedicalRecordUpdateRequest.class);

        when(req.version()).thenReturn(0L);
        when(req.diagnosis()).thenReturn("Cam");
        when(req.bloodPressure())
                .thenReturn("120/80");

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .department(dept)
                .build();

        setStaffPrincipal(staffId, "ROLE_DOCTOR");

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        when(recordRepo.save(record))
                .thenReturn(record);

        when(invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()))
                .thenReturn(List.of());

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.completeAndReturnRecord(
                ticketId,
                req
        );

        assertNotNull(
                record.getVitalSigns()
        );

        assertEquals(
                "120/80",
                record.getVitalSigns()
                        .getBloodPressure()
        );
    }


// =========================================================
// VITAL SIGNS - UPDATE EXISTING
// =========================================================

    @Test
    void completeAndReturnRecord_ShouldUpdateExistingVitalSigns() {

        UUID ticketId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        StaffInfo doctor = mock(StaffInfo.class);
        when(doctor.getStaffId()).thenReturn(staffId);

        Department dept = Department.builder()
                .departmentId(deptId)
                .status(DepartmentStatus.AVAILABLE)
                .build();

        VitalSigns vitalSigns = VitalSigns.builder()
                .bloodPressure("110/70")
                .heartRate(70)
                .build();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .version(0L)
                .vitalSigns(vitalSigns)
                .build();

        MedicalRecordUpdateRequest req =
                mock(MedicalRecordUpdateRequest.class);

        when(req.version()).thenReturn(0L);
        when(req.diagnosis()).thenReturn("Cam");
        when(req.bloodPressure())
                .thenReturn("130/90");
        when(req.heartRate())
                .thenReturn(90);

        QueueTicket q = QueueTicket.builder()
                .ticketId(ticketId)
                .status(QueueStatus.IN_PROGRESS)
                .visit(visit)
                .department(dept)
                .build();

        setStaffPrincipal(staffId, "ROLE_DOCTOR");

        when(repo.findById(ticketId))
                .thenReturn(Optional.of(q));

        when(recordRepo.findByQueueTicket_TicketId(ticketId))
                .thenReturn(Optional.of(record));

        when(recordRepo.save(record))
                .thenReturn(record);

        when(invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()))
                .thenReturn(List.of());

        when(repo.save(q))
                .thenReturn(q);

        when(departmentRepo.findById(deptId))
                .thenReturn(Optional.empty());

        queueTicketService.completeAndReturnRecord(
                ticketId,
                req
        );

        assertEquals(
                "130/90",
                vitalSigns.getBloodPressure()
        );

        assertEquals(
                90,
                vitalSigns.getHeartRate()
        );
    }
}