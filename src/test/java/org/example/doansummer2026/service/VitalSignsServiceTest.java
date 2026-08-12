package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.vitalSigns.VitalSignsCreateRequest;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsUpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.VitalSigns;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.VitalSignsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VitalSignsServiceTest {

    @Mock
    private VitalSignsRepository repo;

    @Mock
    private MedicalRecordRepository medicalRecordRepo;

    @Mock
    private StaffInfoRepository staffRepo;

    @InjectMocks
    private VitalSignsService vitalSignsService;


    // =========================================================
    // HELPERS
    // =========================================================

    private MedicalRecord record(UUID id) {
        return MedicalRecord.builder()
                .recordId(id)
                .recordCode("MR-TEST")
                .build();
    }

    private StaffInfo staff(UUID id) {
        return StaffInfo.builder()
                .staffId(id)
                .staffCode("STF-TEST")
                .build();
    }

    private VitalSigns vitalSigns(UUID id) {
        return VitalSigns.builder()
                .vitalId(id)
                .bloodPressure("120/80")
                .heartRate(80)
                .temperature(new BigDecimal("36.5"))
                .weight(new BigDecimal("60"))
                .height(new BigDecimal("170"))
                .recordedAt(LocalDateTime.now())
                .build();
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenFound() {

        UUID id = UUID.randomUUID();

        VitalSigns vitalSigns = vitalSigns(id);

        when(repo.findById(id))
                .thenReturn(Optional.of(vitalSigns));

        assertSame(
                vitalSigns,
                vitalSignsService.findById(id)
        );
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vitalSignsService.findById(id)
        );
    }


    // =========================================================
    // GET
    // =========================================================

    @Test
    void get_ShouldReturnResponse_WhenFound() {

        UUID id = UUID.randomUUID();

        VitalSigns vitalSigns = vitalSigns(id);

        when(repo.findById(id))
                .thenReturn(Optional.of(vitalSigns));

        var result =
                vitalSignsService.get(id);

        assertNotNull(result);
    }


    // =========================================================
    // CREATE - MEDICAL RECORD MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenMedicalRecordMissing() {

        UUID recordId = UUID.randomUUID();

        VitalSignsCreateRequest req =
                mock(VitalSignsCreateRequest.class);

        when(req.medicalRecordId())
                .thenReturn(recordId);

        when(medicalRecordRepo.findById(recordId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vitalSignsService.create(req)
        );

        verifyNoInteractions(repo);
        verifyNoInteractions(staffRepo);
    }


    // =========================================================
    // CREATE - ALREADY EXISTS
    // =========================================================

    @Test
    void create_ShouldThrowConflict_WhenRecordAlreadyHasVitalSigns() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record =
                record(recordId);

        VitalSigns existing =
                vitalSigns(UUID.randomUUID());

        VitalSignsCreateRequest req =
                mock(VitalSignsCreateRequest.class);

        when(req.medicalRecordId())
                .thenReturn(recordId);

        when(medicalRecordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(repo.findByMedicalRecord_RecordId(recordId))
                .thenReturn(Optional.of(existing));

        assertThrows(
                ConflictException.class,
                () -> vitalSignsService.create(req)
        );

        verify(repo, never())
                .save(any(VitalSigns.class));

        verifyNoInteractions(staffRepo);
    }


    // =========================================================
    // CREATE - RECORDED BY MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenRecordedByDoesNotExist() {

        UUID recordId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        MedicalRecord record =
                record(recordId);

        VitalSignsCreateRequest req =
                mock(VitalSignsCreateRequest.class);

        when(req.medicalRecordId())
                .thenReturn(recordId);

        when(req.recordedById())
                .thenReturn(staffId);

        when(medicalRecordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(repo.findByMedicalRecord_RecordId(recordId))
                .thenReturn(Optional.empty());

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vitalSignsService.create(req)
        );

        verify(repo, never())
                .save(any(VitalSigns.class));
    }


    // =========================================================
    // CREATE - RECORDED BY NULL
    // =========================================================

    @Test
    void create_ShouldCreateWithoutRecordedBy_WhenRecordedByIdNull() {

        UUID recordId = UUID.randomUUID();
        UUID vitalSignsId = UUID.randomUUID();

        MedicalRecord record =
                record(recordId);

        VitalSignsCreateRequest req =
                mock(VitalSignsCreateRequest.class);

        when(req.medicalRecordId())
                .thenReturn(recordId);

        when(req.bloodPressure())
                .thenReturn("120/80");

        when(req.heartRate())
                .thenReturn(80);

        when(req.temperature())
                .thenReturn(new BigDecimal("36.7"));

        when(req.weight())
                .thenReturn(new BigDecimal("65"));

        when(req.height())
                .thenReturn(new BigDecimal("172"));

        when(medicalRecordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(repo.findByMedicalRecord_RecordId(recordId))
                .thenReturn(Optional.empty());

        when(repo.save(any(VitalSigns.class)))
                .thenAnswer(invocation -> {
                    VitalSigns v = invocation.getArgument(0);
                    v.setVitalId(vitalSignsId);
                    return v;
                });

        when(medicalRecordRepo.save(record))
                .thenReturn(record);

        var result =
                vitalSignsService.create(req);

        assertNotNull(result);

        verifyNoInteractions(staffRepo);

        ArgumentCaptor<VitalSigns> captor =
                ArgumentCaptor.forClass(VitalSigns.class);

        verify(repo)
                .save(captor.capture());

        VitalSigns saved =
                captor.getValue();

        assertSame(
                record,
                saved.getMedicalRecord()
        );

        assertEquals(
                "120/80",
                saved.getBloodPressure()
        );

        assertEquals(
                80,
                saved.getHeartRate()
        );

        assertEquals(
                new BigDecimal("36.7"),
                saved.getTemperature()
        );

        assertEquals(
                new BigDecimal("65"),
                saved.getWeight()
        );

        assertEquals(
                new BigDecimal("172"),
                saved.getHeight()
        );

        assertNull(
                saved.getRecordedBy()
        );

        assertNotNull(
                saved.getRecordedAt()
        );

        assertSame(
                saved,
                record.getVitalSigns()
        );

        verify(medicalRecordRepo)
                .save(record);
    }


    // =========================================================
    // CREATE - WITH RECORDED BY
    // =========================================================

    @Test
    void create_ShouldCreateWithRecordedBy() {

        UUID recordId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        MedicalRecord record =
                record(recordId);

        StaffInfo staff =
                staff(staffId);

        VitalSignsCreateRequest req =
                mock(VitalSignsCreateRequest.class);

        when(req.medicalRecordId())
                .thenReturn(recordId);

        when(req.recordedById())
                .thenReturn(staffId);

        when(req.bloodPressure())
                .thenReturn("130/85");

        when(req.heartRate())
                .thenReturn(90);

        when(req.temperature())
                .thenReturn(new BigDecimal("37.2"));

        when(req.weight())
                .thenReturn(new BigDecimal("70"));

        when(req.height())
                .thenReturn(new BigDecimal("175"));

        when(medicalRecordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(repo.findByMedicalRecord_RecordId(recordId))
                .thenReturn(Optional.empty());

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(repo.save(any(VitalSigns.class)))
                .thenAnswer(invocation -> {
                    VitalSigns v = invocation.getArgument(0);
                    v.setVitalId(UUID.randomUUID());
                    return v;
                });

        when(medicalRecordRepo.save(record))
                .thenReturn(record);

        var result =
                vitalSignsService.create(req);

        assertNotNull(result);

        verify(repo)
                .save(argThat(v ->
                        v.getRecordedBy() == staff
                                && v.getMedicalRecord() == record
                                && "130/85".equals(v.getBloodPressure())
                                && Integer.valueOf(90).equals(v.getHeartRate())
                ));

        verify(medicalRecordRepo)
                .save(record);
    }


    // =========================================================
    // CREATE - ALL NULL MEASUREMENTS
    // Covers entity building with null input fields
    // =========================================================

    @Test
    void create_ShouldAllowNullMeasurementFields() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .recordCode("MR-TEST")
                        .build();

        VitalSignsCreateRequest req =
                new VitalSignsCreateRequest(
                        recordId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(medicalRecordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(repo.findByMedicalRecord_RecordId(recordId))
                .thenReturn(Optional.empty());

        when(repo.save(any(VitalSigns.class)))
                .thenAnswer(invocation -> {
                    VitalSigns v = invocation.getArgument(0);
                    v.setVitalId(UUID.randomUUID());
                    return v;
                });

        when(medicalRecordRepo.save(record))
                .thenReturn(record);

        var result =
                vitalSignsService.create(req);

        assertNotNull(result);

        ArgumentCaptor<VitalSigns> captor =
                ArgumentCaptor.forClass(VitalSigns.class);

        verify(repo).save(captor.capture());

        VitalSigns saved =
                captor.getValue();

        assertNull(saved.getBloodPressure());
        assertNull(saved.getHeartRate());
        assertNull(saved.getTemperature());
        assertNull(saved.getWeight());
        assertNull(saved.getHeight());
        assertNull(saved.getRecordedBy());

        assertNotNull(saved.getRecordedAt());

        assertSame(
                record,
                saved.getMedicalRecord()
        );

        assertSame(
                saved,
                record.getVitalSigns()
        );

        verify(medicalRecordRepo)
                .save(record);
    }


    // =========================================================
    // UPDATE - ALL FIELDS
    // =========================================================

    @Test
    void update_ShouldUpdateAllFields() {

        UUID id = UUID.randomUUID();

        VitalSigns v =
                vitalSigns(id);

        VitalSignsUpdateRequest req =
                mock(VitalSignsUpdateRequest.class);

        when(req.bloodPressure())
                .thenReturn("140/90");

        when(req.heartRate())
                .thenReturn(95);

        when(req.temperature())
                .thenReturn(new BigDecimal("38.1"));

        when(req.weight())
                .thenReturn(new BigDecimal("75"));

        when(req.height())
                .thenReturn(new BigDecimal("180"));

        when(repo.findById(id))
                .thenReturn(Optional.of(v));

        when(repo.save(v))
                .thenReturn(v);

        var result =
                vitalSignsService.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                "140/90",
                v.getBloodPressure()
        );

        assertEquals(
                95,
                v.getHeartRate()
        );

        assertEquals(
                new BigDecimal("38.1"),
                v.getTemperature()
        );

        assertEquals(
                new BigDecimal("75"),
                v.getWeight()
        );

        assertEquals(
                new BigDecimal("180"),
                v.getHeight()
        );

        verify(repo)
                .save(v);
    }


    // =========================================================
    // UPDATE - EMPTY REQUEST
    // Covers all false sides of null checks
    // =========================================================

    @Test
    void update_ShouldKeepOldValues_WhenRequestEmpty() {

        UUID id = UUID.randomUUID();

        VitalSigns v = VitalSigns.builder()
                .vitalId(id)
                .bloodPressure("120/80")
                .heartRate(80)
                .temperature(new BigDecimal("36.5"))
                .weight(new BigDecimal("60"))
                .height(new BigDecimal("170"))
                .recordedAt(LocalDateTime.now())
                .build();

        VitalSignsUpdateRequest req =
                new VitalSignsUpdateRequest(
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(v));

        when(repo.save(v))
                .thenReturn(v);

        var result =
                vitalSignsService.update(id, req);

        assertNotNull(result);

        assertEquals("120/80", v.getBloodPressure());
        assertEquals(80, v.getHeartRate());
        assertEquals(new BigDecimal("36.5"), v.getTemperature());
        assertEquals(new BigDecimal("60"), v.getWeight());
        assertEquals(new BigDecimal("170"), v.getHeight());

        verify(repo).save(v);
    }


    // =========================================================
    // UPDATE INDIVIDUAL FIELD BRANCHES
    // =========================================================

    @Test
    void update_ShouldUpdateOnlyBloodPressure() {

        UUID id = UUID.randomUUID();

        VitalSigns v = VitalSigns.builder()
                .vitalId(id)
                .bloodPressure("120/80")
                .heartRate(80)
                .temperature(new BigDecimal("36.5"))
                .weight(new BigDecimal("60"))
                .height(new BigDecimal("170"))
                .recordedAt(LocalDateTime.now())
                .build();

        VitalSignsUpdateRequest req =
                new VitalSignsUpdateRequest(
                        "110/70",
                        null,
                        null,
                        null,
                        null
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(v));

        when(repo.save(v))
                .thenReturn(v);

        vitalSignsService.update(id, req);

        assertEquals("110/70", v.getBloodPressure());
        assertEquals(80, v.getHeartRate());
        assertEquals(new BigDecimal("36.5"), v.getTemperature());
        assertEquals(new BigDecimal("60"), v.getWeight());
        assertEquals(new BigDecimal("170"), v.getHeight());
    }


    @Test
    void update_ShouldUpdateOnlyHeartRate() {

        UUID id = UUID.randomUUID();

        VitalSigns v =
                vitalSigns(id);

        VitalSignsUpdateRequest req =
                mock(VitalSignsUpdateRequest.class);

        when(req.heartRate())
                .thenReturn(65);

        when(repo.findById(id))
                .thenReturn(Optional.of(v));

        when(repo.save(v))
                .thenReturn(v);

        vitalSignsService.update(id, req);

        assertEquals(
                65,
                v.getHeartRate()
        );
    }


    @Test
    void update_ShouldUpdateOnlyTemperature() {

        UUID id = UUID.randomUUID();

        VitalSigns v =
                vitalSigns(id);

        VitalSignsUpdateRequest req =
                mock(VitalSignsUpdateRequest.class);

        when(req.temperature())
                .thenReturn(new BigDecimal("39.0"));

        when(repo.findById(id))
                .thenReturn(Optional.of(v));

        when(repo.save(v))
                .thenReturn(v);

        vitalSignsService.update(id, req);

        assertEquals(
                new BigDecimal("39.0"),
                v.getTemperature()
        );
    }


    @Test
    void update_ShouldUpdateOnlyWeight() {

        UUID id = UUID.randomUUID();

        VitalSigns v =
                vitalSigns(id);

        VitalSignsUpdateRequest req =
                mock(VitalSignsUpdateRequest.class);

        when(req.weight())
                .thenReturn(new BigDecimal("80"));

        when(repo.findById(id))
                .thenReturn(Optional.of(v));

        when(repo.save(v))
                .thenReturn(v);

        vitalSignsService.update(id, req);

        assertEquals(
                new BigDecimal("80"),
                v.getWeight()
        );
    }


    @Test
    void update_ShouldUpdateOnlyHeight() {

        UUID id = UUID.randomUUID();

        VitalSigns v =
                vitalSigns(id);

        VitalSignsUpdateRequest req =
                mock(VitalSignsUpdateRequest.class);

        when(req.height())
                .thenReturn(new BigDecimal("185"));

        when(repo.findById(id))
                .thenReturn(Optional.of(v));

        when(repo.save(v))
                .thenReturn(v);

        vitalSignsService.update(id, req);

        assertEquals(
                new BigDecimal("185"),
                v.getHeight()
        );
    }


    // =========================================================
    // UPDATE - ID NOT FOUND
    // =========================================================

    @Test
    void update_ShouldThrow_WhenVitalSignsMissing() {

        UUID id = UUID.randomUUID();

        VitalSignsUpdateRequest req =
                mock(VitalSignsUpdateRequest.class);

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vitalSignsService.update(
                        id,
                        req
                )
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenMissing() {

        UUID id =
                UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> vitalSignsService.delete(id)
        );

        verify(repo, never())
                .deleteById(id);
    }


    @Test
    void delete_ShouldDelete_WhenExists() {

        UUID id =
                UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(true);

        vitalSignsService.delete(id);

        verify(repo)
                .deleteById(id);
    }
}