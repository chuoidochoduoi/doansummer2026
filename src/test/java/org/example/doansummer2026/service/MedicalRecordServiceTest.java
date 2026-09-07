package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.appointment.AppointmentCreateRequest;
import org.example.doansummer2026.dto.icd.ICD10SelectionCreateRequest;
import org.example.doansummer2026.dto.medicalrecord.FeedbackRequest;
import org.example.doansummer2026.dto.medicalrecord.MedicalRecordCreateRequest;
import org.example.doansummer2026.dto.medicalrecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.dto.medicalrecord.PrescriptionItemCreateRequest;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private CustomerVisitRepository customerVisitRepository;
    @Mock private StaffInfoRepository staffInfoRepository;
    @Mock private VitalSignsRepository vitalSignsRepository;
    @Mock private QueueTicketRepository queueTicketRepository;
    @Mock private Icd10CodeRepository icd10CodeRepository;
    @Mock private TestRequestRepository testRequestRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private MedicalServiceRepository medicalServiceRepository;
    @Mock private ShiftConfigRepository shiftConfigRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuthService authService;
    @Mock private StaffDutyService staffDutyService;
    @Mock private ShiftScheduleResolver shiftScheduleResolver;
    @Mock private ClinicalFormTemplateService clinicalFormTemplateService;
    @Mock private TestResultRevisionRepository testResultRevisionRepository;
    @Mock private TestResultAttachmentRepository testResultAttachmentRepository;
    @Mock private SameDayParaclinicalResultService sameDayParaclinicalResultService;

    @InjectMocks
    private MedicalRecordService medicalRecordService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MedicalRecord record(UUID id) {
        return MedicalRecord.builder()
                .recordId(id)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .prescriptionItems(new LinkedHashSet<>())
                .icdSelections(new LinkedHashSet<>())
                .build();
    }

    private VitalSigns validVitalSigns(MedicalRecord record) {
        return VitalSigns.builder()
                .medicalRecord(record)
                .bloodPressure("120/80")
                .heartRate(72)
                .temperature(new BigDecimal("36.5"))
                .height(new BigDecimal("170"))
                .weight(new BigDecimal("60.0"))
                .build();
    }

    private StaffInfo doctor(UUID id) {
        return StaffInfo.builder().staffId(id).systemRole(SystemRole.DOCTOR).build();
    }

    private void asTreatingDoctor(MedicalRecord record) {
        StaffInfo treatingDoctor = doctor(UUID.randomUUID());
        record.setDoctor(treatingDoctor);
        asDoctor(treatingDoctor);
    }

    private void asDoctor(StaffInfo doctor) {
        UUID id = doctor.getStaffId();


        when(authService.currentStaffId())
                .thenReturn(id);


        when(staffInfoRepository.findById(id))
                .thenReturn(Optional.of(doctor));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "doctor",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_DOCTOR")
                        )
                )
        );
    }

    private void asNurse(StaffInfo nurse, UUID nurseId) {
        when(authService.currentStaffId()).thenReturn(nurseId);
        when(nurse.getSystemRole()).thenReturn(SystemRole.NURSE);
        when(staffInfoRepository.findById(nurseId)).thenReturn(Optional.of(nurse));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "nurse",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_NURSE"))
                )
        );
    }

    @Test
    void findById_ShouldReturnRecord_WhenRecordExists() {
        UUID id = UUID.randomUUID();
        MedicalRecord r = mock(MedicalRecord.class);
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        assertSame(r, medicalRecordService.findById(id));
    }

    @Test
    void administratorCannotEditClinicalRecordThroughDoctorWorkflow() {
        MedicalRecord record = record(UUID.randomUUID());
        StaffInfo admin = StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(SystemRole.ADMIN).build();
        when(medicalRecordRepository.findById(record.getRecordId())).thenReturn(Optional.of(record));
        when(authService.currentStaffId()).thenReturn(admin.getStaffId());
        when(staffInfoRepository.findById(admin.getStaffId())).thenReturn(Optional.of(admin));
        assertThrows(BadRequestException.class, () -> medicalRecordService.update(
                record.getRecordId(), mock(MedicalRecordUpdateRequest.class)));
        verify(medicalRecordRepository, never()).saveAndFlush(any());
    }

    @Test
    void administratorCannotCompleteClinicalRecordThroughDoctorWorkflow() {
        MedicalRecord record = record(UUID.randomUUID());
        StaffInfo admin = StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(SystemRole.ADMIN).build();
        record.setDoctor(doctor(UUID.randomUUID()));
        when(medicalRecordRepository.findById(record.getRecordId())).thenReturn(Optional.of(record));
        when(authService.currentStaffId()).thenReturn(admin.getStaffId());
        when(staffInfoRepository.findById(admin.getStaffId())).thenReturn(Optional.of(admin));
        assertThrows(BadRequestException.class, () -> medicalRecordService.complete(record.getRecordId()));
        assertEquals(MedicalRecordStatus.IN_PROGRESS, record.getStatus());
        verify(medicalRecordRepository, never()).save(any());
        verifyNoInteractions(notificationService, queueTicketRepository);
    }

    @Test
    void findById_ShouldThrowNotFound_WhenRecordDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> medicalRecordService.findById(id));
    }

    @Test
    void delete_ShouldThrowNotFound_WhenMissing() {
        UUID id = UUID.randomUUID();
        when(medicalRecordRepository.existsById(id)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> medicalRecordService.delete(id));
    }

    @Test
    void delete_ShouldRejectDeletion_WhenRecordExists() {
        UUID id = UUID.randomUUID();
        when(medicalRecordRepository.existsById(id)).thenReturn(true);
        assertThrows(ConflictException.class, () -> medicalRecordService.delete(id));
        verify(medicalRecordRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void create_ShouldThrowNotFound_WhenVisitDoesNotExist() {
        UUID visitId = UUID.randomUUID();
        MedicalRecordCreateRequest req = mock(MedicalRecordCreateRequest.class);
        when(req.visitId()).thenReturn(visitId);
        when(customerVisitRepository.findById(visitId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> medicalRecordService.create(req));
    }

    @Test
    void create_ShouldThrowConflict_WhenVisitAlreadyHasIndependentRecord() {
        UUID visitId = UUID.randomUUID();
        MedicalRecordCreateRequest req = mock(MedicalRecordCreateRequest.class);
        when(req.visitId()).thenReturn(visitId);
        when(customerVisitRepository.findById(visitId)).thenReturn(Optional.of(mock(CustomerVisit.class)));
        when(medicalRecordRepository.findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(visitId))
                .thenReturn(Optional.of(mock(MedicalRecord.class)));
        assertThrows(ConflictException.class, () -> medicalRecordService.create(req));
    }

    @Test
    void create_ShouldThrowNotFound_WhenDoctorDoesNotExist() {
        UUID visitId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        MedicalRecordCreateRequest req = mock(MedicalRecordCreateRequest.class);
        when(req.visitId()).thenReturn(visitId);
        when(req.doctorId()).thenReturn(doctorId);
        when(customerVisitRepository.findById(visitId)).thenReturn(Optional.of(mock(CustomerVisit.class)));
        when(medicalRecordRepository.findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(visitId))
                .thenReturn(Optional.empty());
        when(staffInfoRepository.findById(doctorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> medicalRecordService.create(req));
    }

    @Test
    void update_ShouldRejectCompletedRecord() {
        UUID id = UUID.randomUUID();
        MedicalRecord r = MedicalRecord.builder().recordId(id).status(MedicalRecordStatus.COMPLETED).build();
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        assertThrows(ConflictException.class,
                () -> medicalRecordService.update(id, mock(MedicalRecordUpdateRequest.class)));
    }

    @Test
    void update_ShouldUpdateBasicFields_AsTreatingDoctor() {
        UUID id = UUID.randomUUID();
        MedicalRecord r = record(id);
        MedicalRecordUpdateRequest req = mock(MedicalRecordUpdateRequest.class);
        when(req.version()).thenReturn(r.getVersion());
        when(req.chiefComplaint()).thenReturn("Dau bung");
        when(req.diagnosis()).thenReturn("Viem da day");
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        when(medicalRecordRepository.saveAndFlush(r)).thenAnswer(invocation -> {
            r.setVersion(r.getVersion() + 1);
            return r;
        });
        asTreatingDoctor(r);
        var result = medicalRecordService.update(id, req);
        assertNotNull(result);
        assertEquals(1L, result.version());
        assertEquals("Dau bung", r.getChiefComplaint());
        assertEquals("Viem da day", r.getDiagnosis());
    }

    @Test
    void update_ShouldCreateVitalSigns_AsTreatingDoctor() {
        UUID id = UUID.randomUUID();
        MedicalRecord r = record(id);
        MedicalRecordUpdateRequest req = mock(MedicalRecordUpdateRequest.class);
        when(req.version()).thenReturn(r.getVersion());
        when(req.bloodPressure()).thenReturn("130/90");
        when(req.heartRate()).thenReturn(90);
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        when(medicalRecordRepository.saveAndFlush(r)).thenReturn(r);
        asTreatingDoctor(r);
        medicalRecordService.update(id, req);
        assertNotNull(r.getVitalSigns());
        assertEquals("130/90", r.getVitalSigns().getBloodPressure());
    }

    @Test
    void update_ShouldAddIcdSelection_WhenCodeNameProvided() {
        UUID id = UUID.randomUUID();
        MedicalRecord r = record(id);
        ICD10SelectionCreateRequest icd = mock(ICD10SelectionCreateRequest.class);
        when(icd.code()).thenReturn("J02.9");
        when(icd.codeName()).thenReturn("Viem hong cap");
        MedicalRecordUpdateRequest req = mock(MedicalRecordUpdateRequest.class);
        when(req.version()).thenReturn(r.getVersion());
        when(req.icdSelections()).thenReturn(List.of(icd));
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        when(medicalRecordRepository.saveAndFlush(r)).thenReturn(r);
        asTreatingDoctor(r);
        medicalRecordService.update(id, req);
        assertEquals(1, r.getIcdSelections().size());
        assertEquals("Viem hong cap", r.getIcdSelections().iterator().next().getCodeName());
    }

    @Test
    void update_ShouldLookupIcdName_WhenCodeNameMissing() {
        UUID id = UUID.randomUUID();
        MedicalRecord r = record(id);
        ICD10SelectionCreateRequest icd = mock(ICD10SelectionCreateRequest.class);
        when(icd.code()).thenReturn("J02.9");
        when(icd.codeName()).thenReturn(null);
        Icd10Code code = mock(Icd10Code.class);
        when(code.getName()).thenReturn("Viem hong cap");
        MedicalRecordUpdateRequest req = mock(MedicalRecordUpdateRequest.class);
        when(req.version()).thenReturn(r.getVersion());
        when(req.icdSelections()).thenReturn(List.of(icd));
        when(icd10CodeRepository.findById("J02.9")).thenReturn(Optional.of(code));
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        when(medicalRecordRepository.saveAndFlush(r)).thenReturn(r);
        asTreatingDoctor(r);
        medicalRecordService.update(id, req);
        assertEquals("Viem hong cap", r.getIcdSelections().iterator().next().getCodeName());
    }

    @Test
    void saveDraft_ShouldAllowDoctor_WhenDoctorOwnsRecord() {
        UUID id = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        StaffInfo doctor = doctor(doctorId);
        MedicalRecord r = record(id);
        r.setDoctor(doctor);
        MedicalRecordUpdateRequest req = mock(MedicalRecordUpdateRequest.class);
        when(req.version()).thenReturn(r.getVersion());
        when(req.diagnosis()).thenReturn("Viem phe quan");
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        when(medicalRecordRepository.saveAndFlush(r)).thenReturn(r);
        asDoctor(doctor);
        var result = medicalRecordService.saveDraft(id, req);
        assertNotNull(result);
        assertEquals(MedicalRecordStatus.DRAFT, r.getStatus());
    }

    @Test
    void saveDraft_ShouldRejectDoctor_WhenAnotherDoctorOwnsRecord() {
        UUID id = UUID.randomUUID();
        StaffInfo owner = doctor(UUID.randomUUID());
        StaffInfo actor = doctor(UUID.randomUUID());
        MedicalRecord r = record(id);
        r.setDoctor(owner);
        MedicalRecordUpdateRequest req = mock(MedicalRecordUpdateRequest.class);
        when(req.version()).thenReturn(r.getVersion());
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        asDoctor(actor);
        assertThrows(BadRequestException.class, () -> medicalRecordService.saveDraft(id, req));
    }

    @Test
    void saveDraft_ShouldRejectNurse_WhenRecordHasNoQueueTicket() {
        UUID id = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();
        StaffInfo nurse = mock(StaffInfo.class);
        MedicalRecord r = record(id);
        MedicalRecordUpdateRequest req = mock(MedicalRecordUpdateRequest.class);
        when(req.version()).thenReturn(r.getVersion());
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        asNurse(nurse, nurseId);
        assertThrows(BadRequestException.class, () -> medicalRecordService.saveDraft(id, req));
        verify(medicalRecordRepository, never()).save(r);
    }

    @Test
    void saveDraft_ShouldAllowNurse_WhenDepartmentMatches() {
        UUID id = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();
        Department department = Department.builder().departmentId(UUID.randomUUID()).build();
        StaffInfo nurse = mock(StaffInfo.class);
        QueueTicket ticket = mock(QueueTicket.class);
        when(ticket.getDepartment()).thenReturn(department);
        MedicalRecord r = record(id);
        r.setQueueTicket(ticket);
        MedicalRecordUpdateRequest req = mock(MedicalRecordUpdateRequest.class);
        when(req.version()).thenReturn(r.getVersion());
        when(req.chiefComplaint()).thenReturn("Met moi");
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        when(medicalRecordRepository.saveAndFlush(r)).thenReturn(r);
        asNurse(nurse, nurseId);
        var result = medicalRecordService.saveDraft(id, req);
        assertNotNull(result);
        assertEquals(MedicalRecordStatus.DRAFT, r.getStatus());
        assertSame(nurse, r.getNursingUpdatedBy());
    }

    @Test
    void complete_ShouldRejectAlreadyCompletedRecord() {
        UUID id = UUID.randomUUID();
        MedicalRecord r = MedicalRecord.builder().recordId(id).status(MedicalRecordStatus.COMPLETED).build();
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        asTreatingDoctor(r);
        assertThrows(BadRequestException.class, () -> medicalRecordService.complete(id));
    }

    @Test
    void complete_ShouldRejectIncompleteTests() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        MedicalRecord r = record(id);
        r.setVisit(visit);
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        when(testRequestRepository.countByMedicalRecordAndStatusIn(eq(id), anyList())).thenReturn(1L);
        asTreatingDoctor(r);
        assertThrows(BadRequestException.class, () -> medicalRecordService.complete(id));
    }

    @Test
    void complete_ShouldRejectPendingInvoice() {
        UUID id = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        MedicalRecord r = record(id);
        r.setVisit(visit);
        Invoice invoice = mock(Invoice.class);
        when(invoice.getStatus()).thenReturn(InvoiceStatus.PENDING);
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        when(testRequestRepository.countByMedicalRecordAndStatusIn(eq(id), anyList())).thenReturn(0L);
        when(invoiceRepository.findAllByMedicalRecord_RecordId(id)).thenReturn(List.of(invoice));
        asTreatingDoctor(r);
        assertThrows(BadRequestException.class, () -> medicalRecordService.complete(id));
    }

    @Test
    void complete_ShouldCompleteRecord_AsTreatingDoctor() {
        UUID id = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).build();
        MedicalRecord r = record(id);
        r.setVisit(visit);
        r.setDiagnosis("Viem hong");
        r.setVitalSigns(validVitalSigns(r));
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        when(testRequestRepository.countByMedicalRecordAndStatusIn(eq(id), anyList())).thenReturn(0L);
        when(invoiceRepository.findAllByMedicalRecord_RecordId(id)).thenReturn(List.of());
        when(medicalRecordRepository.save(r)).thenReturn(r);
        when(queueTicketRepository.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        asTreatingDoctor(r);
        var result = medicalRecordService.complete(id);
        assertNotNull(result);
        assertEquals(MedicalRecordStatus.COMPLETED, r.getStatus());
        assertNotNull(r.getCompletedAt());
    }

    @Test
    void complete_ShouldSetQueueDone_WhenDoctorCompletesOwnRecord() {
        UUID id = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        StaffInfo doctor = doctor(doctorId);
        Department department = Department.builder()
                .departmentId(UUID.randomUUID())
                .headDoctor(doctor)
                .build();
        QueueTicket recordTicket = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .department(department)
                .status(QueueStatus.IN_PROGRESS)
                .build();
        QueueTicket active = mock(QueueTicket.class);
        when(active.getStatus()).thenReturn(QueueStatus.IN_PROGRESS);
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).build();
        MedicalRecord r = record(id);
        r.setVisit(visit);
        r.setQueueTicket(recordTicket);
        r.setDoctor(doctor);
        r.setDiagnosis("Viem hong");
        r.setVitalSigns(validVitalSigns(r));
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        when(testRequestRepository.countByMedicalRecordAndStatusIn(eq(id), anyList())).thenReturn(0L);
        when(invoiceRepository.findAllByMedicalRecord_RecordId(id)).thenReturn(List.of());
        when(medicalRecordRepository.save(r)).thenReturn(r);
        when(queueTicketRepository.findAllByVisit_VisitId(visitId)).thenReturn(List.of(active));
        asDoctor(doctor);
        medicalRecordService.complete(id);
        verify(active).setStatus(QueueStatus.DONE);
        verify(active).setCompletedAt(any(LocalDateTime.class));
        verify(queueTicketRepository).save(active);
        assertSame(doctor, r.getDoctorConfirmedBy());
    }

    @Test
    void validateVitalSignsForCompletion_ShouldRejectMissingValues() {
        MedicalRecord r = record(UUID.randomUUID());

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> medicalRecordService.validateVitalSignsForCompletion(r));

        assertTrue(error.getMessage().contains("nhịp tim"));
    }

    @Test
    void validateVitalSignsForCompletion_ShouldRejectInvalidBloodPressure() {
        MedicalRecord r = record(UUID.randomUUID());
        VitalSigns vitalSigns = validVitalSigns(r);
        vitalSigns.setBloodPressure("80/75");
        r.setVitalSigns(vitalSigns);

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> medicalRecordService.validateVitalSignsForCompletion(r));

        assertTrue(error.getMessage().contains("ít nhất 10 mmHg"));
    }

    @Test
    void inheritFirstVisitVitalSigns_ShouldCloneEarliestCompletedExaminationVitals() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).build();
        StaffInfo recorder = StaffInfo.builder().staffId(UUID.randomUUID()).build();
        Department examinationRoom = Department.builder()
                .departmentId(UUID.randomUUID())
                .departmentType(DepartmentType.EXAMINATION)
                .build();

        MedicalRecord source = record(UUID.randomUUID());
        source.setVisit(visit);
        source.setStatus(MedicalRecordStatus.COMPLETED);
        source.setQueueTicket(QueueTicket.builder().department(examinationRoom).build());
        VitalSigns sourceVitals = validVitalSigns(source);
        LocalDateTime measuredAt = LocalDateTime.of(2026, 9, 3, 8, 15);
        sourceVitals.setRecordedAt(measuredAt);
        sourceVitals.setRecordedBy(recorder);
        source.setVitalSigns(sourceVitals);

        MedicalRecord laterCompleted = record(UUID.randomUUID());
        laterCompleted.setVisit(visit);
        laterCompleted.setStatus(MedicalRecordStatus.COMPLETED);
        laterCompleted.setQueueTicket(QueueTicket.builder().department(examinationRoom).build());
        VitalSigns laterVitals = validVitalSigns(laterCompleted);
        laterVitals.setWeight(new BigDecimal("72.5"));
        laterCompleted.setVitalSigns(laterVitals);

        MedicalRecord target = record(UUID.randomUUID());
        target.setVisit(visit);
        target.setQueueTicket(QueueTicket.builder().department(examinationRoom).build());
        when(medicalRecordRepository.findAllByVisit_VisitIdOrderByCreatedAtAsc(visitId))
                .thenReturn(List.of(source, laterCompleted, target));
        when(medicalRecordRepository.save(target)).thenReturn(target);

        MedicalRecord result = medicalRecordService.inheritFirstVisitVitalSigns(target);

        assertSame(target, result);
        assertNotNull(target.getVitalSigns());
        assertNotSame(sourceVitals, target.getVitalSigns());
        assertSame(target, target.getVitalSigns().getMedicalRecord());
        assertEquals(sourceVitals.getBloodPressure(), target.getVitalSigns().getBloodPressure());
        assertEquals(sourceVitals.getHeartRate(), target.getVitalSigns().getHeartRate());
        assertEquals(sourceVitals.getTemperature(), target.getVitalSigns().getTemperature());
        assertEquals(sourceVitals.getHeight(), target.getVitalSigns().getHeight());
        assertEquals(sourceVitals.getWeight(), target.getVitalSigns().getWeight());
        assertEquals(measuredAt, target.getVitalSigns().getRecordedAt());
        assertSame(recorder, target.getVitalSigns().getRecordedBy());
        verify(medicalRecordRepository).save(target);
    }

    @Test
    void inheritFirstVisitVitalSigns_ShouldNeverOverwriteExistingVitals() {
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        MedicalRecord target = record(UUID.randomUUID());
        target.setVisit(visit);
        VitalSigns existing = validVitalSigns(target);
        existing.setWeight(new BigDecimal("72.5"));
        target.setVitalSigns(existing);

        MedicalRecord result = medicalRecordService.inheritFirstVisitVitalSigns(target);

        assertSame(target, result);
        assertSame(existing, target.getVitalSigns());
        assertEquals(new BigDecimal("72.5"), target.getVitalSigns().getWeight());
        verifyNoInteractions(medicalRecordRepository);
    }


    @Test
    void inheritFirstVisitVitalSigns_ShouldNotCopyFromUnfinishedExamination() {
        var visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        var room = Department.builder().departmentType(DepartmentType.EXAMINATION).build();
        var source = record(UUID.randomUUID());
        source.setVisit(visit);
        source.setStatus(MedicalRecordStatus.IN_PROGRESS);
        source.setQueueTicket(QueueTicket.builder().department(room).build());
        source.setVitalSigns(validVitalSigns(source));
        var target = record(UUID.randomUUID());
        target.setVisit(visit);
        when(medicalRecordRepository.findAllByVisit_VisitIdOrderByCreatedAtAsc(visit.getVisitId()))
                .thenReturn(List.of(source, target));

        medicalRecordService.inheritFirstVisitVitalSigns(target);

        assertNull(target.getVitalSigns());
        assertEquals(MedicalRecordStatus.IN_PROGRESS, source.getStatus());
        verify(medicalRecordRepository, never()).save(any());
    }

    @Test
    void inheritedVitalsAcrossRoomsRemainIndependentAndThirdUsesFirstSource() {
        var visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        var firstRoom = Department.builder().departmentId(UUID.randomUUID()).departmentType(DepartmentType.EXAMINATION).build();
        var nextRoom = Department.builder().departmentId(UUID.randomUUID()).departmentType(DepartmentType.EXAMINATION).build();
        var first = record(UUID.randomUUID());
        first.setVisit(visit);
        first.setStatus(MedicalRecordStatus.COMPLETED);
        first.setQueueTicket(QueueTicket.builder().department(firstRoom).build());
        first.setVitalSigns(validVitalSigns(first));
        var originalWeight = first.getVitalSigns().getWeight();
        var second = record(UUID.randomUUID());
        second.setVisit(visit);
        second.setQueueTicket(QueueTicket.builder().department(nextRoom).build());
        var third = record(UUID.randomUUID());
        third.setVisit(visit);
        third.setQueueTicket(QueueTicket.builder().department(firstRoom).build());
        when(medicalRecordRepository.findAllByVisit_VisitIdOrderByCreatedAtAsc(visit.getVisitId()))
                .thenReturn(List.of(first, second, third));
        when(medicalRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        medicalRecordService.inheritFirstVisitVitalSigns(second);
        second.getVitalSigns().setWeight(new BigDecimal("75.0"));
        second.setStatus(MedicalRecordStatus.COMPLETED);
        medicalRecordService.inheritFirstVisitVitalSigns(third);

        assertEquals(originalWeight, first.getVitalSigns().getWeight());
        assertEquals(originalWeight, third.getVitalSigns().getWeight());
        assertEquals(new BigDecimal("75.0"), second.getVitalSigns().getWeight());
        assertNotSame(first.getVitalSigns(), second.getVitalSigns());
        assertNotSame(first.getVitalSigns(), third.getVitalSigns());
        assertNotSame(second.getVitalSigns(), third.getVitalSigns());
    }

    @Test
    void rate_ShouldSaveRating_WhenCompleted() {
        UUID id = UUID.randomUUID();
        MedicalRecord r = MedicalRecord.builder().recordId(id).status(MedicalRecordStatus.COMPLETED).build();
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        when(medicalRecordRepository.save(r)).thenReturn(r);
        var result = medicalRecordService.rate(id, 5);
        assertNotNull(result);
        assertEquals(5, r.getRatingScore());
    }

    @Test
    void search_ShouldReturnPage() {
        UUID doctorId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 10);
        when(medicalRecordRepository.search(doctorId, MedicalRecordStatus.COMPLETED, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));
        assertNotNull(medicalRecordService.search(
                doctorId, MedicalRecordStatus.COMPLETED, null, null, pageable));
    }

    @Test
    void scheduleFollowUp_ShouldReject_WhenAlreadyScheduled() {
        UUID id = UUID.randomUUID();
        MedicalRecord r = MedicalRecord.builder()
                .recordId(id)
                .followUpAppointment(mock(Appointment.class))
                .build();
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        assertThrows(ConflictException.class,
                () -> medicalRecordService.scheduleFollowUp(id, mock(AppointmentCreateRequest.class)));
    }

    @Test
    void rateRejectsOutOfRangeAndUnfinishedRecords() {
        assertThrows(BadRequestException.class, () -> medicalRecordService.rate(UUID.randomUUID(), 0));
        assertThrows(BadRequestException.class, () -> medicalRecordService.rate(UUID.randomUUID(), 6));
        MedicalRecord draft = record(UUID.randomUUID());
        when(medicalRecordRepository.findById(draft.getRecordId())).thenReturn(Optional.of(draft));
        assertThrows(BadRequestException.class, () -> medicalRecordService.rate(draft.getRecordId(), 4));
    }

    @Test
    void submitFeedbackChecksOwnershipAndCompletionThenStoresOnlyOverallFeedback() {
        Profile owner = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Anh Đức").build();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(owner).build();
        MedicalRecord completed = record(UUID.randomUUID());
        completed.setVisit(visit);
        completed.setStatus(MedicalRecordStatus.COMPLETED);
        completed.getFeedbackTargets().add(FeedbackTarget.builder().targetKey("old").targetType("STAFF")
                .targetName("Old").rating(1).build());
        when(medicalRecordRepository.findById(completed.getRecordId())).thenReturn(Optional.of(completed));
        when(medicalRecordRepository.save(completed)).thenReturn(completed);

        assertThrows(ResourceNotFoundException.class, () -> medicalRecordService.submitFeedback(
                completed.getRecordId(), UUID.randomUUID(), new FeedbackRequest(5, "Tốt")));

        var response = medicalRecordService.submitFeedback(completed.getRecordId(), owner.getProfileId(),
                new FeedbackRequest(5, "Bác sĩ giải thích rõ ràng"));

        assertEquals(5, response.overallRating());
        assertEquals("NEW", response.status());
        assertEquals("Bác sĩ giải thích rõ ràng", response.comment());
        assertTrue(response.targets().isEmpty());
        assertFalse(completed.getContactRequested());
    }

    @Test
    void submitFeedbackRejectsProfilelessAndUnfinishedRecords() {
        MedicalRecord noVisit = record(UUID.randomUUID());
        when(medicalRecordRepository.findById(noVisit.getRecordId())).thenReturn(Optional.of(noVisit));
        assertThrows(ResourceNotFoundException.class, () -> medicalRecordService.submitFeedback(
                noVisit.getRecordId(), UUID.randomUUID(), new FeedbackRequest(4, null)));

        Profile owner = Profile.builder().profileId(UUID.randomUUID()).build();
        noVisit.setVisit(CustomerVisit.builder().customer(owner).build());
        assertThrows(BadRequestException.class, () -> medicalRecordService.submitFeedback(
                noVisit.getRecordId(), owner.getProfileId(), new FeedbackRequest(4, null)));
    }

    @Test
    void feedbackListingCountingAndResponseCoverBothModes() {
        MedicalRecord rated = record(UUID.randomUUID());
        rated.setFeedbackTargets(new LinkedHashSet<>());
        rated.setRatingScore(4);
        var page = new PageImpl<>(List.of(rated));
        PageRequest pageable = PageRequest.of(0, 10);
        when(medicalRecordRepository.findByRatingScoreIsNotNull(pageable)).thenReturn(page);
        when(medicalRecordRepository.findFeedbacksForStaff(any(), eq(pageable))).thenReturn(page);
        when(medicalRecordRepository.countUnansweredFeedbacks()).thenReturn(3L);

        assertEquals(1, medicalRecordService.listFeedbacks(null, pageable).content().size());
        assertEquals(1, medicalRecordService.listFeedbacks(UUID.randomUUID(), pageable).content().size());
        assertEquals(3, medicalRecordService.countUnansweredFeedbacks());

        StaffInfo manager = StaffInfo.builder().staffId(UUID.randomUUID())
                .profile(Profile.builder().fullName("Quản lý").build()).build();
        when(medicalRecordRepository.findById(rated.getRecordId())).thenReturn(Optional.of(rated));
        when(staffInfoRepository.findById(manager.getStaffId())).thenReturn(Optional.of(manager));
        when(medicalRecordRepository.save(rated)).thenReturn(rated);
        var responded = medicalRecordService.respondFeedback(rated.getRecordId(), manager.getStaffId(), "Đã tiếp nhận");
        assertEquals("RESPONDED", responded.status());
        assertEquals("Quản lý", responded.respondedByName());
        assertEquals("IN_REVIEW", medicalRecordService.respondFeedback(rated.getRecordId(), null, null).status());
    }

    @Test
    void explainFeedbackAcceptsDoctorOrTargetAndRejectsUnrelatedStaff() {
        StaffInfo doctor = doctor(UUID.randomUUID());
        MedicalRecord rated = record(UUID.randomUUID());
        rated.setDoctor(doctor);
        rated.setFeedbackTargets(new LinkedHashSet<>());
        when(medicalRecordRepository.findById(rated.getRecordId())).thenReturn(Optional.of(rated));
        when(medicalRecordRepository.save(rated)).thenReturn(rated);

        assertEquals("WAITING_INTERNAL", medicalRecordService.explainFeedback(
                rated.getRecordId(), doctor.getStaffId(), "Đã giải thích").status());
        assertThrows(ResourceNotFoundException.class, () -> medicalRecordService.explainFeedback(
                rated.getRecordId(), UUID.randomUUID(), "Không liên quan"));

        StaffInfo nurse = StaffInfo.builder().staffId(UUID.randomUUID()).build();
        rated.setDoctor(null);
        rated.getFeedbackTargets().add(FeedbackTarget.builder().staff(nurse).targetKey("nurse")
                .targetType("STAFF").targetName("Y tá").rating(5).build());
        assertDoesNotThrow(() -> medicalRecordService.explainFeedback(
                rated.getRecordId(), nurse.getStaffId(), "Cảm ơn"));
    }

    @Test
    void pendingFollowUpsNormalizeSearchAndPreviousHistoryHandlesMissingPatient() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(medicalRecordRepository.findPendingFollowUps("nguyen", pageable))
                .thenReturn(new PageImpl<>(List.of()));
        assertTrue(medicalRecordService.getPendingFollowUps("  NGUYEN ", pageable).content().isEmpty());

        MedicalRecord current = record(UUID.randomUUID());
        when(medicalRecordRepository.findById(current.getRecordId())).thenReturn(Optional.of(current));
        assertTrue(medicalRecordService.getPreviousHistoryForDoctor(current.getRecordId()).isEmpty());

        Profile patient = Profile.builder().profileId(UUID.randomUUID()).build();
        current.setVisit(CustomerVisit.builder().customer(patient).build());
        when(medicalRecordRepository.findCompletedHistoryByProfileIdExcludingRecord(
                patient.getProfileId(), current.getRecordId())).thenReturn(List.of());
        assertTrue(medicalRecordService.getPreviousHistoryForDoctor(current.getRecordId()).isEmpty());
    }

    @Test
    void patientVisitDetailRejectsMissingVisitAndForeignProfileWithoutLeakingData() {
        UUID visitId = UUID.randomUUID();
        UUID requestedProfileId = UUID.randomUUID();
        when(customerVisitRepository.findById(visitId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> medicalRecordService.getPatientVisitDetail(visitId, requestedProfileId));

        Profile owner = Profile.builder().profileId(UUID.randomUUID()).build();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).customer(owner).build();
        when(customerVisitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        assertThrows(ResourceNotFoundException.class,
                () -> medicalRecordService.getPatientVisitDetail(visitId, requestedProfileId));
        verify(medicalRecordRepository, never()).findAllByVisit_VisitIdOrderByCreatedAtAsc(any());
    }

    @Test
    void patientVisitDetailDoesNotPublishDraftExaminationOrUnsignedResults() {
        UUID visitId = UUID.randomUUID();
        Profile owner = Profile.builder().profileId(UUID.randomUUID()).build();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).customer(owner).build();
        Department examination = Department.builder().departmentType(DepartmentType.EXAMINATION).build();
        QueueTicket queue = QueueTicket.builder().department(examination).build();
        MedicalRecord draft = record(UUID.randomUUID());
        draft.setVisit(visit);
        draft.setQueueTicket(queue);
        TestResult unsignedResult = TestResult.builder().resultId(UUID.randomUUID()).build();
        TestRequest unsignedRequest = TestRequest.builder().status(TestRequestStatus.COMPLETED)
                .testResult(unsignedResult).build();
        when(customerVisitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findAllByVisit_VisitIdOrderByCreatedAtAsc(visitId)).thenReturn(List.of(draft));
        when(testRequestRepository.findAllByVisitIdWithDetails(visitId)).thenReturn(List.of(unsignedRequest));
        when(testResultRevisionRepository.findFirstByTestResult_ResultIdAndStatusOrderByRevisionNoDesc(
                unsignedResult.getResultId(), TestResultRevisionStatus.SIGNED)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> medicalRecordService.getPatientVisitDetail(visitId, owner.getProfileId()));
    }

    @Test
    void visitDetailEntryPointsEnforceOwnershipAndRequireLinkedVisit() {
        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID foreignProfileId = UUID.randomUUID();
        when(medicalRecordRepository.findFirstByVisit_VisitIdOrderByCreatedAtDesc(visitId))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> medicalRecordService.getVisitDetail(visitId, foreignProfileId));

        MedicalRecord detached = record(recordId);
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(detached));
        assertThrows(ResourceNotFoundException.class,
                () -> medicalRecordService.getVisitDetailByRecordId(recordId, foreignProfileId));
        assertThrows(ResourceNotFoundException.class,
                () -> medicalRecordService.getVisitDetailForStaff(recordId));
    }

    @Test
    void historyQueriesApplyStableDefaultSortAndPreserveExplicitSort() {
        UUID profileId = UUID.randomUUID();
        PageRequest unsorted = PageRequest.of(1, 7);
        when(medicalRecordRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        medicalRecordService.getMedicalHistoryForPatient(profileId, "  nội khoa  ", unsorted);
        verify(medicalRecordRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class),
                argThat((org.springframework.data.domain.Pageable page) -> page.getPageNumber() == 1
                        && page.getPageSize() == 7
                        && page.getSort().getOrderFor("createdAt") != null));

        PageRequest explicit = PageRequest.of(0, 5,
                org.springframework.data.domain.Sort.by("checkOutTime").ascending());
        when(customerVisitRepository.findMedicalHistoryVisits(eq(profileId), eq("tim mạch"), eq(explicit)))
                .thenReturn(new PageImpl<>(List.of(), explicit, 0));
        medicalRecordService.getVisitHistoryForPatient(profileId, " tim mạch ", explicit);
        verify(customerVisitRepository).findMedicalHistoryVisits(profileId, "tim mạch", explicit);
    }

    @Test
    void visitHistorySummaryCombinesPublishedExaminationSignedTestsAndSkippedServices() {
        UUID visitId = UUID.randomUUID();
        Profile patient = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Anh Đức").build();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).customer(patient)
                .status(VisitStatus.COMPLETED).checkInTime(LocalDateTime.of(2026, 9, 5, 8, 30)).build();

        MedicalService examinationService = MedicalService.builder().serviceId(UUID.randomUUID())
                .name("Khám Nội tổng quát").build();
        Department examinationDepartment = Department.builder().departmentType(DepartmentType.EXAMINATION).build();
        QueueTicket examinationQueue = QueueTicket.builder().ticketId(UUID.randomUUID())
                .department(examinationDepartment).service(examinationService).status(QueueStatus.DONE).build();
        StaffInfo doctor = StaffInfo.builder().staffId(UUID.randomUUID())
                .profile(Profile.builder().fullName("Bác sĩ Đỗ Anh Tuấn").build()).build();
        MedicalRecord examinationRecord = record(UUID.randomUUID());
        examinationRecord.setVisit(visit);
        examinationRecord.setQueueTicket(examinationQueue);
        examinationRecord.setDoctor(doctor);
        examinationRecord.setStatus(MedicalRecordStatus.COMPLETED);
        examinationRecord.setDiagnosis("Tăng huyết áp nhẹ");

        MedicalService testService = MedicalService.builder().serviceId(UUID.randomUUID())
                .name("Đường huyết").build();
        TestResult testResult = TestResult.builder().resultId(UUID.randomUUID()).build();
        TestRequest signedRequest = TestRequest.builder().testRequestId(UUID.randomUUID())
                .service(testService).status(TestRequestStatus.COMPLETED).testResult(testResult).build();
        TestResultRevision signedRevision = TestResultRevision.builder().revisionId(UUID.randomUUID())
                .status(TestResultRevisionStatus.SIGNED).testResult(testResult).build();

        MedicalService skippedService = MedicalService.builder().serviceId(UUID.randomUUID())
                .name("Khám Tim mạch cơ bản").build();
        QueueTicket skippedQueue = QueueTicket.builder().ticketId(UUID.randomUUID())
                .service(skippedService).status(QueueStatus.SKIPPED).build();

        PageRequest pageable = PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by("checkInTime").descending());
        when(customerVisitRepository.findMedicalHistoryVisits(patient.getProfileId(), "", pageable))
                .thenReturn(new PageImpl<>(List.of(visit), pageable, 1));
        when(medicalRecordRepository.findAllByVisit_VisitIdOrderByCreatedAtAsc(visitId))
                .thenReturn(List.of(examinationRecord));
        when(testRequestRepository.findAllByVisitIdWithDetails(visitId)).thenReturn(List.of(signedRequest));
        when(testResultRevisionRepository.findFirstByTestResult_ResultIdAndStatusOrderByRevisionNoDesc(
                testResult.getResultId(), TestResultRevisionStatus.SIGNED)).thenReturn(Optional.of(signedRevision));
        when(queueTicketRepository.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(examinationQueue, skippedQueue));

        var response = medicalRecordService.getVisitHistoryForPatient(patient.getProfileId(), null, pageable);
        var summary = response.content().get(0);

        assertEquals(1, summary.examinationCount());
        assertEquals(1, summary.testCount());
        assertEquals("PARTIAL", summary.completionStatus());
        assertEquals(List.of("Khám Tim mạch cơ bản"), summary.skippedServiceNames());
        assertTrue(summary.completedServiceNames().containsAll(List.of("Khám Nội tổng quát", "Đường huyết")));
        assertEquals("Tăng huyết áp nhẹ", summary.diagnosisSummary());
    }

    @Test
    void scheduleFollowUpValidatesShiftExistenceDateAvailabilityAndActualTimeWindow() {
        UUID recordId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        MedicalRecord completed = record(recordId);
        completed.setVisit(CustomerVisit.builder().customer(Profile.builder().profileId(UUID.randomUUID()).build()).build());
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(completed));

        AppointmentCreateRequest request = new AppointmentCreateRequest(null,
                LocalDateTime.of(2026, 9, 8, 9, 0), null, shiftId, null);
        when(shiftConfigRepository.findById(shiftId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> medicalRecordService.scheduleFollowUp(recordId, request));

        ShiftConfig shift = ShiftConfig.builder().shiftId(shiftId).name("Ca sáng").build();
        when(shiftConfigRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        AppointmentCreateRequest missingDate = new AppointmentCreateRequest(null, null, null, shiftId, null);
        assertThrows(BadRequestException.class,
                () -> medicalRecordService.scheduleFollowUp(recordId, missingDate));

        var unavailable = new ShiftScheduleResolver.ResolvedShift(shift, null, null, null,
                ShiftTimeSource.NORMAL, ShiftUnavailableReason.SHIFT_OFF);
        when(shiftScheduleResolver.resolve(shift, request.scheduledAt().toLocalDate())).thenReturn(unavailable);
        assertThrows(ConflictException.class,
                () -> medicalRecordService.scheduleFollowUp(recordId, request));

        var available = new ShiftScheduleResolver.ResolvedShift(shift, null,
                java.time.LocalTime.of(8, 0), java.time.LocalTime.of(10, 0), ShiftTimeSource.NORMAL, null);
        when(shiftScheduleResolver.resolve(shift, request.scheduledAt().toLocalDate())).thenReturn(available);
        AppointmentCreateRequest beforeShift = new AppointmentCreateRequest(null,
                LocalDateTime.of(2026, 9, 8, 7, 59), null, shiftId, null);
        when(shiftScheduleResolver.resolve(shift, beforeShift.scheduledAt().toLocalDate())).thenReturn(available);
        assertThrows(BadRequestException.class,
                () -> medicalRecordService.scheduleFollowUp(recordId, beforeShift));

        AppointmentCreateRequest atEnd = new AppointmentCreateRequest(null,
                LocalDateTime.of(2026, 9, 8, 10, 0), null, shiftId, null);
        when(shiftScheduleResolver.resolve(shift, atEnd.scheduledAt().toLocalDate())).thenReturn(available);
        assertThrows(BadRequestException.class,
                () -> medicalRecordService.scheduleFollowUp(recordId, atEnd));
    }

    @Test
    void scheduleFollowUpCreatesCustomerAppointmentWithExplicitServices() {
        UUID recordId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Profile customer = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Anh Đức").build();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(customer).build();
        MedicalRecord completed = record(recordId);
        completed.setRecordCode("MR-2026-0001");
        completed.setVisit(visit);
        completed.setFollowUpNote("Tái khám sau 7 ngày");
        MedicalService service = MedicalService.builder().serviceId(serviceId).name("Khám Nội tổng quát").build();
        AppointmentCreateRequest request = new AppointmentCreateRequest(customer.getProfileId(),
                LocalDateTime.of(2026, 9, 8, 9, 0), null, null, java.util.Set.of(serviceId));
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(completed));
        when(medicalServiceRepository.findById(serviceId)).thenReturn(Optional.of(service));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(medicalRecordRepository.saveAndFlush(completed)).thenReturn(completed);

        var response = medicalRecordService.scheduleFollowUp(recordId, request);

        assertEquals(recordId, response.recordId());
        assertEquals(request.scheduledAt().toLocalDate(), response.followUpDate());
        assertNotNull(completed.getFollowUpAppointment());
        assertEquals(customer, completed.getFollowUpAppointment().getCustomer());
        assertEquals(java.util.Set.of(service), completed.getFollowUpAppointment().getServices());
    }

    @Test
    void scheduleFollowUpCopiesGuestIdentityAndFallsBackToOriginalService() {
        UUID recordId = UUID.randomUUID();
        Appointment oldAppointment = Appointment.builder().isGuest(true).guestFullName("Trần Minh Anh")
                .guestPhone("0900000001").guestAge(30).guestGender(Gender.FEMALE)
                .guestAddress("Hà Nội").build();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).appointment(oldAppointment).build();
        MedicalService originalService = MedicalService.builder().serviceId(UUID.randomUUID())
                .name("Khám Tim mạch cơ bản").build();
        MedicalRecord completed = record(recordId);
        completed.setVisit(visit);
        completed.setQueueTicket(QueueTicket.builder().service(originalService).build());
        AppointmentCreateRequest request = new AppointmentCreateRequest(null,
                LocalDateTime.of(2026, 9, 9, 14, 0), null, null, java.util.Set.of());
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(completed));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(medicalRecordRepository.saveAndFlush(completed)).thenReturn(completed);

        var response = medicalRecordService.scheduleFollowUp(recordId, request);

        Appointment created = completed.getFollowUpAppointment();
        assertTrue(created.getIsGuest());
        assertEquals("Trần Minh Anh", created.getGuestFullName());
        assertEquals("0900000001", created.getGuestPhone());
        assertTrue(created.getServices().contains(originalService));
        assertEquals("Trần Minh Anh", response.customerName());
    }

    @Test
    void receptionistPhoneSearchNormalizesRegisteredAndGuestProfiles() {
        Profile registered = Profile.builder().profileId(UUID.randomUUID()).patientCode("BN-0001")
                .fullName("Nguyễn Anh Đức").phone("0912345678")
                .account(Account.builder().role(Role.CUSTOMER).build()).build();
        when(profileRepository.findFirstByPhoneIn(List.of("0912345678", "+84912345678")))
                .thenReturn(Optional.of(registered));

        var registeredResult = medicalRecordService.searchByPhone(" 0912.345-678 ");

        assertEquals(1, registeredResult.size());
        assertEquals("Nguyễn Anh Đức", registeredResult.get(0).fullName());
        verifyNoInteractions(appointmentRepository);

        reset(profileRepository, appointmentRepository);
        Profile guestProfile = Profile.builder().profileId(UUID.randomUUID()).patientCode("BN-GUEST")
                .fullName("Lê Minh Anh").phone("0987654321").account(null).build();
        when(profileRepository.findFirstByPhoneIn(List.of("0987654321", "+84987654321")))
                .thenReturn(Optional.of(guestProfile));
        var guestResult = medicalRecordService.searchByPhone("+84987654321");
        assertEquals(1, guestResult.size());
        assertEquals("Lê Minh Anh", guestResult.get(0).fullName());
    }

    @Test
    void receptionistPhoneSearchFallsBackToGuestAppointmentsAndDeduplicatesIdentity() {
        Account staffAccount = Account.builder().role(Role.STAFF).build();
        Profile staff = Profile.builder().profileId(UUID.randomUUID()).account(staffAccount).build();
        when(profileRepository.findFirstByPhoneIn(List.of("0900000002", "+84900000002")))
                .thenReturn(Optional.of(staff));
        Appointment guest = Appointment.builder().isGuest(true).guestFullName("Phạm Thu Hà")
                .guestPhone("0900000002").guestGender(Gender.FEMALE).guestEmail("ha@example.test")
                .guestAddress("Đà Nẵng").build();
        when(appointmentRepository.findGuestAppointmentsByPhone("0900000002")).thenReturn(List.of(guest, guest));
        when(appointmentRepository.findGuestAppointmentsByPhone("+84900000002")).thenReturn(List.of(guest));

        var result = medicalRecordService.searchByPhone("0900 000 002");

        assertEquals(1, result.size());
        assertEquals("Phạm Thu Hà", result.get(0).fullName());
        assertEquals("0900000002", result.get(0).phone());
    }

    @Test
    void receptionistPhoneSearchTreatsBlankInputAsNoCandidates() {
        assertTrue(medicalRecordService.searchByPhone(null).isEmpty());
        assertTrue(medicalRecordService.searchByPhone("  . - ").isEmpty());
        verify(profileRepository, times(2)).findFirstByPhoneIn(List.of());
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void uniqueCustomerFiltersExecuteAllSupportedAgeGenderAndBloodTypeBranches() {
        PageRequest pageable = PageRequest.of(0, 10);
        var root = (jakarta.persistence.criteria.Root<Profile>) mock(
                jakarta.persistence.criteria.Root.class, RETURNS_DEEP_STUBS);
        var query = (jakarta.persistence.criteria.CriteriaQuery<Object>) mock(
                jakarta.persistence.criteria.CriteriaQuery.class, RETURNS_DEEP_STUBS);
        var criteriaBuilder = mock(jakarta.persistence.criteria.CriteriaBuilder.class, RETURNS_DEEP_STUBS);
        doAnswer(invocation -> {
            org.springframework.data.jpa.domain.Specification<Profile> specification = invocation.getArgument(0);
            assertDoesNotThrow(() -> specification.toPredicate(root, query, criteriaBuilder));
            return new PageImpl<Profile>(List.of(), pageable, 0);
        }).when(profileRepository).findAll(
                org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Profile>>any(),
                org.mockito.ArgumentMatchers.<org.springframework.data.domain.Pageable>same(pageable));

        for (String age : List.of("0-18", "19-40", "41-60", "60+", "không xác định")) {
            assertTrue(medicalRecordService.searchUniqueCustomers(
                    "Nguyễn", "Nam", age, BloodType.A_POSITIVE, pageable).content().isEmpty());
        }
        assertTrue(medicalRecordService.searchUniqueCustomers(
                "", "Nữ", "", null, pageable).content().isEmpty());
        verify(profileRepository, times(6)).findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                org.mockito.ArgumentMatchers.<org.springframework.data.domain.Pageable>same(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void receptionistRecordFiltersExecuteGuestAndRegisteredAgeBranches() {
        PageRequest pageable = PageRequest.of(0, 10);
        var root = (jakarta.persistence.criteria.Root<MedicalRecord>) mock(
                jakarta.persistence.criteria.Root.class, RETURNS_DEEP_STUBS);
        var query = (jakarta.persistence.criteria.CriteriaQuery<Object>) mock(
                jakarta.persistence.criteria.CriteriaQuery.class, RETURNS_DEEP_STUBS);
        var criteriaBuilder = mock(jakarta.persistence.criteria.CriteriaBuilder.class, RETURNS_DEEP_STUBS);
        doAnswer(invocation -> {
            org.springframework.data.jpa.domain.Specification<MedicalRecord> specification = invocation.getArgument(0);
            assertDoesNotThrow(() -> specification.toPredicate(root, query, criteriaBuilder));
            return new PageImpl<MedicalRecord>(List.of(), pageable, 0);
        }).when(medicalRecordRepository).findAll(
                org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<MedicalRecord>>any(),
                org.mockito.ArgumentMatchers.<org.springframework.data.domain.Pageable>same(pageable));

        for (String age : List.of("0-18", "19-40", "41-60", "60+", "không xác định")) {
            assertTrue(medicalRecordService.searchForReceptionist(
                    "MR-2026", "Nữ", age, BloodType.O_POSITIVE, pageable).content().isEmpty());
        }
        assertTrue(medicalRecordService.searchForReceptionist(
                null, null, null, null, pageable).content().isEmpty());
        verify(medicalRecordRepository, times(6)).findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                org.mockito.ArgumentMatchers.<org.springframework.data.domain.Pageable>same(pageable));
    }

    @Test
    void allergyUpdateValidatesStateCombinationsAndPersistsNormalizedItems() {
        UUID recordId = UUID.randomUUID();
        MedicalRecord record = record(recordId);
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        assertThrows(BadRequestException.class, () -> medicalRecordService.updatePatientAllergies(recordId,
                new org.example.doansummer2026.dto.medicalrecord.PatientAllergyRequest(
                        AllergyStatus.NONE_REPORTED, List.of())));

        Profile patient = Profile.builder().profileId(UUID.randomUUID()).build();
        record.setVisit(CustomerVisit.builder().customer(patient).build());
        assertThrows(BadRequestException.class, () -> medicalRecordService.updatePatientAllergies(recordId,
                new org.example.doansummer2026.dto.medicalrecord.PatientAllergyRequest(
                        AllergyStatus.UNVERIFIED, List.of())));
        assertThrows(BadRequestException.class, () -> medicalRecordService.updatePatientAllergies(recordId,
                new org.example.doansummer2026.dto.medicalrecord.PatientAllergyRequest(
                        AllergyStatus.REPORTED, List.of(" "))));
        assertThrows(BadRequestException.class, () -> medicalRecordService.updatePatientAllergies(recordId,
                new org.example.doansummer2026.dto.medicalrecord.PatientAllergyRequest(
                        AllergyStatus.NONE_REPORTED, List.of("Penicillin"))));

        when(profileRepository.save(patient)).thenReturn(patient);
        var none = medicalRecordService.updatePatientAllergies(recordId,
                new org.example.doansummer2026.dto.medicalrecord.PatientAllergyRequest(
                        AllergyStatus.NONE_REPORTED, null));
        assertEquals(AllergyStatus.NONE_REPORTED, none.status());
        assertEquals("", patient.getAllergies());

        var reported = medicalRecordService.updatePatientAllergies(recordId,
                new org.example.doansummer2026.dto.medicalrecord.PatientAllergyRequest(
                        AllergyStatus.REPORTED, java.util.Arrays.asList(
                                " Penicillin ", "penicillin", null, "Hải sản")));
        assertEquals(AllergyStatus.REPORTED, reported.status());
        assertEquals(List.of("Penicillin", "Hải sản"), reported.items());
        assertEquals("Penicillin\nHải sản", patient.getAllergies());
    }

    @Test
    void vitalSignValidationCoversEveryBoundaryAndAcceptsValidMeasurements() {
        MedicalRecord record = record(UUID.randomUUID());
        VitalSigns valid = validVitalSigns(record);
        record.setVitalSigns(valid);
        assertDoesNotThrow(() -> medicalRecordService.validateVitalSignsForCompletion(record));

        java.util.List<java.util.function.Consumer<VitalSigns>> invalidMutations = List.of(
                vital -> vital.setHeartRate(null),
                vital -> vital.setHeartRate(29),
                vital -> vital.setHeartRate(221),
                vital -> vital.setBloodPressure(null),
                vital -> vital.setBloodPressure(" "),
                vital -> vital.setBloodPressure("120-80"),
                vital -> vital.setBloodPressure("59/80"),
                vital -> vital.setBloodPressure("251/80"),
                vital -> vital.setBloodPressure("120/39"),
                vital -> vital.setBloodPressure("120/151"),
                vital -> vital.setBloodPressure("80/75"),
                vital -> vital.setTemperature(null),
                vital -> vital.setTemperature(new BigDecimal("33.9")),
                vital -> vital.setTemperature(new BigDecimal("43.1")),
                vital -> vital.setTemperature(new BigDecimal("36.55")),
                vital -> vital.setHeight(null),
                vital -> vital.setHeight(new BigDecimal("29")),
                vital -> vital.setHeight(new BigDecimal("251")),
                vital -> vital.setWeight(null),
                vital -> vital.setWeight(new BigDecimal("0.9")),
                vital -> vital.setWeight(new BigDecimal("300.1")),
                vital -> vital.setWeight(new BigDecimal("60.55"))
        );
        for (var mutation : invalidMutations) {
            VitalSigns candidate = validVitalSigns(record);
            mutation.accept(candidate);
            record.setVitalSigns(candidate);
            BadRequestException error = assertThrows(BadRequestException.class,
                    () -> medicalRecordService.validateVitalSignsForCompletion(record));
            assertTrue(error.getMessage().startsWith("Chỉ số sinh hiệu không hợp lệ:"));
        }
    }

    @Test
    void vitalSignPresenceHelpers_ShouldCoverEmptyAndEachIndividualField() {
        MedicalRecordCreateRequest emptyCreate = mock(MedicalRecordCreateRequest.class);
        when(emptyCreate.heartRate()).thenReturn(null);
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(medicalRecordService, "hasVitalSigns", emptyCreate)));
        MedicalRecordCreateRequest pressure = mock(MedicalRecordCreateRequest.class); when(pressure.bloodPressure()).thenReturn("120/80");
        MedicalRecordCreateRequest heart = mock(MedicalRecordCreateRequest.class); when(heart.heartRate()).thenReturn(70);
        MedicalRecordCreateRequest temperature = mock(MedicalRecordCreateRequest.class); when(temperature.heartRate()).thenReturn(null); when(temperature.temperature()).thenReturn(new BigDecimal("36.5"));
        MedicalRecordCreateRequest weight = mock(MedicalRecordCreateRequest.class); when(weight.heartRate()).thenReturn(null); when(weight.weight()).thenReturn(new BigDecimal("60"));
        MedicalRecordCreateRequest height = mock(MedicalRecordCreateRequest.class); when(height.heartRate()).thenReturn(null); when(height.height()).thenReturn(new BigDecimal("170"));
        for (MedicalRecordCreateRequest request : List.of(pressure, heart, temperature, weight, height)) {
            assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(medicalRecordService, "hasVitalSigns", request)));
        }

        MedicalRecordUpdateRequest emptyUpdate = mock(MedicalRecordUpdateRequest.class);
        when(emptyUpdate.heartRate()).thenReturn(null);
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(medicalRecordService, "hasVitalSignsUpdate", emptyUpdate)));
        MedicalRecordUpdateRequest updatePressure = mock(MedicalRecordUpdateRequest.class); when(updatePressure.bloodPressure()).thenReturn("110/70");
        MedicalRecordUpdateRequest updateHeart = mock(MedicalRecordUpdateRequest.class); when(updateHeart.heartRate()).thenReturn(65);
        MedicalRecordUpdateRequest updateTemperature = mock(MedicalRecordUpdateRequest.class); when(updateTemperature.heartRate()).thenReturn(null); when(updateTemperature.temperature()).thenReturn(new BigDecimal("37.0"));
        MedicalRecordUpdateRequest updateWeight = mock(MedicalRecordUpdateRequest.class); when(updateWeight.heartRate()).thenReturn(null); when(updateWeight.weight()).thenReturn(new BigDecimal("55"));
        MedicalRecordUpdateRequest updateHeight = mock(MedicalRecordUpdateRequest.class); when(updateHeight.heartRate()).thenReturn(null); when(updateHeight.height()).thenReturn(new BigDecimal("165"));
        for (MedicalRecordUpdateRequest request : List.of(updatePressure, updateHeart, updateTemperature, updateWeight, updateHeight)) {
            assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(medicalRecordService, "hasVitalSignsUpdate", request)));
        }
    }

    @Test
    void updateNursingDraftFields_ShouldCreateUpdateAndPreserveUnsubmittedVitals() {
        MedicalRecord record = record(UUID.randomUUID());
        MedicalRecordUpdateRequest noVitals = mock(MedicalRecordUpdateRequest.class);
        when(noVitals.heartRate()).thenReturn(null);
        ReflectionTestUtils.invokeMethod(medicalRecordService, "updateNursingDraftFields", record, noVitals);
        assertNull(record.getVitalSigns());

        MedicalRecordUpdateRequest create = mock(MedicalRecordUpdateRequest.class);
        when(create.chiefComplaint()).thenReturn("Đau đầu"); when(create.clinicalFindings()).thenReturn("Tỉnh táo");
        when(create.bloodPressure()).thenReturn("120/80"); when(create.heartRate()).thenReturn(72);
        when(create.temperature()).thenReturn(new BigDecimal("36.7")); when(create.weight()).thenReturn(new BigDecimal("60"));
        when(create.height()).thenReturn(new BigDecimal("170"));
        ReflectionTestUtils.invokeMethod(medicalRecordService, "updateNursingDraftFields", record, create);
        assertEquals("Đau đầu", record.getChiefComplaint()); assertEquals("Tỉnh táo", record.getClinicalFindings());

        MedicalRecordUpdateRequest partial = mock(MedicalRecordUpdateRequest.class);
        when(partial.heartRate()).thenReturn(80); when(partial.height()).thenReturn(new BigDecimal("171"));
        ReflectionTestUtils.invokeMethod(medicalRecordService, "updateNursingDraftFields", record, partial);
        assertEquals(80, record.getVitalSigns().getHeartRate());
        assertEquals(new BigDecimal("171"), record.getVitalSigns().getHeight());
        assertEquals("120/80", record.getVitalSigns().getBloodPressure());
        assertEquals(new BigDecimal("36.7"), record.getVitalSigns().getTemperature());
        assertEquals(new BigDecimal("60"), record.getVitalSigns().getWeight());
    }

    @Test
    void clinicalFormAccess_ShouldCoverMissingLinksStoredLatestAndPatientOwnership() {
        UUID recordId = UUID.randomUUID();
        MedicalRecord record = record(recordId);
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        assertThrows(ResourceNotFoundException.class, () -> medicalRecordService.getClinicalForm(recordId));
        MedicalService service = MedicalService.builder().serviceId(UUID.randomUUID()).build();
        record.setQueueTicket(QueueTicket.builder().service(service).build());
        ClinicalFormTemplateVersion latest = ClinicalFormTemplateVersion.builder().versionId(UUID.randomUUID()).build();
        var expectedLatest = mock(org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse.class);
        when(clinicalFormTemplateService.resolveVersion(service.getServiceId(), null)).thenReturn(latest);
        when(clinicalFormTemplateService.resolvedResponse(latest, null)).thenReturn(expectedLatest);
        assertSame(expectedLatest, medicalRecordService.getClinicalForm(recordId));

        ClinicalFormTemplateVersion stored = ClinicalFormTemplateVersion.builder().versionId(UUID.randomUUID()).build();
        var data = new tools.jackson.databind.json.JsonMapper().readTree("{\"pain\":2}");
        record.setFormTemplateVersion(stored); record.setSpecialtyData(data);
        var expectedStored = mock(org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse.class);
        when(clinicalFormTemplateService.resolvedResponse(stored, data)).thenReturn(expectedStored);
        assertSame(expectedStored, medicalRecordService.getClinicalForm(recordId));

        UUID profileId = UUID.randomUUID();
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> medicalRecordService.getClinicalFormForPatient(recordId, null));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> medicalRecordService.getClinicalFormForPatient(recordId, profileId));
        record.setVisit(CustomerVisit.builder().customer(Profile.builder().profileId(profileId).build()).build());
        assertSame(expectedStored, medicalRecordService.getClinicalFormForPatient(recordId, profileId));
    }
}
