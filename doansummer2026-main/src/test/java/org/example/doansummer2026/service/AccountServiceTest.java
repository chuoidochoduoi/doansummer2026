package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.account.AccountUpdateRequest;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private StaffInfoRepository staffInfoRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;


    // =====================================================
    // CREATE
    // =====================================================

    @Test
    void create_ShouldSaveActiveAccount_WhenUsernameIsAvailable() {

        String username = "customer001";
        String rawPassword = "88888888";
        String encodedPassword = "encoded-password";

        when(accountRepository.existsByUsername(username))
                .thenReturn(false);

        when(passwordEncoder.encode(rawPassword))
                .thenReturn(encodedPassword);

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Account result =
                accountService.create(
                        username,
                        rawPassword,
                        Role.CUSTOMER
                );

        assertEquals(
                username,
                result.getUsername()
        );

        assertEquals(
                encodedPassword,
                result.getPasswordHash()
        );

        assertEquals(
                Role.CUSTOMER,
                result.getRole()
        );

        assertTrue(
                result.getIsActive()
        );

        verify(passwordEncoder)
                .encode(rawPassword);

        verify(accountRepository)
                .save(result);
    }


    @Test
    void create_ShouldThrowConflict_WhenUsernameAlreadyExists() {

        String username = "customer001";

        when(accountRepository.existsByUsername(username))
                .thenReturn(true);

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> accountService.create(
                                username,
                                "88888888",
                                Role.CUSTOMER
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(username)
        );

        verifyNoInteractions(passwordEncoder);

        verify(
                accountRepository,
                never()
        ).save(any(Account.class));
    }


    // =====================================================
    // FIND BY ID
    // =====================================================

    @Test
    void findById_ShouldReturnAccount_WhenAccountExists() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "user01",
                        true
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        Account result =
                accountService.findById(
                        accountId
                );

        assertSame(
                account,
                result
        );

        verify(accountRepository)
                .findById(accountId);
    }


    @Test
    void findById_ShouldThrowNotFound_WhenAccountDoesNotExist() {

        UUID accountId =
                UUID.randomUUID();

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.findById(
                        accountId
                )
        );

        verify(accountRepository)
                .findById(accountId);
    }


    // =====================================================
    // FIND BY USERNAME
    // =====================================================

    @Test
    void findByUsername_ShouldReturnAccount_WhenUsernameExists() {

        String username =
                "doctor01";

        Account account =
                account(
                        username,
                        true
                );

        when(accountRepository.findFirstByUsername(username))
                .thenReturn(
                        Optional.of(account)
                );

        Account result =
                accountService.findByUsername(
                        username
                );

        assertSame(
                account,
                result
        );

        verify(accountRepository)
                .findFirstByUsername(username);
    }


    @Test
    void findByUsername_ShouldThrowNotFound_WhenUsernameDoesNotExist() {

        String username =
                "unknown";

        when(accountRepository.findFirstByUsername(username))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.findByUsername(
                        username
                )
        );

        verify(accountRepository)
                .findFirstByUsername(username);
    }


    // =====================================================
    // UPDATE - SUCCESS
    // =====================================================

    @Test
    void update_ShouldUpdateUsername_WhenRequestIsValid() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "oldUsername",
                        true
                );

        AccountUpdateRequest request =
                new AccountUpdateRequest(
                        "newUsername",
                        null,
                        null
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                accountRepository.existsByUsername(
                        "newUsername"
                )
        ).thenReturn(false);

        when(accountRepository.save(account))
                .thenReturn(account);

        Account result =
                accountService.update(
                        accountId,
                        request
                );

        assertSame(
                account,
                result
        );

        assertEquals(
                "newUsername",
                result.getUsername()
        );

        assertEquals(
                Role.STAFF,
                result.getRole()
        );

        assertTrue(
                result.getIsActive()
        );

        verify(accountRepository)
                .existsByUsername(
                        "newUsername"
                );

        verify(accountRepository)
                .save(account);
    }


    // =====================================================
    // UPDATE - DUPLICATE USERNAME
    // =====================================================

    @Test
    void update_ShouldThrowConflict_WhenNewUsernameAlreadyExists() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "oldUsername",
                        true
                );

        AccountUpdateRequest request =
                new AccountUpdateRequest(
                        "existingUsername",
                        null,
                        null
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                accountRepository.existsByUsername(
                        "existingUsername"
                )
        ).thenReturn(true);

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> accountService.update(
                                accountId,
                                request
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Tên đăng nhập đã tồn tại"
                        )
        );

        assertEquals(
                "oldUsername",
                account.getUsername()
        );

        verify(accountRepository)
                .existsByUsername(
                        "existingUsername"
                );

        verify(
                accountRepository,
                never()
        ).save(account);
    }


    // =====================================================
    // UPDATE - SAME USERNAME
    // =====================================================

    @Test
    void update_ShouldNotCheckDuplicate_WhenUsernameDoesNotChange() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "doctor01",
                        true
                );

        AccountUpdateRequest request =
                new AccountUpdateRequest(
                        "doctor01",
                        null,
                        null
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(accountRepository.save(account))
                .thenReturn(account);

        Account result =
                accountService.update(
                        accountId,
                        request
                );

        assertSame(
                account,
                result
        );

        assertEquals(
                "doctor01",
                result.getUsername()
        );

        verify(
                accountRepository,
                never()
        ).existsByUsername(
                "doctor01"
        );

        verify(accountRepository)
                .save(account);
    }


    // =====================================================
    // UPDATE - NULL FIELDS
    // =====================================================

    @Test
    void update_ShouldKeepOldValues_WhenRequestFieldsAreNull() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "doctor01",
                        true
                );

        AccountUpdateRequest request =
                new AccountUpdateRequest(
                        null,
                        null,
                        null
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(accountRepository.save(account))
                .thenReturn(account);

        Account result =
                accountService.update(
                        accountId,
                        request
                );

        assertSame(
                account,
                result
        );

        assertEquals(
                "doctor01",
                result.getUsername()
        );

        assertEquals(
                Role.STAFF,
                result.getRole()
        );

        assertTrue(
                result.getIsActive()
        );

        verify(
                accountRepository,
                never()
        ).existsByUsername(
                anyString()
        );

        verify(accountRepository)
                .save(account);
    }


    // =====================================================
    // UPDATE - ROLE CHANGE
    // =====================================================

    @Test
    void update_ShouldThrowConflict_WhenTryingToChangeRole() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "doctor01",
                        true
                );

        AccountUpdateRequest request =
                new AccountUpdateRequest(
                        null,
                        Role.CUSTOMER,
                        null
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> accountService.update(
                                accountId,
                                request
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Không được đổi loại tài khoản"
                        )
        );

        assertEquals(
                Role.STAFF,
                account.getRole()
        );

        verify(
                accountRepository,
                never()
        ).save(account);
    }


    // =====================================================
    // UPDATE - ACTIVE STATUS CHANGE
    // =====================================================

    @Test
    void update_ShouldThrowConflict_WhenTryingToChangeActiveStatus() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "doctor01",
                        true
                );

        AccountUpdateRequest request =
                new AccountUpdateRequest(
                        null,
                        null,
                        false
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> accountService.update(
                                accountId,
                                request
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "khóa hoặc mở khóa"
                        )
        );

        assertTrue(
                account.getIsActive()
        );

        verify(
                accountRepository,
                never()
        ).save(account);
    }


    // =====================================================
    // UPDATE - CUSTOMER ACCOUNT
    // =====================================================

    @Test
    void update_ShouldThrowConflict_WhenAccountIsCustomer() {

        UUID accountId =
                UUID.randomUUID();

        Account customer =
                Account.builder()
                        .accountId(accountId)
                        .username("customer01")
                        .passwordHash(
                                "encoded-password"
                        )
                        .role(
                                Role.CUSTOMER
                        )
                        .isActive(true)
                        .build();

        AccountUpdateRequest request =
                new AccountUpdateRequest(
                        "customer02",
                        null,
                        null
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(customer)
                );

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> accountService.update(
                                accountId,
                                request
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Tài khoản khách hàng chỉ được khóa hoặc mở khóa"
                        )
        );

        assertEquals(
                "customer01",
                customer.getUsername()
        );

        verify(
                accountRepository,
                never()
        ).existsByUsername(
                anyString()
        );

        verify(
                accountRepository,
                never()
        ).save(customer);
    }


    // =====================================================
    // UPDATE - BLANK USERNAME
    // =====================================================

    @Test
    void update_ShouldThrowBadRequest_WhenNewUsernameIsBlank() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "doctor01",
                        true
                );

        AccountUpdateRequest request =
                new AccountUpdateRequest(
                        "   ",
                        null,
                        null
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> accountService.update(
                                accountId,
                                request
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Tên đăng nhập không được để trống"
                        )
        );

        assertEquals(
                "doctor01",
                account.getUsername()
        );

        verify(
                accountRepository,
                never()
        ).existsByUsername(
                anyString()
        );

        verify(
                accountRepository,
                never()
        ).save(account);
    }


    // =====================================================
    // CHANGE PASSWORD
    // =====================================================

    @Test
    void changePassword_ShouldEncodeAndSave_WhenOldPasswordIsCorrect() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "doctor01",
                        true
                );

        account.setPasswordHash(
                "old-encoded-password"
        );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                passwordEncoder.matches(
                        "old-password",
                        "old-encoded-password"
                )
        ).thenReturn(true);

        when(
                passwordEncoder.encode(
                        "new-password"
                )
        ).thenReturn(
                "new-encoded-password"
        );

        accountService.changePassword(
                accountId,
                "old-password",
                "new-password"
        );

        assertEquals(
                "new-encoded-password",
                account.getPasswordHash()
        );

        verify(passwordEncoder)
                .encode(
                        "new-password"
                );

        verify(accountRepository)
                .save(account);
    }


    @Test
    void changePassword_ShouldThrowConflictAndNotSave_WhenOldPasswordIsIncorrect() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "doctor01",
                        true
                );

        account.setPasswordHash(
                "old-encoded-password"
        );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                passwordEncoder.matches(
                        "wrong-password",
                        "old-encoded-password"
                )
        ).thenReturn(false);

        assertThrows(
                ConflictException.class,
                () -> accountService.changePassword(
                        accountId,
                        "wrong-password",
                        "new-password"
                )
        );

        verify(
                passwordEncoder,
                never()
        ).encode(
                "new-password"
        );

        verify(
                accountRepository,
                never()
        ).save(account);
    }


    // =====================================================
    // FORCE CHANGE PASSWORD
    // =====================================================

    @Test
    void forceChangePassword_ShouldEncodeAndSaveNewPassword() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "doctor01",
                        true
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                passwordEncoder.encode(
                        "new-password"
                )
        ).thenReturn(
                "encoded-new-password"
        );

        accountService.forceChangePassword(
                accountId,
                "new-password"
        );

        assertEquals(
                "encoded-new-password",
                account.getPasswordHash()
        );

        verify(passwordEncoder)
                .encode(
                        "new-password"
                );

        verify(accountRepository)
                .save(account);
    }


    // =====================================================
    // ADMIN RESET PASSWORD
    // =====================================================

    @Test
    void adminResetPassword_ShouldEncodeAndSaveNewPassword() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "customer01",
                        true
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                passwordEncoder.encode(
                        "reset-password"
                )
        ).thenReturn(
                "encoded-reset-password"
        );

        accountService.adminResetPassword(
                accountId,
                "reset-password"
        );

        assertEquals(
                "encoded-reset-password",
                account.getPasswordHash()
        );

        verify(passwordEncoder)
                .encode(
                        "reset-password"
                );

        verify(accountRepository)
                .save(account);
    }


    @Test
    void adminResetPassword_ShouldThrowBadRequest_WhenPasswordIsTooShort() {

        UUID accountId =
                UUID.randomUUID();

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                accountService.adminResetPassword(
                                        accountId,
                                        "1234567"
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "ít nhất 8 ký tự"
                        )
        );

        verifyNoInteractions(
                accountRepository
        );

        verifyNoInteractions(
                passwordEncoder
        );
    }


    @Test
    void adminResetPassword_ShouldThrowBadRequest_WhenPasswordIsNull() {

        UUID accountId =
                UUID.randomUUID();

        assertThrows(
                BadRequestException.class,
                () ->
                        accountService.adminResetPassword(
                                accountId,
                                null
                        )
        );

        verifyNoInteractions(
                accountRepository
        );

        verifyNoInteractions(
                passwordEncoder
        );
    }


    // =====================================================
    // SOFT DELETE
    // =====================================================

    @Test
    void softDelete_ShouldSetAccountInactive() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "customer01",
                        true
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username(
                                account.getUsername()
                        )
        ).thenReturn(
                Optional.empty()
        );

        accountService.softDelete(
                accountId
        );

        assertFalse(
                account.getIsActive()
        );

        verify(accountRepository)
                .save(account);
    }


    @Test
    void softDelete_ShouldRejectAdminAccount() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "admin01",
                        true
                );

        StaffInfo admin =
                StaffInfo.builder()
                        .systemRole(
                                SystemRole.ADMIN
                        )
                        .build();

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username(
                                account.getUsername()
                        )
        ).thenReturn(
                Optional.of(admin)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        accountService.softDelete(
                                accountId
                        )
        );

        assertTrue(
                account.getIsActive()
        );

        verify(
                accountRepository,
                never()
        ).save(account);
    }


    // =====================================================
    // LIST ACCOUNTS
    // =====================================================

    @Test
    void list_ShouldFindByRole_WhenRoleIsProvided() {

        Pageable pageable =
                PageRequest.of(
                        0,
                        10
                );

        Account account =
                account(
                        "doctor01",
                        true
                );

        Page<Account> page =
                new PageImpl<>(
                        List.of(account)
                );

        when(
                accountRepository.findByRole(
                        Role.STAFF,
                        pageable
                )
        ).thenReturn(page);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username(
                                "doctor01"
                        )
        ).thenReturn(
                Optional.empty()
        );

        var result =
                accountService.list(
                        Role.STAFF,
                        pageable
                );

        assertNotNull(
                result
        );

        verify(accountRepository)
                .findByRole(
                        Role.STAFF,
                        pageable
                );

        verify(
                accountRepository,
                never()
        ).findAll(
                pageable
        );
    }


    @Test
    void list_ShouldFindAll_WhenRoleIsNull() {

        Pageable pageable =
                PageRequest.of(
                        0,
                        10
                );

        Account account =
                account(
                        "customer01",
                        true
                );

        Page<Account> page =
                new PageImpl<>(
                        List.of(account)
                );

        when(
                accountRepository.findAll(
                        pageable
                )
        ).thenReturn(page);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username(
                                "customer01"
                        )
        ).thenReturn(
                Optional.empty()
        );

        var result =
                accountService.list(
                        null,
                        pageable
                );

        assertNotNull(
                result
        );

        verify(accountRepository)
                .findAll(
                        pageable
                );

        verify(
                accountRepository,
                never()
        ).findByRole(
                any(),
                any()
        );
    }


    @Test
    void list_ShouldIncludeSystemRole_WhenStaffInfoExists() {

        Pageable pageable =
                PageRequest.of(
                        0,
                        10
                );

        Account account =
                account(
                        "doctor01",
                        true
                );

        StaffInfo staff =
                StaffInfo.builder()
                        .systemRole(
                                SystemRole.DOCTOR
                        )
                        .build();

        Page<Account> page =
                new PageImpl<>(
                        List.of(account)
                );

        when(
                accountRepository.findByRole(
                        Role.STAFF,
                        pageable
                )
        ).thenReturn(page);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username(
                                "doctor01"
                        )
        ).thenReturn(
                Optional.of(staff)
        );

        var result =
                accountService.list(
                        Role.STAFF,
                        pageable
                );

        assertNotNull(
                result
        );

        verify(accountRepository)
                .findByRole(
                        Role.STAFF,
                        pageable
                );

        verify(
                staffInfoRepository
        ).findFirstByProfile_Account_Username(
                "doctor01"
        );
    }


    // =====================================================
    // LIST STAFF
    // =====================================================

    @Test
    void listStaff_ShouldReturnMappedStaffAccounts() {

        Pageable pageable =
                PageRequest.of(
                        0,
                        10
                );

        Account account =
                account(
                        "doctor01",
                        true
                );

        Profile profile =
                mock(
                        Profile.class
                );

        StaffInfo staff =
                mock(
                        StaffInfo.class
                );

        when(
                staff.getProfile()
        ).thenReturn(
                profile
        );

        when(
                staff.getStaffCode()
        ).thenReturn(
                "ST001"
        );

        when(
                staff.getSystemRole()
        ).thenReturn(
                SystemRole.DOCTOR
        );

        when(
                profile.getAccount()
        ).thenReturn(
                account
        );

        when(
                profile.getFullName()
        ).thenReturn(
                "Nguyen Van A"
        );

        Page<StaffInfo> page =
                new PageImpl<>(
                        List.of(staff)
                );

        when(
                staffInfoRepository.search(
                        "doctor",
                        null,
                        SystemRole.DOCTOR,
                        pageable
                )
        ).thenReturn(
                page
        );

        var result =
                accountService.listStaff(
                        "doctor",
                        SystemRole.DOCTOR,
                        pageable
                );

        assertNotNull(
                result
        );

        verify(
                staffInfoRepository
        ).search(
                "doctor",
                null,
                SystemRole.DOCTOR,
                pageable
        );
    }


    // =====================================================
    // LIST CUSTOMERS
    // =====================================================

    @Test
    void listCustomers_ShouldUseTrue_WhenStatusIsActive() {

        Pageable pageable =
                PageRequest.of(
                        0,
                        10
                );

        Account account =
                account(
                        "customer01",
                        true
                );

        Page<Account> page =
                new PageImpl<>(
                        List.of(account)
                );

        Profile profile =
                mock(
                        Profile.class
                );

        when(
                profile.getFullName()
        ).thenReturn(
                "Nguyen Van Customer"
        );

        when(
                accountRepository.searchCustomers(
                        "customer",
                        true,
                        pageable
                )
        ).thenReturn(
                page
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                account.getAccountId()
                        )
        ).thenReturn(
                Optional.of(profile)
        );

        var result =
                accountService.listCustomers(
                        "customer",
                        "active",
                        pageable
                );

        assertNotNull(
                result
        );

        verify(accountRepository)
                .searchCustomers(
                        "customer",
                        true,
                        pageable
                );
    }


    @Test
    void listCustomers_ShouldUseFalse_WhenStatusIsLocked() {

        Pageable pageable =
                PageRequest.of(
                        0,
                        10
                );

        Account account =
                account(
                        "customer01",
                        false
                );

        Page<Account> page =
                new PageImpl<>(
                        List.of(account)
                );

        when(
                accountRepository.searchCustomers(
                        "customer",
                        false,
                        pageable
                )
        ).thenReturn(
                page
        );

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                account.getAccountId()
                        )
        ).thenReturn(
                Optional.empty()
        );

        var result =
                accountService.listCustomers(
                        "customer",
                        "locked",
                        pageable
                );

        assertNotNull(
                result
        );

        verify(accountRepository)
                .searchCustomers(
                        "customer",
                        false,
                        pageable
                );
    }


    @Test
    void listCustomers_ShouldUseNull_WhenStatusIsUnknown() {

        Pageable pageable =
                PageRequest.of(
                        0,
                        10
                );

        Page<Account> page =
                new PageImpl<>(
                        List.of()
                );

        when(
                accountRepository.searchCustomers(
                        "customer",
                        null,
                        pageable
                )
        ).thenReturn(
                page
        );

        var result =
                accountService.listCustomers(
                        "customer",
                        "all",
                        pageable
                );

        assertNotNull(
                result
        );

        verify(accountRepository)
                .searchCustomers(
                        "customer",
                        null,
                        pageable
                );
    }


    // =====================================================
    // LOCK
    // =====================================================

    @Test
    void lock_ShouldToggleAccountStatus_WhenAccountIsNotProtected() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "cashier01",
                        true
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username(
                                account.getUsername()
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                accountRepository.save(account)
        ).thenReturn(
                account
        );

        Account result =
                accountService.lock(
                        accountId
                );

        assertSame(
                account,
                result
        );

        assertFalse(
                result.getIsActive()
        );

        verify(accountRepository)
                .save(account);
    }


    @Test
    void lock_ShouldUnlockAccount_WhenAccountIsCurrentlyLocked() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "cashier01",
                        false
                );

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username(
                                account.getUsername()
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                accountRepository.save(account)
        ).thenReturn(
                account
        );

        Account result =
                accountService.lock(
                        accountId
                );

        assertTrue(
                result.getIsActive()
        );

        verify(accountRepository)
                .save(account);
    }


    @Test
    void lock_ShouldRejectProtectedAdminAccount() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "admin01",
                        true
                );

        StaffInfo admin =
                StaffInfo.builder()
                        .systemRole(
                                SystemRole.ADMIN
                        )
                        .build();

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username(
                                account.getUsername()
                        )
        ).thenReturn(
                Optional.of(admin)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        accountService.lock(
                                accountId
                        )
        );

        assertTrue(
                account.getIsActive()
        );

        verify(
                accountRepository,
                never()
        ).save(account);
    }


    @Test
    void lock_ShouldRejectClinicManagerAccount() {

        UUID accountId =
                UUID.randomUUID();

        Account account =
                account(
                        "manager01",
                        true
                );

        StaffInfo manager =
                StaffInfo.builder()
                        .systemRole(
                                SystemRole.CLINIC_MANAGER
                        )
                        .build();

        when(accountRepository.findById(accountId))
                .thenReturn(
                        Optional.of(account)
                );

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username(
                                account.getUsername()
                        )
        ).thenReturn(
                Optional.of(manager)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        accountService.lock(
                                accountId
                        )
        );

        assertTrue(
                account.getIsActive()
        );

        verify(
                accountRepository,
                never()
        ).save(account);
    }


    // =====================================================
    // HELPER
    // =====================================================

    private Account account(
            String username,
            boolean active
    ) {

        return Account.builder()
                .accountId(
                        UUID.randomUUID()
                )
                .username(
                        username
                )
                .passwordHash(
                        "encoded-password"
                )
                .role(
                        Role.STAFF
                )
                .isActive(
                        active
                )
                .build();
    }
}