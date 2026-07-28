package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.profile.*;
import org.example.doansummer2026.exception.ConflictException;
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
        Profile p = profileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong co profile cho account id=" + accountId));
        return ProfileResponse.from(p);
    }

    @Transactional(readOnly = true)
    public ProfileCustomerResponse getMyProfile(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account khong ton tai"));
        Profile profile = profileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile khong ton tai"));

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
                        "Account khong ton tai: " + req.accountId()));
        if (profileRepository.findByAccount_AccountId(account.getAccountId()).isPresent()) {
            throw new ConflictException("Account da co profile");
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
        if (req.fullName() != null) p.setFullName(req.fullName());
        if (req.dateOfBirth() != null) p.setDateOfBirth(req.dateOfBirth());
        if (req.gender() != null) p.setGender(parseGender(req.gender()));
        if (req.bloodType() != null) p.setBloodType(req.bloodType());
        if (req.address() != null) p.setAddress(req.address());
        if (req.phone() != null || req.email() != null) {
            String newPhone = req.phone() != null ? req.phone() : p.getPhone();
            String newEmail = req.email() != null ? req.email() : p.getEmail();
            validateUnique(newPhone, newEmail, p.getProfileId(), null);
            if (req.phone() != null) p.setPhone(req.phone());
            if (req.email() != null) p.setEmail(req.email());
        }
        return ProfileResponse.from(profileRepository.save(p));
    }

    public void delete(UUID id) {
        if (!profileRepository.existsById(id)) {
            throw new ResourceNotFoundException("Profile khong ton tai: " + id);
        }
        profileRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProfileResponse> search(String keyword, Pageable pageable) {
        Page<Profile> page = profileRepository.search(keyword, pageable);
        return PageResponse.from(page, ProfileResponse::from);
    }

    public Profile findById(UUID id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile khong ton tai: " + id));
    }

    private void validateUnique(String phone, String email, UUID ignoreProfileId, UUID ignoreAccountId) {
        profileRepository.findByPhone(phone).ifPresent(p -> {
            if (ignoreProfileId == null || !p.getProfileId().equals(ignoreProfileId)) {
                throw new ConflictException("So dien thoai da duoc su dung");
            }
        });
        profileRepository.findByEmail(email).ifPresent(p -> {
            if (ignoreProfileId == null || !p.getProfileId().equals(ignoreProfileId)) {
                throw new ConflictException("Email da duoc su dung");
            }
        });
    }

    private Gender parseGender(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Gender.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Gender khong hop le: " + raw);
        }
    }
}



