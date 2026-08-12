package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.profile.ProfileCreateRequest;
import org.example.doansummer2026.dto.profile.ProfileUpdateRequest;
import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TestRequestService testRequestService;

    @InjectMocks
    private ProfileService profileService;


    // =========================================================
    // HELPERS
    // =========================================================

    private Account account(UUID id, String username) {
        return Account.builder()
                .accountId(id)
                .username(username)
                .role(Role.CUSTOMER)
                .isActive(true)
                .build();
    }

    private Profile profile(UUID id, Account account) {
        return Profile.builder()
                .profileId(id)
                .account(account)
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .phone("0901234567")
                .email("test@gmail.com")
                .address("Ha Noi")
                .build();
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenFound() {

        UUID id = UUID.randomUUID();

        Profile profile =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer01")
                );

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(profile));

        assertSame(
                profile,
                profileService.findById(id)
        );
    }

    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(profileRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> profileService.findById(id)
        );
    }


    // =========================================================
    // GET
    // =========================================================

    @Test
    void get_ShouldReturnResponse() {

        UUID id = UUID.randomUUID();

        Profile profile =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer01")
                );

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(profile));

        assertNotNull(
                profileService.get(id)
        );
    }


    // =========================================================
    // GET BY ACCOUNT
    // =========================================================

    @Test
    void getByAccount_ShouldReturn_WhenFound() {

        UUID accountId = UUID.randomUUID();

        Profile profile =
                profile(
                        UUID.randomUUID(),
                        account(accountId, "customer01")
                );

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(profile));

        assertNotNull(
                profileService.getByAccount(accountId)
        );
    }

    @Test
    void getByAccount_ShouldThrow_WhenMissing() {

        UUID accountId = UUID.randomUUID();

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> profileService.getByAccount(accountId)
        );
    }


    // =========================================================
    // GET MY PROFILE - ACCOUNT MISSING
    // =========================================================

    @Test
    void getMyProfile_ShouldThrow_WhenAccountMissing() {

        UUID accountId = UUID.randomUUID();

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> profileService.getMyProfile(accountId)
        );

        verifyNoInteractions(appointmentRepository);
    }


    // =========================================================
    // GET MY PROFILE - PROFILE MISSING
    // =========================================================

    @Test
    void getMyProfile_ShouldThrow_WhenProfileMissing() {

        UUID accountId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> profileService.getMyProfile(accountId)
        );
    }


    // =========================================================
    // GET MY PROFILE - EMPTY
    // =========================================================

    @Test
    void getMyProfile_ShouldReturn_WhenNoAppointmentsAndNoTests() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        Profile profile =
                profile(profileId, account);

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(profile));

        when(
                appointmentRepository.findByCustomerId(profileId)
        ).thenReturn(List.of());

        when(
                testRequestService.findMyCompletedTests(profileId)
        ).thenReturn(List.of());

        var result =
                profileService.getMyProfile(accountId);

        assertNotNull(result);
    }


    // =========================================================
    // GET MY PROFILE - APPOINTMENT WITH CUSTOMER NAME + SERVICE
    // =========================================================

    @Test
    void getMyProfile_ShouldMapAppointmentWithCustomerNameAndService() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        Profile profile =
                profile(profileId, account);

        MedicalService medicalService =
                MedicalService.builder()
                        .serviceId(UUID.randomUUID())
                        .name("Kham noi")
                        .build();

        Appointment appointment =
                mock(Appointment.class);

        when(appointment.getCustomer())
                .thenReturn(profile);

        when(appointment.getServices())
                .thenReturn(Set.of(medicalService));

        when(appointment.getScheduledAt())
                .thenReturn(
                        LocalDateTime.of(
                                2026,
                                8,
                                10,
                                10,
                                0
                        )
                );

        when(appointment.getStatus())
                .thenReturn(
                        org.example.doansummer2026.enums.AppointmentStatus.PENDING
                );

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(profile));

        when(
                appointmentRepository.findByCustomerId(profileId)
        ).thenReturn(List.of(appointment));

        when(
                testRequestService.findMyCompletedTests(profileId)
        ).thenReturn(List.of());

        var result =
                profileService.getMyProfile(accountId);

        assertNotNull(result);
    }


    // =========================================================
    // GET MY PROFILE - APPOINTMENT CUSTOMER NULL
    // =========================================================

    @Test
    void getMyProfile_ShouldHandleAppointmentWithoutCustomer() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        Profile profile =
                profile(profileId, account);

        Appointment appointment =
                mock(Appointment.class);

        when(appointment.getCustomer())
                .thenReturn(null);

        when(appointment.getServices())
                .thenReturn(null);

        when(appointment.getScheduledAt())
                .thenReturn(LocalDateTime.now());

        when(appointment.getStatus())
                .thenReturn(
                        org.example.doansummer2026.enums.AppointmentStatus.PENDING
                );

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(profile));

        when(
                appointmentRepository.findByCustomerId(profileId)
        ).thenReturn(List.of(appointment));

        when(
                testRequestService.findMyCompletedTests(profileId)
        ).thenReturn(List.of());

        assertNotNull(
                profileService.getMyProfile(accountId)
        );
    }


    // =========================================================
    // GET MY PROFILE - CUSTOMER NAME NULL
    // =========================================================

    @Test
    void getMyProfile_ShouldHandleAppointmentCustomerNameNull() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        Profile profile =
                profile(profileId, account);

        Profile appointmentCustomer =
                Profile.builder()
                        .profileId(UUID.randomUUID())
                        .fullName(null)
                        .build();

        Appointment appointment =
                mock(Appointment.class);

        when(appointment.getCustomer())
                .thenReturn(appointmentCustomer);

        when(appointment.getServices())
                .thenReturn(Set.of());

        when(appointment.getScheduledAt())
                .thenReturn(LocalDateTime.now());

        when(appointment.getStatus())
                .thenReturn(
                        org.example.doansummer2026.enums.AppointmentStatus.PENDING
                );

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(profile));

        when(
                appointmentRepository.findByCustomerId(profileId)
        ).thenReturn(List.of(appointment));

        when(
                testRequestService.findMyCompletedTests(profileId)
        ).thenReturn(List.of());

        assertNotNull(
                profileService.getMyProfile(accountId)
        );
    }


    // =========================================================
    // GET MY PROFILE - TEST COMPLETED DATE
    // =========================================================

    @Test
    void getMyProfile_ShouldMapCompletedTest() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        Profile profile =
                profile(profileId, account);

        MedicalService medicalService =
                MedicalService.builder()
                        .serviceId(UUID.randomUUID())
                        .name("Xet nghiem mau")
                        .build();

        TestRequest test =
                TestRequest.builder()
                        .testRequestId(UUID.randomUUID())
                        .service(medicalService)
                        .completedAt(
                                LocalDateTime.of(
                                        2026,
                                        8,
                                        9,
                                        15,
                                        30
                                )
                        )
                        .build();

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(profile));

        when(
                appointmentRepository.findByCustomerId(profileId)
        ).thenReturn(List.of());

        when(
                testRequestService.findMyCompletedTests(profileId)
        ).thenReturn(List.of(test));

        assertNotNull(
                profileService.getMyProfile(accountId)
        );
    }


    // =========================================================
    // GET MY PROFILE - TEST COMPLETED AT NULL
    // =========================================================

    @Test
    void getMyProfile_ShouldHandleTestCompletedAtNull() {

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        Profile profile =
                profile(profileId, account);

        MedicalService medicalService =
                MedicalService.builder()
                        .serviceId(UUID.randomUUID())
                        .name("Xet nghiem")
                        .build();

        TestRequest test =
                TestRequest.builder()
                        .testRequestId(UUID.randomUUID())
                        .service(medicalService)
                        .completedAt(null)
                        .build();

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(profile));

        when(
                appointmentRepository.findByCustomerId(profileId)
        ).thenReturn(List.of());

        when(
                testRequestService.findMyCompletedTests(profileId)
        ).thenReturn(List.of(test));

        assertNotNull(
                profileService.getMyProfile(accountId)
        );
    }


    // =========================================================
    // CREATE - ACCOUNT MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenAccountMissing() {

        UUID accountId = UUID.randomUUID();

        ProfileCreateRequest req =
                mock(ProfileCreateRequest.class);

        when(req.accountId())
                .thenReturn(accountId);

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> profileService.create(req)
        );
    }


    // =========================================================
    // CREATE - PROFILE ALREADY EXISTS
    // =========================================================

    @Test
    void create_ShouldReject_WhenAccountAlreadyHasProfile() {

        UUID accountId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        Profile existing =
                profile(UUID.randomUUID(), account);

        ProfileCreateRequest req =
                mock(ProfileCreateRequest.class);

        when(req.accountId())
                .thenReturn(accountId);

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.of(existing));

        assertThrows(
                ConflictException.class,
                () -> profileService.create(req)
        );
    }


    // =========================================================
    // CREATE - DUPLICATE PHONE
    // =========================================================

    @Test
    void create_ShouldRejectDuplicatePhone() {

        UUID accountId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        ProfileCreateRequest req =
                mock(ProfileCreateRequest.class);

        when(req.accountId()).thenReturn(accountId);
        when(req.phone()).thenReturn("0901234567");

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.empty());

        when(
                profileRepository.findFirstByPhone("0901234567")
        ).thenReturn(
                Optional.of(
                        profile(
                                UUID.randomUUID(),
                                account(
                                        UUID.randomUUID(),
                                        "other"
                                )
                        )
                )
        );

        assertThrows(
                ConflictException.class,
                () -> profileService.create(req)
        );
    }


    // =========================================================
    // CREATE - DUPLICATE EMAIL
    // =========================================================

    @Test
    void create_ShouldRejectDuplicateEmail() {

        UUID accountId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        ProfileCreateRequest req =
                mock(ProfileCreateRequest.class);

        when(req.accountId()).thenReturn(accountId);
        when(req.email()).thenReturn("test@gmail.com");

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.empty());

        when(
                profileRepository.findFirstByEmail("test@gmail.com")
        ).thenReturn(
                Optional.of(
                        profile(
                                UUID.randomUUID(),
                                account(
                                        UUID.randomUUID(),
                                        "other"
                                )
                        )
                )
        );

        assertThrows(
                ConflictException.class,
                () -> profileService.create(req)
        );
    }


    // =========================================================
    // CREATE - BLANK PHONE / EMAIL SKIP UNIQUE
    // =========================================================

    @Test
    void create_ShouldSkipUniqueChecks_WhenPhoneAndEmailBlank() {

        UUID accountId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        ProfileCreateRequest req =
                mock(ProfileCreateRequest.class);

        when(req.accountId()).thenReturn(accountId);
        when(req.fullName()).thenReturn("Nguyen Van A");
        when(req.phone()).thenReturn(" ");
        when(req.email()).thenReturn("");

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.empty());

        when(profileRepository.save(any(Profile.class)))
                .thenAnswer(i -> {
                    Profile p = i.getArgument(0);
                    p.setProfileId(UUID.randomUUID());
                    return p;
                });

        profileService.create(req);

        verify(profileRepository, never())
                .findFirstByPhone(anyString());

        verify(profileRepository, never())
                .findFirstByEmail(anyString());
    }


    // =========================================================
    // CREATE - GENDER NULL
    // =========================================================

    @Test
    void create_ShouldAllowNullGender() {

        UUID accountId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        ProfileCreateRequest req =
                mock(ProfileCreateRequest.class);

        when(req.accountId()).thenReturn(accountId);
        when(req.fullName()).thenReturn("Nguyen Van A");

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.empty());

        when(profileRepository.save(any(Profile.class)))
                .thenAnswer(i -> {
                    Profile p = i.getArgument(0);
                    p.setProfileId(UUID.randomUUID());
                    return p;
                });

        profileService.create(req);

        verify(profileRepository)
                .save(argThat(p ->
                        p.getGender() == null
                ));
    }


    // =========================================================
    // CREATE - GENDER BLANK
    // =========================================================

    @Test
    void create_ShouldAllowBlankGender() {

        UUID accountId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        ProfileCreateRequest req =
                mock(ProfileCreateRequest.class);

        when(req.accountId()).thenReturn(accountId);
        when(req.fullName()).thenReturn("Nguyen Van A");
        when(req.gender()).thenReturn("   ");

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.empty());

        when(profileRepository.save(any(Profile.class)))
                .thenAnswer(i -> {
                    Profile p = i.getArgument(0);
                    p.setProfileId(UUID.randomUUID());
                    return p;
                });

        profileService.create(req);

        verify(profileRepository)
                .save(argThat(p ->
                        p.getGender() == null
                ));
    }


    // =========================================================
    // CREATE - VALID GENDER
    // =========================================================

    @Test
    void create_ShouldParseGenderCaseInsensitive() {

        UUID accountId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        ProfileCreateRequest req =
                mock(ProfileCreateRequest.class);

        when(req.accountId()).thenReturn(accountId);
        when(req.fullName()).thenReturn("Nguyen Van A");
        when(req.gender()).thenReturn(" female ");

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.empty());

        when(profileRepository.save(any(Profile.class)))
                .thenAnswer(i -> {
                    Profile p = i.getArgument(0);
                    p.setProfileId(UUID.randomUUID());
                    return p;
                });

        profileService.create(req);

        verify(profileRepository)
                .save(argThat(p ->
                        p.getGender() == Gender.FEMALE
                ));
    }


    // =========================================================
    // CREATE - GENDER OTHER
    // =========================================================

    @Test
    void create_ShouldRejectOtherGender() {

        UUID accountId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        ProfileCreateRequest req =
                mock(ProfileCreateRequest.class);

        when(req.accountId()).thenReturn(accountId);
        when(req.gender()).thenReturn("OTHER");

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.empty());

        assertThrows(
                ConflictException.class,
                () -> profileService.create(req)
        );
    }


    // =========================================================
    // CREATE - INVALID GENDER
    // =========================================================

    @Test
    void create_ShouldRejectInvalidGender() {

        UUID accountId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        ProfileCreateRequest req =
                mock(ProfileCreateRequest.class);

        when(req.accountId()).thenReturn(accountId);
        when(req.gender()).thenReturn("XYZ");

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.empty());

        assertThrows(
                ConflictException.class,
                () -> profileService.create(req)
        );
    }


    // =========================================================
    // CREATE SUCCESS
    // =========================================================

    @Test
    void create_ShouldCreateProfileSuccessfully() {

        UUID accountId = UUID.randomUUID();

        Account account =
                account(accountId, "customer01");

        ProfileCreateRequest req =
                mock(ProfileCreateRequest.class);

        when(req.accountId()).thenReturn(accountId);
        when(req.fullName()).thenReturn("Nguyen Van B");
        when(req.dateOfBirth()).thenReturn(LocalDate.of(2001, 5, 10));
        when(req.gender()).thenReturn("male");
        when(req.phone()).thenReturn("0911111111");
        when(req.email()).thenReturn("b@gmail.com");
        when(req.address()).thenReturn("Ha Noi");

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                profileRepository.findFirstByAccount_AccountId(accountId)
        ).thenReturn(Optional.empty());

        when(
                profileRepository.findFirstByPhone("0911111111")
        ).thenReturn(Optional.empty());

        when(
                profileRepository.findFirstByEmail("b@gmail.com")
        ).thenReturn(Optional.empty());

        when(profileRepository.save(any(Profile.class)))
                .thenAnswer(i -> {
                    Profile p = i.getArgument(0);
                    p.setProfileId(UUID.randomUUID());
                    return p;
                });

        var result =
                profileService.create(req);

        assertNotNull(result);

        verify(profileRepository)
                .save(argThat(p ->
                        p.getAccount() == account
                                && "Nguyen Van B".equals(p.getFullName())
                                && p.getGender() == Gender.MALE
                                && "0911111111".equals(p.getPhone())
                                && "b@gmail.com".equals(p.getEmail())
                ));
    }


    // =========================================================
    // UPDATE - BASIC FIELDS
    // =========================================================

    @Test
    void update_ShouldUpdateAllBasicFields() {

        UUID id = UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer")
                );

        ProfileUpdateRequest req =
                mock(ProfileUpdateRequest.class);

        when(req.fullName()).thenReturn("New Name");
        when(req.dateOfBirth()).thenReturn(LocalDate.of(1999, 10, 10));
        when(req.gender()).thenReturn("female");
        when(req.bloodType()).thenReturn(BloodType.A_POSITIVE);
        when(req.address()).thenReturn("Da Nang");
        when(req.insuranceId()).thenReturn("BH001");
        when(req.height()).thenReturn(170);
        when(req.weight()).thenReturn(60);

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(p));

        when(profileRepository.save(p))
                .thenReturn(p);

        var result =
                profileService.update(id, req);

        assertNotNull(result);

        assertEquals("New Name", p.getFullName());
        assertEquals(LocalDate.of(1999, 10, 10), p.getDateOfBirth());
        assertEquals(Gender.FEMALE, p.getGender());
        assertEquals(BloodType.A_POSITIVE, p.getBloodType());
        assertEquals("Da Nang", p.getAddress());
        assertEquals("BH001", p.getInsuranceId());
        assertEquals(170, p.getHeight());
        assertEquals(60, p.getWeight());
    }


    // =========================================================
    // UPDATE - ALLERGIES
    // =========================================================

    @Test
    void update_ShouldTrimRemoveBlankAndDistinctAllergies() {

        UUID id = UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer")
                );

        ProfileUpdateRequest req =
                mock(ProfileUpdateRequest.class);

        when(req.allergies())
                .thenReturn(
                        List.of(
                                " Penicillin ",
                                "",
                                "  ",
                                "Seafood",
                                "Penicillin"
                        )
                );

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(p));

        when(profileRepository.save(p))
                .thenReturn(p);

        profileService.update(id, req);

        assertEquals(
                "Penicillin\nSeafood",
                p.getAllergies()
        );
    }


    // =========================================================
    // UPDATE PHONE - DUPLICATE OTHER PROFILE
    // =========================================================

    @Test
    void update_ShouldRejectPhoneUsedByDifferentProfile() {

        UUID id = UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer")
                );

        Profile other =
                profile(
                        UUID.randomUUID(),
                        account(UUID.randomUUID(), "other")
                );

        ProfileUpdateRequest req =
                mock(ProfileUpdateRequest.class);

        when(req.phone())
                .thenReturn("0999999999");

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(p));

        when(
                profileRepository.findFirstByPhone("0999999999")
        ).thenReturn(Optional.of(other));

        assertThrows(
                ConflictException.class,
                () -> profileService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE PHONE - SAME PROFILE ALLOWED
    // =========================================================

    @Test
    void update_ShouldAllowPhoneOwnedBySameProfile() {

        UUID id = UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer")
                );

        ProfileUpdateRequest req =
                mock(ProfileUpdateRequest.class);

        when(req.phone())
                .thenReturn("0999999999");

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(p));

        when(
                profileRepository.findFirstByPhone("0999999999")
        ).thenReturn(Optional.of(p));

        when(profileRepository.save(p))
                .thenReturn(p);

        assertDoesNotThrow(
                () -> profileService.update(id, req)
        );

        assertEquals(
                "0999999999",
                p.getPhone()
        );
    }


    // =========================================================
    // UPDATE EMAIL - DUPLICATE
    // =========================================================

    @Test
    void update_ShouldRejectEmailUsedByDifferentProfile() {

        UUID id = UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer")
                );

        Profile other =
                profile(
                        UUID.randomUUID(),
                        account(UUID.randomUUID(), "other")
                );

        ProfileUpdateRequest req =
                mock(ProfileUpdateRequest.class);

        when(req.email())
                .thenReturn("new@gmail.com");

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(p));

        when(
                profileRepository.findFirstByEmail("new@gmail.com")
        ).thenReturn(Optional.of(other));

        assertThrows(
                ConflictException.class,
                () -> profileService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE EMAIL SAME PROFILE
    // =========================================================

    @Test
    void update_ShouldAllowEmailOwnedBySameProfile() {

        UUID id = UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer")
                );

        ProfileUpdateRequest req =
                mock(ProfileUpdateRequest.class);

        when(req.email())
                .thenReturn("same@gmail.com");

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(p));

        when(
                profileRepository.findFirstByEmail("same@gmail.com")
        ).thenReturn(Optional.of(p));

        when(profileRepository.save(p))
                .thenReturn(p);

        profileService.update(id, req);

        assertEquals(
                "same@gmail.com",
                p.getEmail()
        );
    }


    // =========================================================
    // UPDATE PHONE ONLY -> KEEP OLD EMAIL
    // =========================================================

    @Test
    void update_ShouldUseOldEmail_WhenOnlyPhoneChanges() {

        UUID id = UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer")
                );

        p.setEmail("old@gmail.com");

        ProfileUpdateRequest req =
                mock(ProfileUpdateRequest.class);

        when(req.phone())
                .thenReturn("0988888888");

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(p));

        when(
                profileRepository.findFirstByPhone("0988888888")
        ).thenReturn(Optional.empty());

        when(
                profileRepository.findFirstByEmail("old@gmail.com")
        ).thenReturn(Optional.of(p));

        when(profileRepository.save(p))
                .thenReturn(p);

        profileService.update(id, req);

        assertEquals(
                "0988888888",
                p.getPhone()
        );

        assertEquals(
                "old@gmail.com",
                p.getEmail()
        );
    }


    // =========================================================
    // UPDATE EMAIL ONLY -> KEEP OLD PHONE
    // =========================================================

    @Test
    void update_ShouldUseOldPhone_WhenOnlyEmailChanges() {

        UUID id = UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer")
                );

        p.setPhone("0900000000");

        ProfileUpdateRequest req =
                mock(ProfileUpdateRequest.class);

        when(req.email())
                .thenReturn("new@gmail.com");

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(p));

        when(
                profileRepository.findFirstByPhone("0900000000")
        ).thenReturn(Optional.of(p));

        when(
                profileRepository.findFirstByEmail("new@gmail.com")
        ).thenReturn(Optional.empty());

        when(profileRepository.save(p))
                .thenReturn(p);

        profileService.update(id, req);

        assertEquals(
                "0900000000",
                p.getPhone()
        );

        assertEquals(
                "new@gmail.com",
                p.getEmail()
        );
    }


    // =========================================================
    // UPDATE - NOTHING CHANGED
    // =========================================================

    @Test
    void update_ShouldStillSave_WhenRequestEmpty() {

        UUID id = UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer")
                );

        ProfileUpdateRequest req =
                mock(ProfileUpdateRequest.class);

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(p));

        when(profileRepository.save(p))
                .thenReturn(p);

        var result =
                profileService.update(id, req);

        assertNotNull(result);

        verify(profileRepository)
                .save(p);
    }


    // =========================================================
    // UPDATE - INVALID GENDER
    // =========================================================

    @Test
    void update_ShouldRejectInvalidGender() {

        UUID id = UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer")
                );

        ProfileUpdateRequest req =
                mock(ProfileUpdateRequest.class);

        when(req.gender())
                .thenReturn("abc");

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(p));

        assertThrows(
                ConflictException.class,
                () -> profileService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE - OTHER GENDER
    // =========================================================

    @Test
    void update_ShouldRejectOtherGender() {

        UUID id = UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(UUID.randomUUID(), "customer")
                );

        ProfileUpdateRequest req =
                mock(ProfileUpdateRequest.class);

        when(req.gender())
                .thenReturn("OTHER");

        when(profileRepository.findById(id))
                .thenReturn(Optional.of(p));

        assertThrows(
                ConflictException.class,
                () -> profileService.update(id, req)
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenProfileMissing() {

        UUID id = UUID.randomUUID();

        when(profileRepository.existsById(id))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> profileService.delete(id)
        );

        verify(profileRepository, never())
                .deleteById(id);
    }

    @Test
    void delete_ShouldDelete_WhenExists() {

        UUID id = UUID.randomUUID();

        when(profileRepository.existsById(id))
                .thenReturn(true);

        profileService.delete(id);

        verify(profileRepository)
                .deleteById(id);
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void search_ShouldReturnMappedPage() {

        var pageable =
                PageRequest.of(0, 10);

        Profile p =
                profile(
                        UUID.randomUUID(),
                        account(UUID.randomUUID(), "customer")
                );

        when(
                profileRepository.search(
                        "nguyen",
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of(p))
        );

        var result =
                profileService.search(
                        "nguyen",
                        pageable
                );

        assertNotNull(result);

        verify(profileRepository)
                .search(
                        "nguyen",
                        pageable
                );
    }
}