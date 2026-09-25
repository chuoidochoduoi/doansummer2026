package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.dto.profile.ProfileCreateRequest;
import vn.edu.fpt.cares.dto.profile.ProfileUpdateRequest;
import vn.edu.fpt.cares.enums.AppointmentStatus;
import vn.edu.fpt.cares.enums.BloodType;
import vn.edu.fpt.cares.enums.Gender;
import vn.edu.fpt.cares.enums.Role;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.Account;
import vn.edu.fpt.cares.model.Appointment;
import vn.edu.fpt.cares.model.MedicalService;
import vn.edu.fpt.cares.model.Profile;
import vn.edu.fpt.cares.model.TestRequest;
import vn.edu.fpt.cares.repository.AccountRepository;
import vn.edu.fpt.cares.repository.AppointmentRepository;
import vn.edu.fpt.cares.repository.ProfileRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private Account account(
            UUID id,
            String username
    ) {
        return Account.builder()
                .accountId(id)
                .username(username)
                .role(Role.CUSTOMER)
                .isActive(true)
                .build();
    }


    private Profile profile(
            UUID id,
            Account account
    ) {
        return Profile.builder()
                .profileId(id)
                .account(account)
                .fullName("Nguyen Van A")
                .dateOfBirth(
                        LocalDate.of(
                                2000,
                                1,
                                1
                        )
                )
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

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer01"
                        )
                );

        when(
                profileRepository.findById(id)
        ).thenReturn(
                Optional.of(p)
        );

        assertSame(
                p,
                profileService.findById(id)
        );
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id =
                UUID.randomUUID();

        when(
                profileRepository.findById(id)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        profileService.findById(
                                id
                        )
        );
    }


    // =========================================================
    // GET
    // =========================================================

    @Test
    void get_ShouldReturnResponse() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer01"
                        )
                );

        when(
                profileRepository.findById(id)
        ).thenReturn(
                Optional.of(p)
        );

        assertNotNull(
                profileService.get(id)
        );
    }


    // =========================================================
    // GET BY ACCOUNT
    // =========================================================

    @Test
    void getByAccount_ShouldReturn_WhenFound() {

        UUID accountId =
                UUID.randomUUID();

        Profile p =
                profile(
                        UUID.randomUUID(),
                        account(
                                accountId,
                                "customer01"
                        )
                );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(p)
        );

        assertNotNull(
                profileService
                        .getByAccount(
                                accountId
                        )
        );
    }


    @Test
    void getByAccount_ShouldThrow_WhenMissing() {

        UUID accountId =
                UUID.randomUUID();

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        profileService
                                .getByAccount(
                                        accountId
                                )
        );
    }


    // =========================================================
    // GET MY PROFILE
    // =========================================================

    @Test
    void getMyProfile_ShouldThrow_WhenAccountMissing() {

        UUID accountId =
                UUID.randomUUID();

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        profileService
                                .getMyProfile(
                                        accountId
                                )
        );

        verifyNoInteractions(
                appointmentRepository
        );

        verifyNoInteractions(
                testRequestService
        );
    }


    @Test
    void getMyProfile_ShouldThrow_WhenProfileMissing() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        profileService
                                .getMyProfile(
                                        accountId
                                )
        );
    }


    @Test
    void getMyProfile_ShouldReturn_WhenNoAppointmentsAndNoTests() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        Profile profile =
                profile(
                        profileId,
                        account
                );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(profile)
        );

        when(
                appointmentRepository
                        .findByCustomerId(
                                profileId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                testRequestService
                        .findMyCompletedTests(
                                profileId
                        )
        ).thenReturn(
                List.of()
        );

        var result =
                profileService
                        .getMyProfile(
                                accountId
                        );

        assertNotNull(result);
    }


    @Test
    void getMyProfile_ShouldMapAppointmentWithService() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        Profile profile =
                profile(
                        profileId,
                        account
                );

        MedicalService service =
                MedicalService.builder()
                        .serviceId(
                                UUID.randomUUID()
                        )
                        .name(
                                "Kham noi"
                        )
                        .build();

        Appointment appointment =
                mock(Appointment.class);

        when(
                appointment.getServices()
        ).thenReturn(
                Set.of(service)
        );

        when(
                appointment.getScheduledAt()
        ).thenReturn(
                LocalDateTime.now().plusDays(1)
        );

        when(
                appointment.getStatus()
        ).thenReturn(
                AppointmentStatus.PENDING
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(profile)
        );

        when(
                appointmentRepository
                        .findByCustomerId(
                                profileId
                        )
        ).thenReturn(
                List.of(appointment)
        );

        when(
                testRequestService
                        .findMyCompletedTests(
                                profileId
                        )
        ).thenReturn(
                List.of()
        );

        var result =
                profileService
                        .getMyProfile(
                                accountId
                        );

        assertNotNull(result);
    }


    @Test
    void getMyProfile_ShouldHandleAppointmentWithoutServices() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        Profile profile =
                profile(
                        profileId,
                        account
                );

        Appointment appointment =
                mock(Appointment.class);

        when(
                appointment.getServices()
        ).thenReturn(null);

        when(
                appointment.getScheduledAt()
        ).thenReturn(
                LocalDateTime.now().plusDays(1)
        );

        when(
                appointment.getStatus()
        ).thenReturn(
                AppointmentStatus.PENDING
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(profile)
        );

        when(
                appointmentRepository
                        .findByCustomerId(
                                profileId
                        )
        ).thenReturn(
                List.of(appointment)
        );

        when(
                testRequestService
                        .findMyCompletedTests(
                                profileId
                        )
        ).thenReturn(
                List.of()
        );

        assertNotNull(
                profileService
                        .getMyProfile(
                                accountId
                        )
        );
    }


    @Test
    void getMyProfile_ShouldHandleAppointmentWithEmptyServices() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        Profile profile =
                profile(
                        profileId,
                        account
                );

        Appointment appointment =
                mock(Appointment.class);

        when(
                appointment.getServices()
        ).thenReturn(
                Set.of()
        );

        when(
                appointment.getScheduledAt()
        ).thenReturn(
                LocalDateTime.now().plusDays(1)
        );

        when(
                appointment.getStatus()
        ).thenReturn(
                AppointmentStatus.PENDING
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(profile)
        );

        when(
                appointmentRepository
                        .findByCustomerId(
                                profileId
                        )
        ).thenReturn(
                List.of(appointment)
        );

        when(
                testRequestService
                        .findMyCompletedTests(
                                profileId
                        )
        ).thenReturn(
                List.of()
        );

        assertNotNull(
                profileService
                        .getMyProfile(
                                accountId
                        )
        );
    }


    @Test
    void getMyProfile_ShouldMapCompletedTest() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        Profile profile =
                profile(
                        profileId,
                        account
                );

        MedicalService service =
                MedicalService.builder()
                        .serviceId(
                                UUID.randomUUID()
                        )
                        .name(
                                "Xet nghiem mau"
                        )
                        .build();

        TestRequest test =
                TestRequest.builder()
                        .testRequestId(
                                UUID.randomUUID()
                        )
                        .service(service)
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

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(profile)
        );

        when(
                appointmentRepository
                        .findByCustomerId(
                                profileId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                testRequestService
                        .findMyCompletedTests(
                                profileId
                        )
        ).thenReturn(
                List.of(test)
        );

        assertNotNull(
                profileService
                        .getMyProfile(
                                accountId
                        )
        );
    }


    @Test
    void getMyProfile_ShouldHandleTestCompletedAtNull() {

        UUID accountId =
                UUID.randomUUID();

        UUID profileId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        Profile profile =
                profile(
                        profileId,
                        account
                );

        MedicalService service =
                MedicalService.builder()
                        .serviceId(
                                UUID.randomUUID()
                        )
                        .name(
                                "Xet nghiem"
                        )
                        .build();

        TestRequest test =
                TestRequest.builder()
                        .testRequestId(
                                UUID.randomUUID()
                        )
                        .service(service)
                        .completedAt(null)
                        .build();

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(profile)
        );

        when(
                appointmentRepository
                        .findByCustomerId(
                                profileId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                testRequestService
                        .findMyCompletedTests(
                                profileId
                        )
        ).thenReturn(
                List.of(test)
        );

        assertNotNull(
                profileService
                        .getMyProfile(
                                accountId
                        )
        );
    }


    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void create_ShouldThrow_WhenAccountMissing() {

        UUID accountId =
                UUID.randomUUID();

        ProfileCreateRequest req =
                mock(
                        ProfileCreateRequest.class
                );

        when(
                req.accountId()
        ).thenReturn(
                accountId
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        profileService
                                .create(req)
        );
    }


    @Test
    void create_ShouldReject_WhenAccountAlreadyHasProfile() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        Profile existing =
                profile(
                        UUID.randomUUID(),
                        account
                );

        ProfileCreateRequest req =
                mock(
                        ProfileCreateRequest.class
                );

        when(
                req.accountId()
        ).thenReturn(
                accountId
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.of(existing)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        profileService
                                .create(req)
        );
    }


    @Test
    void create_ShouldRejectDuplicatePhone() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        Profile other =
                profile(
                        UUID.randomUUID(),
                        account(
                                UUID.randomUUID(),
                                "other"
                        )
                );

        ProfileCreateRequest req =
                mock(
                        ProfileCreateRequest.class
                );

        when(
                req.accountId()
        ).thenReturn(
                accountId
        );

        when(
                req.phone()
        ).thenReturn(
                "0901234567"
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                profileRepository
                        .findFirstByPhone(
                                "0901234567"
                        )
        ).thenReturn(
                Optional.of(other)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        profileService
                                .create(req)
        );

        verify(
                profileRepository,
                never()
        ).save(
                any(Profile.class)
        );
    }


    @Test
    void create_ShouldRejectDuplicateEmail() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        Profile other =
                profile(
                        UUID.randomUUID(),
                        account(
                                UUID.randomUUID(),
                                "other"
                        )
                );

        ProfileCreateRequest req =
                mock(
                        ProfileCreateRequest.class
                );

        when(
                req.accountId()
        ).thenReturn(
                accountId
        );

        when(
                req.email()
        ).thenReturn(
                "test@gmail.com"
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                profileRepository
                        .findFirstByEmailIgnoreCase(
                                "test@gmail.com"
                        )
        ).thenReturn(
                Optional.of(other)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        profileService
                                .create(req)
        );

        verify(
                profileRepository,
                never()
        ).save(
                any(Profile.class)
        );
    }


    @Test
    void create_ShouldSkipUniqueChecks_WhenPhoneAndEmailBlank() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        ProfileCreateRequest req =
                mock(
                        ProfileCreateRequest.class
                );

        when(
                req.accountId()
        ).thenReturn(
                accountId
        );

        when(
                req.fullName()
        ).thenReturn(
                "Nguyen Van A"
        );

        when(
                req.phone()
        ).thenReturn(
                " "
        );

        when(
                req.email()
        ).thenReturn(
                ""
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                profileRepository.save(
                        any(Profile.class)
                )
        ).thenAnswer(
                invocation -> {

                    Profile p =
                            invocation.getArgument(0);

                    p.setProfileId(
                            UUID.randomUUID()
                    );

                    return p;
                }
        );

        assertNotNull(
                profileService.create(req)
        );

        verify(
                profileRepository,
                never()
        ).findFirstByPhone(
                anyString()
        );

        verify(
                profileRepository,
                never()
        ).findFirstByEmailIgnoreCase(
                anyString()
        );
    }


    @Test
    void create_ShouldAllowNullGender() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        ProfileCreateRequest req =
                mock(
                        ProfileCreateRequest.class
                );

        when(
                req.accountId()
        ).thenReturn(
                accountId
        );

        when(
                req.fullName()
        ).thenReturn(
                "Nguyen Van A"
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                profileRepository.save(
                        any(Profile.class)
                )
        ).thenAnswer(
                invocation -> {

                    Profile p =
                            invocation.getArgument(0);

                    p.setProfileId(
                            UUID.randomUUID()
                    );

                    return p;
                }
        );

        profileService.create(req);

        verify(
                profileRepository
        ).save(
                argThat(
                        p ->
                                p.getGender()
                                        == null
                )
        );
    }


    @Test
    void create_ShouldAllowBlankGender() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        ProfileCreateRequest req =
                mock(
                        ProfileCreateRequest.class
                );

        when(
                req.accountId()
        ).thenReturn(
                accountId
        );

        when(
                req.fullName()
        ).thenReturn(
                "Nguyen Van A"
        );

        when(
                req.gender()
        ).thenReturn(
                "   "
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                profileRepository.save(
                        any(Profile.class)
                )
        ).thenAnswer(
                invocation -> {

                    Profile p =
                            invocation.getArgument(0);

                    p.setProfileId(
                            UUID.randomUUID()
                    );

                    return p;
                }
        );

        profileService.create(req);

        verify(
                profileRepository
        ).save(
                argThat(
                        p ->
                                p.getGender()
                                        == null
                )
        );
    }


    @Test
    void create_ShouldParseGenderCaseInsensitive() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        ProfileCreateRequest req =
                mock(
                        ProfileCreateRequest.class
                );

        when(
                req.accountId()
        ).thenReturn(
                accountId
        );

        when(
                req.fullName()
        ).thenReturn(
                "Nguyen Van A"
        );

        when(
                req.gender()
        ).thenReturn(
                " female "
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                profileRepository.save(
                        any(Profile.class)
                )
        ).thenAnswer(
                invocation -> {

                    Profile p =
                            invocation.getArgument(0);

                    p.setProfileId(
                            UUID.randomUUID()
                    );

                    return p;
                }
        );

        profileService.create(req);

        verify(
                profileRepository
        ).save(
                argThat(
                        p ->
                                p.getGender()
                                        == Gender.FEMALE
                )
        );
    }


    @Test
    void create_ShouldRejectOtherGender() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        ProfileCreateRequest req =
                mock(
                        ProfileCreateRequest.class
                );

        when(
                req.accountId()
        ).thenReturn(
                accountId
        );

        when(
                req.gender()
        ).thenReturn(
                "OTHER"
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ConflictException.class,
                () ->
                        profileService.create(req)
        );

        verify(
                profileRepository,
                never()
        ).save(
                any(Profile.class)
        );
    }


    @Test
    void create_ShouldRejectInvalidGender() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        ProfileCreateRequest req =
                mock(
                        ProfileCreateRequest.class
                );

        when(
                req.accountId()
        ).thenReturn(
                accountId
        );

        when(
                req.gender()
        ).thenReturn(
                "XYZ"
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ConflictException.class,
                () ->
                        profileService.create(req)
        );
    }


    @Test
    void create_ShouldCreateProfileSuccessfully() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        accountId,
                        "customer01"
                );

        ProfileCreateRequest req =
                mock(
                        ProfileCreateRequest.class
                );

        when(
                req.accountId()
        ).thenReturn(
                accountId
        );

        when(
                req.fullName()
        ).thenReturn(
                "Nguyen Van B"
        );

        when(
                req.dateOfBirth()
        ).thenReturn(
                LocalDate.of(
                        2001,
                        5,
                        10
                )
        );

        when(
                req.gender()
        ).thenReturn(
                "male"
        );

        when(
                req.phone()
        ).thenReturn(
                "0911111111"
        );

        when(
                req.email()
        ).thenReturn(
                "b@gmail.com"
        );

        when(
                req.address()
        ).thenReturn(
                "Ha Noi"
        );

        when(
                accountRepository.findById(
                        accountId
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                accountId
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                profileRepository.save(
                        any(Profile.class)
                )
        ).thenAnswer(
                invocation -> {

                    Profile p =
                            invocation.getArgument(0);

                    p.setProfileId(
                            UUID.randomUUID()
                    );

                    return p;
                }
        );

        var result =
                profileService.create(req);

        assertNotNull(result);

        verify(
                profileRepository
        ).findFirstByPhone(
                "0911111111"
        );

        verify(
                profileRepository
        ).findFirstByEmailIgnoreCase(
                "b@gmail.com"
        );

        verify(
                profileRepository
        ).save(
                argThat(
                        p ->
                                p.getAccount()
                                        == account
                                        &&
                                        "Nguyen Van B"
                                                .equals(
                                                        p.getFullName()
                                                )
                                        &&
                                        p.getGender()
                                                == Gender.MALE
                                        &&
                                        "0911111111"
                                                .equals(
                                                        p.getPhone()
                                                )
                                        &&
                                        "b@gmail.com"
                                                .equals(
                                                        p.getEmail()
                                                )
                )
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void update_ShouldUpdateAllBasicFields() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.fullName()
        ).thenReturn(
                "New Name"
        );

        when(
                req.dateOfBirth()
        ).thenReturn(
                LocalDate.of(
                        1999,
                        10,
                        10
                )
        );

        when(
                req.gender()
        ).thenReturn(
                "female"
        );

        when(
                req.bloodType()
        ).thenReturn(
                BloodType.A_POSITIVE
        );

        when(
                req.address()
        ).thenReturn(
                "Da Nang"
        );

        when(
                req.insuranceId()
        ).thenReturn(
                "BH001"
        );

        when(
                req.height()
        ).thenReturn(
                170
        );

        when(
                req.weight()
        ).thenReturn(
                60
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository.save(p)
        ).thenReturn(p);

        var result =
                profileService.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                "New Name",
                p.getFullName()
        );

        assertEquals(
                LocalDate.of(
                        1999,
                        10,
                        10
                ),
                p.getDateOfBirth()
        );

        assertEquals(
                Gender.FEMALE,
                p.getGender()
        );

        assertEquals(
                BloodType.A_POSITIVE,
                p.getBloodType()
        );

        assertEquals(
                "Da Nang",
                p.getAddress()
        );

        assertEquals(
                "BH001",
                p.getInsuranceId()
        );

        assertEquals(
                170,
                p.getHeight()
        );

        assertEquals(
                60,
                p.getWeight()
        );
    }


    @Test
    void update_ShouldNormalizeFullName() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.fullName()
        ).thenReturn(
                "  Nguyen   Van   B  "
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository.save(p)
        ).thenReturn(p);

        profileService.update(
                id,
                req
        );

        assertEquals(
                "Nguyen Van B",
                p.getFullName()
        );
    }


    @Test
    void update_ShouldTrimRemoveBlankAndDistinctAllergies() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.allergies()
        ).thenReturn(
                List.of(
                        " Penicillin ",
                        "",
                        "  ",
                        "Seafood",
                        "Penicillin"
                )
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository.save(p)
        ).thenReturn(p);

        profileService.update(
                id,
                req
        );

        assertEquals(
                "Penicillin\nSeafood",
                p.getAllergies()
        );
    }


    @Test
    void update_ShouldRejectPhoneUsedByDifferentProfile() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        Profile other =
                profile(
                        UUID.randomUUID(),
                        account(
                                UUID.randomUUID(),
                                "other"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.phone()
        ).thenReturn(
                "0999999999"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository.findFirstByPhone(
                        "0999999999"
                )
        ).thenReturn(
                Optional.of(other)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        profileService.update(
                                id,
                                req
                        )
        );

        verify(
                profileRepository,
                never()
        ).save(
                any(Profile.class)
        );
    }


    @Test
    void update_ShouldAllowPhoneOwnedBySameProfile() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.phone()
        ).thenReturn(
                "0999999999"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository.findFirstByPhone(
                        "0999999999"
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository.save(p)
        ).thenReturn(p);

        assertDoesNotThrow(
                () ->
                        profileService.update(
                                id,
                                req
                        )
        );

        assertEquals(
                "0999999999",
                p.getPhone()
        );
    }


    @Test
    void update_ShouldRejectEmailUsedByDifferentProfile() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        Profile other =
                profile(
                        UUID.randomUUID(),
                        account(
                                UUID.randomUUID(),
                                "other"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.email()
        ).thenReturn(
                "new@gmail.com"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository
                        .findFirstByEmailIgnoreCase(
                                "new@gmail.com"
                        )
        ).thenReturn(
                Optional.of(other)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        profileService.update(
                                id,
                                req
                        )
        );

        verify(
                profileRepository,
                never()
        ).save(
                any(Profile.class)
        );
    }


    @Test
    void update_ShouldAllowEmailOwnedBySameProfile() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.email()
        ).thenReturn(
                "SAME@GMAIL.COM"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository
                        .findFirstByEmailIgnoreCase(
                                "same@gmail.com"
                        )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository.save(p)
        ).thenReturn(p);

        profileService.update(
                id,
                req
        );

        assertEquals(
                "same@gmail.com",
                p.getEmail()
        );
    }


    @Test
    void update_ShouldUseOldEmail_WhenOnlyPhoneChanges() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        p.setEmail(
                "old@gmail.com"
        );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.phone()
        ).thenReturn(
                "0988888888"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository
                        .findFirstByEmailIgnoreCase(
                                "old@gmail.com"
                        )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository.save(p)
        ).thenReturn(p);

        profileService.update(
                id,
                req
        );

        assertEquals(
                "0988888888",
                p.getPhone()
        );

        assertEquals(
                "old@gmail.com",
                p.getEmail()
        );

        verify(
                profileRepository
        ).findFirstByPhone(
                "0988888888"
        );
    }


    @Test
    void update_ShouldUseOldPhone_WhenOnlyEmailChanges() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        p.setPhone(
                "0900000000"
        );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.email()
        ).thenReturn(
                "NEW@GMAIL.COM"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository.findFirstByPhone(
                        "0900000000"
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository.save(p)
        ).thenReturn(p);

        profileService.update(
                id,
                req
        );

        assertEquals(
                "0900000000",
                p.getPhone()
        );

        assertEquals(
                "new@gmail.com",
                p.getEmail()
        );

        verify(
                profileRepository
        ).findFirstByEmailIgnoreCase(
                "new@gmail.com"
        );
    }


    @Test
    void update_ShouldStillSave_WhenRequestEmpty() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository.save(p)
        ).thenReturn(p);

        var result =
                profileService.update(
                        id,
                        req
                );

        assertNotNull(result);

        verify(
                profileRepository
        ).save(p);
    }


    @Test
    void update_ShouldRejectInvalidGender() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.gender()
        ).thenReturn(
                "abc"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        profileService.update(
                                id,
                                req
                        )
        );
    }


    @Test
    void update_ShouldRejectOtherGender() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.gender()
        ).thenReturn(
                "OTHER"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        profileService.update(
                                id,
                                req
                        )
        );
    }


    // =========================================================
    // VALIDATION AFTER UPDATE
    // =========================================================

    @Test
    void update_ShouldRejectBlankFullName() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.fullName()
        ).thenReturn(
                " "
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        profileService.update(
                                id,
                                req
                        )
        );

        verify(
                profileRepository,
                never()
        ).save(
                any(Profile.class)
        );
    }


    @Test
    void update_ShouldRejectFullNameContainingNumber() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.fullName()
        ).thenReturn(
                "Nguyen Van 123"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        profileService.update(
                                id,
                                req
                        )
        );
    }


    @Test
    void update_ShouldRejectFutureDateOfBirth() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.dateOfBirth()
        ).thenReturn(
                LocalDate.now()
                        .plusDays(1)
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        profileService.update(
                                id,
                                req
                        )
        );
    }


    @Test
    void update_ShouldReject_WhenBothPhoneAndEmailBecomeBlank() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.phone()
        ).thenReturn(
                " "
        );

        when(
                req.email()
        ).thenReturn(
                " "
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        profileService.update(
                                id,
                                req
                        )
        );

        verify(
                profileRepository,
                never()
        ).save(
                any(Profile.class)
        );
    }


    // =========================================================
    // UPDATE SELF
    // =========================================================

    @Test
    void updateSelf_ShouldRejectPhoneChange() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.phone()
        ).thenReturn(
                "0999999999"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        profileService
                                .updateSelf(
                                        id,
                                        req
                                )
        );
    }


    @Test
    void updateSelf_ShouldRejectEmailChange() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.email()
        ).thenReturn(
                "other@gmail.com"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        profileService
                                .updateSelf(
                                        id,
                                        req
                                )
        );
    }


    @Test
    void updateSelf_ShouldRejectInsuranceChange() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        p.setInsuranceId(
                "OLD"
        );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.insuranceId()
        ).thenReturn(
                "NEW"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        profileService
                                .updateSelf(
                                        id,
                                        req
                                )
        );
    }


    @Test
    void updateSelf_ShouldAllowSafeProfileFields() {

        UUID id =
                UUID.randomUUID();

        Profile p =
                profile(
                        id,
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        ProfileUpdateRequest req =
                mock(
                        ProfileUpdateRequest.class
                );

        when(
                req.fullName()
        ).thenReturn(
                "Nguyen Van B"
        );

        when(
                req.address()
        ).thenReturn(
                "Da Nang"
        );

        when(
                profileRepository.findById(
                        id
                )
        ).thenReturn(
                Optional.of(p)
        );

        when(
                profileRepository.save(p)
        ).thenReturn(p);

        var result =
                profileService
                        .updateSelf(
                                id,
                                req
                        );

        assertNotNull(result);

        assertEquals(
                "Nguyen Van B",
                p.getFullName()
        );

        assertEquals(
                "Da Nang",
                p.getAddress()
        );

        assertEquals(
                "0901234567",
                p.getPhone()
        );

        assertEquals(
                "test@gmail.com",
                p.getEmail()
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenProfileMissing() {

        UUID id =
                UUID.randomUUID();

        when(
                profileRepository.existsById(id)
        ).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        profileService.delete(id)
        );

        verify(
                profileRepository,
                never()
        ).deleteById(
                any(UUID.class)
        );
    }


    @Test
    void delete_ShouldRejectDeletion_WhenProfileExists() {

        UUID id =
                UUID.randomUUID();

        when(
                profileRepository.existsById(id)
        ).thenReturn(true);

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                profileService.delete(id)
                );

        assertEquals(
                "Không thể xóa hồ sơ cá nhân đã tạo. Vui lòng khóa tài khoản để ngừng sử dụng và giữ nguyên lịch sử",
                exception.getMessage()
        );

        verify(
                profileRepository,
                never()
        ).deleteById(
                any(UUID.class)
        );
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void search_ShouldReturnMappedPage() {

        var pageable =
                PageRequest.of(
                        0,
                        10
                );

        Profile p =
                profile(
                        UUID.randomUUID(),
                        account(
                                UUID.randomUUID(),
                                "customer"
                        )
                );

        when(
                profileRepository.search(
                        "nguyen",
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of(p)
                )
        );

        var result =
                profileService.search(
                        "nguyen",
                        pageable
                );

        assertNotNull(result);

        verify(
                profileRepository
        ).search(
                "nguyen",
                pageable
        );
    }


    @Test
    void search_ShouldReturnEmptyPage() {

        var pageable =
                PageRequest.of(
                        0,
                        10
                );

        when(
                profileRepository.search(
                        null,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of()
                )
        );

        assertNotNull(
                profileService.search(
                        null,
                        pageable
                )
        );
    }

    @Test
    void validateUpdatedProfileCoversEveryRequiredIdentityBoundary() {
        LocalDate validDob = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).minusYears(20);
        Profile candidate = Profile.builder().fullName(null).dateOfBirth(validDob)
                .gender(Gender.MALE).phone("0900000000").build();
        assertThrows(BadRequestException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "validateUpdatedProfile", candidate));
        candidate.setFullName(" ");
        assertThrows(BadRequestException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "validateUpdatedProfile", candidate));
        candidate.setFullName("A");
        assertThrows(BadRequestException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "validateUpdatedProfile", candidate));
        candidate.setFullName("Nguyễn 2 An");
        assertThrows(BadRequestException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "validateUpdatedProfile", candidate));

        candidate.setFullName("Nguyễn An");
        candidate.setDateOfBirth(null);
        assertThrows(BadRequestException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "validateUpdatedProfile", candidate));
        candidate.setDateOfBirth(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        assertThrows(BadRequestException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "validateUpdatedProfile", candidate));
        candidate.setDateOfBirth(validDob);
        candidate.setGender(null);
        assertThrows(BadRequestException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "validateUpdatedProfile", candidate));
        candidate.setGender(Gender.OTHER);
        assertThrows(BadRequestException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "validateUpdatedProfile", candidate));

        candidate.setGender(Gender.FEMALE);
        candidate.setPhone(" ");
        candidate.setEmail(null);
        assertThrows(BadRequestException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "validateUpdatedProfile", candidate));
        candidate.setEmail("patient@example.com");
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "validateUpdatedProfile", candidate));
    }

    @Test
    void blankAndEmailNormalizationCoverNullBlankTrimAndCase() {
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "blankToNull", new Object[]{null}));
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "blankToNull", "  "));
        assertEquals("value", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "blankToNull", " value "));
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "normalizeEmail", "  "));
        assertEquals("user@example.com", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                profileService, "normalizeEmail", " USER@EXAMPLE.COM "));
    }
}
