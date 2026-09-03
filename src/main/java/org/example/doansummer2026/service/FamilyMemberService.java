package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.family.FamilyMemberRequest;
import org.example.doansummer2026.dto.family.FamilyMemberResponse;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.AllergyStatus;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.FamilyMember;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.FamilyMemberRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FamilyMemberService {

    private final FamilyAccessService familyAccessService;
    private final FamilyMemberRepository familyMemberRepository;
    private final ProfileRepository profileRepository;
    private final AppointmentRepository appointmentRepository;
    private final CustomerVisitRepository customerVisitRepository;

    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> list(UUID accountId, boolean includeInactive) {
        Profile owner = familyAccessService.ownerProfile(accountId);
        List<FamilyMember> items = includeInactive
                ? familyMemberRepository.findAllByOwnerProfile_ProfileIdOrderByCreatedAtAsc(owner.getProfileId())
                : familyMemberRepository.findAllByOwnerProfile_ProfileIdAndIsActiveTrueOrderByCreatedAtAsc(owner.getProfileId());
        return items.stream().map(FamilyMemberResponse::from).toList();
    }

    public FamilyMemberResponse create(UUID accountId, FamilyMemberRequest request) {
        validateRequest(request);
        Profile owner = familyAccessService.ownerProfile(accountId);
        ensureNotDuplicate(owner, null, request);

        Profile member = Profile.builder()
                .fullName(clean(request.fullName()))
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .address(emptyToNull(request.address()))
                .bloodType(request.bloodType())
                .allergies(allergyStorage(request))
                .build();
        member = profileRepository.save(member);

        FamilyMember relation = FamilyMember.builder()
                .ownerProfile(owner)
                .memberProfile(member)
                .relationship(request.relationship())
                .isActive(true)
                .build();
        return FamilyMemberResponse.from(familyMemberRepository.save(relation));
    }

    public FamilyMemberResponse update(UUID accountId, UUID id, FamilyMemberRequest request) {
        validateRequest(request);
        Profile owner = familyAccessService.ownerProfile(accountId);
        FamilyMember relation = findOwned(owner.getProfileId(), id);
        if (!Boolean.TRUE.equals(relation.getIsActive())) {
            throw new BadRequestException("Vui lòng khôi phục thành viên trước khi chỉnh sửa");
        }
        ensureNotDuplicate(owner, relation.getMemberProfile().getProfileId(), request);
        Profile member = relation.getMemberProfile();
        member.setFullName(clean(request.fullName()));
        member.setDateOfBirth(request.dateOfBirth());
        member.setGender(request.gender());
        member.setAddress(emptyToNull(request.address()));
        member.setBloodType(request.bloodType());
        member.setAllergies(allergyStorage(request));
        relation.setRelationship(request.relationship());
        profileRepository.save(member);
        return FamilyMemberResponse.from(familyMemberRepository.save(relation));
    }

    public void archive(UUID accountId, UUID id) {
        Profile owner = familyAccessService.ownerProfile(accountId);
        FamilyMember relation = findOwned(owner.getProfileId(), id);
        if (!Boolean.TRUE.equals(relation.getIsActive())) return;
        UUID profileId = relation.getMemberProfile().getProfileId();
        boolean hasUpcomingAppointment = appointmentRepository
                .existsByCustomer_ProfileIdAndStatusInAndScheduledAtGreaterThanEqual(
                        profileId, List.of(AppointmentStatus.PENDING, AppointmentStatus.RESCHEDULED),
                        LocalDateTime.now());
        boolean hasActiveVisit = customerVisitRepository.existsByCustomer_ProfileIdAndStatusIn(
                profileId, List.of(VisitStatus.CHECKED_IN, VisitStatus.IN_PROGRESS));
        if (hasUpcomingAppointment || hasActiveVisit) {
            throw new ConflictException("Không thể ngừng quản lý khi thành viên còn lịch hẹn sắp tới hoặc lượt khám đang hoạt động");
        }
        relation.setIsActive(false);
        familyMemberRepository.save(relation);
    }

    public FamilyMemberResponse restore(UUID accountId, UUID id) {
        Profile owner = familyAccessService.ownerProfile(accountId);
        FamilyMember relation = findOwned(owner.getProfileId(), id);
        relation.setIsActive(true);
        return FamilyMemberResponse.from(familyMemberRepository.save(relation));
    }

    private FamilyMember findOwned(UUID ownerProfileId, UUID id) {
        return familyMemberRepository.findByFamilyMemberIdAndOwnerProfile_ProfileId(id, ownerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên gia đình"));
    }

    private void validateRequest(FamilyMemberRequest request) {
        if (request.gender() != Gender.MALE && request.gender() != Gender.FEMALE) {
            throw new BadRequestException("Giới tính chỉ được chọn Nam hoặc Nữ");
        }
    }

    private void ensureNotDuplicate(Profile owner, UUID excludedProfileId, FamilyMemberRequest request) {
        String expectedName = normalize(request.fullName());
        boolean duplicate = familyMemberRepository
                .findAllByOwnerProfile_ProfileIdOrderByCreatedAtAsc(owner.getProfileId()).stream()
                .map(FamilyMember::getMemberProfile)
                .filter(profile -> excludedProfileId == null || !excludedProfileId.equals(profile.getProfileId()))
                .anyMatch(profile -> normalize(profile.getFullName()).equals(expectedName)
                        && request.dateOfBirth().equals(profile.getDateOfBirth())
                        && request.gender() == profile.getGender());
        if (duplicate) {
            throw new ConflictException("Thành viên có cùng họ tên, ngày sinh và giới tính đã tồn tại trong gia đình");
        }
        if (normalize(owner.getFullName()).equals(expectedName)
                && request.dateOfBirth().equals(owner.getDateOfBirth())
                && request.gender() == owner.getGender()) {
            throw new ConflictException("Không thể thêm chính bạn làm thành viên gia đình");
        }
    }

    private String joinAllergies(List<String> values) {
        List<String> normalized = org.example.doansummer2026.dto.medicalRecord.PatientAllergyResponse.normalize(values);
        return normalized.isEmpty() ? null : String.join("\n", normalized);
    }

    private String allergyStorage(FamilyMemberRequest request) {
        AllergyStatus status = request.allergyStatus();
        if (status == null) return joinAllergies(request.allergies());
        if (status == AllergyStatus.UNVERIFIED) return null;
        if (status == AllergyStatus.NONE_REPORTED) return "";

        List<String> normalized = org.example.doansummer2026.dto.medicalRecord.PatientAllergyResponse
                .normalize(request.allergies());
        if (normalized.isEmpty()) {
            throw new BadRequestException("Vui lòng thêm ít nhất một dị ứng đã ghi nhận");
        }
        return String.join("\n", normalized);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }

    private String emptyToNull(String value) {
        String clean = clean(value);
        return clean == null || clean.isBlank() ? null : clean;
    }
}
