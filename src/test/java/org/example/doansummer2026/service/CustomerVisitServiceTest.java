package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.customerVisit.CustomerVisitCreateRequest;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitUpdateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceResponse;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.ServiceStatus;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.InsuranceRuleRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CustomerVisitServiceTest {

    private static final ZoneId CLINIC_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");


    @Mock
    private CustomerVisitRepository repo;

    @Mock
    private ProfileRepository profileRepo;

    @Mock
    private AppointmentRepository appointmentRepo;

    @Mock
    private MedicalServiceRepository serviceRepo;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InsuranceRuleRepository insuranceRuleRepo;

    @Mock
    private InvoiceRepository invoiceRepo;

    @Mock
    private StaffInfoRepository staffInfoRepository;

    @InjectMocks
    private CustomerVisitService customerVisitService;


    // =========================================================
    // HELPERS
    // =========================================================

    private LocalDate clinicToday() {
        return LocalDate.now(CLINIC_ZONE);
    }


    private Profile profile(UUID id) {

        return Profile.builder()
                .profileId(id)
                .fullName("Nguyen Van A")
                .phone("0901234567")
                .gender(Gender.MALE)
                .dateOfBirth(
                        LocalDate.of(
                                2000,
                                1,
                                1
                        )
                )
                .build();
    }


    private StaffInfo staff(UUID id) {

        return StaffInfo.builder()
                .staffId(id)
                .staffCode("STAFF001")
                .build();
    }


    private MedicalService medicalService(
            UUID id,
            String name,
            String code,
            BigDecimal price,
            DepartmentType departmentType
    ) {

        return MedicalService.builder()
                .serviceId(id)
                .name(name)
                .serviceCode(code)
                .price(price)
                .departmentType(departmentType)
                .status(ServiceStatus.ACTIVE)
                .build();
    }


    private CustomerVisit visit(
            UUID id,
            Profile customer,
            VisitStatus status
    ) {

        return CustomerVisit.builder()
                .visitId(id)
                .customer(customer)
                .checkInTime(
                        LocalDateTime.now()
                                .minusMinutes(30)
                )
                .status(status)
                .build();
    }


    private InvoiceResponse mockInvoiceResponse(
            UUID invoiceId
    ) {

        InvoiceResponse response =
                mock(InvoiceResponse.class);

        doReturn(invoiceId)
                .when(response)
                .invoiceId();

        return response;
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void search_ShouldReturnMappedPage() {

        UUID customerId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        LocalDateTime from =
                LocalDateTime.of(
                        2026,
                        8,
                        1,
                        0,
                        0
                );

        LocalDateTime to =
                LocalDateTime.of(
                        2026,
                        8,
                        31,
                        23,
                        59
                );

        var pageable =
                PageRequest.of(
                        0,
                        10
                );

        CustomerVisit visit =
                visit(
                        visitId,
                        profile(customerId),
                        VisitStatus.CHECKED_IN
                );

        when(
                repo.search(
                        customerId,
                        VisitStatus.CHECKED_IN,
                        from,
                        to,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of(visit)
                )
        );

        when(
                invoiceRepo
                        .findAllByVisit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

        var result =
                customerVisitService.search(
                        customerId,
                        VisitStatus.CHECKED_IN,
                        from,
                        to,
                        pageable
                );

        assertNotNull(result);

        verify(repo).search(
                customerId,
                VisitStatus.CHECKED_IN,
                from,
                to,
                pageable
        );

        verify(invoiceRepo)
                .findAllByVisit_VisitId(
                        visitId
                );
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenFound() {

        UUID id =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.CHECKED_IN
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(visit)
                );

        assertSame(
                visit,
                customerVisitService.findById(id)
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
                        customerVisitService
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

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.CHECKED_IN
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(visit)
                );

        when(
                invoiceRepo
                        .findAllByVisit_VisitId(id)
        ).thenReturn(
                List.of()
        );

        var result =
                customerVisitService.get(id);

        assertNotNull(result);

        verify(invoiceRepo)
                .findAllByVisit_VisitId(id);
    }


    // =========================================================
    // CREATE - SERVICE IDS NULL
    // =========================================================

    @Test
    void create_ShouldReject_WhenServiceIdsNull() {

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(null);

        assertThrows(
                BadRequestException.class,
                () ->
                        customerVisitService
                                .create(req)
        );

        verifyNoInteractions(
                profileRepo
        );

        verifyNoInteractions(
                serviceRepo
        );
    }


    // =========================================================
    // CREATE - SERVICE IDS EMPTY
    // =========================================================

    @Test
    void create_ShouldReject_WhenServiceIdsEmpty() {

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of()
                );

        assertThrows(
                BadRequestException.class,
                () ->
                        customerVisitService
                                .create(req)
        );

        verifyNoInteractions(
                profileRepo
        );

        verifyNoInteractions(
                serviceRepo
        );
    }


    // =========================================================
    // CREATE - SERVICE MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenServiceMissing() {

        UUID serviceId =
                UUID.randomUUID();

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        customerVisitService
                                .create(req)
        );

        verifyNoInteractions(
                profileRepo
        );

        verify(
                invoiceService,
                never()
        ).create(any());
    }


    // =========================================================
    // CREATE - REGISTERED CUSTOMER MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenRegisteredCustomerMissing() {

        UUID customerId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(req.customerId())
                .thenReturn(
                        customerId
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(
                profileRepo.findById(customerId)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        customerVisitService
                                .create(req)
        );

        verify(
                profileRepo,
                never()
        ).findByIdForUpdate(any());
    }


    // =========================================================
    // CREATE - CUSTOMER CANNOT BE LOCKED
    // =========================================================

    @Test
    void create_ShouldThrow_WhenCustomerCannotBeLocked() {

        UUID customerId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Profile customer =
                profile(customerId);

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(req.customerId())
                .thenReturn(
                        customerId
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(
                profileRepo.findById(customerId)
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                profileRepo.findByIdForUpdate(
                        customerId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        customerVisitService
                                .create(req)
        );

        verify(
                repo,
                never()
        ).save(any(CustomerVisit.class));
    }


    // =========================================================
    // CREATE - INSURANCE NOT ALLOWED
    // =========================================================

    @Test
    void create_ShouldReject_WhenInsuranceProvided() {

        UUID serviceId =
                UUID.randomUUID();

        UUID insuranceId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(req.insuranceId())
                .thenReturn(
                        insuranceId
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                customerVisitService
                                        .create(req)
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "quầy thu ngân"
                        )
        );

        verifyNoInteractions(
                profileRepo
        );

        verifyNoInteractions(
                insuranceRuleRepo
        );
    }


    // =========================================================
    // CREATE - DUPLICATE SERVICES
    // =========================================================

    @Test
    void create_ShouldRejectDuplicateServices() {
        UUID serviceId = UUID.randomUUID();
        CustomerVisitCreateRequest req = mock(CustomerVisitCreateRequest.class);
        when(req.serviceIds()).thenReturn(List.of(serviceId, serviceId));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> customerVisitService.create(req));

        assertTrue(exception.getMessage().contains("trùng dịch vụ"));
        verifyNoInteractions(profileRepo);
    }


    // =========================================================
    // CREATE GUEST - FULL NAME MISSING
    // =========================================================

    @Test
    void create_ShouldRejectGuest_WhenFullNameMissing() {

        UUID serviceId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(req.guestFullName())
                .thenReturn(null);

        when(req.guestPhone())
                .thenReturn(
                        "0901234567"
                );

        assertThrows(
                BadRequestException.class,
                () ->
                        customerVisitService
                                .create(req)
        );
    }


    // =========================================================
    // CREATE GUEST - INVALID PHONE
    // =========================================================

    @Test
    void create_ShouldRejectGuest_WhenPhoneInvalid() {

        UUID serviceId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(req.guestFullName())
                .thenReturn(
                        "Guest A"
                );

        when(req.guestPhone())
                .thenReturn(
                        "123"
                );

        assertThrows(
                BadRequestException.class,
                () ->
                        customerVisitService
                                .create(req)
        );
    }


    // =========================================================
    // CREATE GUEST - GENDER INVALID
    // =========================================================

    @Test
    void create_ShouldRejectGuest_WhenGenderOther() {

        UUID serviceId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(req.guestFullName())
                .thenReturn(
                        "Guest A"
                );

        when(req.guestPhone())
                .thenReturn(
                        "0901234567"
                );

        when(req.guestGender())
                .thenReturn(
                        Gender.OTHER
                );

        assertThrows(
                BadRequestException.class,
                () ->
                        customerVisitService
                                .create(req)
        );
    }


    // =========================================================
    // CREATE GUEST - REUSE EXISTING PROFILE
    // =========================================================

    @Test
    void create_ShouldReuseExistingGuestProfile_WhenPhoneExists() {

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        Profile existing =
                profile(profileId);

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham tong quat",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        StaffInfo staff =
                staff(staffId);

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(req.guestFullName())
                .thenReturn(
                        "Guest Updated"
                );

        when(req.guestPhone())
                .thenReturn(
                        "0901234567"
                );

        when(req.guestGender())
                .thenReturn(
                        Gender.MALE
                );

        when(req.guestDateOfBirth())
                .thenReturn(
                        LocalDate.of(
                                2000,
                                1,
                                1
                        )
                );

        when(req.issuedById())
                .thenReturn(
                        staffId
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(
                profileRepo.findFirstByPhone(
                        "0901234567"
                )
        ).thenReturn(
                Optional.of(existing)
        );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenReturn(
                Optional.of(existing)
        );

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                staffInfoRepository.findById(
                        staffId
                )
        ).thenReturn(
                Optional.of(staff)
        );

        when(
                repo.save(
                        any(CustomerVisit.class)
                )
        ).thenAnswer(
                invocation -> {

                    CustomerVisit saved =
                            invocation.getArgument(0);

                    saved.setVisitId(
                            visitId
                    );

                    return saved;
                }
        );

        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(
                        UUID.randomUUID()
                );

        when(
                invoiceService.create(any())
        ).thenReturn(
                invoiceResponse
        );

        var result =
                customerVisitService.create(req);

        assertNotNull(result);

        assertEquals(
                "Guest Updated",
                existing.getFullName()
        );

        verify(profileRepo)
                .save(existing);

        verify(
                staffInfoRepository
        ).findById(staffId);

        verify(invoiceService)
                .create(any());
    }


    // =========================================================
    // CREATE GUEST - NEW PROFILE
    // =========================================================

    @Test
    void create_ShouldCreateNewGuestProfile() {

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        StaffInfo staff =
                staff(staffId);

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(req.guestFullName())
                .thenReturn(
                        "Guest A"
                );

        when(req.guestPhone())
                .thenReturn(
                        "0909999999"
                );

        when(req.guestAddress())
                .thenReturn(
                        "Ha Noi"
                );

        when(req.guestDateOfBirth())
                .thenReturn(
                        LocalDate.of(
                                2000,
                                1,
                                1
                        )
                );

        when(req.guestGender())
                .thenReturn(
                        Gender.FEMALE
                );

        when(req.issuedById())
                .thenReturn(
                        staffId
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(
                profileRepo.findFirstByPhone(
                        "0909999999"
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                profileRepo.save(
                        any(Profile.class)
                )
        ).thenAnswer(
                invocation -> {

                    Profile saved =
                            invocation.getArgument(0);

                    if (saved.getProfileId() == null) {
                        saved.setProfileId(
                                profileId
                        );
                    }

                    return saved;
                }
        );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenAnswer(
                invocation -> {

                    Profile locked =
                            Profile.builder()
                                    .profileId(profileId)
                                    .fullName("Guest A")
                                    .phone("0909999999")
                                    .address("Ha Noi")
                                    .dateOfBirth(
                                            LocalDate.of(
                                                    2000,
                                                    1,
                                                    1
                                            )
                                    )
                                    .gender(Gender.FEMALE)
                                    .build();

                    return Optional.of(locked);
                }
        );

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                staffInfoRepository.findById(
                        staffId
                )
        ).thenReturn(
                Optional.of(staff)
        );

        when(
                repo.save(
                        any(CustomerVisit.class)
                )
        ).thenAnswer(
                invocation -> {

                    CustomerVisit visit =
                            invocation.getArgument(0);

                    visit.setVisitId(
                            visitId
                    );

                    return visit;
                }
        );

        UUID invoiceId =
                UUID.randomUUID();

        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(
                        invoiceId
                );

        when(
                invoiceService.create(any())
        ).thenReturn(
                invoiceResponse
        );

        var result =
                customerVisitService.create(req);

        assertNotNull(result);

        verify(
                profileRepo,
                atLeastOnce()
        ).save(
                any(Profile.class)
        );

        verify(invoiceService)
                .create(any());
    }


    // =========================================================
    // CREATE - ACTIVE VISIT EXISTS
    // =========================================================

    @Test
    void create_ShouldReject_WhenCustomerAlreadyHasActiveVisit() {

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Profile customer =
                profile(profileId);

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisit active =
                visit(
                        UUID.randomUUID(),
                        customer,
                        VisitStatus.IN_PROGRESS
                );

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(req.customerId())
                .thenReturn(
                        profileId
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(
                profileRepo.findById(profileId)
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        eq(
                                List.of(
                                        VisitStatus.CHECKED_IN,
                                        VisitStatus.IN_PROGRESS
                                )
                        )
                )
        ).thenReturn(
                Optional.of(active)
        );

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                customerVisitService
                                        .create(req)
                );

        assertTrue(
                exception.getMessage()
                        .contains("VIS-")
        );

        verify(
                repo,
                never()
        ).save(any(CustomerVisit.class));
    }


    // =========================================================
    // CREATE - APPOINTMENT MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenAppointmentMissing() {

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID appointmentId =
                UUID.randomUUID();

        Profile customer =
                profile(profileId);

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(req.customerId())
                .thenReturn(
                        profileId
                );

        when(req.appointmentId())
                .thenReturn(
                        appointmentId
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(
                profileRepo.findById(profileId)
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                appointmentRepo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        customerVisitService
                                .create(req)
        );

        verifyNoInteractions(
                staffInfoRepository
        );
    }


    // =========================================================
    // CREATE - STAFF ID MISSING
    // =========================================================

    @Test
    void create_ShouldReject_WhenIssuedByMissing() {

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Profile customer =
                profile(profileId);

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(req.customerId())
                .thenReturn(
                        profileId
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(
                profileRepo.findById(profileId)
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(
                Optional.empty()
        );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                customerVisitService
                                        .create(req)
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "nhân viên"
                        )
        );
    }


    // =========================================================
    // CREATE - STAFF NOT FOUND
    // =========================================================

    @Test
    void create_ShouldThrow_WhenStaffMissing() {

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        Profile customer =
                profile(profileId);

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(req.customerId())
                .thenReturn(
                        profileId
                );

        when(req.issuedById())
                .thenReturn(
                        staffId
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(
                profileRepo.findById(profileId)
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                staffInfoRepository.findById(
                        staffId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        customerVisitService
                                .create(req)
        );
    }


    // =========================================================
    // CREATE - SUCCESS
    // =========================================================

    @Test
    void create_ShouldCreateCheckedInVisitAndInvoice() {

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        UUID invoiceId =
                UUID.randomUUID();

        Profile customer =
                profile(profileId);

        StaffInfo staff =
                staff(staffId);

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham tong quat",
                        "DV001",
                        new BigDecimal("200000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(req.customerId())
                .thenReturn(
                        profileId
                );

        when(req.issuedById())
                .thenReturn(
                        staffId
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(
                profileRepo.findById(profileId)
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                staffInfoRepository.findById(
                        staffId
                )
        ).thenReturn(
                Optional.of(staff)
        );

        when(
                repo.save(
                        any(CustomerVisit.class)
                )
        ).thenAnswer(
                invocation -> {

                    CustomerVisit saved =
                            invocation.getArgument(0);

                    saved.setVisitId(
                            visitId
                    );

                    return saved;
                }
        );

        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(
                        invoiceId
                );

        when(
                invoiceService.create(any())
        ).thenReturn(
                invoiceResponse
        );

        var result =
                customerVisitService.create(req);

        assertNotNull(result);

        verify(repo).save(
                argThat(
                        saved ->
                                saved.getCustomer()
                                        == customer
                                        &&
                                        saved.getCheckedInBy()
                                                == staff
                                        &&
                                        saved.getStatus()
                                                == VisitStatus.CHECKED_IN
                                        &&
                                        saved.getCheckInTime()
                                                != null
                )
        );

        verify(invoiceService)
                .create(
                        argThat(
                                invoice ->
                                        profileId.equals(
                                                invoice.customerId()
                                        )
                                                &&
                                                visitId.equals(
                                                        invoice.visitId()
                                                )
                                                &&
                                                staffId.equals(
                                                        invoice.issuedById()
                                                )
                                                &&
                                                BigDecimal.ZERO.compareTo(
                                                        invoice.discount()
                                                ) == 0
                                                &&
                                                invoice.items()
                                                        != null
                                                &&
                                                invoice.items()
                                                        .size()
                                                        == 1
                        )
                );
    }


    // =========================================================
    // CREATE - APPOINTMENT SUCCESS
    // =========================================================

    @Test
    void create_ShouldMarkAppointmentCheckedIn() {

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID appointmentId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        Profile customer =
                profile(profileId);

        StaffInfo staff =
                staff(staffId);

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        Appointment appointment =
                Appointment.builder()
                        .appointmentId(
                                appointmentId
                        )
                        .customer(customer)
                        .scheduledAt(
                                clinicToday()
                                        .atTime(9, 0)
                        )
                        .status(
                                AppointmentStatus.PENDING
                        )
                        .build();

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(req.customerId())
                .thenReturn(
                        profileId
                );

        when(req.appointmentId())
                .thenReturn(
                        appointmentId
                );

        when(req.issuedById())
                .thenReturn(
                        staffId
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(
                profileRepo.findById(profileId)
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                appointmentRepo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.of(appointment)
        );

        when(
                repo.findByAppointment_AppointmentId(
                        appointmentId
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                staffInfoRepository.findById(
                        staffId
                )
        ).thenReturn(
                Optional.of(staff)
        );

        when(
                repo.save(
                        any(CustomerVisit.class)
                )
        ).thenAnswer(
                invocation -> {

                    CustomerVisit saved =
                            invocation.getArgument(0);

                    saved.setVisitId(
                            visitId
                    );

                    return saved;
                }
        );

        UUID invoiceId =
                UUID.randomUUID();

        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(
                        invoiceId
                );

        when(
                invoiceService.create(any())
        ).thenReturn(
                invoiceResponse
        );

        customerVisitService.create(req);

        assertEquals(
                AppointmentStatus.CHECKED_IN,
                appointment.getStatus()
        );

        verify(appointmentRepo)
                .save(appointment);
    }


    // =========================================================
    // CREATE - INACTIVE SERVICE
    // =========================================================

    @Test
    void create_ShouldReject_WhenServiceInactive() {

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        Profile customer =
                profile(profileId);

        MedicalService service =
                medicalService(
                        serviceId,
                        "Inactive Service",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        service.setStatus(
                ServiceStatus.INACTIVE
        );

        CustomerVisitCreateRequest req =
                mock(
                        CustomerVisitCreateRequest.class
                );

        when(req.serviceIds())
                .thenReturn(
                        List.of(serviceId)
                );

        when(req.customerId())
                .thenReturn(
                        profileId
                );

        when(req.issuedById())
                .thenReturn(
                        staffId
                );

        when(
                serviceRepo.findById(serviceId)
        ).thenReturn(
                Optional.of(service)
        );

        when(
                profileRepo.findById(profileId)
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                staffInfoRepository.findById(
                        staffId
                )
        ).thenReturn(
                Optional.of(
                        staff(staffId)
                )
        );

        when(
                repo.save(
                        any(CustomerVisit.class)
                )
        ).thenAnswer(
                invocation -> {

                    CustomerVisit saved =
                            invocation.getArgument(0);

                    saved.setVisitId(
                            UUID.randomUUID()
                    );

                    return saved;
                }
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        customerVisitService
                                .create(req)
        );

        verify(
                invoiceService,
                never()
        ).create(any());
    }


    // =========================================================
    // UPDATE - VISIT MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenVisitMissing() {

        UUID id =
                UUID.randomUUID();

        CustomerVisitUpdateRequest req =
                mock(
                        CustomerVisitUpdateRequest.class
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        customerVisitService
                                .update(
                                        id,
                                        req
                                )
        );
    }


    // =========================================================
    // UPDATE - MANUAL CHECKOUT NOT ALLOWED
    // =========================================================

    @Test
    void update_ShouldReject_WhenCheckoutTimeProvided() {

        UUID id =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.CHECKED_IN
                );

        CustomerVisitUpdateRequest req =
                mock(
                        CustomerVisitUpdateRequest.class
                );

        when(req.checkOutTime())
                .thenReturn(
                        LocalDateTime.now()
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(visit)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        customerVisitService
                                .update(
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
    // UPDATE - STATUS MUST BE CANCELLED
    // =========================================================

    @Test
    void update_ShouldReject_WhenStatusIsNotCancelled() {

        UUID id =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.CHECKED_IN
                );

        CustomerVisitUpdateRequest req =
                mock(
                        CustomerVisitUpdateRequest.class
                );

        when(req.status())
                .thenReturn(
                        VisitStatus.IN_PROGRESS
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(visit)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        customerVisitService
                                .update(
                                        id,
                                        req
                                )
        );

        verifyNoInteractions(
                invoiceRepo
        );
    }


    // =========================================================
    // UPDATE - CURRENT STATUS MUST BE CHECKED IN
    // =========================================================

    @Test
    void update_ShouldRejectCancellation_WhenVisitAlreadyInProgress() {

        UUID id =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.IN_PROGRESS
                );

        CustomerVisitUpdateRequest req =
                mock(
                        CustomerVisitUpdateRequest.class
                );

        when(req.status())
                .thenReturn(
                        VisitStatus.CANCELLED
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(visit)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        customerVisitService
                                .update(
                                        id,
                                        req
                                )
        );

        verifyNoInteractions(
                invoiceRepo
        );
    }


    // =========================================================
    // UPDATE - CANCEL SUCCESS
    // =========================================================

    @Test
    void update_ShouldCancelCheckedInVisit() {

        UUID id =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.CHECKED_IN
                );

        CustomerVisitUpdateRequest req =
                mock(
                        CustomerVisitUpdateRequest.class
                );

        when(req.status())
                .thenReturn(
                        VisitStatus.CANCELLED
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                invoiceRepo
                        .findAllByVisit_VisitId(id)
        ).thenReturn(
                List.of()
        );

        when(repo.save(visit))
                .thenReturn(visit);

        var result =
                customerVisitService.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                VisitStatus.CANCELLED,
                visit.getStatus()
        );

        assertNotNull(
                visit.getCheckOutTime()
        );

        verify(repo)
                .save(visit);
    }


    // =========================================================
    // UPDATE - CANCEL INVOICES
    // =========================================================

    @Test
    void update_ShouldCancelInvoices_WhenVisitCancelled() {

        UUID id =
                UUID.randomUUID();

        UUID invoiceId =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.CHECKED_IN
                );

        Invoice invoice =
                mock(Invoice.class);

        when(invoice.getInvoiceId())
                .thenReturn(
                        invoiceId
                );

        CustomerVisitUpdateRequest req =
                mock(
                        CustomerVisitUpdateRequest.class
                );

        when(req.status())
                .thenReturn(
                        VisitStatus.CANCELLED
                );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                invoiceRepo
                        .findAllByVisit_VisitId(id)
        ).thenReturn(
                List.of(invoice)
        );

        when(repo.save(visit))
                .thenReturn(visit);

        customerVisitService.update(
                id,
                req
        );

        verify(invoiceService)
                .cancel(invoiceId);

        assertEquals(
                VisitStatus.CANCELLED,
                visit.getStatus()
        );

        assertNotNull(
                visit.getCheckOutTime()
        );
    }


    // =========================================================
    // DELETE - MISSING
    // =========================================================

    @Test
    void delete_ShouldThrowNotFound_WhenVisitMissing() {

        UUID id =
                UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        customerVisitService
                                .delete(id)
        );

        verify(
                repo,
                never()
        ).deleteById(any());
    }


    // =========================================================
    // DELETE - EXISTING VISIT CANNOT BE DELETED
    // =========================================================

    @Test
    void delete_ShouldReject_WhenVisitExists() {

        UUID id =
                UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.CHECKED_IN
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(visit)
                );

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                customerVisitService
                                        .delete(id)
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Không thể xóa lượt khám"
                        )
        );

        verify(
                repo,
                never()
        ).deleteById(any());
    }
}
