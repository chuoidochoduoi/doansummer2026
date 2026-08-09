package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.profile.ProfileResponse;
import org.example.doansummer2026.dto.specialization.SpecializationResponse;
import org.example.doansummer2026.dto.staff.StaffCreateRequest;
import org.example.doansummer2026.dto.staff.StaffOptionResponse;
import org.example.doansummer2026.dto.staff.StaffResponse;
import org.example.doansummer2026.dto.staff.StaffUpdateRequest;
import org.example.doansummer2026.dto.staff.StaffProfessionalUpdateRequest;
import org.example.doansummer2026.dto.staff.StaffCapabilityRequest;
import org.example.doansummer2026.dto.staff.StaffCapabilityResponse;
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
import org.example.doansummer2026.repository.StaffCapabilityRepository;
import org.example.doansummer2026.repository.ServiceCapabilityRepository;
import org.example.doansummer2026.model.StaffCapability;
import org.example.doansummer2026.enums.StaffCapabilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.StaffServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.util.UUID;

import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.model.Department;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * Tao / cap nhat / xoa / truy van StaffInfo.
 * Quy trinh tao (1 transaction):
 *   1. validate unique (CCCD, license, phone, email, username)
 *   2. tao Account(role duoc chi dinh) - password BCrypt
 *   3. tao Profile lien ket Account
 *   4. tao StaffInfo lien ket Profile, Specialization
 * NOTE: StaffInfo khong con department - chi quan he Department.headDoctor toi StaffInfo.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StaffService implements StaffServiceInterface {

    private final StaffInfoRepository staffRepo;
    private final ProfileRepository profileRepo;
    private final AccountRepository accountRepo;
    private final DepartmentRepository departmentRepo;
    private final SpecializationService specializationService;
    private final PasswordEncoder passwordEncoder;
    private final StaffCapabilityRepository staffCapabilityRepo;
    private final ServiceCapabilityRepository capabilityRepo;

    public StaffResponse create(StaffCreateRequest req) {
        SystemRole systemRole = req.systemRole().normalized();
        // Validate unique
        if (accountRepo.existsByUsername(req.username())) {
            throw new ConflictException("Username da ton tai: " + req.username());
        }
        if (req.nationalId() != null && !req.nationalId().isBlank() && staffRepo.existsByNationalId(req.nationalId())) {
            throw new ConflictException("CCCD/CMND da ton tai: " + req.nationalId());
        }
        if (req.licenseNumber() != null && !req.licenseNumber().isBlank()
                && staffRepo.existsByLicenseNumber(req.licenseNumber())) {
            throw new ConflictException("So giay phep hanh nghe da ton tai");
        }
        if (profileRepo.findFirstByPhone(req.phone()).isPresent()) {
            throw new ConflictException("So dien thoai da duoc su dung");
        }
        if (profileRepo.findFirstByEmail(req.email()).isPresent()) {
            throw new ConflictException("Email da duoc su dung");
        }

        // Moi bac si deu phai khai bao pham vi chuyen khoa phuc vu.
        if (systemRole.isDoctor()
                && req.specializationId() == null) {
            throw new ConflictException("Bac si phai co specializationId; bac si da khoa chon chuyen khoa Kham tong quat");
        }

        Role accountRole = mapSystemRoleToRole(systemRole);

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
                .systemRole(systemRole)
                .nationalId(blankToNull(req.nationalId()))
                .bankAccount(req.bankAccount())
                .highestDegree(blankToNull(req.highestDegree()))
                .university(blankToNull(req.university()))
                .licenseNumber(blankToNull(req.licenseNumber()))
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

    public StaffResponse getByAccountId(UUID accountId) {
        StaffInfo s = staffRepo.findFirstByProfile_Account_AccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan su voi accountId=" + accountId));
        return toResponse(s);
    }

    public StaffResponse update(UUID staffId, StaffUpdateRequest req) {
        StaffInfo s = findById(staffId);
        
        // Update Account
        if (req.username() != null && !req.username().isBlank()) {
            s.getProfile().getAccount().setUsername(req.username());
        }

        // Update Profile
        if (req.fullName() != null) s.getProfile().setFullName(req.fullName());
        if (req.phone() != null) s.getProfile().setPhone(req.phone());
        if (req.email() != null) s.getProfile().setEmail(req.email());
        if (req.gender() != null) s.getProfile().setGender(parseGender(req.gender()));
        if (req.address() != null) s.getProfile().setAddress(req.address());

        // Update StaffInfo
        if (req.nationalId() != null && !req.nationalId().equals(s.getNationalId())) {
            if (!req.nationalId().isBlank() && staffRepo.existsByNationalId(req.nationalId())) {
                throw new ConflictException("CCCD/CMND da ton tai");
            }
        }
        s.setNationalId(blankToNull(req.nationalId()));
        if (req.licenseNumber() != null && !req.licenseNumber().equals(s.getLicenseNumber())) {
            if (!req.licenseNumber().isBlank() && staffRepo.existsByLicenseNumber(req.licenseNumber())) {
                throw new ConflictException("So giay phep hanh nghe da ton tai");
            }
        }
        s.setLicenseNumber(blankToNull(req.licenseNumber()));
        if (req.systemRole() != null) s.setSystemRole(req.systemRole().normalized());
        if (req.bankAccount() != null) s.setBankAccount(req.bankAccount());
        if (req.highestDegree() != null) s.setHighestDegree(blankToNull(req.highestDegree()));
        if (req.university() != null) s.setUniversity(blankToNull(req.university()));
        if (req.specializationId() != null) {
            s.setSpecialization(specializationService.findById(req.specializationId()));
        }
        boolean isDoctor = s.getSystemRole().isDoctor();
        if (isDoctor && s.getSpecialization() == null) {
            throw new ConflictException("Bac si phai co chuyen khoa phuc vu");
        }
        return toResponse(staffRepo.save(s));
    }

    public StaffResponse updateOwnProfessionalInfo(UUID staffId, StaffProfessionalUpdateRequest req) {
        StaffInfo staff = findById(staffId);
        staff.setHighestDegree(blankToNull(req.highestDegree()));
        staff.setUniversity(blankToNull(req.university()));
        return toResponse(staffRepo.save(staff));
    }

    public void delete(UUID staffId) {
        if (!staffRepo.existsById(staffId)) {
            throw new ResourceNotFoundException("Nhan vien khong ton tai: " + staffId);
        }
        staffRepo.deleteById(staffId);
    }

    /**
     * Khoa tai khoan nhan su (set isActive = false).
     * KHONG cho phep khoa tai khoan ADMIN hoac CLINIC_MANAGER.
     */
    public StaffResponse lock(UUID staffId) {
        StaffInfo s = findById(staffId);
        if (s.getSystemRole() == SystemRole.ADMIN || s.getSystemRole() == SystemRole.CLINIC_MANAGER) {
            throw new ConflictException("Khong the khoa tai khoan ADMIN hoac CLINIC_MANAGER");
        }
        Account account = s.getProfile().getAccount();
        account.setIsActive(false);
        accountRepo.save(account);
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<StaffResponse> search(String search, UUID specializationId,
                                              SystemRole systemRole, Pageable pageable) {
        Page<StaffInfo> page = staffRepo.search(search, specializationId, systemRole, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    public StaffInfo findById(UUID id) {
        return staffRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai: " + id));
    }

    @Transactional(readOnly = true)
    public List<StaffCapabilityResponse> listCapabilities(UUID staffId) {
        findById(staffId);
        return staffCapabilityRepo.findAllByStaff_StaffId(staffId).stream()
                .map(StaffCapabilityResponse::from).toList();
    }

    public List<StaffCapabilityResponse> replaceCapabilities(UUID staffId, List<StaffCapabilityRequest> requests) {
        StaffInfo staff = findById(staffId);
        if (!staff.getSystemRole().isDoctor()) throw new ConflictException("Chi bac si moi duoc cap nang luc thuc hien");
        staffCapabilityRepo.deleteAllByStaff_StaffId(staffId);
        List<StaffCapabilityRequest> unique = (requests == null ? List.<StaffCapabilityRequest>of() : requests).stream()
                .filter(request -> request.capabilityId() != null)
                .collect(java.util.stream.Collectors.toMap(StaffCapabilityRequest::capabilityId,
                        request -> request, (first, ignored) -> first)).values().stream().toList();
        List<StaffCapability> values = unique.stream().map(request -> StaffCapability.builder()
                .staff(staff)
                .capability(capabilityRepo.findById(request.capabilityId()).orElseThrow(() ->
                        new ResourceNotFoundException("Nang luc khong ton tai: " + request.capabilityId())))
                .certificateNumber(blankToNull(request.certificateNumber()))
                .issuedDate(request.issuedDate()).expiryDate(request.expiryDate())
                .issuingOrganization(blankToNull(request.issuingOrganization()))
                .status(request.status() != null ? request.status() : StaffCapabilityStatus.ACTIVE)
                .build()).toList();
        return staffCapabilityRepo.saveAll(values).stream().map(StaffCapabilityResponse::from).toList();
    }

    /**
     * Danh sach nhan su cho Schedule (khong phan trang).
     */
    @Transactional(readOnly = true)
    public List<StaffOptionResponse> listForSchedule(SystemRole systemRole) {
        List<StaffInfo> staff;
        if (systemRole != null) {
            staff = systemRole.isDoctor()
                    ? staffRepo.findAllBySystemRoleIn(doctorRoles())
                    : staffRepo.findAllBySystemRoleIn(List.of(systemRole));
        } else {
            staff = staffRepo.findAll();
        }
        return staff.stream().map(StaffOptionResponse::from).toList();
    }

    /**
     * Lay danh sach tat ca bac si de chon lam head doctor.
     */
    @Transactional(readOnly = true)
    public List<StaffOptionResponse> findAllDoctors() {
        List<StaffInfo> doctors = staffRepo.findAllBySystemRoleIn(doctorRoles());
                
        List<Department> allDepts = departmentRepo.findAll();
        Map<UUID, UUID> doctorToDeptMap = allDepts.stream()
            .filter(d -> d.getHeadDoctor() != null)
            .collect(Collectors.toMap(d -> d.getHeadDoctor().getStaffId(), Department::getDepartmentId, (a, b) -> a));

        return doctors.stream().map(d -> StaffOptionResponse.from(d, doctorToDeptMap.get(d.getStaffId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<StaffOptionResponse> findAllNurses() {
        List<StaffInfo> nurses = staffRepo.findAllBySystemRoleIn(
                List.of(SystemRole.NURSE));
        return nurses.stream().map(n -> StaffOptionResponse.from(n, n.getDepartment() != null ? n.getDepartment().getDepartmentId() : null)).toList();
    }

    private StaffResponse toResponse(StaffInfo s) {
        ProfileResponse p = ProfileResponse.from(s.getProfile());
        SpecializationResponse sp = s.getSpecialization() != null
                ? SpecializationResponse.from(s.getSpecialization()) : null;
        return StaffResponse.from(s, p, sp);
    }

    private Gender parseGender(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            Gender gender = Gender.valueOf(raw.trim().toUpperCase());
            if (gender == Gender.OTHER) throw new IllegalArgumentException();
            return gender;
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Gender khong hop le: " + raw);
        }
    }

    private Role mapSystemRoleToRole(SystemRole systemRole) {
        return Role.STAFF;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private List<SystemRole> doctorRoles() {
        return List.of(SystemRole.DOCTOR, SystemRole.GENERAL_DOCTOR, SystemRole.SPECIALIST_DOCTOR);
    }
}
