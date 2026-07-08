package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.department.DepartmentResponse;
import org.example.doansummer2026.dto.profile.ProfileResponse;
import org.example.doansummer2026.dto.specialization.SpecializationResponse;
import org.example.doansummer2026.dto.staff.StaffCreateRequest;
import org.example.doansummer2026.dto.staff.StaffResponse;
import org.example.doansummer2026.dto.staff.StaffUpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.StaffServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Tao / cap nhat / xoa / truy van StaffInfo.
 * Quy trinh tao (1 transaction):
 *   1. validate unique (CCCD, license, phone, email, username)
 *   2. tao Account(role=STAFF) - password BCrypt
 *   3. tao Profile lien ket Account
 *   4. tao StaffInfo lien ket Profile, Department, Specialization
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StaffService implements StaffServiceInterface {

    private final StaffInfoRepository staffRepo;
    private final ProfileRepository profileRepo;
    private final AccountRepository accountRepo;
    private final DepartmentService departmentService;
    private final SpecializationService specializationService;
    private final PasswordEncoder passwordEncoder;

    public StaffResponse create(StaffCreateRequest req) {
        // Validate unique
        if (accountRepo.existsByUsername(req.username())) {
            throw new ConflictException("Username da ton tai: " + req.username());
        }
        if (staffRepo.existsByNationalId(req.nationalId())) {
            throw new ConflictException("CCCD/CMND da ton tai: " + req.nationalId());
        }
        if (req.licenseNumber() != null && !req.licenseNumber().isBlank()
                && staffRepo.existsByLicenseNumber(req.licenseNumber())) {
            throw new ConflictException("So giay phep hanh nghe da ton tai");
        }
        if (profileRepo.findByPhone(req.phone()).isPresent()) {
            throw new ConflictException("So dien thoai da duoc su dung");
        }
        if (profileRepo.findByEmail(req.email()).isPresent()) {
            throw new ConflictException("Email da duoc su dung");
        }

        Role accountRole = mapSystemRoleToRole(req.systemRole());

        Account account = Account.builder()
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(accountRole)
                .isActive(true)
                .build();
        account = accountRepo.save(account);

        Profile profile = Profile.builder()
                .account(account)
                .fullName(req.fullName())
                .dateOfBirth(req.dateOfBirth())
                .gender(parseGender(req.gender()))
                .phone(req.phone())
                .email(req.email())
                .address(req.address())
                .build();
        profile = profileRepo.save(profile);

        StaffInfo staff = StaffInfo.builder()
                .profile(profile)
                .systemRole(req.systemRole())
                .nationalId(req.nationalId())
                .bankAccount(req.bankAccount())
                .highestDegree(req.highestDegree())
                .university(req.university())
                .licenseNumber(req.licenseNumber())
                .department(req.departmentId() != null
                        ? departmentService.findById(req.departmentId()) : null)
                .specialization(req.specializationId() != null
                        ? specializationService.findById(req.specializationId()) : null)
                .build();
        staff = staffRepo.save(staff);
        return toResponse(staff);
    }

    @Transactional(readOnly = true)
    public StaffResponse get(UUID staffId) {
        return toResponse(findById(staffId));
    }

    public StaffResponse update(UUID staffId, StaffUpdateRequest req) {
        StaffInfo s = findById(staffId);
        if (req.nationalId() != null && !req.nationalId().equals(s.getNationalId())) {
            if (staffRepo.existsByNationalId(req.nationalId())) {
                throw new ConflictException("CCCD/CMND da ton tai");
            }
            s.setNationalId(req.nationalId());
        }
        if (req.licenseNumber() != null && !req.licenseNumber().equals(s.getLicenseNumber())) {
            if (!req.licenseNumber().isBlank() && staffRepo.existsByLicenseNumber(req.licenseNumber())) {
                throw new ConflictException("So giay phep hanh nghe da ton tai");
            }
            s.setLicenseNumber(req.licenseNumber());
        }
        if (req.systemRole() != null) s.setSystemRole(req.systemRole());
        if (req.bankAccount() != null) s.setBankAccount(req.bankAccount());
        if (req.highestDegree() != null) s.setHighestDegree(req.highestDegree());
        if (req.university() != null) s.setUniversity(req.university());
        if (req.departmentId() != null) {
            s.setDepartment(departmentService.findById(req.departmentId()));
        }
        if (req.specializationId() != null) {
            s.setSpecialization(specializationService.findById(req.specializationId()));
        }
        return toResponse(staffRepo.save(s));
    }

    public void delete(UUID staffId) {
        if (!staffRepo.existsById(staffId)) {
            throw new ResourceNotFoundException("Nhan vien khong ton tai: " + staffId);
        }
        staffRepo.deleteById(staffId);
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> search(UUID departmentId, UUID specializationId,
                                              SystemRole systemRole, Pageable pageable) {
        Page<StaffInfo> page = staffRepo.search(departmentId, specializationId, systemRole, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    public StaffInfo findById(UUID id) {
        return staffRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai: " + id));
    }

    private StaffResponse toResponse(StaffInfo s) {
        ProfileResponse p = ProfileResponse.from(s.getProfile());
        DepartmentResponse d = s.getDepartment() != null
                ? DepartmentResponse.from(s.getDepartment()) : null;
        SpecializationResponse sp = s.getSpecialization() != null
                ? SpecializationResponse.from(s.getSpecialization()) : null;
        return StaffResponse.from(s, p, d, sp);
    }

    private Gender parseGender(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Gender.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Gender khong hop le: " + raw);
        }
    }

    private Role mapSystemRoleToRole(SystemRole systemRole) {
        return switch (systemRole) {
            case GENERAL_DOCTOR, SPECIALIST_DOCTOR -> Role.DOCTOR;
            case NURSE -> Role.NURSE;
            case RECEPTIONIST -> Role.RECEPTIONIST;
            case CASHIER -> Role.CASHIER;
            case CLINIC_MANAGER -> Role.ADMIN;
        };
    }
}