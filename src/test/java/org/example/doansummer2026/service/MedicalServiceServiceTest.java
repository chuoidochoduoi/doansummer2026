package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.ServiceStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.dto.medicalService.MedicalServiceCreateRequest;
import org.example.doansummer2026.dto.medicalService.MedicalServiceUpdateRequest;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.Specialization;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ServiceCategoryRepository;
import org.example.doansummer2026.repository.SpecializationRepository;
import org.example.doansummer2026.repository.ServiceCapabilityRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalServiceServiceTest {

    @Mock
    private MedicalServiceRepository repo;

    @Mock
    private ServiceCategoryRepository categoryRepo;

    @Mock
    private SpecializationRepository specializationRepo;

    @Mock
    private ServiceCapabilityRepository capabilityRepo;

    @InjectMocks
    private MedicalServiceService medicalServiceService;


    // =========================================================
    // HELPERS
    // =========================================================

    private MedicalService service(
            UUID id,
            String name,
            DepartmentType type,
            ServiceStatus status
    ) {

        return MedicalService.builder()
                .serviceId(id)
                .serviceCode("DV001")
                .name(name)
                .description("Mo ta")
                .departmentType(type)
                .price(new BigDecimal("100000"))
                .status(status)
                .isPointOfCare(false)
                .durationMinutes(15)
                .workflowPriority(1)
                .requiresDoctorOrder(false)
                .requiresReturnToDoctor(false)
                .resultWaitMinutes(0)
                .allowCustomerBooking(true)
                .minimumAge(0)
                .maximumAge(120)
                .build();
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void search_ShouldReturnMappedPage() {

        var pageable = PageRequest.of(0, 10);

        MedicalService s =
                service(
                        UUID.randomUUID(),
                        "Kham tong quat",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.ACTIVE
                );

        when(
                repo.search(
                        "kham",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.ACTIVE,
                        null,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of(s))
        );

        var result =
                medicalServiceService.search(
                        "kham",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.ACTIVE,
                        null,
                        pageable
                );

        assertNotNull(result);

        verify(repo).search(
                "kham",
                DepartmentType.EXAMINATION,
                ServiceStatus.ACTIVE,
                null,
                pageable
        );
    }


    @Test
    void search_ShouldReturnEmptyPage() {

        var pageable = PageRequest.of(0, 10);

        when(
                repo.search(
                        null,
                        null,
                        null,
                        null,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        assertNotNull(
                medicalServiceService.search(
                        null,
                        null,
                        null,
                        null,
                        pageable
                )
        );
    }


    // =========================================================
    // AVAILABLE
    // =========================================================

    @Test
    void listAvailable_ShouldReturnMappedPage() {

        var pageable = PageRequest.of(0, 10);

        MedicalService s =
                service(
                        UUID.randomUUID(),
                        "Kham noi",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.ACTIVE
                );

        when(
                repo.searchCustomerBookable(
                        "noi",
                        DepartmentType.EXAMINATION,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of(s))
        );

        var result =
                medicalServiceService.listAvailable(
                        "noi",
                        DepartmentType.EXAMINATION,
                        pageable
                );

        assertNotNull(result);

        verify(repo)
                .searchCustomerBookable(
                        "noi",
                        DepartmentType.EXAMINATION,
                        pageable
                );
    }


    // =========================================================
    // GET / FIND
    // =========================================================

    @Test
    void findById_ShouldReturnService_WhenFound() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        assertSame(
                s,
                medicalServiceService.findById(id)
        );
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalServiceService.findById(id)
        );
    }


    @Test
    void get_ShouldReturnResponse() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        assertNotNull(
                medicalServiceService.get(id)
        );
    }


    // =========================================================
    // STATS
    // =========================================================

    @Test
    void getStats_ShouldReturnAllCounters() {

        when(repo.count())
                .thenReturn(10L);

        when(repo.count(any(Specification.class)))
                .thenReturn(
                        5L,
                        2L,
                        3L
                );

        var result =
                medicalServiceService.getStats();

        assertEquals(10L, result.get("total"));
        assertEquals(5L, result.get("active"));
        assertEquals(2L, result.get("suspended"));
        assertEquals(3L, result.get("draft"));

        verify(repo, times(3))
                .count(any(Specification.class));
    }


    // =========================================================
    // CREATE - INVALID AGE RANGE
    // =========================================================

    @Test
    void create_ShouldReject_WhenMinimumAgeGreaterThanMaximumAge() {

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.minimumAge())
                .thenReturn(70);

        when(req.maximumAge())
                .thenReturn(20);

        assertThrows(
                BadRequestException.class,
                () -> medicalServiceService.create(req)
        );

        verifyNoInteractions(repo);
    }


    // =========================================================
    // CREATE - GENDER OTHER
    // =========================================================

    @Test
    void create_ShouldRejectOtherGender() {

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.allowedGender())
                .thenReturn(Gender.OTHER);

        assertThrows(
                BadRequestException.class,
                () -> medicalServiceService.create(req)
        );
    }


    // =========================================================
    // CREATE - DUPLICATE NAME
    // =========================================================

    @Test
    void create_ShouldRejectDuplicateName() {

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.name())
                .thenReturn("Kham noi");

        when(repo.existsByName("Kham noi"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> medicalServiceService.create(req)
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // CREATE - DUPLICATE CODE
    // =========================================================

    @Test
    void create_ShouldRejectDuplicateServiceCode() {

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.name())
                .thenReturn("Service");

        when(req.serviceCode())
                .thenReturn("DV01");

        when(repo.existsByName("Service"))
                .thenReturn(false);

        when(repo.existsByServiceCode("DV01"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> medicalServiceService.create(req)
        );
    }


    // =========================================================
    // CREATE - EXAMINATION REQUIRES SPECIALIZATION
    // =========================================================

    @Test
    void create_ShouldRejectExaminationWithoutSpecialization() {

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.name())
                .thenReturn("Kham noi");

        when(req.serviceCode())
                .thenReturn("KN01");

        when(req.departmentType())
                .thenReturn(DepartmentType.EXAMINATION);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(repo.existsByServiceCode(anyString()))
                .thenReturn(false);

        assertThrows(
                BadRequestException.class,
                () -> medicalServiceService.create(req)
        );
    }


    // =========================================================
    // CREATE - PARACLINICAL REQUIRES CAPABILITY
    // =========================================================

    @Test
    void create_ShouldRejectLaboratoryWithoutCapability() {

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.name())
                .thenReturn("Xet nghiem");

        when(req.serviceCode())
                .thenReturn("XN01");

        when(req.departmentType())
                .thenReturn(DepartmentType.LABORATORY);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(repo.existsByServiceCode(anyString()))
                .thenReturn(false);

        assertThrows(
                BadRequestException.class,
                () -> medicalServiceService.create(req)
        );
    }


    // =========================================================
    // CREATE - SPECIALIZATION NOT FOUND
    // =========================================================

    @Test
    void create_ShouldThrow_WhenSpecializationMissing() {

        UUID specializationId =
                UUID.randomUUID();

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.name()).thenReturn("Kham");
        when(req.serviceCode()).thenReturn("K01");

        when(req.departmentType())
                .thenReturn(DepartmentType.EXAMINATION);

        when(req.requiredSpecializationId())
                .thenReturn(specializationId);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(repo.existsByServiceCode(anyString()))
                .thenReturn(false);

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalServiceService.create(req)
        );
    }


    // =========================================================
    // CREATE - CAPABILITY NOT FOUND
    // =========================================================

    @Test
    void create_ShouldThrow_WhenCapabilityMissing() {

        UUID capabilityId =
                UUID.randomUUID();

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.name()).thenReturn("XN");
        when(req.serviceCode()).thenReturn("XN01");

        when(req.departmentType())
                .thenReturn(DepartmentType.LABORATORY);

        when(req.requiredCapabilityId())
                .thenReturn(capabilityId);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(repo.existsByServiceCode(anyString()))
                .thenReturn(false);

        when(
                capabilityRepo.findById(capabilityId)
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalServiceService.create(req)
        );
    }


    // =========================================================
    // CREATE EXAMINATION SUCCESS
    // =========================================================

    @Test
    void create_ShouldCreateExaminationServiceSuccessfully() {

        UUID specializationId =
                UUID.randomUUID();

        Specialization specialization =
                mock(Specialization.class);

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.name())
                .thenReturn("Kham Noi");

        when(req.serviceCode())
                .thenReturn("KN01");

        when(req.description())
                .thenReturn("Kham noi tong quat");

        when(req.departmentType())
                .thenReturn(DepartmentType.EXAMINATION);

        when(req.requiredSpecializationId())
                .thenReturn(specializationId);

        when(req.price())
                .thenReturn(new BigDecimal("200000"));

        when(req.status())
                .thenReturn(ServiceStatus.DRAFT);

        when(req.isPointOfCare())
                .thenReturn(true);

        when(req.durationMinutes())
                .thenReturn(30);

        when(req.workflowPriority())
                .thenReturn(2);

        when(req.requiresDoctorOrder())
                .thenReturn(true);

        when(req.requiresReturnToDoctor())
                .thenReturn(true);

        when(req.resultWaitMinutes())
                .thenReturn(10);

        when(req.allowCustomerBooking())
                .thenReturn(false);

        when(req.minimumAge())
                .thenReturn(18);

        when(req.maximumAge())
                .thenReturn(70);

        when(req.allowedGender())
                .thenReturn(Gender.MALE);

        when(repo.existsByName("Kham Noi"))
                .thenReturn(false);

        when(repo.existsByServiceCode("KN01"))
                .thenReturn(false);

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(
                Optional.of(specialization)
        );

        when(repo.save(any(MedicalService.class)))
                .thenAnswer(i -> {
                    MedicalService s = i.getArgument(0);

                    s.setServiceId(UUID.randomUUID());

                    return s;
                });

        var result =
                medicalServiceService.create(req);

        assertNotNull(result);

        verify(repo).save(argThat(s ->
                "KN01".equals(s.getServiceCode())
                        && "Kham Noi".equals(s.getName())
                        && s.getDepartmentType()
                        == DepartmentType.EXAMINATION
                        && s.getRequiredSpecialization()
                        == specialization
                        && s.getDepartment() == null
                        && Boolean.TRUE.equals(
                        s.getIsPointOfCare()
                )
                        && s.getDurationMinutes() == 30
                        && s.getWorkflowPriority() == 2
                        && Boolean.TRUE.equals(
                        s.getRequiresDoctorOrder()
                )
                        && Boolean.TRUE.equals(
                        s.getRequiresReturnToDoctor()
                )
                        && s.getResultWaitMinutes() == 10
                        && Boolean.FALSE.equals(
                        s.getAllowCustomerBooking()
                )
                        && s.getMinimumAge() == 18
                        && s.getMaximumAge() == 70
                        && s.getAllowedGender()
                        == Gender.MALE
        ));
    }


    // =========================================================
    // CREATE DEFAULT VALUES
    // =========================================================

    @Test
    void create_ShouldUseDefaultValues() {

        UUID specializationId = UUID.randomUUID();

        Specialization specialization =
                mock(Specialization.class);

        // =====================================================
        // DÙNG REQUEST THẬT, KHÔNG MOCK
        // Như vậy Integer null thực sự là null
        // =====================================================

        MedicalServiceCreateRequest req =
                new MedicalServiceCreateRequest(
                        "DEF01",                         // serviceCode
                        "Kham mac dinh",                // name
                        null,                            // description
                        DepartmentType.EXAMINATION,      // departmentType
                        BigDecimal.ZERO,                 // price
                        null,                            // status
                        null,                            // isPointOfCare
                        null,                            // durationMinutes -> DEFAULT 15
                        null,                            // workflowPriority -> DEFAULT 1
                        null,                            // requiresDoctorOrder
                        null,                            // requiresReturnToDoctor
                        null,                            // requiresSpecimen
                        null,                            // resultWaitMinutes -> DEFAULT 0
                        null,                            // allowCustomerBooking -> DEFAULT true
                        null,                            // minimumAge -> DEFAULT 0
                        null,                            // maximumAge -> DEFAULT 120
                        null,                            // allowedGender
                        null,                            // departmentId
                        specializationId,                // requiredSpecializationId
                        null                             // requiredCapabilityId
                );

        // =====================================================
        // REPOSITORY
        // =====================================================

        when(repo.existsByName("Kham mac dinh"))
                .thenReturn(false);

        when(repo.existsByServiceCode("DEF01"))
                .thenReturn(false);

        when(specializationRepo.findById(specializationId))
                .thenReturn(Optional.of(specialization));

        when(repo.save(any(MedicalService.class)))
                .thenAnswer(invocation -> {

                    MedicalService entity =
                            invocation.getArgument(0);

                    if (entity.getServiceId() == null) {
                        entity.setServiceId(UUID.randomUUID());
                    }

                    return entity;
                });

        // =====================================================
        // ACT
        // =====================================================

        var result =
                medicalServiceService.create(req);

        assertNotNull(result);

        // =====================================================
        // CAPTURE ENTITY
        // =====================================================

        ArgumentCaptor<MedicalService> captor =
                ArgumentCaptor.forClass(MedicalService.class);

        verify(repo)
                .save(captor.capture());

        MedicalService saved =
                captor.getValue();

        // =====================================================
        // BASIC
        // =====================================================

        assertEquals(
                "DEF01",
                saved.getServiceCode()
        );

        assertEquals(
                "Kham mac dinh",
                saved.getName()
        );

        assertEquals(
                DepartmentType.EXAMINATION,
                saved.getDepartmentType()
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(saved.getPrice())
        );

        // =====================================================
        // DEFAULT VALUES
        // =====================================================

        assertEquals(
                ServiceStatus.DRAFT,
                saved.getStatus()
        );

        assertFalse(
                saved.getIsPointOfCare()
        );

        assertEquals(
                15,
                saved.getDurationMinutes()
        );

        assertEquals(
                1,
                saved.getWorkflowPriority()
        );

        assertFalse(
                saved.getRequiresDoctorOrder()
        );

        assertFalse(
                saved.getRequiresReturnToDoctor()
        );

        assertEquals(
                0,
                saved.getResultWaitMinutes()
        );

        assertTrue(
                saved.getAllowCustomerBooking()
        );

        assertEquals(
                0,
                saved.getMinimumAge()
        );

        assertEquals(
                120,
                saved.getMaximumAge()
        );

        assertNull(
                saved.getAllowedGender()
        );

        // =====================================================
        // SPECIALIZATION / CAPABILITY
        // =====================================================

        assertSame(
                specialization,
                saved.getRequiredSpecialization()
        );

        assertNull(
                saved.getRequiredCapability()
        );

        assertNull(
                saved.getDepartment()
        );

        // =====================================================
        // VERIFY
        // =====================================================

        verify(repo)
                .existsByName("Kham mac dinh");

        verify(repo)
                .existsByServiceCode("DEF01");

        verify(specializationRepo)
                .findById(specializationId);

        verifyNoInteractions(capabilityRepo);
    }


    // =========================================================
    // CREATE PARACLINICAL SUCCESS
    // =========================================================

    @Test
    void create_ShouldCreateLaboratoryServiceWithCapability() {

        UUID capabilityId =
                UUID.randomUUID();

        var capability =
                mock(
                        org.example.doansummer2026.model.ServiceCapability.class
                );

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.name())
                .thenReturn("Cong thuc mau");

        when(req.serviceCode())
                .thenReturn("XN001");

        when(req.departmentType())
                .thenReturn(DepartmentType.LABORATORY);

        when(req.requiredCapabilityId())
                .thenReturn(capabilityId);

        when(repo.existsByName(anyString()))
                .thenReturn(false);

        when(repo.existsByServiceCode(anyString()))
                .thenReturn(false);

        when(capabilityRepo.findById(capabilityId))
                .thenReturn(Optional.of(capability));

        when(repo.save(any()))
                .thenAnswer(i -> {
                    MedicalService s = i.getArgument(0);
                    s.setServiceId(UUID.randomUUID());
                    return s;
                });

        var result =
                medicalServiceService.create(req);

        assertNotNull(result);

        verify(repo).save(argThat(s ->
                s.getDepartmentType() == DepartmentType.PARACLINICAL
                        && s.getRequiredCapability() == capability
                        && s.getRequiredSpecialization() == null
        ));
    }


    // =========================================================
    // UPDATE - INVALID AGE
    // =========================================================

    @Test
    void update_ShouldRejectInvalidAgeRange() {

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.minimumAge())
                .thenReturn(80);

        when(req.maximumAge())
                .thenReturn(30);

        assertThrows(
                BadRequestException.class,
                () -> medicalServiceService.update(
                        UUID.randomUUID(),
                        req
                )
        );

        verifyNoInteractions(repo);
    }


    // =========================================================
    // UPDATE - OTHER GENDER
    // =========================================================

    @Test
    void update_ShouldRejectOtherGender() {

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.allowedGender())
                .thenReturn(Gender.OTHER);

        assertThrows(
                BadRequestException.class,
                () -> medicalServiceService.update(
                        UUID.randomUUID(),
                        req
                )
        );
    }


    // =========================================================
    // UPDATE - INACTIVE
    // =========================================================

    @Test
    void update_ShouldRejectInactiveService() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Old",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.INACTIVE
                );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        assertThrows(
                ConflictException.class,
                () -> medicalServiceService.update(
                        id,
                        req
                )
        );
    }


    // =========================================================
    // UPDATE - DUPLICATE NEW NAME
    // =========================================================

    @Test
    void update_ShouldRejectDuplicateNewName() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Old",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.name())
                .thenReturn("New");

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(repo.existsByName("New"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> medicalServiceService.update(
                        id,
                        req
                )
        );
    }


    // =========================================================
    // UPDATE - SAME NAME
    // =========================================================

    @Test
    void update_ShouldNotCheckDuplicate_WhenNameUnchanged() {

        UUID id = UUID.randomUUID();

        Specialization specialization =
                mock(Specialization.class);

        MedicalService s =
                service(
                        id,
                        "Same",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        s.setRequiredSpecialization(
                specialization
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.name())
                .thenReturn("Same");

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(repo.save(s))
                .thenReturn(s);

        medicalServiceService.update(
                id,
                req
        );

        verify(repo, never())
                .existsByName(anyString());
    }


    // =========================================================
    // UPDATE - ALL BASIC FIELDS
    // =========================================================

    @Test
    void update_ShouldUpdateAllBasicFields() {

        UUID id = UUID.randomUUID();

        Specialization specialization =
                mock(Specialization.class);

        MedicalService s =
                service(
                        id,
                        "Old",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        s.setRequiredSpecialization(
                specialization
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.name()).thenReturn("New");
        when(req.description()).thenReturn("New desc");
        when(req.price()).thenReturn(
                new BigDecimal("250000")
        );

        when(req.status())
                .thenReturn(ServiceStatus.ACTIVE);

        when(req.isPointOfCare())
                .thenReturn(true);

        when(req.durationMinutes())
                .thenReturn(45);

        when(req.workflowPriority())
                .thenReturn(5);

        when(req.requiresDoctorOrder())
                .thenReturn(true);

        when(req.requiresReturnToDoctor())
                .thenReturn(true);

        when(req.resultWaitMinutes())
                .thenReturn(20);

        when(req.allowCustomerBooking())
                .thenReturn(false);

        when(req.minimumAge())
                .thenReturn(10);

        when(req.maximumAge())
                .thenReturn(90);

        when(req.allowedGender())
                .thenReturn(Gender.FEMALE);

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(repo.existsByName("New"))
                .thenReturn(false);

        when(repo.save(s))
                .thenReturn(s);

        var result =
                medicalServiceService.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals("New", s.getName());
        assertEquals("New desc", s.getDescription());

        assertEquals(
                0,
                new BigDecimal("250000")
                        .compareTo(s.getPrice())
        );

        assertEquals(
                ServiceStatus.ACTIVE,
                s.getStatus()
        );

        assertTrue(s.getIsPointOfCare());
        assertEquals(45, s.getDurationMinutes());
        assertEquals(5, s.getWorkflowPriority());

        assertTrue(
                s.getRequiresDoctorOrder()
        );

        assertTrue(
                s.getRequiresReturnToDoctor()
        );

        assertEquals(
                20,
                s.getResultWaitMinutes()
        );

        assertFalse(
                s.getAllowCustomerBooking()
        );

        assertEquals(
                10,
                s.getMinimumAge()
        );

        assertEquals(
                90,
                s.getMaximumAge()
        );

        assertEquals(
                Gender.FEMALE,
                s.getAllowedGender()
        );

        assertNull(
                s.getDepartment()
        );
    }


    // =========================================================
    // UPDATE EXAMINATION - SPECIALIZATION
    // =========================================================

    @Test
    void update_ShouldSetSpecializationForExamination() {

        UUID id = UUID.randomUUID();
        UUID specId = UUID.randomUUID();

        Specialization oldSpec =
                mock(Specialization.class);

        Specialization newSpec =
                mock(Specialization.class);

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        s.setRequiredSpecialization(oldSpec);

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.requiredSpecializationId())
                .thenReturn(specId);

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(specializationRepo.findById(specId))
                .thenReturn(Optional.of(newSpec));

        when(repo.save(s))
                .thenReturn(s);

        medicalServiceService.update(id, req);

        assertSame(
                newSpec,
                s.getRequiredSpecialization()
        );
    }


    @Test
    void update_ShouldThrow_WhenNewSpecializationMissing() {

        UUID id = UUID.randomUUID();
        UUID specId = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.requiredSpecializationId())
                .thenReturn(specId);

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(specializationRepo.findById(specId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalServiceService.update(
                        id,
                        req
                )
        );
    }


    // =========================================================
    // UPDATE EXAMINATION WITHOUT SPECIALIZATION
    // =========================================================

    @Test
    void update_ShouldRejectExaminationWithoutSpecialization() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        s.setRequiredSpecialization(null);

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        assertThrows(
                BadRequestException.class,
                () -> medicalServiceService.update(
                        id,
                        req
                )
        );
    }


    // =========================================================
    // UPDATE CHANGE TO PARACLINICAL
    // =========================================================

    @Test
    void update_ShouldNormalizeLaboratoryToParaclinicalAndClearSpecialization() {

        UUID id = UUID.randomUUID();

        var capability =
                mock(org.example.doansummer2026.model.ServiceCapability.class);

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        s.setRequiredSpecialization(
                mock(Specialization.class)
        );

        s.setRequiredCapability(capability);

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.departmentType())
                .thenReturn(DepartmentType.LABORATORY);

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(repo.save(s))
                .thenReturn(s);

        medicalServiceService.update(
                id,
                req
        );

        assertEquals(
                DepartmentType.PARACLINICAL,
                s.getDepartmentType()
        );

        assertNull(
                s.getRequiredSpecialization()
        );
    }


    // =========================================================
    // UPDATE PARACLINICAL WITHOUT CAPABILITY
    // =========================================================

    @Test
    void update_ShouldRejectLaboratoryWithoutCapability() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "XN",
                        DepartmentType.LABORATORY,
                        ServiceStatus.DRAFT
                );

        s.setRequiredCapability(null);

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        assertThrows(
                BadRequestException.class,
                () -> medicalServiceService.update(
                        id,
                        req
                )
        );
    }


    // =========================================================
    // UPDATE CAPABILITY SUCCESS
    // =========================================================

    @Test
    void update_ShouldSetCapability() {

        UUID id = UUID.randomUUID();
        UUID capabilityId = UUID.randomUUID();

        var capability =
                mock(
                        org.example.doansummer2026.model.ServiceCapability.class
                );

        MedicalService s =
                service(
                        id,
                        "XN",
                        DepartmentType.LABORATORY,
                        ServiceStatus.DRAFT
                );

        /*
         * Cần capability cũ để vượt validation ở line 175
         * trước khi code đi tới req.requiredCapabilityId().
         */
        s.setRequiredCapability(
                mock(
                        org.example.doansummer2026.model.ServiceCapability.class
                )
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.requiredCapabilityId())
                .thenReturn(capabilityId);

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(capabilityRepo.findById(capabilityId))
                .thenReturn(Optional.of(capability));

        when(repo.save(s))
                .thenReturn(s);

        medicalServiceService.update(
                id,
                req
        );

        assertSame(
                capability,
                s.getRequiredCapability()
        );
    }


    @Test
    void update_ShouldThrow_WhenNewCapabilityMissing() {

        UUID id = UUID.randomUUID();
        UUID capabilityId = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "XN",
                        DepartmentType.LABORATORY,
                        ServiceStatus.DRAFT
                );

        s.setRequiredCapability(
                mock(
                        org.example.doansummer2026.model.ServiceCapability.class
                )
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.requiredCapabilityId())
                .thenReturn(capabilityId);

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(capabilityRepo.findById(capabilityId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalServiceService.update(
                        id,
                        req
                )
        );
    }


    // =========================================================
    // UPDATE DEMOGRAPHIC DEFAULTS
    // =========================================================

    @Test
    void update_ShouldDefaultMinimumToZero_WhenOnlyMaximumProvided() {

        UUID id = UUID.randomUUID();

        Specialization spec =
                mock(Specialization.class);

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        s.setRequiredSpecialization(spec);

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.maximumAge())
                .thenReturn(50);

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(repo.save(s))
                .thenReturn(s);

        medicalServiceService.update(
                id,
                req
        );

        assertEquals(
                0,
                s.getMinimumAge()
        );

        assertEquals(
                50,
                s.getMaximumAge()
        );
    }

    @Test
    void update_ShouldDefaultMaximumTo120_WhenOnlyMinimumProvided() {

        UUID id = UUID.randomUUID();

        Specialization spec =
                mock(Specialization.class);

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        s.setRequiredSpecialization(spec);

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        doReturn(20)
                .when(req)
                .minimumAge();

        doReturn(null)
                .when(req)
                .maximumAge();

        doReturn(null)
                .when(req)
                .allowedGender();

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(repo.save(s))
                .thenReturn(s);

        medicalServiceService.update(
                id,
                req
        );

        assertEquals(
                20,
                s.getMinimumAge()
        );

        assertEquals(
                120,
                s.getMaximumAge()
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldDeleteDraftService() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Draft",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        medicalServiceService.delete(id);

        verify(repo)
                .deleteById(id);
    }


    @Test
    void delete_ShouldRejectNonDraftService() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Active",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.ACTIVE
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        assertThrows(
                ConflictException.class,
                () -> medicalServiceService.delete(id)
        );

        verify(repo, never())
                .deleteById(id);
    }


    // =========================================================
    // DEACTIVATE
    // =========================================================

    @Test
    void deactivate_ShouldDeactivateActiveService() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Active",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.ACTIVE
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(repo.save(s))
                .thenReturn(s);

        var result =
                medicalServiceService.deactivate(id);

        assertNotNull(result);

        assertEquals(
                ServiceStatus.INACTIVE,
                s.getStatus()
        );
    }


    @Test
    void deactivate_ShouldReject_WhenNotActive() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Draft",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        assertThrows(
                ConflictException.class,
                () -> medicalServiceService.deactivate(id)
        );
    }


    // =========================================================
    // PUBLISH
    // =========================================================

    @Test
    void publish_ShouldReject_WhenNotDraft() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Active",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.ACTIVE
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        assertThrows(
                ConflictException.class,
                () -> medicalServiceService.publish(id)
        );
    }


    @Test
    void publish_ShouldRejectExaminationWithoutSpecialization() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Kham",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        s.setRequiredSpecialization(null);

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        assertThrows(
                BadRequestException.class,
                () -> medicalServiceService.publish(id)
        );
    }


    @Test
    void publish_ShouldRejectLaboratoryWithoutCapability() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "XN",
                        DepartmentType.LABORATORY,
                        ServiceStatus.DRAFT
                );

        s.setRequiredCapability(null);

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        assertThrows(
                BadRequestException.class,
                () -> medicalServiceService.publish(id)
        );
    }


    @Test
    void publish_ShouldPublishExaminationService() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Kham",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        s.setRequiredSpecialization(
                mock(Specialization.class)
        );

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(repo.save(s))
                .thenReturn(s);

        var result =
                medicalServiceService.publish(id);

        assertNotNull(result);

        assertEquals(
                ServiceStatus.ACTIVE,
                s.getStatus()
        );

        verify(repo)
                .save(s);
    }


    @Test
    void publish_ShouldPublishLaboratoryService() {

        UUID id = UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "XN",
                        DepartmentType.LABORATORY,
                        ServiceStatus.DRAFT
                );

        s.setRequiredCapability(
                mock(
                        org.example.doansummer2026.model.ServiceCapability.class
                )
        );

        when(repo.findById(id))
                .thenReturn(Optional.of(s));

        when(repo.save(s))
                .thenReturn(s);

        medicalServiceService.publish(id);

        assertEquals(
                ServiceStatus.ACTIVE,
                s.getStatus()
        );
    }
}
