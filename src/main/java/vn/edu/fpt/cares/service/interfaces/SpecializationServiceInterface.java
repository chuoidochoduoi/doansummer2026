package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.specialization.SpecializationResponse;
import vn.edu.fpt.cares.dto.specialization.SpecializationCreateRequest;
import vn.edu.fpt.cares.dto.specialization.SpecializationUpdateRequest;
import vn.edu.fpt.cares.model.Specialization;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Service interface for Specialization management. */
public interface SpecializationServiceInterface {
    PageResponse<SpecializationResponse> list(Pageable pageable);
    SpecializationResponse get(UUID id);
    SpecializationResponse create(SpecializationCreateRequest req);
    SpecializationResponse update(UUID id, SpecializationUpdateRequest req);
    void delete(UUID id);
    Specialization findById(UUID id);
}



