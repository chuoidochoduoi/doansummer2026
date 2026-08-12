package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.customerVisit.CustomerVisitCreateRequest;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitUpdateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceResponse;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.InsuranceRule;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.InsuranceRuleRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerVisitServiceTest {

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

    @InjectMocks
    private CustomerVisitService customerVisitService;


    // =========================================================
    // HELPERS
    // =========================================================

    private Profile profile(UUID id) {
        return Profile.builder()
                .profileId(id)
                .fullName("Nguyen Van A")
                .phone("0901234567")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
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
                .checkInTime(LocalDateTime.now().minusMinutes(30))
                .status(status)
                .build();
    }

    private InvoiceResponse mockInvoiceResponse(UUID invoiceId) {
        InvoiceResponse response = mock(InvoiceResponse.class);
        doReturn(invoiceId).when(response).invoiceId();
        return response;
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void search_ShouldReturnMappedPage() {

        UUID customerId = UUID.randomUUID();

        LocalDateTime from =
                LocalDateTime.of(2026, 8, 1, 0, 0);

        LocalDateTime to =
                LocalDateTime.of(2026, 8, 31, 23, 59);

        var pageable =
                PageRequest.of(0, 10);

        CustomerVisit visit =
                visit(
                        UUID.randomUUID(),
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
                new PageImpl<>(List.of(visit))
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
    }


    // =========================================================
    // GET / FIND
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenFound() {

        UUID id = UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.CHECKED_IN
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(visit));

        assertSame(
                visit,
                customerVisitService.findById(id)
        );
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> customerVisitService.findById(id)
        );
    }


    @Test
    void get_ShouldReturnResponse() {

        UUID id = UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.CHECKED_IN
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(visit));

        assertNotNull(
                customerVisitService.get(id)
        );
    }


    // =========================================================
    // CREATE - NO SERVICE
    // =========================================================

    @Test
    void create_ShouldReject_WhenServiceIdsNull() {

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(null);

        assertThrows(
                BadRequestException.class,
                () -> customerVisitService.create(req)
        );

        verifyNoInteractions(profileRepo);
    }


    @Test
    void create_ShouldReject_WhenServiceIdsEmpty() {

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of());

        assertThrows(
                BadRequestException.class,
                () -> customerVisitService.create(req)
        );
    }


    // =========================================================
    // CREATE REGISTERED CUSTOMER - NOT FOUND
    // =========================================================

    @Test
    void create_ShouldThrow_WhenRegisteredCustomerMissing() {

        UUID customerId = UUID.randomUUID();

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(UUID.randomUUID()));

        when(req.customerId())
                .thenReturn(customerId);

        when(profileRepo.findById(customerId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> customerVisitService.create(req)
        );
    }


    // =========================================================
    // CREATE - PROFILE LOCK MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenCustomerCannotBeLocked() {

        UUID customerId = UUID.randomUUID();

        Profile profile =
                profile(customerId);

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(UUID.randomUUID()));

        when(req.customerId())
                .thenReturn(customerId);

        when(profileRepo.findById(customerId))
                .thenReturn(Optional.of(profile));

        when(profileRepo.findByIdForUpdate(customerId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> customerVisitService.create(req)
        );
    }


    // =========================================================
    // CREATE GUEST - REUSE PROFILE BY PHONE
    // =========================================================

    @Test
    void create_ShouldReuseExistingGuestProfile_WhenPhoneExists() {

        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

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

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(serviceId));

        when(req.customerId())
                .thenReturn(null);

        when(req.guestPhone())
                .thenReturn(" 0901234567 ");

        when(profileRepo.findFirstByPhone("0901234567"))
                .thenReturn(Optional.of(existing));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(existing));

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(Optional.empty());

        when(repo.save(any(CustomerVisit.class)))
                .thenAnswer(invocation -> {
                    CustomerVisit visit =
                            invocation.getArgument(0);

                    visit.setVisitId(visitId);

                    return visit;
                });

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(UUID.randomUUID());

        when(invoiceService.create(any()))
                .thenReturn(invoiceResponse);

        var result =
                customerVisitService.create(req);

        assertNotNull(result);

        verify(profileRepo, never())
                .save(any(Profile.class));
    }


    // =========================================================
    // CREATE GUEST - NO PHONE
    // =========================================================

    @Test
    void create_ShouldCreateNewGuestProfile_WhenPhoneNull() {

        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        MedicalService medicalService =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(serviceId));

        when(req.customerId())
                .thenReturn(null);

        when(req.guestPhone())
                .thenReturn(null);

        when(req.guestFullName())
                .thenReturn("Guest A");

        when(req.guestAddress())
                .thenReturn("Ha Noi");

        when(req.guestDateOfBirth())
                .thenReturn(LocalDate.of(2000, 1, 1));

        when(req.guestGender())
                .thenReturn(Gender.MALE);


        // Khi service tạo guest mới -> save profile
        when(profileRepo.save(any(Profile.class)))
                .thenAnswer(invocation -> {

                    Profile p = invocation.getArgument(0);

                    p.setProfileId(profileId);

                    return p;
                });


        // Sau khi save, service gọi findByIdForUpdate
        Profile lockedProfile =
                Profile.builder()
                        .profileId(profileId)
                        .fullName("Guest A")
                        .phone(null)
                        .address("Ha Noi")
                        .dateOfBirth(LocalDate.of(2000, 1, 1))
                        .gender(Gender.MALE)
                        .build();

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(lockedProfile));


        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(Optional.empty());


        when(repo.save(any(CustomerVisit.class)))
                .thenAnswer(invocation -> {

                    CustomerVisit visit =
                            invocation.getArgument(0);

                    visit.setVisitId(visitId);

                    return visit;
                });


        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(medicalService));


        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(UUID.randomUUID());

        when(invoiceService.create(any()))
                .thenReturn(invoiceResponse);


        // ACT
        var result =
                customerVisitService.create(req);


        // ASSERT
        assertNotNull(result);


        // Profile chỉ được save đúng 1 lần bởi production service
        verify(profileRepo, times(1))
                .save(argThat(p ->
                        "Guest A".equals(p.getFullName())
                                && p.getPhone() == null
                                && "Ha Noi".equals(p.getAddress())
                                && LocalDate.of(2000, 1, 1)
                                .equals(p.getDateOfBirth())
                                && p.getGender() == Gender.MALE
                ));

        verify(profileRepo)
                .findByIdForUpdate(profileId);

        verify(repo)
                .save(argThat(v ->
                        v.getCustomer() == lockedProfile
                                && v.getStatus() == VisitStatus.CHECKED_IN
                                && v.getCheckInTime() != null
                ));

        verify(invoiceService)
                .create(any());
    }


    // =========================================================
    // CREATE GUEST - BLANK PHONE + DEFAULT GENDER OTHER
    // =========================================================

    @Test
    void create_ShouldCreateGuestWithOtherGender_WhenGenderNull() {

        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(serviceId));

        when(req.guestPhone())
                .thenReturn(" ");

        when(req.guestFullName())
                .thenReturn("Guest");

        when(profileRepo.save(any(Profile.class)))
                .thenAnswer(invocation -> {
                    Profile p = invocation.getArgument(0);
                    p.setProfileId(profileId);
                    return p;
                });

        Profile locked =
                Profile.builder()
                        .profileId(profileId)
                        .fullName("Guest")
                        .gender(Gender.OTHER)
                        .build();

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(locked));

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(Optional.empty());

        when(repo.save(any(CustomerVisit.class)))
                .thenAnswer(invocation -> {
                    CustomerVisit v = invocation.getArgument(0);
                    v.setVisitId(visitId);
                    return v;
                });

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV",
                        BigDecimal.ZERO,
                        DepartmentType.EXAMINATION
                );

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(UUID.randomUUID());

        when(invoiceService.create(any()))
                .thenReturn(invoiceResponse);

        customerVisitService.create(req);

        verify(profileRepo).save(argThat(p ->
                p.getGender() == Gender.OTHER
        ));
    }


    // =========================================================
    // CREATE - ACTIVE VISIT
    // =========================================================

    @Test
    void create_ShouldReject_WhenCustomerAlreadyHasActiveVisit() {

        UUID profileId = UUID.randomUUID();

        Profile profile =
                profile(profileId);

        CustomerVisit active =
                visit(
                        UUID.randomUUID(),
                        profile,
                        VisitStatus.IN_PROGRESS
                );

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(UUID.randomUUID()));

        when(req.customerId())
                .thenReturn(profileId);

        when(profileRepo.findById(profileId))
                .thenReturn(Optional.of(profile));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(profile));

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(Optional.of(active));

        ConflictException ex =
                assertThrows(
                        ConflictException.class,
                        () -> customerVisitService.create(req)
                );

        assertTrue(
                ex.getMessage().contains("VIS-")
        );

        verify(repo, never())
                .save(any(CustomerVisit.class));
    }


    // =========================================================
    // CREATE - APPOINTMENT MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenAppointmentMissing() {

        UUID profileId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        Profile profile =
                profile(profileId);

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(UUID.randomUUID()));

        when(req.customerId())
                .thenReturn(profileId);

        when(req.appointmentId())
                .thenReturn(appointmentId);

        when(profileRepo.findById(profileId))
                .thenReturn(Optional.of(profile));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(profile));

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(Optional.empty());

        when(appointmentRepo.findById(appointmentId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> customerVisitService.create(req)
        );
    }


    // =========================================================
    // CREATE - APPOINTMENT SUCCESS
    // =========================================================

    @Test
    void create_ShouldMarkAppointmentCheckedIn() {

        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        Profile profile =
                profile(profileId);

        Appointment appointment =
                Appointment.builder()
                        .appointmentId(appointmentId)
                        .status(AppointmentStatus.PENDING)
                        .build();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(serviceId));

        when(req.customerId())
                .thenReturn(profileId);

        when(req.appointmentId())
                .thenReturn(appointmentId);

        when(profileRepo.findById(profileId))
                .thenReturn(Optional.of(profile));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(profile));

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(Optional.empty());

        when(appointmentRepo.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        when(repo.save(any(CustomerVisit.class)))
                .thenAnswer(invocation -> {
                    CustomerVisit v = invocation.getArgument(0);
                    v.setVisitId(visitId);
                    return v;
                });

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(UUID.randomUUID());

        when(invoiceService.create(any()))
                .thenReturn(invoiceResponse);

        customerVisitService.create(req);

        assertEquals(
                AppointmentStatus.CHECKED_IN,
                appointment.getStatus()
        );

        verify(appointmentRepo)
                .save(appointment);
    }


    // =========================================================
    // CREATE - SERVICE MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenServiceMissing() {

        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        Profile profile =
                profile(profileId);

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(serviceId));

        when(req.customerId())
                .thenReturn(profileId);

        when(profileRepo.findById(profileId))
                .thenReturn(Optional.of(profile));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(profile));

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(Optional.empty());

        when(repo.save(any(CustomerVisit.class)))
                .thenAnswer(invocation -> {
                    CustomerVisit v = invocation.getArgument(0);
                    v.setVisitId(UUID.randomUUID());
                    return v;
                });

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> customerVisitService.create(req)
        );

        verify(invoiceService, never())
                .create(any());
    }


    // =========================================================
    // CREATE - NO INSURANCE
    // =========================================================

    @Test
    void create_ShouldCreateInvoiceWithoutInsuranceDiscount() {

        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID issuerId = UUID.randomUUID();

        Profile profile =
                profile(profileId);

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham tong quat",
                        "DV001",
                        new BigDecimal("200000"),
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(serviceId));

        when(req.customerId())
                .thenReturn(profileId);

        when(req.issuedById())
                .thenReturn(issuerId);

        when(profileRepo.findById(profileId))
                .thenReturn(Optional.of(profile));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(profile));

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(Optional.empty());

        when(repo.save(any(CustomerVisit.class)))
                .thenAnswer(invocation -> {
                    CustomerVisit v = invocation.getArgument(0);
                    v.setVisitId(visitId);
                    return v;
                });

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(UUID.randomUUID());

        when(invoiceService.create(any()))
                .thenReturn(invoiceResponse);

        customerVisitService.create(req);

        verifyNoInteractions(
                insuranceRuleRepo
        );

        verify(invoiceService)
                .create(argThat(invoice ->
                        profileId.equals(invoice.customerId())
                                && visitId.equals(invoice.visitId())
                                && issuerId.equals(invoice.issuedById())
                                && BigDecimal.ZERO.compareTo(invoice.discount()) == 0
                                && invoice.items() != null
                                && invoice.items().size() == 1
                ));
    }


    // =========================================================
    // CREATE - INSURANCE WITHOUT MATCHING RULE
    // =========================================================

    @Test
    void create_ShouldUseZeroDiscount_WhenInsuranceRuleMissing() {

        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID insuranceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        Profile profile =
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
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(serviceId));

        when(req.customerId())
                .thenReturn(profileId);

        when(req.insuranceId())
                .thenReturn(insuranceId);

        when(profileRepo.findById(profileId))
                .thenReturn(Optional.of(profile));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(profile));

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(Optional.empty());

        when(repo.save(any(CustomerVisit.class)))
                .thenAnswer(invocation -> {
                    CustomerVisit v = invocation.getArgument(0);
                    v.setVisitId(visitId);
                    return v;
                });

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(
                insuranceRuleRepo
                        .findByInsurance_InsuranceIdAndDepartmentType(
                                insuranceId,
                                DepartmentType.EXAMINATION
                        )
        ).thenReturn(Optional.empty());

        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(UUID.randomUUID());

        when(invoiceService.create(any()))
                .thenReturn(invoiceResponse);

        customerVisitService.create(req);

        verify(invoiceService)
                .create(argThat(invoice ->
                        BigDecimal.ZERO.compareTo(invoice.discount()) == 0
                                && invoice.items().get(0).discountPercent()
                                .compareTo(BigDecimal.ZERO) == 0
                ));
    }


    // =========================================================
    // CREATE - INSURANCE DISCOUNT
    // =========================================================

    @Test
    void create_ShouldCalculateInsuranceDiscount() {

        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID insuranceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        Profile profile =
                profile(profileId);

        MedicalService service =
                medicalService(
                        serviceId,
                        "Xet nghiem",
                        "XN01",
                        new BigDecimal("200000"),
                        DepartmentType.PARACLINICAL
                );

        InsuranceRule rule =
                mock(InsuranceRule.class);

        when(rule.getDiscountPercent())
                .thenReturn(
                        new BigDecimal("20")
                );

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(serviceId));

        when(req.customerId())
                .thenReturn(profileId);

        when(req.insuranceId())
                .thenReturn(insuranceId);

        when(profileRepo.findById(profileId))
                .thenReturn(Optional.of(profile));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(profile));

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(Optional.empty());

        when(repo.save(any(CustomerVisit.class)))
                .thenAnswer(invocation -> {
                    CustomerVisit v = invocation.getArgument(0);
                    v.setVisitId(visitId);
                    return v;
                });

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(
                insuranceRuleRepo
                        .findByInsurance_InsuranceIdAndDepartmentType(
                                insuranceId,
                                DepartmentType.PARACLINICAL
                        )
        ).thenReturn(Optional.of(rule));

        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(UUID.randomUUID());

        when(invoiceService.create(any()))
                .thenReturn(invoiceResponse);

        customerVisitService.create(req);

        verify(invoiceService)
                .create(argThat(invoice -> {

                    if (invoice.discount() == null) {
                        return false;
                    }

                    if (invoice.items() == null
                            || invoice.items().size() != 1) {
                        return false;
                    }

                    var item =
                            invoice.items().get(0);

                    return invoice.discount()
                            .compareTo(new BigDecimal("40000.00")) == 0

                            && item.discountPercent()
                            .compareTo(new BigDecimal("20")) == 0

                            && item.discountAmount()
                            .compareTo(new BigDecimal("40000.00")) == 0

                            && item.finalPrice()
                            .compareTo(new BigDecimal("160000.00")) == 0;
                }));
    }


    // =========================================================
    // CREATE - MULTIPLE SERVICES / TOTAL DISCOUNT
    // =========================================================

    @Test
    void create_ShouldSumDiscountAcrossMultipleServices() {

        UUID profileId = UUID.randomUUID();
        UUID insuranceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        UUID s1Id = UUID.randomUUID();
        UUID s2Id = UUID.randomUUID();

        Profile profile =
                profile(profileId);

        MedicalService s1 =
                medicalService(
                        s1Id,
                        "Service 1",
                        "S1",
                        new BigDecimal("100000"),
                        DepartmentType.EXAMINATION
                );

        MedicalService s2 =
                medicalService(
                        s2Id,
                        "Service 2",
                        "S2",
                        new BigDecimal("200000"),
                        DepartmentType.PARACLINICAL
                );

        InsuranceRule rule1 =
                mock(InsuranceRule.class);

        InsuranceRule rule2 =
                mock(InsuranceRule.class);

        when(rule1.getDiscountPercent())
                .thenReturn(new BigDecimal("10"));

        when(rule2.getDiscountPercent())
                .thenReturn(new BigDecimal("20"));

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(s1Id, s2Id));

        when(req.customerId())
                .thenReturn(profileId);

        when(req.insuranceId())
                .thenReturn(insuranceId);

        when(profileRepo.findById(profileId))
                .thenReturn(Optional.of(profile));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(profile));

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(Optional.empty());

        when(repo.save(any(CustomerVisit.class)))
                .thenAnswer(invocation -> {
                    CustomerVisit v = invocation.getArgument(0);
                    v.setVisitId(visitId);
                    return v;
                });

        when(serviceRepo.findById(s1Id))
                .thenReturn(Optional.of(s1));

        when(serviceRepo.findById(s2Id))
                .thenReturn(Optional.of(s2));

        when(
                insuranceRuleRepo
                        .findByInsurance_InsuranceIdAndDepartmentType(
                                insuranceId,
                                DepartmentType.EXAMINATION
                        )
        ).thenReturn(Optional.of(rule1));

        when(
                insuranceRuleRepo
                        .findByInsurance_InsuranceIdAndDepartmentType(
                                insuranceId,
                                DepartmentType.PARACLINICAL
                        )
        ).thenReturn(Optional.of(rule2));

        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(UUID.randomUUID());

        when(invoiceService.create(any()))
                .thenReturn(invoiceResponse);

        customerVisitService.create(req);

        /*
         * 100000 * 10% = 10000
         * 200000 * 20% = 40000
         * totalDiscount = 50000
         */
        verify(invoiceService)
                .create(argThat(invoice ->
                        invoice.discount()
                                .compareTo(
                                        new BigDecimal("50000.00")
                                ) == 0
                                && invoice.items() != null
                                && invoice.items().size() == 2
                ));
    }


    // =========================================================
    // CREATE - CAPTURE VISIT
    // =========================================================

    @Test
    void create_ShouldCreateCheckedInVisit() {

        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        Profile profile =
                profile(profileId);

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham",
                        "DV01",
                        BigDecimal.ZERO,
                        DepartmentType.EXAMINATION
                );

        CustomerVisitCreateRequest req =
                mock(CustomerVisitCreateRequest.class);

        when(req.serviceIds())
                .thenReturn(List.of(serviceId));

        when(req.customerId())
                .thenReturn(profileId);

        when(profileRepo.findById(profileId))
                .thenReturn(Optional.of(profile));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(profile));

        when(
                repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                        eq(profileId),
                        anyList()
                )
        ).thenReturn(Optional.empty());

        when(repo.save(any(CustomerVisit.class)))
                .thenAnswer(invocation -> {
                    CustomerVisit v = invocation.getArgument(0);
                    v.setVisitId(visitId);
                    return v;
                });

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        InvoiceResponse invoiceResponse =
                mockInvoiceResponse(UUID.randomUUID());

        when(invoiceService.create(any()))
                .thenReturn(invoiceResponse);

        customerVisitService.create(req);

        ArgumentCaptor<CustomerVisit> captor =
                ArgumentCaptor.forClass(
                        CustomerVisit.class
                );

        verify(repo)
                .save(captor.capture());

        CustomerVisit saved =
                captor.getValue();

        assertSame(
                profile,
                saved.getCustomer()
        );

        assertEquals(
                VisitStatus.CHECKED_IN,
                saved.getStatus()
        );

        assertNotNull(
                saved.getCheckInTime()
        );

        assertNull(
                saved.getAppointment()
        );
    }


    // =========================================================
    // UPDATE - STATUS
    // =========================================================

    @Test
    void update_ShouldUpdateStatus() {

        UUID id = UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.CHECKED_IN
                );

        CustomerVisitUpdateRequest req =
                mock(CustomerVisitUpdateRequest.class);

        when(req.status())
                .thenReturn(VisitStatus.IN_PROGRESS);

        when(repo.findById(id))
                .thenReturn(Optional.of(visit));

        when(repo.save(visit))
                .thenReturn(visit);

        customerVisitService.update(
                id,
                req
        );

        assertEquals(
                VisitStatus.IN_PROGRESS,
                visit.getStatus()
        );
    }


    // =========================================================
    // UPDATE - CHECKOUT PROVIDED
    // =========================================================

    @Test
    void update_ShouldUseProvidedCheckoutTime() {

        UUID id = UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.IN_PROGRESS
                );

        LocalDateTime checkout =
                LocalDateTime.of(
                        2026,
                        8,
                        10,
                        15,
                        0
                );

        CustomerVisitUpdateRequest req =
                mock(CustomerVisitUpdateRequest.class);

        when(req.checkOutTime())
                .thenReturn(checkout);

        when(repo.findById(id))
                .thenReturn(Optional.of(visit));

        when(repo.save(visit))
                .thenReturn(visit);

        customerVisitService.update(
                id,
                req
        );

        assertEquals(
                checkout,
                visit.getCheckOutTime()
        );
    }


    // =========================================================
    // UPDATE - COMPLETED AUTO CHECKOUT
    // =========================================================

    @Test
    void update_ShouldAutomaticallySetCheckout_WhenCompleted() {

        UUID id = UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.IN_PROGRESS
                );

        visit.setCheckOutTime(null);

        CustomerVisitUpdateRequest req =
                mock(CustomerVisitUpdateRequest.class);

        when(req.status())
                .thenReturn(VisitStatus.COMPLETED);

        when(repo.findById(id))
                .thenReturn(Optional.of(visit));

        when(repo.save(visit))
                .thenReturn(visit);

        LocalDateTime before =
                LocalDateTime.now();

        customerVisitService.update(
                id,
                req
        );

        LocalDateTime after =
                LocalDateTime.now();

        assertEquals(
                VisitStatus.COMPLETED,
                visit.getStatus()
        );

        assertNotNull(
                visit.getCheckOutTime()
        );

        assertFalse(
                visit.getCheckOutTime()
                        .isBefore(before)
        );

        assertFalse(
                visit.getCheckOutTime()
                        .isAfter(after)
        );
    }


    // =========================================================
    // UPDATE - COMPLETED PRESERVE EXISTING CHECKOUT
    // =========================================================

    @Test
    void update_ShouldKeepExistingCheckout_WhenAlreadySet() {

        UUID id = UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.IN_PROGRESS
                );

        LocalDateTime oldCheckout =
                LocalDateTime.now()
                        .minusMinutes(10);

        visit.setCheckOutTime(oldCheckout);

        CustomerVisitUpdateRequest req =
                mock(CustomerVisitUpdateRequest.class);

        when(req.status())
                .thenReturn(VisitStatus.COMPLETED);

        when(repo.findById(id))
                .thenReturn(Optional.of(visit));

        when(repo.save(visit))
                .thenReturn(visit);

        customerVisitService.update(
                id,
                req
        );

        assertEquals(
                oldCheckout,
                visit.getCheckOutTime()
        );
    }


    // =========================================================
    // UPDATE - EMPTY REQUEST
    // =========================================================

    @Test
    void update_ShouldSaveWithoutChanges_WhenRequestEmpty() {

        UUID id = UUID.randomUUID();

        CustomerVisit visit =
                visit(
                        id,
                        profile(UUID.randomUUID()),
                        VisitStatus.CHECKED_IN
                );

        CustomerVisitUpdateRequest req =
                mock(CustomerVisitUpdateRequest.class);

        when(repo.findById(id))
                .thenReturn(Optional.of(visit));

        when(repo.save(visit))
                .thenReturn(visit);

        var result =
                customerVisitService.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                VisitStatus.CHECKED_IN,
                visit.getStatus()
        );

        verify(repo)
                .save(visit);
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenVisitMissing() {

        UUID id =
                UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> customerVisitService.delete(id)
        );

        verify(repo, never())
                .deleteById(id);
    }


    @Test
    void delete_ShouldDelete_WhenVisitExists() {

        UUID id =
                UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(true);

        customerVisitService.delete(id);

        verify(repo)
                .deleteById(id);
    }
}