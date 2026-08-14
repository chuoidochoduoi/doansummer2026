package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.profile.*;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.ProfileServiceInterface;
import org.springframework.transaction.annotation.Transactional;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.ProfileServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.LocalDate;

import org.example.doansummer2026.model.TestRequest;

@Service
@Transactional
@RequiredArgsConstructor
public class ProfileService implements ProfileServiceInterface {

    private final ProfileRepository profileRepository;
    private final AccountRepository accountRepository;
    private final AppointmentRepository appointmentRepository;
    private final TestRequestService testRequestService;

    @Transactional(readOnly = true)
    public ProfileResponse get(UUID id) {
        return ProfileResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public ProfileResponse getByAccount(UUID accountId) {
        Profile p = profileRepository.findFirstByAccount_AccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không có hồ sơ cá nhân cho tài khoản: " + accountId));
        return ProfileResponse.from(p);
    }

    @Transactional(readOnly = true)
    public ProfileCustomerResponse getMyProfile(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));
        Profile profile = profileRepository.findFirstByAccount_AccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ cá nhân không tồn tại"));

        // Lay appointments
        List<Appointment> appointments = appointmentRepository.findByCustomerId(profile.getProfileId());
        List<ProfileCustomerResponse.AppointmentSummary> appointmentSummaries = appointments.stream()
                .map(a -> {
                    String doctor = a.getCustomer() != null && a.getCustomer().getFullName() != null
                            ? a.getCustomer().getFullName()
                            : null;
                    String specialty = a.getServices() != null && !a.getServices().isEmpty()
                            ? a.getServices().stream().findFirst().map(s -> s.getName()).orElse(null)
                            : null;
                    return new ProfileCustomerResponse.AppointmentSummary(
                            a.getScheduledAt().toLocalDate().toString(),
                            doctor,
                            specialty,
                            a.getStatus().name()
                    );
                })
                .toList();

        // Lay test results
        List<TestRequest> testRequests = testRequestService.findMyCompletedTests(profile.getProfileId());
        List<ProfileCustomerResponse.TestResultSummary> testResultSummaries = testRequests.stream()
                .map(t -> new ProfileCustomerResponse.TestResultSummary(
                        t.getService().getName(),
                        t.getCompletedAt() != null ? t.getCompletedAt().toLocalDate().toString() : null
                ))
                .collect(java.util.stream.Collectors.toList());

        return ProfileCustomerResponse.from(profile, account, appointmentSummaries, testResultSummaries);
    }

    public ProfileResponse create(ProfileCreateRequest req) {
        Account account = accountRepository.findById(req.accountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tài khoản không tồn tại: " + req.accountId()));
        if (profileRepository.findFirstByAccount_AccountId(account.getAccountId()).isPresent()) {
            throw new ConflictException("Tài khoản đã có hồ sơ cá nhân");
        }
        validateUnique(req.phone(), req.email(), null, null);

        Profile p = Profile.builder()
                .account(account)
                .fullName(req.fullName())
                .dateOfBirth(req.dateOfBirth())
                .gender(parseGender(req.gender()))
                .phone(req.phone())
                .email(req.email())
                .address(req.address())
                .build();
        return ProfileResponse.from(profileRepository.save(p));
    }

    public ProfileResponse update(UUID id, ProfileUpdateRequest req) {
        Profile p = findById(id);
        if (req.fullName() != null) p.setFullName(req.fullName().trim().replaceAll("\\s+", " "));
        if (req.dateOfBirth() != null) p.setDateOfBirth(req.dateOfBirth());
        if (req.gender() != null) p.setGender(parseGender(req.gender()));
        if (req.bloodType() != null) p.setBloodType(req.bloodType());
        if (req.address() != null) p.setAddress(blankToNull(req.address()));
        if (req.insuranceId() != null) p.setInsuranceId(blankToNull(req.insuranceId()));
        if (req.height() != null) p.setHeight(req.height());
        if (req.weight() != null) p.setWeight(req.weight());
        if (req.allergies() != null) {
            p.setAllergies(req.allergies().stream().map(String::trim)
                    .filter(value -> !value.isBlank()).distinct().collect(java.util.stream.Collectors.joining("\n")));
        }
        if (req.phone() != null || req.email() != null) {
            String newPhone = req.phone() != null ? blankToNull(req.phone()) : p.getPhone();
            String newEmail = req.email() != null ? normalizeEmail(req.email()) : p.getEmail();
            validateUnique(newPhone, newEmail, p.getProfileId(), null);
            if (req.phone() != null) p.setPhone(newPhone);
            if (req.email() != null) p.setEmail(newEmail);
        }
        validateUpdatedProfile(p);
        return ProfileResponse.from(profileRepository.save(p));
    }

    /**
     * Cập nhật hồ sơ của chính người dùng. Số điện thoại và email là thông tin
     * định danh/liên hệ do nhân viên có thẩm quyền cập nhật, không cho phép đổi
     * qua màn hồ sơ cá nhân.
     */
    public ProfileResponse updateSelf(UUID id, ProfileUpdateRequest req) {
        Profile current = findById(id);
        String requestedPhone = req.phone() == null ? current.getPhone() : blankToNull(req.phone());
        String requestedEmail = req.email() == null ? current.getEmail() : normalizeEmail(req.email());
        String currentEmail = normalizeEmail(current.getEmail());

        if (!Objects.equals(requestedPhone, current.getPhone())
                || !Objects.equals(requestedEmail, currentEmail)) {
            throw new BadRequestException(
                    "Số điện thoại và email chỉ được cập nhật bởi nhân viên lễ tân");
        }

        ProfileUpdateRequest safeRequest = new ProfileUpdateRequest(
                req.fullName(), req.dateOfBirth(), req.gender(),
                null, null, req.address(), req.bloodType(), req.insuranceId(),
                req.height(), req.weight(), req.allergies());
        return update(id, safeRequest);
    }

    private void validateUpdatedProfile(Profile profile) {
        if (profile.getFullName() == null || profile.getFullName().isBlank()
                || profile.getFullName().length() < 2) {
            throw new BadRequestException("Họ tên phải có ít nhất 2 ký tự");
        }
        if (profile.getFullName().codePoints().anyMatch(Character::isDigit)) {
            throw new BadRequestException("Họ tên không được chứa chữ số");
        }
        if (profile.getDateOfBirth() == null || !profile.getDateOfBirth().isBefore(LocalDate.now())) {
            throw new BadRequestException("Ngày sinh phải là ngày hợp lệ trong quá khứ");
        }
        if (profile.getGender() == null || profile.getGender() == Gender.OTHER) {
            throw new BadRequestException("Giới tính chỉ được chọn Nam hoặc Nữ");
        }
        boolean hasPhone = profile.getPhone() != null && !profile.getPhone().isBlank();
        boolean hasEmail = profile.getEmail() != null && !profile.getEmail().isBlank();
        if (!hasPhone && !hasEmail) {
            throw new BadRequestException("Vui lòng cung cấp số điện thoại hoặc email");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String normalizeEmail(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public void delete(UUID id) {
        if (!profileRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hồ sơ cá nhân không tồn tại: " + id);
        }
        throw new ConflictException("Không thể xóa hồ sơ cá nhân đã tạo. Vui lòng khóa tài khoản để ngừng sử dụng và giữ nguyên lịch sử");
    }

    @Transactional(readOnly = true)
    public PageResponse<ProfileResponse> search(String keyword, Pageable pageable) {
        Page<Profile> page = profileRepository.search(keyword, pageable);
        return PageResponse.from(page, ProfileResponse::from);
    }

    public Profile findById(UUID id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ cá nhân không tồn tại: " + id));
    }

    private void validateUnique(String phone, String email, UUID ignoreProfileId, UUID ignoreAccountId) {
        if (phone != null && !phone.isBlank()) {
            profileRepository.findFirstByPhone(phone).ifPresent(p -> {
                if (ignoreProfileId == null || !p.getProfileId().equals(ignoreProfileId)) {
                    throw new ConflictException("Số điện thoại đã được sử dụng");
                }
            });
        }
        if (email != null && !email.isBlank()) {
            profileRepository.findFirstByEmailIgnoreCase(email).ifPresent(p -> {
                if (ignoreProfileId == null || !p.getProfileId().equals(ignoreProfileId)) {
                    throw new ConflictException("Email đã được sử dụng");
                }
            });
        }
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
}
