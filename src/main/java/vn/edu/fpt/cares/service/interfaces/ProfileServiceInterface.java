package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.profile.ProfileResponse;
import vn.edu.fpt.cares.dto.profile.ProfileCreateRequest;
import vn.edu.fpt.cares.dto.profile.ProfileUpdateRequest;
import vn.edu.fpt.cares.model.Profile;
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



