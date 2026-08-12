package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.appointment.AppointmentCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.VitalSigns;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.model.ShiftConfig;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.VitalSigns;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

import org.example.doansummer2026.dto.icd.ICD10SelectionCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.PrescriptionItemCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.FeedbackRequest;
import org.example.doansummer2026.model.Icd10Code;
import org.example.doansummer2026.model.PrescriptionItem;
import org.example.doansummer2026.model.Profile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;

import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.dto.appointment.AppointmentCreateRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.model.Icd10Selection;

import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CustomerVisitRepository customerVisitRepository;

    @Mock
    private StaffInfoRepository staffInfoRepository;

    @Mock
    private VitalSignsRepository vitalSignsRepository;

    @Mock
    private QueueTicketRepository queueTicketRepository;

    @Mock
    private Icd10CodeRepository icd10CodeRepository;

    @Mock
    private TestRequestRepository testRequestRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ShiftConfigRepository shiftConfigRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MedicalRecordService medicalRecordService;


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturnRecord_WhenRecordExists() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = mock(MedicalRecord.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        MedicalRecord result =
                medicalRecordService.findById(recordId);

        assertSame(record, result);

        verify(medicalRecordRepository)
                .findById(recordId);
    }


    @Test
    void findById_ShouldThrowNotFound_WhenRecordDoesNotExist() {

        UUID recordId = UUID.randomUUID();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService.findById(recordId)
        );

        verify(medicalRecordRepository)
                .findById(recordId);
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldDeleteRecord_WhenRecordExists() {

        UUID recordId = UUID.randomUUID();

        when(medicalRecordRepository.existsById(recordId))
                .thenReturn(true);

        medicalRecordService.delete(recordId);

        verify(medicalRecordRepository)
                .deleteById(recordId);
    }


    @Test
    void delete_ShouldThrowNotFound_WhenRecordDoesNotExist() {

        UUID recordId = UUID.randomUUID();

        when(medicalRecordRepository.existsById(recordId))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService.delete(recordId)
        );

        verify(medicalRecordRepository, never())
                .deleteById(recordId);
    }


    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void create_ShouldThrowNotFound_WhenVisitDoesNotExist() {

        UUID visitId = UUID.randomUUID();

        MedicalRecordCreateRequest request =
                mock(MedicalRecordCreateRequest.class);

        when(request.visitId())
                .thenReturn(visitId);

        when(customerVisitRepository.findById(visitId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService.create(request)
        );

        verify(medicalRecordRepository, never())
                .save(any(MedicalRecord.class));
    }


    @Test
    void create_ShouldThrowConflict_WhenVisitAlreadyHasIndependentRecord() {

        UUID visitId = UUID.randomUUID();

        MedicalRecordCreateRequest request =
                mock(MedicalRecordCreateRequest.class);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        MedicalRecord existingRecord =
                mock(MedicalRecord.class);

        when(request.visitId())
                .thenReturn(visitId);

        when(customerVisitRepository.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(
                medicalRecordRepository
                        .findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(
                                visitId
                        )
        ).thenReturn(Optional.of(existingRecord));

        assertThrows(
                ConflictException.class,
                () -> medicalRecordService.create(request)
        );

        verify(staffInfoRepository, never())
                .findById(any(UUID.class));

        verify(medicalRecordRepository, never())
                .save(any(MedicalRecord.class));
    }


    @Test
    void create_ShouldThrowNotFound_WhenDoctorDoesNotExist() {

        UUID visitId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        MedicalRecordCreateRequest request =
                mock(MedicalRecordCreateRequest.class);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(request.visitId())
                .thenReturn(visitId);

        when(request.doctorId())
                .thenReturn(doctorId);

        when(customerVisitRepository.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(
                medicalRecordRepository
                        .findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(
                                visitId
                        )
        ).thenReturn(Optional.empty());

        when(staffInfoRepository.findById(doctorId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService.create(request)
        );

        verify(medicalRecordRepository, never())
                .save(any(MedicalRecord.class));
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void update_ShouldThrowConflict_WhenRecordAlreadyCompleted() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record =
                mock(MedicalRecord.class);

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(record.getStatus())
                .thenReturn(MedicalRecordStatus.COMPLETED);

        assertThrows(
                ConflictException.class,
                () -> medicalRecordService.update(recordId, request)
        );

        verify(medicalRecordRepository, never())
                .save(record);
    }


    @Test
    void update_ShouldThrowConflict_WhenVersionIsDifferent() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record =
                mock(MedicalRecord.class);

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(record.getStatus())
                .thenReturn(MedicalRecordStatus.IN_PROGRESS);

        /*
         * Nếu getVersion() của MedicalRecord là Long
         * thì đoạn dưới chạy được.
         */
        when(record.getVersion())
                .thenReturn(1L);

        when(request.version())
                .thenReturn(2L);

        assertThrows(
                ConflictException.class,
                () -> medicalRecordService.update(recordId, request)
        );

        verify(medicalRecordRepository, never())
                .save(record);
    }


    // =========================================================
    // RATE
    // =========================================================

    @Test
    void rate_ShouldThrowBadRequest_WhenScoreIsBelowOne() {

        UUID recordId = UUID.randomUUID();

        assertThrows(
                BadRequestException.class,
                () -> medicalRecordService.rate(recordId, 0)
        );

        verifyNoInteractions(medicalRecordRepository);
    }


    @Test
    void rate_ShouldThrowBadRequest_WhenScoreIsGreaterThanFive() {

        UUID recordId = UUID.randomUUID();

        assertThrows(
                BadRequestException.class,
                () -> medicalRecordService.rate(recordId, 6)
        );

        verifyNoInteractions(medicalRecordRepository);
    }


    @Test
    void rate_ShouldThrowBadRequest_WhenRecordIsNotCompleted() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record =
                mock(MedicalRecord.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(record.getStatus())
                .thenReturn(MedicalRecordStatus.IN_PROGRESS);

        assertThrows(
                BadRequestException.class,
                () -> medicalRecordService.rate(recordId, 5)
        );

        verify(medicalRecordRepository, never())
                .save(record);
    }


    // =========================================================
    // GET CUSTOMER FOR RECEPTIONIST
    // =========================================================

    @Test
    void getCustomerForReceptionist_ShouldThrowNotFound_WhenProfileDoesNotExist() {

        UUID customerId = UUID.randomUUID();

        when(profileRepository.findById(customerId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService
                        .getCustomerForReceptionist(customerId)
        );
    }


    // =========================================================
    // SCHEDULE FOLLOW UP
    // =========================================================

    @Test
    void scheduleFollowUp_ShouldThrowBadRequest_WhenRecordHasNoFollowUpRequest() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record =
                mock(MedicalRecord.class);

        AppointmentCreateRequest request =
                mock(AppointmentCreateRequest.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(record.getFollowUpDate())
                .thenReturn(null);

        when(record.getFollowUpNote())
                .thenReturn(null);

        assertThrows(
                BadRequestException.class,
                () -> medicalRecordService
                        .scheduleFollowUp(recordId, request)
        );

        verify(appointmentRepository, never())
                .save(any());
    }


    @Test
    void scheduleFollowUp_ShouldThrowConflict_WhenFollowUpAlreadyScheduled() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record =
                mock(MedicalRecord.class);

        AppointmentCreateRequest request =
                mock(AppointmentCreateRequest.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        // Có yêu cầu tái khám
        when(record.getFollowUpNote())
                .thenReturn("Tai kham sau 7 ngay");

        when(record.getFollowUpAppointment())
                .thenReturn(
                        mock(org.example.doansummer2026.model.Appointment.class)
                );

        assertThrows(
                ConflictException.class,
                () -> medicalRecordService
                        .scheduleFollowUp(recordId, request)
        );

        verify(appointmentRepository, never())
                .save(any());
    }

    // =========================================================
// CREATE - SUCCESS
// =========================================================

    @Test
    void create_ShouldCreateRecordWithoutVitalSigns_WhenRequestHasNoVitals() {

        UUID visitId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        MedicalRecordCreateRequest request =
                mock(MedicalRecordCreateRequest.class);

        CustomerVisit visit = mock(CustomerVisit.class);
        StaffInfo doctor = mock(StaffInfo.class);

        when(request.visitId()).thenReturn(visitId);
        when(request.doctorId()).thenReturn(doctorId);
        when(request.chiefComplaint()).thenReturn("Dau dau");

        // Quan trọng: đảm bảo KHÔNG có vital signs
        when(request.bloodPressure()).thenReturn(null);
        when(request.heartRate()).thenReturn(null);
        when(request.temperature()).thenReturn(null);
        when(request.weight()).thenReturn(null);
        when(request.height()).thenReturn(null);

        when(customerVisitRepository.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(
                medicalRecordRepository
                        .findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(visitId)
        ).thenReturn(Optional.empty());

        when(staffInfoRepository.findById(doctorId))
                .thenReturn(Optional.of(doctor));

        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = medicalRecordService.create(request);

        assertNotNull(result);

        verify(medicalRecordRepository)
                .save(argThat(record ->
                        record.getVisit() == visit
                                && record.getDoctor() == doctor
                                && record.getStatus() == MedicalRecordStatus.IN_PROGRESS
                                && "Dau dau".equals(record.getChiefComplaint())
                                && record.getVitalSigns() == null
                ));
    }


    @Test
    void create_ShouldCreateVitalSigns_WhenVitalsAreProvided() {

        UUID visitId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID recordedById = UUID.randomUUID();

        MedicalRecordCreateRequest request =
                mock(MedicalRecordCreateRequest.class);

        CustomerVisit visit = mock(CustomerVisit.class);
        StaffInfo doctor = mock(StaffInfo.class);
        StaffInfo recordedBy = mock(StaffInfo.class);

        when(request.visitId()).thenReturn(visitId);
        when(request.doctorId()).thenReturn(doctorId);
        when(request.recordedById()).thenReturn(recordedById);

        when(request.chiefComplaint()).thenReturn("Sot");
        when(request.bloodPressure()).thenReturn("120/80");

        when(customerVisitRepository.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(
                medicalRecordRepository
                        .findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(visitId)
        ).thenReturn(Optional.empty());

        when(staffInfoRepository.findById(doctorId))
                .thenReturn(Optional.of(doctor));

        when(staffInfoRepository.findById(recordedById))
                .thenReturn(Optional.of(recordedBy));

        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        medicalRecordService.create(request);

        verify(medicalRecordRepository)
                .save(argThat(record ->
                        record.getVitalSigns() != null
                                && "120/80".equals(
                                record.getVitalSigns().getBloodPressure()
                        )
                                && record.getVitalSigns().getRecordedBy() == recordedBy
                ));
    }


    @Test
    void create_ShouldThrowNotFound_WhenRecordedByDoesNotExist() {

        UUID visitId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID recordedById = UUID.randomUUID();

        MedicalRecordCreateRequest request =
                mock(MedicalRecordCreateRequest.class);

        CustomerVisit visit = mock(CustomerVisit.class);
        StaffInfo doctor = mock(StaffInfo.class);

        when(request.visitId()).thenReturn(visitId);
        when(request.doctorId()).thenReturn(doctorId);
        when(request.recordedById()).thenReturn(recordedById);

        // chỉ cần 1 vital != null để hasVitalSigns() = true
        when(request.bloodPressure()).thenReturn("120/80");

        when(customerVisitRepository.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(
                medicalRecordRepository
                        .findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(visitId)
        ).thenReturn(Optional.empty());

        when(staffInfoRepository.findById(doctorId))
                .thenReturn(Optional.of(doctor));

        when(staffInfoRepository.findById(recordedById))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService.create(request)
        );

        verify(medicalRecordRepository, never())
                .save(any(MedicalRecord.class));
    }


// =========================================================
// UPDATE - SUCCESS
// =========================================================

    @Test
    void update_ShouldUpdateBasicFieldsAndSave() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .chiefComplaint("Cu")
                .diagnosis("Chan doan cu")
                .prescriptionItems(new LinkedHashSet<>())
                .icdSelections(new LinkedHashSet<>())
                .build();

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version()).thenReturn(record.getVersion());

        when(request.chiefComplaint())
                .thenReturn("Dau bung");

        when(request.clinicalFindings())
                .thenReturn("Dau vung thuong vi");

        when(request.diagnosis())
                .thenReturn("Viem da day");

        when(request.conclusion())
                .thenReturn("Theo doi");

        when(request.patientInstruction())
                .thenReturn("An nhe");

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        var result =
                medicalRecordService.update(recordId, request);

        assertNotNull(result);

        assertEquals(
                "Dau bung",
                record.getChiefComplaint()
        );

        assertEquals(
                "Dau vung thuong vi",
                record.getClinicalFindings()
        );

        assertEquals(
                "Viem da day",
                record.getDiagnosis()
        );

        assertEquals(
                "Theo doi",
                record.getConclusion()
        );

        assertEquals(
                "An nhe",
                record.getPatientInstruction()
        );

        verify(medicalRecordRepository).save(record);
    }


// =========================================================
// UPDATE - VITAL SIGNS
// =========================================================

    @Test
    void update_ShouldCreateVitalSigns_WhenRecordHasNone() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .prescriptionItems(new LinkedHashSet<>())
                .icdSelections(new LinkedHashSet<>())
                .build();

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version()).thenReturn(record.getVersion());

        when(request.bloodPressure())
                .thenReturn("130/90");

        when(request.heartRate())
                .thenReturn(90);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.update(recordId, request);

        assertNotNull(record.getVitalSigns());

        assertEquals(
                "130/90",
                record.getVitalSigns().getBloodPressure()
        );

        assertEquals(
                90,
                record.getVitalSigns().getHeartRate()
        );
    }


    @Test
    void update_ShouldUpdateExistingVitalSigns() {

        UUID recordId = UUID.randomUUID();

        VitalSigns vitalSigns = VitalSigns.builder()
                .bloodPressure("120/80")
                .heartRate(70)
                .build();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .vitalSigns(vitalSigns)
                .prescriptionItems(new LinkedHashSet<>())
                .icdSelections(new LinkedHashSet<>())
                .build();

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version()).thenReturn(record.getVersion());

        when(request.bloodPressure())
                .thenReturn("140/90");

        when(request.heartRate())
                .thenReturn(100);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.update(recordId, request);

        assertEquals(
                "140/90",
                vitalSigns.getBloodPressure()
        );

        assertEquals(
                100,
                vitalSigns.getHeartRate()
        );
    }

    // =========================================================
// SAVE DRAFT
// =========================================================

    @Test
    void saveDraft_ShouldThrowConflict_WhenRecordCompleted() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = mock(MedicalRecord.class);

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(record.getStatus())
                .thenReturn(MedicalRecordStatus.COMPLETED);

        assertThrows(
                ConflictException.class,
                () -> medicalRecordService.saveDraft(
                        recordId,
                        request
                )
        );

        verify(medicalRecordRepository, never())
                .save(record);
    }

    @Test
    void saveDraft_ShouldSaveAsDraft_WhenNormalDoctorUpdatesOwnRecord() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .prescriptionItems(new LinkedHashSet<>())
                .icdSelections(new LinkedHashSet<>())
                .build();

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version())
                .thenReturn(record.getVersion());

        when(request.diagnosis())
                .thenReturn("Viem hong");

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        var authentication =
                mock(org.springframework.security.core.Authentication.class);

        when(authentication.getAuthorities())
                .thenReturn(java.util.List.of());

        when(authentication.getName())
                .thenReturn("doctor01");

        org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("doctor01")
        ).thenReturn(Optional.empty());

        try {

            medicalRecordService.saveDraft(
                    recordId,
                    request
            );

            assertEquals(
                    MedicalRecordStatus.DRAFT,
                    record.getStatus()
            );

            assertEquals(
                    "Viem hong",
                    record.getDiagnosis()
            );

            verify(medicalRecordRepository)
                    .save(record);

        } finally {

            org.springframework.security.core.context.SecurityContextHolder
                    .clearContext();
        }
    }

    // =========================================================
// COMPLETE - ERROR CASES
// =========================================================

    @Test
    void complete_ShouldThrow_WhenRecordAlreadyCompleted() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.COMPLETED)
                .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        org.springframework.security.core.context.SecurityContextHolder
                .clearContext();

        assertThrows(
                BadRequestException.class,
                () -> medicalRecordService.complete(recordId)
        );
    }


    @Test
    void complete_ShouldThrow_WhenTestRequestsAreIncomplete() {

        UUID recordId = UUID.randomUUID();

        CustomerVisit visit =
                CustomerVisit.builder()
                        .visitId(UUID.randomUUID())
                        .build();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                testRequestRepository
                        .countByMedicalRecordAndStatusIn(
                                eq(recordId),
                                anyList()
                        )
        ).thenReturn(1L);

        org.springframework.security.core.context.SecurityContextHolder
                .clearContext();

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> medicalRecordService.complete(recordId)
                );

        assertTrue(
                exception.getMessage()
                        .contains("xet nghiem")
        );

        verify(medicalRecordRepository, never())
                .save(record);
    }
    @Test
    void complete_ShouldThrow_WhenThereIsPendingInvoice() {

        UUID recordId = UUID.randomUUID();

        CustomerVisit visit =
                CustomerVisit.builder()
                        .visitId(UUID.randomUUID())
                        .build();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .build();

        var invoice =
                mock(org.example.doansummer2026.model.Invoice.class);

        when(invoice.getStatus())
                .thenReturn(
                        org.example.doansummer2026.enums.InvoiceStatus.PENDING
                );

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                testRequestRepository
                        .countByMedicalRecordAndStatusIn(
                                eq(recordId),
                                anyList()
                        )
        ).thenReturn(0L);

        when(
                invoiceRepository
                        .findAllByMedicalRecord_RecordId(recordId)
        ).thenReturn(java.util.List.of(invoice));

        org.springframework.security.core.context.SecurityContextHolder
                .clearContext();

        assertThrows(
                BadRequestException.class,
                () -> medicalRecordService.complete(recordId)
        );

        verify(medicalRecordRepository, never())
                .save(record);
    }


    @Test
    void complete_ShouldThrow_WhenDiagnosisConclusionAndIcdAreEmpty() {

        UUID recordId = UUID.randomUUID();

        CustomerVisit visit =
                CustomerVisit.builder()
                        .visitId(UUID.randomUUID())
                        .build();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .icdSelections(new LinkedHashSet<>())
                        .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                testRequestRepository
                        .countByMedicalRecordAndStatusIn(
                                eq(recordId),
                                anyList()
                        )
        ).thenReturn(0L);

        when(
                invoiceRepository
                        .findAllByMedicalRecord_RecordId(recordId)
        ).thenReturn(java.util.List.of());

        org.springframework.security.core.context.SecurityContextHolder
                .clearContext();

        assertThrows(
                BadRequestException.class,
                () -> medicalRecordService.complete(recordId)
        );

        verify(medicalRecordRepository, never())
                .save(record);
    }

    @Test
    void complete_ShouldCompleteRecord_WhenAllConditionsAreValid() {

        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        CustomerVisit visit =
                CustomerVisit.builder()
                        .visitId(visitId)
                        .build();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .diagnosis("Viem hong")
                        .icdSelections(new LinkedHashSet<>())
                        .prescriptionItems(new LinkedHashSet<>())
                        .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                testRequestRepository
                        .countByMedicalRecordAndStatusIn(
                                eq(recordId),
                                anyList()
                        )
        ).thenReturn(0L);

        when(
                invoiceRepository
                        .findAllByMedicalRecord_RecordId(recordId)
        ).thenReturn(java.util.List.of());

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        when(
                queueTicketRepository
                        .findAllByVisit_VisitId(visitId)
        ).thenReturn(java.util.List.of());

        /*
         * Sau save, service fetch lại record.
         */
        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        org.springframework.security.core.context.SecurityContextHolder
                .clearContext();

        var result =
                medicalRecordService.complete(recordId);

        assertNotNull(result);

        assertEquals(
                MedicalRecordStatus.COMPLETED,
                record.getStatus()
        );

        assertNotNull(
                record.getCompletedAt()
        );

        verify(medicalRecordRepository)
                .save(record);

        verify(notificationService)
                .notifyStaffByRole(
                        eq(SystemRole.RECEPTIONIST),
                        eq("Khám bệnh hoàn tất"),
                        anyString(),
                        eq("MedicalRecord"),
                        eq(recordId)
                );
    }

    @Test
    void update_ShouldReplacePrescriptionItems_WhenValidItemsProvided() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        // Thuốc cũ
        PrescriptionItem oldItem = PrescriptionItem.builder()
                .medicineName("Thuoc cu")
                .quantity(1)
                .build();

        record.getPrescriptionItems().add(oldItem);

        PrescriptionItemCreateRequest itemRequest =
                mock(PrescriptionItemCreateRequest.class);

        when(itemRequest.medicineName()).thenReturn("Paracetamol");
        when(itemRequest.quantity()).thenReturn(10);

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version()).thenReturn(record.getVersion());
        when(request.prescriptionItems())
                .thenReturn(List.of(itemRequest));

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.update(recordId, request);

        assertEquals(1, record.getPrescriptionItems().size());

        PrescriptionItem result =
                record.getPrescriptionItems()
                        .iterator()
                        .next();

        assertEquals(
                "Paracetamol",
                result.getMedicineName()
        );

        assertEquals(
                10,
                result.getQuantity()
        );

        assertSame(
                record,
                result.getMedicalRecord()
        );
    }

    @Test
    void update_ShouldIgnorePrescriptionItem_WhenMedicineNameIsBlank() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        PrescriptionItemCreateRequest itemRequest =
                mock(PrescriptionItemCreateRequest.class);

        when(itemRequest.medicineName())
                .thenReturn("");

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version())
                .thenReturn(record.getVersion());

        when(request.prescriptionItems())
                .thenReturn(List.of(itemRequest));

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.update(recordId, request);

        assertTrue(record.getPrescriptionItems().isEmpty());

        verify(itemRequest, times(2))
                .medicineName();

        verify(itemRequest, never())
                .quantity();
    }
    @Test
    void update_ShouldAddIcdSelection_WhenCodeNameProvided() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        ICD10SelectionCreateRequest icdRequest =
                mock(ICD10SelectionCreateRequest.class);

        when(icdRequest.code())
                .thenReturn("J02.9");

        when(icdRequest.codeName())
                .thenReturn("Viem hong cap");

        when(icdRequest.note())
                .thenReturn("Theo doi");

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version())
                .thenReturn(record.getVersion());

        when(request.icdSelections())
                .thenReturn(List.of(icdRequest));

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.update(recordId, request);

        assertEquals(
                1,
                record.getIcdSelections().size()
        );

        var selection =
                record.getIcdSelections()
                        .iterator()
                        .next();

        assertEquals(
                "J02.9",
                selection.getCode()
        );

        assertEquals(
                "Viem hong cap",
                selection.getCodeName()
        );

        verifyNoInteractions(icd10CodeRepository);
    }

    @Test
    void update_ShouldLookupIcdName_WhenCodeNameIsMissing() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        ICD10SelectionCreateRequest icdRequest =
                mock(ICD10SelectionCreateRequest.class);

        when(icdRequest.code())
                .thenReturn("J02.9");

        when(icdRequest.codeName())
                .thenReturn(null);

        Icd10Code icdCode =
                mock(Icd10Code.class);

        when(icdCode.getName())
                .thenReturn("Viem hong cap");

        when(icd10CodeRepository.findById("J02.9"))
                .thenReturn(Optional.of(icdCode));

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version())
                .thenReturn(record.getVersion());

        when(request.icdSelections())
                .thenReturn(List.of(icdRequest));

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.update(recordId, request);

        var selection =
                record.getIcdSelections()
                        .iterator()
                        .next();

        assertEquals(
                "Viem hong cap",
                selection.getCodeName()
        );

        verify(icd10CodeRepository)
                .findById("J02.9");
    }

    @Test
    void rate_ShouldSaveRating_WhenRecordIsCompleted() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.COMPLETED)
                .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        var result =
                medicalRecordService.rate(recordId, 5);

        assertNotNull(result);

        assertEquals(
                5,
                record.getRatingScore()
        );

        assertNotNull(
                record.getRatedAt()
        );

        verify(medicalRecordRepository)
                .save(record);
    }
    @Test
    void submitFeedback_ShouldThrowNotFound_WhenPatientDoesNotOwnRecord() {

        UUID recordId = UUID.randomUUID();
        UUID actualProfileId = UUID.randomUUID();
        UUID otherProfileId = UUID.randomUUID();

        Profile profile = mock(Profile.class);

        when(profile.getProfileId())
                .thenReturn(actualProfileId);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getCustomer())
                .thenReturn(profile);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .status(MedicalRecordStatus.COMPLETED)
                .build();

        FeedbackRequest request =
                mock(FeedbackRequest.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService.submitFeedback(
                        recordId,
                        otherProfileId,
                        request
                )
        );

        verify(medicalRecordRepository, never())
                .save(record);
    }

    @Test
    void submitFeedback_ShouldThrow_WhenRecordIsNotCompleted() {

        UUID recordId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Profile profile = mock(Profile.class);

        when(profile.getProfileId())
                .thenReturn(profileId);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getCustomer())
                .thenReturn(profile);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        FeedbackRequest request =
                mock(FeedbackRequest.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        assertThrows(
                BadRequestException.class,
                () -> medicalRecordService.submitFeedback(
                        recordId,
                        profileId,
                        request
                )
        );

        verify(medicalRecordRepository, never())
                .save(record);
    }

    @Test
    void submitFeedback_ShouldSaveFeedback_WhenRequestIsValid() {

        UUID recordId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Profile profile = mock(Profile.class);

        when(profile.getProfileId())
                .thenReturn(profileId);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getCustomer())
                .thenReturn(profile);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .status(MedicalRecordStatus.COMPLETED)
                .build();

        FeedbackRequest request =
                mock(FeedbackRequest.class);

        when(request.overallRating())
                .thenReturn(5);

        when(request.comment())
                .thenReturn("Dich vu tot");

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        var result =
                medicalRecordService.submitFeedback(
                        recordId,
                        profileId,
                        request
                );

        assertNotNull(result);

        assertEquals(
                5,
                record.getRatingScore()
        );

        assertEquals(
                "Dich vu tot",
                record.getRatingComment()
        );

        assertEquals(
                "NEW",
                record.getFeedbackStatus()
        );

        assertFalse(
                record.getContactRequested()
        );

        assertNotNull(
                record.getRatedAt()
        );

        assertTrue(
                record.getFeedbackTargets().isEmpty()
        );

        verify(medicalRecordRepository)
                .save(record);
    }

    @Test
    void respondFeedback_ShouldSaveManagerResponse() {

        UUID recordId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .build();

        StaffInfo staff =
                mock(StaffInfo.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(staffInfoRepository.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        var result =
                medicalRecordService.respondFeedback(
                        recordId,
                        staffId,
                        "Da tiep nhan phan hoi",
                        "Can theo doi",
                        null
                );

        assertNotNull(result);

        assertEquals(
                "Da tiep nhan phan hoi",
                record.getManagerResponse()
        );

        assertEquals(
                "Can theo doi",
                record.getInternalNote()
        );

        assertEquals(
                "RESPONDED",
                record.getFeedbackStatus()
        );

        assertSame(
                staff,
                record.getRespondedBy()
        );

        assertNotNull(
                record.getRespondedAt()
        );
    }

    @Test
    void respondFeedback_ShouldSetInReview_WhenResponseIsNull() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.respondFeedback(
                recordId,
                null,
                null,
                null,
                null
        );

        assertEquals(
                "IN_REVIEW",
                record.getFeedbackStatus()
        );
    }

    @Test
    void explainFeedback_ShouldSaveExplanation_WhenDoctorOwnsRecord() {

        UUID recordId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        StaffInfo doctor =
                mock(StaffInfo.class);

        when(doctor.getStaffId())
                .thenReturn(doctorId);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .doctor(doctor)
                        .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        var result =
                medicalRecordService.explainFeedback(
                        recordId,
                        doctorId,
                        "Toi da trao doi voi benh nhan"
                );

        assertNotNull(result);

        assertEquals(
                "Toi da trao doi voi benh nhan",
                record.getDoctorExplanation()
        );

        assertEquals(
                "WAITING_INTERNAL",
                record.getFeedbackStatus()
        );

        verify(medicalRecordRepository)
                .save(record);
    }

    @Test
    void explainFeedback_ShouldThrow_WhenDoctorIsNotRelated() {

        UUID recordId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID anotherDoctorId = UUID.randomUUID();

        StaffInfo doctor =
                mock(StaffInfo.class);

        when(doctor.getStaffId())
                .thenReturn(anotherDoctorId);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .doctor(doctor)
                        .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService.explainFeedback(
                        recordId,
                        doctorId,
                        "Giai trinh"
                )
        );

        verify(medicalRecordRepository, never())
                .save(record);
    }

    @Test
    void listFeedbacks_ShouldFindAllFeedbacks_WhenDoctorIdIsNull() {

        var pageable =
                PageRequest.of(0, 10);

        when(
                medicalRecordRepository
                        .findByRatingScoreIsNotNull(pageable)
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        var result =
                medicalRecordService.listFeedbacks(
                        null,
                        pageable
                );

        assertNotNull(result);

        verify(medicalRecordRepository)
                .findByRatingScoreIsNotNull(pageable);

        verify(medicalRecordRepository, never())
                .findFeedbacksForStaff(
                        any(UUID.class),
                        any()
                );
    }

    @Test
    void listFeedbacks_ShouldFindDoctorFeedbacks_WhenDoctorIdProvided() {

        UUID doctorId = UUID.randomUUID();

        var pageable =
                PageRequest.of(0, 10);

        when(
                medicalRecordRepository
                        .findFeedbacksForStaff(
                                doctorId,
                                pageable
                        )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        var result =
                medicalRecordService.listFeedbacks(
                        doctorId,
                        pageable
                );

        assertNotNull(result);

        verify(medicalRecordRepository)
                .findFeedbacksForStaff(
                        doctorId,
                        pageable
                );
    }

    @Test
    void getPendingFollowUps_ShouldConvertNullSearchToEmptyString() {

        var pageable =
                PageRequest.of(0, 10);

        when(
                medicalRecordRepository
                        .findPendingFollowUps(
                                "",
                                pageable
                        )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        var result =
                medicalRecordService.getPendingFollowUps(
                        null,
                        pageable
                );

        assertNotNull(result);

        verify(medicalRecordRepository)
                .findPendingFollowUps(
                        "",
                        pageable
                );
    }

    @Test
    void getPendingFollowUps_ShouldTrimAndLowercaseSearch() {

        var pageable =
                PageRequest.of(0, 10);

        when(
                medicalRecordRepository
                        .findPendingFollowUps(
                                "nguyen van a",
                                pageable
                        )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        medicalRecordService.getPendingFollowUps(
                "  NGUYEN VAN A  ",
                pageable
        );

        verify(medicalRecordRepository)
                .findPendingFollowUps(
                        "nguyen van a",
                        pageable
                );
    }

    // =========================================================
// MEDICAL RECORD SERVICE - PART 4
// Search / Customer / History / Permission / Draft / Follow-up
// =========================================================


    @Test
    void search_ShouldCallRepositoryAndReturnPage() {

        UUID doctorId = UUID.randomUUID();

        var pageable = PageRequest.of(0, 10);

        when(
                medicalRecordRepository.search(
                        doctorId,
                        MedicalRecordStatus.COMPLETED,
                        null,
                        null,
                        pageable
                )
        ).thenReturn(new PageImpl<>(List.of()));

        var result = medicalRecordService.search(
                doctorId,
                MedicalRecordStatus.COMPLETED,
                null,
                null,
                pageable
        );

        assertNotNull(result);

        verify(medicalRecordRepository).search(
                doctorId,
                MedicalRecordStatus.COMPLETED,
                null,
                null,
                pageable
        );
    }


    @Test
    void searchForReceptionist_ShouldReturnEmptyPage_WhenNoRecordsFound() {

        var pageable = PageRequest.of(0, 10);

        when(
                medicalRecordRepository.findAll(
                        any(org.springframework.data.jpa.domain.Specification.class),
                        eq(pageable)
                )
        ).thenReturn(new PageImpl<>(List.of()));

        var result = medicalRecordService.searchForReceptionist(
                "nguyen",
                "Nam",
                "19-40",
                null,
                pageable
        );

        assertNotNull(result);

        verify(medicalRecordRepository).findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                eq(pageable)
        );
    }


    @Test
    void searchUniqueCustomers_ShouldReturnEmptyPage_WhenNoCustomersFound() {

        var pageable = PageRequest.of(0, 10);

        when(
                profileRepository.findAll(
                        any(org.springframework.data.jpa.domain.Specification.class),
                        eq(pageable)
                )
        ).thenReturn(new PageImpl<>(List.of()));

        var result = medicalRecordService.searchUniqueCustomers(
                "customer",
                "Nu",
                "41-60",
                null,
                pageable
        );

        assertNotNull(result);

        verify(profileRepository).findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                eq(pageable)
        );
    }


    @Test
    void getCustomerForReceptionist_ShouldReturnCustomer_WhenProfileExists() {

        UUID profileId = UUID.randomUUID();

        Profile profile = mock(Profile.class);

        when(profile.getProfileId())
                .thenReturn(profileId);

        when(profileRepository.findById(profileId))
                .thenReturn(Optional.of(profile));

        var result =
                medicalRecordService.getCustomerForReceptionist(profileId);

        assertNotNull(result);

        verify(profileRepository)
                .findById(profileId);
    }


// =========================================================
// SEARCH BY PHONE
// =========================================================

    @Test
    void searchByPhone_ShouldReturnRegisteredCustomer_WhenCustomerProfileExists() {

        String phone = "0912345678";

        Profile profile = mock(Profile.class);

        org.example.doansummer2026.model.Account account =
                mock(org.example.doansummer2026.model.Account.class);

        UUID profileId = UUID.randomUUID();

        when(profile.getProfileId())
                .thenReturn(profileId);

        when(profile.getPhone())
                .thenReturn(phone);

        when(profile.getFullName())
                .thenReturn("Nguyen Van A");

        when(profile.getPatientCode())
                .thenReturn("BN001");

        when(profile.getAccount())
                .thenReturn(account);

        when(account.getRole())
                .thenReturn(Role.CUSTOMER);

        when(profileRepository.findFirstByPhone(phone))
                .thenReturn(Optional.of(profile));

        var result =
                medicalRecordService.searchByPhone(phone);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(profileRepository)
                .findFirstByPhone(phone);

        // Vì đã tìm được Profile nên không cần tìm appointment guest
        verify(appointmentRepository, never())
                .findGuestAppointmentsByPhone(phone);
    }


    @Test
    void searchByPhone_ShouldReturnGuest_WhenProfileHasNoAccount() {

        String phone = "0987654321";

        Profile profile = mock(Profile.class);

        when(profile.getProfileId())
                .thenReturn(UUID.randomUUID());

        when(profile.getPhone())
                .thenReturn(phone);

        when(profile.getFullName())
                .thenReturn("Khach Vang Lai");

        when(profile.getPatientCode())
                .thenReturn("GUEST001");

        when(profile.getAccount())
                .thenReturn(null);

        when(profileRepository.findFirstByPhone(phone))
                .thenReturn(Optional.of(profile));

        var result =
                medicalRecordService.searchByPhone(phone);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(appointmentRepository, never())
                .findGuestAppointmentsByPhone(phone);
    }


    @Test
    void searchByPhone_ShouldIgnoreStaffProfile() {

        String phone = "0900000000";

        Profile profile = mock(Profile.class);

        org.example.doansummer2026.model.Account account =
                mock(org.example.doansummer2026.model.Account.class);

        when(profile.getAccount())
                .thenReturn(account);

        when(account.getRole())
                .thenReturn(Role.STAFF);

        when(profileRepository.findFirstByPhone(phone))
                .thenReturn(Optional.of(profile));

        when(appointmentRepository.findGuestAppointmentsByPhone(phone))
                .thenReturn(List.of());

        var result =
                medicalRecordService.searchByPhone(phone);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(appointmentRepository)
                .findGuestAppointmentsByPhone(phone);
    }


    @Test
    void searchByPhone_ShouldSearchGuestAppointments_WhenProfileDoesNotExist() {

        String phone = "0933333333";

        Appointment appointment =
                mock(Appointment.class);

        when(appointment.getGuestPhone())
                .thenReturn(phone);

        when(appointment.getGuestFullName())
                .thenReturn("Guest A");

        when(profileRepository.findFirstByPhone(phone))
                .thenReturn(Optional.empty());

        when(appointmentRepository.findGuestAppointmentsByPhone(phone))
                .thenReturn(List.of(appointment));

        var result =
                medicalRecordService.searchByPhone(phone);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(appointmentRepository)
                .findGuestAppointmentsByPhone(phone);
    }


    @Test
    void searchByPhone_ShouldRemoveDuplicateGuestAppointments() {

        String phone = "0944444444";

        Appointment appointment1 =
                mock(Appointment.class);

        Appointment appointment2 =
                mock(Appointment.class);

        when(appointment1.getGuestPhone())
                .thenReturn(phone);

        when(appointment1.getGuestFullName())
                .thenReturn("Guest Duplicate");

        when(appointment2.getGuestPhone())
                .thenReturn(phone);

        when(appointment2.getGuestFullName())
                .thenReturn("Guest Duplicate");

        when(profileRepository.findFirstByPhone(phone))
                .thenReturn(Optional.empty());

        when(appointmentRepository.findGuestAppointmentsByPhone(phone))
                .thenReturn(List.of(
                        appointment1,
                        appointment2
                ));

        var result =
                medicalRecordService.searchByPhone(phone);

        assertEquals(1, result.size());
    }


// =========================================================
// MEDICAL HISTORY
// =========================================================

    @Test
    void getMedicalHistoryForPatient_ShouldReturnEmptyPage() {

        UUID profileId = UUID.randomUUID();

        var pageable =
                PageRequest.of(0, 10);

        when(
                medicalRecordRepository.findAll(
                        any(org.springframework.data.jpa.domain.Specification.class),
                        eq(pageable)
                )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        var result =
                medicalRecordService.getMedicalHistoryForPatient(
                        profileId,
                        "viem",
                        pageable
                );

        assertNotNull(result);

        verify(medicalRecordRepository).findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                eq(pageable)
        );
    }


// =========================================================
// VISIT DETAIL
// =========================================================

    @Test
    void getVisitDetail_ShouldThrowNotFound_WhenRecordDoesNotExist() {

        UUID visitId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(
                medicalRecordRepository
                        .findFirstByVisit_VisitIdOrderByCreatedAtDesc(visitId)
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService.getVisitDetail(
                        visitId,
                        profileId
                )
        );
    }


    @Test
    void getVisitDetail_ShouldThrowNotFound_WhenVisitIsNull() {

        UUID visitId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(UUID.randomUUID())
                        .visit(null)
                        .build();

        when(
                medicalRecordRepository
                        .findFirstByVisit_VisitIdOrderByCreatedAtDesc(visitId)
        ).thenReturn(Optional.of(record));

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService.getVisitDetail(
                        visitId,
                        profileId
                )
        );
    }


    @Test
    void getVisitDetail_ShouldThrowNotFound_WhenProfileDoesNotOwnVisit() {

        UUID visitId = UUID.randomUUID();

        UUID ownerId = UUID.randomUUID();
        UUID anotherProfileId = UUID.randomUUID();

        Profile customer =
                mock(Profile.class);

        when(customer.getProfileId())
                .thenReturn(ownerId);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getCustomer())
                .thenReturn(customer);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(UUID.randomUUID())
                        .visit(visit)
                        .build();

        when(
                medicalRecordRepository
                        .findFirstByVisit_VisitIdOrderByCreatedAtDesc(visitId)
        ).thenReturn(Optional.of(record));

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService.getVisitDetail(
                        visitId,
                        anotherProfileId
                )
        );
    }


// =========================================================
// VISIT DETAIL BY RECORD ID
// =========================================================

    @Test
    void getVisitDetailByRecordId_ShouldThrowNotFound_WhenRecordDoesNotExist() {

        UUID recordId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService
                        .getVisitDetailByRecordId(
                                recordId,
                                profileId
                        )
        );
    }


    @Test
    void getVisitDetailByRecordId_ShouldThrow_WhenProfileDoesNotOwnRecord() {

        UUID recordId = UUID.randomUUID();

        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        Profile customer =
                mock(Profile.class);

        when(customer.getProfileId())
                .thenReturn(ownerId);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getCustomer())
                .thenReturn(customer);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService
                        .getVisitDetailByRecordId(
                                recordId,
                                otherId
                        )
        );
    }


// =========================================================
// SAVE DRAFT - NURSE
// =========================================================

    @Test
    void saveDraft_ShouldUpdateNursingFields_WhenUserIsNurse() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .build();

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version())
                .thenReturn(record.getVersion());

        when(request.chiefComplaint())
                .thenReturn("Dau dau");

        when(request.clinicalFindings())
                .thenReturn("Sot nhe");

        when(request.bloodPressure())
                .thenReturn("120/80");

        StaffInfo nurse =
                mock(StaffInfo.class);

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "nurse01",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_NURSE")
                        )
                );

        org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("nurse01")
        ).thenReturn(Optional.of(nurse));

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        try {

            var result =
                    medicalRecordService.saveDraft(
                            recordId,
                            request
                    );

            assertNotNull(result);

            assertEquals(
                    MedicalRecordStatus.DRAFT,
                    record.getStatus()
            );

            assertEquals(
                    "Dau dau",
                    record.getChiefComplaint()
            );

            assertEquals(
                    "Sot nhe",
                    record.getClinicalFindings()
            );

            assertNotNull(
                    record.getVitalSigns()
            );

            assertEquals(
                    "120/80",
                    record.getVitalSigns().getBloodPressure()
            );

            assertSame(
                    nurse,
                    record.getNursingUpdatedBy()
            );

            assertNotNull(
                    record.getNursingUpdatedAt()
            );

            verify(medicalRecordRepository)
                    .save(record);

        } finally {

            org.springframework.security.core.context.SecurityContextHolder
                    .clearContext();
        }
    }


// =========================================================
// SAVE DRAFT - DOCTOR OTHER RECORD
// =========================================================

    @Test
    void saveDraft_ShouldRejectDoctor_WhenRecordBelongsToAnotherDoctor() {

        UUID recordId = UUID.randomUUID();

        UUID ownerDoctorId =
                UUID.randomUUID();

        UUID currentDoctorId =
                UUID.randomUUID();

        StaffInfo ownerDoctor =
                mock(StaffInfo.class);

        StaffInfo currentDoctor =
                mock(StaffInfo.class);

        when(ownerDoctor.getStaffId())
                .thenReturn(ownerDoctorId);

        when(currentDoctor.getStaffId())
                .thenReturn(currentDoctorId);

        /*
         * getSystemRole().isDoctor() phải = true.
         * Dùng một role bác sĩ thực tế trong enum của bạn.
         */
        when(currentDoctor.getSystemRole())
                .thenReturn(SystemRole.DOCTOR);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .doctor(ownerDoctor)
                        .build();

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version())
                .thenReturn(record.getVersion());

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "doctor02",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_DOCTOR")
                        )
                );

        org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("doctor02")
        ).thenReturn(Optional.of(currentDoctor));

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        try {

            assertThrows(
                    BadRequestException.class,
                    () -> medicalRecordService.saveDraft(
                            recordId,
                            request
                    )
            );

            verify(medicalRecordRepository, never())
                    .save(record);

        } finally {

            org.springframework.security.core.context.SecurityContextHolder
                    .clearContext();
        }
    }


// =========================================================
// COMPLETE - WRONG DOCTOR
// =========================================================

    @Test
    void complete_ShouldRejectDoctor_WhenAnotherDoctorOwnsRecord() {

        UUID recordId = UUID.randomUUID();

        UUID ownerDoctorId =
                UUID.randomUUID();

        UUID actorDoctorId =
                UUID.randomUUID();

        StaffInfo ownerDoctor =
                mock(StaffInfo.class);

        StaffInfo actor =
                mock(StaffInfo.class);

        when(ownerDoctor.getStaffId())
                .thenReturn(ownerDoctorId);

        when(actor.getStaffId())
                .thenReturn(actorDoctorId);

        when(actor.getSystemRole())
                .thenReturn(SystemRole.DOCTOR);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .doctor(ownerDoctor)
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "doctor02",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_DOCTOR")
                        )
                );

        org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("doctor02")
        ).thenReturn(Optional.of(actor));

        try {

            assertThrows(
                    BadRequestException.class,
                    () -> medicalRecordService.complete(recordId)
            );

            verify(medicalRecordRepository, never())
                    .save(record);

        } finally {

            org.springframework.security.core.context.SecurityContextHolder
                    .clearContext();
        }
    }


// =========================================================
// SCHEDULE FOLLOW UP - SUCCESS WITH CUSTOMER
// =========================================================

    @Test
    void scheduleFollowUp_ShouldCreateAppointment_ForRegisteredCustomer() {

        UUID recordId = UUID.randomUUID();

        Profile customer = mock(Profile.class);
        CustomerVisit visit = mock(CustomerVisit.class);

        when(visit.getCustomer())
                .thenReturn(customer);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .followUpNote("Tai kham sau 7 ngay")
                        .build();

        AppointmentCreateRequest request =
                mock(AppointmentCreateRequest.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        var result =
                medicalRecordService.scheduleFollowUp(
                        recordId,
                        request
                );

        assertNotNull(result);

        assertNotNull(record.getFollowUpAppointment());

        assertSame(
                customer,
                record.getFollowUpAppointment().getCustomer()
        );

        verify(appointmentRepository)
                .save(any(Appointment.class));

        verify(medicalRecordRepository)
                .save(record);
    }


// =========================================================
// SCHEDULE FOLLOW UP - GUEST
// =========================================================

    @Test
    void scheduleFollowUp_ShouldCopyGuestInformation_FromOldAppointment() {

        UUID recordId =
                UUID.randomUUID();

        Appointment oldAppointment =
                mock(Appointment.class);

        when(oldAppointment.getIsGuest())
                .thenReturn(true);

        when(oldAppointment.getGuestFullName())
                .thenReturn("Nguyen Van Guest");

        when(oldAppointment.getGuestPhone())
                .thenReturn("0911111111");

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getCustomer())
                .thenReturn(null);

        when(visit.getAppointment())
                .thenReturn(oldAppointment);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .followUpNote("Tai kham")
                        .build();

        AppointmentCreateRequest request =
                mock(AppointmentCreateRequest.class);


        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.scheduleFollowUp(
                recordId,
                request
        );

        Appointment newAppointment =
                record.getFollowUpAppointment();

        assertNotNull(newAppointment);

        assertTrue(
                newAppointment.getIsGuest()
        );

        assertEquals(
                "Nguyen Van Guest",
                newAppointment.getGuestFullName()
        );

        assertEquals(
                "0911111111",
                newAppointment.getGuestPhone()
        );
    }


// =========================================================
// SCHEDULE FOLLOW UP - UNKNOWN GUEST
// =========================================================

    @Test
    void scheduleFollowUp_ShouldCreateGenericGuest_WhenNoCustomerAndNoGuestAppointment() {

        UUID recordId =
                UUID.randomUUID();

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getCustomer())
                .thenReturn(null);

        when(visit.getAppointment())
                .thenReturn(null);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .followUpDate(LocalDate.now().plusDays(7))
                        .build();

        AppointmentCreateRequest request =
                mock(AppointmentCreateRequest.class);



        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.scheduleFollowUp(
                recordId,
                request
        );

        Appointment appointment =
                record.getFollowUpAppointment();

        assertNotNull(appointment);

        assertTrue(
                appointment.getIsGuest()
        );

        assertEquals(
                "Khách vãng lai",
                appointment.getGuestFullName()
        );
    }
    // =========================================================
// MEDICAL RECORD SERVICE - PART 5
// BRANCH COVERAGE BOOST
// =========================================================


// =========================================================
// SAVE DRAFT - NURSE SAI DEPARTMENT
// =========================================================

    @Test
    void saveDraft_ShouldRejectNurse_WhenAssignedToDifferentDepartment() {

        UUID recordId = UUID.randomUUID();

        Department nurseDepartment = mock(Department.class);
        Department recordDepartment = mock(Department.class);

        UUID nurseDepartmentId = UUID.randomUUID();
        UUID recordDepartmentId = UUID.randomUUID();

        when(nurseDepartment.getDepartmentId())
                .thenReturn(nurseDepartmentId);

        when(recordDepartment.getDepartmentId())
                .thenReturn(recordDepartmentId);

        StaffInfo nurse = mock(StaffInfo.class);

        when(nurse.getDepartment())
                .thenReturn(nurseDepartment);

        QueueTicket queueTicket = mock(QueueTicket.class);

        when(queueTicket.getDepartment())
                .thenReturn(recordDepartment);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .queueTicket(queueTicket)
                .build();

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version())
                .thenReturn(record.getVersion());

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "nurse01",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_NURSE")
                        )
                );

        org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("nurse01")
        ).thenReturn(Optional.of(nurse));

        try {

            assertThrows(
                    BadRequestException.class,
                    () -> medicalRecordService.saveDraft(
                            recordId,
                            request
                    )
            );

            verify(medicalRecordRepository, never())
                    .save(record);

        } finally {

            org.springframework.security.core.context.SecurityContextHolder
                    .clearContext();
        }
    }


// =========================================================
// SAVE DRAFT - NURSE KHÔNG CÓ DEPARTMENT
// =========================================================

    @Test
    void saveDraft_ShouldRejectNurse_WhenNurseHasNoDepartment() {

        UUID recordId = UUID.randomUUID();

        StaffInfo nurse = mock(StaffInfo.class);

        QueueTicket queueTicket = mock(QueueTicket.class);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .queueTicket(queueTicket)
                .build();

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version())
                .thenReturn(record.getVersion());

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "nurse02",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_NURSE")
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("nurse02")
        ).thenReturn(Optional.of(nurse));

        try {

            assertThrows(
                    BadRequestException.class,
                    () -> medicalRecordService.saveDraft(
                            recordId,
                            request
                    )
            );

            verify(medicalRecordRepository, never())
                    .save(record);

        } finally {

            SecurityContextHolder.clearContext();
        }
    }


// =========================================================
// SAVE DRAFT - NURSE ĐÚNG DEPARTMENT
// + UPDATE VITAL SIGNS ĐÃ TỒN TẠI
// =========================================================

    @Test
    void saveDraft_ShouldAllowNurse_WhenDepartmentMatches_AndUpdateExistingVitals() {

        UUID recordId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        Department department = mock(Department.class);

        when(department.getDepartmentId())
                .thenReturn(departmentId);

        StaffInfo nurse = mock(StaffInfo.class);

        when(nurse.getDepartment())
                .thenReturn(department);

        QueueTicket queueTicket = mock(QueueTicket.class);

        when(queueTicket.getDepartment())
                .thenReturn(department);

        VitalSigns vitals = VitalSigns.builder()
                .bloodPressure("110/70")
                .heartRate(70)
                .build();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .queueTicket(queueTicket)
                .vitalSigns(vitals)
                .build();

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version())
                .thenReturn(record.getVersion());

        when(request.bloodPressure())
                .thenReturn("130/90");

        when(request.heartRate())
                .thenReturn(95);

        when(request.chiefComplaint())
                .thenReturn("Met moi");

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "nurse03",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_NURSE")
                        )
                );

        org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("nurse03")
        ).thenReturn(Optional.of(nurse));

        try {

            var result =
                    medicalRecordService.saveDraft(
                            recordId,
                            request
                    );

            assertNotNull(result);

            assertEquals(
                    MedicalRecordStatus.DRAFT,
                    record.getStatus()
            );

            assertEquals(
                    "Met moi",
                    record.getChiefComplaint()
            );

            assertEquals(
                    "130/90",
                    vitals.getBloodPressure()
            );

            assertEquals(
                    95,
                    vitals.getHeartRate()
            );

            assertSame(
                    nurse,
                    record.getNursingUpdatedBy()
            );

            assertNotNull(
                    record.getNursingUpdatedAt()
            );

            verify(medicalRecordRepository)
                    .save(record);

        } finally {

            org.springframework.security.core.context.SecurityContextHolder
                    .clearContext();
        }
    }


// =========================================================
// COMPLETE - DOCTOR ĐÚNG NGƯỜI PHỤ TRÁCH
// + QUEUE IN_PROGRESS -> DONE
// =========================================================

    @Test
    void complete_ShouldSetQueueDone_WhenDoctorCompletesOwnRecord() {

        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        StaffInfo doctor = mock(StaffInfo.class);

        when(doctor.getStaffId())
                .thenReturn(doctorId);

        when(doctor.getSystemRole())
                .thenReturn(SystemRole.DOCTOR);

        Profile customer = mock(Profile.class);

        when(customer.getFullName())
                .thenReturn("Nguyen Van A");

        Appointment appointment = mock(Appointment.class);

        when(appointment.getCustomer())
                .thenReturn(customer);

        CustomerVisit visit = mock(CustomerVisit.class);

        when(visit.getVisitId())
                .thenReturn(visitId);

        when(visit.getAppointment())
                .thenReturn(appointment);

        QueueTicket ticket = mock(QueueTicket.class);

        when(ticket.getStatus())
                .thenReturn(QueueStatus.IN_PROGRESS);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .diagnosis("Viem hong")
                .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                testRequestRepository
                        .countByMedicalRecordAndStatusIn(
                                eq(recordId),
                                anyList()
                        )
        ).thenReturn(0L);

        when(
                invoiceRepository
                        .findAllByMedicalRecord_RecordId(recordId)
        ).thenReturn(List.of());

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        when(
                queueTicketRepository
                        .findAllByVisit_VisitId(visitId)
        ).thenReturn(List.of(ticket));

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "doctor01",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_DOCTOR")
                        )
                );

        org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("doctor01")
        ).thenReturn(Optional.of(doctor));

        try {

            var result =
                    medicalRecordService.complete(recordId);

            assertNotNull(result);

            assertEquals(
                    MedicalRecordStatus.COMPLETED,
                    record.getStatus()
            );

            assertNotNull(
                    record.getCompletedAt()
            );

            assertSame(
                    doctor,
                    record.getDoctorConfirmedBy()
            );

            assertNotNull(
                    record.getDoctorConfirmedAt()
            );

            verify(ticket)
                    .setStatus(QueueStatus.DONE);

            verify(ticket)
                    .setCompletedAt(any(LocalDateTime.class));

            verify(queueTicketRepository)
                    .save(ticket);

            verify(notificationService)
                    .notifyStaffByRole(
                            eq(SystemRole.RECEPTIONIST),
                            eq("Khám bệnh hoàn tất"),
                            anyString(),
                            eq("MedicalRecord"),
                            eq(recordId)
                    );

        } finally {

            org.springframework.security.core.context.SecurityContextHolder
                    .clearContext();
        }
    }


// =========================================================
// COMPLETE - QUEUE KHÔNG PHẢI IN_PROGRESS
// =========================================================

    @Test
    void complete_ShouldNotUpdateQueue_WhenNoInProgressTicketExists() {

        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        when(visit.getVisitId())
                .thenReturn(visitId);

        QueueTicket ticket = mock(QueueTicket.class);

        when(ticket.getStatus())
                .thenReturn(QueueStatus.DONE);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .diagnosis("Cam")
                .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                testRequestRepository
                        .countByMedicalRecordAndStatusIn(
                                eq(recordId),
                                anyList()
                        )
        ).thenReturn(0L);

        when(
                invoiceRepository
                        .findAllByMedicalRecord_RecordId(recordId)
        ).thenReturn(List.of());

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        when(
                queueTicketRepository
                        .findAllByVisit_VisitId(visitId)
        ).thenReturn(List.of(ticket));

        org.springframework.security.core.context.SecurityContextHolder
                .clearContext();

        medicalRecordService.complete(recordId);

        verify(queueTicketRepository, never())
                .save(ticket);
    }


// =========================================================
// COMPLETE - INVOICE CÓ NHƯNG ĐÃ THANH TOÁN
// =========================================================

    @Test
    void complete_ShouldAllowCompletion_WhenInvoiceIsNotPending() {

        UUID recordId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        var invoice =
                mock(org.example.doansummer2026.model.Invoice.class);

        /*
         * Dùng trạng thái khác PENDING trong enum của bạn.
         * Nếu enum của bạn dùng PAID thì giữ nguyên.
         */
        when(invoice.getStatus())
                .thenReturn(InvoiceStatus.PAID);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .conclusion("Suc khoe on dinh")
                .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                testRequestRepository
                        .countByMedicalRecordAndStatusIn(
                                eq(recordId),
                                anyList()
                        )
        ).thenReturn(0L);

        when(
                invoiceRepository
                        .findAllByMedicalRecord_RecordId(recordId)
        ).thenReturn(List.of(invoice));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        org.springframework.security.core.context.SecurityContextHolder
                .clearContext();

        var result =
                medicalRecordService.complete(recordId);

        assertNotNull(result);

        assertEquals(
                MedicalRecordStatus.COMPLETED,
                record.getStatus()
        );
    }


// =========================================================
// COMPLETE - KHÔNG CÓ VISIT
// checkTestRequestsCompletion() -> false
// =========================================================

    @Test
    void complete_ShouldAllowRecordWithoutVisit_WhenDiagnosisExists() {

        UUID recordId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .diagnosis("Chan doan test")
                .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        org.springframework.security.core.context.SecurityContextHolder
                .clearContext();

        var result =
                medicalRecordService.complete(recordId);

        assertNotNull(result);

        assertEquals(
                MedicalRecordStatus.COMPLETED,
                record.getStatus()
        );

        /*
         * visit == null => service không cần query test request.
         */
        verifyNoInteractions(testRequestRepository);

        /*
         * recordId != null nên invoice vẫn được check.
         */
        verify(invoiceRepository)
                .findAllByMedicalRecord_RecordId(recordId);
    }


// =========================================================
// COMPLETE - REQUEST != NULL
// updateMedicalRecordFields trước khi complete
// =========================================================

    @Test
    void complete_ShouldApplyUpdateRequestBeforeCompleting() {

        UUID recordId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version())
                .thenReturn(record.getVersion());

        when(request.diagnosis())
                .thenReturn("Viem amidan");

        when(request.conclusion())
                .thenReturn("Dieu tri ngoai tru");

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                testRequestRepository
                        .countByMedicalRecordAndStatusIn(
                                eq(recordId),
                                anyList()
                        )
        ).thenReturn(0L);

        when(
                invoiceRepository
                        .findAllByMedicalRecord_RecordId(recordId)
        ).thenReturn(List.of());

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        org.springframework.security.core.context.SecurityContextHolder
                .clearContext();

        var result =
                medicalRecordService.complete(
                        recordId,
                        request
                );

        assertNotNull(result);

        assertEquals(
                "Viem amidan",
                record.getDiagnosis()
        );

        assertEquals(
                "Dieu tri ngoai tru",
                record.getConclusion()
        );

        assertEquals(
                MedicalRecordStatus.COMPLETED,
                record.getStatus()
        );
    }


// =========================================================
// SCHEDULE FOLLOW UP - SHIFT KHÔNG TỒN TẠI
// =========================================================

    @Test
    void scheduleFollowUp_ShouldThrowNotFound_WhenShiftDoesNotExist() {

        UUID recordId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .followUpNote("Tai kham")
                .visit(mock(CustomerVisit.class))
                .build();

        AppointmentCreateRequest request =
                mock(AppointmentCreateRequest.class);

        when(request.shiftId())
                .thenReturn(shiftId);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(shiftConfigRepository.findById(shiftId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalRecordService.scheduleFollowUp(
                        recordId,
                        request
                )
        );

        verify(appointmentRepository, never())
                .save(any(Appointment.class));
    }


// =========================================================
// SCHEDULE FOLLOW UP - CÓ SHIFT
// =========================================================

    @Test
    void scheduleFollowUp_ShouldUseShiftInformation_WhenShiftExists() {

        UUID recordId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        Profile customer = mock(Profile.class);

        CustomerVisit visit = mock(CustomerVisit.class);

        when(visit.getCustomer())
                .thenReturn(customer);

        ShiftConfig shift =
                mock(ShiftConfig.class);

        when(shift.getName())
                .thenReturn("Ca sang");

        when(shift.getStartTime())
                .thenReturn(String.valueOf(LocalTime.of(8, 0)));

        when(shift.getEndTime())
                .thenReturn(String.valueOf(LocalTime.of(11, 30)));

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .followUpDate(LocalDate.now().plusDays(10))
                .build();

        AppointmentCreateRequest request =
                mock(AppointmentCreateRequest.class);

        when(request.shiftId())
                .thenReturn(shiftId);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(shiftConfigRepository.findById(shiftId))
                .thenReturn(Optional.of(shift));

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(
                        invocation -> invocation.getArgument(0)
                );

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.scheduleFollowUp(
                recordId,
                request
        );

        Appointment appointment =
                record.getFollowUpAppointment();

        assertNotNull(appointment);

        assertEquals(
                "Ca sang",
                appointment.getShiftName()
        );

        assertEquals(
                "08:00 - 11:30",
                appointment.getShiftTime()
        );

        assertSame(
                customer,
                appointment.getCustomer()
        );
    }


// =========================================================
// SCHEDULE FOLLOW UP
// OLD APPOINTMENT CÓ NHƯNG KHÔNG PHẢI GUEST
// => FALLBACK "Khách vãng lai"
// =========================================================

    @Test
    void scheduleFollowUp_ShouldCreateGenericGuest_WhenOldAppointmentIsNotGuest() {

        UUID recordId = UUID.randomUUID();

        Appointment oldAppointment =
                mock(Appointment.class);

        when(oldAppointment.getIsGuest())
                .thenReturn(false);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getAppointment())
                .thenReturn(oldAppointment);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .followUpNote("Tai kham")
                        .build();

        AppointmentCreateRequest request =
                mock(AppointmentCreateRequest.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(
                        invocation -> invocation.getArgument(0)
                );

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.scheduleFollowUp(
                recordId,
                request
        );

        Appointment created =
                record.getFollowUpAppointment();

        assertNotNull(created);

        assertTrue(
                created.getIsGuest()
        );

        assertEquals(
                "Khách vãng lai",
                created.getGuestFullName()
        );
    }


// =========================================================
// SCHEDULE FOLLOW UP
// QUEUE TICKET CÓ MEDICAL SERVICE
// =========================================================

    @Test
    void scheduleFollowUp_ShouldCopyServiceFromQueueTicket() {

        UUID recordId = UUID.randomUUID();

        Profile customer = mock(Profile.class);

        CustomerVisit visit = mock(CustomerVisit.class);

        when(visit.getCustomer())
                .thenReturn(customer);

        QueueTicket queueTicket =
                mock(QueueTicket.class);

        var medicalService =
                mock(org.example.doansummer2026.model.MedicalService.class);

        when(queueTicket.getService())
                .thenReturn(medicalService);

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .visit(visit)
                        .queueTicket(queueTicket)
                        .followUpNote("Tai kham sau dieu tri")
                        .build();

        AppointmentCreateRequest request =
                mock(AppointmentCreateRequest.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(
                        invocation -> invocation.getArgument(0)
                );

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.scheduleFollowUp(
                recordId,
                request
        );

        Appointment appointment =
                record.getFollowUpAppointment();

        assertNotNull(appointment);

        assertTrue(
                appointment.getServices()
                        .contains(medicalService)
        );
    }
    // =========================================================
// MEDICAL RECORD SERVICE - PART 6
// MORE BRANCH COVERAGE
// =========================================================


// =========================================================
// GET VISIT DETAIL - SUCCESS
// =========================================================

    @Test
    void getVisitDetail_ShouldReturnDetail_WhenProfileOwnsVisit() {

        UUID visitId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Profile customer = mock(Profile.class);

        when(customer.getProfileId())
                .thenReturn(profileId);

        CustomerVisit visit = mock(CustomerVisit.class);

        when(visit.getVisitId())
                .thenReturn(visitId);

        when(visit.getCustomer())
                .thenReturn(customer);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .visit(visit)
                .status(MedicalRecordStatus.COMPLETED)
                .build();

        when(
                medicalRecordRepository
                        .findFirstByVisit_VisitIdOrderByCreatedAtDesc(visitId)
        ).thenReturn(Optional.of(record));

        when(
                medicalRecordRepository
                        .findAllByVisit_VisitIdOrderByCreatedAtAsc(visitId)
        ).thenReturn(List.of(record));

        var result =
                medicalRecordService.getVisitDetail(
                        visitId,
                        profileId
                );

        assertNotNull(result);

        verify(medicalRecordRepository)
                .findAllByVisit_VisitIdOrderByCreatedAtAsc(visitId);
    }


// =========================================================
// GET VISIT DETAIL BY RECORD ID - SUCCESS
// =========================================================

    @Test
    void getVisitDetailByRecordId_ShouldReturnDetail_WhenProfileOwnsRecord() {

        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Profile customer = mock(Profile.class);

        when(customer.getProfileId())
                .thenReturn(profileId);

        CustomerVisit visit = mock(CustomerVisit.class);

        when(visit.getVisitId())
                .thenReturn(visitId);

        when(visit.getCustomer())
                .thenReturn(customer);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .status(MedicalRecordStatus.COMPLETED)
                .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                medicalRecordRepository
                        .findAllByVisit_VisitIdOrderByCreatedAtAsc(visitId)
        ).thenReturn(List.of(record));

        var result =
                medicalRecordService.getVisitDetailByRecordId(
                        recordId,
                        profileId
                );

        assertNotNull(result);

        verify(medicalRecordRepository)
                .findAllByVisit_VisitIdOrderByCreatedAtAsc(visitId);
    }


// =========================================================
// SAVE DRAFT - DOCTOR ĐÚNG NGƯỜI PHỤ TRÁCH
// =========================================================

    @Test
    void saveDraft_ShouldAllowDoctor_WhenDoctorOwnsRecord() {

        UUID recordId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        StaffInfo doctor = mock(StaffInfo.class);

        when(doctor.getStaffId())
                .thenReturn(doctorId);

        when(doctor.getSystemRole())
                .thenReturn(SystemRole.DOCTOR);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .doctor(doctor)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version())
                .thenReturn(record.getVersion());

        when(request.diagnosis())
                .thenReturn("Viem phe quan");

        when(request.conclusion())
                .thenReturn("Theo doi va dieu tri");

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "doctor01",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_DOCTOR")
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("doctor01")
        ).thenReturn(Optional.of(doctor));

        try {

            var result =
                    medicalRecordService.saveDraft(
                            recordId,
                            request
                    );

            assertNotNull(result);

            assertEquals(
                    MedicalRecordStatus.DRAFT,
                    record.getStatus()
            );

            assertEquals(
                    "Viem phe quan",
                    record.getDiagnosis()
            );

            assertEquals(
                    "Theo doi va dieu tri",
                    record.getConclusion()
            );

            verify(medicalRecordRepository)
                    .save(record);

        } finally {

            SecurityContextHolder.clearContext();
        }
    }


// =========================================================
// SAVE DRAFT - NURSE + QUEUE TICKET NULL
// =========================================================

    @Test
    void saveDraft_ShouldAllowNurse_WhenRecordHasNoQueueTicket() {

        UUID recordId = UUID.randomUUID();

        StaffInfo nurse =
                mock(StaffInfo.class);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        MedicalRecordUpdateRequest request =
                mock(MedicalRecordUpdateRequest.class);

        when(request.version())
                .thenReturn(record.getVersion());

        when(request.chiefComplaint())
                .thenReturn("Chong mat");

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "nurse04",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_NURSE")
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("nurse04")
        ).thenReturn(Optional.of(nurse));

        try {

            var result =
                    medicalRecordService.saveDraft(
                            recordId,
                            request
                    );

            assertNotNull(result);

            assertEquals(
                    MedicalRecordStatus.DRAFT,
                    record.getStatus()
            );

            assertEquals(
                    "Chong mat",
                    record.getChiefComplaint()
            );

            assertSame(
                    nurse,
                    record.getNursingUpdatedBy()
            );

            verify(medicalRecordRepository)
                    .save(record);

        } finally {

            SecurityContextHolder.clearContext();
        }
    }


// =========================================================
// COMPLETE - ICD-10 LÀ ĐỦ ĐỂ HOÀN THÀNH
// diagnosis = null
// conclusion = null
// icdSelections != empty
// =========================================================

    @Test
    void complete_ShouldAllowCompletion_WhenOnlyIcd10Exists() {

        UUID recordId = UUID.randomUUID();

        CustomerVisit visit =
                mock(CustomerVisit.class);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        record.getIcdSelections()
                .add(mock(Icd10Selection.class));

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                testRequestRepository
                        .countByMedicalRecordAndStatusIn(
                                eq(recordId),
                                anyList()
                        )
        ).thenReturn(0L);

        when(
                invoiceRepository
                        .findAllByMedicalRecord_RecordId(recordId)
        ).thenReturn(List.of());

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        SecurityContextHolder.clearContext();

        var result =
                medicalRecordService.complete(recordId);

        assertNotNull(result);

        assertEquals(
                MedicalRecordStatus.COMPLETED,
                record.getStatus()
        );

        assertNotNull(
                record.getCompletedAt()
        );
    }


// =========================================================
// COMPLETE - GUEST APPOINTMENT
// patientName lấy từ guestFullName
// =========================================================

    @Test
    void complete_ShouldUseGuestNameInNotification_WhenAppointmentIsGuest() {

        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        Appointment appointment =
                mock(Appointment.class);

        when(appointment.getIsGuest())
                .thenReturn(true);

        when(appointment.getGuestFullName())
                .thenReturn("Tran Van Guest");

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getVisitId())
                .thenReturn(visitId);

        when(visit.getAppointment())
                .thenReturn(appointment);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .diagnosis("Cam cum")
                .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                testRequestRepository
                        .countByMedicalRecordAndStatusIn(
                                eq(recordId),
                                anyList()
                        )
        ).thenReturn(0L);

        when(
                invoiceRepository
                        .findAllByMedicalRecord_RecordId(recordId)
        ).thenReturn(List.of());

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        when(
                queueTicketRepository
                        .findAllByVisit_VisitId(visitId)
        ).thenReturn(List.of());

        SecurityContextHolder.clearContext();

        medicalRecordService.complete(recordId);

        verify(notificationService)
                .notifyStaffByRole(
                        eq(SystemRole.RECEPTIONIST),
                        eq("Khám bệnh hoàn tất"),
                        argThat(message ->
                                message.contains("Tran Van Guest")
                        ),
                        eq("MedicalRecord"),
                        eq(recordId)
                );
    }


// =========================================================
// COMPLETE - APPOINTMENT KHÔNG PHẢI GUEST
// nhưng không có CUSTOMER
// => patientName = "Khách"
// =========================================================

    @Test
    void complete_ShouldUseDefaultGuestName_WhenAppointmentHasNoCustomer() {

        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        Appointment appointment =
                mock(Appointment.class);

        when(appointment.getIsGuest())
                .thenReturn(false);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getVisitId())
                .thenReturn(visitId);

        when(visit.getAppointment())
                .thenReturn(appointment);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .diagnosis("Viem hong")
                .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                testRequestRepository
                        .countByMedicalRecordAndStatusIn(
                                eq(recordId),
                                anyList()
                        )
        ).thenReturn(0L);

        when(
                invoiceRepository
                        .findAllByMedicalRecord_RecordId(recordId)
        ).thenReturn(List.of());

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        when(
                queueTicketRepository
                        .findAllByVisit_VisitId(visitId)
        ).thenReturn(List.of());

        SecurityContextHolder.clearContext();

        medicalRecordService.complete(recordId);

        verify(notificationService)
                .notifyStaffByRole(
                        eq(SystemRole.RECEPTIONIST),
                        eq("Khám bệnh hoàn tất"),
                        argThat(message ->
                                message.contains("Khách")
                        ),
                        eq("MedicalRecord"),
                        eq(recordId)
                );
    }


// =========================================================
// COMPLETE - REGISTERED CUSTOMER NAME
// =========================================================

    @Test
    void complete_ShouldUseCustomerNameInNotification_WhenRegisteredPatient() {

        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        Profile customer =
                mock(Profile.class);

        when(customer.getFullName())
                .thenReturn("Nguyen Van Customer");

        Appointment appointment =
                mock(Appointment.class);

        when(appointment.getIsGuest())
                .thenReturn(false);

        when(appointment.getCustomer())
                .thenReturn(customer);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getVisitId())
                .thenReturn(visitId);

        when(visit.getAppointment())
                .thenReturn(appointment);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .status(MedicalRecordStatus.IN_PROGRESS)
                .diagnosis("Dau da day")
                .build();

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(
                testRequestRepository
                        .countByMedicalRecordAndStatusIn(
                                eq(recordId),
                                anyList()
                        )
        ).thenReturn(0L);

        when(
                invoiceRepository
                        .findAllByMedicalRecord_RecordId(recordId)
        ).thenReturn(List.of());

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        when(
                queueTicketRepository
                        .findAllByVisit_VisitId(visitId)
        ).thenReturn(List.of());

        SecurityContextHolder.clearContext();

        medicalRecordService.complete(recordId);

        verify(notificationService)
                .notifyStaffByRole(
                        eq(SystemRole.RECEPTIONIST),
                        eq("Khám bệnh hoàn tất"),
                        argThat(message ->
                                message.contains("Nguyen Van Customer")
                        ),
                        eq("MedicalRecord"),
                        eq(recordId)
                );
    }


// =========================================================
// FOLLOW UP - COPY ĐẦY ĐỦ THÔNG TIN GUEST
// =========================================================

    @Test
    void scheduleFollowUp_ShouldCopyAllGuestInformation() {

        UUID recordId = UUID.randomUUID();

        Appointment oldAppointment =
                mock(Appointment.class);

        when(oldAppointment.getIsGuest())
                .thenReturn(true);

        when(oldAppointment.getGuestFullName())
                .thenReturn("Le Van Guest");

        when(oldAppointment.getGuestPhone())
                .thenReturn("0966666666");

        when(oldAppointment.getGuestAge())
                .thenReturn(35);

        when(oldAppointment.getGuestGender())
                .thenReturn(Gender.MALE);

        when(oldAppointment.getGuestAddress())
                .thenReturn("Ha Noi");

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getAppointment())
                .thenReturn(oldAppointment);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .followUpNote("Tai kham sau 2 tuan")
                .build();

        AppointmentCreateRequest request =
                mock(AppointmentCreateRequest.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(
                        invocation -> invocation.getArgument(0)
                );

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.scheduleFollowUp(
                recordId,
                request
        );

        Appointment created =
                record.getFollowUpAppointment();

        assertNotNull(created);

        assertTrue(created.getIsGuest());

        assertEquals(
                "Le Van Guest",
                created.getGuestFullName()
        );

        assertEquals(
                "0966666666",
                created.getGuestPhone()
        );

        assertEquals(
                35,
                created.getGuestAge()
        );

        assertEquals(
                Gender.MALE,
                created.getGuestGender()
        );

        assertEquals(
                "Ha Noi",
                created.getGuestAddress()
        );
    }


// =========================================================
// FOLLOW UP - QUEUE TICKET CÓ NHƯNG SERVICE NULL
// =========================================================

    @Test
    void scheduleFollowUp_ShouldNotAddService_WhenQueueTicketServiceIsNull() {

        UUID recordId = UUID.randomUUID();

        Profile customer =
                mock(Profile.class);

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getCustomer())
                .thenReturn(customer);

        QueueTicket queueTicket =
                mock(QueueTicket.class);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .visit(visit)
                .queueTicket(queueTicket)
                .followUpNote("Tai kham")
                .build();

        AppointmentCreateRequest request =
                mock(AppointmentCreateRequest.class);

        when(medicalRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(
                        invocation -> invocation.getArgument(0)
                );

        when(medicalRecordRepository.save(record))
                .thenReturn(record);

        medicalRecordService.scheduleFollowUp(
                recordId,
                request
        );

        Appointment created =
                record.getFollowUpAppointment();

        assertNotNull(created);

        assertTrue(
                created.getServices().isEmpty()
        );
    }
}