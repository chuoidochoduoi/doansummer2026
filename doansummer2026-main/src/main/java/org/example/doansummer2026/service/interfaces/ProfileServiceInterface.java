package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.profile.ProfileResponse;
import org.example.doansummer2026.dto.profile.ProfileCreateRequest;
import org.example.doansummer2026.dto.profile.ProfileUpdateRequest;
import org.example.doansummer2026.model.Profile;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Service interface for Profile management. */
public interface ProfileServiceInterface {
    ProfileResponse get(UUID id);
    ProfileResponse getByAccount(UUID accountId);
    ProfileResponse create(ProfileCreateRequest req);
    ProfileResponse update(UUID id, ProfileUpdateRequest req);
    void delete(UUID id);
    PageResponse<ProfileResponse> search(String keyword, Pageable pageable);
    Profile findById(UUID id);
}



