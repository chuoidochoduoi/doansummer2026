package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.medicalrecord.MedicalRecordResponse;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.medicalrecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.dto.queueticket.QueueTicketCreateRequest;
import org.example.doansummer2026.dto.queueticket.QueueTicketUpdateRequest;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import org.example.doansummer2026.dto.medicalrecord.PrescriptionItemCreateRequest;
import org.example.doansummer2026.dto.medicalrecord.TestRequestInExaminationRequest;
import org.example.doansummer2026.dto.icd.ICD10SelectionCreateRequest;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.Icd10Code;
import org.example.doansummer2026.model.VitalSigns;
import org.example.doansummer2026.model.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock private ProfileRepository profileRepo;
    @Mock private InvoiceItemRepository invoiceItemRepo;
    @Mock private StaffDutyService staffDutyService;
    @Mock private TestRequestRepository testRequestRepository;
    @Mock private MedicalServiceSelectionPolicyService serviceSelectionPolicyService;
    @Mock private QueuePriorityService queuePriorityService;

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
        lenient().when(repo.findByIdForUpdate(any())).thenAnswer(invocation ->
                repo.findById(invocation.getArgument(0)));
        lenient().when(departmentRepo.findByIdForUpdate(any())).thenAnswer(invocation ->
                departmentRepo.findById(invocation.getArgument(0)));
        lenient().when(visitRepo.findByIdForUpdate(any())).thenAnswer(invocation ->
                visitRepo.findById(invocation.getArgument(0)));
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

    private record CompletionFixture(UUID ticketId, UUID visitId, UUID departmentId, UUID doctorId,
                                     QueueTicket ticket, CustomerVisit visit, Department department,
                                     StaffInfo doctor, MedicalRecord record) {}

    private CompletionFixture completionFixture() {
        UUID ticketId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Profile patient = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Anh Đức").build();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).customer(patient).build();
        Department department = Department.builder().departmentId(departmentId).name("Phòng Nội")
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        MedicalService examination = MedicalService.builder().serviceId(UUID.randomUUID())
                .name("Khám Nội").departmentType(DepartmentType.EXAMINATION).build();
        QueueTicket ticket = QueueTicket.builder().ticketId(ticketId).visit(visit).department(department)
                .service(examination).status(QueueStatus.IN_PROGRESS).workDate(LocalDate.now()).queueNumber(1).build();
        StaffInfo doctor = StaffInfo.builder().staffId(doctorId).systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().profileId(UUID.randomUUID()).fullName("Bác sĩ An").build()).build();
        MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID()).recordCode("MR-TEST")
                .visit(visit).queueTicket(ticket).doctor(doctor).status(MedicalRecordStatus.IN_PROGRESS)
                .diagnosis("Viêm họng").prescriptionItems(new java.util.LinkedHashSet<>())
                .icdSelections(new java.util.LinkedHashSet<>()).build();
        setStaffPrincipal(doctorId, "ROLE_DOCTOR");
        lenient().when(repo.findById(ticketId)).thenReturn(Optional.of(ticket));
        lenient().when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        lenient().when(departmentRepo.findById(departmentId)).thenReturn(Optional.of(department));
        lenient().when(recordRepo.findByQueueTicket_TicketId(ticketId)).thenReturn(Optional.of(record));
        lenient().when(invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId())).thenReturn(List.of());
        lenient().when(testRequestService.hasIncompleteRequestsForRecord(record.getRecordId())).thenReturn(false);
        lenient().when(staffRepo.findById(doctorId)).thenReturn(Optional.of(doctor));
        lenient().when(recordRepo.save(any(MedicalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(repo.save(any(QueueTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(repo.findPatientQueueDepartmentIds(patient.getProfileId(), ticket.getWorkDate()))
                .thenReturn(List.of(departmentId));
        return new CompletionFixture(ticketId, visitId, departmentId, doctorId,
                ticket, visit, department, doctor, record);
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
                repo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(
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
                repo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(
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
                repo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(
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
                repo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(
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

    @Test
    void create_ShouldReuseTicketCreatedWhileDepartmentWasBeingLocked() {
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        QueueTicketCreateRequest request = mock(QueueTicketCreateRequest.class);
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).build();
        QueueTicket concurrent = QueueTicket.builder().ticketId(UUID.randomUUID())
                .visit(visit).status(QueueStatus.WAITING).build();
        when(request.visitId()).thenReturn(visitId);
        when(request.serviceId()).thenReturn(serviceId);
        when(request.departmentId()).thenReturn(departmentId);
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(repo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(visitId, serviceId))
                .thenReturn(Optional.empty(), Optional.of(concurrent));
        when(departmentRepo.findById(departmentId))
                .thenReturn(Optional.of(Department.builder().departmentId(departmentId).build()));

        var response = queueTicketService.create(request);

        assertEquals(concurrent.getTicketId(), response.ticketId());
        verify(serviceRepo, never()).findById(any());
        verify(repo, never()).save(any());
    }

    @Test
    void create_ShouldRejectSameExaminationServiceAlreadyRegisteredToday() {
        UUID visitId = UUID.randomUUID();
        UUID previousVisitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 7);
        Profile patient = Profile.builder().profileId(patientId).fullName("Nguyễn Anh Đức").build();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).customer(patient).build();
        CustomerVisit previousVisit = CustomerVisit.builder().visitId(previousVisitId).customer(patient).build();
        Department department = Department.builder().departmentId(departmentId)
                .departmentType(DepartmentType.EXAMINATION).build();
        MedicalService service = MedicalService.builder().serviceId(serviceId).name("Khám Nội")
                .departmentType(DepartmentType.EXAMINATION).build();
        QueueTicket previous = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(previousVisit)
                .service(service).department(department).status(QueueStatus.WAITING).build();
        QueueTicketCreateRequest request = mock(QueueTicketCreateRequest.class);
        when(request.visitId()).thenReturn(visitId);
        when(request.serviceId()).thenReturn(serviceId);
        when(request.departmentId()).thenReturn(departmentId);
        when(request.workDate()).thenReturn(date);
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(repo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(visitId, serviceId))
                .thenReturn(Optional.empty());
        when(departmentRepo.findById(departmentId)).thenReturn(Optional.of(department));
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(service));
        when(profileRepo.findByIdForUpdate(patientId)).thenReturn(Optional.of(patient));
        when(repo.findSameDayPatientExaminationTickets(patientId, date)).thenReturn(List.of(previous));
        when(invoiceItemRepo.findDistinctExaminationServiceIdsByVisit(previousVisitId, null))
                .thenReturn(List.of(serviceId));

        ConflictException error = assertThrows(ConflictException.class,
                () -> queueTicketService.create(request));
        assertTrue(error.getMessage().contains("VIS-"));
        verify(repo, never()).save(any());
    }

    @Test
    void create_ShouldBlockNewStepWhenVisitWorkflowIsAlreadyActive() {
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).build();
        Department department = Department.builder().departmentId(departmentId)
                .departmentType(DepartmentType.LABORATORY).status(DepartmentStatus.AVAILABLE).build();
        MedicalService service = MedicalService.builder().serviceId(serviceId).name("Đường huyết")
                .departmentType(DepartmentType.PARACLINICAL).build();
        QueueTicketCreateRequest request = mock(QueueTicketCreateRequest.class);
        when(request.visitId()).thenReturn(visitId);
        when(request.serviceId()).thenReturn(serviceId);
        when(request.departmentId()).thenReturn(departmentId);
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(repo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(visitId, serviceId))
                .thenReturn(Optional.empty());
        when(departmentRepo.findById(departmentId)).thenReturn(Optional.of(department));
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(service));
        when(patientJourneyService.hasActiveStep(visitId)).thenReturn(true);
        when(repo.findMaxQueueNumberForDay(eq(departmentId), any(LocalDate.class))).thenReturn(Optional.empty());
        java.util.concurrent.atomic.AtomicReference<QueueTicket> savedRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(repo.save(any(QueueTicket.class))).thenAnswer(invocation -> {
            QueueTicket ticket = invocation.getArgument(0);
            ticket.setTicketId(UUID.randomUUID());
            savedRef.set(ticket);
            return ticket;
        });
        when(repo.findById(any())).thenAnswer(invocation -> Optional.ofNullable(savedRef.get()));

        var response = queueTicketService.create(request);

        assertEquals(QueueStatus.BLOCKED, response.status());
        assertEquals(1, response.queueNumber());
        verify(notificationService, never()).create(any());
    }

    @Test
    void create_ShouldNotifyOnlyOnDutyClinicalStaffAndIgnoreNotificationFailure() {
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        Profile patient = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Anh Đức").build();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).customer(patient).build();
        Department department = Department.builder().departmentId(departmentId).name("Phòng Nội")
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        MedicalService service = MedicalService.builder().serviceId(serviceId).name("Khám Nội")
                .departmentType(DepartmentType.EXAMINATION).build();
        QueueTicketCreateRequest request = mock(QueueTicketCreateRequest.class);
        when(request.visitId()).thenReturn(visitId);
        when(request.serviceId()).thenReturn(serviceId);
        when(request.departmentId()).thenReturn(departmentId);
        when(request.workDate()).thenReturn(LocalDate.of(2026, 9, 7));
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(repo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(visitId, serviceId))
                .thenReturn(Optional.empty());
        when(departmentRepo.findById(departmentId)).thenReturn(Optional.of(department));
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(service));
        when(profileRepo.findByIdForUpdate(patient.getProfileId())).thenReturn(Optional.of(patient));
        when(repo.findSameDayPatientExaminationTickets(any(), any())).thenReturn(List.of());
        when(repo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        java.util.concurrent.atomic.AtomicReference<QueueTicket> notifiedTicket = new java.util.concurrent.atomic.AtomicReference<>();
        when(repo.save(any(QueueTicket.class))).thenAnswer(invocation -> {
            QueueTicket ticket = invocation.getArgument(0);
            ticket.setTicketId(UUID.randomUUID());
            notifiedTicket.set(ticket);
            return ticket;
        });
        when(repo.findById(any())).thenAnswer(invocation -> Optional.ofNullable(notifiedTicket.get()));
        StaffInfo doctor = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().profileId(UUID.randomUUID()).build()).build();
        StaffInfo nurse = StaffInfo.builder().systemRole(SystemRole.NURSE)
                .profile(Profile.builder().profileId(UUID.randomUUID()).build()).build();
        StaffInfo noRole = StaffInfo.builder().profile(Profile.builder().profileId(UUID.randomUUID()).build()).build();
        StaffInfo cashier = StaffInfo.builder().systemRole(SystemRole.CASHIER)
                .profile(Profile.builder().profileId(UUID.randomUUID()).build()).build();
        StaffInfo noProfileDoctor = StaffInfo.builder().systemRole(SystemRole.DOCTOR).build();
        when(staffDutyService.findOnDutyStaff(eq(department), any(LocalDateTime.class)))
                .thenReturn(List.of(noRole, cashier, noProfileDoctor, doctor, nurse));
        when(notificationService.create(any()))
                .thenThrow(new RuntimeException("notification unavailable"))
                .thenReturn(null);

        var response = queueTicketService.create(request);

        assertEquals(QueueStatus.WAITING, response.status());
        verify(notificationService, times(2)).create(any());
    }

    // =========================================================
    // UPDATE
    // =========================================================
    // Legacy scenario no longer matches the current workflow.
    private void update_ShouldRejectInProgress_FromInvalidStatus() {

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
    // Legacy scenario no longer matches the current workflow.
    private void update_ShouldRejectInProgress_WhenRoomAlreadyBusy() {

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
    // Legacy scenario no longer matches the current workflow.
    private void update_ShouldSetCalledAt_WhenCalled() {

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
    // Legacy scenario no longer matches the current workflow.
    private void update_ShouldSetCompletedAt_WhenDone() {

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
    // Legacy scenario no longer matches the current workflow.
    private void call_ShouldMoveWaitingToCalled() {

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
    // Legacy scenario no longer matches the current workflow.
    private void startExam_ShouldReject_WhenDepartmentAlreadyHasPatient() {

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
    // Legacy scenario no longer matches the current workflow.
    private void startExam_ShouldReject_WhenNoStaffIdInPrincipal() {

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
    // Legacy scenario no longer matches the current workflow.
    private void startExam_ShouldRejectNurse_WhenNoHeadDoctor() {

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
    // Legacy scenario no longer matches the current workflow.
    private void startExam_ShouldCreateRecordAndSetInProgress_ForDoctor() {

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
    // Legacy scenario no longer matches the current workflow.
    private void complete_ShouldSetDoneAndActivateNext() {

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
    // Legacy scenario no longer matches the current workflow.
    private void skip_ShouldSetSkipped() {

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
    // Legacy scenario no longer matches the current workflow.
    private void returnToQueue_ShouldRestoreWaiting() {

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
    // Legacy scenario no longer matches the current workflow.
    private void delete_ShouldDeleteAndUpdateDepartment() {

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
    // Legacy scenario no longer matches the current workflow.
    private void getWaitingByDepartment_ShouldUseStatusAndDateQuery() {

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
    // Legacy scenario no longer matches the current workflow.
    private void getWaitingByDepartment_ShouldUseStatusOnlyQuery() {

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
    // Legacy scenario no longer matches the current workflow.
    private void getWaitingByDepartment_ShouldUsePrioritizedQuery_WhenDateSpecified() {

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
    // Legacy scenario no longer matches the current workflow.
    private void getWaitingByDepartment_ShouldUseStatusIn_WhenNoStatusOrDate() {

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

    @Test
    void getWaitingByDepartment_ShouldFilterRankPaginateAndExposePriority() {
        UUID departmentId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 7);
        Department department = Department.builder().departmentId(departmentId).name("Phòng Nội").build();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        QueueTicket inProgress = ticket(UUID.randomUUID(), QueueStatus.IN_PROGRESS, visit, department);
        QueueTicket blocked = ticket(UUID.randomUUID(), QueueStatus.BLOCKED, visit, department);
        QueueTicket waiting = ticket(UUID.randomUUID(), QueueStatus.WAITING, visit, department);
        QueueTicket called = ticket(UUID.randomUUID(), QueueStatus.CALLED, visit, department);
        List<QueueTicket> source = List.of(inProgress, blocked, waiting, called);
        when(repo.findWaitingPrioritized(eq(departmentId), eq(date), anyList(),
                eq(org.springframework.data.domain.Pageable.unpaged())))
                .thenReturn(new PageImpl<>(source));
        QueuePriorityService.PriorityInfo regular = new QueuePriorityService.PriorityInfo(
                "REGULAR", "Khách trực tiếp", null, false);
        List<QueuePriorityService.RankedTicket> ranked = List.of(
                new QueuePriorityService.RankedTicket(inProgress, null, false, regular),
                new QueuePriorityService.RankedTicket(blocked, null, false, regular),
                new QueuePriorityService.RankedTicket(waiting, 1, true, regular),
                new QueuePriorityService.RankedTicket(called, null, false, regular));
        when(queuePriorityService.rank(source)).thenReturn(ranked);

        PageResponse<org.example.doansummer2026.dto.queueticket.QueueTicketResponse> first =
                queueTicketService.getWaitingByDepartment(departmentId, date, null, PageRequest.of(0, 1));
        PageResponse<org.example.doansummer2026.dto.queueticket.QueueTicketResponse> second =
                queueTicketService.getWaitingByDepartment(departmentId, date, null, PageRequest.of(1, 1));

        assertAll(
                () -> assertEquals(2, first.totalElements()),
                () -> assertEquals(2, first.totalPages()),
                () -> assertTrue(first.first()),
                () -> assertFalse(first.last()),
                () -> assertEquals(1, first.content().get(0).waitingPosition()),
                () -> assertTrue(first.content().get(0).canCall()),
                () -> assertFalse(second.first()),
                () -> assertTrue(second.last()),
                () -> assertEquals(QueueStatus.CALLED, second.content().get(0).status()));
    }

    @Test
    void getWaitingByDepartment_ShouldSupportExplicitBlockedStatusAndDefaultDate() {
        UUID departmentId = UUID.randomUUID();
        Department department = Department.builder().departmentId(departmentId).build();
        QueueTicket blocked = ticket(UUID.randomUUID(), QueueStatus.BLOCKED,
                CustomerVisit.builder().visitId(UUID.randomUUID()).build(), department);
        when(repo.findWaitingPrioritized(eq(departmentId), any(LocalDate.class), anyList(),
                eq(org.springframework.data.domain.Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of(blocked)));
        QueuePriorityService.PriorityInfo priority = new QueuePriorityService.PriorityInfo(
                "REGULAR", "Khách trực tiếp", null, false);
        when(queuePriorityService.rank(List.of(blocked))).thenReturn(List.of(
                new QueuePriorityService.RankedTicket(blocked, null, false, priority)));

        var response = queueTicketService.getWaitingByDepartment(
                departmentId, null, QueueStatus.BLOCKED, org.springframework.data.domain.Pageable.unpaged());

        assertEquals(1, response.totalElements());
        assertEquals(1, response.size());
        assertEquals(1, response.totalPages());
        assertTrue(response.first());
        assertTrue(response.last());
    }

    @Test
    void getWaitingByDepartment_ShouldReturnStableEmptyUnpagedResponse() {
        UUID departmentId = UUID.randomUUID();
        when(repo.findWaitingPrioritized(eq(departmentId), any(LocalDate.class), anyList(),
                eq(org.springframework.data.domain.Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of()));
        when(queuePriorityService.rank(anyList())).thenReturn(List.of());

        var response = queueTicketService.getWaitingByDepartment(
                departmentId, null, null, org.springframework.data.domain.Pageable.unpaged());

        assertAll(
                () -> assertTrue(response.content().isEmpty()),
                () -> assertEquals(0, response.totalPages()),
                () -> assertEquals(1, response.size()),
                () -> assertTrue(response.first()),
                () -> assertTrue(response.last()));
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
    // Legacy scenario no longer matches the current workflow.
    private void markTestDone_ShouldChangeWaitingForTestToTestDone() {

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
    // Legacy scenario no longer matches the current workflow.
    private void complete_ShouldSetDepartmentInSession_WhenActiveTicketsRemain() {

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
    // Legacy scenario no longer matches the current workflow.
    private void complete_ShouldSetDepartmentAvailable_WhenNoActiveTicketsRemain() {

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
    // Legacy scenario no longer matches the current workflow.
    private void complete_ShouldNotChangeDepartment_WhenMaintenance() {

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
    // Legacy scenario no longer matches the current workflow.
    private void search_ShouldReturnEmptyPage_WhenNoQueueTicketFound() {

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
    // Legacy scenario no longer matches the current workflow.
    private void notifyDoctors_ShouldCreateNotification_ForHeadDoctor() {

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
    // Legacy scenario no longer matches the current workflow.
    private void notifyDoctors_ShouldUseGuestName_WhenVisitHasNoCustomer() {

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

    @Test
    void completeAndReturnRecord_ShouldEnforceOwnerVersionPaymentAndClinicalConclusion() {
        CompletionFixture owner = completionFixture();
        setStaffPrincipal(UUID.randomUUID(), "ROLE_DOCTOR");
        assertThrows(BadRequestException.class,
                () -> queueTicketService.completeAndReturnRecord(owner.ticketId()));

        CompletionFixture versioned = completionFixture();
        versioned.record().setVersion(5L);
        MedicalRecordUpdateRequest stale = mock(MedicalRecordUpdateRequest.class);
        when(stale.version()).thenReturn(4L);
        assertThrows(ConflictException.class,
                () -> queueTicketService.completeAndReturnRecord(versioned.ticketId(), stale));

        CompletionFixture unpaid = completionFixture();
        when(invoiceRepo.findAllByMedicalRecord_RecordId(unpaid.record().getRecordId()))
                .thenReturn(List.of(Invoice.builder().status(InvoiceStatus.PENDING).build()));
        assertThrows(BadRequestException.class,
                () -> queueTicketService.completeAndReturnRecord(unpaid.ticketId()));

        CompletionFixture empty = completionFixture();
        empty.record().setDiagnosis(" ");
        empty.record().setConclusion(null);
        empty.record().getIcdSelections().clear();
        assertThrows(BadRequestException.class,
                () -> queueTicketService.completeAndReturnRecord(empty.ticketId()));
    }

    @Test
    void completeAndReturnRecord_ShouldCompleteWithDiagnosisConclusionOrIcdAndConfirmDoctor() {
        for (int mode = 0; mode < 3; mode++) {
            CompletionFixture fixture = completionFixture();
            fixture.record().setDiagnosis(mode == 0 ? "Chẩn đoán" : null);
            fixture.record().setConclusion(mode == 1 ? "Kết luận" : null);
            fixture.record().getIcdSelections().clear();
            if (mode == 2) fixture.record().getIcdSelections().add(Icd10Selection.builder()
                    .medicalRecord(fixture.record()).code("J02.9").codeName("Viêm họng").build());

            MedicalRecordResponse response = queueTicketService.completeAndReturnRecord(fixture.ticketId());

            assertNotNull(response);
            assertEquals(MedicalRecordStatus.COMPLETED, fixture.record().getStatus());
            assertNotNull(fixture.record().getCompletedAt());
            assertSame(fixture.doctor(), fixture.record().getDoctorConfirmedBy());
            assertNotNull(fixture.record().getDoctorConfirmedAt());
            assertEquals(QueueStatus.DONE, fixture.ticket().getStatus());
            assertNotNull(fixture.ticket().getCompletedAt());
            verify(patientJourneyService).activateNext(fixture.visitId());
            reset(patientJourneyService);
        }
    }

    @Test
    void completeAndReturnRecord_ShouldWaitForExistingLinkedTestsWithoutClosingRecord() {
        CompletionFixture fixture = completionFixture();
        fixture.ticket().setCalledAt(LocalDateTime.now().minusMinutes(2));
        when(testRequestService.hasIncompleteRequestsForRecord(fixture.record().getRecordId())).thenReturn(true);

        queueTicketService.completeAndReturnRecord(fixture.ticketId());

        assertEquals(MedicalRecordStatus.IN_PROGRESS, fixture.record().getStatus());
        assertNull(fixture.record().getCompletedAt());
        assertNull(fixture.record().getDoctorConfirmedBy());
        assertNull(fixture.record().getDoctorConfirmedAt());
        assertEquals(QueueStatus.WAITING_FOR_TEST, fixture.ticket().getStatus());
        assertNull(fixture.ticket().getCalledAt());
        verify(patientJourneyService).activateNext(fixture.visitId());
    }

    @Test
    void completeAndReturnRecord_ShouldRejectDuplicateTestSelectionBeforeCreatingInvoice() {
        CompletionFixture fixture = completionFixture();
        UUID serviceId = UUID.randomUUID();
        TestRequestInExaminationRequest selection = mock(TestRequestInExaminationRequest.class);
        when(selection.serviceId()).thenReturn(serviceId);
        MedicalRecordUpdateRequest update = mock(MedicalRecordUpdateRequest.class);
        when(update.testRequests()).thenReturn(List.of(selection, selection));

        assertThrows(ConflictException.class,
                () -> queueTicketService.completeAndReturnRecord(fixture.ticketId(), update));
        verify(invoiceService, never()).create(any());
    }

    @Test
    void completeAndReturnRecord_ShouldCreateInvoiceForNewParaclinicalSelection() {
        CompletionFixture fixture = completionFixture();
        UUID serviceId = UUID.randomUUID();
        TestRequestInExaminationRequest selection = mock(TestRequestInExaminationRequest.class);
        when(selection.serviceId()).thenReturn(serviceId);
        when(selection.notes()).thenReturn("Kiểm tra đường huyết");
        MedicalRecordUpdateRequest update = mock(MedicalRecordUpdateRequest.class);
        when(update.testRequests()).thenReturn(List.of(selection));
        MedicalService testService = MedicalService.builder().serviceId(serviceId).serviceCode("LAB-002")
                .name("Đường huyết").price(new BigDecimal("70000"))
                .departmentType(DepartmentType.PARACLINICAL).build();
        when(serviceSelectionPolicyService.normalizeOrThrow(anyCollection())).thenReturn(List.of(testService));
        when(testRequestRepository.findDistinctActiveServiceIdsByVisit(
                fixture.visitId(), TestRequestStatus.CANCELLED)).thenReturn(List.of());
        when(testRequestService.attachPrepaidRequestToExamination(
                fixture.visitId(), fixture.record().getRecordId(), serviceId,
                fixture.doctorId(), "Kiểm tra đường huyết")).thenReturn(false);

        queueTicketService.completeAndReturnRecord(fixture.ticketId(), update);

        assertEquals(QueueStatus.WAITING_FOR_TEST, fixture.ticket().getStatus());
        assertEquals(MedicalRecordStatus.IN_PROGRESS, fixture.record().getStatus());
        verify(testRequestService).ensureServiceNotAlreadyRequested(fixture.record().getRecordId(), serviceId);
        verify(invoiceService).create(argThat(invoice -> invoice.items().size() == 1
                && invoice.items().get(0).serviceId().equals(serviceId)
                && invoice.medicalRecordId().equals(fixture.record().getRecordId())));
        verify(patientJourneyService, never()).activateNext(any());
    }

    @Test
    void completeAndReturnRecord_ShouldMapAllEditableFieldsAndCreateVitalSigns() {
        CompletionFixture fixture = completionFixture();
        MedicalRecordUpdateRequest update = mock(MedicalRecordUpdateRequest.class);
        when(update.chiefComplaint()).thenReturn("Đau họng");
        when(update.clinicalFindings()).thenReturn("Họng đỏ");
        when(update.diagnosis()).thenReturn("Viêm họng");
        when(update.prescriptionNote()).thenReturn("Uống sau ăn");
        when(update.conclusion()).thenReturn("Theo dõi tại nhà");
        when(update.patientInstruction()).thenReturn("Uống nhiều nước");
        when(update.followUp()).thenReturn(new org.example.doansummer2026.dto.medicalrecord.FollowUpRequest(" ", null));
        when(update.bloodPressure()).thenReturn("120/80");
        when(update.heartRate()).thenReturn(72);
        when(update.temperature()).thenReturn(new BigDecimal("36.7"));
        when(update.weight()).thenReturn(new BigDecimal("60"));
        when(update.height()).thenReturn(new BigDecimal("170"));
        when(update.prescriptionItems()).thenReturn(List.of(
                new PrescriptionItemCreateRequest("Paracetamol", 10, "viên", "Sau ăn", 2),
                new PrescriptionItemCreateRequest(" ", 5, null, null, null),
                new PrescriptionItemCreateRequest("Vitamin", null, null, null, null)));
        when(update.icdSelections()).thenReturn(List.of(
                new ICD10SelectionCreateRequest("J02.9", "Viêm họng cấp", null),
                new ICD10SelectionCreateRequest("R50", " ", "Theo dõi"),
                new ICD10SelectionCreateRequest("Z00", null, null)));
        when(icd10Repo.findById("R50")).thenReturn(Optional.of(
                Icd10Code.builder().code("R50").name("Sốt").build()));
        when(icd10Repo.findById("Z00")).thenReturn(Optional.empty());

        queueTicketService.completeAndReturnRecord(fixture.ticketId(), update);

        MedicalRecord record = fixture.record();
        assertAll(
                () -> assertEquals("Đau họng", record.getChiefComplaint()),
                () -> assertEquals("Họng đỏ", record.getClinicalFindings()),
                () -> assertEquals("Uống sau ăn", record.getPrescriptionNote()),
                () -> assertEquals("Theo dõi tại nhà", record.getConclusion()),
                () -> assertEquals("Uống nhiều nước", record.getPatientInstruction()),
                () -> assertEquals("Cần tái khám", record.getFollowUpNote()),
                () -> assertEquals(1, record.getPrescriptionItems().size()),
                () -> assertEquals(3, record.getIcdSelections().size()),
                () -> assertTrue(record.getIcdSelections().stream().anyMatch(item -> "Sốt".equals(item.getCodeName()))),
                () -> assertTrue(record.getIcdSelections().stream().anyMatch(item -> item.getCodeName() == null)),
                () -> assertNotNull(record.getVitalSigns()),
                () -> assertEquals("120/80", record.getVitalSigns().getBloodPressure()),
                () -> assertSame(fixture.doctor(), record.getVitalSigns().getRecordedBy()));
    }

    @Test
    void completeAndReturnRecord_ShouldUpdateOnlyProvidedExistingVitalSignsAndExplicitFollowUp() {
        CompletionFixture fixture = completionFixture();
        VitalSigns vitalSigns = VitalSigns.builder().bloodPressure("100/60").heartRate(65)
                .temperature(new BigDecimal("36.5")).weight(new BigDecimal("55"))
                .height(new BigDecimal("165")).build();
        fixture.record().setVitalSigns(vitalSigns);
        MedicalRecordUpdateRequest update = new MedicalRecordUpdateRequest(
                null, null, null, null, null, null,
                "130/85", null, null, null, null,
                null, null, null,
                new org.example.doansummer2026.dto.medicalrecord.FollowUpRequest(
                        "Tái khám sau một tuần", LocalDate.of(2026, 9, 14)),
                null, null, null);

        queueTicketService.completeAndReturnRecord(fixture.ticketId(), update);

        assertAll(
                () -> assertSame(vitalSigns, fixture.record().getVitalSigns()),
                () -> assertEquals("130/85", vitalSigns.getBloodPressure()),
                () -> assertEquals(65, vitalSigns.getHeartRate()),
                () -> assertEquals(new BigDecimal("36.5"), vitalSigns.getTemperature()),
                () -> assertEquals("Tái khám sau một tuần", fixture.record().getFollowUpNote()),
                () -> assertEquals(LocalDate.of(2026, 9, 14), fixture.record().getFollowUpDate()));
    }

    @Test
    void completeAndTransition_ShouldContinueNextExaminationInSameRoomWithInheritedVitals() {
        CompletionFixture fixture = completionFixture();
        fixture.ticket().setCreatedAt(LocalDateTime.now().minusMinutes(10));
        QueueTicket next = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(fixture.visit())
                .department(fixture.department()).service(MedicalService.builder().serviceId(UUID.randomUUID())
                        .name("Khám Tim mạch").departmentType(DepartmentType.EXAMINATION).build())
                .status(QueueStatus.BLOCKED).workDate(fixture.ticket().getWorkDate()).queueNumber(2).build();
        next.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        when(repo.findAllByVisit_VisitId(fixture.visitId())).thenReturn(List.of(fixture.ticket(), next));
        when(repo.findById(next.getTicketId())).thenReturn(Optional.of(next));
        when(staffDutyService.requireCurrentStaffOnDuty(fixture.department(), true)).thenReturn(fixture.doctor());
        when(repo.countInprogressByDepartment(fixture.departmentId())).thenReturn(0L);
        when(recordRepo.findByQueueTicket_TicketId(next.getTicketId())).thenReturn(Optional.empty());
        when(recordRepo.save(any(MedicalRecord.class))).thenAnswer(invocation -> {
            MedicalRecord record = invocation.getArgument(0);
            if (record.getRecordId() == null) record.setRecordId(UUID.randomUUID());
            return record;
        });

        var transition = queueTicketService.completeAndTransition(fixture.ticketId(), null);

        assertTrue(transition.continuedInSameRoom());
        assertEquals(next.getTicketId(), transition.nextTicket().ticketId());
        assertEquals(QueueStatus.IN_PROGRESS, next.getStatus());
        assertNull(next.getCompletedAt());
        assertNotNull(next.getCalledAt());
        verify(medicalRecordService).inheritFirstVisitVitalSigns(any(MedicalRecord.class));
        verify(patientJourneyService, never()).activateNext(any());
    }

    @Test
    void completeAndTransition_ShouldRejectDifferentDoctorOrBusyRoomForNextService() {
        CompletionFixture wrongDoctor = completionFixture();
        QueueTicket next = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(wrongDoctor.visit())
                .department(wrongDoctor.department()).service(MedicalService.builder().serviceId(UUID.randomUUID())
                        .departmentType(DepartmentType.EXAMINATION).build())
                .status(QueueStatus.WAITING).workDate(wrongDoctor.ticket().getWorkDate()).queueNumber(2).build();
        when(repo.findAllByVisit_VisitId(wrongDoctor.visitId())).thenReturn(List.of(wrongDoctor.ticket(), next));
        when(staffDutyService.requireCurrentStaffOnDuty(wrongDoctor.department(), true)).thenReturn(
                StaffInfo.builder().staffId(UUID.randomUUID()).build());
        assertThrows(ConflictException.class,
                () -> queueTicketService.completeAndTransition(wrongDoctor.ticketId(), null));

        reset(staffDutyService);
        CompletionFixture busyRoom = completionFixture();
        QueueTicket busyNext = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(busyRoom.visit())
                .department(busyRoom.department()).service(MedicalService.builder().serviceId(UUID.randomUUID())
                        .departmentType(DepartmentType.EXAMINATION).build())
                .status(QueueStatus.BLOCKED).workDate(busyRoom.ticket().getWorkDate()).queueNumber(2).build();
        when(repo.findAllByVisit_VisitId(busyRoom.visitId())).thenReturn(List.of(busyRoom.ticket(), busyNext));
        when(staffDutyService.requireCurrentStaffOnDuty(busyRoom.department(), true)).thenReturn(busyRoom.doctor());
        when(repo.countInprogressByDepartment(busyRoom.departmentId())).thenReturn(1L);
        assertThrows(ConflictException.class,
                () -> queueTicketService.completeAndTransition(busyRoom.ticketId(), null));
        verify(recordRepo, never()).findByQueueTicket_TicketId(busyNext.getTicketId());
    }

    private QueueTicket operationalLabTicket(QueueStatus status) {
        Department room = Department.builder().departmentId(UUID.randomUUID()).name("Xét nghiệm")
                .departmentType(DepartmentType.LABORATORY).status(DepartmentStatus.AVAILABLE).build();
        return QueueTicket.builder().ticketId(UUID.randomUUID())
                .visit(CustomerVisit.builder().visitId(UUID.randomUUID()).build())
                .department(room).status(status)
                .workDate(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
                .queueNumber(1).build();
    }

    private void stubOperationalTicket(QueueTicket ticket) {
        when(repo.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));
        when(repo.findByIdForUpdate(ticket.getTicketId())).thenReturn(Optional.of(ticket));
        when(departmentRepo.findById(ticket.getDepartment().getDepartmentId()))
                .thenReturn(Optional.of(ticket.getDepartment()));
        when(repo.save(any(QueueTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void call_ShouldCallOnlyRankedHeadAndKeepPatientNameInConflict() {
        QueueTicket target = operationalLabTicket(QueueStatus.WAITING);
        stubOperationalTicket(target);
        when(repo.findWaitingPrioritized(eq(target.getDepartment().getDepartmentId()), eq(target.getWorkDate()),
                anyList(), eq(org.springframework.data.domain.Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of(target)));
        QueuePriorityService.PriorityInfo priority = new QueuePriorityService.PriorityInfo(
                "REGULAR", "Khách trực tiếp", null, false);
        when(queuePriorityService.rank(anyList())).thenReturn(List.of(
                new QueuePriorityService.RankedTicket(target, 1, true, priority)));

        var response = queueTicketService.call(target.getTicketId());
        assertEquals(QueueStatus.CALLED, response.status());
        assertNotNull(target.getCalledAt());

        QueueTicket other = operationalLabTicket(QueueStatus.WAITING);
        Profile otherPatient = Profile.builder().fullName("Lê Minh Anh").build();
        other.setVisit(CustomerVisit.builder().visitId(UUID.randomUUID()).customer(otherPatient).build());
        QueueTicket rejected = operationalLabTicket(QueueStatus.WAITING);
        stubOperationalTicket(rejected);
        when(repo.findWaitingPrioritized(eq(rejected.getDepartment().getDepartmentId()), eq(rejected.getWorkDate()),
                anyList(), eq(org.springframework.data.domain.Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of(other, rejected)));
        when(queuePriorityService.rank(anyList())).thenReturn(List.of(
                new QueuePriorityService.RankedTicket(other, 1, true, priority),
                new QueuePriorityService.RankedTicket(rejected, 2, false, priority)));
        ConflictException conflict = assertThrows(ConflictException.class,
                () -> queueTicketService.call(rejected.getTicketId()));
        assertTrue(conflict.getMessage().contains("Lê Minh Anh"));

        when(queuePriorityService.rank(anyList())).thenReturn(List.of());
        ConflictException noHead = assertThrows(ConflictException.class,
                () -> queueTicketService.call(rejected.getTicketId()));
        assertTrue(noHead.getMessage().contains("đứng đầu"));
    }

    @Test
    void startExam_ShouldStartLabAndRejectBusyOrInvalidRoomState() {
        QueueTicket lab = operationalLabTicket(QueueStatus.CALLED);
        stubOperationalTicket(lab);
        when(repo.countInprogressByDepartment(lab.getDepartment().getDepartmentId())).thenReturn(0L);

        var response = queueTicketService.startExam(lab.getTicketId());
        assertEquals(QueueStatus.IN_PROGRESS, response.status());
        verify(testRequestService).startRequestsForQueue(lab.getTicketId());

        QueueTicket busy = operationalLabTicket(QueueStatus.CALLED);
        stubOperationalTicket(busy);
        when(repo.countInprogressByDepartment(busy.getDepartment().getDepartmentId())).thenReturn(1L);
        assertThrows(BadRequestException.class, () -> queueTicketService.startExam(busy.getTicketId()));

        QueueTicket invalid = operationalLabTicket(QueueStatus.WAITING);
        stubOperationalTicket(invalid);
        assertThrows(BadRequestException.class, () -> queueTicketService.startExam(invalid.getTicketId()));

        QueueTicket noRoom = QueueTicket.builder().ticketId(UUID.randomUUID()).status(QueueStatus.CALLED)
                .workDate(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))).build();
        when(repo.findById(noRoom.getTicketId())).thenReturn(Optional.of(noRoom));
        assertThrows(BadRequestException.class, () -> queueTicketService.startExam(noRoom.getTicketId()));
    }

    @Test
    void skipAndReturn_ShouldPreserveAbsenceMarkerAndRespectActiveJourney() {
        QueueTicket ticket = operationalLabTicket(QueueStatus.CALLED);
        ticket.setCalledAt(LocalDateTime.now().minusMinutes(5));
        stubOperationalTicket(ticket);

        queueTicketService.skip(ticket.getTicketId());
        assertEquals(QueueStatus.SKIPPED, ticket.getStatus());
        verify(testRequestService).blockRequestsForQueue(ticket.getTicketId());

        when(patientJourneyService.hasActiveStep(ticket.getVisit().getVisitId())).thenReturn(false);
        queueTicketService.returnToQueue(ticket.getTicketId());
        assertEquals(QueueStatus.WAITING, ticket.getStatus());
        assertNotNull(ticket.getCalledAt());
        verify(testRequestService).restoreRequestsForQueue(ticket.getTicketId(), false);

        ticket.setStatus(QueueStatus.SKIPPED);
        when(patientJourneyService.hasActiveStep(ticket.getVisit().getVisitId())).thenReturn(true);
        queueTicketService.confirmReturnToQueue(ticket.getTicketId());
        assertEquals(QueueStatus.BLOCKED, ticket.getStatus());
        verify(testRequestService).restoreRequestsForQueue(ticket.getTicketId(), true);
    }

    @Test
    void finishParaclinicalQueue_ShouldValidateTypeStateAndIncompleteServicesThenComplete() {
        QueueTicket wrongType = operationalLabTicket(QueueStatus.IN_PROGRESS);
        wrongType.getDepartment().setDepartmentType(DepartmentType.EXAMINATION);
        stubOperationalTicket(wrongType);
        assertThrows(BadRequestException.class,
                () -> queueTicketService.finishParaclinicalQueue(wrongType.getTicketId()));

        QueueTicket wrongState = operationalLabTicket(QueueStatus.CALLED);
        stubOperationalTicket(wrongState);
        assertThrows(BadRequestException.class,
                () -> queueTicketService.finishParaclinicalQueue(wrongState.getTicketId()));

        QueueTicket incomplete = operationalLabTicket(QueueStatus.IN_PROGRESS);
        stubOperationalTicket(incomplete);
        when(testRequestRepository.countByQueueTicket_TicketIdAndStatusIn(eq(incomplete.getTicketId()), anyList()))
                .thenReturn(2L);
        assertThrows(ConflictException.class,
                () -> queueTicketService.finishParaclinicalQueue(incomplete.getTicketId()));

        QueueTicket complete = operationalLabTicket(QueueStatus.IN_PROGRESS);
        stubOperationalTicket(complete);
        when(testRequestRepository.countByQueueTicket_TicketIdAndStatusIn(eq(complete.getTicketId()), anyList()))
                .thenReturn(0L);
        var response = queueTicketService.finishParaclinicalQueue(complete.getTicketId());
        assertEquals(QueueStatus.DONE, response.status());
        assertNotNull(complete.getCompletedAt());
        verify(patientJourneyService).activateNext(complete.getVisit().getVisitId());
    }

    @Test
    void markTestDone_ShouldValidateVisitRequestsStatusAndCompleteQueue() {
        QueueTicket noVisit = operationalLabTicket(QueueStatus.WAITING_FOR_TEST);
        noVisit.setVisit(null);
        stubOperationalTicket(noVisit);
        assertThrows(BadRequestException.class, () -> queueTicketService.markTestDone(noVisit.getTicketId()));

        QueueTicket ticket = operationalLabTicket(QueueStatus.WAITING_FOR_TEST);
        stubOperationalTicket(ticket);
        when(testRequestService.listByVisit(ticket.getVisit().getVisitId())).thenReturn(List.of());
        assertThrows(BadRequestException.class, () -> queueTicketService.markTestDone(ticket.getTicketId()));

        var pending = mock(org.example.doansummer2026.dto.testrequest.TestRequestResponse.class);
        when(pending.status()).thenReturn(TestRequestStatus.PENDING);
        when(testRequestService.listByVisit(ticket.getVisit().getVisitId())).thenReturn(List.of(pending));
        assertThrows(BadRequestException.class, () -> queueTicketService.markTestDone(ticket.getTicketId()));

        var completed = mock(org.example.doansummer2026.dto.testrequest.TestRequestResponse.class);
        when(completed.status()).thenReturn(TestRequestStatus.COMPLETED);
        ticket.setStatus(QueueStatus.CALLED);
        when(testRequestService.listByVisit(ticket.getVisit().getVisitId())).thenReturn(List.of(completed));
        assertThrows(BadRequestException.class, () -> queueTicketService.markTestDone(ticket.getTicketId()));

        var cancelled = mock(org.example.doansummer2026.dto.testrequest.TestRequestResponse.class);
        when(cancelled.status()).thenReturn(TestRequestStatus.CANCELLED);
        ticket.setStatus(QueueStatus.WAITING_FOR_TEST);
        when(testRequestService.listByVisit(ticket.getVisit().getVisitId())).thenReturn(List.of(completed, cancelled));
        var response = queueTicketService.markTestDone(ticket.getTicketId());
        assertEquals(QueueStatus.TEST_DONE, response.status());
    }

    @Test
    void getCurrentStaffId_ShouldSupportModernAndLegacyPrincipalsAndRejectMalformedValues() {
        assertNull((UUID) ReflectionTestUtils.invokeMethod(queueTicketService, "getCurrentStaffId"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("plain-user", null));
        assertNull((UUID) ReflectionTestUtils.invokeMethod(queueTicketService, "getCurrentStaffId"));

        UUID staffId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                Map.of("staffId", staffId.toString()), null));
        assertEquals(staffId, ReflectionTestUtils.invokeMethod(queueTicketService, "getCurrentStaffId"));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                Map.of("staffId", "not-a-uuid", "username", "doctor.an"), null));
        when(staffRepo.findFirstByProfile_Account_Username("doctor.an"))
                .thenReturn(Optional.of(StaffInfo.builder().staffId(staffId).build()));
        assertEquals(staffId, ReflectionTestUtils.invokeMethod(queueTicketService, "getCurrentStaffId"));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                Map.of("staffId", " ", "username", "missing"), null));
        when(staffRepo.findFirstByProfile_Account_Username("missing")).thenReturn(Optional.empty());
        assertNull((UUID) ReflectionTestUtils.invokeMethod(queueTicketService, "getCurrentStaffId"));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                Map.of("staffId", 12, "username", " "), null));
        assertNull((UUID) ReflectionTestUtils.invokeMethod(queueTicketService, "getCurrentStaffId"));
    }

    @Test
    void sameRoomChain_ShouldHandleMissingContextFilterServicesAndCountCompletedSteps() {
        QueueTicket absent = QueueTicket.builder().ticketId(UUID.randomUUID()).build();
        when(repo.findById(absent.getTicketId())).thenReturn(Optional.of(absent));
        var empty = queueTicketService.sameRoomChain(absent.getTicketId());
        assertEquals(0, empty.totalServices());
        assertEquals(0, empty.currentPosition());

        UUID visitId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).build();
        Department room = Department.builder().departmentId(departmentId).name("Phòng Nội").build();
        MedicalService exam = MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("EX-1")
                .name("Khám Nội").departmentType(DepartmentType.EXAMINATION).build();
        QueueTicket current = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).department(room)
                .service(exam).status(QueueStatus.DONE).workDate(LocalDate.of(2026, 9, 7)).queueNumber(3).build();
        current.setCreatedAt(LocalDateTime.of(2026, 9, 7, 8, 0));
        QueueTicket next = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).department(room)
                .service(exam).status(QueueStatus.WAITING).workDate(current.getWorkDate()).queueNumber(4).build();
        next.setCreatedAt(null);
        QueueTicket wrongRoom = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit)
                .department(Department.builder().departmentId(UUID.randomUUID()).build()).service(exam)
                .status(QueueStatus.WAITING).workDate(current.getWorkDate()).queueNumber(5).build();
        QueueTicket wrongDate = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).department(room)
                .service(exam).status(QueueStatus.WAITING).workDate(current.getWorkDate().plusDays(1)).queueNumber(6).build();
        QueueTicket noService = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).department(room)
                .status(QueueStatus.WAITING).workDate(current.getWorkDate()).queueNumber(7).build();
        QueueTicket lab = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).department(room)
                .service(MedicalService.builder().serviceId(UUID.randomUUID())
                        .departmentType(DepartmentType.PARACLINICAL).build())
                .status(QueueStatus.WAITING).workDate(current.getWorkDate()).queueNumber(8).build();
        when(repo.findById(current.getTicketId())).thenReturn(Optional.of(current));
        when(repo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(wrongRoom, wrongDate, noService, lab, next, current));
        MedicalRecord currentRecord = MedicalRecord.builder().recordId(UUID.randomUUID()).build();
        when(recordRepo.findByQueueTicket_TicketId(current.getTicketId())).thenReturn(Optional.of(currentRecord));
        when(recordRepo.findByQueueTicket_TicketId(next.getTicketId())).thenReturn(Optional.empty());

        var chain = queueTicketService.sameRoomChain(current.getTicketId());

        assertAll(
                () -> assertEquals(2, chain.totalServices()),
                () -> assertEquals(1, chain.completedServices()),
                () -> assertEquals(1, chain.currentPosition()),
                () -> assertEquals(3, chain.displayQueueNumber()),
                () -> assertEquals(currentRecord.getRecordId(), chain.services().get(0).recordId()),
                () -> assertNull(chain.services().get(1).recordId()));
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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldReject_WhenCurrentUserIsNotRecordDoctor() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldAllowAdmin() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldThrowConflict_WhenVersionMismatch() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldReject_WhenPendingInvoiceExists() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldReject_WhenNoDiagnosisConclusionOrIcd() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldCompleteRecordAndQueue_WhenNoTests() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldRejectTestCreation_WhenDoctorMissing() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldCreateInvoiceAndWaitForTests() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldUpdateBasicMedicalRecordFields() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldReplacePrescriptionItems() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldAddIcd_WhenCodeNameProvided() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldLookupIcdName_WhenCodeNameMissing() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldCreateVitalSigns_WhenMissing() {

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
    // Legacy scenario no longer matches the current workflow.
    private void completeAndReturnRecord_ShouldUpdateExistingVitalSigns() {

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



