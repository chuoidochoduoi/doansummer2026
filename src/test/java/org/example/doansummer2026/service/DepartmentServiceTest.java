package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.department.DepartmentCreateRequest;
import org.example.doansummer2026.dto.department.DepartmentUpdateRequest;
import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.StaffCapabilityStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.ServiceCapability;
import org.example.doansummer2026.model.Specialization;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.ServiceCapabilityRepository;
import org.example.doansummer2026.repository.SpecializationRepository;
import org.example.doansummer2026.repository.StaffCapabilityRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository repo;

    @Mock
    private StaffInfoRepository staffRepo;

    @Mock
    private SpecializationRepository specializationRepo;

    @Mock
    private ServiceCapabilityRepository capabilityRepo;

    @Mock
    private StaffCapabilityRepository staffCapabilityRepo;

    @Mock
    private AuthService authService;

    @Mock private org.example.doansummer2026.repository.StaffScheduleRepository staffScheduleRepo;
    @Mock private org.example.doansummer2026.repository.MedicalRecordRepository medicalRecordRepo;

    @InjectMocks
    private DepartmentService departmentService;

    @BeforeEach
    void useRealEligibilityPolicyWithIsolatedRepositories() {
        org.springframework.test.util.ReflectionTestUtils.setField(departmentService, "staffDutyService",
                new StaffDutyService(staffScheduleRepo, staffRepo, staffCapabilityRepo, authService));
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private DepartmentUpdateRequest partialUpdate() {
        return mock(DepartmentUpdateRequest.class, invocation -> {
            if (List.of("doctorIds", "nurseIds", "capabilityIds").contains(invocation.getMethod().getName())) return null;
            return RETURNS_DEFAULTS.answer(invocation);
        });
    }

    private Department department(
            UUID id,
            String roomCode,
            String name,
            DepartmentType type
    ) {
        return Department.builder()
                .departmentId(id)
                .roomCode(roomCode)
                .name(name)
                .status(DepartmentStatus.AVAILABLE)
                .departmentType(type)
                .specialization(type.normalized() == DepartmentType.EXAMINATION
                        ? Specialization.builder().specializationId(UUID.randomUUID()).active(true).build() : null)
                .capabilities(type.normalized().isParaclinical()
                        ? new HashSet<>(List.of(ServiceCapability.builder().capabilityId(UUID.randomUUID()).active(true).build()))
                        : new HashSet<>())
                .build();
    }


    private StaffInfo staff(
            UUID id,
            String username,
            String fullName,
            SystemRole role
    ) {

        Account account =
                Account.builder()
                        .accountId(UUID.randomUUID())
                        .username(username)
                        .isActive(true)
                        .build();

        Profile profile =
                Profile.builder()
                        .profileId(UUID.randomUUID())
                        .account(account)
                        .fullName(fullName)
                        .build();

        return StaffInfo.builder()
                .staffId(id)
                .staffCode(
                        "STF-" +
                                id.toString()
                                        .substring(0, 4)
                )
                .profile(profile)
                .systemRole(role)
                .build();
    }


    private Specialization activeSpecialization() {

        Specialization specialization =
                mock(Specialization.class);

        when(specialization.getActive())
                .thenReturn(true);

        return specialization;
    }


    private ServiceCapability activeCapability(
            UUID capabilityId
    ) {

        ServiceCapability capability =
                mock(ServiceCapability.class);

        when(capability.getCapabilityId())
                .thenReturn(capabilityId);

        when(capability.getActive())
                .thenReturn(true);

        return capability;
    }


    // =========================================================
    // LIST ALL
    // =========================================================

    @Test
    void listAll_ShouldReturnMappedPage() {

        var pageable =
                PageRequest.of(0, 10);

        Department department =
                department(
                        UUID.randomUUID(),
                        "P101",
                        "Phong 101",
                        DepartmentType.EXAMINATION
                );

        when(
                repo.findAllWithHeadDoctor(
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of(department)
                )
        );

        var result =
                departmentService.listAll(
                        pageable
                );

        assertNotNull(result);

        verify(repo)
                .findAllWithHeadDoctor(
                        pageable
                );
    }


    // =========================================================
    // LIST BY TYPE
    // =========================================================

    @Test
    void list_ShouldReturnDepartmentsByType() {

        var pageable =
                PageRequest.of(0, 10);

        Department department =
                department(
                        UUID.randomUUID(),
                        "P102",
                        "Phong kham",
                        DepartmentType.EXAMINATION
                );

        when(
                repo.findAllByDepartmentType(
                        DepartmentType.EXAMINATION,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of(department)
                )
        );

        var result =
                departmentService.list(
                        DepartmentType.EXAMINATION,
                        pageable
                );

        assertNotNull(result);

        verify(repo)
                .findAllByDepartmentType(
                        DepartmentType.EXAMINATION,
                        pageable
                );
    }


    // =========================================================
    // LIST MULTIPLE
    // =========================================================

    @Test
    void listMultiple_ShouldReturnDepartmentsByTypes() {

        var pageable =
                PageRequest.of(0, 10);

        List<DepartmentType> types =
                List.of(
                        DepartmentType.EXAMINATION,
                        DepartmentType.PARACLINICAL
                );

        when(
                repo.findAllByDepartmentTypeIn(
                        types,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of()
                )
        );

        var result =
                departmentService.listMultiple(
                        pageable,
                        types
                );

        assertNotNull(result);

        verify(repo)
                .findAllByDepartmentTypeIn(
                        types,
                        pageable
                );
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturnDepartment() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P101",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        assertSame(
                department,
                departmentService.findById(id)
        );
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id =
                UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService
                                .findById(id)
        );
    }


    // =========================================================
    // GET
    // =========================================================

    @Test
    void get_ShouldReturnResponse() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P101",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        assertNotNull(
                departmentService.get(id)
        );
    }


    // =========================================================
    // CREATE - DUPLICATE ROOM CODE
    // =========================================================

    @Test
    void create_ShouldRejectDuplicateRoomCode() {

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("P101");

        when(
                repo.existsByRoomCode("P101")
        ).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.create(req)
        );

        verify(
                repo,
                never()
        ).save(any());
    }


    // =========================================================
    // CREATE - DUPLICATE NAME
    // =========================================================

    @Test
    void create_ShouldRejectDuplicateName() {

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("P101");

        when(req.name())
                .thenReturn("Khoa Noi");

        when(
                repo.existsByRoomCode("P101")
        ).thenReturn(false);

        when(
                repo.existsByName("Khoa Noi")
        ).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.create(req)
        );

        verify(
                repo,
                never()
        ).save(any());
    }


    // =========================================================
    // CREATE - DEFAULT TYPE EXAMINATION
    // =========================================================

    @Test
    void create_ShouldRejectWhenDefaultExaminationHasNoSpecialization() {

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("P101");

        when(req.name())
                .thenReturn("Phong");

        when(
                repo.existsByRoomCode("P101")
        ).thenReturn(false);

        when(
                repo.existsByName("Phong")
        ).thenReturn(false);

        assertThrows(
                BadRequestException.class,
                () ->
                        departmentService.create(req)
        );

        verify(
                repo,
                never()
        ).save(any());
    }


    // =========================================================
    // CREATE - SPECIALIZATION MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenSpecializationMissing() {

        UUID specializationId =
                UUID.randomUUID();

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("P101");

        when(req.name())
                .thenReturn("Khoa");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.EXAMINATION
                );

        when(req.specializationId())
                .thenReturn(
                        specializationId
                );

        when(
                repo.existsByRoomCode("P101")
        ).thenReturn(false);

        when(
                repo.existsByName("Khoa")
        ).thenReturn(false);

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - INACTIVE SPECIALIZATION
    // =========================================================

    @Test
    void create_ShouldReject_WhenSpecializationInactive() {

        UUID specializationId =
                UUID.randomUUID();

        Specialization specialization =
                mock(Specialization.class);

        when(
                specialization.getActive()
        ).thenReturn(false);

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("P101");

        when(req.name())
                .thenReturn("Phong");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.EXAMINATION
                );

        when(req.specializationId())
                .thenReturn(
                        specializationId
                );

        when(
                repo.existsByRoomCode("P101")
        ).thenReturn(false);

        when(
                repo.existsByName("Phong")
        ).thenReturn(false);

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(
                Optional.of(specialization)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - HEAD DOCTOR MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenHeadDoctorMissing() {

        UUID specializationId =
                UUID.randomUUID();

        UUID doctorId =
                UUID.randomUUID();

        Specialization specialization =
                activeSpecialization();

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("P101");

        when(req.name())
                .thenReturn("Phong");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.EXAMINATION
                );

        when(req.specializationId())
                .thenReturn(
                        specializationId
                );

        when(req.headDoctorId())
                .thenReturn(
                        doctorId
                );

        when(
                repo.existsByRoomCode("P101")
        ).thenReturn(false);

        when(
                repo.existsByName("Phong")
        ).thenReturn(false);

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(
                Optional.of(specialization)
        );

        when(
                staffRepo.findById(
                        doctorId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - HEAD DOCTOR WRONG ROLE
    // =========================================================

    @Test
    void create_ShouldReject_WhenHeadDoctorIsNotDoctor() {

        UUID doctorId =
                UUID.randomUUID();

        StaffInfo nurse =
                staff(
                        doctorId,
                        "nurse01",
                        "Nurse",
                        SystemRole.NURSE
                );

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("LAB01");

        when(req.name())
                .thenReturn("Lab");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.PARACLINICAL
                );

        when(req.headDoctorId())
                .thenReturn(
                        doctorId
                );

        when(
                repo.existsByRoomCode("LAB01")
        ).thenReturn(false);

        when(
                repo.existsByName("Lab")
        ).thenReturn(false);

        when(
                staffRepo.findById(
                        doctorId
                )
        ).thenReturn(
                Optional.of(nurse)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - HEAD DOCTOR ALREADY USED
    // =========================================================

    @Test
    void create_ShouldReject_WhenHeadDoctorAlreadyManagesAnotherRoom() {

        UUID specializationId =
                UUID.randomUUID();

        UUID doctorId =
                UUID.randomUUID();

        Specialization specialization =
                activeSpecialization();

        StaffInfo doctor =
                staff(
                        doctorId,
                        "doctor",
                        "Doctor",
                        SystemRole.DOCTOR
                );

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("P1");

        when(req.name())
                .thenReturn("Phong 1");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.EXAMINATION
                );

        when(req.specializationId())
                .thenReturn(
                        specializationId
                );

        when(req.headDoctorId())
                .thenReturn(
                        doctorId
                );

        when(
                repo.existsByRoomCode("P1")
        ).thenReturn(false);

        when(
                repo.existsByName("Phong 1")
        ).thenReturn(false);

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(
                Optional.of(specialization)
        );

        when(
                staffRepo.findById(
                        doctorId
                )
        ).thenReturn(
                Optional.of(doctor)
        );

        when(
                repo.existsByHeadDoctor_StaffId(
                        doctorId
                )
        ).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.create(req)
        );

        verify(
                repo,
                never()
        ).save(any());
    }


    // =========================================================
    // CREATE - CAPABILITY MISSING
    // =========================================================

    @Test
    void create_ShouldReject_WhenCapabilityIdsContainMissingCapability() {

        UUID capability1 =
                UUID.randomUUID();

        UUID capability2 =
                UUID.randomUUID();

        ServiceCapability capability =
                mock(ServiceCapability.class);

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("LAB01");

        when(req.name())
                .thenReturn("Phong XN");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.PARACLINICAL
                );

        when(req.capabilityIds())
                .thenReturn(
                        List.of(
                                capability1,
                                capability2
                        )
                );

        when(
                repo.existsByRoomCode("LAB01")
        ).thenReturn(false);

        when(
                repo.existsByName("Phong XN")
        ).thenReturn(false);

        when(
                capabilityRepo.findAllById(any())
        ).thenReturn(
                List.of(capability)
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService.create(req)
        );

        verify(
                repo,
                never()
        ).save(any());
    }


    // =========================================================
    // CREATE - INACTIVE CAPABILITY
    // =========================================================

    @Test
    void create_ShouldReject_WhenCapabilityInactive() {

        UUID capabilityId =
                UUID.randomUUID();

        ServiceCapability capability =
                mock(ServiceCapability.class);

        when(
                capability.getActive()
        ).thenReturn(false);

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("LAB01");

        when(req.name())
                .thenReturn("Lab");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.PARACLINICAL
                );

        when(req.capabilityIds())
                .thenReturn(
                        List.of(capabilityId)
                );

        when(
                repo.existsByRoomCode("LAB01")
        ).thenReturn(false);

        when(
                repo.existsByName("Lab")
        ).thenReturn(false);

        when(
                capabilityRepo.findAllById(any())
        ).thenReturn(
                List.of(capability)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.create(req)
        );

        verify(
                repo,
                never()
        ).save(any());
    }


    // =========================================================
    // CREATE - HEAD DOCTOR CAPABILITY DOES NOT MATCH
    // =========================================================

    @Test
    void create_ShouldReject_WhenHeadDoctorHasNoMatchingCapability() {

        UUID doctorId =
                UUID.randomUUID();

        UUID capabilityId =
                UUID.randomUUID();

        StaffInfo doctor =
                staff(
                        doctorId,
                        "doctor01",
                        "Doctor",
                        SystemRole.DOCTOR
                );

        ServiceCapability capability =
                activeCapability(
                        capabilityId
                );

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("LAB01");

        when(req.name())
                .thenReturn("Lab");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.PARACLINICAL
                );

        when(req.headDoctorId())
                .thenReturn(
                        doctorId
                );

        when(req.capabilityIds())
                .thenReturn(
                        List.of(capabilityId)
                );

        when(
                repo.existsByRoomCode("LAB01")
        ).thenReturn(false);

        when(
                repo.existsByName("Lab")
        ).thenReturn(false);

        when(
                staffRepo.findById(
                        doctorId
                )
        ).thenReturn(
                Optional.of(doctor)
        );

        when(
                repo.existsByHeadDoctor_StaffId(
                        doctorId
                )
        ).thenReturn(false);

        when(
                capabilityRepo.findAllById(any())
        ).thenReturn(
                List.of(capability)
        );

        when(
                staffCapabilityRepo
                        .existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(
                                doctorId,
                                capabilityId,
                                StaffCapabilityStatus.ACTIVE
                        )
        ).thenReturn(false);

        when(repo.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(
                BadRequestException.class,
                () ->
                        departmentService.create(req)
        );


    }


    // =========================================================
    // CREATE SUCCESS - PARACLINICAL
    // =========================================================

    @Test
    void create_ShouldCreateParaclinicalRoom_WhenDoctorCapabilityMatches() {

        UUID id =
                UUID.randomUUID();

        UUID doctorId =
                UUID.randomUUID();

        UUID capabilityId =
                UUID.randomUUID();

        StaffInfo doctor =
                staff(
                        doctorId,
                        "doctor01",
                        "Doctor",
                        SystemRole.DOCTOR
                );

        ServiceCapability capability =
                activeCapability(
                        capabilityId
                );

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("LAB01");

        when(req.name())
                .thenReturn("Phong XN");

        when(req.description())
                .thenReturn("Mo ta");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.PARACLINICAL
                );

        when(req.headDoctorId())
                .thenReturn(
                        doctorId
                );

        when(req.capabilityIds())
                .thenReturn(
                        List.of(capabilityId)
                );

        when(
                repo.existsByRoomCode("LAB01")
        ).thenReturn(false);

        when(
                repo.existsByName("Phong XN")
        ).thenReturn(false);

        when(
                staffRepo.findById(
                        doctorId
                )
        ).thenReturn(
                Optional.of(doctor)
        );

        when(
                repo.existsByHeadDoctor_StaffId(
                        doctorId
                )
        ).thenReturn(false);

        when(
                capabilityRepo.findAllById(any())
        ).thenReturn(
                List.of(capability)
        );

        when(
                staffCapabilityRepo
                        .existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(
                                doctorId,
                                capabilityId,
                                StaffCapabilityStatus.ACTIVE
                        )
        ).thenReturn(true);

        when(
                repo.save(
                        any(Department.class)
                )
        ).thenAnswer(
                invocation -> {

                    Department saved =
                            invocation.getArgument(0);

                    saved.setDepartmentId(id);

                    return saved;
                }
        );

        when(repo.findById(id))
                .thenAnswer(
                        invocation ->
                                Optional.of(
                                        department(
                                                id,
                                                "LAB01",
                                                "Phong XN",
                                                DepartmentType.PARACLINICAL
                                        )
                                )
                );

        var result =
                departmentService.create(req);

        assertNotNull(result);

        verify(repo)
                .save(
                        argThat(
                                saved ->
                                        saved.getHeadDoctor()
                                                == doctor
                                                &&
                                                saved.getCapabilities()
                                                        .contains(capability)
                                                &&
                                                saved.getDepartmentType()
                                                        == DepartmentType.PARACLINICAL
                                                &&
                                                saved.getStatus()
                                                        == DepartmentStatus.AVAILABLE
                        )
                );
    }


    // =========================================================
    // CREATE - NURSE MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenNurseMissing() {

        UUID id =
                UUID.randomUUID();

        UUID nurseId =
                UUID.randomUUID();

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("LAB01");

        when(req.name())
                .thenReturn("Lab");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.PARACLINICAL
                );

        when(req.nurseIds())
                .thenReturn(
                        List.of(nurseId)
                );

        when(
                repo.existsByRoomCode("LAB01")
        ).thenReturn(false);

        when(
                repo.existsByName("Lab")
        ).thenReturn(false);

        when(
                repo.save(any(Department.class))
        ).thenAnswer(
                invocation -> {

                    Department saved =
                            invocation.getArgument(0);

                    saved.setDepartmentId(id);

                    return saved;
                }
        );

        when(
                staffRepo.findById(nurseId)
        ).thenReturn(
                Optional.empty()
        );

        UUID configuredCapabilityId = UUID.randomUUID();
        when(req.capabilityIds()).thenReturn(List.of(configuredCapabilityId));
        when(capabilityRepo.findAllById(List.of(configuredCapabilityId))).thenReturn(List.of(
                ServiceCapability.builder().capabilityId(configuredCapabilityId).active(true).build()));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - NURSE ALREADY ASSIGNED
    // =========================================================

    @Test
    void create_ShouldReject_WhenNurseAlreadyAssignedToRoom() {

        UUID id =
                UUID.randomUUID();

        UUID nurseId =
                UUID.randomUUID();

        StaffInfo nurse =
                staff(
                        nurseId,
                        "nurse",
                        "Nurse",
                        SystemRole.NURSE
                );

        nurse.setDepartment(
                department(
                        UUID.randomUUID(),
                        "OLD",
                        "Old room",
                        DepartmentType.EXAMINATION
                )
        );

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("LAB");

        when(req.name())
                .thenReturn("Lab");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.PARACLINICAL
                );

        when(req.nurseIds())
                .thenReturn(
                        List.of(nurseId)
                );

        when(
                repo.existsByRoomCode("LAB")
        ).thenReturn(false);

        when(
                repo.existsByName("Lab")
        ).thenReturn(false);

        when(
                repo.save(any(Department.class))
        ).thenAnswer(
                invocation -> {

                    Department saved =
                            invocation.getArgument(0);

                    saved.setDepartmentId(id);

                    return saved;
                }
        );

        when(
                staffRepo.findById(nurseId)
        ).thenReturn(
                Optional.of(nurse)
        );

        UUID configuredCapabilityId = UUID.randomUUID();
        when(req.capabilityIds()).thenReturn(List.of(configuredCapabilityId));
        when(capabilityRepo.findAllById(List.of(configuredCapabilityId))).thenReturn(List.of(
                ServiceCapability.builder().capabilityId(configuredCapabilityId).active(true).build()));

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE SUCCESS WITH NURSE
    // =========================================================

    @Test
    void create_ShouldAssignNursesToSavedDepartment() {

        UUID id =
                UUID.randomUUID();

        UUID nurseId =
                UUID.randomUUID();

        StaffInfo nurse =
                staff(
                        nurseId,
                        "nurse",
                        "Nurse",
                        SystemRole.NURSE
                );

        Department saved =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        DepartmentCreateRequest req =
                mock(
                        DepartmentCreateRequest.class
                );

        when(req.roomCode())
                .thenReturn("LAB");

        when(req.name())
                .thenReturn("Lab");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.PARACLINICAL
                );

        when(req.nurseIds())
                .thenReturn(
                        List.of(nurseId)
                );

        when(
                repo.existsByRoomCode("LAB")
        ).thenReturn(false);

        when(
                repo.existsByName("Lab")
        ).thenReturn(false);

        when(
                repo.save(any(Department.class))
        ).thenReturn(saved);

        when(
                staffRepo.findById(nurseId)
        ).thenReturn(
                Optional.of(nurse)
        );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(saved)
                );

        UUID configuredCapabilityId = UUID.randomUUID();
        when(req.capabilityIds()).thenReturn(List.of(configuredCapabilityId));
        when(capabilityRepo.findAllById(List.of(configuredCapabilityId))).thenReturn(List.of(
                ServiceCapability.builder().capabilityId(configuredCapabilityId).active(true).build()));
        when(staffCapabilityRepo.existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(any(), any(), eq(StaffCapabilityStatus.ACTIVE)))
                .thenReturn(true);

        var result =
                departmentService.create(req);

        assertNotNull(result);

        assertSame(
                saved,
                nurse.getDepartment()
        );

        verify(staffRepo)
                .save(nurse);
    }


    // =========================================================
    // UPDATE - MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenDepartmentMissing() {

        UUID id =
                UUID.randomUUID();

        DepartmentUpdateRequest req =
                partialUpdate();

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService.update(
                                id,
                                req
                        )
        );
    }


    // =========================================================
    // UPDATE - DUPLICATE ROOM
    // =========================================================

    @Test
    void update_ShouldRejectDuplicateNewRoomCode() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "OLD",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.roomCode())
                .thenReturn("NEW");

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                repo.existsByRoomCode("NEW")
        ).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.update(
                                id,
                                req
                        )
        );

        verify(
                repo,
                never()
        ).save(any());
    }


    // =========================================================
    // UPDATE - SAME ROOM
    // =========================================================

    @Test
    void update_ShouldNotCheckDuplicate_WhenRoomCodeUnchanged() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "ROOM",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.roomCode())
                .thenReturn("ROOM");

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(repo.save(department))
                .thenReturn(department);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        departmentService.update(
                id,
                req
        );

        verify(
                repo,
                never()
        ).existsByRoomCode(anyString());
    }


    // =========================================================
    // UPDATE - DUPLICATE NAME
    // =========================================================

    @Test
    void update_ShouldRejectDuplicateNewName() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Old",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.name())
                .thenReturn("New");

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                repo.existsByName("New")
        ).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.update(
                                id,
                                req
                        )
        );
    }


    // =========================================================
    // UPDATE BASIC FIELDS
    // =========================================================

    @Test
    void update_ShouldUpdateBasicFields() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "OLD",
                        "Old",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.roomCode())
                .thenReturn("NEW");

        when(req.name())
                .thenReturn("New");

        when(req.description())
                .thenReturn(
                        "New description"
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                repo.existsByRoomCode("NEW")
        ).thenReturn(false);

        when(
                repo.existsByName("New")
        ).thenReturn(false);

        when(repo.save(department))
                .thenReturn(department);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        var result =
                departmentService.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                "NEW",
                department.getRoomCode()
        );

        assertEquals(
                "New",
                department.getName()
        );

        assertEquals(
                "New description",
                department.getDescription()
        );
    }


    // =========================================================
    // UPDATE TYPE
    // =========================================================

    @Test
    void update_ShouldNormalizeTypeAndClearSpecialization_WhenNotExamination() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        department.setSpecialization(
                mock(Specialization.class)
        );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.LABORATORY
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                repo.countOperationalReferences(id)
        ).thenReturn(0L);

        when(repo.save(department))
                .thenReturn(department);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        UUID configuredCapabilityId = UUID.randomUUID();
        when(req.capabilityIds()).thenReturn(List.of(configuredCapabilityId));
        when(capabilityRepo.findAllById(List.of(configuredCapabilityId))).thenReturn(List.of(
                ServiceCapability.builder().capabilityId(configuredCapabilityId).active(true).build()));

        departmentService.update(
                id,
                req
        );

        assertEquals(
                DepartmentType.PARACLINICAL,
                department.getDepartmentType()
        );

        assertNull(
                department.getSpecialization()
        );
    }


    // =========================================================
    // UPDATE CLASSIFICATION HAS REFERENCES
    // =========================================================

    @Test
    void update_ShouldRejectClassificationChange_WhenOperationalReferencesExist() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.PARACLINICAL
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                repo.countOperationalReferences(id)
        ).thenReturn(3L);

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.update(
                                id,
                                req
                        )
        );

        verify(
                repo,
                never()
        ).save(any());
    }


    // =========================================================
    // UPDATE SPECIALIZATION MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenNewSpecializationMissing() {

        UUID id =
                UUID.randomUUID();

        UUID specializationId =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.specializationId())
                .thenReturn(
                        specializationId
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                repo.countOperationalReferences(id)
        ).thenReturn(0L);

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService.update(
                                id,
                                req
                        )
        );
    }


    // =========================================================
    // UPDATE SPECIALIZATION SUCCESS
    // =========================================================

    @Test
    void update_ShouldSetSpecialization() {

        UUID id =
                UUID.randomUUID();

        UUID specializationId =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        Specialization specialization =
                activeSpecialization();

        when(
                specialization
                        .getSpecializationId()
        ).thenReturn(
                specializationId
        );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.specializationId())
                .thenReturn(
                        specializationId
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                repo.countOperationalReferences(id)
        ).thenReturn(0L);

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(
                Optional.of(specialization)
        );

        when(repo.save(department))
                .thenReturn(department);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        departmentService.update(
                id,
                req
        );

        assertSame(
                specialization,
                department.getSpecialization()
        );
    }


    // =========================================================
    // UPDATE CAPABILITY MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenSomeCapabilitiesDoNotExist() {

        UUID id =
                UUID.randomUUID();

        UUID capability1 =
                UUID.randomUUID();

        UUID capability2 =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        ServiceCapability capability =
                mock(ServiceCapability.class);

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.capabilityIds())
                .thenReturn(
                        List.of(
                                capability1,
                                capability2
                        )
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                capabilityRepo.findAllById(any())
        ).thenReturn(
                List.of(capability)
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService.update(
                                id,
                                req
                        )
        );

        verify(
                repo,
                never()
        ).save(any());
    }


    // =========================================================
    // UPDATE HEAD DOCTOR CONFLICT
    // =========================================================

    @Test
    void update_ShouldRejectHeadDoctorAssignedToDifferentDepartment() {

        UUID id =
                UUID.randomUUID();

        UUID doctorId =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.headDoctorId())
                .thenReturn(
                        doctorId
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                repo.existsByHeadDoctor_StaffId(
                        doctorId
                )
        ).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.update(
                                id,
                                req
                        )
        );
    }


    // =========================================================
    // UPDATE SAME HEAD DOCTOR
    // =========================================================

    @Test
    void update_ShouldAllowExistingHeadDoctorOfSameDepartment() {

        UUID id =
                UUID.randomUUID();

        UUID doctorId =
                UUID.randomUUID();

        StaffInfo doctor =
                staff(
                        doctorId,
                        "doctor",
                        "Doctor",
                        SystemRole.DOCTOR
                );

        Department department =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        department.setHeadDoctor(
                doctor
        );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.headDoctorId())
                .thenReturn(
                        doctorId
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                repo.existsByHeadDoctor_StaffId(
                        doctorId
                )
        ).thenReturn(true);

        when(
                staffRepo.findById(
                        doctorId
                )
        ).thenReturn(
                Optional.of(doctor)
        );

        when(repo.save(department))
                .thenReturn(department);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        when(staffCapabilityRepo.existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(any(), any(), eq(StaffCapabilityStatus.ACTIVE)))
                .thenReturn(true);

        departmentService.update(
                id,
                req
        );

        assertSame(
                doctor,
                department.getHeadDoctor()
        );
    }


    // =========================================================
    // UPDATE HEAD DOCTOR MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenNewHeadDoctorMissing() {

        UUID id =
                UUID.randomUUID();

        UUID doctorId =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.headDoctorId())
                .thenReturn(
                        doctorId
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                repo.existsByHeadDoctor_StaffId(
                        doctorId
                )
        ).thenReturn(false);

        when(
                staffRepo.findById(
                        doctorId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService.update(
                                id,
                                req
                        )
        );
    }


    // =========================================================
    // UPDATE NURSES - REMOVE OLD
    // =========================================================

    @Test
    void update_ShouldRemoveOldNurseNotInRequest() {

        UUID id =
                UUID.randomUUID();

        UUID oldNurseId =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        StaffInfo oldNurse =
                staff(
                        oldNurseId,
                        "oldnurse",
                        "Old Nurse",
                        SystemRole.NURSE
                );

        oldNurse.setDepartment(
                department
        );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.nurseIds())
                .thenReturn(
                        List.of()
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                staffRepo
                        .findByDepartment_DepartmentId(
                                id
                        )
        ).thenAnswer(inv -> oldNurse.getDepartment() == department ? List.of(oldNurse) : List.of());

        when(repo.save(department))
                .thenReturn(department);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        departmentService.update(
                id,
                req
        );

        assertNull(
                oldNurse.getDepartment()
        );

        verify(staffRepo)
                .save(oldNurse);
    }


    // =========================================================
    // UPDATE NURSES - KEEP EXISTING
    // =========================================================

    @Test
    void update_ShouldKeepExistingNurse_WhenStillRequested() {

        UUID id =
                UUID.randomUUID();

        UUID nurseId =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        StaffInfo nurse =
                staff(
                        nurseId,
                        "nurse",
                        "Nurse",
                        SystemRole.NURSE
                );

        nurse.setDepartment(
                department
        );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.nurseIds())
                .thenReturn(
                        List.of(nurseId)
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                staffRepo
                        .findByDepartment_DepartmentId(
                                id
                        )
        ).thenReturn(
                List.of(nurse)
        );

        when(
                staffRepo.findById(nurseId)
        ).thenReturn(
                Optional.of(nurse)
        );

        when(repo.save(department))
                .thenReturn(department);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        when(staffCapabilityRepo.existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(any(), any(), eq(StaffCapabilityStatus.ACTIVE)))
                .thenReturn(true);

        departmentService.update(
                id,
                req
        );

        assertSame(
                department,
                nurse.getDepartment()
        );

        verify(staffRepo)
                .save(nurse);
    }


    // =========================================================
    // UPDATE NURSE MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenRequestedNurseMissing() {

        UUID id =
                UUID.randomUUID();

        UUID nurseId =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.nurseIds())
                .thenReturn(
                        List.of(nurseId)
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                staffRepo
                        .findByDepartment_DepartmentId(
                                id
                        )
        ).thenReturn(
                List.of()
        );

        when(
                staffRepo.findById(nurseId)
        ).thenReturn(
                Optional.empty()
        );

        when(repo.save(department)).thenReturn(department);

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService.update(
                                id,
                                req
                        )
        );
    }


    // =========================================================
    // UPDATE NURSE OTHER DEPARTMENT
    // =========================================================

    @Test
    void update_ShouldRejectNurseBelongingToAnotherDepartment() {

        UUID id =
                UUID.randomUUID();

        UUID nurseId =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P1",
                        "Phong 1",
                        DepartmentType.PARACLINICAL
                );

        Department other =
                department(
                        UUID.randomUUID(),
                        "P2",
                        "Phong 2",
                        DepartmentType.PARACLINICAL
                );

        StaffInfo nurse =
                staff(
                        nurseId,
                        "nurse",
                        "Nurse",
                        SystemRole.NURSE
                );

        nurse.setDepartment(
                other
        );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.nurseIds())
                .thenReturn(
                        List.of(nurseId)
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                staffRepo
                        .findByDepartment_DepartmentId(
                                id
                        )
        ).thenReturn(
                List.of()
        );

        when(
                staffRepo.findById(nurseId)
        ).thenReturn(
                Optional.of(nurse)
        );

        when(repo.save(department)).thenReturn(department);

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.update(
                                id,
                                req
                        )
        );
    }


    // =========================================================
    // UPDATE CAPABILITY VALIDATION FAIL
    // =========================================================

    @Test
    void update_ShouldReject_WhenHeadDoctorNoLongerMatchesCapabilities() {

        UUID id =
                UUID.randomUUID();

        UUID doctorId =
                UUID.randomUUID();

        UUID capabilityId =
                UUID.randomUUID();

        StaffInfo doctor =
                staff(
                        doctorId,
                        "doctor",
                        "Doctor",
                        SystemRole.DOCTOR
                );

        ServiceCapability capability =
                activeCapability(
                        capabilityId
                );

        Department department =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        department.setHeadDoctor(
                doctor
        );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.capabilityIds())
                .thenReturn(
                        List.of(capabilityId)
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                capabilityRepo.findAllById(any())
        ).thenReturn(
                List.of(capability)
        );

        when(
                staffCapabilityRepo
                        .existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(
                                doctorId,
                                capabilityId,
                                StaffCapabilityStatus.ACTIVE
                        )
        ).thenReturn(false);

        when(repo.save(department)).thenReturn(department);
        when(staffRepo.findById(doctorId)).thenReturn(Optional.of(doctor));

        assertThrows(
                BadRequestException.class,
                () ->
                        departmentService.update(
                                id,
                                req
                        )
        );

        // The facade saves the managed department before validating membership;
        // its transaction rolls back on rejection. No staff assignment may be written.
        verify(staffRepo, never()).save(any());
    }


    // =========================================================
    // UPDATE CAPABILITY VALIDATION SUCCESS
    // =========================================================

    @Test
    void update_ShouldSave_WhenHeadDoctorMatchesAtLeastOneCapability() {

        UUID id =
                UUID.randomUUID();

        UUID doctorId =
                UUID.randomUUID();

        UUID capabilityId =
                UUID.randomUUID();

        StaffInfo doctor =
                staff(
                        doctorId,
                        "doctor",
                        "Doctor",
                        SystemRole.DOCTOR
                );

        ServiceCapability capability =
                activeCapability(
                        capabilityId
                );

        Department department =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        department.setHeadDoctor(
                doctor
        );

        DepartmentUpdateRequest req =
                partialUpdate();

        when(req.capabilityIds())
                .thenReturn(
                        List.of(capabilityId)
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                capabilityRepo.findAllById(any())
        ).thenReturn(
                List.of(capability)
        );

        when(
                staffCapabilityRepo
                        .existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(
                                doctorId,
                                capabilityId,
                                StaffCapabilityStatus.ACTIVE
                        )
        ).thenReturn(true);

        when(repo.save(department))
                .thenReturn(department);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        when(staffRepo.findById(doctorId)).thenReturn(Optional.of(doctor));

        departmentService.update(
                id,
                req
        );

        verify(repo)
                .save(department);
    }


    // =========================================================
    // UPDATE STATUS
    // =========================================================

    @Test
    void updateStatus_ShouldChangeStatus_WhenStatusProvided() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                repo.countOpenQueueTickets(id)
        ).thenReturn(0L);

        when(
                repo.countOpenTestRequests(id)
        ).thenReturn(0L);

        when(repo.save(department))
                .thenReturn(department);

        departmentService.updateStatus(
                id,
                DepartmentStatus.MAINTENANCE
        );

        assertEquals(
                DepartmentStatus.MAINTENANCE,
                department.getStatus()
        );
    }


    @Test
    void updateStatus_ShouldRejectMaintenance_WhenOpenQueueExists() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(
                repo.countOpenQueueTickets(id)
        ).thenReturn(1L);

        when(
                repo.countOpenTestRequests(id)
        ).thenReturn(0L);

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.updateStatus(
                                id,
                                DepartmentStatus.MAINTENANCE
                        )
        );

        verify(
                repo,
                never()
        ).save(any());
    }


    @Test
    void updateStatus_ShouldKeepOldStatus_WhenStatusNull() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(department)
        );

        when(repo.save(department))
                .thenReturn(department);

        departmentService.updateStatus(
                id,
                null
        );

        assertEquals(
                DepartmentStatus.AVAILABLE,
                department.getStatus()
        );
    }


    // =========================================================
    // DELETE - MISSING
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenDepartmentMissing() {

        UUID id =
                UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService.delete(id)
        );

        verify(
                repo,
                never()
        ).delete(any(Department.class));
    }


    // =========================================================
    // DELETE - OPERATIONAL REFERENCES
    // =========================================================

    @Test
    void delete_ShouldReject_WhenOperationalReferencesExist() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        when(
                repo.countOperationalReferences(id)
        ).thenReturn(2L);

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.delete(id)
        );

        verify(
                repo,
                never()
        ).delete(any(Department.class));
    }


    // =========================================================
    // DELETE - CONFIGURATION REFERENCES
    // =========================================================

    @Test
    void delete_ShouldReject_WhenConfigurationReferencesExist() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        when(
                repo.countOperationalReferences(id)
        ).thenReturn(0L);

        when(
                repo.countActiveConfigurationReferences(id)
        ).thenReturn(3L);

        assertThrows(
                ConflictException.class,
                () ->
                        departmentService.delete(id)
        );

        verify(
                repo,
                never()
        ).delete(any(Department.class));
    }


    // =========================================================
    // DELETE SUCCESS
    // =========================================================

    @Test
    void delete_ShouldDelete_WhenDepartmentHasNoReferences() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(department)
                );

        when(
                repo.countOperationalReferences(id)
        ).thenReturn(0L);

        when(
                repo.countActiveConfigurationReferences(id)
        ).thenReturn(0L);

        departmentService.delete(id);

        assertTrue(
                department.getCapabilities()
                        .isEmpty()
        );

        verify(repo)
                .saveAndFlush(department);

        verify(repo)
                .delete(department);

        verify(
                repo,
                never()
        ).deleteById(any());
    }


    // =========================================================
    // FIND WITH HEAD DOCTOR
    // =========================================================

    @Test
    void findByIdWithHeadDoctor_ShouldReturn_WhenFound() {

        UUID id =
                UUID.randomUUID();

        Department department =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        when(
                repo.findWithHeadDoctorById(id)
        ).thenReturn(
                Optional.of(department)
        );

        assertSame(
                department,
                departmentService
                        .findByIdWithHeadDoctor(id)
        );
    }


    @Test
    void findByIdWithHeadDoctor_ShouldThrow_WhenMissing() {

        UUID id =
                UUID.randomUUID();

        when(
                repo.findWithHeadDoctorById(id)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService
                                .findByIdWithHeadDoctor(id)
        );
    }


    // =========================================================
    // GET MY DEPARTMENT - STAFF MISSING
    // =========================================================

    @Test
    void getMyDepartment_ShouldThrow_WhenCurrentAccountIsNotStaff() {

        Account account =
                Account.builder()
                        .username("user01")
                        .build();

        when(
                authService.currentAccount()
        ).thenReturn(account);

        when(
                staffRepo
                        .findFirstByProfile_Account_Username(
                                "user01"
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService
                                .getMyDepartment()
        );
    }


    // =========================================================
    // GET MY DEPARTMENT - HEAD DOCTOR
    // =========================================================

    @Test
    void getMyDepartment_ShouldReturnDepartment_WhenStaffIsHeadDoctor() {

        UUID staffId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .username("doctor01")
                        .build();

        StaffInfo staff =
                staff(
                        staffId,
                        "doctor01",
                        "Doctor",
                        SystemRole.DOCTOR
                );

        Department department =
                department(
                        UUID.randomUUID(),
                        "P",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        when(
                authService.currentAccount()
        ).thenReturn(account);

        when(
                staffRepo
                        .findFirstByProfile_Account_Username(
                                "doctor01"
                        )
        ).thenReturn(
                Optional.of(staff)
        );

        staff.setDepartment(department);

        var result =
                departmentService
                        .getMyDepartment();

        assertNotNull(result);

        verify(
                repo,
                never()
        ).findFirstByNurses_StaffId(any());
    }


    // =========================================================
    // GET MY DEPARTMENT - NURSE
    // =========================================================

    @Test
    void getMyDepartment_ShouldReturnAssignedNurseDepartment() {

        UUID staffId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .username("nurse01")
                        .build();

        StaffInfo staff =
                staff(
                        staffId,
                        "nurse01",
                        "Nurse",
                        SystemRole.NURSE
                );

        Department department =
                department(
                        UUID.randomUUID(),
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        when(
                authService.currentAccount()
        ).thenReturn(account);

        when(
                staffRepo
                        .findFirstByProfile_Account_Username(
                                "nurse01"
                        )
        ).thenReturn(
                Optional.of(staff)
        );



        staff.setDepartment(department);

        assertNotNull(
                departmentService
                        .getMyDepartment()
        );
    }


    // =========================================================
    // GET MY DEPARTMENT - NO ASSIGNMENT
    // =========================================================

    @Test
    void getMyDepartment_ShouldThrow_WhenStaffHasNoDepartment() {

        UUID staffId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .username("staff01")
                        .build();

        StaffInfo staff =
                staff(
                        staffId,
                        "staff01",
                        "Staff",
                        SystemRole.NURSE
                );

        when(
                authService.currentAccount()
        ).thenReturn(account);

        when(
                staffRepo
                        .findFirstByProfile_Account_Username(
                                "staff01"
                        )
        ).thenReturn(
                Optional.of(staff)
        );





        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        departmentService
                                .getMyDepartment()
        );
    }
}
