package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.dto.customervisit.CustomerVisitCreateRequest;
import vn.edu.fpt.cares.dto.customervisit.CustomerVisitUpdateRequest;
import vn.edu.fpt.cares.dto.invoice.InvoiceResponse;
import vn.edu.fpt.cares.enums.AppointmentStatus;
import vn.edu.fpt.cares.enums.AllergyStatus;
import vn.edu.fpt.cares.enums.BloodType;
import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.enums.Gender;
import vn.edu.fpt.cares.enums.ServiceStatus;
import vn.edu.fpt.cares.enums.VisitStatus;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.Appointment;
import vn.edu.fpt.cares.model.Account;
import vn.edu.fpt.cares.model.CustomerVisit;
import vn.edu.fpt.cares.model.Invoice;
import vn.edu.fpt.cares.model.MedicalService;
import vn.edu.fpt.cares.model.Profile;
import vn.edu.fpt.cares.model.StaffInfo;
import vn.edu.fpt.cares.repository.AppointmentRepository;
import vn.edu.fpt.cares.repository.CustomerVisitRepository;
import vn.edu.fpt.cares.repository.InsuranceRuleRepository;
import vn.edu.fpt.cares.repository.InvoiceRepository;
import vn.edu.fpt.cares.repository.MedicalServiceRepository;
import vn.edu.fpt.cares.repository.ProfileRepository;
import vn.edu.fpt.cares.repository.StaffInfoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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

    @Mock private vn.edu.fpt.cares.repository.InvoiceItemRepository invoiceItemRepo;
    @Mock private vn.edu.fpt.cares.repository.QueueTicketRepository queueTicketRepo;
    @Mock private vn.edu.fpt.cares.repository.ShiftConfigRepository shiftConfigRepository;
    @Mock private ShiftScheduleResolver shiftScheduleResolver;
    @Mock private ServiceAvailabilityService serviceAvailabilityService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private CustomerVisitService customerVisitService;


    // =========================================================
    // HELPERS
    // =========================================================

    private void availableExaminationShift() {
        var shift = vn.edu.fpt.cares.model.ShiftConfig.builder().shiftId(UUID.randomUUID()).build();
        when(shiftConfigRepository.findAllByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(shift));
        when(shiftScheduleResolver.resolve(eq(shift), any())).thenReturn(
                new ShiftScheduleResolver.ResolvedShift(shift, null, java.time.LocalTime.MIN,
                        java.time.LocalTime.MAX, vn.edu.fpt.cares.enums.ShiftTimeSource.NORMAL, null));
        when(serviceAvailabilityService.evaluate(any(), any(), eq(shift), eq(false)))
                .thenReturn(new ServiceAvailabilityService.Evaluation(true, null, List.of()));
    }

    private LocalDate clinicToday() {
        return LocalDate.now(CLINIC_ZONE);
    }

    private CustomerVisitCreateRequest intakeRequest(String name, String phone, String email,
                                                     LocalDate dateOfBirth, Gender gender,
                                                     String address, AllergyStatus allergyStatus,
                                                     List<String> allergies) {
        return new CustomerVisitCreateRequest(null, null, List.of(), null, name, phone, address,
                dateOfBirth, gender, email, BloodType.O_POSITIVE, allergyStatus, allergies, true, null);
    }

    private void applyIntake(Profile profile, CustomerVisitCreateRequest request) {
        ReflectionTestUtils.invokeMethod(customerVisitService, "applyPatientIntake", profile, request);
    }

    @Test
    void guestInformationValidationCoversIdentityContactAndGenderBranches() {
        CustomerVisitCreateRequest valid = intakeRequest("Nguyễn Thị Ánh", null, "anh@example.com",
                null, Gender.FEMALE, null, AllergyStatus.NONE_REPORTED, List.of());
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateGuestInformation", valid, null));

        for (CustomerVisitCreateRequest invalid : List.of(
                intakeRequest("A", "0909123456", null, null, Gender.FEMALE, null, null, List.of()),
                intakeRequest("Tên 123", "0909123456", null, null, Gender.FEMALE, null, null, List.of()),
                intakeRequest("Nguyễn An", null, null, null, Gender.FEMALE, null, null, List.of()),
                intakeRequest("Nguyễn An", "0909123456", null, null, null, null, null, List.of()),
                intakeRequest("Nguyễn An", "0909123456", null, null, Gender.OTHER, null, null, List.of())
        )) assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateGuestInformation", invalid, invalid.guestPhone()));

        CustomerVisitCreateRequest badPhone = intakeRequest("Nguyễn An", "123", null,
                null, Gender.MALE, null, null, List.of());
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateGuestInformation", badPhone, "123"));
        CustomerVisitCreateRequest birthDateFallback = intakeRequest("Nguyễn An", null, null,
                clinicToday().minusYears(30), Gender.MALE, null, null, List.of());
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateGuestInformation", birthDateFallback, " "));
    }

    @Test
    void appointmentCheckInValidationCoversStatusDuplicateDateOwnershipAndGuestPhone() {
        UUID appointmentId = UUID.randomUUID();
        Profile selected = profile(UUID.randomUUID());
        selected.setPhone("0909123456");
        Appointment appointment = Appointment.builder().appointmentId(appointmentId)
                .status(AppointmentStatus.CANCELLED).scheduledAt(clinicToday().atTime(8, 0)).build();
        assertThrows(ConflictException.class, () -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateAppointmentForCheckIn", appointment, selected));

        appointment.setStatus(AppointmentStatus.PENDING);
        when(repo.findByAppointment_AppointmentId(appointmentId))
                .thenReturn(Optional.of(CustomerVisit.builder().build()), Optional.empty());
        assertThrows(ConflictException.class, () -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateAppointmentForCheckIn", appointment, selected));

        appointment.setScheduledAt(null);
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateAppointmentForCheckIn", appointment, selected));
        appointment.setScheduledAt(clinicToday().minusDays(1).atTime(8, 0));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateAppointmentForCheckIn", appointment, selected));

        appointment.setScheduledAt(clinicToday().atTime(8, 0));
        appointment.setCustomer(profile(UUID.randomUUID()));
        assertThrows(ConflictException.class, () -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateAppointmentForCheckIn", appointment, selected));
        appointment.setCustomer(selected);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateAppointmentForCheckIn", appointment, selected));

        appointment.setCustomer(null);
        appointment.setGuestPhone("0911111111");
        assertThrows(ConflictException.class, () -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateAppointmentForCheckIn", appointment, selected));
        appointment.setGuestPhone("+84 909 123 456");
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateAppointmentForCheckIn", appointment, selected));
        appointment.setGuestPhone(null);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                customerVisitService, "validateAppointmentForCheckIn", appointment, selected));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"A", "Tên 123"})
    void patientIntakeRejectsInvalidNames(String name) {
        Profile patient = profile(UUID.randomUUID());
        CustomerVisitCreateRequest request = intakeRequest(name, "0909123456", "patient@example.com",
                clinicToday().minusYears(20), Gender.FEMALE, "Hà Nội", AllergyStatus.NONE_REPORTED, List.of());
        assertThrows(BadRequestException.class, () -> applyIntake(patient, request));
        verify(profileRepo, never()).save(any());
    }

    @Test
    void patientIntakeRejectsNameOverOneHundredCharacters() {
        Profile patient = profile(UUID.randomUUID());
        CustomerVisitCreateRequest request = intakeRequest("A".repeat(101), "0909123456", null,
                clinicToday().minusYears(20), Gender.MALE, null, AllergyStatus.NONE_REPORTED, List.of());
        assertThrows(BadRequestException.class, () -> applyIntake(patient, request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "84909123456", "+8412", "0909ABCDEF"})
    void patientIntakeRejectsInvalidPhoneNumbers(String phone) {
        Profile patient = profile(UUID.randomUUID());
        CustomerVisitCreateRequest request = intakeRequest("Nguyễn Thị Ánh", phone, null,
                clinicToday().minusYears(20), Gender.FEMALE, null, AllergyStatus.NONE_REPORTED, List.of());
        assertThrows(BadRequestException.class, () -> applyIntake(patient, request));
    }

    @Test
    void patientIntakeRejectsPhoneOwnedByAnotherProfileButAcceptsOwnPhone() {
        Profile patient = profile(UUID.randomUUID());
        Profile other = profile(UUID.randomUUID());
        CustomerVisitCreateRequest request = intakeRequest("Nguyễn Thị Ánh", "+84 909.123.456", null,
                clinicToday().minusYears(20), Gender.FEMALE, null, AllergyStatus.NONE_REPORTED, List.of());
        when(profileRepo.findFirstByPhoneIn(List.of("0909123456", "+84909123456")))
                .thenReturn(Optional.of(other));
        assertThrows(ConflictException.class, () -> applyIntake(patient, request));

        when(profileRepo.findFirstByPhoneIn(List.of("0909123456", "+84909123456")))
                .thenReturn(Optional.of(patient));
        applyIntake(patient, request);
        assertEquals("0909123456", patient.getPhone());
    }

    @Test
    void patientIntakeRequiresContactForAccountProfileAndRejectsDuplicateEmail() {
        Profile patient = profile(UUID.randomUUID());
        patient.setAccount(Account.builder().accountId(UUID.randomUUID()).build());
        CustomerVisitCreateRequest noContact = intakeRequest("Nguyễn Thị Ánh", " ", " ",
                clinicToday().minusYears(20), Gender.FEMALE, null, AllergyStatus.NONE_REPORTED, List.of());
        assertThrows(BadRequestException.class, () -> applyIntake(patient, noContact));

        CustomerVisitCreateRequest duplicateEmail = intakeRequest("Nguyễn Thị Ánh", null, " USED@EXAMPLE.COM ",
                clinicToday().minusYears(20), Gender.FEMALE, null, AllergyStatus.NONE_REPORTED, List.of());
        when(profileRepo.findFirstByEmailIgnoreCase("used@example.com"))
                .thenReturn(Optional.of(profile(UUID.randomUUID())));
        assertThrows(ConflictException.class, () -> applyIntake(patient, duplicateEmail));
    }

    @Test
    void patientIntakeAcceptsOwnEmailAndNormalizesOptionalValues() {
        Profile patient = profile(UUID.randomUUID());
        CustomerVisitCreateRequest request = intakeRequest("  Nguyễn   Thị Ánh  ", " ", " ANH@EXAMPLE.COM ",
                clinicToday().minusYears(20), Gender.FEMALE, "  Hà Nội  ", AllergyStatus.REPORTED,
                List.of(" Penicillin ", "penicillin", "Hải sản"));
        when(profileRepo.findFirstByEmailIgnoreCase("anh@example.com")).thenReturn(Optional.of(patient));
        applyIntake(patient, request);
        assertEquals("Nguyễn Thị Ánh", patient.getFullName());
        assertNull(patient.getPhone());
        assertEquals("anh@example.com", patient.getEmail());
        assertEquals("Hà Nội", patient.getAddress());
        assertEquals("Penicillin\nHải sản", patient.getAllergies());
        verify(profileRepo).save(patient);
    }

    @Test
    void patientIntakeValidatesBirthDateGenderAndContactFallback() {
        Profile patient = profile(UUID.randomUUID());
        assertThrows(BadRequestException.class, () -> applyIntake(patient,
                intakeRequest("Nguyễn Thị Ánh", null, null, clinicToday(), Gender.FEMALE,
                        null, AllergyStatus.NONE_REPORTED, List.of())));
        assertThrows(BadRequestException.class, () -> applyIntake(patient,
                intakeRequest("Nguyễn Thị Ánh", null, null, null, Gender.FEMALE,
                        null, AllergyStatus.NONE_REPORTED, List.of())));
        assertThrows(BadRequestException.class, () -> applyIntake(patient,
                intakeRequest("Nguyễn Thị Ánh", "0909123456", null, clinicToday().minusYears(20), null,
                        null, AllergyStatus.NONE_REPORTED, List.of())));
        assertThrows(BadRequestException.class, () -> applyIntake(patient,
                intakeRequest("Nguyễn Thị Ánh", "0909123456", null, clinicToday().minusYears(20), Gender.OTHER,
                        null, AllergyStatus.NONE_REPORTED, List.of())));
    }

    @Test
    void patientIntakeValidatesAllergyCollectionAndStoresEveryStatus() {
        Profile patient = profile(UUID.randomUUID());
        List<String> tooMany = java.util.stream.IntStream.range(0, 21).mapToObj(i -> "Dị ứng " + i).toList();
        assertThrows(BadRequestException.class, () -> applyIntake(patient,
                intakeRequest("Nguyễn Thị Ánh", "0909123456", null, clinicToday().minusYears(20), Gender.FEMALE,
                        null, AllergyStatus.REPORTED, tooMany)));
        assertThrows(BadRequestException.class, () -> applyIntake(patient,
                intakeRequest("Nguyễn Thị Ánh", "0909123456", null, clinicToday().minusYears(20), Gender.FEMALE,
                        null, AllergyStatus.REPORTED, List.of("A".repeat(101)))));
        assertThrows(BadRequestException.class, () -> applyIntake(patient,
                intakeRequest("Nguyễn Thị Ánh", "0909123456", null, clinicToday().minusYears(20), Gender.FEMALE,
                        null, AllergyStatus.REPORTED, List.of())));

        applyIntake(patient, intakeRequest("Nguyễn Thị Ánh", "0909123456", null,
                clinicToday().minusYears(20), Gender.FEMALE, " ", AllergyStatus.UNVERIFIED, List.of()));
        assertNull(patient.getAllergies());
        assertNull(patient.getAddress());
        applyIntake(patient, intakeRequest("Nguyễn Thị Ánh", "0909123456", null,
                clinicToday().minusYears(20), Gender.FEMALE, null, AllergyStatus.NONE_REPORTED, List.of()));
        assertEquals("", patient.getAllergies());
        applyIntake(patient, intakeRequest("Nguyễn Thị Ánh", "0909123456", null,
                clinicToday().minusYears(20), Gender.FEMALE, null, null, List.of()));
        assertEquals("", patient.getAllergies());
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
    void create_ShouldNormalizeDuplicateServicesBeforeResolvingPatient() {
        UUID serviceId = UUID.randomUUID();
        UUID missingPatientId = UUID.randomUUID();
        MedicalService selected = medicalService(serviceId, "Khám Nội", "EX-1",
                new BigDecimal("200000"), DepartmentType.EXAMINATION);
        CustomerVisitCreateRequest req = mock(CustomerVisitCreateRequest.class);
        when(req.serviceIds()).thenReturn(List.of(serviceId, serviceId));
        when(req.customerId()).thenReturn(missingPatientId);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(selected));

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> customerVisitService.create(req));

        assertTrue(error.getMessage().contains("Khách hàng không tồn tại"));
        verify(serviceRepo).findById(serviceId);
        verify(profileRepo).findById(missingPatientId);
        verifyNoInteractions(invoiceService);
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
        availableExaminationShift();

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
        when(req.updatePatientProfile()).thenReturn(true);

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
                profileRepo.findFirstByPhoneIn(List.of("0901234567", "+84901234567"))
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
        availableExaminationShift();

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
                profileRepo.findFirstByPhoneIn(List.of("0909999999", "+84909999999"))
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
    void create_ShouldReject_WhenExaminationAlreadyRegisteredToday() {

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

        when(invoiceItemRepo.findSameDayExaminationRegistrations(eq(profileId), any(), any(), eq(null)))
                .thenReturn(List.of(vn.edu.fpt.cares.model.InvoiceItem.builder()
                        .service(service).invoice(Invoice.builder().visit(active).build()).build()));

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
        availableExaminationShift();

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
        availableExaminationShift();

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

    @Test
    void queueStatusLabel_ShouldCoverEveryStatusAndNull() {
        assertEquals("Đã đăng ký", ReflectionTestUtils.invokeMethod(customerVisitService, "queueStatusLabel", (Object) null));
        assertEquals("Chưa đến lượt", ReflectionTestUtils.invokeMethod(customerVisitService, "queueStatusLabel", vn.edu.fpt.cares.enums.QueueStatus.BLOCKED));
        assertEquals("Đang chờ gọi", ReflectionTestUtils.invokeMethod(customerVisitService, "queueStatusLabel", vn.edu.fpt.cares.enums.QueueStatus.WAITING));
        assertEquals("Đã gọi", ReflectionTestUtils.invokeMethod(customerVisitService, "queueStatusLabel", vn.edu.fpt.cares.enums.QueueStatus.CALLED));
        assertEquals("Đang khám", ReflectionTestUtils.invokeMethod(customerVisitService, "queueStatusLabel", vn.edu.fpt.cares.enums.QueueStatus.IN_PROGRESS));
        assertEquals("Chờ cận lâm sàng", ReflectionTestUtils.invokeMethod(customerVisitService, "queueStatusLabel", vn.edu.fpt.cares.enums.QueueStatus.WAITING_FOR_TEST));
        assertEquals("Chờ quay lại bác sĩ", ReflectionTestUtils.invokeMethod(customerVisitService, "queueStatusLabel", vn.edu.fpt.cares.enums.QueueStatus.TEST_DONE));
        assertEquals("Đã hoàn thành", ReflectionTestUtils.invokeMethod(customerVisitService, "queueStatusLabel", vn.edu.fpt.cares.enums.QueueStatus.DONE));
        assertEquals("Vắng mặt", ReflectionTestUtils.invokeMethod(customerVisitService, "queueStatusLabel", vn.edu.fpt.cares.enums.QueueStatus.SKIPPED));
    }

    @Test
    void normalizationHelpers_ShouldCoverNullBlankInternationalAndLocalValues() {
        assertNull(ReflectionTestUtils.invokeMethod(customerVisitService, "normalizeVietnamesePhone", (Object) null));
        assertNull(ReflectionTestUtils.invokeMethod(customerVisitService, "normalizeVietnamesePhone", "  -  "));
        assertEquals("0912345678", ReflectionTestUtils.invokeMethod(customerVisitService, "normalizeVietnamesePhone", "+84 912.345.678"));
        assertEquals("0912345678", ReflectionTestUtils.invokeMethod(customerVisitService, "normalizeVietnamesePhone", "0912-345-678"));
        assertNull(ReflectionTestUtils.invokeMethod(customerVisitService, "normalizeEmail", (Object) null));
        assertNull(ReflectionTestUtils.invokeMethod(customerVisitService, "normalizeEmail", "  "));
        assertEquals("patient@example.com", ReflectionTestUtils.invokeMethod(customerVisitService, "normalizeEmail", " Patient@Example.COM "));
        assertEquals("", ReflectionTestUtils.invokeMethod(customerVisitService, "normalizePhone", (Object) null));
        assertEquals("0912345678", ReflectionTestUtils.invokeMethod(customerVisitService, "normalizePhone", "+84 912-345-678"));
        assertEquals("0912345678", ReflectionTestUtils.invokeMethod(customerVisitService, "normalizePhone", "0912.345.678"));

        assertEquals(List.of(), ReflectionTestUtils.invokeMethod(customerVisitService, "phoneVariants", (Object) null));
        assertEquals(List.of(), ReflectionTestUtils.invokeMethod(customerVisitService, "phoneVariants", "  "));
        assertEquals(List.of("0912345678", "+84912345678"),
                ReflectionTestUtils.invokeMethod(customerVisitService, "phoneVariants", "0912345678"));
        assertEquals(List.of("12345"), ReflectionTestUtils.invokeMethod(customerVisitService, "phoneVariants", "12345"));
    }

    @Test
    void findExistingGuestProfile_ShouldPreferPhoneOrEmailAndRejectDifferentOwners() {
        Profile phoneOwner = Profile.builder().profileId(UUID.randomUUID()).build();
        Profile emailOwner = Profile.builder().profileId(UUID.randomUUID()).build();
        when(profileRepo.findFirstByPhoneIn(anyList())).thenReturn(Optional.empty());
        when(profileRepo.findFirstByEmailIgnoreCase("a@b.vn")).thenReturn(Optional.empty());
        assertNull(ReflectionTestUtils.invokeMethod(customerVisitService,
                "findExistingGuestProfile", "0900000000", "a@b.vn"));

        when(profileRepo.findFirstByPhoneIn(anyList())).thenReturn(Optional.of(phoneOwner));
        assertSame(phoneOwner, ReflectionTestUtils.invokeMethod(customerVisitService,
                "findExistingGuestProfile", "0900000000", null));
        when(profileRepo.findFirstByPhoneIn(anyList())).thenReturn(Optional.empty());
        when(profileRepo.findFirstByEmailIgnoreCase("a@b.vn")).thenReturn(Optional.of(emailOwner));
        assertSame(emailOwner, ReflectionTestUtils.invokeMethod(customerVisitService,
                "findExistingGuestProfile", null, "a@b.vn"));

        when(profileRepo.findFirstByPhoneIn(anyList())).thenReturn(Optional.of(phoneOwner));
        when(profileRepo.findFirstByEmailIgnoreCase("a@b.vn")).thenReturn(Optional.of(phoneOwner));
        assertSame(phoneOwner, ReflectionTestUtils.invokeMethod(customerVisitService,
                "findExistingGuestProfile", "0900000000", "a@b.vn"));
        when(profileRepo.findFirstByEmailIgnoreCase("a@b.vn")).thenReturn(Optional.of(emailOwner));
        assertThrows(ConflictException.class, () -> ReflectionTestUtils.invokeMethod(customerVisitService,
                "findExistingGuestProfile", "0900000000", "a@b.vn"));
    }

    @Test
    void hasActiveExaminationInvoice_ShouldCoverMissingPartsAndMembership() {
        var empty = vn.edu.fpt.cares.model.QueueTicket.builder().build();
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(customerVisitService,
                "hasActiveExaminationInvoice", empty)));
        var visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        empty.setVisit(visit);
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(customerVisitService,
                "hasActiveExaminationInvoice", empty)));
        MedicalService service = MedicalService.builder().serviceId(UUID.randomUUID()).build();
        empty.setService(service);
        when(invoiceItemRepo.findDistinctExaminationServiceIdsByVisit(visit.getVisitId(), null))
                .thenReturn(List.of());
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(customerVisitService,
                "hasActiveExaminationInvoice", empty)));
        when(invoiceItemRepo.findDistinctExaminationServiceIdsByVisit(visit.getVisitId(), null))
                .thenReturn(List.of(service.getServiceId()));
        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(customerVisitService,
                "hasActiveExaminationInvoice", empty)));
    }

    @Test
    void validateServiceEligibility_ShouldCoverStatusAgeAndGenderBoundaries() {
        Profile adultFemale = Profile.builder().dateOfBirth(LocalDate.now(CLINIC_ZONE).minusYears(30))
                .gender(Gender.FEMALE).build();
        MedicalService service = MedicalService.builder().name("Khám chuyên khoa")
                .status(ServiceStatus.INACTIVE).build();
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(customerVisitService,
                "validateServiceEligibility", service, adultFemale));

        service.setStatus(ServiceStatus.ACTIVE);
        service.setMinimumAge(18);
        Profile noDob = Profile.builder().gender(Gender.FEMALE).build();
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(customerVisitService,
                "validateServiceEligibility", service, noDob));
        service.setMinimumAge(31);
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(customerVisitService,
                "validateServiceEligibility", service, adultFemale));
        service.setMinimumAge(18);
        service.setMaximumAge(29);
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(customerVisitService,
                "validateServiceEligibility", service, adultFemale));
        service.setMaximumAge(60);
        service.setAllowedGender(Gender.MALE);
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(customerVisitService,
                "validateServiceEligibility", service, adultFemale));
        service.setAllowedGender(Gender.FEMALE);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(customerVisitService,
                "validateServiceEligibility", service, adultFemale));
        service.setAllowedGender(null);
        service.setMinimumAge(null);
        service.setMaximumAge(null);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(customerVisitService,
                "validateServiceEligibility", service, noDob));
    }

    @Test
    void scheduleProfileAuditWritesImmediatelyAndCarriesActorAccountWhenAvailable() {
        UUID profileId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        StaffInfo actor = StaffInfo.builder().staffId(UUID.randomUUID()).profile(Profile.builder()
                .account(Account.builder().accountId(accountId).build()).build()).build();

        ReflectionTestUtils.invokeMethod(customerVisitService, "scheduleProfileAudit",
                profileId, actor, "old", "new");
        var captor = org.mockito.ArgumentCaptor.forClass(
                vn.edu.fpt.cares.dto.auditlog.AuditLogCreateRequest.class);
        verify(auditLogService).create(captor.capture());
        assertEquals(profileId.toString(), captor.getValue().entityId());
        assertEquals(accountId, captor.getValue().actorAccountId());

        clearInvocations(auditLogService);
        ReflectionTestUtils.invokeMethod(customerVisitService, "scheduleProfileAudit",
                profileId, null, null, null);
        verify(auditLogService).create(any());
    }

    @Test
    void scheduleProfileAuditDefersWriteUntilTransactionCommit() {
        UUID profileId = UUID.randomUUID();
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            ReflectionTestUtils.invokeMethod(customerVisitService, "scheduleProfileAudit",
                    profileId, StaffInfo.builder().build(), "before", "after");
            verifyNoInteractions(auditLogService);
            var synchronizations = org.springframework.transaction.support.TransactionSynchronizationManager
                    .getSynchronizations();
            assertEquals(1, synchronizations.size());
            synchronizations.get(0).afterCommit();
            verify(auditLogService).create(any());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sameDayExaminationServicesCombineInvoicesAndQueuesWithoutDuplicatingService() {
        UUID customerId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        MedicalService service = MedicalService.builder().serviceId(serviceId).serviceCode("EX-1")
                .name("Khám Nội").departmentType(DepartmentType.EXAMINATION).build();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).status(VisitStatus.IN_PROGRESS)
                .checkInTime(LocalDateTime.now()).build();
        Invoice pending = Invoice.builder().invoiceId(UUID.randomUUID()).visit(visit)
                .status(vn.edu.fpt.cares.enums.InvoiceStatus.PENDING).build();
        var item = vn.edu.fpt.cares.model.InvoiceItem.builder()
                .invoice(pending).service(service).build();
        when(profileRepo.existsById(customerId)).thenReturn(true);
        when(invoiceItemRepo.findSameDayExaminationRegistrations(eq(customerId), any(), any(), isNull()))
                .thenReturn(List.of(item, item));
        when(queueTicketRepo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(visitId, serviceId))
                .thenReturn(Optional.empty());

        var invoiceOnly = customerVisitService.getSameDayExaminationServices(customerId);
        assertEquals(1, invoiceOnly.size());
        assertTrue(invoiceOnly.get(0).reason().contains("Chờ thanh toán"));

        var queue = vn.edu.fpt.cares.model.QueueTicket.builder().ticketId(UUID.randomUUID())
                .visit(visit).service(service).status(vn.edu.fpt.cares.enums.QueueStatus.CALLED).build();
        when(queueTicketRepo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(visitId, serviceId))
                .thenReturn(Optional.of(queue));
        assertTrue(customerVisitService.getSameDayExaminationServices(customerId).get(0).reason()
                .contains("Đã gọi"));
    }

    @Test
    void sameDayExaminationServicesRejectUnknownCustomerAndIncludeQueueFallback() {
        UUID customerId = UUID.randomUUID();
        when(profileRepo.existsById(customerId)).thenReturn(false, true);
        assertThrows(ResourceNotFoundException.class,
                () -> customerVisitService.getSameDayExaminationServices(customerId));

        UUID serviceId = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID())
                .status(VisitStatus.CHECKED_IN).checkInTime(LocalDateTime.now()).build();
        MedicalService service = MedicalService.builder().serviceId(serviceId).serviceCode("EX-2")
                .name("Khám Ngoại").departmentType(DepartmentType.EXAMINATION).build();
        var queue = vn.edu.fpt.cares.model.QueueTicket.builder().ticketId(UUID.randomUUID())
                .visit(visit).service(service).status(vn.edu.fpt.cares.enums.QueueStatus.WAITING).build();
        when(invoiceItemRepo.findSameDayExaminationRegistrations(eq(customerId), any(), any(), isNull()))
                .thenReturn(List.of());
        when(queueTicketRepo.findSameDayPatientExaminationTickets(eq(customerId), any())).thenReturn(List.of(queue));
        when(invoiceItemRepo.findDistinctExaminationServiceIdsByVisit(visit.getVisitId(), null))
                .thenReturn(List.of(serviceId));

        var result = customerVisitService.getSameDayExaminationServices(customerId);
        assertEquals(1, result.size());
        assertEquals(vn.edu.fpt.cares.enums.QueueStatus.WAITING, result.get(0).queueStatus());
        assertTrue(result.get(0).reason().contains("Đang chờ gọi"));
    }

    @Test
    void noSameDayRegistrationIgnoresParaclinicalButRejectsQueueDuplicate() {
        UUID customerId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        MedicalService lab = MedicalService.builder().serviceId(labId).name("Đường huyết")
                .departmentType(DepartmentType.LABORATORY).build();
        when(serviceRepo.findById(labId)).thenReturn(Optional.of(lab));
        assertDoesNotThrow(() -> customerVisitService.validateNoSameDayExaminationRegistration(
                customerId, List.of(labId, labId)));

        UUID examinationId = UUID.randomUUID();
        MedicalService examination = MedicalService.builder().serviceId(examinationId).name("Khám Nội")
                .departmentType(DepartmentType.EXAMINATION).build();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        var queue = vn.edu.fpt.cares.model.QueueTicket.builder().ticketId(UUID.randomUUID())
                .visit(visit).service(examination)
                .status(vn.edu.fpt.cares.enums.QueueStatus.WAITING).build();
        when(serviceRepo.findById(examinationId)).thenReturn(Optional.of(examination));
        when(invoiceItemRepo.findSameDayExaminationRegistrations(eq(customerId), any(), any(), isNull()))
                .thenReturn(List.of());
        when(queueTicketRepo.findSameDayPatientExaminationTickets(eq(customerId), any()))
                .thenReturn(List.of(vn.edu.fpt.cares.model.QueueTicket.builder().build(), queue));
        when(invoiceItemRepo.findDistinctExaminationServiceIdsByVisit(visit.getVisitId(), null))
                .thenReturn(List.of(examinationId));
        ConflictException error = assertThrows(ConflictException.class,
                () -> customerVisitService.validateNoSameDayExaminationRegistration(
                        customerId, List.of(examinationId)));
        assertTrue(error.getMessage().contains("VIS-"));
    }
}
