package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.profile.ProfileResponse;
import vn.edu.fpt.cares.dto.specialization.SpecializationResponse;
import vn.edu.fpt.cares.dto.staff.StaffCreateRequest;
import vn.edu.fpt.cares.dto.staff.StaffOptionResponse;
import vn.edu.fpt.cares.dto.staff.StaffResponse;
import vn.edu.fpt.cares.dto.staff.StaffUpdateRequest;
import vn.edu.fpt.cares.dto.staff.StaffProfessionalUpdateRequest;
import vn.edu.fpt.cares.dto.staff.StaffCapabilityRequest;
import vn.edu.fpt.cares.dto.staff.StaffCapabilityResponse;
import vn.edu.fpt.cares.dto.staff.ClinicManagerStaffResponse;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.Account;
import vn.edu.fpt.cares.enums.Gender;
import vn.edu.fpt.cares.model.Profile;
import vn.edu.fpt.cares.enums.Role;
import vn.edu.fpt.cares.model.StaffInfo;
import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.repository.AccountRepository;
import vn.edu.fpt.cares.repository.ProfileRepository;
import vn.edu.fpt.cares.repository.StaffInfoRepository;
import vn.edu.fpt.cares.repository.StaffCapabilityRepository;
import vn.edu.fpt.cares.repository.ServiceCapabilityRepository;
import vn.edu.fpt.cares.model.StaffCapability;
import vn.edu.fpt.cares.enums.StaffCapabilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.fpt.cares.service.interfaces.StaffServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.util.Comparator;
import java.util.UUID;

import vn.edu.fpt.cares.repository.DepartmentRepository;
import vn.edu.fpt.cares.model.Department;
import java.util.Map;
import java.util.Locale;

/**
 * Tao / cap nhat / xoa / truy van StaffInfo.
 * Quy trinh tao (1 transaction):
 *   1. validate unique (CCCD, license, phone, email, username)
 *   2. tao Account(role duoc chi dinh) - password BCrypt
 *   3. tao Profile lien ket Account
 *   4. tao StaffInfo lien ket Profile, Specialization
 * StaffInfo.department la phong chuyen mon chinh cua nhan su; Department.headDoctor
 * chi la thong tin phu trach chuyen mon.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StaffService implements StaffServiceInterface {

    private static final java.time.ZoneId CLINIC_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

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
        String username = req.username().trim();
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String phone = req.phone().trim();
        // Validate unique
        if (accountRepo.existsByUsername(username)) {
            throw new ConflictException("Tên đăng nhập đã tồn tại: " + username);
        }
        if (req.nationalId() != null && !req.nationalId().isBlank() && staffRepo.existsByNationalId(req.nationalId())) {
            throw new ConflictException("CCCD/CMND đã tồn tại: " + req.nationalId());
        }
        if (req.licenseNumber() != null && !req.licenseNumber().isBlank()
                && staffRepo.existsByLicenseNumber(req.licenseNumber())) {
            throw new ConflictException("Số giấy phép hành nghề đã tồn tại");
        }
        if (profileRepo.findFirstByPhone(phone).isPresent()) {
            throw new ConflictException("Số điện thoại đã được sử dụng");
        }
        if (profileRepo.findFirstByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("Email đã được sử dụng");
        }

        // Moi bac si deu phai khai bao pham vi chuyen khoa phuc vu.
        if (systemRole.isDoctor()
                && req.specializationId() == null) {
            throw new ConflictException("Bác sĩ phải có chuyên khoa phục vụ; bác sĩ đa khoa chọn chuyên khoa Khám tổng quát");
        }

        Role accountRole = mapSystemRoleToRole(systemRole);

        Account account = Account.builder()
                .username(username)
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
                .phone(phone)
                .email(email)
                .address(blankToNull(req.address()))
                .avatarUrl(blankToNull(req.avatarUrl()))
                .build();
        profile = profileRepo.save(profile);

        StaffInfo staff = StaffInfo.builder()
                .profile(profile)
                .systemRole(systemRole)
                .nationalId(blankToNull(req.nationalId()))
                .highestDegree(blankToNull(req.highestDegree()))
                .university(blankToNull(req.university()))
                .licenseNumber(blankToNull(req.licenseNumber()))
                .specialization(req.specializationId() != null
                        ? specializationService.findActiveById(req.specializationId()) : null)
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự của tài khoản: " + accountId));
        return toResponse(s);
    }

    public StaffResponse update(UUID staffId, StaffUpdateRequest req) {
        StaffInfo s = findById(staffId);
        Account account = s.getProfile().getAccount();
        Profile profile = s.getProfile();
        
        // Update Account
        if (req.username() != null && !req.username().isBlank()) {
            String username = req.username().trim();
            accountRepo.findFirstByUsername(username).ifPresent(existing -> {
                if (!existing.getAccountId().equals(account.getAccountId())) {
                    throw new ConflictException("Tên đăng nhập đã được sử dụng");
                }
            });
            account.setUsername(username);
        }

        // Update Profile
        if (req.fullName() != null) profile.setFullName(req.fullName().trim().replaceAll("\\s+", " "));
        if (req.phone() != null) {
            String phone = blankToNull(req.phone());
            if (phone != null) {
                profileRepo.findFirstByPhone(phone).ifPresent(existing -> {
                    if (!existing.getProfileId().equals(profile.getProfileId())) {
                        throw new ConflictException("Số điện thoại đã được sử dụng");
                    }
                });
            }
            profile.setPhone(phone);
        }
        if (req.email() != null) {
            String email = blankToNull(req.email());
            if (email != null) {
                email = email.toLowerCase(Locale.ROOT);
                String normalizedEmail = email;
                profileRepo.findFirstByEmailIgnoreCase(normalizedEmail).ifPresent(existing -> {
                    if (!existing.getProfileId().equals(profile.getProfileId())) {
                        throw new ConflictException("Email đã được sử dụng");
                    }
                });
            }
            profile.setEmail(email);
        }
        if (req.dateOfBirth() != null) profile.setDateOfBirth(req.dateOfBirth());
        if (req.gender() != null) profile.setGender(parseGender(req.gender()));
        if (req.address() != null) profile.setAddress(blankToNull(req.address()));
        if (req.avatarUrl() != null) profile.setAvatarUrl(blankToNull(req.avatarUrl()));

        if (profile.getFullName() == null || profile.getFullName().isBlank()) {
            throw new BadRequestException("Họ tên không được để trống");
        }
        if (profile.getFullName().codePoints().anyMatch(Character::isDigit)) {
            throw new BadRequestException("Họ tên không được chứa chữ số");
        }
        if (profile.getDateOfBirth() == null
                || !profile.getDateOfBirth().isBefore(java.time.LocalDate.now(CLINIC_ZONE))) {
            throw new BadRequestException("Ngày sinh phải là ngày hợp lệ trong quá khứ");
        }
        if (profile.getGender() == null || profile.getGender() == Gender.OTHER) {
            throw new BadRequestException("Giới tính chỉ được chọn Nam hoặc Nữ");
        }
        if ((profile.getPhone() == null || profile.getPhone().isBlank())
                && (profile.getEmail() == null || profile.getEmail().isBlank())) {
            throw new BadRequestException("Vui lòng cung cấp số điện thoại hoặc email");
        }

        // Update StaffInfo
        if (req.nationalId() != null && !req.nationalId().equals(s.getNationalId())) {
            if (!req.nationalId().isBlank() && staffRepo.existsByNationalId(req.nationalId())) {
                throw new ConflictException("CCCD/CMND đã tồn tại");
            }
        }
        s.setNationalId(blankToNull(req.nationalId()));
        if (req.licenseNumber() != null && !req.licenseNumber().equals(s.getLicenseNumber())) {
            if (!req.licenseNumber().isBlank() && staffRepo.existsByLicenseNumber(req.licenseNumber())) {
                throw new ConflictException("Số giấy phép hành nghề đã tồn tại");
            }
        }
        s.setLicenseNumber(blankToNull(req.licenseNumber()));
        if (req.systemRole() != null && req.systemRole().normalized() != s.getSystemRole().normalized()) {
            throw new ConflictException("Không được đổi vai trò của nhân sự sau khi đã tạo tài khoản");
        }
        if (req.highestDegree() != null) s.setHighestDegree(blankToNull(req.highestDegree()));
        if (req.university() != null) s.setUniversity(blankToNull(req.university()));
        if (req.specializationId() != null) {
            UUID currentSpecializationId = s.getSpecialization() == null
                    ? null : s.getSpecialization().getSpecializationId();
            if (!req.specializationId().equals(currentSpecializationId)) {
                throw new ConflictException("Không được đổi chuyên khoa của nhân sự sau khi đã tạo tài khoản");
            }
        }
        boolean isDoctor = s.getSystemRole().isDoctor();
        if (isDoctor && s.getSpecialization() == null) {
            throw new ConflictException("Bác sĩ phải có chuyên khoa phục vụ");
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
            throw new ResourceNotFoundException("Nhân viên không tồn tại: " + staffId);
        }
        throw new ConflictException("Không thể xóa nhân sự đã tạo. Vui lòng khóa tài khoản sau khi đã gỡ lịch trực, phòng phụ trách và ca đang xử lý");
    }

    /**
     * Khoa tai khoan nhan su (set isActive = false).
     * KHONG cho phep khoa tai khoan ADMIN hoac CLINIC_MANAGER.
     */
    public StaffResponse lock(UUID staffId) {
        StaffInfo s = findById(staffId);
        if (s.getSystemRole() == SystemRole.ADMIN || s.getSystemRole() == SystemRole.CLINIC_MANAGER) {
            throw new ConflictException("Không thể khóa tài khoản quản trị viên hoặc quản lý phòng khám");
        }
        if (staffRepo.countBlockingLockReferences(staffId) > 0) {
            throw new ConflictException(
                    "Không thể khóa nhân sự khi còn phòng phụ trách, phòng được phân công, lịch trực tương lai hoặc hồ sơ đang xử lý. "
                            + "Vui lòng xử lý các phân công liên quan trước."
            );
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

    @Transactional(readOnly = true)
    public PageResponse<ClinicManagerStaffResponse> searchForClinicManager(String search, Pageable pageable) {
        Page<StaffInfo> page = staffRepo.search(search, null, null, pageable);
        return PageResponse.from(page, ClinicManagerStaffResponse::from);
    }

    @Transactional(readOnly = true)
    public ClinicManagerStaffResponse getForClinicManager(UUID staffId) {
        return ClinicManagerStaffResponse.from(findById(staffId));
    }

    public StaffInfo findById(UUID id) {
        return staffRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại: " + id));
    }

    @Transactional(readOnly = true)
    public List<StaffCapabilityResponse> listCapabilities(UUID staffId) {
        findById(staffId);
        return staffCapabilityRepo.findAllByStaff_StaffId(staffId).stream()
                .map(StaffCapabilityResponse::from).toList();
    }

    public List<StaffCapabilityResponse> replaceCapabilities(UUID staffId, List<StaffCapabilityRequest> requests) {
        StaffInfo staff = findById(staffId);
        if (!staff.getSystemRole().isDoctor()) throw new ConflictException("Chỉ bác sĩ mới được cấp kỹ thuật chuyên môn");
        staffCapabilityRepo.deleteAllByStaff_StaffId(staffId);
        List<StaffCapabilityRequest> unique = (requests == null ? List.<StaffCapabilityRequest>of() : requests).stream()
                .filter(request -> request.capabilityId() != null)
                .collect(java.util.stream.Collectors.toMap(StaffCapabilityRequest::capabilityId,
                        request -> request, (first, ignored) -> first)).values().stream().toList();
        List<StaffCapability> values = unique.stream().map(request -> StaffCapability.builder()
                .staff(staff)
                .capability(findActiveCapability(request.capabilityId()))
                .certificateNumber(blankToNull(request.certificateNumber()))
                .issuedDate(request.issuedDate()).expiryDate(request.expiryDate())
                .issuingOrganization(blankToNull(request.issuingOrganization()))
                .status(request.status() != null ? request.status() : StaffCapabilityStatus.ACTIVE)
                .build()).toList();
        return staffCapabilityRepo.saveAll(values).stream().map(StaffCapabilityResponse::from).toList();
    }

    private vn.edu.fpt.cares.model.ServiceCapability findActiveCapability(UUID capabilityId) {
        vn.edu.fpt.cares.model.ServiceCapability capability = capabilityRepo.findById(capabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục kỹ thuật không tồn tại: " + capabilityId));
        if (Boolean.FALSE.equals(capability.getActive())) {
            throw new ConflictException("Danh mục kỹ thuật đã ngừng hoạt động và không thể cấp cho nhân sự");
        }
        return capability;
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
        return staff.stream()
                .filter(item -> item.getProfile() != null
                        && item.getProfile().getAccount() != null
                        && Boolean.TRUE.equals(item.getProfile().getAccount().getIsActive()))
                .map(this::toStaffOption)
                .toList();
    }

    /**
     * Lay danh sach tat ca bac si de chon lam head doctor.
     */
    @Transactional(readOnly = true)
    public List<StaffOptionResponse> findAllDoctors() {
        List<StaffInfo> doctors = staffRepo.findAllBySystemRoleIn(doctorRoles());
        return doctors.stream()
                .filter(this::isActiveStaff)
                .map(this::toStaffOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffOptionResponse> findAllNurses() {
        List<StaffInfo> nurses = staffRepo.findAllBySystemRoleIn(
                List.of(SystemRole.NURSE));
        return nurses.stream().filter(this::isActiveStaff).map(this::toStaffOption).toList();
    }

    /** Danh sách đầy đủ bác sĩ đang hoạt động cho khu vực công khai. */
    @Transactional(readOnly = true)
    public List<StaffOptionResponse> getPublicActiveDoctors() {
        return staffRepo.findAllBySystemRoleIn(doctorRoles()).stream()
                .filter(this::isActiveStaff)
                .sorted(Comparator
                        .comparing((StaffInfo staff) -> staff.getSpecialization() == null
                                        || staff.getSpecialization().getName() == null
                                        ? "" : staff.getSpecialization().getName(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(staff -> staff.getProfile() == null
                                        || staff.getProfile().getFullName() == null
                                        ? "" : staff.getProfile().getFullName(),
                                String.CASE_INSENSITIVE_ORDER))
                .map(this::toStaffOption)
                .toList();
    }

    private boolean isActiveStaff(StaffInfo staff) {
        return staff.getProfile() != null && staff.getProfile().getAccount() != null
                && Boolean.TRUE.equals(staff.getProfile().getAccount().getIsActive());
    }

    private StaffOptionResponse toStaffOption(StaffInfo staff) {
        List<UUID> capabilityIds = staffCapabilityRepo
                .findAllByStaff_StaffIdAndStatus(staff.getStaffId(), StaffCapabilityStatus.ACTIVE)
                .stream()
                .filter(value -> value.getCapability() != null)
                .map(value -> value.getCapability().getCapabilityId())
                .distinct()
                .toList();
        return StaffOptionResponse.from(
                staff,
                staff.getDepartment() != null ? staff.getDepartment().getDepartmentId() : null,
                capabilityIds);
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
            throw new ConflictException("Giới tính không hợp lệ: " + raw);
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
