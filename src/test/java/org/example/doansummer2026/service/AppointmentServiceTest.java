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
import java.time.LocalTime;
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
    private InvoiceRepository invoiceRepository;

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

    @Test
    void resolveCurrentShiftSkipsClosedIncompleteAndOutOfTimeCandidates() {
        ShiftScheduleResolver resolver = mock(ShiftScheduleResolver.class);
        ReflectionTestUtils.setField(appointmentService, "shiftScheduleResolver", resolver);
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        ShiftConfig closed = ShiftConfig.builder().shiftId(UUID.randomUUID()).build();
        ShiftConfig missingStart = ShiftConfig.builder().shiftId(UUID.randomUUID()).build();
        ShiftConfig missingEnd = ShiftConfig.builder().shiftId(UUID.randomUUID()).build();
        ShiftConfig alreadyEnded = ShiftConfig.builder().shiftId(UUID.randomUUID()).build();
        ShiftConfig active = ShiftConfig.builder().shiftId(UUID.randomUUID()).build();
        when(shiftConfigRepository.findAllByIsActiveTrueOrderByStartTimeAsc())
                .thenReturn(List.of(closed, missingStart, missingEnd, alreadyEnded, active));
        when(resolver.resolve(closed, today)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                closed, null, null, null, ShiftTimeSource.NORMAL, ShiftUnavailableReason.SHIFT_OFF));
        when(resolver.resolve(missingStart, today)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                missingStart, null, null, LocalTime.MAX, ShiftTimeSource.NORMAL, null));
        when(resolver.resolve(missingEnd, today)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                missingEnd, null, LocalTime.MIN, null, ShiftTimeSource.NORMAL, null));
        when(resolver.resolve(alreadyEnded, today)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                alreadyEnded, null, LocalTime.MIN, LocalTime.MIN, ShiftTimeSource.NORMAL, null));
        when(resolver.resolve(active, today)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                active, null, LocalTime.MIN, LocalTime.MAX, ShiftTimeSource.NORMAL, null));

        ShiftConfig result = ReflectionTestUtils.invokeMethod(appointmentService, "resolveCurrentShift");

        assertSame(active, result);
    }

    @Test
    void resolveCurrentShiftReturnsNullWhenNoCandidateMatches() {
        ShiftScheduleResolver resolver = mock(ShiftScheduleResolver.class);
        ReflectionTestUtils.setField(appointmentService, "shiftScheduleResolver", resolver);
        when(shiftConfigRepository.findAllByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of());
        assertNull(ReflectionTestUtils.invokeMethod(appointmentService, "resolveCurrentShift"));
    }

    @Test
    void validateServiceEligibilityCoversAgeGenderAndStatusBoundaries() {
        MedicalService service = MedicalService.builder().name("Khám chuyên khoa")
                .status(ServiceStatus.ACTIVE).minimumAge(18).maximumAge(60).allowedGender(Gender.FEMALE).build();
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "validateServiceEligibility", service, null, Gender.FEMALE));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "validateServiceEligibility", service, 17, Gender.FEMALE));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "validateServiceEligibility", service, 61, Gender.FEMALE));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "validateServiceEligibility", service, 30, null));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "validateServiceEligibility", service, 30, Gender.MALE));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                appointmentService, "validateServiceEligibility", service, 18, Gender.FEMALE));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                appointmentService, "validateServiceEligibility", service, 60, Gender.FEMALE));

        service.setStatus(ServiceStatus.INACTIVE);
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "validateServiceEligibility", service, 30, Gender.FEMALE));
        service.setStatus(ServiceStatus.ACTIVE);
        service.setMinimumAge(null);
        service.setMaximumAge(null);
        service.setAllowedGender(null);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                appointmentService, "validateServiceEligibility", service, null, null));
    }

    @Test
    void customerStatusNotificationCoversGuestMissingCustomerAllStatusesAndFailureIsolation() {
        Profile customer = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn An").build();
        Appointment appointment = Appointment.builder().appointmentId(UUID.randomUUID())
                .scheduledAt(LocalDateTime.now(CLINIC_ZONE).plusDays(1)).isGuest(true).build();
        ReflectionTestUtils.invokeMethod(appointmentService, "notifyCustomerStatusChange",
                appointment, AppointmentStatus.PENDING);
        verifyNoInteractions(notificationService);

        appointment.setIsGuest(false);
        appointment.setCustomer(null);
        ReflectionTestUtils.invokeMethod(appointmentService, "notifyCustomerStatusChange",
                appointment, AppointmentStatus.PENDING);
        appointment.setCustomer(customer);
        appointment.setStatus(AppointmentStatus.PENDING);
        ReflectionTestUtils.invokeMethod(appointmentService, "notifyCustomerStatusChange",
                appointment, AppointmentStatus.PENDING);
        verifyNoInteractions(notificationService);

        when(familyAccessService.notificationRecipientProfileId(customer)).thenReturn(customer.getProfileId());
        for (AppointmentStatus status : List.of(AppointmentStatus.CHECKED_IN,
                AppointmentStatus.CANCELLED, AppointmentStatus.RESCHEDULED)) {
            appointment.setStatus(status);
            ReflectionTestUtils.invokeMethod(appointmentService, "notifyCustomerStatusChange",
                    appointment, AppointmentStatus.PENDING);
        }
        verify(notificationService, times(3)).create(any());

        doThrow(new RuntimeException("notification unavailable")).when(notificationService).create(any());
        appointment.setStatus(AppointmentStatus.CANCELLED);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(appointmentService,
                "notifyCustomerStatusChange", appointment, AppointmentStatus.PENDING));
    }

    @Test
    void appointmentShiftResolutionUsesVersionLegacyNameAndMissingFallbacks() {
        ShiftConfig versionShift = ShiftConfig.builder().shiftId(UUID.randomUUID()).name("Sáng").build();
        Appointment appointment = Appointment.builder().shiftVersion(ShiftVersion.builder()
                .shift(versionShift).build()).shiftName("Tên cũ").build();
        assertSame(versionShift, ReflectionTestUtils.invokeMethod(
                appointmentService, "shiftFromAppointment", appointment));
        assertSame(versionShift, ReflectionTestUtils.invokeMethod(
                appointmentService, "resolveAppointmentShift", appointment));

        appointment.setShiftVersion(null);
        ShiftConfig legacyShift = ShiftConfig.builder().shiftId(UUID.randomUUID()).name("Tên cũ").build();
        ShiftConfig other = ShiftConfig.builder().shiftId(UUID.randomUUID()).name("Chiều").build();
        when(shiftConfigRepository.findAll()).thenReturn(List.of(other, legacyShift));
        assertSame(legacyShift, ReflectionTestUtils.invokeMethod(
                appointmentService, "shiftFromAppointment", appointment));
        assertSame(legacyShift, ReflectionTestUtils.invokeMethod(
                appointmentService, "resolveAppointmentShift", appointment));

        appointment.setShiftName(null);
        assertNull(ReflectionTestUtils.invokeMethod(appointmentService, "shiftFromAppointment", appointment));
        assertNull(ReflectionTestUtils.invokeMethod(appointmentService, "resolveAppointmentShift", appointment));
        appointment.setShiftName("Không tồn tại");
        assertNull(ReflectionTestUtils.invokeMethod(appointmentService, "shiftFromAppointment", appointment));
        assertNull(ReflectionTestUtils.invokeMethod(appointmentService, "resolveAppointmentShift", appointment));
    }

    @Test
    void rescheduleConflictSkipsGuestAndRejectsOnlyConflictingCustomer() {
        LocalDateTime target = LocalDateTime.now(CLINIC_ZONE).plusDays(2);
        Appointment appointment = Appointment.builder().appointmentId(UUID.randomUUID()).customer(null).build();
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                appointmentService, "validateRescheduleConflict", appointment, target));
        verifyNoInteractions(repo);

        Profile customer = Profile.builder().profileId(UUID.randomUUID()).build();
        appointment.setCustomer(customer);
        when(repo.existsOtherCustomerConflict(eq(customer.getProfileId()), eq(appointment.getAppointmentId()),
                anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(false, true);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                appointmentService, "validateRescheduleConflict", appointment, target));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "validateRescheduleConflict", appointment, target));
    }

    @Test
    void resolveBookingShiftCoversDateShiftAndAvailabilityBranches() {
        ShiftScheduleResolver resolver = mock(ShiftScheduleResolver.class);
        ServiceAvailabilityService availabilityService = mock(ServiceAvailabilityService.class);
        ReflectionTestUtils.setField(appointmentService, "shiftScheduleResolver", resolver);
        ReflectionTestUtils.setField(appointmentService, "serviceAvailabilityService", availabilityService);
        LocalDate today = LocalDate.now(CLINIC_ZONE);

        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "resolveBookingShift", null, null, Set.of()));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "resolveBookingShift", null, today.atTime(9, 0), Set.of()));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "resolveBookingShift", null, today.plusMonths(12).plusDays(1).atTime(9, 0), Set.of()));

        LocalDateTime tomorrow = today.plusDays(1).atTime(9, 0);
        assertNull(ReflectionTestUtils.invokeMethod(
                appointmentService, "resolveBookingShift", null, tomorrow, null));

        ShiftConfig custom = ShiftConfig.builder().name("Ca tự chọn").build();
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "resolveBookingShift", custom, tomorrow, Set.of()));

        ShiftConfig morning = ShiftConfig.builder().shiftId(UUID.randomUUID())
                .name(ShiftConfigService.MORNING).build();
        ShiftScheduleResolver.ResolvedShift unavailable = new ShiftScheduleResolver.ResolvedShift(
                morning, null, null, null, ShiftTimeSource.NORMAL, ShiftUnavailableReason.CLINIC_CLOSED);
        when(resolver.resolve(morning, tomorrow.toLocalDate())).thenReturn(unavailable);
        assertThrows(ConflictException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "resolveBookingShift", morning, tomorrow, Set.of()));

        ShiftScheduleResolver.ResolvedShift resolved = new ShiftScheduleResolver.ResolvedShift(
                morning, null, LocalTime.of(7, 30), LocalTime.of(11, 30), ShiftTimeSource.NORMAL, null);
        when(resolver.resolve(morning, tomorrow.toLocalDate())).thenReturn(resolved);
        MedicalService service = MedicalService.builder().name("Khám Nội").build();
        when(availabilityService.evaluate(service, tomorrow.toLocalDate(), morning, true))
                .thenReturn(new ServiceAvailabilityService.Evaluation(
                        false, ShiftUnavailableReason.NO_QUALIFIED_STAFF, List.of()));
        assertThrows(ConflictException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "resolveBookingShift", morning, tomorrow, Set.of(service)));

        when(availabilityService.evaluate(service, tomorrow.toLocalDate(), morning, true))
                .thenReturn(new ServiceAvailabilityService.Evaluation(true, null, List.of()));
        assertSame(resolved, ReflectionTestUtils.invokeMethod(
                appointmentService, "resolveBookingShift", morning, tomorrow, Set.of(service)));
        assertSame(resolved, ReflectionTestUtils.invokeMethod(
                appointmentService, "resolveBookingShift", morning, tomorrow, null));
    }

    @Test
    void createMyGroupRejectsDuplicatePatientBeforeCreatingAnyAppointment() {
        UUID accountId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        GroupAppointmentCreateRequest request = new GroupAppointmentCreateRequest(
                futureAppointmentTime(), UUID.randomUUID(), List.of(
                new GroupAppointmentCreateRequest.MemberBooking(patientId, List.of(serviceId)),
                new GroupAppointmentCreateRequest.MemberBooking(patientId, List.of(serviceId))));
        when(familyAccessService.resolveActiveProfile(accountId, patientId))
                .thenReturn(customer(patientId));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> appointmentService.createMyGroup(accountId, request));

        assertEquals("Không được chọn trùng người trong cùng một lịch nhóm", error.getMessage());
        verify(familyAccessService, times(1)).resolveActiveProfile(accountId, patientId);
        verify(repo, never()).save(any());
    }

    @Test
    void createMyGroupCreatesOneAppointmentPerAuthorizedFamilyMember() {
        ShiftScheduleResolver resolver = mock(ShiftScheduleResolver.class);
        ServiceAvailabilityService availabilityService = mock(ServiceAvailabilityService.class);
        ReflectionTestUtils.setField(appointmentService, "shiftScheduleResolver", resolver);
        ReflectionTestUtils.setField(appointmentService, "serviceAvailabilityService", availabilityService);
        UUID accountId = UUID.randomUUID();
        UUID firstPatientId = UUID.randomUUID();
        UUID secondPatientId = UUID.randomUUID();
        UUID firstServiceId = UUID.randomUUID();
        UUID secondServiceId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        LocalDateTime requestedAt = futureAppointmentTime();
        Profile firstPatient = customer(firstPatientId);
        Profile secondPatient = customer(secondPatientId);
        MedicalService firstService = service(firstServiceId, "Khám Nội");
        MedicalService secondService = service(secondServiceId, "Khám Ngoại");
        ShiftConfig shift = ShiftConfig.builder().shiftId(shiftId)
                .name(ShiftConfigService.MORNING).build();
        ShiftScheduleResolver.ResolvedShift resolved = new ShiftScheduleResolver.ResolvedShift(
                shift, null, LocalTime.of(7, 30), LocalTime.of(11, 30),
                ShiftTimeSource.NORMAL, null);
        GroupAppointmentCreateRequest request = new GroupAppointmentCreateRequest(
                requestedAt, shiftId, List.of(
                new GroupAppointmentCreateRequest.MemberBooking(firstPatientId, List.of(firstServiceId)),
                new GroupAppointmentCreateRequest.MemberBooking(secondPatientId, List.of(secondServiceId))));
        when(familyAccessService.resolveActiveProfile(accountId, firstPatientId)).thenReturn(firstPatient);
        when(familyAccessService.resolveActiveProfile(accountId, secondPatientId)).thenReturn(secondPatient);
        when(serviceRepo.findById(firstServiceId)).thenReturn(Optional.of(firstService));
        when(serviceRepo.findById(secondServiceId)).thenReturn(Optional.of(secondService));
        when(shiftConfigRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(resolver.resolve(shift, requestedAt.toLocalDate())).thenReturn(resolved);
        when(availabilityService.evaluate(any(), eq(requestedAt.toLocalDate()), eq(shift), eq(true)))
                .thenReturn(new ServiceAvailabilityService.Evaluation(true, null, List.of()));
        when(repo.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setAppointmentId(UUID.randomUUID());
            return saved;
        });

        List<AppointmentResponse> result = appointmentService.createMyGroup(accountId, request);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(item -> item.scheduledAt().toLocalTime().equals(LocalTime.of(7, 30))));
        verify(repo, times(2)).save(any(Appointment.class));
        verify(notificationService, times(2)).notifyStaffByRole(
                eq(SystemRole.RECEPTIONIST), eq("Lịch hẹn mới"), anyString(),
                eq("Appointment"), any(UUID.class));
    }

    @Test
    void validateExaminationAvailabilityForShiftCoversStaffingAndServiceTypeRules() {
        ServiceAvailabilityService availabilityService = mock(ServiceAvailabilityService.class);
        ReflectionTestUtils.setField(appointmentService, "serviceAvailabilityService", availabilityService);
        LocalDate date = clinicToday().plusDays(1);
        ShiftConfig shift = ShiftConfig.builder().shiftId(UUID.randomUUID())
                .name(ShiftConfigService.MORNING).build();
        MedicalService laboratory = service(UUID.randomUUID(), "Đường huyết");
        laboratory.setDepartmentType(DepartmentType.LABORATORY);
        MedicalService examination = service(UUID.randomUUID(), "Khám Nội");
        examination.setDepartmentType(DepartmentType.EXAMINATION);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateExaminationAvailabilityForShift", null, null, date));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateExaminationAvailabilityForShift", List.of(laboratory), null, date));
        assertThrows(ConflictException.class, () -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateExaminationAvailabilityForShift", List.of(examination), null, date));

        when(availabilityService.evaluate(examination, date, shift, false))
                .thenReturn(new ServiceAvailabilityService.Evaluation(
                        false, ShiftUnavailableReason.NO_QUALIFIED_STAFF, List.of()));
        ConflictException unavailable = assertThrows(ConflictException.class,
                () -> ReflectionTestUtils.invokeMethod(appointmentService,
                        "validateExaminationAvailabilityForShift", List.of(examination), shift, date));
        assertTrue(unavailable.getMessage().contains("NO_QUALIFIED_STAFF"));

        when(availabilityService.evaluate(examination, date, shift, false))
                .thenReturn(new ServiceAvailabilityService.Evaluation(false, null, List.of()));
        ConflictException unexplained = assertThrows(ConflictException.class,
                () -> ReflectionTestUtils.invokeMethod(appointmentService,
                        "validateExaminationAvailabilityForShift", List.of(examination), shift, date));
        assertFalse(unexplained.getMessage().contains("("));

        when(availabilityService.evaluate(examination, date, shift, false))
                .thenReturn(new ServiceAvailabilityService.Evaluation(true, null, List.of()));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(appointmentService,
                "validateExaminationAvailabilityForShift",
                List.of(laboratory, examination), shift, date));
    }


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
        when(profileRepo.saveAndFlush(any(Profile.class))).thenAnswer(invocation -> {
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
        verify(profileRepo).saveAndFlush(any(Profile.class));
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
    void guestCheckIn_CreatesNewProfileAndSavesAndFlushes_WhenNotFound() {
        UUID staffId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        GuestCheckInRequest req = mock(GuestCheckInRequest.class);
        MedicalService service = service(serviceId, "Kham tong quat");

        when(req.guestPhone()).thenReturn("0900000000");
        when(req.guestFullName()).thenReturn("New Guest");
        when(req.guestGender()).thenReturn(Gender.MALE);
        when(req.guestAge()).thenReturn(30);
        when(req.issuedById()).thenReturn(staffId);
        when(req.serviceIds()).thenReturn(Set.of(serviceId));
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(service));

        when(profileRepo.findFirstByPhone("0900000000")).thenReturn(Optional.empty());

        UUID newProfileId = UUID.randomUUID();
        when(profileRepo.saveAndFlush(any(Profile.class))).thenAnswer(invocation -> {
            Profile created = invocation.getArgument(0);
            created.setProfileId(newProfileId);
            return created;
        });

        Profile lockedGuest = customer(newProfileId);
        when(profileRepo.findByIdForUpdate(newProfileId)).thenReturn(Optional.of(lockedGuest));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));

        when(visitRepo.save(any(CustomerVisit.class))).thenAnswer(i -> {
            CustomerVisit v = i.getArgument(0);
            v.setVisitId(UUID.randomUUID());
            return v;
        });

        when(invoiceService.create(any())).thenReturn(
                mock(InvoiceResponse.class)
        );

        assertNotNull(appointmentService.guestCheckIn(req));
        verify(profileRepo).saveAndFlush(any(Profile.class));
    }

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

    @Test
    void getMyAppointments_ShouldMapSelectedFamilyProfileAndRelationship() {
        UUID accountId = UUID.randomUUID();
        Profile owner = customer(UUID.randomUUID());
        Profile child = customer(UUID.randomUUID());
        Appointment appointment = appointment(UUID.randomUUID(), AppointmentStatus.PENDING, child);
        appointment.setServices(Set.of(service(UUID.randomUUID(), "Khám Nhi")));
        var pageable = PageRequest.of(0, 10);
        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        when(familyAccessService.resolveReadableProfile(accountId, child.getProfileId())).thenReturn(child);
        when(familyAccessService.relationship(accountId, child.getProfileId())).thenReturn(FamilyRelationship.CHILD);
        when(repo.searchForCustomers(eq(List.of(child.getProfileId())), isNull(), isNull(), isNull(),
                isNull(), isNull(), eq(pageable))).thenReturn(new PageImpl<>(List.of(appointment), pageable, 1));

        var result = appointmentService.getMyAppointments(accountId, child.getProfileId(), false,
                null, null, null, null, null, pageable);

        assertEquals(1, result.content().size());
        assertEquals("Con", result.content().get(0).relationship());
        assertFalse(result.content().get(0).isSelf());
    }

    @Test
    void getMyAppointments_ShouldMapReadableProfilesIncludingGuestRow() {
        UUID accountId = UUID.randomUUID();
        Profile owner = customer(UUID.randomUUID());
        Appointment guest = appointment(UUID.randomUUID(), AppointmentStatus.PENDING, null);
        guest.setGuestFullName("Khách vãng lai");
        var pageable = PageRequest.of(0, 10);
        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        when(familyAccessService.readableProfiles(accountId, true)).thenReturn(List.of(owner));
        when(repo.searchForCustomers(eq(List.of(owner.getProfileId())), isNull(), isNull(), isNull(),
                isNull(), isNull(), eq(pageable))).thenReturn(new PageImpl<>(List.of(guest), pageable, 1));

        var result = appointmentService.getMyAppointments(accountId, null, true,
                null, null, null, null, null, pageable);

        assertEquals("Khách vãng lai", result.content().get(0).patientName());
        assertNull(result.content().get(0).relationship());
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

    @Test
    void getMyAppointmentDetail_ShouldRejectGuestAppointment() {
        UUID appointmentId = UUID.randomUUID();
        when(repo.findById(appointmentId)).thenReturn(Optional.of(
                appointment(appointmentId, AppointmentStatus.PENDING, null)));

        assertThrows(BadRequestException.class,
                () -> appointmentService.getMyAppointmentDetail(UUID.randomUUID(), appointmentId));
    }

    @Test
    void getMyAppointmentDetail_ShouldReturnOwnedAppointment() {
        UUID accountId = UUID.randomUUID();
        Profile owner = customer(UUID.randomUUID());
        Appointment appointment = appointment(UUID.randomUUID(), AppointmentStatus.PENDING, owner);
        when(repo.findById(appointment.getAppointmentId())).thenReturn(Optional.of(appointment));
        when(familyAccessService.resolveReadableProfile(accountId, owner.getProfileId())).thenReturn(owner);
        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        when(familyAccessService.relationship(accountId, owner.getProfileId())).thenReturn(null);

        var result = appointmentService.getMyAppointmentDetail(accountId, appointment.getAppointmentId());

        assertTrue(result.isSelf());
        assertEquals(owner.getProfileId(), result.patientProfileId());
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

    @Test
    void updateMyAppointment_ShouldSaveUnchangedPendingAppointment() {
        UUID accountId = UUID.randomUUID();
        Profile owner = customer(UUID.randomUUID());
        Appointment appointment = appointment(UUID.randomUUID(), AppointmentStatus.PENDING, owner);
        AppointmentUpdateRequest request = mock(AppointmentUpdateRequest.class);
        when(repo.findByIdForUpdate(appointment.getAppointmentId())).thenReturn(Optional.of(appointment));
        when(familyAccessService.resolveActiveProfile(accountId, owner.getProfileId())).thenReturn(owner);
        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        when(familyAccessService.relationship(accountId, owner.getProfileId())).thenReturn(FamilyRelationship.OTHER);
        when(repo.save(appointment)).thenReturn(appointment);

        var result = appointmentService.updateMyAppointment(accountId, appointment.getAppointmentId(), request);

        assertEquals("Khác", result.relationship());
        verify(repo).save(appointment);
    }

    @Test
    void updateProfileInformationNormalizesFieldsAndCoversDuplicateOwnershipRules() {
        UUID profileId = UUID.randomUUID();
        Profile profile = Profile.builder().profileId(profileId).fullName("Tên cũ")
                .phone("0900000000").email("old@example.com").build();
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "updateProfileInformation", profile, "Nguyen 123", null,
                null, null, null, null));

        Profile other = Profile.builder().profileId(UUID.randomUUID()).build();
        when(profileRepo.findFirstByPhone("0912345678")).thenReturn(Optional.of(other));
        assertThrows(ConflictException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "updateProfileInformation", profile, null, " 0912345678 ",
                null, null, null, null));

        when(profileRepo.findFirstByPhone("0912345678"))
                .thenReturn(Optional.of(Profile.builder().profileId(profileId).build()));
        when(profileRepo.findFirstByEmail("new@example.com")).thenReturn(Optional.of(other));
        assertThrows(ConflictException.class, () -> ReflectionTestUtils.invokeMethod(
                appointmentService, "updateProfileInformation", profile, null, " 0912345678 ",
                " NEW@EXAMPLE.COM ", null, null, null));

        when(profileRepo.findFirstByEmail("new@example.com"))
                .thenReturn(Optional.of(Profile.builder().profileId(profileId).build()));
        LocalDate dob = LocalDate.of(1990, 1, 2);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                appointmentService, "updateProfileInformation", profile, " Nguyễn Văn An ", " 0912345678 ",
                " NEW@EXAMPLE.COM ", "  Hà Nội  ", dob, Gender.MALE));
        assertEquals("Nguyễn Văn An", profile.getFullName());
        assertEquals("0912345678", profile.getPhone());
        assertEquals("new@example.com", profile.getEmail());
        assertEquals("Hà Nội", profile.getAddress());
        assertEquals(dob, profile.getDateOfBirth());
        assertEquals(Gender.MALE, profile.getGender());
        verify(profileRepo).save(profile);
    }

    @Test
    void resolveExistingPatientProfileUsesCustomerThenPhoneThenEmail() {
        Profile customer = customer(UUID.randomUUID());
        Appointment appointment = Appointment.builder().customer(customer).build();
        assertSame(customer, ReflectionTestUtils.invokeMethod(appointmentService,
                "resolveExistingPatientProfile", appointment, "0900", "a@b.vn"));

        appointment.setCustomer(null);
        Profile byPhone = customer(UUID.randomUUID());
        when(profileRepo.findFirstByPhoneIn(anySet())).thenReturn(Optional.of(byPhone), Optional.empty());
        assertSame(byPhone, ReflectionTestUtils.invokeMethod(appointmentService,
                "resolveExistingPatientProfile", appointment, "0900000000", null));

        Profile byEmail = customer(UUID.randomUUID());
        when(profileRepo.findFirstByEmailIgnoreCase("a@b.vn")).thenReturn(Optional.of(byEmail));
        assertSame(byEmail, ReflectionTestUtils.invokeMethod(appointmentService,
                "resolveExistingPatientProfile", appointment, "0900000000", "a@b.vn"));
        assertNull(ReflectionTestUtils.invokeMethod(appointmentService,
                "resolveExistingPatientProfile", appointment, null, null));
    }

    @Test
    void linkGuestHistoryMovesAppointmentVisitAndInvoicesToRegisteredProfile() {
        Profile profile = Profile.builder().profileId(UUID.randomUUID())
                .account(Account.builder().accountId(UUID.randomUUID()).build()).build();
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(appointmentService,
                "linkGuestHistoryBeforeContactChange", profile, null, " "));
        verify(repo, never()).findGuestAppointmentsByPhonesOrEmails(anySet(), anySet());

        Appointment guest = Appointment.builder().appointmentId(UUID.randomUUID()).isGuest(true).build();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        Invoice invoice = Invoice.builder().invoiceId(UUID.randomUUID()).build();
        when(repo.findGuestAppointmentsByPhonesOrEmails(anySet(), anySet())).thenReturn(List.of(guest));
        when(visitRepo.findByAppointment_AppointmentId(guest.getAppointmentId())).thenReturn(Optional.of(visit));
        when(invoiceRepository.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(invoice));

        ReflectionTestUtils.invokeMethod(appointmentService, "linkGuestHistoryBeforeContactChange",
                profile, "0900000000", "OLD@EXAMPLE.COM");
        assertSame(profile, guest.getCustomer());
        assertFalse(guest.getIsGuest());
        assertSame(profile, visit.getCustomer());
        assertSame(profile, invoice.getCustomer());
        verify(invoiceRepository).saveAll(List.of(invoice));
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

    // =========================================================
    // EXTRA TESTS FOR BRANCH COVERAGE
    // =========================================================

    @Test
    void update_WithServiceIds_GuestAndCustomerAgeGenderBranches() {
        UUID appId = UUID.randomUUID();
        Appointment a = appointment(appId, AppointmentStatus.PENDING, null);
        a.setIsGuest(true);
        a.setGuestAge(30);
        a.setGuestGender(Gender.MALE);
        
        MedicalService service1 = service(UUID.randomUUID(), "Test Service 1");
        service1.setMinimumAge(18);
        service1.setAllowedGender(Gender.MALE);
        
        when(repo.findById(appId)).thenReturn(Optional.of(a));
        when(serviceRepo.findById(service1.getServiceId())).thenReturn(Optional.of(service1));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        
        AppointmentUpdateRequest req1 = new AppointmentUpdateRequest(null, null, null, Set.of(service1.getServiceId()), null, null, null, null, null, null, 30, Gender.MALE);
        assertDoesNotThrow(() -> appointmentService.update(appId, req1));
        
        // Null guest gender fallback to appointment guest gender
        AppointmentUpdateRequest req2 = new AppointmentUpdateRequest(null, null, null, Set.of(service1.getServiceId()), null, null, null, null, null, null, null, null);
        assertDoesNotThrow(() -> appointmentService.update(appId, req2));
        
        // Use customer instead of guest
        Profile customer = customer(UUID.randomUUID());
        customer.setGender(Gender.MALE);
        customer.setDateOfBirth(LocalDate.now().minusYears(25));
        a.setIsGuest(false);
        a.setCustomer(customer);
        
        AppointmentUpdateRequest req3 = new AppointmentUpdateRequest(null, null, null, Set.of(service1.getServiceId()), null, null, null, null, null, null, null, null);
        assertDoesNotThrow(() -> appointmentService.update(appId, req3));
    }

    // =========================================================
    // BRANCH COVERAGE: create() with STAFF role
    // =========================================================

    @Test
    void create_ShouldAllowStaffRole() {
        ShiftScheduleResolver resolver = mock(ShiftScheduleResolver.class);
        ServiceAvailabilityService availabilityService = mock(ServiceAvailabilityService.class);
        ReflectionTestUtils.setField(appointmentService, "shiftScheduleResolver", resolver);
        ReflectionTestUtils.setField(appointmentService, "serviceAvailabilityService", availabilityService);

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        Account account = Account.builder().accountId(accountId).role(Role.STAFF).build();
        Profile customer = customer(profileId);
        MedicalService svc = service(serviceId, "Khám tổng quát");

        AppointmentCreateRequest req = mock(AppointmentCreateRequest.class);
        when(req.customerId()).thenReturn(accountId);
        when(req.scheduledAt()).thenReturn(futureAppointmentTime());
        when(req.serviceIds()).thenReturn(Set.of(serviceId));
        when(req.shiftId()).thenReturn(null);

        when(accountRepo.findById(accountId)).thenReturn(Optional.of(account));
        when(profileRepo.findFirstByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));
        when(repo.existsCustomerConflict(any(), anyList(), any(), any())).thenReturn(false);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));
        when(repo.save(any(Appointment.class))).thenAnswer(i -> {
            Appointment saved = i.getArgument(0);
            saved.setAppointmentId(UUID.randomUUID());
            return saved;
        });

        var result = appointmentService.create(req);
        assertNotNull(result);
    }

    // =========================================================
    // BRANCH COVERAGE: create() with null serviceIds
    // =========================================================

    @Test
    void create_ShouldPassNullServiceIdsToCreateForPatient() {
        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Account account = Account.builder().accountId(accountId).role(Role.CUSTOMER).build();
        Profile customer = customer(profileId);

        AppointmentCreateRequest req = mock(AppointmentCreateRequest.class);
        when(req.customerId()).thenReturn(accountId);
        when(req.scheduledAt()).thenReturn(futureAppointmentTime());
        when(req.serviceIds()).thenReturn(null);

        when(accountRepo.findById(accountId)).thenReturn(Optional.of(account));
        when(profileRepo.findFirstByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));

        // serviceIds=null triggers validateDistinctServices which throws
        assertThrows(BadRequestException.class, () -> appointmentService.create(req));
    }

    // =========================================================
    // BRANCH COVERAGE: createForPatient() - onlineBooking restriction
    // =========================================================

    @Test
    void createMy_ShouldRejectServiceNotAllowedForOnlineBooking() {
        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        Profile patient = customer(profileId);
        MedicalService svc = service(serviceId, "Xét nghiệm lẻ");
        svc.setAllowCustomerBooking(false);

        when(familyAccessService.resolveActiveProfile(accountId, profileId)).thenReturn(patient);
        when(repo.existsCustomerConflict(any(), anyList(), any(), any())).thenReturn(false);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));

        CustomerAppointmentCreateRequest req = new CustomerAppointmentCreateRequest(
                profileId, futureAppointmentTime(), null, List.of(serviceId));

        assertThrows(BadRequestException.class, () -> appointmentService.createMy(accountId, req));
    }

    @Test
    void createMy_ShouldAllowServiceWithCustomerBookingEnabled() {
        ShiftScheduleResolver resolver = mock(ShiftScheduleResolver.class);
        ServiceAvailabilityService availabilityService = mock(ServiceAvailabilityService.class);
        ReflectionTestUtils.setField(appointmentService, "shiftScheduleResolver", resolver);
        ReflectionTestUtils.setField(appointmentService, "serviceAvailabilityService", availabilityService);

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        Profile patient = customer(profileId);
        MedicalService svc = service(serviceId, "Khám online");
        svc.setAllowCustomerBooking(true);

        when(familyAccessService.resolveActiveProfile(accountId, profileId)).thenReturn(patient);
        when(repo.existsCustomerConflict(any(), anyList(), any(), any())).thenReturn(false);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));
        when(repo.save(any(Appointment.class))).thenAnswer(i -> {
            Appointment saved = i.getArgument(0);
            saved.setAppointmentId(UUID.randomUUID());
            return saved;
        });

        CustomerAppointmentCreateRequest req = new CustomerAppointmentCreateRequest(
                profileId, futureAppointmentTime(), null, List.of(serviceId));

        assertNotNull(appointmentService.createMy(accountId, req));
    }

    // =========================================================
    // BRANCH COVERAGE: createForPatient() - conflict after shift normalization
    // =========================================================

    @Test
    void createForPatient_ShouldRejectConflictAfterShiftResolution() {
        ShiftScheduleResolver resolver = mock(ShiftScheduleResolver.class);
        ServiceAvailabilityService availabilityService = mock(ServiceAvailabilityService.class);
        ReflectionTestUtils.setField(appointmentService, "shiftScheduleResolver", resolver);
        ReflectionTestUtils.setField(appointmentService, "serviceAvailabilityService", availabilityService);

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        Profile patient = customer(profileId);
        MedicalService svc = service(serviceId, "Khám tổng quát");
        svc.setAllowCustomerBooking(true);
        ShiftConfig shift = ShiftConfig.builder().shiftId(shiftId)
                .name(ShiftConfigService.MORNING).build();
        LocalDateTime requestedAt = futureAppointmentTime();
        // Shift resolves to 7:30 - different from requested 10:00
        ShiftScheduleResolver.ResolvedShift resolved = new ShiftScheduleResolver.ResolvedShift(
                shift, null, LocalTime.of(7, 30), LocalTime.of(11, 30),
                ShiftTimeSource.NORMAL, null);

        when(familyAccessService.resolveActiveProfile(accountId, profileId)).thenReturn(patient);
        // First call (original time) - no conflict; second call (normalized time) - conflict
        when(repo.existsCustomerConflict(eq(profileId), anyList(), any(), any()))
                .thenReturn(false, true);
        when(shiftConfigRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));
        when(resolver.resolve(shift, requestedAt.toLocalDate())).thenReturn(resolved);
        when(availabilityService.evaluate(any(), any(), any(), eq(true)))
                .thenReturn(new ServiceAvailabilityService.Evaluation(true, null, List.of()));

        CustomerAppointmentCreateRequest req = new CustomerAppointmentCreateRequest(
                profileId, requestedAt, shiftId, List.of(serviceId));

        assertThrows(BadRequestException.class, () -> appointmentService.createMy(accountId, req));
    }

    // =========================================================
    // BRANCH COVERAGE: checkIn() - scheduledAt is null
    // =========================================================

    @Test
    void checkIn_ShouldReject_WhenScheduledAtIsNull() {
        UUID appointmentId = UUID.randomUUID();
        Appointment a = Appointment.builder().appointmentId(appointmentId)
                .status(AppointmentStatus.PENDING).scheduledAt(null).build();

        AppointmentCheckInRequest req = mock(AppointmentCheckInRequest.class);
        when(req.appointmentId()).thenReturn(appointmentId);
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> appointmentService.checkIn(req));
        assertTrue(ex.getMessage().contains("ngày khám hợp lệ"));
    }

    // =========================================================
    // BRANCH COVERAGE: checkIn() - past date
    // =========================================================

    @Test
    void checkIn_ShouldReject_WhenScheduledDateIsPast() {
        UUID appointmentId = UUID.randomUUID();
        Appointment a = Appointment.builder().appointmentId(appointmentId)
                .status(AppointmentStatus.PENDING)
                .scheduledAt(clinicToday().minusDays(1).atTime(9, 0)).build();

        AppointmentCheckInRequest req = mock(AppointmentCheckInRequest.class);
        when(req.appointmentId()).thenReturn(appointmentId);
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> appointmentService.checkIn(req));
        assertTrue(ex.getMessage().contains("quá ngày"));
    }

    // =========================================================
    // BRANCH COVERAGE: checkIn() - guest with services override and age/gender fallbacks
    // =========================================================

    @Test
    void checkIn_WithServiceOverride_GuestAgeFallbackBranches() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        MedicalService svc = service(serviceId, "Xét nghiệm");
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, null);
        a.setIsGuest(true);
        a.setGuestFullName("Khách");
        a.setGuestPhone("0900000000");
        a.setGuestGender(Gender.MALE);
        a.setServices(Set.of(svc));

        Profile existingGuest = customer(profileId);
        existingGuest.setDateOfBirth(null); // No DOB on existing profile

        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        when(profileRepo.findFirstByPhoneIn(anySet())).thenReturn(Optional.of(existingGuest));
        when(profileRepo.findByIdForUpdate(profileId)).thenReturn(Optional.of(existingGuest));
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));
        when(visitRepo.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(visitRepo.save(any(CustomerVisit.class))).thenAnswer(i -> {
            CustomerVisit v = i.getArgument(0);
            v.setVisitId(UUID.randomUUID());
            return v;
        });
        when(invoiceService.create(any())).thenReturn(mock(InvoiceResponse.class));
        when(repo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        // Service override with patientAge fallback (no DOB on customer, no req DOB)
        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, Set.of(serviceId), staffId, "Khách", "0900000000", null, null,
                null, 30, Gender.MALE);

        assertDoesNotThrow(() -> appointmentService.checkIn(req));
        verify(invoiceService).create(any());
    }

    // =========================================================
    // BRANCH COVERAGE: checkIn() - guest with existing profile found by email
    // =========================================================

    @Test
    void checkIn_GuestProfileFoundByEmail_ShouldReuse() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        MedicalService svc = service(UUID.randomUUID(), "Khám");
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, null);
        a.setIsGuest(true);
        a.setGuestFullName("Email Guest");
        a.setGuestEmail("guest@test.com");
        a.setGuestGender(Gender.FEMALE);
        a.setServices(Set.of(svc));

        Profile byEmail = customer(profileId);
        byEmail.setEmail("guest@test.com");

        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        // email found
        when(profileRepo.findFirstByEmailIgnoreCase("guest@test.com")).thenReturn(Optional.of(byEmail));
        when(profileRepo.findByIdForUpdate(profileId)).thenReturn(Optional.of(byEmail));
        when(visitRepo.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(visitRepo.save(any(CustomerVisit.class))).thenAnswer(i -> {
            CustomerVisit v = i.getArgument(0);
            v.setVisitId(UUID.randomUUID());
            return v;
        });
        when(invoiceService.create(any())).thenReturn(mock(InvoiceResponse.class));
        when(repo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, null, staffId, "Email Guest", null, "guest@test.com", null,
                null, 25, Gender.FEMALE);

        assertDoesNotThrow(() -> appointmentService.checkIn(req));
    }

    // =========================================================
    // BRANCH COVERAGE: checkIn() - existing visit reuse
    // =========================================================

    @Test
    void checkIn_ShouldReuseExistingVisit_WhenAlreadyCreated() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        Profile customer = customer(profileId);
        MedicalService svc = service(UUID.randomUUID(), "Khám");
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, customer);
        a.setServices(Set.of(svc));

        CustomerVisit existingVisit = CustomerVisit.builder().visitId(visitId)
                .customer(customer).build();

        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        when(profileRepo.findByIdForUpdate(profileId)).thenReturn(Optional.of(customer));
        when(visitRepo.findByAppointment_AppointmentId(appointmentId))
                .thenReturn(Optional.of(existingVisit));
        when(invoiceService.create(any())).thenReturn(mock(InvoiceResponse.class));
        when(repo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, null, staffId, null, null, null, null,
                null, null, null);

        assertDoesNotThrow(() -> appointmentService.checkIn(req));
        // Verify visitRepo.save was NOT called (existing visit reused)
        verify(visitRepo, never()).save(any(CustomerVisit.class));
    }

    // =========================================================
    // BRANCH COVERAGE: checkIn() - service with null price
    // =========================================================

    @Test
    void checkIn_ServiceWithNullPrice_ShouldDefaultToZero() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Profile customer = customer(profileId);
        MedicalService svc = service(UUID.randomUUID(), "Free Service");
        svc.setPrice(null);
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, customer);
        a.setServices(Set.of(svc));

        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        when(profileRepo.findByIdForUpdate(profileId)).thenReturn(Optional.of(customer));
        when(visitRepo.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(visitRepo.save(any(CustomerVisit.class))).thenAnswer(i -> {
            CustomerVisit v = i.getArgument(0);
            v.setVisitId(UUID.randomUUID());
            return v;
        });
        when(invoiceService.create(any())).thenReturn(mock(InvoiceResponse.class));
        when(repo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, null, staffId, null, null, null, null, null, null, null);

        assertDoesNotThrow(() -> appointmentService.checkIn(req));
        verify(invoiceService).create(argThat(invoice ->
                invoice.items() != null && invoice.items().get(0).unitPrice().compareTo(BigDecimal.ZERO) == 0));
    }

    // =========================================================
    // BRANCH COVERAGE: checkIn() - RESCHEDULED status
    // =========================================================

    @Test
    void checkIn_ShouldAcceptRescheduledStatus() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Profile customer = customer(profileId);
        MedicalService svc = service(UUID.randomUUID(), "Khám");
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.RESCHEDULED, customer);
        a.setServices(Set.of(svc));

        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        when(profileRepo.findByIdForUpdate(profileId)).thenReturn(Optional.of(customer));
        when(visitRepo.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(visitRepo.save(any(CustomerVisit.class))).thenAnswer(i -> {
            CustomerVisit v = i.getArgument(0);
            v.setVisitId(UUID.randomUUID());
            return v;
        });
        when(invoiceService.create(any())).thenReturn(mock(InvoiceResponse.class));
        when(repo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, null, staffId, null, null, null, null, null, null, null);

        assertDoesNotThrow(() -> appointmentService.checkIn(req));
        assertEquals(AppointmentStatus.CHECKED_IN, a.getStatus());
    }

    // =========================================================
    // BRANCH COVERAGE: checkIn() with DOB-based age from request
    // =========================================================

    @Test
    void checkIn_WithServiceOverride_UsesRequestDob() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        MedicalService svc = service(serviceId, "Khám người lớn");
        svc.setMinimumAge(18);
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, null);
        a.setIsGuest(true);
        a.setGuestFullName("Khách");
        a.setGuestPhone("0900000000");
        a.setGuestGender(Gender.MALE);
        a.setServices(Set.of(svc));

        Profile existingGuest = customer(profileId);

        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        when(profileRepo.findFirstByPhoneIn(anySet())).thenReturn(Optional.of(existingGuest));
        when(profileRepo.findByIdForUpdate(profileId)).thenReturn(Optional.of(existingGuest));
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));
        when(visitRepo.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(visitRepo.save(any(CustomerVisit.class))).thenAnswer(i -> {
            CustomerVisit v = i.getArgument(0);
            v.setVisitId(UUID.randomUUID());
            return v;
        });
        when(invoiceService.create(any())).thenReturn(mock(InvoiceResponse.class));
        when(repo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        // DOB provided in request → age computed from DOB
        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, Set.of(serviceId), staffId, "Khách", "0900000000", null, null,
                clinicToday().minusYears(25), null, Gender.MALE);

        assertDoesNotThrow(() -> appointmentService.checkIn(req));
    }

    // =========================================================
    // BRANCH COVERAGE: checkIn() age from customer DOB
    // =========================================================

    @Test
    void checkIn_WithServiceOverride_UsesCustomerDob() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Profile customer = customer(profileId);
        customer.setDateOfBirth(clinicToday().minusYears(30));
        MedicalService svc = service(serviceId, "Khám");
        svc.setMinimumAge(18);
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, customer);
        a.setServices(Set.of(svc));

        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        when(profileRepo.findByIdForUpdate(profileId)).thenReturn(Optional.of(customer));
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));
        when(visitRepo.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(visitRepo.save(any(CustomerVisit.class))).thenAnswer(i -> {
            CustomerVisit v = i.getArgument(0);
            v.setVisitId(UUID.randomUUID());
            return v;
        });
        when(invoiceService.create(any())).thenReturn(mock(InvoiceResponse.class));
        when(repo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        // Service override: DOB null in req, falls back to customer DOB
        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, Set.of(serviceId), staffId, null, null, null, null,
                null, null, null);

        assertDoesNotThrow(() -> appointmentService.checkIn(req));
    }

    // =========================================================
    // BRANCH COVERAGE: update() - status not changed notification
    // =========================================================

    @Test
    void update_ShouldNotNotify_WhenStatusNotChanged() {
        UUID appId = UUID.randomUUID();
        Profile customer = customer(UUID.randomUUID());
        Appointment a = appointment(appId, AppointmentStatus.PENDING, customer);

        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        when(req.status()).thenReturn(AppointmentStatus.PENDING); // Same status

        when(repo.findById(appId)).thenReturn(Optional.of(a));
        when(repo.save(a)).thenReturn(a);

        appointmentService.update(appId, req);

        // No notification since status didn't change
        verify(notificationService, never()).create(any());
        verify(notificationService, never()).notifyStaffByRole(any(), any(), any(), any(), any());
    }

    // =========================================================
    // BRANCH COVERAGE: update() - CANCELLED but already cancelled
    // =========================================================

    @Test
    void update_ShouldReject_WhenAlreadyCancelled() {
        UUID appId = UUID.randomUUID();
        Appointment a = appointment(appId, AppointmentStatus.CANCELLED, customer(UUID.randomUUID()));
        when(repo.findById(appId)).thenReturn(Optional.of(a));

        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        assertThrows(BadRequestException.class, () -> appointmentService.update(appId, req));
    }

    @Test
    void update_ShouldReject_WhenAlreadyCheckedIn() {
        UUID appId = UUID.randomUUID();
        Appointment a = appointment(appId, AppointmentStatus.CHECKED_IN, customer(UUID.randomUUID()));
        when(repo.findById(appId)).thenReturn(Optional.of(a));

        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        assertThrows(BadRequestException.class, () -> appointmentService.update(appId, req));
    }

    // =========================================================
    // BRANCH COVERAGE: update() - guest cancelled notification
    // =========================================================

    @Test
    void update_GuestCancelled_ShouldNotifyReceptionistWithGuestName() {
        UUID appId = UUID.randomUUID();
        Appointment a = Appointment.builder().appointmentId(appId)
                .status(AppointmentStatus.PENDING).scheduledAt(futureAppointmentTime())
                .isGuest(true).guestFullName("Khách vãng lai").build();

        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        when(req.status()).thenReturn(AppointmentStatus.CANCELLED);
        when(req.cancelReason()).thenReturn("Lý do hủy");

        when(repo.findById(appId)).thenReturn(Optional.of(a));
        when(repo.save(a)).thenReturn(a);

        appointmentService.update(appId, req);

        verify(notificationService).notifyStaffByRole(
                eq(SystemRole.RECEPTIONIST), eq("Lịch hẹn đã bị hủy"),
                argThat(msg -> msg.contains("Khách vãng lai")),
                eq("Appointment"), eq(appId));
    }

    @Test
    void update_NullCustomerCancelled_ShouldUseDefaultName() {
        UUID appId = UUID.randomUUID();
        Appointment a = Appointment.builder().appointmentId(appId)
                .status(AppointmentStatus.PENDING).scheduledAt(futureAppointmentTime())
                .isGuest(false).customer(null).build();

        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        when(req.status()).thenReturn(AppointmentStatus.CANCELLED);
        when(req.cancelReason()).thenReturn("Lý do hủy");

        when(repo.findById(appId)).thenReturn(Optional.of(a));
        when(repo.save(a)).thenReturn(a);

        appointmentService.update(appId, req);

        verify(notificationService).notifyStaffByRole(
                eq(SystemRole.RECEPTIONIST), eq("Lịch hẹn đã bị hủy"),
                argThat(msg -> msg.contains("Khách")),
                eq("Appointment"), eq(appId));
    }

    // =========================================================
    // BRANCH COVERAGE: update() - with shiftId only (no scheduledAt, no serviceIds)
    // =========================================================

    @Test
    void update_WithShiftIdOnly_ShouldRefreshBooking() {
        ShiftScheduleResolver resolver = mock(ShiftScheduleResolver.class);
        ReflectionTestUtils.setField(appointmentService, "shiftScheduleResolver", resolver);

        UUID appId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        Profile customer = customer(UUID.randomUUID());
        Appointment a = appointment(appId, AppointmentStatus.PENDING, customer);
        ShiftConfig shift = ShiftConfig.builder().shiftId(shiftId)
                .name(ShiftConfigService.MORNING).build();
        ShiftScheduleResolver.ResolvedShift resolved = new ShiftScheduleResolver.ResolvedShift(
                shift, null, LocalTime.of(7, 30), LocalTime.of(11, 30),
                ShiftTimeSource.NORMAL, null);

        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        when(req.shiftId()).thenReturn(shiftId);

        when(repo.findById(appId)).thenReturn(Optional.of(a));
        when(shiftConfigRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(resolver.resolve(shift, a.getScheduledAt().toLocalDate())).thenReturn(resolved);
        when(repo.existsOtherCustomerConflict(any(), any(), anyList(), any(), any())).thenReturn(false);
        when(repo.save(a)).thenReturn(a);

        assertDoesNotThrow(() -> appointmentService.update(appId, req));
        assertEquals(shift.getName(), a.getShiftName());
    }

    // =========================================================
    // BRANCH COVERAGE: guestCheckIn() - digits in name
    // =========================================================

    @Test
    void guestCheckIn_ShouldReject_WhenNameContainsDigits() {
        UUID staffId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        GuestCheckInRequest req = mock(GuestCheckInRequest.class);
        MedicalService svc = service(serviceId, "Khám");

        when(req.issuedById()).thenReturn(staffId);
        when(req.serviceIds()).thenReturn(Set.of(serviceId));
        when(req.guestFullName()).thenReturn("Nguyen123");
        when(req.guestGender()).thenReturn(Gender.MALE);
        when(req.guestAge()).thenReturn(30);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));

        assertThrows(BadRequestException.class, () -> appointmentService.guestCheckIn(req));
    }

    // =========================================================
    // BRANCH COVERAGE: guestCheckIn() - OTHER gender
    // =========================================================

    @Test
    void guestCheckIn_ShouldReject_WhenGenderIsOther() {
        UUID staffId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        GuestCheckInRequest req = mock(GuestCheckInRequest.class);
        MedicalService svc = service(serviceId, "Khám");

        when(req.issuedById()).thenReturn(staffId);
        when(req.serviceIds()).thenReturn(Set.of(serviceId));
        when(req.guestFullName()).thenReturn("Khách");
        when(req.guestGender()).thenReturn(Gender.OTHER);
        when(req.guestAge()).thenReturn(30);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));

        assertThrows(BadRequestException.class, () -> appointmentService.guestCheckIn(req));
    }

    // =========================================================
    // BRANCH COVERAGE: guestCheckIn() - issued by null
    // =========================================================

    @Test
    void guestCheckIn_ShouldReject_WhenIssuedByNull() {
        GuestCheckInRequest req = mock(GuestCheckInRequest.class);
        when(req.issuedById()).thenReturn(null);

        assertThrows(BadRequestException.class, () -> appointmentService.guestCheckIn(req));
    }

    // =========================================================
    // BRANCH COVERAGE: guestCheckIn() - empty services
    // =========================================================

    @Test
    void guestCheckIn_ShouldReject_WhenServicesEmpty() {
        GuestCheckInRequest req = mock(GuestCheckInRequest.class);
        when(req.issuedById()).thenReturn(UUID.randomUUID());
        when(req.serviceIds()).thenReturn(Set.of());

        assertThrows(BadRequestException.class, () -> appointmentService.guestCheckIn(req));
    }

    @Test
    void guestCheckIn_ShouldReject_WhenServicesNull() {
        GuestCheckInRequest req = mock(GuestCheckInRequest.class);
        when(req.issuedById()).thenReturn(UUID.randomUUID());
        when(req.serviceIds()).thenReturn(null);

        assertThrows(BadRequestException.class, () -> appointmentService.guestCheckIn(req));
    }

    // =========================================================
    // BRANCH COVERAGE: appointmentResponse() - with FamilyMember
    // =========================================================

    @Test
    void appointmentResponse_ShouldUseOwnerProfileFromFamilyMember() {
        UUID profileId = UUID.randomUUID();
        UUID ownerProfileId = UUID.randomUUID();
        Profile customer = customer(profileId);
        Profile ownerProfile = customer(ownerProfileId);
        ownerProfile.setPhone("0888888888");

        Appointment a = appointment(UUID.randomUUID(), AppointmentStatus.PENDING, customer);
        FamilyMember member = FamilyMember.builder().ownerProfile(ownerProfile)
                .memberProfile(customer).build();

        when(repo.findById(a.getAppointmentId())).thenReturn(Optional.of(a));
        when(familyMemberRepository.findByMemberProfile_ProfileId(profileId))
                .thenReturn(Optional.of(member));

        var result = appointmentService.get(a.getAppointmentId());

        assertNotNull(result);
        assertEquals("0888888888", result.contactManagerPhone());
    }

    @Test
    void appointmentResponse_ShouldUseSelfProfile_WhenNoFamilyMember() {
        UUID profileId = UUID.randomUUID();
        Profile customer = customer(profileId);

        Appointment a = appointment(UUID.randomUUID(), AppointmentStatus.PENDING, customer);

        when(repo.findById(a.getAppointmentId())).thenReturn(Optional.of(a));
        when(familyMemberRepository.findByMemberProfile_ProfileId(profileId))
                .thenReturn(Optional.empty());

        var result = appointmentService.get(a.getAppointmentId());

        assertNotNull(result);
        assertEquals(customer.getPhone(), result.contactManagerPhone());
    }

    @Test
    void appointmentResponse_ShouldHandleNullCustomer() {
        Appointment a = Appointment.builder().appointmentId(UUID.randomUUID())
                .status(AppointmentStatus.PENDING).scheduledAt(futureAppointmentTime())
                .isGuest(true).guestFullName("Khách").build();

        when(repo.findById(a.getAppointmentId())).thenReturn(Optional.of(a));

        var result = appointmentService.get(a.getAppointmentId());

        assertNotNull(result);
        assertNull(result.contactManagerPhone());
    }

    // =========================================================
    // BRANCH COVERAGE: notifyReceptionists() - guest vs customer vs null
    // =========================================================

    @Test
    void notifyReceptionists_CoversGuestAndCustomerAndNullName() {
        // Guest → uses guestFullName
        Appointment guest = Appointment.builder().appointmentId(UUID.randomUUID())
                .scheduledAt(futureAppointmentTime()).isGuest(true)
                .guestFullName("Guest Name").build();
        ReflectionTestUtils.invokeMethod(appointmentService, "notifyReceptionists", guest);
        verify(notificationService).notifyStaffByRole(eq(SystemRole.RECEPTIONIST), eq("Lịch hẹn mới"),
                argThat(msg -> msg.contains("Guest Name")), eq("Appointment"), any());

        // Customer → uses customer fullName
        reset(notificationService);
        Profile customer = Profile.builder().profileId(UUID.randomUUID()).fullName("Customer Name").build();
        Appointment withCustomer = Appointment.builder().appointmentId(UUID.randomUUID())
                .scheduledAt(futureAppointmentTime()).isGuest(false).customer(customer).build();
        ReflectionTestUtils.invokeMethod(appointmentService, "notifyReceptionists", withCustomer);
        verify(notificationService).notifyStaffByRole(eq(SystemRole.RECEPTIONIST), eq("Lịch hẹn mới"),
                argThat(msg -> msg.contains("Customer Name")), eq("Appointment"), any());

        // Null isGuest and null customer → "Khách"
        reset(notificationService);
        Appointment noInfo = Appointment.builder().appointmentId(UUID.randomUUID())
                .scheduledAt(futureAppointmentTime()).isGuest(null).customer(null).build();
        ReflectionTestUtils.invokeMethod(appointmentService, "notifyReceptionists", noInfo);
        verify(notificationService).notifyStaffByRole(eq(SystemRole.RECEPTIONIST), eq("Lịch hẹn mới"),
                argThat(msg -> msg.contains("Khách")), eq("Appointment"), any());
    }

    // =========================================================
    // BRANCH COVERAGE: updatePatientInformation() - all field combinations
    // =========================================================

    @Test
    void updatePatientInformation_ShouldUpdateAllGuestFields() {
        Appointment a = Appointment.builder().build();

        ReflectionTestUtils.invokeMethod(appointmentService, "updatePatientInformation",
                a, "Tên Mới", "0912345678", "test@mail.com", "123 Street",
                LocalDate.of(1990, 1, 1), 30, Gender.FEMALE);

        assertEquals("Tên Mới", a.getGuestFullName());
        assertEquals("0912345678", a.getGuestPhone());
        assertEquals("test@mail.com", a.getGuestEmail());
        assertEquals("123 Street", a.getGuestAddress());
        assertEquals(30, a.getGuestAge());
        assertEquals(Gender.FEMALE, a.getGuestGender());
    }

    @Test
    void updatePatientInformation_ShouldSkipNullFields() {
        Appointment a = Appointment.builder()
                .guestFullName("Old").guestPhone("0900").guestEmail("old@test.com")
                .guestAddress("Old Addr").guestAge(20).guestGender(Gender.MALE).build();

        ReflectionTestUtils.invokeMethod(appointmentService, "updatePatientInformation",
                a, null, null, null, null, (LocalDate) null, (Integer) null, (Gender) null);

        assertEquals("Old", a.getGuestFullName());
        assertEquals("0900", a.getGuestPhone());
        assertEquals("old@test.com", a.getGuestEmail());
        assertEquals("Old Addr", a.getGuestAddress());
        assertEquals(20, a.getGuestAge());
        assertEquals(Gender.MALE, a.getGuestGender());
    }

    // =========================================================
    // BRANCH COVERAGE: emptyToNull
    // =========================================================

    @Test
    void emptyToNull_ShouldReturnTrimmedValueOrNull() {
        assertNull(ReflectionTestUtils.invokeMethod(appointmentService, "emptyToNull", (Object) null));
        assertNull(ReflectionTestUtils.invokeMethod(appointmentService, "emptyToNull", "  "));
        assertNull(ReflectionTestUtils.invokeMethod(appointmentService, "emptyToNull", ""));
        assertEquals("hello", ReflectionTestUtils.invokeMethod(appointmentService, "emptyToNull", " hello "));
    }

    // =========================================================
    // BRANCH COVERAGE: createForGuest() - with shift
    // =========================================================

    @Test
    void createForGuest_WithShift_ShouldResolveAndSave() {
        ShiftScheduleResolver resolver = mock(ShiftScheduleResolver.class);
        ServiceAvailabilityService availabilityService = mock(ServiceAvailabilityService.class);
        ReflectionTestUtils.setField(appointmentService, "shiftScheduleResolver", resolver);
        ReflectionTestUtils.setField(appointmentService, "serviceAvailabilityService", availabilityService);

        UUID serviceId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        MedicalService svc = service(serviceId, "Khám guest");
        svc.setAllowCustomerBooking(true);
        ShiftConfig shift = ShiftConfig.builder().shiftId(shiftId)
                .name(ShiftConfigService.MORNING).build();
        LocalDateTime requestedAt = futureAppointmentTime();
        ShiftScheduleResolver.ResolvedShift resolved = new ShiftScheduleResolver.ResolvedShift(
                shift, null, LocalTime.of(7, 30), LocalTime.of(11, 30),
                ShiftTimeSource.NORMAL, null);

        AppointmentGuestCreateRequest req = mock(AppointmentGuestCreateRequest.class);
        when(req.guestGender()).thenReturn(Gender.FEMALE);
        when(req.guestAge()).thenReturn(30);
        when(req.guestFullName()).thenReturn("Guest A");
        when(req.guestPhone()).thenReturn("0900000000");
        when(req.scheduledAt()).thenReturn(requestedAt);
        when(req.shiftId()).thenReturn(shiftId);
        when(req.serviceIds()).thenReturn(Set.of(serviceId));

        when(shiftConfigRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));
        when(resolver.resolve(shift, requestedAt.toLocalDate())).thenReturn(resolved);
        when(availabilityService.evaluate(any(), any(), any(), eq(true)))
                .thenReturn(new ServiceAvailabilityService.Evaluation(true, null, List.of()));
        when(repo.existsGuestConflict(any(), any(), anyList(), any(), any())).thenReturn(false);
        when(repo.save(any(Appointment.class))).thenAnswer(i -> {
            Appointment saved = i.getArgument(0);
            saved.setAppointmentId(UUID.randomUUID());
            return saved;
        });

        var result = appointmentService.createForGuest(req);

        assertNotNull(result);
        verify(repo).save(argThat(a -> a.getShiftName() != null && a.getShiftTime() != null));
    }

    // =========================================================
    // BRANCH COVERAGE: createForGuest() - service not allowed for customer booking
    // =========================================================

    @Test
    void createForGuest_ShouldReject_WhenServiceNotAllowedForCustomerBooking() {
        UUID serviceId = UUID.randomUUID();
        MedicalService svc = service(serviceId, "XN lẻ");
        svc.setAllowCustomerBooking(false);

        AppointmentGuestCreateRequest req = mock(AppointmentGuestCreateRequest.class);
        when(req.serviceIds()).thenReturn(Set.of(serviceId));
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));

        assertThrows(BadRequestException.class, () -> appointmentService.createForGuest(req));
    }

    // =========================================================
    // BRANCH COVERAGE: createForGuest() - email conflict check
    // =========================================================

    @Test
    void createForGuest_ShouldCheckConflictByEmail_WhenPhoneBlank() {
        AppointmentGuestCreateRequest req = mock(AppointmentGuestCreateRequest.class);
        when(req.guestGender()).thenReturn(Gender.MALE);
        when(req.scheduledAt()).thenReturn(futureAppointmentTime());
        when(req.guestPhone()).thenReturn(" ");
        when(req.guestEmail()).thenReturn("guest@test.com");

        when(repo.existsGuestConflict(isNull(), eq("guest@test.com"), anyList(), any(), any()))
                .thenReturn(true);

        assertThrows(BadRequestException.class, () -> appointmentService.createForGuest(req));
    }

    // =========================================================
    // BRANCH COVERAGE: updateMyAppointment() - guest appointment rejection
    // =========================================================

    @Test
    void updateMyAppointment_ShouldReject_WhenGuestAppointment() {
        UUID appId = UUID.randomUUID();
        Appointment a = Appointment.builder().appointmentId(appId)
                .status(AppointmentStatus.PENDING).scheduledAt(futureAppointmentTime())
                .customer(null).build();

        when(repo.findByIdForUpdate(appId)).thenReturn(Optional.of(a));

        assertThrows(BadRequestException.class, () ->
                appointmentService.updateMyAppointment(UUID.randomUUID(), appId,
                        mock(AppointmentUpdateRequest.class)));
    }

    // =========================================================
    // BRANCH COVERAGE: updateMyAppointment() - with service update
    // =========================================================

    @Test
    void updateMyAppointment_WithServiceUpdate_ShouldValidateEligibility() {
        ShiftScheduleResolver resolver = mock(ShiftScheduleResolver.class);
        ServiceAvailabilityService availabilityService = mock(ServiceAvailabilityService.class);
        ReflectionTestUtils.setField(appointmentService, "shiftScheduleResolver", resolver);
        ReflectionTestUtils.setField(appointmentService, "serviceAvailabilityService", availabilityService);

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Profile owner = customer(profileId);
        MedicalService svc = service(serviceId, "Khám");

        Appointment a = appointment(UUID.randomUUID(), AppointmentStatus.PENDING, owner);

        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        when(req.serviceIds()).thenReturn(Set.of(serviceId));
        when(req.scheduledAt()).thenReturn(null);
        when(req.shiftId()).thenReturn(null);

        when(repo.findByIdForUpdate(a.getAppointmentId())).thenReturn(Optional.of(a));
        when(familyAccessService.resolveActiveProfile(accountId, profileId)).thenReturn(owner);
        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        when(familyAccessService.relationship(accountId, profileId)).thenReturn(null);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));
        when(repo.existsOtherCustomerConflict(any(), any(), anyList(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = appointmentService.updateMyAppointment(accountId, a.getAppointmentId(), req);

        assertNotNull(result);
    }

    // =========================================================
    // BRANCH COVERAGE: updateMyAppointment() - customer DOB null
    // =========================================================

    @Test
    void updateMyAppointment_WithServiceUpdate_NoDob_ShouldUseNullAge() {
        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Profile owner = customer(profileId);
        owner.setDateOfBirth(null);
        MedicalService svc = service(serviceId, "Khám chung");

        Appointment a = appointment(UUID.randomUUID(), AppointmentStatus.RESCHEDULED, owner);

        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        when(req.serviceIds()).thenReturn(Set.of(serviceId));

        when(repo.findByIdForUpdate(a.getAppointmentId())).thenReturn(Optional.of(a));
        when(familyAccessService.resolveActiveProfile(accountId, profileId)).thenReturn(owner);
        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        when(familyAccessService.relationship(accountId, profileId)).thenReturn(null);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));
        when(repo.existsOtherCustomerConflict(any(), any(), anyList(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> appointmentService.updateMyAppointment(
                accountId, a.getAppointmentId(), req));
    }

    // =========================================================
    // BRANCH COVERAGE: cancelMyAppointment() - guest appointment
    // =========================================================

    @Test
    void cancelMyAppointment_ShouldReject_WhenGuestAppointment() {
        UUID appId = UUID.randomUUID();
        Appointment a = Appointment.builder().appointmentId(appId)
                .status(AppointmentStatus.PENDING).scheduledAt(futureAppointmentTime())
                .customer(null).build();

        when(repo.findByIdForUpdate(appId)).thenReturn(Optional.of(a));

        assertThrows(BadRequestException.class, () ->
                appointmentService.cancelMyAppointment(UUID.randomUUID(), appId));
    }

    // =========================================================
    // BRANCH COVERAGE: cancelMyAppointment() - RESCHEDULED
    // =========================================================

    @Test
    void cancelMyAppointment_ShouldCancelRescheduledAppointment() {
        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        Profile customer = customer(profileId);
        Appointment a = appointment(UUID.randomUUID(), AppointmentStatus.RESCHEDULED, customer);

        when(familyAccessService.resolveActiveProfile(accountId, profileId)).thenReturn(customer);
        when(repo.findByIdForUpdate(a.getAppointmentId())).thenReturn(Optional.of(a));
        when(repo.save(a)).thenReturn(a);

        appointmentService.cancelMyAppointment(accountId, a.getAppointmentId());

        assertEquals(AppointmentStatus.CANCELLED, a.getStatus());
    }

    // =========================================================
    // BRANCH COVERAGE: validateDistinctServices()
    // =========================================================

    @Test
    void validateDistinctServices_ShouldRejectNullIdsInList() {
        java.util.ArrayList<UUID> idsWithNull = new java.util.ArrayList<>();
        idsWithNull.add(UUID.randomUUID());
        idsWithNull.add(null);

        assertThrows(BadRequestException.class, () ->
                ReflectionTestUtils.invokeMethod(appointmentService, "validateDistinctServices", idsWithNull));
    }

    @Test
    void validateDistinctServices_ShouldRejectDuplicateIds() {
        UUID dup = UUID.randomUUID();
        assertThrows(BadRequestException.class, () ->
                ReflectionTestUtils.invokeMethod(appointmentService, "validateDistinctServices",
                        List.of(dup, dup)));
    }

    @Test
    void validateDistinctServices_ShouldRejectEmptyList() {
        assertThrows(BadRequestException.class, () ->
                ReflectionTestUtils.invokeMethod(appointmentService, "validateDistinctServices",
                        List.of()));
    }

    // =========================================================
    // BRANCH COVERAGE: getGuestHistoryByPhone() - valid phone
    // =========================================================

    @Test
    void getGuestHistoryByPhone_ShouldReturnResults_WhenPhoneValid() {
        Appointment a = Appointment.builder().appointmentId(UUID.randomUUID())
                .guestFullName("Test").guestPhone("0900").scheduledAt(LocalDateTime.now())
                .status(AppointmentStatus.PENDING).isGuest(true).build();
        when(repo.findGuestAppointmentsByPhone("0900000000")).thenReturn(List.of(a));

        var result = appointmentService.getGuestHistoryByPhone("0900000000");

        assertEquals(1, result.size());
    }

    // =========================================================
    // BRANCH COVERAGE: guestCheckIn() - service with null price
    // =========================================================

    @Test
    void guestCheckIn_ServiceWithNullPrice_ShouldDefaultToZero() {
        UUID staffId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        GuestCheckInRequest req = mock(GuestCheckInRequest.class);
        MedicalService svc = service(serviceId, "Free");
        svc.setPrice(null);

        when(req.issuedById()).thenReturn(staffId);
        when(req.serviceIds()).thenReturn(Set.of(serviceId));
        when(req.guestFullName()).thenReturn("Guest");
        when(req.guestGender()).thenReturn(Gender.MALE);
        when(req.guestAge()).thenReturn(30);
        when(req.guestPhone()).thenReturn("0900000000");
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));

        Profile guest = customer(profileId);
        when(profileRepo.findFirstByPhone("0900000000")).thenReturn(Optional.of(guest));
        when(profileRepo.findByIdForUpdate(profileId)).thenReturn(Optional.of(guest));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        when(visitRepo.save(any(CustomerVisit.class))).thenAnswer(i -> {
            CustomerVisit v = i.getArgument(0);
            v.setVisitId(UUID.randomUUID());
            return v;
        });
        when(invoiceService.create(any())).thenReturn(mock(InvoiceResponse.class));

        assertDoesNotThrow(() -> appointmentService.guestCheckIn(req));
        verify(invoiceService).create(argThat(inv ->
                inv.items() != null && inv.items().get(0).unitPrice().compareTo(BigDecimal.ZERO) == 0));
    }

    // =========================================================
    // BRANCH COVERAGE: update() with scheduledAt change triggering notification
    // =========================================================

    @Test
    void update_ShouldNotifyCustomer_WhenRescheduled() {
        UUID appId = UUID.randomUUID();
        Profile customer = customer(UUID.randomUUID());
        Appointment a = appointment(appId, AppointmentStatus.PENDING, customer);

        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        when(req.status()).thenReturn(AppointmentStatus.RESCHEDULED);
        when(req.scheduledAt()).thenReturn(clinicToday().plusDays(5).atTime(14, 0));

        when(repo.findById(appId)).thenReturn(Optional.of(a));
        when(repo.existsOtherCustomerConflict(any(), any(), anyList(), any(), any())).thenReturn(false);
        when(repo.save(a)).thenReturn(a);
        when(familyAccessService.notificationRecipientProfileId(customer))
                .thenReturn(customer.getProfileId());

        appointmentService.update(appId, req);

        verify(notificationService).create(any());
    }

    // =========================================================
    // BRANCH COVERAGE: linkGuestHistory - guest with no account
    // =========================================================

    @Test
    void linkGuestHistory_GuestWithoutAccount_ShouldNotSetIsGuestFalse() {
        Profile profile = Profile.builder().profileId(UUID.randomUUID()).account(null).build();
        Appointment guest = Appointment.builder().appointmentId(UUID.randomUUID()).isGuest(true).build();
        when(repo.findGuestAppointmentsByPhonesOrEmails(anySet(), anySet())).thenReturn(List.of(guest));
        when(visitRepo.findByAppointment_AppointmentId(guest.getAppointmentId())).thenReturn(Optional.empty());

        ReflectionTestUtils.invokeMethod(appointmentService, "linkGuestHistoryBeforeContactChange",
                profile, "0900000000", null);

        assertSame(profile, guest.getCustomer());
        assertTrue(guest.getIsGuest()); // Not changed because account is null
    }

    // =========================================================
    // BRANCH COVERAGE: refreshBookingSelection - resolved null
    // =========================================================

    @Test
    void refreshBookingSelection_WithNoShift_ShouldUseTargetTime() {
        Appointment a = appointment(UUID.randomUUID(), AppointmentStatus.PENDING, null);
        LocalDateTime newTime = clinicToday().plusDays(5).atTime(9, 0);

        // No shift, resolved = null
        ReflectionTestUtils.invokeMethod(appointmentService, "refreshBookingSelection",
                a, newTime, (UUID) null, a.getServices());

        assertEquals(newTime, a.getScheduledAt());
    }

    // =========================================================
    // BRANCH COVERAGE: updatePatientInformation with customer + email normalization
    // =========================================================

    @Test
    void updatePatientInformation_WithCustomer_ShouldUpdateBothGuestAndProfile() {
        UUID profileId = UUID.randomUUID();
        Profile customer = Profile.builder().profileId(profileId).fullName("Old")
                .phone("0900000000").email("old@test.com").build();
        Appointment a = Appointment.builder().customer(customer).build();

        when(profileRepo.findFirstByPhone("0911111111")).thenReturn(Optional.empty());
        when(profileRepo.findFirstByEmail("new@test.com")).thenReturn(Optional.empty());
        when(profileRepo.save(customer)).thenReturn(customer);

        ReflectionTestUtils.invokeMethod(appointmentService, "updatePatientInformation",
                a, "New Name", "0911111111", "NEW@TEST.COM", "Addr",
                LocalDate.of(1995, 5, 5), 28, Gender.FEMALE);

        assertEquals("New Name", a.getGuestFullName());
        assertEquals("New Name", customer.getFullName());
        assertEquals("0911111111", customer.getPhone());
        assertEquals("new@test.com", customer.getEmail());
    }

    // =========================================================
    // BRANCH COVERAGE: updateProfileInformation - same phone/email no change
    // =========================================================

    @Test
    void updateProfileInformation_ShouldNotCheckDuplicates_WhenPhoneUnchanged() {
        UUID profileId = UUID.randomUUID();
        Profile profile = Profile.builder().profileId(profileId)
                .phone("0900000000").email("same@test.com").build();
        when(profileRepo.save(profile)).thenReturn(profile);

        // Same phone and email - should not trigger duplicate checks
        ReflectionTestUtils.invokeMethod(appointmentService, "updateProfileInformation",
                profile, null, "0900000000", "same@test.com", null, null, null);

        verify(profileRepo, never()).findFirstByPhone(any());
        verify(profileRepo, never()).findFirstByEmail(any());
    }

    // =========================================================
    // BRANCH COVERAGE: updateProfileInformation - null phone clearing
    // =========================================================

    @Test
    void updateProfileInformation_ShouldClearPhoneAndEmail_WhenBlank() {
        UUID profileId = UUID.randomUUID();
        Profile profile = Profile.builder().profileId(profileId)
                .phone("0900000000").email("old@test.com").build();
        when(profileRepo.save(profile)).thenReturn(profile);

        ReflectionTestUtils.invokeMethod(appointmentService, "updateProfileInformation",
                profile, null, "  ", "  ", null, null, null);

        assertNull(profile.getPhone());
        assertNull(profile.getEmail());
    }

    // =========================================================
    // BRANCH COVERAGE: normalizeServiceSelection with null policy service
    // =========================================================

    @Test
    void normalizeServiceSelection_ShouldFallbackToRepo_WhenPolicyServiceNull() {
        ReflectionTestUtils.setField(appointmentService, "serviceSelectionPolicyService", null);
        UUID serviceId = UUID.randomUUID();
        MedicalService svc = service(serviceId, "Test");
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(svc));

        @SuppressWarnings("unchecked")
        List<MedicalService> result = ReflectionTestUtils.invokeMethod(appointmentService,
                "normalizeServiceSelection", (Object) List.of(serviceId));

        assertEquals(1, result.size());
        assertSame(svc, result.get(0));
    }

    @Test
    void normalizeServiceSelection_ShouldThrow_WhenServiceNotFound_FallbackPath() {
        ReflectionTestUtils.setField(appointmentService, "serviceSelectionPolicyService", null);
        UUID serviceId = UUID.randomUUID();
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                ReflectionTestUtils.invokeMethod(appointmentService,
                        "normalizeServiceSelection", (Object) List.of(serviceId)));
    }

    // =========================================================
    // BRANCH COVERAGE: formatShiftTime
    // =========================================================

    @Test
    void formatShiftTime_ShouldReturnFormattedTimeRange() {
        ShiftScheduleResolver.ResolvedShift resolved = new ShiftScheduleResolver.ResolvedShift(
                null, null, LocalTime.of(7, 30), LocalTime.of(11, 30),
                ShiftTimeSource.NORMAL, null);

        String result = ReflectionTestUtils.invokeMethod(appointmentService, "formatShiftTime", resolved);
        assertEquals("07:30 - 11:30", result);
    }

    // =========================================================
    // BRANCH COVERAGE: resolveRequestedShift
    // =========================================================

    @Test
    void resolveRequestedShift_ShouldReturnNull_WhenShiftIdNull() {
        assertNull(ReflectionTestUtils.invokeMethod(appointmentService,
                "resolveRequestedShift", (UUID) null, Appointment.builder().build()));
    }

    @Test
    void resolveRequestedShift_ShouldThrow_WhenShiftNotFound() {
        UUID shiftId = UUID.randomUUID();
        when(shiftConfigRepository.findById(shiftId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                ReflectionTestUtils.invokeMethod(appointmentService,
                        "resolveRequestedShift", shiftId, Appointment.builder().build()));
    }

    @Test
    void resolveRequestedShift_ShouldReturnShift_WhenFound() {
        UUID shiftId = UUID.randomUUID();
        ShiftConfig shift = ShiftConfig.builder().shiftId(shiftId).build();
        when(shiftConfigRepository.findById(shiftId)).thenReturn(Optional.of(shift));

        assertSame(shift, ReflectionTestUtils.invokeMethod(appointmentService,
                "resolveRequestedShift", shiftId, Appointment.builder().build()));
    }

    // =========================================================
    // BRANCH COVERAGE: checkIn() - guest new profile (no phone, no email, no existing)
    // =========================================================

    @Test
    void checkIn_Guest_NoExistingProfile_ShouldCreateNewProfile() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID newProfileId = UUID.randomUUID();

        MedicalService svc = service(UUID.randomUUID(), "Khám");
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, null);
        a.setIsGuest(true);
        a.setGuestFullName("New Guest");
        a.setGuestGender(Gender.MALE);
        a.setServices(Set.of(svc));

        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        when(profileRepo.saveAndFlush(any(Profile.class))).thenAnswer(i -> {
            Profile p = i.getArgument(0);
            p.setProfileId(newProfileId);
            return p;
        });
        when(profileRepo.findByIdForUpdate(newProfileId)).thenReturn(Optional.of(
                Profile.builder().profileId(newProfileId).fullName("New Guest").gender(Gender.MALE).build()));
        when(visitRepo.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(visitRepo.save(any(CustomerVisit.class))).thenAnswer(i -> {
            CustomerVisit v = i.getArgument(0);
            v.setVisitId(UUID.randomUUID());
            return v;
        });
        when(invoiceService.create(any())).thenReturn(mock(InvoiceResponse.class));
        when(repo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, null, staffId, "New Guest", null, null, null,
                null, null, Gender.MALE);

        assertDoesNotThrow(() -> appointmentService.checkIn(req));
        verify(profileRepo).saveAndFlush(any(Profile.class));
    }
    // =========================================================
    // BRANCH COVERAGE: Additional Branches
    // =========================================================

    @Test
    void normalizeServiceSelection_WithPolicyService_ShouldCallPolicy() {
        org.example.doansummer2026.service.MedicalServiceSelectionPolicyService mockPolicy = mock(org.example.doansummer2026.service.MedicalServiceSelectionPolicyService.class);
        ReflectionTestUtils.setField(appointmentService, "serviceSelectionPolicyService", mockPolicy);
        UUID serviceId = UUID.randomUUID();
        when(mockPolicy.normalizeOrThrow(anyCollection())).thenReturn(List.of(service(serviceId, "Test")));

        @SuppressWarnings("unchecked")
        List<MedicalService> result = (List<MedicalService>) ReflectionTestUtils.invokeMethod(appointmentService,
                "normalizeServiceSelection", (Object) List.of(serviceId));

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(mockPolicy).normalizeOrThrow(anyCollection());
    }

    @Test
    void createForGuest_ShouldThrow_WhenShiftNotFound() {
        UUID shiftId = UUID.randomUUID();
        AppointmentGuestCreateRequest req = mock(AppointmentGuestCreateRequest.class);
        when(req.guestGender()).thenReturn(Gender.MALE);
        when(req.shiftId()).thenReturn(shiftId);
        when(shiftConfigRepository.findById(shiftId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> appointmentService.createForGuest(req));
    }

    @Test
    void createForGuest_WithNullServiceIds_ShouldCreate() {
        AppointmentGuestCreateRequest req = mock(AppointmentGuestCreateRequest.class);
        when(req.guestGender()).thenReturn(Gender.MALE);
        when(req.shiftId()).thenReturn(null);
        when(req.serviceIds()).thenReturn(null);
        when(req.scheduledAt()).thenReturn(futureAppointmentTime());
        
        when(repo.save(any(Appointment.class))).thenAnswer(i -> {
            Appointment a = i.getArgument(0);
            a.setAppointmentId(UUID.randomUUID());
            return a;
        });

        assertDoesNotThrow(() -> appointmentService.createForGuest(req));
    }

    @Test
    void update_WithNullServiceIds_ShouldNotUpdateServices() {
        UUID appointmentId = UUID.randomUUID();
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, null);
        a.setServices(Set.of(service(UUID.randomUUID(), "Test")));

        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        when(req.serviceIds()).thenReturn(null);
        
        when(repo.findById(appointmentId)).thenReturn(Optional.of(a));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> appointmentService.update(appointmentId, req));
    }

    @Test
    void update_WithNullScheduledAtShiftIdServiceIds_ShouldNotRefresh() {
        UUID appointmentId = UUID.randomUUID();
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, null);
        
        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        when(req.scheduledAt()).thenReturn(null);
        when(req.shiftId()).thenReturn(null);
        when(req.serviceIds()).thenReturn(null);

        when(repo.findById(appointmentId)).thenReturn(Optional.of(a));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> appointmentService.update(appointmentId, req));
    }

    @Test
    void update_ShouldNotifyWithGuestName_WhenIsGuest() {
        UUID appointmentId = UUID.randomUUID();
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, null);
        a.setIsGuest(true);
        a.setGuestFullName("Guest User");
        a.setScheduledAt(clinicToday().atTime(9, 0));

        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        when(req.status()).thenReturn(AppointmentStatus.CANCELLED);
        when(req.cancelReason()).thenReturn("Lý do");
        when(req.serviceIds()).thenReturn(null);
        when(req.scheduledAt()).thenReturn(null);
        when(req.shiftId()).thenReturn(null);

        when(repo.findById(appointmentId)).thenReturn(Optional.of(a));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> appointmentService.update(appointmentId, req));
        verify(notificationService).notifyStaffByRole(eq(SystemRole.RECEPTIONIST), anyString(), contains("Guest User đã hủy"), anyString(), eq(appointmentId));
    }

    @Test
    void update_ShouldNotifyWithKhach_WhenNotGuestAndCustomerNull() {
        UUID appointmentId = UUID.randomUUID();
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, null);
        a.setIsGuest(null);
        a.setScheduledAt(clinicToday().atTime(9, 0));

        AppointmentUpdateRequest req = mock(AppointmentUpdateRequest.class);
        when(req.status()).thenReturn(AppointmentStatus.CANCELLED);
        when(req.cancelReason()).thenReturn("Lý do");
        when(req.serviceIds()).thenReturn(null);
        when(req.scheduledAt()).thenReturn(null);
        when(req.shiftId()).thenReturn(null);

        when(repo.findById(appointmentId)).thenReturn(Optional.of(a));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> appointmentService.update(appointmentId, req));
        verify(notificationService).notifyStaffByRole(eq(SystemRole.RECEPTIONIST), anyString(), contains("Khách đã hủy"), anyString(), eq(appointmentId));
    }

    @Test
    void validateServiceEligibility_AgeNull_MaxAgeNotNull_ShouldThrow() {
        MedicalService svc = service(UUID.randomUUID(), "Test");
        svc.setMinimumAge(null);
        svc.setMaximumAge(10);
        svc.setAllowCustomerBooking(true);
        svc.setStatus(org.example.doansummer2026.enums.ServiceStatus.ACTIVE);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                ReflectionTestUtils.invokeMethod(appointmentService, "validateServiceEligibility", svc, null, Gender.MALE));
        assertTrue(ex.getMessage().contains("Vui lòng cập nhật ngày sinh trước khi đặt dịch vụ"));
    }

    @Test
    void checkIn_WithNullCustomerInVisit_ShouldSetNullCustomerIdInInvoice() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Profile customer = customer(UUID.randomUUID());
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, customer);
        a.setServices(Set.of(service(UUID.randomUUID(), "Test")));

        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, null, staffId, "Khách", "0900000000", null, null, null, null, Gender.MALE);
        
        when(profileRepo.findByIdForUpdate(customer.getProfileId())).thenReturn(Optional.of(customer));
        
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(null).build();
        when(visitRepo.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.of(visit));
        
        when(invoiceService.create(any())).thenReturn(mock(InvoiceResponse.class));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> appointmentService.checkIn(req));
        
        org.mockito.ArgumentCaptor<org.example.doansummer2026.dto.invoice.InvoiceCreateRequest> captor = org.mockito.ArgumentCaptor.forClass(org.example.doansummer2026.dto.invoice.InvoiceCreateRequest.class);
        verify(invoiceService).create(captor.capture());
        assertNull(captor.getValue().customerId());
    }

    @Test
    void getMyAppointments_Overload_ShouldCallMainMethod() {
        UUID accountId = UUID.randomUUID();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Appointment> page = new org.springframework.data.domain.PageImpl<>(List.of());
        when(repo.searchForCustomers(anyList(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        var result = appointmentService.getMyAppointments(accountId, "CODE", "SPEC", "STATUS", null, null, pageable);
        assertNotNull(result);
    }

    @Test
    void checkIn_WithEmptyServices_ShouldThrow() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, customer(UUID.randomUUID()));
        a.setServices(java.util.Collections.emptySet());

        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, null, staffId, "Patient", "0900000000", null, null, null, null, Gender.MALE);
        
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> appointmentService.checkIn(req));
        assertTrue(ex.getMessage().contains("Lịch hẹn chưa chọn dịch vụ"));
    }

    @Test
    void checkIn_WithServiceIdsAndPatientDob_ShouldResolveAgeAndGenderFromCustomer() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Profile customer = customer(UUID.randomUUID());
        customer.setDateOfBirth(LocalDate.of(2000, 1, 1));
        customer.setGender(Gender.MALE);
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, customer);

        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, Set.of(UUID.randomUUID()), staffId, "Patient", "0900000000", null, null, null, null, null);
        
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        
        org.example.doansummer2026.service.MedicalServiceSelectionPolicyService mockPolicy = mock(org.example.doansummer2026.service.MedicalServiceSelectionPolicyService.class);
        ReflectionTestUtils.setField(appointmentService, "serviceSelectionPolicyService", mockPolicy);
        MedicalService svc = service(UUID.randomUUID(), "Test");
        when(mockPolicy.normalizeOrThrow(any())).thenReturn(List.of(svc));
        
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> appointmentService.checkIn(req));
        assertTrue(ex.getMessage().contains("Không tìm thấy hồ sơ"));
    }

    @Test
    void resolveExistingPatientProfile_WithEmptyPhone_ShouldCheckEmail() {
        Appointment a = new Appointment();
        
        Profile mockProfile = mock(Profile.class);
        when(profileRepo.findFirstByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(mockProfile));

        Profile result = (Profile) ReflectionTestUtils.invokeMethod(appointmentService,
                "resolveExistingPatientProfile", a, "   ", "test@test.com");
        
        assertNotNull(result);
        verify(profileRepo).findFirstByEmailIgnoreCase("test@test.com");
    }

    @Test
    void resolveExistingPatientProfile_WithEmptyEmail_ShouldReturnNull() {
        Appointment a = new Appointment();

        Profile result = (Profile) ReflectionTestUtils.invokeMethod(appointmentService,
                "resolveExistingPatientProfile", a, "   ", "   ");
        
        assertNull(result);
    }

    @Test
    void linkGuestHistoryBeforeContactChange_WithBlankOldPhone_ShouldCheckEmail() {
        Profile profile = customer(UUID.randomUUID());
        profile.setPhone("0912345678");
        profile.setEmail("new@test.com");
        
        Appointment mockGuestAppt = new Appointment();
        mockGuestAppt.setAppointmentId(UUID.randomUUID());
        when(repo.findGuestAppointmentsByPhonesOrEmails(any(), any())).thenReturn(List.of(mockGuestAppt));
        
        CustomerVisit mockVisit = new CustomerVisit();
        mockVisit.setVisitId(UUID.randomUUID());
        when(visitRepo.findByAppointment_AppointmentId(any())).thenReturn(Optional.of(mockVisit));
        
        org.example.doansummer2026.model.Invoice mockInvoice = new org.example.doansummer2026.model.Invoice();
        when(invoiceRepository.findAllByVisit_VisitId(any())).thenReturn(List.of(mockInvoice));
        when(invoiceRepository.saveAll(any())).thenReturn(List.of(mockInvoice));

        ReflectionTestUtils.invokeMethod(appointmentService, "linkGuestHistoryBeforeContactChange",
                profile, "   ", "old@test.com");
                
        verify(invoiceRepository).saveAll(any());
    }

    @Test
    void checkIn_WithNullDobAndGender_ShouldFallback() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        // Use guest appointment to trigger a.getCustomer() == null
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, null);
        a.setIsGuest(true);
        a.setGuestGender(Gender.OTHER);
        a.setGuestPhone("0900000000");
        a.setGuestFullName("Guest");

        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, Set.of(UUID.randomUUID()), staffId, "Guest", "0900000000", null, null, null, null, null);
        
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));
        
        Profile guestProfile = customer(UUID.randomUUID());
        guestProfile.setDateOfBirth(null);
        guestProfile.setGender(null);
        when(profileRepo.findFirstByPhoneIn(any())).thenReturn(Optional.of(guestProfile));
        when(profileRepo.findByIdForUpdate(guestProfile.getProfileId())).thenReturn(Optional.of(guestProfile));
        
        org.example.doansummer2026.service.MedicalServiceSelectionPolicyService mockPolicy = mock(org.example.doansummer2026.service.MedicalServiceSelectionPolicyService.class);
        ReflectionTestUtils.setField(appointmentService, "serviceSelectionPolicyService", mockPolicy);
        MedicalService svc = service(UUID.randomUUID(), "Test");
        when(mockPolicy.normalizeOrThrow(any())).thenReturn(List.of(svc));
        
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        when(visitRepo.findByAppointment_AppointmentId(any())).thenReturn(Optional.of(visit));
        when(invoiceService.create(any())).thenReturn(mock(InvoiceResponse.class));
        
        assertDoesNotThrow(() -> appointmentService.checkIn(req));
    }

    @Test
    void checkIn_WithCustomerNoDob_ShouldCoverBranch() {
        UUID appointmentId = UUID.randomUUID();
        Profile customer = customer(UUID.randomUUID());
        customer.setDateOfBirth(null);
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, customer);

        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, Set.of(UUID.randomUUID()), UUID.randomUUID(), "Patient", "0900000000", null, null, null, null, Gender.MALE);
        
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(any())).thenReturn(Optional.of(mock(StaffInfo.class)));
        
        org.example.doansummer2026.service.MedicalServiceSelectionPolicyService mockPolicy = mock(org.example.doansummer2026.service.MedicalServiceSelectionPolicyService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(appointmentService, "serviceSelectionPolicyService", mockPolicy);
        MedicalService svc = service(UUID.randomUUID(), "Test");
        when(mockPolicy.normalizeOrThrow(any())).thenReturn(List.of(svc));
        
        when(profileRepo.findByIdForUpdate(any())).thenReturn(Optional.of(customer));
        
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        when(visitRepo.findByAppointment_AppointmentId(any())).thenReturn(Optional.of(visit));
        when(invoiceService.create(any())).thenReturn(mock(InvoiceResponse.class));
        
        assertDoesNotThrow(() -> appointmentService.checkIn(req));
    }

    @Test
    void checkIn_WithNullServices_ShouldThrow() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, null);
        a.setServices(null); // Force null

        AppointmentCheckInRequest req = new AppointmentCheckInRequest(
                appointmentId, null, staffId, "Patient", "0900000000", null, null, null, null, Gender.MALE);
        
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(staffRepo.findById(staffId)).thenReturn(Optional.of(mock(StaffInfo.class)));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> appointmentService.checkIn(req));
        assertTrue(ex.getMessage().contains("Lịch hẹn chưa chọn dịch vụ"));
    }

    @Test
    void phoneVariants_WithDifferentFormats_ShouldCoverBranches() {
        Appointment a = new Appointment();
        // Trigger phoneVariants with short phone and non-0 phone
        ReflectionTestUtils.invokeMethod(appointmentService, "resolveExistingPatientProfile", a, "0912", null);
        ReflectionTestUtils.invokeMethod(appointmentService, "resolveExistingPatientProfile", a, "19001234", null);
        ReflectionTestUtils.invokeMethod(appointmentService, "resolveExistingPatientProfile", a, "84123456789", null);
        ReflectionTestUtils.invokeMethod(appointmentService, "resolveExistingPatientProfile", a, "841234567", null);
        ReflectionTestUtils.invokeMethod(appointmentService, "resolveExistingPatientProfile", a, "0", null);
    }

    @Test
    void getMyAppointmentDetail_WithRelationship_ShouldIncludeDisplayName() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Profile customer = customer(customerId);
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, customer);
        
        when(repo.findById(appointmentId)).thenReturn(Optional.of(a));
        when(familyAccessService.resolveReadableProfile(customerId, customerId)).thenReturn(customer);
        when(familyAccessService.ownerProfile(customerId)).thenReturn(customer);
        
        when(familyAccessService.relationship(customerId, customerId)).thenReturn(org.example.doansummer2026.enums.FamilyRelationship.PARENT);
        
        var result = appointmentService.getMyAppointmentDetail(customerId, appointmentId);
        assertNotNull(result);
        assertEquals(org.example.doansummer2026.enums.FamilyRelationship.PARENT.getDisplayName(), result.relationship());
    }

    @Test
    void updateMyAppointment_WithAllNull_ShouldNotRefresh() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Profile customer = customer(customerId);
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, customer);
        
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(familyAccessService.resolveActiveProfile(customerId, customerId)).thenReturn(customer);
        when(familyAccessService.ownerProfile(customerId)).thenReturn(customer);
        when(repo.save(a)).thenReturn(a);
        
        AppointmentUpdateRequest req = new AppointmentUpdateRequest(null, null, null, null, null, null, null, null, null, null, null, null);
        assertDoesNotThrow(() -> appointmentService.updateMyAppointment(customerId, appointmentId, req));
    }
    
    @Test
    void updateMyAppointment_WithServicesButNoScheduledAt_ShouldRefresh() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Profile customer = customer(customerId);
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, customer);
        a.setScheduledAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(2));
        
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(familyAccessService.resolveActiveProfile(customerId, customerId)).thenReturn(customer);
        when(familyAccessService.ownerProfile(customerId)).thenReturn(customer);
        when(repo.save(a)).thenReturn(a);
        
        org.example.doansummer2026.service.MedicalServiceSelectionPolicyService mockPolicy = mock(org.example.doansummer2026.service.MedicalServiceSelectionPolicyService.class);
        ReflectionTestUtils.setField(appointmentService, "serviceSelectionPolicyService", mockPolicy);
        when(mockPolicy.normalizeOrThrow(any())).thenReturn(List.of(service(UUID.randomUUID(), "Test")));
        
        AppointmentUpdateRequest req = new AppointmentUpdateRequest(null, null, null, Set.of(UUID.randomUUID()), null, null, null, null, null, null, null, null);
        assertDoesNotThrow(() -> appointmentService.updateMyAppointment(customerId, appointmentId, req));
    }

    @Test
    void updateMyAppointment_WithShiftId_ShouldRefresh() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Profile customer = customer(customerId);
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, customer);
        a.setScheduledAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(2));
        
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(familyAccessService.resolveActiveProfile(customerId, customerId)).thenReturn(customer);
        when(familyAccessService.ownerProfile(customerId)).thenReturn(customer);
        when(repo.save(a)).thenReturn(a);
        
        UUID shiftId = UUID.randomUUID();
        org.example.doansummer2026.model.ShiftConfig shift = new org.example.doansummer2026.model.ShiftConfig();
        shift.setName(org.example.doansummer2026.service.ShiftConfigService.MORNING);
        when(shiftConfigRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        
        java.time.LocalTime now = java.time.LocalTime.now();
        org.example.doansummer2026.service.ShiftScheduleResolver.ResolvedShift mockResolved = new org.example.doansummer2026.service.ShiftScheduleResolver.ResolvedShift(shift, null, now.plusHours(1), now.plusHours(2), null, null);
        org.example.doansummer2026.service.ShiftScheduleResolver mockResolver = mock(org.example.doansummer2026.service.ShiftScheduleResolver.class);
        org.springframework.test.util.ReflectionTestUtils.setField(appointmentService, "shiftScheduleResolver", mockResolver);
        when(mockResolver.resolve(any(), any())).thenReturn(mockResolved);
        
        AppointmentUpdateRequest req = new AppointmentUpdateRequest(null, null, null, null, shiftId, null, null, null, null, null, null, null);
        assertDoesNotThrow(() -> appointmentService.updateMyAppointment(customerId, appointmentId, req));
    }

    @Test
    void updateMyAppointment_WithServicesAndScheduledAt_ShouldRefresh() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Profile customer = customer(customerId);
        customer.setDateOfBirth(LocalDate.of(2000, 1, 1));
        Appointment a = appointmentForToday(appointmentId, AppointmentStatus.PENDING, customer);
        
        when(repo.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(a));
        when(familyAccessService.resolveActiveProfile(customerId, customerId)).thenReturn(customer);
        when(familyAccessService.ownerProfile(customerId)).thenReturn(customer);
        when(repo.save(a)).thenReturn(a);
        
        org.example.doansummer2026.service.MedicalServiceSelectionPolicyService mockPolicy = mock(org.example.doansummer2026.service.MedicalServiceSelectionPolicyService.class);
        ReflectionTestUtils.setField(appointmentService, "serviceSelectionPolicyService", mockPolicy);
        when(mockPolicy.normalizeOrThrow(any())).thenReturn(List.of(service(UUID.randomUUID(), "Test")));
        
        AppointmentUpdateRequest req = new AppointmentUpdateRequest(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(2), null, null, Set.of(UUID.randomUUID()), null, null, null, null, null, null, null, null);
        assertDoesNotThrow(() -> appointmentService.updateMyAppointment(customerId, appointmentId, req));
    }

    @Test
    void validateExaminationAvailabilityForShift_WithNullDepartmentType() {
        MedicalService svc1 = service(UUID.randomUUID(), "Test");
        svc1.setDepartmentType(null); // Cover branch
        
        MedicalService svc2 = service(UUID.randomUUID(), "Test 2");
        svc2.setDepartmentType(org.example.doansummer2026.enums.DepartmentType.PARACLINICAL); // Not EXAMINATION
        
        MedicalService svc3 = service(UUID.randomUUID(), "Test 3");
        svc3.setDepartmentType(org.example.doansummer2026.enums.DepartmentType.EXAMINATION);
        
        org.example.doansummer2026.model.ShiftConfig shift = new org.example.doansummer2026.model.ShiftConfig();
        
        org.example.doansummer2026.service.ServiceAvailabilityService mockAvailability = mock(org.example.doansummer2026.service.ServiceAvailabilityService.class);
        when(mockAvailability.evaluate(any(), any(), any(), anyBoolean())).thenReturn(new org.example.doansummer2026.service.ServiceAvailabilityService.Evaluation(true, null, List.of()));
        ReflectionTestUtils.setField(appointmentService, "serviceAvailabilityService", mockAvailability);
        
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(appointmentService, 
                "validateExaminationAvailabilityForShift", Set.of(svc1, svc2, svc3), shift, LocalDate.now()));
    }

    @Test
    void resolveCurrentShift_WithOutdatedAndFutureShifts() {
        org.example.doansummer2026.model.ShiftConfig shift = new org.example.doansummer2026.model.ShiftConfig();
        when(shiftConfigRepository.findAllByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(shift, shift, shift));
        
        java.time.LocalTime now = java.time.LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        
        var futureRes = new org.example.doansummer2026.service.ShiftScheduleResolver.ResolvedShift(
                shift, null, now.plusHours(1), now.plusHours(2), null, null);
        var pastRes = new org.example.doansummer2026.service.ShiftScheduleResolver.ResolvedShift(
                shift, null, now.minusHours(2), now.minusHours(1), null, null);
        var currentRes = new org.example.doansummer2026.service.ShiftScheduleResolver.ResolvedShift(
                shift, null, now.minusHours(1), now.plusHours(1), null, null);
                
        org.example.doansummer2026.service.ShiftScheduleResolver mockResolver = mock(org.example.doansummer2026.service.ShiftScheduleResolver.class);
        ReflectionTestUtils.setField(appointmentService, "shiftScheduleResolver", mockResolver);
        
        when(mockResolver.resolve(any(), any())).thenReturn(futureRes, pastRes, currentRes);
        
        Object result = ReflectionTestUtils.invokeMethod(appointmentService, "resolveCurrentShift");
        assertNotNull(result);
    }
}
