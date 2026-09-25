package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.dto.vitalsigns.VitalSignsCreateRequest;
import vn.edu.fpt.cares.dto.vitalsigns.VitalSignsUpdateRequest;
import vn.edu.fpt.cares.enums.MedicalRecordStatus;
import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.Department;
import vn.edu.fpt.cares.model.MedicalRecord;
import vn.edu.fpt.cares.model.QueueTicket;
import vn.edu.fpt.cares.model.StaffInfo;
import vn.edu.fpt.cares.model.VitalSigns;
import vn.edu.fpt.cares.repository.MedicalRecordRepository;
import vn.edu.fpt.cares.repository.StaffInfoRepository;
import vn.edu.fpt.cares.repository.VitalSignsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VitalSignsServiceTest {

    @Mock VitalSignsRepository repository;
    @Mock MedicalRecordRepository medicalRecordRepository;
    @Mock StaffInfoRepository staffRepository;
    @Mock AuthService authService;
    @Mock StaffDutyService staffDutyService;
    @InjectMocks VitalSignsService service;

    @Test
    void createPersistsVitalsAndLinksThemToRecord() {
        UUID recordId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        MedicalRecord record = editableRecord(staffId, null);
        record.setRecordId(recordId);
        StaffInfo recorder = staff(staffId, SystemRole.DOCTOR);
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(authService.currentStaffId()).thenReturn(staffId);
        when(staffRepository.findById(staffId)).thenReturn(Optional.of(recorder));
        when(repository.save(any())).thenAnswer(invocation -> {
            VitalSigns value = invocation.getArgument(0);
            value.setVitalId(UUID.randomUUID());
            return value;
        });

        var response = service.create(createRequest(recordId));

        assertAll(
                () -> assertEquals(recordId, response.medicalRecordId()),
                () -> assertEquals("120/80", response.bloodPressure()),
                () -> assertEquals(72, response.heartRate()),
                () -> assertEquals(staffId, response.recordedById()),
                () -> assertNotNull(response.recordedAt()),
                () -> assertSame(record.getVitalSigns().getMedicalRecord(), record));
        verify(repository).save(any(VitalSigns.class));
    }

    @Test
    void createRejectsMissingRecordAndExistingVitals() {
        UUID missingId = UUID.randomUUID();
        UUID duplicateId = UUID.randomUUID();
        MedicalRecord record = editableRecord(UUID.randomUUID(), null);
        when(medicalRecordRepository.findById(missingId)).thenReturn(Optional.empty());
        when(medicalRecordRepository.findById(duplicateId)).thenReturn(Optional.of(record));
        when(repository.findByMedicalRecord_RecordId(duplicateId))
                .thenReturn(Optional.of(VitalSigns.builder().build()));

        assertThrows(ResourceNotFoundException.class, () -> service.create(createRequest(missingId)));
        assertThrows(ConflictException.class, () -> service.create(createRequest(duplicateId)));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsUnknownCurrentRecorder() {
        UUID recordId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(editableRecord(staffId, null)));
        when(authService.currentStaffId()).thenReturn(staffId);
        when(staffRepository.findById(staffId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(createRequest(recordId)));
    }

    @Test
    void createRejectsMissingCurrentRecorderIdentity() {
        UUID recordId = UUID.randomUUID();
        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(editableRecord(UUID.randomUUID(), null)));
        when(authService.currentStaffId()).thenReturn(null);

        assertThrows(BadRequestException.class, () -> service.create(createRequest(recordId)));
        verifyNoInteractions(staffRepository);
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        UUID id = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        MedicalRecord record = editableRecord(staffId, null);
        VitalSigns value = vitals(id, record);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(authService.currentStaffId()).thenReturn(staffId);
        when(repository.save(value)).thenReturn(value);

        var response = service.update(id, new VitalSignsUpdateRequest(
                "130/85", 80, new BigDecimal("37.2"), new BigDecimal("60.5"), new BigDecimal("170")));
        assertAll(
                () -> assertEquals("130/85", response.bloodPressure()),
                () -> assertEquals(80, response.heartRate()),
                () -> assertEquals(new BigDecimal("37.2"), response.temperature()),
                () -> assertEquals(new BigDecimal("60.5"), response.weight()),
                () -> assertEquals(new BigDecimal("170"), response.height()));

        var unchanged = service.update(id, new VitalSignsUpdateRequest(null, null, null, null, null));
        assertEquals("130/85", unchanged.bloodPressure());
    }

    @Test
    void treatingDoctorMayUpdateRecordAssignedToDepartment() {
        UUID staffId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Department department = Department.builder().departmentId(UUID.randomUUID()).build();
        MedicalRecord record = editableRecord(staffId, department);
        StaffInfo actor = staff(staffId, SystemRole.DOCTOR);
        VitalSigns value = vitals(id, record);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(authService.currentStaffId()).thenReturn(staffId);
        when(staffRepository.findById(staffId)).thenReturn(Optional.of(actor));
        when(repository.save(value)).thenReturn(value);

        assertDoesNotThrow(() -> service.update(id,
                new VitalSignsUpdateRequest("125/80", null, null, null, null)));
        verifyNoInteractions(staffDutyService);
    }

    @Test
    void onDutyNurseMayUpdateDepartmentRecord() {
        UUID doctorId = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Department department = Department.builder().departmentId(UUID.randomUUID()).build();
        MedicalRecord record = editableRecord(doctorId, department);
        StaffInfo nurse = staff(nurseId, SystemRole.NURSE);
        VitalSigns value = vitals(id, record);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(authService.currentStaffId()).thenReturn(nurseId);
        when(staffRepository.findById(nurseId)).thenReturn(Optional.of(nurse));
        when(repository.save(value)).thenReturn(value);

        service.update(id, new VitalSignsUpdateRequest(null, 75, null, null, null));

        verify(staffDutyService).requireCurrentStaffOnDuty(department, false);
    }

    @Test
    void unrelatedStaffCannotUpdateDepartmentRecord() {
        UUID doctorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Department department = Department.builder().departmentId(UUID.randomUUID()).build();
        MedicalRecord record = editableRecord(doctorId, department);
        when(repository.findById(id)).thenReturn(Optional.of(vitals(id, record)));
        when(authService.currentStaffId()).thenReturn(actorId);
        when(staffRepository.findById(actorId)).thenReturn(Optional.of(staff(actorId, SystemRole.RECEPTIONIST)));

        assertThrows(BadRequestException.class, () -> service.update(id,
                new VitalSignsUpdateRequest(null, 70, null, null, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsCompletedDetachedAndWrongDoctorRecords() {
        UUID actorId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        MedicalRecord completed = editableRecord(actorId, null);
        completed.setStatus(MedicalRecordStatus.COMPLETED);
        MedicalRecord detached = MedicalRecord.builder().status(MedicalRecordStatus.IN_PROGRESS).build();
        MedicalRecord wrongDoctor = editableRecord(UUID.randomUUID(), null);
        when(repository.findById(id1)).thenReturn(Optional.of(vitals(id1, completed)));
        when(repository.findById(id2)).thenReturn(Optional.of(vitals(id2, detached)));
        when(repository.findById(id3)).thenReturn(Optional.of(vitals(id3, wrongDoctor)));
        when(authService.currentStaffId()).thenReturn(actorId);

        assertThrows(ConflictException.class, () -> service.update(id1, emptyUpdate()));
        assertThrows(BadRequestException.class, () -> service.update(id2, emptyUpdate()));
        assertThrows(BadRequestException.class, () -> service.update(id3, emptyUpdate()));
    }

    @Test
    void updateRejectsVitalsWithoutMedicalRecord() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(VitalSigns.builder().vitalId(id).build()));

        assertThrows(BadRequestException.class, () -> service.update(id, emptyUpdate()));
        verifyNoInteractions(authService);
    }

    @Test
    void queueWithoutDepartmentFallsBackToTreatingDoctorCheck() {
        UUID id = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        MedicalRecord record = editableRecord(doctorId, null);
        record.setQueueTicket(QueueTicket.builder().department(null).build());
        VitalSigns value = vitals(id, record);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(authService.currentStaffId()).thenReturn(doctorId);
        when(repository.save(value)).thenReturn(value);

        assertDoesNotThrow(() -> service.update(id, emptyUpdate()));
        verifyNoInteractions(staffRepository, staffDutyService);
    }

    @Test
    void departmentRecordWithoutTreatingDoctorRejectsNonNurse() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Department department = Department.builder().departmentId(UUID.randomUUID()).build();
        MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID())
                .status(MedicalRecordStatus.IN_PROGRESS)
                .queueTicket(QueueTicket.builder().department(department).build())
                .doctor(null).build();
        when(repository.findById(id)).thenReturn(Optional.of(vitals(id, record)));
        when(authService.currentStaffId()).thenReturn(actorId);
        when(staffRepository.findById(actorId))
                .thenReturn(Optional.of(staff(actorId, SystemRole.RECEPTIONIST)));

        assertThrows(BadRequestException.class, () -> service.update(id, emptyUpdate()));
    }

    @Test
    void missingAuthenticationAndMissingActorAreRejected() {
        UUID noAuthId = UUID.randomUUID();
        UUID missingActorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Department department = Department.builder().departmentId(UUID.randomUUID()).build();
        MedicalRecord noAuthRecord = editableRecord(actorId, null);
        MedicalRecord departmentRecord = editableRecord(UUID.randomUUID(), department);
        when(repository.findById(noAuthId)).thenReturn(Optional.of(vitals(noAuthId, noAuthRecord)));
        when(repository.findById(missingActorId)).thenReturn(Optional.of(vitals(missingActorId, departmentRecord)));
        when(authService.currentStaffId()).thenReturn(null, actorId);
        when(staffRepository.findById(actorId)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.update(noAuthId, emptyUpdate()));
        assertThrows(ResourceNotFoundException.class, () -> service.update(missingActorId, emptyUpdate()));
    }

    @Test
    void getDeleteAndFindHandleExistingAndMissingValues() {
        UUID existingId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        VitalSigns existing = VitalSigns.builder().vitalId(existingId).bloodPressure("120/80").build();
        when(repository.findById(existingId)).thenReturn(Optional.of(existing));
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        assertEquals("120/80", service.get(existingId).bloodPressure());
        assertThrows(ConflictException.class, () -> service.delete(existingId));
        assertThrows(ResourceNotFoundException.class, () -> service.findById(missingId));
        verify(repository, never()).delete(any());
    }

    private VitalSignsCreateRequest createRequest(UUID recordId) {
        return new VitalSignsCreateRequest(recordId, "120/80", 72,
                new BigDecimal("36.8"), new BigDecimal("59.0"), new BigDecimal("168.0"), UUID.randomUUID());
    }

    private VitalSignsUpdateRequest emptyUpdate() {
        return new VitalSignsUpdateRequest(null, null, null, null, null);
    }

    private MedicalRecord editableRecord(UUID doctorId, Department department) {
        QueueTicket ticket = department == null ? null : QueueTicket.builder().department(department).build();
        return MedicalRecord.builder().recordId(UUID.randomUUID()).status(MedicalRecordStatus.IN_PROGRESS)
                .doctor(staff(doctorId, SystemRole.DOCTOR)).queueTicket(ticket).build();
    }

    private StaffInfo staff(UUID id, SystemRole role) {
        return StaffInfo.builder().staffId(id).systemRole(role).build();
    }

    private VitalSigns vitals(UUID id, MedicalRecord record) {
        return VitalSigns.builder().vitalId(id).medicalRecord(record).bloodPressure("120/80")
                .heartRate(72).temperature(new BigDecimal("36.8"))
                .weight(new BigDecimal("59.0")).height(new BigDecimal("168.0")).build();
    }
}
