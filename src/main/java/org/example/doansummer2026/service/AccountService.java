package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.account.AccountManagementResponse;
import org.example.doansummer2026.dto.account.AccountUpdateRequest;
import org.example.doansummer2026.dto.account.AccountResponse;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.service.interfaces.AccountServiceInterface;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountService implements AccountServiceInterface {

    private final AccountRepository accountRepository;
    private final StaffInfoRepository staffRepo;
    private final ProfileRepository profileRepo;
    private final PasswordEncoder passwordEncoder;

    public Account create(String username, String rawPassword, Role role) {
        if (accountRepository.existsByUsername(username)) {
            throw new ConflictException("Username da ton tai: " + username);
        }
        Account a = Account.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .isActive(true)
                .build();
        return accountRepository.save(a);
    }

    @Transactional(readOnly = true)
    public Account findById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay account id=" + id));
    }

    @Transactional(readOnly = true)
    public Account findByUsername(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay username=" + username));
    }

    public Account update(UUID id, AccountUpdateRequest req) {
        Account a = findById(id);
        if (req.username() != null && !req.username().equals(a.getUsername())) {
            if (accountRepository.existsByUsername(req.username())) {
                throw new ConflictException("Username da ton tai: " + req.username());
            }
            a.setUsername(req.username());
        }
        if (req.role() != null) {
            a.setRole(req.role());
        }
        if (req.isActive() != null) {
            a.setIsActive(req.isActive());
        }
        return accountRepository.save(a);
    }

    public void changePassword(UUID id, String oldRaw, String newRaw) {
        Account a = findById(id);
        if (!passwordEncoder.matches(oldRaw, a.getPasswordHash())) {
            throw new ConflictException("Mat khau cu khong dung");
        }
        a.setPasswordHash(passwordEncoder.encode(newRaw));
        accountRepository.save(a);
    }

    public void forceChangePassword(UUID id, String newRaw) {
        Account a = findById(id);
        a.setPasswordHash(passwordEncoder.encode(newRaw));
        accountRepository.save(a);
    }

    public void adminResetPassword(UUID id, String newRaw) {
        Account a = findById(id);
        a.setPasswordHash(passwordEncoder.encode(newRaw));
        accountRepository.save(a);
    }

    public void softDelete(UUID id) {
        Account a = findById(id);
        a.setIsActive(false);
        accountRepository.save(a);
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> list(Role role, Pageable pageable) {
        Page<Account> page = (role != null)
                ? accountRepository.findByRole(role, pageable)
                : accountRepository.findAll(pageable);
        return PageResponse.from(page, a -> {
            SystemRole sr = staffRepo.findByProfile_Account_Username(a.getUsername())
                    .map(staff -> staff.getSystemRole())
                    .orElse(null);
            return AccountResponse.from(a, sr);
        });
    }

    /**
     * Danh sach tai khoan nhan su (STAFF) voi thong tin StaffInfo.
     * NOTE: StaffInfo khong con department - department chi quan he voi head_doctor o Department.
     */
    @Transactional(readOnly = true)
    public PageResponse<AccountManagementResponse> listStaff(String search, SystemRole systemRole, Pageable pageable) {
        Page<org.example.doansummer2026.model.StaffInfo> page = staffRepo.search(search, null, systemRole, pageable);
        return PageResponse.from(page, staff -> {
            Account a = staff.getProfile().getAccount();
            return AccountManagementResponse.forStaff(
                    a,
                    staff.getStaffCode(),
                    staff.getProfile().getFullName(),
                    null, // department da xoa khoi StaffInfo
                    staff.getSystemRole()
            );
        });
    }

    /**
     * Danh sach tai khoan khach hang (CUSTOMER).
     */
    @Transactional(readOnly = true)
    public PageResponse<AccountManagementResponse> listCustomers(String search, String status, Pageable pageable) {
        Boolean isActive = null;
        if ("active".equalsIgnoreCase(status)) {
            isActive = true;
        } else if ("locked".equalsIgnoreCase(status)) {
            isActive = false;
        }
        
        Page<Account> page = accountRepository.searchCustomers(search, isActive, pageable);
        return PageResponse.from(page, a -> {
            var profileOpt = profileRepo.findByAccount_AccountId(a.getAccountId());
            String fullName = profileOpt.map(p -> p.getFullName()).orElse(a.getUsername());
            return AccountManagementResponse.forCustomer(a, fullName);
        });
    }

    /**
     * Khoa tai khoan. KHONG cho phep khoa tai khoan ADMIN hoac CLINIC_MANAGER.
     */
    public Account lock(UUID id) {
        Account a = findById(id);
        // Kiem tra xem co phai admin/clinic_manager khong
        var staffOpt = staffRepo.findByProfile_Account_Username(a.getUsername());
        if (staffOpt.isPresent()) {
            SystemRole sr = staffOpt.get().getSystemRole();
            if (sr == SystemRole.ADMIN || sr == SystemRole.CLINIC_MANAGER) {
                throw new ConflictException("Khong the khoa tai khoan ADMIN hoac CLINIC_MANAGER");
            }
        }
        a.setIsActive(false);
        return accountRepository.save(a);
    }
}



