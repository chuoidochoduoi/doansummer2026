package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.medicalService.MedicalServiceCreateRequest;
import org.example.doansummer2026.dto.medicalService.MedicalServiceUpdateRequest;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.ServiceStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.ServiceCapability;
import org.example.doansummer2026.model.Specialization;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ServiceCapabilityRepository;
import org.example.doansummer2026.repository.ServiceCategoryRepository;
import org.example.doansummer2026.repository.SpecializationRepository;
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
                .requiresSpecimen(false)
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

        var pageable =
                PageRequest.of(0, 10);

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

        verify(repo)
                .search(
                        "kham",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.ACTIVE,
                        null,
                        pageable
                );
    }


    @Test
    void search_ShouldReturnEmptyPage() {

        var pageable =
                PageRequest.of(0, 10);

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

        var result =
                medicalServiceService.search(
                        null,
                        null,
                        null,
                        null,
                        pageable
                );

        assertNotNull(result);
    }


    // =========================================================
    // AVAILABLE
    // =========================================================

    @Test
    void listAvailable_ShouldReturnMappedPage() {

        var pageable =
                PageRequest.of(0, 10);

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

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        assertSame(
                s,
                medicalServiceService.findById(id)
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
                () -> medicalServiceService.findById(id)
        );
    }


    @Test
    void get_ShouldReturnResponse() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

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

        when(
                repo.count(
                        any(Specification.class)
                )
        ).thenReturn(
                5L,
                2L,
                3L
        );

        var result =
                medicalServiceService.getStats();

        assertEquals(
                10L,
                result.get("total")
        );

        assertEquals(
                5L,
                result.get("active")
        );

        assertEquals(
                2L,
                result.get("suspended")
        );

        assertEquals(
                3L,
                result.get("draft")
        );

        verify(
                repo,
                times(3)
        ).count(
                any(Specification.class)
        );
    }


    // =========================================================
    // CREATE - INVALID AGE
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
    // CREATE - OTHER GENDER
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

        verifyNoInteractions(repo);
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

        when(
                repo.existsByNameIgnoreCase(
                        "Kham noi"
                )
        ).thenReturn(true);

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

        when(
                repo.existsByServiceCode(
                        "DV01"
                )
        ).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> medicalServiceService.create(req)
        );

        verify(repo, never())
                .save(any());
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
                .thenReturn(
                        DepartmentType.EXAMINATION
                );

        assertThrows(
                BadRequestException.class,
                () -> medicalServiceService.create(req)
        );

        verify(repo, never())
                .save(any());
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
                .thenReturn(
                        DepartmentType.LABORATORY
                );

        assertThrows(
                BadRequestException.class,
                () -> medicalServiceService.create(req)
        );

        verify(repo, never())
                .save(any());
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

        when(req.name())
                .thenReturn("Kham");

        when(req.serviceCode())
                .thenReturn("K01");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.EXAMINATION
                );

        when(req.requiredSpecializationId())
                .thenReturn(
                        specializationId
                );

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalServiceService.create(req)
        );

        verify(repo, never())
                .save(any());
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

        when(req.name())
                .thenReturn("XN");

        when(req.serviceCode())
                .thenReturn("XN01");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.LABORATORY
                );

        when(req.requiredCapabilityId())
                .thenReturn(
                        capabilityId
                );

        when(
                capabilityRepo.findById(
                        capabilityId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> medicalServiceService.create(req)
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // CREATE - SPECIALIZATION INACTIVE
    // =========================================================

    @Test
    void create_ShouldRejectInactiveSpecialization() {

        UUID specializationId =
                UUID.randomUUID();

        Specialization specialization =
                mock(Specialization.class);

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.name())
                .thenReturn("Kham");

        when(req.serviceCode())
                .thenReturn("K01");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.EXAMINATION
                );

        when(req.requiredSpecializationId())
                .thenReturn(
                        specializationId
                );

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(
                Optional.of(specialization)
        );

        when(specialization.getActive())
                .thenReturn(false);

        assertThrows(
                ConflictException.class,
                () -> medicalServiceService.create(req)
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // CREATE - CAPABILITY INACTIVE
    // =========================================================

    @Test
    void create_ShouldRejectInactiveCapability() {

        UUID capabilityId =
                UUID.randomUUID();

        ServiceCapability capability =
                mock(ServiceCapability.class);

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.name())
                .thenReturn("XN");

        when(req.serviceCode())
                .thenReturn("XN01");

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.LABORATORY
                );

        when(req.requiredCapabilityId())
                .thenReturn(
                        capabilityId
                );

        when(
                capabilityRepo.findById(
                        capabilityId
                )
        ).thenReturn(
                Optional.of(capability)
        );

        when(capability.getActive())
                .thenReturn(false);

        assertThrows(
                ConflictException.class,
                () -> medicalServiceService.create(req)
        );

        verify(repo, never())
                .save(any());
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
                .thenReturn(
                        "Kham noi tong quat"
                );

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.EXAMINATION
                );

        when(req.requiredSpecializationId())
                .thenReturn(
                        specializationId
                );

        when(req.price())
                .thenReturn(
                        new BigDecimal("200000")
                );

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
                .thenReturn(
                        Gender.MALE
                );

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(
                Optional.of(specialization)
        );

        when(
                specialization.getActive()
        ).thenReturn(true);

        when(
                repo.save(
                        any(MedicalService.class)
                )
        ).thenAnswer(
                invocation -> {

                    MedicalService s =
                            invocation.getArgument(0);

                    s.setServiceId(
                            UUID.randomUUID()
                    );

                    return s;
                }
        );

        var result =
                medicalServiceService.create(req);

        assertNotNull(result);

        verify(repo)
                .save(
                        argThat(
                                s ->
                                        "KN01".equals(
                                                s.getServiceCode()
                                        )
                                                &&
                                                "Kham Noi".equals(
                                                        s.getName()
                                                )
                                                &&
                                                s.getDepartmentType()
                                                        == DepartmentType.EXAMINATION
                                                &&
                                                s.getRequiredSpecialization()
                                                        == specialization
                                                &&
                                                s.getRequiredCapability()
                                                        == null
                                                &&
                                                s.getDepartment()
                                                        == null
                                                &&
                                                s.getStatus()
                                                        == ServiceStatus.DRAFT
                                                &&
                                                Boolean.TRUE.equals(
                                                        s.getIsPointOfCare()
                                                )
                                                &&
                                                s.getDurationMinutes()
                                                        == 30
                                                &&
                                                s.getWorkflowPriority()
                                                        == 2
                                                &&
                                                Boolean.TRUE.equals(
                                                        s.getRequiresDoctorOrder()
                                                )
                                                &&
                                                Boolean.TRUE.equals(
                                                        s.getRequiresReturnToDoctor()
                                                )
                                                &&
                                                Boolean.FALSE.equals(
                                                        s.getRequiresSpecimen()
                                                )
                                                &&
                                                s.getResultWaitMinutes()
                                                        == 10
                                                &&
                                                Boolean.FALSE.equals(
                                                        s.getAllowCustomerBooking()
                                                )
                                                &&
                                                s.getMinimumAge()
                                                        == 18
                                                &&
                                                s.getMaximumAge()
                                                        == 70
                                                &&
                                                s.getAllowedGender()
                                                        == Gender.MALE
                        )
                );
    }


    // =========================================================
    // CREATE DEFAULT VALUES
    // =========================================================

    @Test
    void create_ShouldUseDefaultValues() {

        UUID specializationId =
                UUID.randomUUID();

        Specialization specialization =
                mock(Specialization.class);

        MedicalServiceCreateRequest req =
                new MedicalServiceCreateRequest(
                        "DEF01",
                        "Kham mac dinh",
                        null,
                        DepartmentType.EXAMINATION,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        specializationId,
                        null
                );

        when(
                specializationRepo.findById(
                        specializationId
                )
        ).thenReturn(
                Optional.of(specialization)
        );

        when(
                specialization.getActive()
        ).thenReturn(true);

        when(
                repo.save(
                        any(MedicalService.class)
                )
        ).thenAnswer(
                invocation -> {

                    MedicalService entity =
                            invocation.getArgument(0);

                    if (
                            entity.getServiceId()
                                    == null
                    ) {
                        entity.setServiceId(
                                UUID.randomUUID()
                        );
                    }

                    return entity;
                }
        );

        var result =
                medicalServiceService.create(req);

        assertNotNull(result);

        ArgumentCaptor<MedicalService> captor =
                ArgumentCaptor.forClass(
                        MedicalService.class
                );

        verify(repo)
                .save(
                        captor.capture()
                );

        MedicalService saved =
                captor.getValue();

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
                BigDecimal.ZERO.compareTo(
                        saved.getPrice()
                )
        );

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

        assertFalse(
                saved.getRequiresSpecimen()
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

        verifyNoInteractions(
                capabilityRepo
        );
    }


    // =========================================================
    // CREATE PARACLINICAL SUCCESS
    // =========================================================

    @Test
    void create_ShouldCreateLaboratoryServiceWithCapability() {

        UUID capabilityId =
                UUID.randomUUID();

        ServiceCapability capability =
                mock(ServiceCapability.class);

        MedicalServiceCreateRequest req =
                mock(MedicalServiceCreateRequest.class);

        when(req.name())
                .thenReturn(
                        "Cong thuc mau"
                );

        when(req.serviceCode())
                .thenReturn(
                        "XN001"
                );

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.LABORATORY
                );

        when(req.requiredCapabilityId())
                .thenReturn(
                        capabilityId
                );

        when(
                capabilityRepo.findById(
                        capabilityId
                )
        ).thenReturn(
                Optional.of(capability)
        );

        when(
                capability.getActive()
        ).thenReturn(true);

        when(
                repo.save(
                        any(MedicalService.class)
                )
        ).thenAnswer(
                invocation -> {

                    MedicalService s =
                            invocation.getArgument(0);

                    s.setServiceId(
                            UUID.randomUUID()
                    );

                    return s;
                }
        );

        var result =
                medicalServiceService.create(req);

        assertNotNull(result);

        verify(repo)
                .save(
                        argThat(
                                s ->
                                        s.getDepartmentType()
                                                == DepartmentType.PARACLINICAL
                                                &&
                                                s.getRequiredCapability()
                                                        == capability
                                                &&
                                                s.getRequiredSpecialization()
                                                        == null
                        )
                );
    }


    // =========================================================
    // UPDATE - INVALID AGE
    // =========================================================

    @Test
    void update_ShouldRejectInvalidAgeRange() {

        UUID id = UUID.randomUUID();
        MedicalService service = service(id, "Draft service", DepartmentType.EXAMINATION, ServiceStatus.DRAFT);

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.minimumAge())
                .thenReturn(80);

        when(req.maximumAge())
                .thenReturn(30);

        when(repo.findById(id)).thenReturn(Optional.of(service));

        assertThrows(
                BadRequestException.class,
                () ->
                        medicalServiceService.update(
                                id,
                                req
                        )
        );

        verify(repo).findById(id);
    }


    // =========================================================
    // UPDATE - OTHER GENDER
    // =========================================================

    @Test
    void update_ShouldRejectOtherGender() {

        UUID id = UUID.randomUUID();
        MedicalService service = service(id, "Draft service", DepartmentType.EXAMINATION, ServiceStatus.DRAFT);

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.allowedGender())
                .thenReturn(Gender.OTHER);

        when(repo.findById(id)).thenReturn(Optional.of(service));

        assertThrows(
                BadRequestException.class,
                () ->
                        medicalServiceService.update(
                                id,
                                req
                        )
        );

        verify(repo).findById(id);
    }


    // =========================================================
    // UPDATE - STATUS
    // =========================================================

    @Test
    void update_ShouldRejectInactiveService_WhenTryingToReturnToDraft() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Old",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.INACTIVE
                );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.status())
                .thenReturn(
                        ServiceStatus.DRAFT
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                medicalServiceService.update(
                                        id,
                                        req
                                )
                );

        assertEquals(
                "Dịch vụ đã áp dụng hoặc tạm ngừng không thể quay lại bản nháp",
                exception.getMessage()
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // UPDATE - DUPLICATE NAME
    // =========================================================

    @Test
    void update_ShouldRejectDuplicateNewName() {

        UUID id =
                UUID.randomUUID();

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

        when(req.name())
                .thenReturn("New");

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        when(
                repo.existsByNameIgnoreCaseAndServiceIdNot(
                        "New",
                        id
                )
        ).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () ->
                        medicalServiceService.update(
                                id,
                                req
                        )
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // UPDATE - SAME NAME
    // =========================================================

    @Test
    void update_ShouldAllowSameName_WhenNoOtherServiceUsesIt() {

        UUID id =
                UUID.randomUUID();

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
                .thenReturn(
                        Optional.of(s)
                );

        when(repo.save(s))
                .thenReturn(s);

        var result =
                medicalServiceService.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                "Same",
                s.getName()
        );

        verify(repo)
                .existsByNameIgnoreCaseAndServiceIdNot(
                        "Same",
                        id
                );
    }


    // =========================================================
    // UPDATE - ALL BASIC FIELDS
    // =========================================================

    @Test
    void update_ShouldUpdateAllBasicFields() {

        UUID id =
                UUID.randomUUID();

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

        when(req.name())
                .thenReturn("New");

        when(req.description())
                .thenReturn("New desc");

        when(req.price())
                .thenReturn(
                        new BigDecimal("250000")
                );

        when(req.status())
                .thenReturn(
                        ServiceStatus.ACTIVE
                );

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
                .thenReturn(
                        Gender.FEMALE
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        when(repo.save(s))
                .thenReturn(s);

        var result =
                medicalServiceService.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                "New",
                s.getName()
        );

        assertEquals(
                "New desc",
                s.getDescription()
        );

        assertEquals(
                0,
                new BigDecimal("250000")
                        .compareTo(
                                s.getPrice()
                        )
        );

        assertEquals(
                ServiceStatus.ACTIVE,
                s.getStatus()
        );

        assertTrue(
                s.getIsPointOfCare()
        );

        assertEquals(
                45,
                s.getDurationMinutes()
        );

        assertEquals(
                5,
                s.getWorkflowPriority()
        );

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

        assertSame(
                specialization,
                s.getRequiredSpecialization()
        );
    }


    // =========================================================
    // UPDATE SPECIALIZATION
    // =========================================================

    @Test
    void update_ShouldSetSpecializationForExamination() {

        UUID id =
                UUID.randomUUID();

        UUID specId =
                UUID.randomUUID();

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

        s.setRequiredSpecialization(
                oldSpec
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.requiredSpecializationId())
                .thenReturn(specId);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        when(
                specializationRepo.findById(
                        specId
                )
        ).thenReturn(
                Optional.of(newSpec)
        );

        when(newSpec.getActive())
                .thenReturn(true);

        when(repo.save(s))
                .thenReturn(s);

        medicalServiceService.update(
                id,
                req
        );

        assertSame(
                newSpec,
                s.getRequiredSpecialization()
        );
    }


    @Test
    void update_ShouldThrow_WhenNewSpecializationMissing() {

        UUID id =
                UUID.randomUUID();

        UUID specId =
                UUID.randomUUID();

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

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.requiredSpecializationId())
                .thenReturn(specId);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        when(
                specializationRepo.findById(
                        specId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        medicalServiceService.update(
                                id,
                                req
                        )
        );
    }


    @Test
    void update_ShouldRejectInactiveSpecialization() {

        UUID id =
                UUID.randomUUID();

        UUID specId =
                UUID.randomUUID();

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

        s.setRequiredSpecialization(
                oldSpec
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.requiredSpecializationId())
                .thenReturn(specId);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        when(
                specializationRepo.findById(
                        specId
                )
        ).thenReturn(
                Optional.of(newSpec)
        );

        when(newSpec.getActive())
                .thenReturn(false);

        assertThrows(
                ConflictException.class,
                () ->
                        medicalServiceService.update(
                                id,
                                req
                        )
        );
    }


    // =========================================================
    // UPDATE EXAM WITHOUT SPECIALIZATION
    // =========================================================

    @Test
    void update_ShouldRejectExaminationWithoutSpecialization() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        s.setRequiredSpecialization(
                null
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        assertThrows(
                BadRequestException.class,
                () ->
                        medicalServiceService.update(
                                id,
                                req
                        )
        );
    }


    // =========================================================
    // UPDATE TO PARACLINICAL
    // =========================================================

    @Test
    void update_ShouldNormalizeLaboratoryToParaclinicalAndClearSpecialization() {

        UUID id =
                UUID.randomUUID();

        ServiceCapability capability =
                mock(ServiceCapability.class);

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

        s.setRequiredCapability(
                capability
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.LABORATORY
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

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

        assertSame(
                capability,
                s.getRequiredCapability()
        );
    }


    // =========================================================
    // UPDATE PARACLINICAL WITHOUT CAPABILITY
    // =========================================================

    @Test
    void update_ShouldRejectLaboratoryWithoutCapability() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "XN",
                        DepartmentType.LABORATORY,
                        ServiceStatus.DRAFT
                );

        s.setRequiredCapability(
                null
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        assertThrows(
                BadRequestException.class,
                () ->
                        medicalServiceService.update(
                                id,
                                req
                        )
        );
    }


    // =========================================================
    // UPDATE CAPABILITY
    // =========================================================

    @Test
    void update_ShouldSetCapability() {

        UUID id =
                UUID.randomUUID();

        UUID capabilityId =
                UUID.randomUUID();

        ServiceCapability oldCapability =
                mock(ServiceCapability.class);

        ServiceCapability capability =
                mock(ServiceCapability.class);

        MedicalService s =
                service(
                        id,
                        "XN",
                        DepartmentType.LABORATORY,
                        ServiceStatus.DRAFT
                );

        s.setRequiredCapability(
                oldCapability
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.requiredCapabilityId())
                .thenReturn(
                        capabilityId
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        when(
                capabilityRepo.findById(
                        capabilityId
                )
        ).thenReturn(
                Optional.of(capability)
        );

        when(capability.getActive())
                .thenReturn(true);

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

        UUID id =
                UUID.randomUUID();

        UUID capabilityId =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "XN",
                        DepartmentType.LABORATORY,
                        ServiceStatus.DRAFT
                );

        s.setRequiredCapability(
                mock(ServiceCapability.class)
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.requiredCapabilityId())
                .thenReturn(
                        capabilityId
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        when(
                capabilityRepo.findById(
                        capabilityId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        medicalServiceService.update(
                                id,
                                req
                        )
        );
    }


    @Test
    void update_ShouldRejectInactiveCapability() {

        UUID id =
                UUID.randomUUID();

        UUID capabilityId =
                UUID.randomUUID();

        ServiceCapability oldCapability =
                mock(ServiceCapability.class);

        ServiceCapability capability =
                mock(ServiceCapability.class);

        MedicalService s =
                service(
                        id,
                        "XN",
                        DepartmentType.LABORATORY,
                        ServiceStatus.DRAFT
                );

        s.setRequiredCapability(
                oldCapability
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.requiredCapabilityId())
                .thenReturn(
                        capabilityId
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        when(
                capabilityRepo.findById(
                        capabilityId
                )
        ).thenReturn(
                Optional.of(capability)
        );

        when(capability.getActive())
                .thenReturn(false);

        assertThrows(
                ConflictException.class,
                () ->
                        medicalServiceService.update(
                                id,
                                req
                        )
        );
    }


    // =========================================================
    // UPDATE DEMOGRAPHICS
    // =========================================================

    @Test
    void update_ShouldDefaultMinimumToZero_WhenOnlyMaximumProvided() {

        UUID id =
                UUID.randomUUID();

        Specialization spec =
                mock(Specialization.class);

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        s.setRequiredSpecialization(
                spec
        );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.maximumAge())
                .thenReturn(50);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

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


//    @Test
//    void update_ShouldDefaultMaximumTo120_WhenOnlyMinimumProvided() {
//
//        UUID id =
//                UUID.randomUUID();
//
//        Specialization spec =
//                mock(Specialization.class);
//
//        MedicalService s =
//                service(
//                        id,
//                        "Service",
//                        DepartmentType.EXAMINATION,
//                        ServiceStatus.DRAFT
//                );
//
//        s.setRequiredSpecialization(
//                spec
//        );
//
//        MedicalServiceUpdateRequest req =
//                mock(MedicalServiceUpdateRequest.class);
//
//        when(req.minimumAge())
//                .thenReturn(20);
//
//        when(repo.findById(id))
//                .thenReturn(
//                        Optional.of(s)
//                );
//
//        when(repo.save(s))
//                .thenReturn(s);
//
//        medicalServiceService.update(
//                id,
//                req
//        );
//
//        assertEquals(
//                20,
//                s.getMinimumAge()
//        );
//
//        assertEquals(
//                120,
//                s.getMaximumAge()
//        );
//    }


    // =========================================================
    // ROUTING CONFIGURATION
    // =========================================================

    @Test
    void update_ShouldRejectRoutingChange_WhenOperationalReferencesExist() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Service",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.ACTIVE
                );

        MedicalServiceUpdateRequest req =
                mock(MedicalServiceUpdateRequest.class);

        when(req.departmentType())
                .thenReturn(
                        DepartmentType.LABORATORY
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        assertThrows(
                ConflictException.class,
                () ->
                        medicalServiceService.update(
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
    void delete_ShouldDeleteDraftService_WhenNoOperationalReferences() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Draft",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        when(
                repo.countOperationalReferences(
                        id
                )
        ).thenReturn(0L);

        medicalServiceService.delete(id);

        verify(repo)
                .deleteById(id);
    }


    @Test
    void delete_ShouldRejectDraftService_WhenOperationalReferencesExist() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Draft",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        when(
                repo.countOperationalReferences(
                        id
                )
        ).thenReturn(2L);

        assertThrows(
                ConflictException.class,
                () ->
                        medicalServiceService.delete(
                                id
                        )
        );

        verify(repo, never())
                .deleteById(id);
    }


    @Test
    void delete_ShouldRejectNonDraftService() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Active",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.ACTIVE
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        assertThrows(
                ConflictException.class,
                () ->
                        medicalServiceService.delete(
                                id
                        )
        );

        verify(repo, never())
                .deleteById(id);

        verify(repo, never())
                .countOperationalReferences(id);
    }


    // =========================================================
    // DEACTIVATE
    // =========================================================

    @Test
    void deactivate_ShouldDeactivateActiveService() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Active",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.ACTIVE
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        when(repo.save(s))
                .thenReturn(s);

        var result =
                medicalServiceService.deactivate(
                        id
                );

        assertNotNull(result);

        assertEquals(
                ServiceStatus.INACTIVE,
                s.getStatus()
        );

        verify(repo)
                .save(s);
    }


    @Test
    void deactivate_ShouldReject_WhenNotActive() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Draft",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        assertThrows(
                ConflictException.class,
                () ->
                        medicalServiceService.deactivate(
                                id
                        )
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // PUBLISH
    // =========================================================

    @Test
    void publish_ShouldReject_WhenNotDraft() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Active",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.ACTIVE
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        assertThrows(
                ConflictException.class,
                () ->
                        medicalServiceService.publish(
                                id
                        )
        );

        verify(repo, never())
                .save(any());
    }


    @Test
    void publish_ShouldRejectExaminationWithoutSpecialization() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "Kham",
                        DepartmentType.EXAMINATION,
                        ServiceStatus.DRAFT
                );

        s.setRequiredSpecialization(
                null
        );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        assertThrows(
                BadRequestException.class,
                () ->
                        medicalServiceService.publish(
                                id
                        )
        );

        verify(repo, never())
                .save(any());
    }


    @Test
    void publish_ShouldRejectLaboratoryWithoutCapability() {

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "XN",
                        DepartmentType.LABORATORY,
                        ServiceStatus.DRAFT
                );

        s.setRequiredCapability(
                null
        );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        assertThrows(
                BadRequestException.class,
                () ->
                        medicalServiceService.publish(
                                id
                        )
        );

        verify(repo, never())
                .save(any());
    }


    @Test
    void publish_ShouldPublishExaminationService() {

        UUID id =
                UUID.randomUUID();

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
                .thenReturn(
                        Optional.of(s)
                );

        when(repo.save(s))
                .thenReturn(s);

        var result =
                medicalServiceService.publish(
                        id
                );

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

        UUID id =
                UUID.randomUUID();

        MedicalService s =
                service(
                        id,
                        "XN",
                        DepartmentType.LABORATORY,
                        ServiceStatus.DRAFT
                );

        s.setRequiredCapability(
                mock(ServiceCapability.class)
        );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(s)
                );

        when(repo.save(s))
                .thenReturn(s);

        var result =
                medicalServiceService.publish(
                        id
                );

        assertNotNull(result);

        assertEquals(
                ServiceStatus.ACTIVE,
                s.getStatus()
        );

        verify(repo)
                .save(s);
    }
}
