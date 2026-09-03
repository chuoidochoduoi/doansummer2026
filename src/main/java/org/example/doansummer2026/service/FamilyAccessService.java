package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.enums.FamilyRelationship;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.FamilyMember;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.FamilyMemberRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyAccessService {

    private final ProfileRepository profileRepository;
    private final FamilyMemberRepository familyMemberRepository;

    public Profile ownerProfile(UUID accountId) {
        return profileRepository.findFirstByAccount_AccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ chủ tài khoản"));
    }

    public Profile resolveReadableProfile(UUID accountId, UUID patientProfileId) {
        Profile owner = ownerProfile(accountId);
        if (patientProfileId == null || owner.getProfileId().equals(patientProfileId)) return owner;
        return familyMemberRepository
                .findByOwnerProfile_ProfileIdAndMemberProfile_ProfileId(owner.getProfileId(), patientProfileId)
                .map(FamilyMember::getMemberProfile)
                .orElseThrow(() -> new BadRequestException("Bạn không có quyền truy cập hồ sơ bệnh nhân này"));
    }

    public Profile resolveActiveProfile(UUID accountId, UUID patientProfileId) {
        Profile owner = ownerProfile(accountId);
        if (patientProfileId == null || owner.getProfileId().equals(patientProfileId)) return owner;
        FamilyMember relation = familyMemberRepository
                .findByOwnerProfile_ProfileIdAndMemberProfile_ProfileId(owner.getProfileId(), patientProfileId)
                .orElseThrow(() -> new BadRequestException("Bạn không có quyền quản lý hồ sơ bệnh nhân này"));
        if (!Boolean.TRUE.equals(relation.getIsActive())) {
            throw new BadRequestException("Thành viên đã ngừng quản lý nên không thể thực hiện thao tác này");
        }
        return relation.getMemberProfile();
    }

    public List<Profile> readableProfiles(UUID accountId, boolean includeFamily) {
        Profile owner = ownerProfile(accountId);
        List<Profile> profiles = new ArrayList<>();
        profiles.add(owner);
        if (includeFamily) {
            familyMemberRepository.findAllByOwnerProfile_ProfileIdOrderByCreatedAtAsc(owner.getProfileId())
                    .forEach(item -> profiles.add(item.getMemberProfile()));
        }
        return profiles;
    }

    public boolean isSelf(UUID accountId, UUID profileId) {
        return ownerProfile(accountId).getProfileId().equals(profileId);
    }

    public FamilyRelationship relationship(UUID accountId, UUID profileId) {
        Profile owner = ownerProfile(accountId);
        if (owner.getProfileId().equals(profileId)) return null;
        return familyMemberRepository
                .findByOwnerProfile_ProfileIdAndMemberProfile_ProfileId(owner.getProfileId(), profileId)
                .map(FamilyMember::getRelationship)
                .orElse(null);
    }

    public UUID notificationRecipientProfileId(Profile patient) {
        if (patient == null) return null;
        if (patient.getAccount() != null) return patient.getProfileId();
        return familyMemberRepository.findByMemberProfile_ProfileId(patient.getProfileId())
                .map(relation -> relation.getOwnerProfile().getProfileId())
                .orElse(patient.getProfileId());
    }
}
