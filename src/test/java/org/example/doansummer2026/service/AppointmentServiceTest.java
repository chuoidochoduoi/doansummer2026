package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.appointment.*;
import org.example.doansummer2026.dto.invoice.InvoiceResponse;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final ZoneId CLINIC_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");


    @Mock
    private AppointmentRepository repo;

    @Mock
    private ProfileRepository profileRepo;

    @Mock
    private AccountRepository accountRepo;

    @Mock
    private CustomerVisitRepository visitRepo;

    @Mock
    private MedicalServiceRepository serviceRepo;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private InsuranceRuleRepository insuranceRuleRepository;

    @Mock
    private StaffInfoRepository staffRepo;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ShiftConfigRepository shiftConfigRepository;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private FamilyAccessService familyAccessService;

    @Mock
    private CustomerVisitService customerVisitService;

    @InjectMocks
    private AppointmentService appointmentService;


    // =========================================================
    // HELPERS
    // =========================================================

    @Test
    void checkIn_WithoutOptionalContact_CreatesSeparateGuestProfile() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        Appointment guest = appointmentForToday(appointmentId, AppointmentStatus.PENDING, null);
        guest.setIsGuest(true);
        guest.setGuestFullName("Nguyễn Văn An");
        guest.setServices(Set.of(service(UUID.randomUUID(), "Xét nghiệm")));
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(guest));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        when(profileRepo.save(any(Profile.class))).thenAnswer(invocation -> {
            Profile created = invocation.getArgument(0);
            assertEquals("Nguyễn Văn An", created.getFullName());
            assertNull(created.getPhone());
            assertNull(created.getEmail());
            assertNull(created.getAddress());
            assertEquals(Gender.MALE, created.getGender());
            created.setProfileId(profileId);
            return created;
        });
        // Stop at the row lock; this test concerns profile selection, not billing.
        when(profileRepo.findByIdForUpdate(profileId)).thenReturn(Optional.empty());
        AppointmentCheckInRequest request = new AppointmentCheckInRequest(
                appointmentId, null, staffId, "Nguyễn Văn An", "", "", "",
                clinicToday().minusYears(20), 20, Gender.MALE);

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.checkIn(request));

        assertEquals("Không tìm thấy hồ sơ bệnh nhân", error.getMessage());
        verify(profileRepo).save(any(Profile.class));
        verify(profileRepo, never()).findFirstByPhone(any());
        verify(profileRepo, never()).findFirstByPhoneIn(any());
        verify(profileRepo, never()).findFirstByEmailIgnoreCase(any());
        verifyNoInteractions(invoiceService);
    }

    private LocalDate clinicToday() {
        return LocalDate.now(CLINIC_ZONE);
    }

    /**
     * Dùng +2 ngày thay vì now().plusDays(1)
     * để test không bị lỗi khi chạy đúng thời điểm qua 0h.
     */
    private LocalDateTime futureAppointmentTime() {
        return clinicToday()
                .plusDays(2)
                .atTime(10, 0);
    }

    private Profile customer(UUID profileId) {
        return Profile.builder()
                .profileId(profileId)
                .fullName("Nguyen Van A")
                .phone("0901234567")
                .gender(Gender.MALE)
                .dateOfBirth(
                        clinicToday().minusYears(25)
                )
                .build();
    }

    private MedicalService service(
            UUID id,
            String name
    ) {
        return MedicalService.builder()
                .serviceId(id)
                .name(name)
                .serviceCode("DV01")
                .price(
                        new BigDecimal("100000")
                )
                .status(ServiceStatus.ACTIVE)
                .build();
    }

    private Appointment appointment(
            UUID id,
            AppointmentStatus status,
            Profile customer
    ) {
        return Appointment.builder()
                .appointmentId(id)
                .customer(customer)
                .scheduledAt(
                        futureAppointmentTime()
                )
                .status(status)
                .services(new HashSet<>())
                .build();
    }

    /**
     * Appointment cho check-in phải đúng ngày hiện tại
     * theo timezone của phòng khám.
     */
    private Appointment appointmentForToday(
            UUID id,
            AppointmentStatus status,
            Profile customer
    ) {
        return Appointment.builder()
                .appointmentId(id)
                .customer(customer)
                .scheduledAt(
                        clinicToday().atTime(9, 0)
                )
                .status(status)
                .services(new HashSet<>())
                .build();
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenExists() {

        UUID id =
                UUID.randomUUID();

        Appointment appointment =
                appointment(
                        id,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(appointment)
                );

        assertSame(
                appointment,
                appointmentService.findById(id)
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
                        appointmentService.findById(id)
        );
    }


    // =========================================================
    // GET
    // =========================================================

    @Test
    void get_ShouldReturnResponse_WhenFound() {

        UUID id =
                UUID.randomUUID();

        Appointment appointment =
                appointment(
                        id,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(appointment)
                );

        assertNotNull(
                appointmentService.get(id)
        );
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void search_ShouldDelegateToRepository() {

        UUID customerId =
                UUID.randomUUID();

        var pageable =
                PageRequest.of(
                        0,
                        10
                );

        when(
                repo.search(
                        customerId,
                        AppointmentStatus.PENDING.name(),
                        null,
                        null,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of()
                )
        );

        var result =
                appointmentService.search(
                        customerId,
                        AppointmentStatus.PENDING,
                        null,
                        null,
                        pageable
                );

        assertNotNull(result);

        verify(repo).search(
                customerId,
                AppointmentStatus.PENDING.name(),
                null,
                null,
                pageable
        );
    }


    @Test
    void search_ShouldPassNullStatus_WhenStatusNull() {

        var pageable =
                PageRequest.of(
                        0,
                        10
                );

        when(
                repo.search(
                        null,
                        null,
                        null,
                        null,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of()
                )
        );

        appointmentService.search(
                null,
                null,
                null,
                null,
                pageable
        );

        verify(repo).search(
                null,
                null,
                null,
                null,
                pageable
        );
    }


    // =========================================================
    // CREATE - ACCOUNT NOT FOUND
    // =========================================================

    @Test
    void create_ShouldThrow_WhenAccountMissing() {

        UUID accountId =
                UUID.randomUUID();

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId())
                .thenReturn(accountId);

        when(accountRepo.findById(accountId))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - INVALID ROLE
    // =========================================================

    @Test
    void create_ShouldReject_WhenAccountRoleIsNull() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(null)
                        .build();

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId())
                .thenReturn(accountId);

        when(accountRepo.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        assertThrows(
                BadRequestException.class,
                () ->
                        appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - PROFILE MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenProfileMissing() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId())
                .thenReturn(accountId);

        when(accountRepo.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - APPOINTMENT CONFLICT
    // =========================================================

    @Test
    void create_ShouldReject_WhenAppointmentConflictExists() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        LocalDateTime scheduledAt =
                futureAppointmentTime();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.serviceIds()).thenReturn(Set.of(UUID.randomUUID()));

        when(req.customerId())
                .thenReturn(accountId);

        when(req.scheduledAt())
                .thenReturn(scheduledAt);

        when(accountRepo.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.existsCustomerConflict(
                        eq(profileId),
                        eq(
                                List.of(
                                        AppointmentStatus.PENDING,
                                        AppointmentStatus.RESCHEDULED
                                )
                        ),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                )
        ).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () ->
                        appointmentService.create(req)
        );

        verify(repo).existsCustomerConflict(eq(profileId), anyList(), any(), any());

        verify(
                repo,
                never()
        ).save(any(Appointment.class));
    }


    // =========================================================
    // CREATE - SHIFT MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenShiftMissing() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        UUID shiftId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.serviceIds()).thenReturn(Set.of(UUID.randomUUID()));

        when(req.customerId())
                .thenReturn(accountId);

        when(req.scheduledAt())
                .thenReturn(
                        futureAppointmentTime()
                );

        when(req.shiftId())
                .thenReturn(shiftId);

        when(accountRepo.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(
                shiftConfigRepository.findById(
                        shiftId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - SERVICE MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenServiceMissing() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId())
                .thenReturn(accountId);

        when(req.scheduledAt())
                .thenReturn(
                        futureAppointmentTime()
                );

        when(req.serviceIds())
                .thenReturn(
                        Set.of(serviceId)
                );

        when(accountRepo.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - AGE REQUIRED
    // =========================================================

    @Test
    void create_ShouldRejectService_WhenBirthDateMissingAndAgeRestricted() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        customer.setDateOfBirth(null);

        MedicalService service =
                service(
                        serviceId,
                        "Kham nhi"
                );

        service.setMinimumAge(5);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId())
                .thenReturn(accountId);

        when(req.scheduledAt())
                .thenReturn(
                        futureAppointmentTime()
                );

        when(req.serviceIds())
                .thenReturn(
                        Set.of(serviceId)
                );

        when(accountRepo.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(
                        Optional.of(service)
                );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                appointmentService.create(req)
                );

        assertTrue(
                exception.getMessage()
                        .contains("ngày sinh")
        );
    }


    // =========================================================
    // CREATE - TOO YOUNG
    // =========================================================

    @Test
    void create_ShouldRejectService_WhenCustomerTooYoung() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        customer.setDateOfBirth(
                clinicToday().minusYears(10)
        );

        MedicalService service =
                service(
                        serviceId,
                        "Kham nguoi lon"
                );

        service.setMinimumAge(18);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId())
                .thenReturn(accountId);

        when(req.scheduledAt())
                .thenReturn(
                        futureAppointmentTime()
                );

        when(req.serviceIds())
                .thenReturn(
                        Set.of(serviceId)
                );

        when(accountRepo.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(
                        Optional.of(service)
                );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                appointmentService.create(req)
                );

        assertTrue(
                exception.getMessage()
                        .contains("18")
        );
    }


    // =========================================================
    // CREATE - TOO OLD
    // =========================================================

    @Test
    void create_ShouldRejectService_WhenCustomerTooOld() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        customer.setDateOfBirth(
                clinicToday().minusYears(70)
        );

        MedicalService service =
                service(
                        serviceId,
                        "Dich vu gioi han tuoi"
                );

        service.setMaximumAge(60);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId())
                .thenReturn(accountId);

        when(req.scheduledAt())
                .thenReturn(
                        futureAppointmentTime()
                );

        when(req.serviceIds())
                .thenReturn(
                        Set.of(serviceId)
                );

        when(accountRepo.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(
                        Optional.of(service)
                );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                appointmentService.create(req)
                );

        assertTrue(
                exception.getMessage()
                        .contains("60")
        );
    }


    // =========================================================
    // CREATE - GENDER MISSING
    // =========================================================

    @Test
    void create_ShouldRejectService_WhenGenderMissing() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        customer.setGender(null);

        MedicalService service =
                service(
                        serviceId,
                        "Dich vu nu"
                );

        service.setAllowedGender(
                Gender.FEMALE
        );

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId())
                .thenReturn(accountId);

        when(req.scheduledAt())
                .thenReturn(
                        futureAppointmentTime()
                );

        when(req.serviceIds())
                .thenReturn(
                        Set.of(serviceId)
                );

        when(accountRepo.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(
                        Optional.of(service)
                );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                appointmentService.create(req)
                );

        assertTrue(
                exception.getMessage()
                        .contains("giới tính")
        );
    }


    // =========================================================
    // CREATE - WRONG GENDER
    // =========================================================

    @Test
    void create_ShouldRejectService_WhenGenderNotAllowed() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        customer.setGender(
                Gender.MALE
        );

        MedicalService service =
                service(
                        serviceId,
                        "Dich vu nu"
                );

        service.setAllowedGender(
                Gender.FEMALE
        );

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId())
                .thenReturn(accountId);

        when(req.scheduledAt())
                .thenReturn(
                        futureAppointmentTime()
                );

        when(req.serviceIds())
                .thenReturn(
                        Set.of(serviceId)
                );

        when(accountRepo.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(
                        Optional.of(service)
                );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                appointmentService.create(req)
                );

        assertTrue(
                exception.getMessage()
                        .contains("giới tính")
        );
    }


    // =========================================================
    // CREATE SUCCESS
    // =========================================================

    @Test
    void create_ShouldCreatePendingAppointmentSuccessfully() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        MedicalService service =
                service(
                        serviceId,
                        "Kham tong quat"
                );

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        LocalDateTime schedule =
                futureAppointmentTime();

        when(req.customerId())
                .thenReturn(accountId);

        when(req.scheduledAt())
                .thenReturn(schedule);

        when(req.serviceIds())
                .thenReturn(
                        Set.of(serviceId)
                );

        when(accountRepo.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(
                        Optional.of(service)
                );

        when(repo.save(any(Appointment.class)))
                .thenAnswer(
                        invocation -> {

                            Appointment appointment =
                                    invocation.getArgument(0);

                            appointment.setAppointmentId(
                                    UUID.randomUUID()
                            );

                            return appointment;
                        }
                );

        var result =
                appointmentService.create(req);

        assertNotNull(result);

        verify(repo).save(
                argThat(
                        appointment ->
                                appointment.getStatus()
                                        == AppointmentStatus.PENDING
                                        &&
                                        appointment.getCustomer()
                                                == customer
                                        &&
                                        appointment
                                                .getServices()
                                                .contains(service)
                )
        );

        verify(notificationService)
                .notifyStaffByRole(
                        eq(SystemRole.RECEPTIONIST),
                        eq("Lịch hẹn mới"),
                        anyString(),
                        eq("Appointment"),
                        any(UUID.class)
                );
    }


    // =========================================================
    // CREATE GUEST - OTHER GENDER
    // =========================================================

    @Test
    void createForGuest_ShouldRejectOtherGender() {

        AppointmentGuestCreateRequest req =
                mock(
                        AppointmentGuestCreateRequest.class
                );

        when(req.guestGender())
                .thenReturn(
                        Gender.OTHER
                );

        assertThrows(
                BadRequestException.class,
                () ->
                        appointmentService
                                .createForGuest(req)
        );
    }


    // =========================================================
    // CREATE GUEST - CONFLICT
    // =========================================================

    @Test
    void createForGuest_ShouldRejectConflict() {

        AppointmentGuestCreateRequest req =
                mock(
                        AppointmentGuestCreateRequest.class
                );

        LocalDateTime scheduledAt =
                futureAppointmentTime();

        when(req.guestGender())
                .thenReturn(
                        Gender.MALE
                );

        when(req.scheduledAt())
                .thenReturn(
                        scheduledAt
                );

        when(req.guestPhone())
                .thenReturn(
                        "0901234567"
                );

        when(
                repo.existsGuestConflict(
                        eq("0901234567"),
                        isNull(),
                        eq(
                                List.of(
                                        AppointmentStatus.PENDING,
                                        AppointmentStatus.RESCHEDULED
                                )
                        ),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                )
        ).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () ->
                        appointmentService
                                .createForGuest(req)
        );
    }


    // =========================================================
    // CREATE GUEST - NO PHONE / EMAIL
    // =========================================================

    @Test
    void createForGuest_ShouldSkipConflictCheck_WhenPhoneAndEmailBlank() {

        AppointmentGuestCreateRequest req =
                mock(
                        AppointmentGuestCreateRequest.class
                );

        when(req.guestGender())
                .thenReturn(
                        Gender.MALE
                );

        when(req.scheduledAt())
                .thenReturn(
                        futureAppointmentTime()
                );

        when(req.guestPhone())
                .thenReturn(" ");

        when(req.guestEmail())
                .thenReturn(null);

        when(repo.save(any(Appointment.class)))
                .thenAnswer(
                        invocation -> {

                            Appointment appointment =
                                    invocation.getArgument(0);

                            appointment.setAppointmentId(
                                    UUID.randomUUID()
                            );

                            return appointment;
                        }
                );

        var result =
                appointmentService
                        .createForGuest(req);

        assertNotNull(result);

        verify(
                repo,
                never()
        ).existsGuestConflict(
                any(),
                any(),
                anyList(),
                any(),
                any()
        );
    }


    // =========================================================
    // CREATE GUEST SUCCESS
    // =========================================================

    @Test
    void createForGuest_ShouldCreateSuccessfully() {

        UUID serviceId =
                UUID.randomUUID();

        AppointmentGuestCreateRequest req =
                mock(
                        AppointmentGuestCreateRequest.class
                );

        MedicalService service =
                service(
                        serviceId,
                        "Kham guest"
                );

        when(req.guestGender())
                .thenReturn(
                        Gender.FEMALE
                );

        when(req.guestAge())
                .thenReturn(30);

        when(req.guestFullName())
                .thenReturn(
                        "Guest A"
                );

        when(req.guestPhone())
                .thenReturn(
                        "0900000000"
                );

        when(req.scheduledAt())
                .thenReturn(
                        futureAppointmentTime()
                );

        when(req.serviceIds())
                .thenReturn(
                        Set.of(serviceId)
                );

        when(
                repo.existsGuestConflict(
                        any(),
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(
                        Optional.of(service)
                );

        when(repo.save(any(Appointment.class)))
                .thenAnswer(
                        invocation -> {

                            Appointment appointment =
                                    invocation.getArgument(0);

                            appointment.setAppointmentId(
                                    UUID.randomUUID()
                            );

                            return appointment;
                        }
                );

        var result =
                appointmentService
                        .createForGuest(req);

        assertNotNull(result);

        verify(repo).save(
                argThat(
                        appointment ->
                                Boolean.TRUE.equals(
                                        appointment.getIsGuest()
                                )
                                        &&
                                        "Guest A".equals(
                                                appointment
                                                        .getGuestFullName()
                                        )
                                        &&
                                        appointment
                                                .getServices()
                                                .contains(service)
                )
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void update_ShouldUpdateFields() {

        UUID appointmentId =
                UUID.randomUUID();

        Appointment appointment =
                appointment(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        AppointmentUpdateRequest req =
                mock(
                        AppointmentUpdateRequest.class
                );

        LocalDateTime newTime =
                clinicToday()
                        .plusDays(3)
                        .atTime(11, 0);

        when(req.scheduledAt())
                .thenReturn(newTime);

        when(req.status())
                .thenReturn(
                        AppointmentStatus.RESCHEDULED
                );

        when(req.cancelReason())
                .thenReturn(
                        "Reason"
                );

        when(req.guestFullName())
                .thenReturn(
                        "Updated"
                );

        when(req.guestPhone())
                .thenReturn(
                        "0999999999"
                );

        when(repo.findById(appointmentId))
                .thenReturn(
                        Optional.of(appointment)
                );

        when(
                repo.existsOtherCustomerConflict(
                        any(),
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(repo.save(appointment))
                .thenReturn(appointment);

        appointmentService.update(
                appointmentId,
                req
        );

        assertEquals(
                newTime,
                appointment.getScheduledAt()
        );

        assertEquals(
                AppointmentStatus.RESCHEDULED,
                appointment.getStatus()
        );

        assertEquals(
                "Updated",
                appointment.getGuestFullName()
        );
    }


    // =========================================================
    // UPDATE - RESCHEDULE CONFLICT
    // =========================================================

    @Test
    void update_ShouldRejectRescheduleConflict() {

        UUID appointmentId =
                UUID.randomUUID();

        Appointment appointment =
                appointment(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        AppointmentUpdateRequest req =
                mock(
                        AppointmentUpdateRequest.class
                );

        when(req.scheduledAt())
                .thenReturn(
                        clinicToday()
                                .plusDays(3)
                                .atTime(10, 0)
                );

        when(repo.findById(appointmentId))
                .thenReturn(
                        Optional.of(appointment)
                );

        when(
                repo.existsOtherCustomerConflict(
                        any(),
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () ->
                        appointmentService.update(
                                appointmentId,
                                req
                        )
        );
    }


    // =========================================================
    // UPDATE - GUEST SKIPS RESCHEDULE CONFLICT
    // =========================================================

    @Test
    void update_ShouldSkipConflictCheck_WhenAppointmentHasNoCustomer() {

        UUID appointmentId =
                UUID.randomUUID();

        Appointment appointment =
                Appointment.builder()
                        .appointmentId(
                                appointmentId
                        )
                        .status(
                                AppointmentStatus.PENDING
                        )
                        .scheduledAt(
                                futureAppointmentTime()
                        )
                        .build();

        AppointmentUpdateRequest req =
                mock(
                        AppointmentUpdateRequest.class
                );

        when(req.scheduledAt())
                .thenReturn(
                        clinicToday()
                                .plusDays(3)
                                .atTime(10, 0)
                );

        when(repo.findById(appointmentId))
                .thenReturn(
                        Optional.of(appointment)
                );

        when(repo.save(appointment))
                .thenReturn(appointment);

        appointmentService.update(
                appointmentId,
                req
        );

        verify(
                repo,
                never()
        ).existsOtherCustomerConflict(
                any(),
                any(),
                anyList(),
                any(),
                any()
        );
    }


    // =========================================================
    // UPDATE - CANCELLED
    // =========================================================

    @Test
    void update_ShouldNotifyReceptionist_WhenCancelled() {

        UUID appointmentId =
                UUID.randomUUID();

        Profile customer =
                customer(
                        UUID.randomUUID()
                );

        Appointment appointment =
                appointment(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer
                );

        AppointmentUpdateRequest req =
                mock(
                        AppointmentUpdateRequest.class
                );

        when(req.status())
                .thenReturn(
                        AppointmentStatus.CANCELLED
                );

        /*
         * Production hiện bắt buộc nhập lý do hủy.
         */
        when(req.cancelReason())
                .thenReturn(
                        "Patient requested cancellation"
                );

        when(repo.findById(appointmentId))
                .thenReturn(
                        Optional.of(appointment)
                );

        when(repo.save(appointment))
                .thenReturn(appointment);

        appointmentService.update(
                appointmentId,
                req
        );

        assertEquals(
                AppointmentStatus.CANCELLED,
                appointment.getStatus()
        );

        verify(notificationService)
                .notifyStaffByRole(
                        eq(SystemRole.RECEPTIONIST),
                        eq("Lịch hẹn đã bị hủy"),
                        anyString(),
                        eq("Appointment"),
                        eq(appointmentId)
                );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrowNotFound_WhenMissing() {

        UUID id =
                UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        appointmentService.delete(id)
        );

        verify(
                repo,
                never()
        ).deleteById(any());
    }


    @Test
    void delete_ShouldRejectDeletion_WhenAppointmentExists() {

        UUID id =
                UUID.randomUUID();

        Appointment appointment =
                appointment(
                        id,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(appointment)
                );

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                appointmentService.delete(id)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Không xóa lịch hẹn")
        );

        verify(
                repo,
                never()
        ).deleteById(any());
    }


    // =========================================================
    // CHECK IN - APPOINTMENT MISSING
    // =========================================================

    @Test
    void checkIn_ShouldThrow_WhenAppointmentMissing() {

        UUID appointmentId =
                UUID.randomUUID();

        AppointmentCheckInRequest req =
                mock(
                        AppointmentCheckInRequest.class
                );

        when(req.appointmentId())
                .thenReturn(
                        appointmentId
                );

        when(
                repo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        appointmentService.checkIn(req)
        );
    }


    // =========================================================
    // CHECK IN - ALREADY CHECKED IN
    // =========================================================

    @Test
    void checkIn_ShouldReject_WhenAlreadyCheckedIn() {

        UUID appointmentId =
                UUID.randomUUID();

        AppointmentCheckInRequest req =
                mock(
                        AppointmentCheckInRequest.class
                );

        Appointment appointment =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.CHECKED_IN,
                        customer(UUID.randomUUID())
                );

        when(req.appointmentId())
                .thenReturn(
                        appointmentId
                );

        when(
                repo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.of(appointment)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        appointmentService.checkIn(req)
        );
    }


    // =========================================================
    // CHECK IN - INVALID STATUS
    // =========================================================

    @Test
    void checkIn_ShouldReject_WhenStatusNotPending() {

        UUID appointmentId =
                UUID.randomUUID();

        AppointmentCheckInRequest req =
                mock(
                        AppointmentCheckInRequest.class
                );

        Appointment appointment =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.CANCELLED,
                        customer(UUID.randomUUID())
                );

        when(req.appointmentId())
                .thenReturn(
                        appointmentId
                );

        when(
                repo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.of(appointment)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        appointmentService.checkIn(req)
        );
    }


    // =========================================================
    // CHECK IN - STAFF ID MISSING
    // =========================================================

    @Test
    void checkIn_ShouldReject_WhenIssuedByMissing() {

        UUID appointmentId =
                UUID.randomUUID();

        AppointmentCheckInRequest req =
                mock(
                        AppointmentCheckInRequest.class
                );

        Appointment appointment =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        when(req.appointmentId())
                .thenReturn(
                        appointmentId
                );

        when(
                repo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.of(appointment)
        );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                appointmentService.checkIn(req)
                );

        assertTrue(
                exception.getMessage()
                        .contains("nhân viên lễ tân")
        );

        verifyNoInteractions(staffRepo);
    }


    // =========================================================
    // CHECK IN - STAFF NOT FOUND
    // =========================================================

    @Test
    void checkIn_ShouldThrow_WhenStaffMissing() {

        UUID appointmentId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        AppointmentCheckInRequest req =
                mock(
                        AppointmentCheckInRequest.class
                );

        Appointment appointment =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
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
                repo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.of(appointment)
        );

        when(staffRepo.findById(staffId))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        appointmentService.checkIn(req)
        );
    }


    // =========================================================
    // CHECK IN - NO SERVICES
    // =========================================================

    @Test
    void checkIn_ShouldReject_WhenAppointmentHasNoServices() {

        UUID appointmentId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        Profile customer =
                customer(
                        UUID.randomUUID()
                );

        Appointment appointment =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer
                );

        appointment.setServices(
                new HashSet<>()
        );

        AppointmentCheckInRequest req =
                mock(
                        AppointmentCheckInRequest.class
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
                repo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.of(appointment)
        );

        when(staffRepo.findById(staffId))
                .thenReturn(
                        Optional.of(
                                mock(StaffInfo.class)
                        )
                );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                appointmentService.checkIn(req)
                );

        assertTrue(
                exception.getMessage()
                        .contains("chưa chọn dịch vụ")
        );
    }


    // =========================================================
    // CHECK IN - ACTIVE VISIT EXISTS
    // =========================================================

    @Test
    void checkIn_ShouldReject_WhenSameDayExaminationAlreadyRegistered() {

        UUID appointmentId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        Profile customer =
                customer(profileId);

        MedicalService service =
                service(
                        UUID.randomUUID(),
                        "Kham"
                );

        Appointment appointment =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer
                );

        appointment.setServices(
                new HashSet<>(
                        List.of(service)
                )
        );

        AppointmentCheckInRequest req =
                mock(
                        AppointmentCheckInRequest.class
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
                repo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.of(appointment)
        );

        when(staffRepo.findById(staffId))
                .thenReturn(
                        Optional.of(
                                mock(StaffInfo.class)
                        )
                );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenReturn(
                Optional.of(customer)
        );

        doThrow(new ConflictException("Dịch vụ đã được đăng ký hôm nay"))
                .when(customerVisitService).validateNoSameDayExaminationRegistration(
                        profileId, List.of(service.getServiceId()));

        assertThrows(
                ConflictException.class,
                () ->
                        appointmentService.checkIn(req)
        );

        verify(
                invoiceService,
                never()
        ).create(any());
    }


    // =========================================================
    // CHECK IN SUCCESS - REGISTERED CUSTOMER
    // =========================================================

    @Test
    void checkIn_ShouldCreateVisitAndInvoice_ForRegisteredCustomer() {

        UUID appointmentId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID invoiceId =
                UUID.randomUUID();

        Profile customer =
                customer(profileId);

        StaffInfo staff =
                mock(
                        StaffInfo.class
                );

        MedicalService service =
                service(
                        serviceId,
                        "Kham tong quat"
                );

        Appointment appointment =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer
                );

        appointment.setServices(
                new HashSet<>(
                        List.of(service)
                )
        );

        AppointmentCheckInRequest req =
                mock(
                        AppointmentCheckInRequest.class
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
                repo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.of(appointment)
        );

        when(staffRepo.findById(staffId))
                .thenReturn(
                        Optional.of(staff)
                );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenReturn(
                Optional.of(customer)
        );



        when(
                visitRepo
                        .findByAppointment_AppointmentId(
                                appointmentId
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                visitRepo.save(
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

        InvoiceResponse invoiceResponse =
                mock(
                        InvoiceResponse.class
                );

        when(invoiceResponse.invoiceId())
                .thenReturn(invoiceId);

        when(invoiceService.create(any()))
                .thenReturn(
                        invoiceResponse
                );

        when(repo.save(appointment))
                .thenReturn(
                        appointment
                );

        var result =
                appointmentService.checkIn(req);

        assertNotNull(result);

        assertEquals(
                AppointmentStatus.CHECKED_IN,
                appointment.getStatus()
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
    // GUEST HISTORY
    // =========================================================

    @Test
    void getGuestHistoryByPhone_ShouldReturnEmpty_WhenPhoneNull() {

        assertTrue(
                appointmentService
                        .getGuestHistoryByPhone(null)
                        .isEmpty()
        );

        verifyNoInteractions(repo);
    }


    @Test
    void getGuestHistoryByPhone_ShouldReturnEmpty_WhenPhoneBlank() {

        assertTrue(
                appointmentService
                        .getGuestHistoryByPhone(" ")
                        .isEmpty()
        );

        verifyNoInteractions(repo);
    }


    // =========================================================
    // GUEST CHECK-IN - ACTIVE VISIT
    // =========================================================

    @Test
    void guestCheckIn_ShouldReject_WhenSameDayExaminationAlreadyRegistered() {

        UUID profileId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        GuestCheckInRequest req =
                mock(
                        GuestCheckInRequest.class
                );

        MedicalService service =
                service(
                        serviceId,
                        "Kham tong quat"
                );

        when(req.issuedById())
                .thenReturn(
                        staffId
                );

        when(req.serviceIds())
                .thenReturn(
                        Set.of(serviceId)
                );

        when(req.guestPhone())
                .thenReturn(
                        "0900000000"
                );

        when(req.guestFullName())
                .thenReturn(
                        "Guest A"
                );

        when(req.guestGender())
                .thenReturn(
                        Gender.MALE
                );

        when(req.guestAge())
                .thenReturn(25);

        when(serviceRepo.findById(serviceId))
                .thenReturn(
                        Optional.of(service)
                );

        Profile guest =
                customer(profileId);

        when(
                profileRepo.findFirstByPhone(
                        "0900000000"
                )
        ).thenReturn(
                Optional.of(guest)
        );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenReturn(
                Optional.of(guest)
        );

        doThrow(new ConflictException("Dịch vụ đã được đăng ký hôm nay"))
                .when(customerVisitService).validateNoSameDayExaminationRegistration(
                        profileId, List.of(service.getServiceId()));

        assertThrows(
                ConflictException.class,
                () ->
                        appointmentService
                                .guestCheckIn(req)
        );

        verify(
                visitRepo,
                never()
        ).save(any(CustomerVisit.class));

        verify(
                invoiceService,
                never()
        ).create(any());
    }


    // =========================================================
    // GUEST CHECK-IN SUCCESS
    // =========================================================

    @Test
    void guestCheckIn_ShouldCreateVisitAndInvoice() {

        UUID profileId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID invoiceId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        GuestCheckInRequest req =
                mock(
                        GuestCheckInRequest.class
                );

        MedicalService service =
                service(
                        serviceId,
                        "Kham tong quat"
                );

        when(req.guestPhone())
                .thenReturn(
                        "0900000000"
                );

        when(req.guestFullName())
                .thenReturn(
                        "Guest"
                );

        when(req.guestGender())
                .thenReturn(
                        Gender.MALE
                );

        when(req.guestAge())
                .thenReturn(25);

        when(req.issuedById())
                .thenReturn(
                        staffId
                );

        when(req.serviceIds())
                .thenReturn(
                        Set.of(serviceId)
                );

        when(serviceRepo.findById(serviceId))
                .thenReturn(
                        Optional.of(service)
                );

        Profile guest =
                customer(profileId);

        when(
                profileRepo.findFirstByPhone(
                        "0900000000"
                )
        ).thenReturn(
                Optional.of(guest)
        );

        when(
                profileRepo.findByIdForUpdate(
                        profileId
                )
        ).thenReturn(
                Optional.of(guest)
        );



        when(staffRepo.findById(staffId))
                .thenReturn(
                        Optional.of(
                                mock(StaffInfo.class)
                        )
                );

        when(
                visitRepo.save(
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

        InvoiceResponse invoiceResponse =
                mock(
                        InvoiceResponse.class
                );

        when(invoiceResponse.invoiceId())
                .thenReturn(
                        invoiceId
                );

        when(invoiceService.create(any()))
                .thenReturn(
                        invoiceResponse
                );

        var result =
                appointmentService
                        .guestCheckIn(req);

        assertNotNull(result);

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
    // GET MY APPOINTMENTS
    // =========================================================

    @Test
    void getMyAppointments_ShouldThrow_WhenCustomerMissing() {

        UUID customerId =
                UUID.randomUUID();

        when(familyAccessService.ownerProfile(customerId))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy hồ sơ chủ tài khoản"));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        appointmentService
                                .getMyAppointments(
                                        customerId,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        PageRequest.of(
                                                0,
                                                10
                                        )
                                )
        );
    }


    // =========================================================
    // GET MY DETAIL - NOT OWNER
    // =========================================================

    @Test
    void getMyAppointmentDetail_ShouldReject_WhenNotOwner() {

        UUID accountId =
                UUID.randomUUID();

        UUID appointmentId =
                UUID.randomUUID();

        Profile customer =
                customer(
                        UUID.randomUUID()
                );

        Profile other =
                customer(
                        UUID.randomUUID()
                );

        Appointment appointment =
                appointment(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        other
                );

        when(familyAccessService.resolveReadableProfile(accountId, other.getProfileId()))
                .thenThrow(new BadRequestException("Bạn không có quyền truy cập hồ sơ bệnh nhân này"));

        when(repo.findById(appointmentId))
                .thenReturn(
                        Optional.of(appointment)
                );

        assertThrows(
                BadRequestException.class,
                () ->
                        appointmentService
                                .getMyAppointmentDetail(
                                        accountId,
                                        appointmentId
                                )
        );
    }


    // =========================================================
    // UPDATE MY APPOINTMENT - NOT OWNER
    // =========================================================

    @Test
    void updateMyAppointment_ShouldReject_WhenNotOwner() {

        UUID accountId =
                UUID.randomUUID();

        UUID appointmentId =
                UUID.randomUUID();

        Profile customer =
                customer(
                        UUID.randomUUID()
                );

        Appointment appointment =
                appointment(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        when(familyAccessService.resolveActiveProfile(accountId, appointment.getCustomer().getProfileId()))
                .thenThrow(new BadRequestException("Bạn không có quyền quản lý hồ sơ bệnh nhân này"));

        when(
                repo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.of(appointment)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        appointmentService
                                .updateMyAppointment(
                                        accountId,
                                        appointmentId,
                                        mock(
                                                AppointmentUpdateRequest.class
                                        )
                                )
        );
    }


    // =========================================================
    // UPDATE MY APPOINTMENT - NOT PENDING
    // =========================================================

    @Test
    void updateMyAppointment_ShouldReject_WhenNotPending() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        UUID appointmentId =
                UUID.randomUUID();

        Profile customer =
                customer(profileId);

        Appointment appointment =
                appointment(
                        appointmentId,
                        AppointmentStatus.CHECKED_IN,
                        customer
                );

        when(familyAccessService.resolveActiveProfile(accountId, profileId)).thenReturn(customer);

        when(
                repo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.of(appointment)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        appointmentService
                                .updateMyAppointment(
                                        accountId,
                                        appointmentId,
                                        mock(
                                                AppointmentUpdateRequest.class
                                        )
                                )
        );
    }


    // =========================================================
    // CANCEL MY APPOINTMENT
    // =========================================================

    @Test
    void cancelMyAppointment_ShouldCancelPendingAppointment() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        UUID appointmentId =
                UUID.randomUUID();

        Profile customer =
                customer(profileId);

        Appointment appointment =
                appointment(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer
                );

        when(familyAccessService.resolveActiveProfile(accountId, profileId)).thenReturn(customer);

        when(
                repo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.of(appointment)
        );

        when(repo.save(appointment))
                .thenReturn(
                        appointment
                );

        appointmentService
                .cancelMyAppointment(
                        accountId,
                        appointmentId
                );

        assertEquals(
                AppointmentStatus.CANCELLED,
                appointment.getStatus()
        );

        verify(repo)
                .save(appointment);
    }


    @Test
    void cancelMyAppointment_ShouldReject_WhenNotPending() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        UUID appointmentId =
                UUID.randomUUID();

        Profile customer =
                customer(profileId);

        Appointment appointment =
                appointment(
                        appointmentId,
                        AppointmentStatus.CHECKED_IN,
                        customer
                );

        when(familyAccessService.resolveActiveProfile(accountId, profileId)).thenReturn(customer);

        when(
                repo.findByIdForUpdate(
                        appointmentId
                )
        ).thenReturn(
                Optional.of(appointment)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        appointmentService
                                .cancelMyAppointment(
                                        accountId,
                                        appointmentId
                                )
        );
    }

    @Test
    void contactAndPhoneHelpers_ShouldNormalizeAllSupportedForms() {
        assertEquals(Set.of(), ReflectionTestUtils.invokeMethod(appointmentService, "contactValues", (Object) null));
        assertEquals(Set.of(), ReflectionTestUtils.invokeMethod(appointmentService, "contactValues", "  "));
        assertEquals(Set.of("a@b.vn"), ReflectionTestUtils.invokeMethod(appointmentService, "contactValues", " a@b.vn "));

        assertEquals(Set.of(), ReflectionTestUtils.invokeMethod(appointmentService, "phoneVariants", (Object) null));
        assertEquals(Set.of(), ReflectionTestUtils.invokeMethod(appointmentService, "phoneVariants", "   "));
        assertEquals(Set.of("+84 912-345-678", "0912345678", "84912345678", "+84912345678"),
                ReflectionTestUtils.invokeMethod(appointmentService, "phoneVariants", "+84 912-345-678"));
        assertEquals(Set.of("0912345678", "84912345678", "+84912345678"),
                ReflectionTestUtils.invokeMethod(appointmentService, "phoneVariants", "0912345678"));
        assertEquals(Set.of("123"), ReflectionTestUtils.invokeMethod(appointmentService, "phoneVariants", "123"));
        assertEquals(Set.of("+"), ReflectionTestUtils.invokeMethod(appointmentService, "phoneVariants", "+"));

        assertNull(ReflectionTestUtils.invokeMethod(appointmentService, "normalizeOptional", (Object) null));
        assertNull(ReflectionTestUtils.invokeMethod(appointmentService, "normalizeOptional", "   "));
        assertEquals("Hà Nội", ReflectionTestUtils.invokeMethod(appointmentService, "normalizeOptional", " Hà Nội "));
    }

    @Test
    void appointmentOwnershipAndAwaitingHelpers_ShouldCoverBothSides() {
        Appointment appointment = Appointment.builder().build();
        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(appointmentService,
                "appointmentHasNoRegisteredCustomer", appointment)));
        appointment.setCustomer(Profile.builder().profileId(UUID.randomUUID()).build());
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(appointmentService,
                "appointmentHasNoRegisteredCustomer", appointment)));
        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(appointmentService,
                "isAwaitingCheckIn", AppointmentStatus.PENDING)));
        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(appointmentService,
                "isAwaitingCheckIn", AppointmentStatus.RESCHEDULED)));
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(appointmentService,
                "isAwaitingCheckIn", AppointmentStatus.CANCELLED)));
        assertEquals(List.of(AppointmentStatus.PENDING, AppointmentStatus.RESCHEDULED),
                ReflectionTestUtils.invokeMethod(appointmentService, "activeAppointmentStatuses"));
    }

    @Test
    void resolveAppointmentAge_ShouldPreferRequestedBirthThenCustomerBirthThenExplicitOrGuestAge() {
        LocalDate scheduled = LocalDate.now(CLINIC_ZONE).plusDays(10);
        Appointment appointment = Appointment.builder().scheduledAt(scheduled.atTime(8, 0)).guestAge(42).build();
        assertEquals(Integer.valueOf(20), ReflectionTestUtils.invokeMethod(appointmentService, "resolveAppointmentAge",
                appointment, scheduled.minusYears(20), 33));
        appointment.setCustomer(Profile.builder().dateOfBirth(scheduled.minusYears(30)).build());
        assertEquals(Integer.valueOf(30), ReflectionTestUtils.invokeMethod(appointmentService, "resolveAppointmentAge",
                appointment, null, 33));
        appointment.getCustomer().setDateOfBirth(null);
        assertEquals(Integer.valueOf(33), ReflectionTestUtils.invokeMethod(appointmentService, "resolveAppointmentAge",
                appointment, null, 33));
        assertEquals(Integer.valueOf(42), ReflectionTestUtils.invokeMethod(appointmentService, "resolveAppointmentAge",
                appointment, null, null));
    }

    @Test
    void validateStaffStatusUpdate_ShouldCoverNoopAndEveryRejectedOrAllowedTransition() {
        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateStaffStatusUpdate", AppointmentStatus.PENDING, req));
        when(req.status()).thenReturn(AppointmentStatus.PENDING);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateStaffStatusUpdate", AppointmentStatus.PENDING, req));

        when(req.status()).thenReturn(AppointmentStatus.CHECKED_IN);
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateStaffStatusUpdate", AppointmentStatus.PENDING, req));
        when(req.status()).thenReturn(AppointmentStatus.PENDING);
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateStaffStatusUpdate", AppointmentStatus.CANCELLED, req));
        when(req.status()).thenReturn(AppointmentStatus.RESCHEDULED);
        when(req.scheduledAt()).thenReturn(null);
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateStaffStatusUpdate", AppointmentStatus.PENDING, req));
        when(req.scheduledAt()).thenReturn(LocalDateTime.now().plusDays(1));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateStaffStatusUpdate", AppointmentStatus.PENDING, req));

        when(req.status()).thenReturn(AppointmentStatus.CANCELLED);
        when(req.cancelReason()).thenReturn(null);
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateStaffStatusUpdate", AppointmentStatus.PENDING, req));
        when(req.cancelReason()).thenReturn("  ");
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateStaffStatusUpdate", AppointmentStatus.PENDING, req));
        when(req.cancelReason()).thenReturn("Bệnh nhân yêu cầu");
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateStaffStatusUpdate", AppointmentStatus.PENDING, req));
    }
}
