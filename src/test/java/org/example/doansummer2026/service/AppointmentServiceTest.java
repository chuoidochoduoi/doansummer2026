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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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

    @InjectMocks
    private AppointmentService appointmentService;


    // =========================================================
    // HELPERS
    // =========================================================

    private Profile customer(UUID profileId) {
        return Profile.builder()
                .profileId(profileId)
                .fullName("Nguyen Van A")
                .phone("0901234567")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.now().minusYears(25))
                .build();
    }

    private MedicalService service(UUID id, String name) {
        return MedicalService.builder()
                .serviceId(id)
                .name(name)
                .serviceCode("DV01")
                .price(new BigDecimal("100000"))
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
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .status(status)
                .services(new HashSet<>())
                .build();
    }

    /**
     * Appointment riêng cho các test check-in.
     *
     * AppointmentService.checkIn() hiện chỉ cho phép check-in
     * lịch hẹn đúng ngày hôm nay.
     *
     * Không sửa helper appointment() phía trên vì các test create/update
     * vẫn cần lịch hẹn ở tương lai.
     */
    private Appointment appointmentForToday(
            UUID id,
            AppointmentStatus status,
            Profile customer
    ) {
        return Appointment.builder()
                .appointmentId(id)
                .customer(customer)
                .scheduledAt(LocalDate.now().atTime(9, 0))
                .status(status)
                .services(new HashSet<>())
                .build();
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenExists() {

        UUID id = UUID.randomUUID();

        Appointment a =
                appointment(
                        id,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(a));

        assertSame(
                a,
                appointmentService.findById(id)
        );
    }

    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.findById(id)
        );
    }


    // =========================================================
    // GET
    // =========================================================

    @Test
    void get_ShouldReturnResponse_WhenFound() {

        UUID id = UUID.randomUUID();

        Appointment a =
                appointment(
                        id,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(a));

        assertNotNull(
                appointmentService.get(id)
        );
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void search_ShouldDelegateToRepository() {

        UUID customerId = UUID.randomUUID();

        var pageable =
                PageRequest.of(0, 10);

        when(
                repo.search(
                        customerId,
                        AppointmentStatus.PENDING.name(),
                        null,
                        null,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of())
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

        UUID accountId = UUID.randomUUID();

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId())
                .thenReturn(accountId);

        when(accountRepo.findById(accountId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - ROLE NOT ALLOWED
    // =========================================================

    @Test
    void create_ShouldReject_WhenAccountRoleIsNull() {

        UUID accountId = UUID.randomUUID();

        Account account = Account.builder()
                .accountId(accountId)
                .role(null)
                .build();

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId())
                .thenReturn(accountId);

        when(accountRepo.findById(accountId))
                .thenReturn(Optional.of(account));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - PROFILE MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenProfileMissing() {

        UUID accountId = UUID.randomUUID();

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
                .thenReturn(Optional.of(account));

        when(
                profileRepo
                        .findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - CONFLICT
    // =========================================================

    @Test
    void create_ShouldReject_WhenAppointmentConflictExists() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        LocalDateTime scheduledAt =
                LocalDateTime.now().plusDays(1);

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
                .thenReturn(scheduledAt);

        when(accountRepo.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepo
                        .findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(
                repo.existsCustomerConflict(
                        eq(profileId),
                        eq(List.of(AppointmentStatus.PENDING)),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                )
        ).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - SHIFT MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenShiftMissing() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId()).thenReturn(accountId);
        when(req.scheduledAt()).thenReturn(
                LocalDateTime.now().plusDays(1)
        );
        when(req.shiftId()).thenReturn(shiftId);

        when(accountRepo.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepo.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(shiftConfigRepository.findById(shiftId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - SERVICE MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenServiceMissing() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId()).thenReturn(accountId);
        when(req.scheduledAt()).thenReturn(
                LocalDateTime.now().plusDays(1)
        );
        when(req.serviceIds()).thenReturn(
                Set.of(serviceId)
        );

        when(accountRepo.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepo.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - AGE REQUIRED
    // =========================================================

    @Test
    void create_ShouldRejectService_WhenBirthDateMissingAndAgeRestricted() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        customer.setDateOfBirth(null);

        MedicalService service =
                service(serviceId, "Kham nhi");

        service.setMinimumAge(5);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId()).thenReturn(accountId);
        when(req.scheduledAt()).thenReturn(
                LocalDateTime.now().plusDays(1)
        );
        when(req.serviceIds()).thenReturn(
                Set.of(serviceId)
        );

        when(accountRepo.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepo.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - TOO YOUNG
    // =========================================================

    @Test
    void create_ShouldRejectService_WhenCustomerTooYoung() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        customer.setDateOfBirth(
                LocalDate.now().minusYears(10)
        );

        MedicalService service =
                service(serviceId, "Kham nguoi lon");

        service.setMinimumAge(18);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId()).thenReturn(accountId);
        when(req.scheduledAt()).thenReturn(
                LocalDateTime.now().plusDays(1)
        );
        when(req.serviceIds()).thenReturn(
                Set.of(serviceId)
        );

        when(accountRepo.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepo.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - TOO OLD
    // =========================================================

    @Test
    void create_ShouldRejectService_WhenCustomerTooOld() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        customer.setDateOfBirth(
                LocalDate.now().minusYears(70)
        );

        MedicalService service =
                service(serviceId, "Dich vu gioi han tuoi");

        service.setMaximumAge(60);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId()).thenReturn(accountId);
        when(req.scheduledAt()).thenReturn(
                LocalDateTime.now().plusDays(1)
        );
        when(req.serviceIds()).thenReturn(
                Set.of(serviceId)
        );

        when(accountRepo.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepo.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - GENDER MISSING
    // =========================================================

    @Test
    void create_ShouldRejectService_WhenGenderMissing() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        customer.setGender(null);

        MedicalService service =
                service(serviceId, "Dich vu nu");

        service.setAllowedGender(Gender.FEMALE);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId()).thenReturn(accountId);
        when(req.scheduledAt()).thenReturn(
                LocalDateTime.now().plusDays(1)
        );
        when(req.serviceIds()).thenReturn(
                Set.of(serviceId)
        );

        when(accountRepo.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepo.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE - GENDER WRONG
    // =========================================================

    @Test
    void create_ShouldRejectService_WhenGenderNotAllowed() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        customer.setGender(Gender.MALE);

        MedicalService service =
                service(serviceId, "Dich vu nu");

        service.setAllowedGender(Gender.FEMALE);

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        when(req.customerId()).thenReturn(accountId);
        when(req.scheduledAt()).thenReturn(
                LocalDateTime.now().plusDays(1)
        );
        when(req.serviceIds()).thenReturn(
                Set.of(serviceId)
        );

        when(accountRepo.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepo.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.create(req)
        );
    }


    // =========================================================
    // CREATE SUCCESS
    // =========================================================

    @Test
    void create_ShouldCreatePendingAppointmentSuccessfully() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        Account account =
                Account.builder()
                        .accountId(accountId)
                        .role(Role.CUSTOMER)
                        .build();

        Profile customer =
                customer(profileId);

        MedicalService service =
                service(serviceId, "Kham tong quat");

        AppointmentCreateRequest req =
                mock(AppointmentCreateRequest.class);

        LocalDateTime schedule =
                LocalDateTime.now().plusDays(1);

        when(req.customerId()).thenReturn(accountId);
        when(req.scheduledAt()).thenReturn(schedule);
        when(req.serviceIds()).thenReturn(
                Set.of(serviceId)
        );

        when(accountRepo.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepo.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(
                repo.existsCustomerConflict(
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(repo.save(any(Appointment.class)))
                .thenAnswer(i -> {
                    Appointment a = i.getArgument(0);
                    a.setAppointmentId(UUID.randomUUID());
                    return a;
                });

        var result =
                appointmentService.create(req);

        assertNotNull(result);

        verify(repo).save(argThat(a ->
                a.getStatus() == AppointmentStatus.PENDING
                        && a.getCustomer() == customer
                        && a.getServices().contains(service)
        ));

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
                mock(AppointmentGuestCreateRequest.class);

        when(req.guestGender())
                .thenReturn(Gender.OTHER);

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.createForGuest(req)
        );
    }


    // =========================================================
    // CREATE GUEST - CONFLICT
    // =========================================================

    @Test
    void createForGuest_ShouldRejectConflict() {

        AppointmentGuestCreateRequest req =
                mock(AppointmentGuestCreateRequest.class);

        LocalDateTime scheduledAt =
                LocalDateTime.now().plusDays(1);

        when(req.guestGender())
                .thenReturn(Gender.MALE);

        when(req.scheduledAt())
                .thenReturn(scheduledAt);

        when(req.guestPhone())
                .thenReturn("0901234567");

        when(repo.existsGuestConflict(
                eq("0901234567"),
                isNull(),
                anyList(),
                any(),
                any()
        )).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.createForGuest(req)
        );
    }


    // =========================================================
    // CREATE GUEST - NO PHONE/EMAIL
    // =========================================================

    @Test
    void createForGuest_ShouldSkipConflictCheck_WhenPhoneAndEmailBlank() {

        AppointmentGuestCreateRequest req =
                mock(AppointmentGuestCreateRequest.class);

        when(req.guestGender())
                .thenReturn(Gender.MALE);

        when(req.scheduledAt())
                .thenReturn(
                        LocalDateTime.now().plusDays(1)
                );

        when(req.guestPhone())
                .thenReturn(" ");

        when(req.guestEmail())
                .thenReturn(null);

        when(repo.save(any(Appointment.class)))
                .thenAnswer(i -> {
                    Appointment a = i.getArgument(0);
                    a.setAppointmentId(UUID.randomUUID());
                    return a;
                });

        var result =
                appointmentService.createForGuest(req);

        assertNotNull(result);

        verify(repo, never())
                .existsGuestConflict(
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

        UUID serviceId = UUID.randomUUID();

        AppointmentGuestCreateRequest req =
                mock(AppointmentGuestCreateRequest.class);

        MedicalService service =
                service(serviceId, "Kham guest");

        when(req.guestGender())
                .thenReturn(Gender.FEMALE);

        when(req.guestAge())
                .thenReturn(30);

        when(req.guestFullName())
                .thenReturn("Guest A");

        when(req.guestPhone())
                .thenReturn("0900000000");

        when(req.scheduledAt())
                .thenReturn(
                        LocalDateTime.now().plusDays(1)
                );

        when(req.serviceIds())
                .thenReturn(Set.of(serviceId));

        when(repo.existsGuestConflict(
                any(),
                any(),
                anyList(),
                any(),
                any()
        )).thenReturn(false);

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        when(repo.save(any(Appointment.class)))
                .thenAnswer(i -> {
                    Appointment a = i.getArgument(0);
                    a.setAppointmentId(UUID.randomUUID());
                    return a;
                });

        var result =
                appointmentService.createForGuest(req);

        assertNotNull(result);

        verify(repo).save(argThat(a ->
                Boolean.TRUE.equals(a.getIsGuest())
                        && "Guest A".equals(a.getGuestFullName())
                        && a.getServices().contains(service)
        ));
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void update_ShouldUpdateFields() {

        UUID appointmentId = UUID.randomUUID();

        Appointment a =
                appointment(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        AppointmentUpdateRequest req =
                mock(AppointmentUpdateRequest.class);

        LocalDateTime newTime =
                LocalDateTime.now().plusDays(3);

        when(req.scheduledAt()).thenReturn(newTime);
        when(req.status()).thenReturn(
                AppointmentStatus.RESCHEDULED
        );
        when(req.cancelReason()).thenReturn("Reason");
        when(req.guestFullName()).thenReturn("Updated");
        when(req.guestPhone()).thenReturn("0999999999");

        when(repo.findById(appointmentId))
                .thenReturn(Optional.of(a));

        when(
                repo.existsOtherCustomerConflict(
                        any(),
                        any(),
                        anyList(),
                        any(),
                        any()
                )
        ).thenReturn(false);

        when(repo.save(a))
                .thenReturn(a);

        appointmentService.update(
                appointmentId,
                req
        );

        assertEquals(
                newTime,
                a.getScheduledAt()
        );

        assertEquals(
                AppointmentStatus.RESCHEDULED,
                a.getStatus()
        );

        assertEquals(
                "Updated",
                a.getGuestFullName()
        );
    }


    // =========================================================
    // UPDATE - RESCHEDULE CONFLICT
    // =========================================================

    @Test
    void update_ShouldRejectRescheduleConflict() {

        UUID appointmentId = UUID.randomUUID();

        Appointment a =
                appointment(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        AppointmentUpdateRequest req =
                mock(AppointmentUpdateRequest.class);

        when(req.scheduledAt())
                .thenReturn(
                        LocalDateTime.now().plusDays(2)
                );

        when(repo.findById(appointmentId))
                .thenReturn(Optional.of(a));

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
                () -> appointmentService.update(
                        appointmentId,
                        req
                )
        );
    }


    // =========================================================
    // UPDATE - GUEST SKIPS RESCHEDULE CHECK
    // =========================================================

    @Test
    void update_ShouldSkipConflictCheck_WhenAppointmentHasNoCustomer() {

        UUID appointmentId = UUID.randomUUID();

        Appointment a =
                Appointment.builder()
                        .appointmentId(appointmentId)
                        .status(AppointmentStatus.PENDING)
                        .scheduledAt(LocalDateTime.now())
                        .build();

        AppointmentUpdateRequest req =
                mock(AppointmentUpdateRequest.class);

        when(req.scheduledAt())
                .thenReturn(
                        LocalDateTime.now().plusDays(2)
                );

        when(repo.findById(appointmentId))
                .thenReturn(Optional.of(a));

        when(repo.save(a))
                .thenReturn(a);

        appointmentService.update(
                appointmentId,
                req
        );

        verify(repo, never())
                .existsOtherCustomerConflict(
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

        UUID appointmentId = UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        Appointment a =
                appointment(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer
                );

        AppointmentUpdateRequest req =
                mock(AppointmentUpdateRequest.class);

        when(req.status())
                .thenReturn(
                        AppointmentStatus.CANCELLED
                );

        when(repo.findById(appointmentId))
                .thenReturn(Optional.of(a));

        when(repo.save(a))
                .thenReturn(a);

        appointmentService.update(
                appointmentId,
                req
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
    void delete_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.delete(id)
        );
    }

    @Test
    void delete_ShouldDelete_WhenExists() {

        UUID id = UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(true);

        appointmentService.delete(id);

        verify(repo).deleteById(id);
    }


    // =========================================================
    // CHECK IN - APPOINTMENT MISSING
    // =========================================================

    @Test
    void checkIn_ShouldThrow_WhenAppointmentMissing() {

        UUID appointmentId = UUID.randomUUID();

        AppointmentCheckInRequest req =
                mock(AppointmentCheckInRequest.class);

        when(req.appointmentId())
                .thenReturn(appointmentId);

        when(repo.findByIdForUpdate(appointmentId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.checkIn(req)
        );
    }


    // =========================================================
    // CHECK IN - ALREADY CHECKED IN
    // =========================================================

    @Test
    void checkIn_ShouldReject_WhenAlreadyCheckedIn() {

        UUID appointmentId = UUID.randomUUID();

        AppointmentCheckInRequest req =
                mock(AppointmentCheckInRequest.class);

        Appointment a =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.CHECKED_IN,
                        customer(UUID.randomUUID())
                );

        when(req.appointmentId())
                .thenReturn(appointmentId);

        when(repo.findByIdForUpdate(appointmentId))
                .thenReturn(Optional.of(a));

        assertThrows(
                ConflictException.class,
                () -> appointmentService.checkIn(req)
        );
    }


    // =========================================================
    // CHECK IN - INVALID STATUS
    // =========================================================

    @Test
    void checkIn_ShouldReject_WhenStatusNotPending() {

        UUID appointmentId = UUID.randomUUID();

        AppointmentCheckInRequest req =
                mock(AppointmentCheckInRequest.class);

        Appointment a =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.CANCELLED,
                        customer(UUID.randomUUID())
                );

        when(req.appointmentId())
                .thenReturn(appointmentId);

        when(repo.findByIdForUpdate(appointmentId))
                .thenReturn(Optional.of(a));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.checkIn(req)
        );
    }


    // =========================================================
    // CHECK IN - STAFF ID MISSING
    // =========================================================

    @Test
    void checkIn_ShouldReject_WhenIssuedByMissing() {

        UUID appointmentId = UUID.randomUUID();

        AppointmentCheckInRequest req =
                mock(AppointmentCheckInRequest.class);

        Appointment a =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        /*
         * Service must not be empty here.
         * Otherwise checkIn() stops at "no services" before it checks issuedById.
         */
        a.setServices(
                new HashSet<>(List.of(
                        service(UUID.randomUUID(), "Kham tong quat")
                ))
        );

        when(req.appointmentId())
                .thenReturn(appointmentId);

        when(repo.findByIdForUpdate(appointmentId))
                .thenReturn(Optional.of(a));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.checkIn(req)
        );
    }


    // =========================================================
    // CHECK IN - STAFF NOT FOUND
    // =========================================================

    @Test
    void checkIn_ShouldThrow_WhenStaffMissing() {

        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        AppointmentCheckInRequest req =
                mock(AppointmentCheckInRequest.class);

        Appointment a =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        /*
         * Add a service so the test reaches staffRepo.findById().
         */
        a.setServices(
                new HashSet<>(List.of(
                        service(UUID.randomUUID(), "Kham tong quat")
                ))
        );

        when(req.appointmentId())
                .thenReturn(appointmentId);

        when(req.issuedById())
                .thenReturn(staffId);

        when(repo.findByIdForUpdate(appointmentId))
                .thenReturn(Optional.of(a));

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.checkIn(req)
        );
    }


    // =========================================================
    // CHECK IN - NO SERVICES
    // =========================================================

    @Test
    void checkIn_ShouldReject_WhenAppointmentHasNoServices() {

        UUID appointmentId = UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        Appointment a =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer
                );

        a.setServices(new HashSet<>());

        AppointmentCheckInRequest req =
                mock(AppointmentCheckInRequest.class);

        when(req.appointmentId())
                .thenReturn(appointmentId);

        when(repo.findByIdForUpdate(appointmentId))
                .thenReturn(Optional.of(a));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.checkIn(req)
        );

        /*
         * No issuedById/staff stubbing here.
         * The method rejects the empty service list before staff lookup.
         */
        verifyNoInteractions(staffRepo);
    }


    // =========================================================
    // CHECK IN - ACTIVE VISIT EXISTS
    // =========================================================

    @Test
    void checkIn_ShouldReject_WhenCustomerHasActiveVisit() {

        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID activeVisitId = UUID.randomUUID();

        Profile customer =
                customer(profileId);

        MedicalService service =
                service(UUID.randomUUID(), "Kham");

        Appointment a =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer
                );

        a.setServices(
                new HashSet<>(List.of(service))
        );

        AppointmentCheckInRequest req =
                mock(AppointmentCheckInRequest.class);

        when(req.appointmentId())
                .thenReturn(appointmentId);

        when(req.issuedById())
                .thenReturn(staffId);

        when(repo.findByIdForUpdate(appointmentId))
                .thenReturn(Optional.of(a));

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(mock(StaffInfo.class)));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(customer));

        CustomerVisit activeVisit =
                CustomerVisit.builder()
                        .visitId(activeVisitId)
                        .status(VisitStatus.IN_PROGRESS)
                        .build();

        when(
                visitRepo
                        .findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                                eq(profileId),
                                anyList()
                        )
        ).thenReturn(Optional.of(activeVisit));

        assertThrows(
                ConflictException.class,
                () -> appointmentService.checkIn(req)
        );
    }


    // =========================================================
    // CHECK IN SUCCESS - REGISTERED CUSTOMER
    // =========================================================

    @Test
    void checkIn_ShouldCreateVisitAndInvoice_ForRegisteredCustomer() {

        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Profile customer =
                customer(profileId);

        StaffInfo staff =
                mock(StaffInfo.class);

        MedicalService service =
                service(
                        serviceId,
                        "Kham tong quat"
                );

        Appointment a =
                appointmentForToday(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer
                );

        a.setServices(
                new HashSet<>(List.of(service))
        );

        AppointmentCheckInRequest req =
                mock(AppointmentCheckInRequest.class);

        when(req.appointmentId())
                .thenReturn(appointmentId);

        when(req.issuedById())
                .thenReturn(staffId);

        when(repo.findByIdForUpdate(appointmentId))
                .thenReturn(Optional.of(a));

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(customer));

        when(
                visitRepo
                        .findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                                eq(profileId),
                                anyList()
                        )
        ).thenReturn(Optional.empty());

        when(
                visitRepo.findByAppointment_AppointmentId(
                        appointmentId
                )
        ).thenReturn(Optional.empty());

        when(visitRepo.save(any(CustomerVisit.class)))
                .thenAnswer(i -> {
                    CustomerVisit visit =
                            i.getArgument(0);

                    visit.setVisitId(visitId);

                    return visit;
                });

        InvoiceResponse invoiceResponse =
                mock(InvoiceResponse.class);

        when(invoiceResponse.invoiceId())
                .thenReturn(invoiceId);

        when(invoiceService.create(any()))
                .thenReturn(invoiceResponse);

        when(repo.save(a))
                .thenReturn(a);

        var result =
                appointmentService.checkIn(req);

        assertNotNull(result);

        assertEquals(
                AppointmentStatus.CHECKED_IN,
                a.getStatus()
        );

        verify(invoiceService)
                .create(argThat(invoice ->
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
                                invoice.items() != null
                                &&
                                invoice.items().size() == 1
                ));
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
    }


    // =========================================================
    // GUEST CHECK-IN - ACTIVE VISIT
    // =========================================================

    @Test
    void guestCheckIn_ShouldReject_WhenActiveVisitExists() {

        UUID profileId = UUID.randomUUID();

        GuestCheckInRequest req =
                mock(GuestCheckInRequest.class);

        when(req.guestPhone())
                .thenReturn("0900000000");

        Profile guest =
                customer(profileId);

        when(
                profileRepo.findFirstByPhone(
                        "0900000000"
                )
        ).thenReturn(Optional.of(guest));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(guest));

        CustomerVisit active =
                CustomerVisit.builder()
                        .visitId(UUID.randomUUID())
                        .status(VisitStatus.CHECKED_IN)
                        .build();

        when(
                visitRepo
                        .findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                                eq(profileId),
                                anyList()
                        )
        ).thenReturn(Optional.of(active));

        assertThrows(
                ConflictException.class,
                () -> appointmentService.guestCheckIn(req)
        );
    }


    // =========================================================
    // GUEST CHECK-IN SUCCESS
    // =========================================================

    @Test
    void guestCheckIn_ShouldCreateVisitAndInvoice() {

        UUID profileId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        GuestCheckInRequest req =
                mock(GuestCheckInRequest.class);

        when(req.guestPhone())
                .thenReturn("0900000000");

        when(req.guestFullName())
                .thenReturn("Guest");

        when(req.issuedById())
                .thenReturn(staffId);

        Profile guest =
                customer(profileId);

        when(
                profileRepo.findFirstByPhone(
                        "0900000000"
                )
        ).thenReturn(Optional.of(guest));

        when(profileRepo.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(guest));

        when(
                visitRepo
                        .findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                                eq(profileId),
                                anyList()
                        )
        ).thenReturn(Optional.empty());

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(mock(StaffInfo.class)));

        when(visitRepo.save(any(CustomerVisit.class)))
                .thenAnswer(i -> {
                    CustomerVisit v =
                            i.getArgument(0);

                    v.setVisitId(visitId);

                    return v;
                });

        InvoiceResponse invoiceResponse =
                mock(InvoiceResponse.class);

        when(invoiceResponse.invoiceId())
                .thenReturn(invoiceId);

        when(invoiceService.create(any()))
                .thenReturn(invoiceResponse);

        var result =
                appointmentService.guestCheckIn(req);

        assertNotNull(result);

        verify(invoiceService)
                .create(argThat(invoice ->
                        profileId.equals(invoice.customerId())
                                &&
                                visitId.equals(invoice.visitId())
                                &&
                                staffId.equals(invoice.issuedById())
                ));
    }


    // =========================================================
    // GET MY APPOINTMENTS
    // =========================================================

    @Test
    void getMyAppointments_ShouldThrow_WhenCustomerMissing() {

        UUID customerId = UUID.randomUUID();

        when(
                profileRepo.findFirstByAccount_AccountId(
                        customerId
                )
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.getMyAppointments(
                        customerId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 10)
                )
        );
    }


    // =========================================================
    // GET MY DETAIL - NOT OWNER
    // =========================================================

    @Test
    void getMyAppointmentDetail_ShouldReject_WhenNotOwner() {

        UUID accountId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        Profile other =
                customer(UUID.randomUUID());

        Appointment a =
                appointment(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        other
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(repo.findById(appointmentId))
                .thenReturn(Optional.of(a));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService
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

        UUID accountId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        Appointment a =
                appointment(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer(UUID.randomUUID())
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(repo.findByIdForUpdate(appointmentId))
                .thenReturn(Optional.of(a));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.updateMyAppointment(
                        accountId,
                        appointmentId,
                        mock(AppointmentUpdateRequest.class)
                )
        );
    }


    // =========================================================
    // UPDATE MY APPOINTMENT - NOT PENDING
    // =========================================================

    @Test
    void updateMyAppointment_ShouldReject_WhenNotPending() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        Profile customer =
                customer(profileId);

        Appointment a =
                appointment(
                        appointmentId,
                        AppointmentStatus.CHECKED_IN,
                        customer
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(repo.findByIdForUpdate(appointmentId))
                .thenReturn(Optional.of(a));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.updateMyAppointment(
                        accountId,
                        appointmentId,
                        mock(AppointmentUpdateRequest.class)
                )
        );
    }


    // =========================================================
    // CANCEL MY APPOINTMENT
    // =========================================================

    @Test
    void cancelMyAppointment_ShouldCancelPendingAppointment() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        Profile customer =
                customer(profileId);

        Appointment a =
                appointment(
                        appointmentId,
                        AppointmentStatus.PENDING,
                        customer
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(repo.findByIdForUpdate(appointmentId))
                .thenReturn(Optional.of(a));

        when(repo.save(a))
                .thenReturn(a);

        appointmentService.cancelMyAppointment(
                accountId,
                appointmentId
        );

        assertEquals(
                AppointmentStatus.CANCELLED,
                a.getStatus()
        );

        verify(repo).save(a);
    }


    @Test
    void cancelMyAppointment_ShouldReject_WhenNotPending() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();

        Profile customer =
                customer(profileId);

        Appointment a =
                appointment(
                        appointmentId,
                        AppointmentStatus.CHECKED_IN,
                        customer
                );

        when(
                profileRepo
                        .findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(customer));

        when(repo.findByIdForUpdate(appointmentId))
                .thenReturn(Optional.of(a));

        assertThrows(
                BadRequestException.class,
                () -> appointmentService.cancelMyAppointment(
                        accountId,
                        appointmentId
                )
        );
    }
}