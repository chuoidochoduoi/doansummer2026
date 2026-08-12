package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.doansummer2026.dto.account.AccountUpdateRequest;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

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

    @Test
    void create_ShouldSaveActiveAccount_WhenUsernameIsAvailable() {
        String username = "customer001";
        String rawPassword = "88888888";
        String encodedPassword = "encoded-password";

        when(accountRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(accountRepository.save(org.mockito.ArgumentMatchers.any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.create(username, rawPassword, Role.CUSTOMER);

        assertEquals(username, result.getUsername());
        assertEquals(encodedPassword, result.getPasswordHash());
        assertEquals(Role.CUSTOMER, result.getRole());
        assertTrue(result.getIsActive());
        verify(passwordEncoder).encode(rawPassword);
        verify(accountRepository).save(result);
    }

    @Test
    void create_ShouldThrowConflict_WhenUsernameAlreadyExists() {
        String username = "customer001";
        when(accountRepository.existsByUsername(username)).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> accountService.create(username, "88888888", Role.CUSTOMER)
        );

        assertTrue(exception.getMessage().contains(username));
        verifyNoInteractions(passwordEncoder);
        verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account.class));
    }

    @Test
    void changePassword_ShouldEncodeAndSave_WhenOldPasswordIsCorrect() {
        UUID accountId = UUID.randomUUID();
        Account account = account("doctor01", true);
        account.setPasswordHash("old-encoded-password");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("old-password", "old-encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded-password");

        accountService.changePassword(accountId, "old-password", "new-password");

        assertEquals("new-encoded-password", account.getPasswordHash());
        verify(accountRepository).save(account);
    }

    @Test
    void changePassword_ShouldThrowConflictAndNotSave_WhenOldPasswordIsIncorrect() {
        UUID accountId = UUID.randomUUID();
        Account account = account("doctor01", true);
        account.setPasswordHash("old-encoded-password");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong-password", "old-encoded-password")).thenReturn(false);

        assertThrows(
                ConflictException.class,
                () -> accountService.changePassword(accountId, "wrong-password", "new-password")
        );

        verify(passwordEncoder, never()).encode("new-password");
        verify(accountRepository, never()).save(account);
    }

    @Test
    void lock_ShouldToggleAccountStatus_WhenAccountIsNotProtected() {
        UUID accountId = UUID.randomUUID();
        Account account = account("cashier01", true);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(staffInfoRepository.findFirstByProfile_Account_Username(account.getUsername()))
                .thenReturn(Optional.empty());
        when(accountRepository.save(account)).thenReturn(account);

        Account result = accountService.lock(accountId);

        assertSame(account, result);
        assertFalse(result.getIsActive());
        verify(accountRepository).save(account);
    }

    @Test
    void lock_ShouldRejectProtectedAdminAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = account("admin01", true);
        StaffInfo admin = StaffInfo.builder().systemRole(SystemRole.ADMIN).build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(staffInfoRepository.findFirstByProfile_Account_Username(account.getUsername()))
                .thenReturn(Optional.of(admin));

        assertThrows(ConflictException.class, () -> accountService.lock(accountId));

        assertTrue(account.getIsActive());
        verify(accountRepository, never()).save(account);
    }

    private Account account(String username, boolean active) {
        return Account.builder()
                .accountId(UUID.randomUUID())
                .username(username)
                .passwordHash("encoded-password")
                .role(Role.STAFF)
                .isActive(active)
                .build();
    }

    // =====================================================
// FIND BY ID
// =====================================================

    @Test
    void findById_ShouldReturnAccount_WhenAccountExists() {
        UUID accountId = UUID.randomUUID();
        Account account = account("user01", true);

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        Account result = accountService.findById(accountId);

        assertSame(account, result);
        verify(accountRepository).findById(accountId);
    }

    @Test
    void findById_ShouldThrowNotFound_WhenAccountDoesNotExist() {
        UUID accountId = UUID.randomUUID();

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.findById(accountId)
        );

        verify(accountRepository).findById(accountId);
    }


// =====================================================
// FIND BY USERNAME
// =====================================================

    @Test
    void findByUsername_ShouldReturnAccount_WhenUsernameExists() {
        String username = "doctor01";
        Account account = account(username, true);

        when(accountRepository.findFirstByUsername(username))
                .thenReturn(Optional.of(account));

        Account result = accountService.findByUsername(username);

        assertSame(account, result);
        verify(accountRepository).findFirstByUsername(username);
    }

    @Test
    void findByUsername_ShouldThrowNotFound_WhenUsernameDoesNotExist() {
        String username = "unknown";

        when(accountRepository.findFirstByUsername(username))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.findByUsername(username)
        );

        verify(accountRepository).findFirstByUsername(username);
    }


// =====================================================
// UPDATE
// =====================================================

    @Test
    void update_ShouldUpdateAllFields_WhenRequestIsValid() {
        UUID accountId = UUID.randomUUID();

        Account account = account("oldUsername", true);

        AccountUpdateRequest request = mock(AccountUpdateRequest.class);

        when(request.username()).thenReturn("newUsername");
        when(request.role()).thenReturn(Role.CUSTOMER);
        when(request.isActive()).thenReturn(false);

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(accountRepository.existsByUsername("newUsername"))
                .thenReturn(false);

        when(accountRepository.save(account))
                .thenReturn(account);

        Account result = accountService.update(accountId, request);

        assertSame(account, result);
        assertEquals("newUsername", result.getUsername());
        assertEquals(Role.CUSTOMER, result.getRole());
        assertFalse(result.getIsActive());

        verify(accountRepository).existsByUsername("newUsername");
        verify(accountRepository).save(account);
    }


    @Test
    void update_ShouldThrowConflict_WhenNewUsernameAlreadyExists() {
        UUID accountId = UUID.randomUUID();

        Account account = account("oldUsername", true);

        AccountUpdateRequest request = mock(AccountUpdateRequest.class);

        when(request.username()).thenReturn("existingUsername");

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(accountRepository.existsByUsername("existingUsername"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> accountService.update(accountId, request)
        );

        assertEquals("oldUsername", account.getUsername());

        verify(accountRepository, never()).save(account);
    }


    @Test
    void update_ShouldNotCheckDuplicate_WhenUsernameDoesNotChange() {
        UUID accountId = UUID.randomUUID();

        Account account = account("doctor01", true);

        AccountUpdateRequest request = mock(AccountUpdateRequest.class);

        when(request.username()).thenReturn("doctor01");
        when(request.role()).thenReturn(null);
        when(request.isActive()).thenReturn(null);

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(accountRepository.save(account))
                .thenReturn(account);

        Account result = accountService.update(accountId, request);

        assertSame(account, result);

        verify(accountRepository, never())
                .existsByUsername("doctor01");

        verify(accountRepository).save(account);
    }


    @Test
    void update_ShouldKeepOldValues_WhenRequestFieldsAreNull() {
        UUID accountId = UUID.randomUUID();

        Account account = account("doctor01", true);
        account.setRole(Role.STAFF);

        AccountUpdateRequest request = mock(AccountUpdateRequest.class);

        when(request.username()).thenReturn(null);
        when(request.role()).thenReturn(null);
        when(request.isActive()).thenReturn(null);

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(accountRepository.save(account))
                .thenReturn(account);

        Account result = accountService.update(accountId, request);

        assertEquals("doctor01", result.getUsername());
        assertEquals(Role.STAFF, result.getRole());
        assertTrue(result.getIsActive());

        verify(accountRepository).save(account);
    }


// =====================================================
// FORCE CHANGE PASSWORD
// =====================================================

    @Test
    void forceChangePassword_ShouldEncodeAndSaveNewPassword() {
        UUID accountId = UUID.randomUUID();

        Account account = account("doctor01", true);

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(passwordEncoder.encode("new-password"))
                .thenReturn("encoded-new-password");

        accountService.forceChangePassword(
                accountId,
                "new-password"
        );

        assertEquals(
                "encoded-new-password",
                account.getPasswordHash()
        );

        verify(passwordEncoder).encode("new-password");
        verify(accountRepository).save(account);
    }


// =====================================================
// ADMIN RESET PASSWORD
// =====================================================

    @Test
    void adminResetPassword_ShouldEncodeAndSaveNewPassword() {
        UUID accountId = UUID.randomUUID();

        Account account = account("customer01", true);

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(passwordEncoder.encode("reset-password"))
                .thenReturn("encoded-reset-password");

        accountService.adminResetPassword(
                accountId,
                "reset-password"
        );

        assertEquals(
                "encoded-reset-password",
                account.getPasswordHash()
        );

        verify(passwordEncoder).encode("reset-password");
        verify(accountRepository).save(account);
    }


// =====================================================
// SOFT DELETE
// =====================================================

    @Test
    void softDelete_ShouldSetAccountInactive() {
        UUID accountId = UUID.randomUUID();

        Account account = account("customer01", true);

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        accountService.softDelete(accountId);

        assertFalse(account.getIsActive());

        verify(accountRepository).save(account);
    }


// =====================================================
// LIST ACCOUNTS
// =====================================================

    @Test
    void list_ShouldFindByRole_WhenRoleIsProvided() {
        Pageable pageable = PageRequest.of(0, 10);

        Account account = account("doctor01", true);

        Page<Account> page =
                new PageImpl<>(List.of(account));

        when(accountRepository.findByRole(Role.STAFF, pageable))
                .thenReturn(page);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("doctor01")
        ).thenReturn(Optional.empty());

        var result = accountService.list(
                Role.STAFF,
                pageable
        );

        assertNotNull(result);

        verify(accountRepository)
                .findByRole(Role.STAFF, pageable);

        verify(accountRepository, never())
                .findAll(pageable);
    }


    @Test
    void list_ShouldFindAll_WhenRoleIsNull() {
        Pageable pageable = PageRequest.of(0, 10);

        Account account = account("customer01", true);

        Page<Account> page =
                new PageImpl<>(List.of(account));

        when(accountRepository.findAll(pageable))
                .thenReturn(page);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("customer01")
        ).thenReturn(Optional.empty());

        var result = accountService.list(
                null,
                pageable
        );

        assertNotNull(result);

        verify(accountRepository)
                .findAll(pageable);

        verify(accountRepository, never())
                .findByRole(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }


    @Test
    void list_ShouldIncludeSystemRole_WhenStaffInfoExists() {
        Pageable pageable = PageRequest.of(0, 10);

        Account account = account("doctor01", true);

        StaffInfo staff = StaffInfo.builder()
                .systemRole(SystemRole.DOCTOR)
                .build();

        Page<Account> page =
                new PageImpl<>(List.of(account));

        when(accountRepository.findByRole(Role.STAFF, pageable))
                .thenReturn(page);

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username("doctor01")
        ).thenReturn(Optional.of(staff));

        var result =
                accountService.list(Role.STAFF, pageable);

        assertNotNull(result);
    }


// =====================================================
// LIST STAFF
// =====================================================

    @Test
    void listStaff_ShouldReturnMappedStaffAccounts() {
        Pageable pageable = PageRequest.of(0, 10);

        Account account = account("doctor01", true);

        Profile profile = mock(Profile.class);
        StaffInfo staff = mock(StaffInfo.class);

        when(staff.getProfile()).thenReturn(profile);
        when(staff.getStaffCode()).thenReturn("ST001");
        when(staff.getSystemRole()).thenReturn(SystemRole.DOCTOR);

        when(profile.getAccount()).thenReturn(account);
        when(profile.getFullName()).thenReturn("Nguyen Van A");

        Page<StaffInfo> page =
                new PageImpl<>(List.of(staff));

        when(
                staffInfoRepository.search(
                        "doctor",
                        null,
                        SystemRole.DOCTOR,
                        pageable
                )
        ).thenReturn(page);

        var result = accountService.listStaff(
                "doctor",
                SystemRole.DOCTOR,
                pageable
        );

        assertNotNull(result);

        verify(staffInfoRepository).search(
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
        Pageable pageable = PageRequest.of(0, 10);

        Account account = account("customer01", true);

        Page<Account> page =
                new PageImpl<>(List.of(account));

        Profile profile = mock(Profile.class);

        when(profile.getFullName())
                .thenReturn("Nguyen Van Customer");

        when(
                accountRepository.searchCustomers(
                        "customer",
                        true,
                        pageable
                )
        ).thenReturn(page);

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                account.getAccountId()
                        )
        ).thenReturn(Optional.of(profile));

        var result = accountService.listCustomers(
                "customer",
                "active",
                pageable
        );

        assertNotNull(result);

        verify(accountRepository).searchCustomers(
                "customer",
                true,
                pageable
        );
    }


    @Test
    void listCustomers_ShouldUseFalse_WhenStatusIsLocked() {
        Pageable pageable = PageRequest.of(0, 10);

        Account account = account("customer01", false);

        Page<Account> page =
                new PageImpl<>(List.of(account));

        when(
                accountRepository.searchCustomers(
                        "customer",
                        false,
                        pageable
                )
        ).thenReturn(page);

        when(
                profileRepository
                        .findFirstByAccount_AccountId(
                                account.getAccountId()
                        )
        ).thenReturn(Optional.empty());

        var result = accountService.listCustomers(
                "customer",
                "locked",
                pageable
        );

        assertNotNull(result);

        verify(accountRepository).searchCustomers(
                "customer",
                false,
                pageable
        );
    }


    @Test
    void listCustomers_ShouldUseNull_WhenStatusIsUnknown() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Account> page =
                new PageImpl<>(List.of());

        when(
                accountRepository.searchCustomers(
                        "customer",
                        null,
                        pageable
                )
        ).thenReturn(page);

        var result = accountService.listCustomers(
                "customer",
                "all",
                pageable
        );

        assertNotNull(result);

        verify(accountRepository).searchCustomers(
                "customer",
                null,
                pageable
        );
    }


// =====================================================
// LOCK
// =====================================================

    @Test
    void lock_ShouldUnlockAccount_WhenAccountIsCurrentlyLocked() {
        UUID accountId = UUID.randomUUID();

        Account account = account("cashier01", false);

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username(
                                account.getUsername()
                        )
        ).thenReturn(Optional.empty());

        when(accountRepository.save(account))
                .thenReturn(account);

        Account result = accountService.lock(accountId);

        assertTrue(result.getIsActive());

        verify(accountRepository).save(account);
    }


    @Test
    void lock_ShouldRejectClinicManagerAccount() {
        UUID accountId = UUID.randomUUID();

        Account account =
                account("manager01", true);

        StaffInfo manager =
                StaffInfo.builder()
                        .systemRole(SystemRole.CLINIC_MANAGER)
                        .build();

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(
                staffInfoRepository
                        .findFirstByProfile_Account_Username(
                                account.getUsername()
                        )
        ).thenReturn(Optional.of(manager));

        assertThrows(
                ConflictException.class,
                () -> accountService.lock(accountId)
        );

        assertTrue(account.getIsActive());

        verify(accountRepository, never())
                .save(account);
    }
}
