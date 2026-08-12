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
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    @InjectMocks
    private DepartmentService departmentService;


    // =========================================================
    // HELPERS
    // =========================================================

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
                .capabilities(new HashSet<>())
                .build();
    }

    private StaffInfo staff(
            UUID id,
            String username,
            String fullName,
            SystemRole role
    ) {
        Account account = Account.builder()
                .accountId(UUID.randomUUID())
                .username(username)
                .build();

        Profile profile = Profile.builder()
                .profileId(UUID.randomUUID())
                .account(account)
                .fullName(fullName)
                .build();

        return StaffInfo.builder()
                .staffId(id)
                .staffCode("STF-" + id.toString().substring(0, 4))
                .profile(profile)
                .systemRole(role)
                .build();
    }


    // =========================================================
    // LIST ALL
    // =========================================================

    @Test
    void listAll_ShouldReturnMappedPage() {

        var pageable = PageRequest.of(0, 10);

        Department d = department(
                UUID.randomUUID(),
                "P101",
                "Phong 101",
                DepartmentType.EXAMINATION
        );

        when(repo.findAllWithHeadDoctor(pageable))
                .thenReturn(new PageImpl<>(List.of(d)));

        var result = departmentService.listAll(pageable);

        assertNotNull(result);

        verify(repo)
                .findAllWithHeadDoctor(pageable);
    }


    // =========================================================
    // LIST BY TYPE
    // =========================================================

    @Test
    void list_ShouldReturnDepartmentsByType() {

        var pageable = PageRequest.of(0, 10);

        Department d = department(
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
                new PageImpl<>(List.of(d))
        );

        var result = departmentService.list(
                DepartmentType.EXAMINATION,
                pageable
        );

        assertNotNull(result);
    }


    // =========================================================
    // LIST MULTIPLE
    // =========================================================

    @Test
    void listMultiple_ShouldReturnDepartmentsByTypes() {

        var pageable = PageRequest.of(0, 10);

        List<DepartmentType> types = List.of(
                DepartmentType.EXAMINATION,
                DepartmentType.PARACLINICAL
        );

        when(
                repo.findAllByDepartmentTypeIn(
                        types,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of())
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
    // FIND / GET
    // =========================================================

    @Test
    void findById_ShouldReturnDepartment() {

        UUID id = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "P101",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        assertSame(
                d,
                departmentService.findById(id)
        );
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService.findById(id)
        );
    }


    @Test
    void get_ShouldReturnResponse() {

        UUID id = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "P101",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        assertNotNull(
                departmentService.get(id)
        );
    }


    // =========================================================
    // CREATE - DUPLICATE ROOM
    // =========================================================

    @Test
    void create_ShouldRejectDuplicateRoomCode() {

        DepartmentCreateRequest req =
                mock(DepartmentCreateRequest.class);

        when(req.roomCode())
                .thenReturn("P101");

        when(repo.existsByRoomCode("P101"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> departmentService.create(req)
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // CREATE - DUPLICATE NAME
    // =========================================================

    @Test
    void create_ShouldRejectDuplicateName() {

        DepartmentCreateRequest req =
                mock(DepartmentCreateRequest.class);

        when(req.roomCode())
                .thenReturn("P101");

        when(req.name())
                .thenReturn("Khoa Noi");

        when(repo.existsByRoomCode("P101"))
                .thenReturn(false);

        when(repo.existsByName("Khoa Noi"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - DEFAULT TYPE = EXAMINATION
    // =========================================================

    @Test
    void create_ShouldRejectWhenDefaultExaminationHasNoSpecialization() {

        DepartmentCreateRequest req =
                mock(DepartmentCreateRequest.class);

        when(req.roomCode())
                .thenReturn("P101");

        when(req.name())
                .thenReturn("Phong");

        when(repo.existsByRoomCode(anyString()))
                .thenReturn(false);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        /*
         * departmentType() mặc định null từ Mockito
         * => service dùng EXAMINATION
         * specializationId() cũng null
         */
        assertThrows(
                BadRequestException.class,
                () -> departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - SPECIALIZATION MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenSpecializationMissing() {

        UUID specializationId =
                UUID.randomUUID();

        DepartmentCreateRequest req =
                mock(DepartmentCreateRequest.class);

        when(req.roomCode())
                .thenReturn("P101");

        when(req.name())
                .thenReturn("Khoa");

        when(req.departmentType())
                .thenReturn(DepartmentType.EXAMINATION);

        when(req.specializationId())
                .thenReturn(specializationId);

        when(repo.existsByRoomCode(anyString()))
                .thenReturn(false);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService.create(req)
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

        DepartmentCreateRequest req =
                mock(DepartmentCreateRequest.class);

        when(req.roomCode()).thenReturn("P101");
        when(req.name()).thenReturn("Phong");

        when(req.departmentType())
                .thenReturn(DepartmentType.EXAMINATION);

        when(req.specializationId())
                .thenReturn(specializationId);

        when(req.headDoctorId())
                .thenReturn(doctorId);

        when(repo.existsByRoomCode(anyString()))
                .thenReturn(false);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(specializationRepo.findById(specializationId))
                .thenReturn(Optional.of(mock(Specialization.class)));

        when(staffRepo.findById(doctorId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - HEAD DOCTOR ALREADY USED
    // =========================================================

    @Test
    void create_ShouldReject_WhenHeadDoctorAlreadyManagesAnotherRoom() {

        UUID specId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        DepartmentCreateRequest req =
                mock(DepartmentCreateRequest.class);

        when(req.roomCode()).thenReturn("P1");
        when(req.name()).thenReturn("Phong 1");

        when(req.departmentType())
                .thenReturn(DepartmentType.EXAMINATION);

        when(req.specializationId())
                .thenReturn(specId);

        when(req.headDoctorId())
                .thenReturn(doctorId);

        when(repo.existsByRoomCode(anyString()))
                .thenReturn(false);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(specializationRepo.findById(specId))
                .thenReturn(Optional.of(mock(Specialization.class)));

        when(staffRepo.findById(doctorId))
                .thenReturn(
                        Optional.of(
                                staff(
                                        doctorId,
                                        "doctor",
                                        "Doctor",
                                        SystemRole.DOCTOR
                                )
                        )
                );

        when(repo.existsByHeadDoctor_StaffId(doctorId))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - CAPABILITY MISSING
    // =========================================================

    @Test
    void create_ShouldReject_WhenCapabilityIdsContainMissingCapability() {

        UUID capability1 = UUID.randomUUID();
        UUID capability2 = UUID.randomUUID();

        DepartmentCreateRequest req =
                mock(DepartmentCreateRequest.class);

        when(req.roomCode()).thenReturn("LAB01");
        when(req.name()).thenReturn("Phong XN");

        when(req.departmentType())
                .thenReturn(DepartmentType.PARACLINICAL);

        when(req.capabilityIds())
                .thenReturn(
                        List.of(
                                capability1,
                                capability2
                        )
                );

        when(repo.existsByRoomCode(anyString()))
                .thenReturn(false);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(capabilityRepo.findAllById(any()))
                .thenReturn(
                        List.of(
                                mock(ServiceCapability.class)
                        )
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - HEAD DOCTOR DOES NOT MATCH CAPABILITY
    // =========================================================

    @Test
    void create_ShouldReject_WhenHeadDoctorHasNoMatchingCapability() {

        UUID doctorId = UUID.randomUUID();
        UUID capabilityId = UUID.randomUUID();

        StaffInfo doctor =
                staff(
                        doctorId,
                        "doctor01",
                        "Doctor",
                        SystemRole.DOCTOR
                );

        ServiceCapability capability =
                mock(ServiceCapability.class);

        when(capability.getCapabilityId())
                .thenReturn(capabilityId);

        DepartmentCreateRequest req =
                mock(DepartmentCreateRequest.class);

        when(req.roomCode()).thenReturn("LAB01");
        when(req.name()).thenReturn("Lab");

        when(req.departmentType())
                .thenReturn(DepartmentType.PARACLINICAL);

        when(req.headDoctorId())
                .thenReturn(doctorId);

        when(req.capabilityIds())
                .thenReturn(List.of(capabilityId));

        when(repo.existsByRoomCode(anyString()))
                .thenReturn(false);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(staffRepo.findById(doctorId))
                .thenReturn(Optional.of(doctor));

        when(repo.existsByHeadDoctor_StaffId(doctorId))
                .thenReturn(false);

        when(capabilityRepo.findAllById(any()))
                .thenReturn(List.of(capability));

        when(
                staffCapabilityRepo
                        .existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(
                                doctorId,
                                capabilityId,
                                StaffCapabilityStatus.ACTIVE
                        )
        ).thenReturn(false);

        assertThrows(
                BadRequestException.class,
                () -> departmentService.create(req)
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // CREATE SUCCESS - PARACLINICAL + CAPABILITY + DOCTOR
    // =========================================================

    @Test
    void create_ShouldCreateParaclinicalRoom_WhenDoctorCapabilityMatches() {

        UUID id = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID capabilityId = UUID.randomUUID();

        StaffInfo doctor =
                staff(
                        doctorId,
                        "doctor01",
                        "Doctor",
                        SystemRole.DOCTOR
                );

        ServiceCapability capability =
                mock(ServiceCapability.class);

        when(capability.getCapabilityId())
                .thenReturn(capabilityId);

        DepartmentCreateRequest req =
                mock(DepartmentCreateRequest.class);

        when(req.roomCode())
                .thenReturn("LAB01");

        when(req.name())
                .thenReturn("Phong XN");

        when(req.description())
                .thenReturn("Mo ta");

        when(req.departmentType())
                .thenReturn(DepartmentType.PARACLINICAL);

        when(req.headDoctorId())
                .thenReturn(doctorId);

        when(req.capabilityIds())
                .thenReturn(List.of(capabilityId));

        when(repo.existsByRoomCode("LAB01"))
                .thenReturn(false);

        when(repo.existsByName("Phong XN"))
                .thenReturn(false);

        when(staffRepo.findById(doctorId))
                .thenReturn(Optional.of(doctor));

        when(repo.existsByHeadDoctor_StaffId(doctorId))
                .thenReturn(false);

        when(capabilityRepo.findAllById(any()))
                .thenReturn(List.of(capability));

        when(
                staffCapabilityRepo
                        .existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(
                                doctorId,
                                capabilityId,
                                StaffCapabilityStatus.ACTIVE
                        )
        ).thenReturn(true);

        when(repo.save(any(Department.class)))
                .thenAnswer(invocation -> {
                    Department d = invocation.getArgument(0);
                    d.setDepartmentId(id);
                    return d;
                });

        when(repo.findById(id))
                .thenAnswer(invocation ->
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
                .save(argThat(d ->
                        d.getHeadDoctor() == doctor
                                && d.getCapabilities().contains(capability)
                                && d.getDepartmentType()
                                == DepartmentType.PARACLINICAL
                                && d.getStatus()
                                == DepartmentStatus.AVAILABLE
                ));
    }


    // =========================================================
    // CREATE - NURSE MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenNurseMissing() {

        UUID id = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();

        DepartmentCreateRequest req =
                mock(DepartmentCreateRequest.class);

        when(req.roomCode()).thenReturn("LAB01");
        when(req.name()).thenReturn("Lab");

        when(req.departmentType())
                .thenReturn(DepartmentType.PARACLINICAL);

        when(req.nurseIds())
                .thenReturn(List.of(nurseId));

        when(repo.existsByRoomCode(anyString()))
                .thenReturn(false);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(repo.save(any(Department.class)))
                .thenAnswer(invocation -> {
                    Department d = invocation.getArgument(0);
                    d.setDepartmentId(id);
                    return d;
                });

        when(staffRepo.findById(nurseId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - NURSE ALREADY ASSIGNED
    // =========================================================

    @Test
    void create_ShouldReject_WhenNurseAlreadyAssignedToRoom() {

        UUID id = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();

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
                mock(DepartmentCreateRequest.class);

        when(req.roomCode()).thenReturn("LAB");
        when(req.name()).thenReturn("Lab");

        when(req.departmentType())
                .thenReturn(DepartmentType.PARACLINICAL);

        when(req.nurseIds())
                .thenReturn(List.of(nurseId));

        when(repo.existsByRoomCode(anyString()))
                .thenReturn(false);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(repo.save(any()))
                .thenAnswer(invocation -> {
                    Department d = invocation.getArgument(0);
                    d.setDepartmentId(id);
                    return d;
                });

        when(staffRepo.findById(nurseId))
                .thenReturn(Optional.of(nurse));

        assertThrows(
                ConflictException.class,
                () -> departmentService.create(req)
        );
    }


    // =========================================================
    // CREATE SUCCESS WITH NURSE
    // =========================================================

    @Test
    void create_ShouldAssignNursesToSavedDepartment() {

        UUID id = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();

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
                mock(DepartmentCreateRequest.class);

        when(req.roomCode()).thenReturn("LAB");
        when(req.name()).thenReturn("Lab");

        when(req.departmentType())
                .thenReturn(DepartmentType.PARACLINICAL);

        when(req.nurseIds())
                .thenReturn(List.of(nurseId));

        when(repo.existsByRoomCode(anyString()))
                .thenReturn(false);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(repo.save(any()))
                .thenReturn(saved);

        when(staffRepo.findById(nurseId))
                .thenReturn(Optional.of(nurse));

        when(repo.findById(id))
                .thenReturn(Optional.of(saved));

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
    // UPDATE - DUPLICATE ROOM
    // =========================================================

    @Test
    void update_ShouldRejectDuplicateNewRoomCode() {

        UUID id = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "OLD",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.roomCode())
                .thenReturn("NEW");

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(repo.existsByRoomCode("NEW"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> departmentService.update(
                        id,
                        req
                )
        );
    }


    // =========================================================
    // UPDATE - SAME ROOM DOES NOT CHECK DUPLICATE
    // =========================================================

    @Test
    void update_ShouldNotCheckDuplicate_WhenRoomCodeUnchanged() {

        UUID id = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "ROOM",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.roomCode())
                .thenReturn("ROOM");

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(repo.save(d))
                .thenReturn(d);

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        departmentService.update(id, req);

        verify(repo, never())
                .existsByRoomCode(anyString());
    }


    // =========================================================
    // UPDATE - DUPLICATE NAME
    // =========================================================

    @Test
    void update_ShouldRejectDuplicateNewName() {

        UUID id = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "P",
                        "Old",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.name())
                .thenReturn("New");

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(repo.existsByName("New"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> departmentService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE BASIC FIELDS
    // =========================================================

    @Test
    void update_ShouldUpdateBasicFields() {

        UUID id = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "OLD",
                        "Old",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.roomCode()).thenReturn("NEW");
        when(req.name()).thenReturn("New");
        when(req.description()).thenReturn("New description");

        when(req.status())
                .thenReturn(DepartmentStatus.MAINTENANCE);

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(repo.existsByRoomCode("NEW"))
                .thenReturn(false);

        when(repo.existsByName("New"))
                .thenReturn(false);

        when(repo.save(d))
                .thenReturn(d);

        departmentService.update(id, req);

        assertEquals("NEW", d.getRoomCode());
        assertEquals("New", d.getName());
        assertEquals(
                "New description",
                d.getDescription()
        );

        assertEquals(
                DepartmentStatus.MAINTENANCE,
                d.getStatus()
        );
    }


    // =========================================================
    // UPDATE TYPE - CLEAR SPECIALIZATION
    // =========================================================

    @Test
    void update_ShouldNormalizeTypeAndClearSpecialization_WhenNotExamination() {

        UUID id = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        d.setSpecialization(
                mock(Specialization.class)
        );

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.departmentType())
                .thenReturn(DepartmentType.LABORATORY);

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(repo.save(d))
                .thenReturn(d);

        departmentService.update(id, req);

        assertEquals(
                DepartmentType.PARACLINICAL,
                d.getDepartmentType()
        );

        assertNull(
                d.getSpecialization()
        );
    }


    // =========================================================
    // UPDATE SPECIALIZATION MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenNewSpecializationMissing() {

        UUID id = UUID.randomUUID();
        UUID specializationId = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.specializationId())
                .thenReturn(specializationId);

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(specializationRepo.findById(specializationId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE SPECIALIZATION SUCCESS
    // =========================================================

    @Test
    void update_ShouldSetSpecialization() {

        UUID id = UUID.randomUUID();
        UUID specializationId = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        Specialization specialization =
                mock(Specialization.class);

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.specializationId())
                .thenReturn(specializationId);

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(specializationRepo.findById(specializationId))
                .thenReturn(Optional.of(specialization));

        when(repo.save(d))
                .thenReturn(d);

        departmentService.update(id, req);

        assertSame(
                specialization,
                d.getSpecialization()
        );
    }


    // =========================================================
    // UPDATE CAPABILITY IDS MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenSomeCapabilitiesDoNotExist() {

        UUID id = UUID.randomUUID();

        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.capabilityIds())
                .thenReturn(List.of(c1, c2));

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(capabilityRepo.findAllById(any()))
                .thenReturn(
                        List.of(
                                mock(ServiceCapability.class)
                        )
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE HEAD DOCTOR - CONFLICT
    // =========================================================

    @Test
    void update_ShouldRejectHeadDoctorAssignedToDifferentDepartment() {

        UUID id = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.headDoctorId())
                .thenReturn(doctorId);

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(repo.existsByHeadDoctor_StaffId(doctorId))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> departmentService.update(
                        id,
                        req
                )
        );
    }


    // =========================================================
    // UPDATE SAME HEAD DOCTOR IS ALLOWED
    // =========================================================

    @Test
    void update_ShouldAllowExistingHeadDoctorOfSameDepartment() {

        UUID id = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        StaffInfo doctor =
                staff(
                        doctorId,
                        "doctor",
                        "Doctor",
                        SystemRole.DOCTOR
                );

        Department d =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        d.setHeadDoctor(doctor);

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.headDoctorId())
                .thenReturn(doctorId);

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(repo.existsByHeadDoctor_StaffId(doctorId))
                .thenReturn(true);

        when(staffRepo.findById(doctorId))
                .thenReturn(Optional.of(doctor));

        when(repo.save(d))
                .thenReturn(d);

        departmentService.update(id, req);

        assertSame(
                doctor,
                d.getHeadDoctor()
        );
    }


    // =========================================================
    // UPDATE HEAD DOCTOR MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenNewHeadDoctorMissing() {

        UUID id = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.headDoctorId())
                .thenReturn(doctorId);

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(repo.existsByHeadDoctor_StaffId(doctorId))
                .thenReturn(false);

        when(staffRepo.findById(doctorId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE NURSES - REMOVE OLD
    // =========================================================

    @Test
    void update_ShouldRemoveOldNurseNotInRequest() {

        UUID id = UUID.randomUUID();
        UUID oldNurseId = UUID.randomUUID();

        Department d =
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

        oldNurse.setDepartment(d);

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.nurseIds())
                .thenReturn(List.of());

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(
                staffRepo.findByDepartment_DepartmentId(id)
        ).thenReturn(
                List.of(oldNurse)
        );

        when(repo.save(d))
                .thenReturn(d);

        departmentService.update(id, req);

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

        UUID id = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();

        Department d =
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

        nurse.setDepartment(d);

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.nurseIds())
                .thenReturn(List.of(nurseId));

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(
                staffRepo.findByDepartment_DepartmentId(id)
        ).thenReturn(
                List.of(nurse)
        );

        when(staffRepo.findById(nurseId))
                .thenReturn(Optional.of(nurse));

        when(repo.save(d))
                .thenReturn(d);

        departmentService.update(id, req);

        assertSame(
                d,
                nurse.getDepartment()
        );
    }


    // =========================================================
    // UPDATE NURSE MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenRequestedNurseMissing() {

        UUID id = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.nurseIds())
                .thenReturn(List.of(nurseId));

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(
                staffRepo.findByDepartment_DepartmentId(id)
        ).thenReturn(List.of());

        when(staffRepo.findById(nurseId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE NURSE BELONGS TO ANOTHER ROOM
    // =========================================================

    @Test
    void update_ShouldRejectNurseBelongingToAnotherDepartment() {

        UUID id = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();

        Department d =
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

        nurse.setDepartment(other);

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.nurseIds())
                .thenReturn(List.of(nurseId));

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(
                staffRepo.findByDepartment_DepartmentId(id)
        ).thenReturn(List.of());

        when(staffRepo.findById(nurseId))
                .thenReturn(Optional.of(nurse));

        assertThrows(
                ConflictException.class,
                () -> departmentService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE CAPABILITY VALIDATION FAIL
    // =========================================================

    @Test
    void update_ShouldReject_WhenHeadDoctorNoLongerMatchesCapabilities() {

        UUID id = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID capabilityId = UUID.randomUUID();

        StaffInfo doctor =
                staff(
                        doctorId,
                        "doctor",
                        "Doctor",
                        SystemRole.DOCTOR
                );

        ServiceCapability capability =
                mock(ServiceCapability.class);

        when(capability.getCapabilityId())
                .thenReturn(capabilityId);

        Department d =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        d.setHeadDoctor(doctor);

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.capabilityIds())
                .thenReturn(List.of(capabilityId));

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(capabilityRepo.findAllById(any()))
                .thenReturn(List.of(capability));

        when(
                staffCapabilityRepo
                        .existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(
                                doctorId,
                                capabilityId,
                                StaffCapabilityStatus.ACTIVE
                        )
        ).thenReturn(false);

        assertThrows(
                BadRequestException.class,
                () -> departmentService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE CAPABILITY VALIDATION SUCCESS
    // =========================================================

    @Test
    void update_ShouldSave_WhenHeadDoctorMatchesAtLeastOneCapability() {

        UUID id = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID capabilityId = UUID.randomUUID();

        StaffInfo doctor =
                staff(
                        doctorId,
                        "doctor",
                        "Doctor",
                        SystemRole.DOCTOR
                );

        ServiceCapability capability =
                mock(ServiceCapability.class);

        when(capability.getCapabilityId())
                .thenReturn(capabilityId);

        Department d =
                department(
                        id,
                        "LAB",
                        "Lab",
                        DepartmentType.PARACLINICAL
                );

        d.setHeadDoctor(doctor);

        DepartmentUpdateRequest req =
                mock(DepartmentUpdateRequest.class);

        when(req.capabilityIds())
                .thenReturn(List.of(capabilityId));

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(capabilityRepo.findAllById(any()))
                .thenReturn(List.of(capability));

        when(
                staffCapabilityRepo
                        .existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(
                                doctorId,
                                capabilityId,
                                StaffCapabilityStatus.ACTIVE
                        )
        ).thenReturn(true);

        when(repo.save(d))
                .thenReturn(d);

        departmentService.update(id, req);

        verify(repo)
                .save(d);
    }


    // =========================================================
    // UPDATE STATUS
    // =========================================================

    @Test
    void updateStatus_ShouldChangeStatus_WhenStatusProvided() {

        UUID id = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(repo.save(d))
                .thenReturn(d);

        departmentService.updateStatus(
                id,
                DepartmentStatus.MAINTENANCE
        );

        assertEquals(
                DepartmentStatus.MAINTENANCE,
                d.getStatus()
        );
    }


    @Test
    void updateStatus_ShouldKeepOldStatus_WhenStatusNull() {

        UUID id = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(d));

        when(repo.save(d))
                .thenReturn(d);

        departmentService.updateStatus(
                id,
                null
        );

        assertEquals(
                DepartmentStatus.AVAILABLE,
                d.getStatus()
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenDepartmentMissing() {

        UUID id = UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService.delete(id)
        );

        verify(repo, never())
                .deleteById(id);
    }


    @Test
    void delete_ShouldDelete_WhenDepartmentExists() {

        UUID id = UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(true);

        departmentService.delete(id);

        verify(repo)
                .deleteById(id);
    }


    // =========================================================
    // FIND WITH HEAD DOCTOR
    // =========================================================

    @Test
    void findByIdWithHeadDoctor_ShouldReturn_WhenFound() {

        UUID id = UUID.randomUUID();

        Department d =
                department(
                        id,
                        "P",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        when(repo.findWithHeadDoctorById(id))
                .thenReturn(Optional.of(d));

        assertSame(
                d,
                departmentService
                        .findByIdWithHeadDoctor(id)
        );
    }


    @Test
    void findByIdWithHeadDoctor_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findWithHeadDoctorById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService
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

        when(authService.currentAccount())
                .thenReturn(account);

        when(
                staffRepo
                        .findFirstByProfile_Account_Username(
                                "user01"
                        )
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService
                        .getMyDepartment()
        );
    }


    // =========================================================
    // GET MY DEPARTMENT - HEAD DOCTOR
    // =========================================================

    @Test
    void getMyDepartment_ShouldReturnDepartment_WhenStaffIsHeadDoctor() {

        UUID staffId = UUID.randomUUID();

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

        Department d =
                department(
                        UUID.randomUUID(),
                        "P",
                        "Phong",
                        DepartmentType.EXAMINATION
                );

        when(authService.currentAccount())
                .thenReturn(account);

        when(
                staffRepo
                        .findFirstByProfile_Account_Username(
                                "doctor01"
                        )
        ).thenReturn(Optional.of(staff));

        when(
                repo.findByHeadDoctor_StaffId(staffId)
        ).thenReturn(Optional.of(d));

        var result =
                departmentService.getMyDepartment();

        assertNotNull(result);

        verify(repo, never())
                .findFirstByNurses_StaffId(any());
    }


    // =========================================================
    // GET MY DEPARTMENT - NURSE FALLBACK
    // =========================================================

    @Test
    void getMyDepartment_ShouldFallbackToNurseDepartment() {

        UUID staffId = UUID.randomUUID();

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

        Department d =
                department(
                        UUID.randomUUID(),
                        "P",
                        "Phong",
                        DepartmentType.PARACLINICAL
                );

        when(authService.currentAccount())
                .thenReturn(account);

        when(
                staffRepo
                        .findFirstByProfile_Account_Username(
                                "nurse01"
                        )
        ).thenReturn(Optional.of(staff));

        when(
                repo.findByHeadDoctor_StaffId(staffId)
        ).thenReturn(Optional.empty());

        when(
                repo.findFirstByNurses_StaffId(staffId)
        ).thenReturn(Optional.of(d));

        assertNotNull(
                departmentService.getMyDepartment()
        );
    }


    // =========================================================
    // GET MY DEPARTMENT - NO ASSIGNMENT
    // =========================================================

    @Test
    void getMyDepartment_ShouldThrow_WhenStaffHasNoDepartment() {

        UUID staffId = UUID.randomUUID();

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

        when(authService.currentAccount())
                .thenReturn(account);

        when(
                staffRepo
                        .findFirstByProfile_Account_Username(
                                "staff01"
                        )
        ).thenReturn(Optional.of(staff));

        when(
                repo.findByHeadDoctor_StaffId(staffId)
        ).thenReturn(Optional.empty());

        when(
                repo.findFirstByNurses_StaffId(staffId)
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService.getMyDepartment()
        );
    }
}