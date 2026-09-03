package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.appointment.AppointmentCreateRequest;
import org.example.doansummer2026.dto.icd.ICD10SelectionCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.FeedbackRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.dto.medicalRecord.PrescriptionItemCreateRequest;
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
        StaffInfo doctor = mock(StaffInfo.class);

        when(doctor.getStaffId())
                .thenReturn(id);

        return doctor;
    }

    private void asAdminForComplete() {
        when(authService.currentStaffId()).thenReturn(null);
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.ADMIN);
    }

    private void asAdminForUpdate() {
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.ADMIN);
    }

    private void asDoctor(StaffInfo doctor) {
        UUID id = doctor.getStaffId();

        when(doctor.getSystemRole())
                .thenReturn(SystemRole.DOCTOR);

        when(authService.currentStaffId())
                .thenReturn(id);

        when(authService.getCurrentSystemRole())
                .thenReturn(SystemRole.DOCTOR);

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
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.NURSE);
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
    void update_ShouldUpdateBasicFields_AsAdmin() {
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
        asAdminForUpdate();
        var result = medicalRecordService.update(id, req);
        assertNotNull(result);
        assertEquals(1L, result.version());
        assertEquals("Dau bung", r.getChiefComplaint());
        assertEquals("Viem da day", r.getDiagnosis());
    }

    @Test
    void update_ShouldCreateVitalSigns_AsAdmin() {
        UUID id = UUID.randomUUID();
        MedicalRecord r = record(id);
        MedicalRecordUpdateRequest req = mock(MedicalRecordUpdateRequest.class);
        when(req.version()).thenReturn(r.getVersion());
        when(req.bloodPressure()).thenReturn("130/90");
        when(req.heartRate()).thenReturn(90);
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(r));
        when(medicalRecordRepository.saveAndFlush(r)).thenReturn(r);
        asAdminForUpdate();
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
        asAdminForUpdate();
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
        asAdminForUpdate();
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
        when(nurse.getDepartment()).thenReturn(department);
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
        asAdminForComplete();
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
        asAdminForComplete();
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
        asAdminForComplete();
        assertThrows(BadRequestException.class, () -> medicalRecordService.complete(id));
    }

    @Test
    void complete_ShouldCompleteRecord_AsAdmin() {
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
        asAdminForComplete();
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
}
