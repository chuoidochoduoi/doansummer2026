package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.specialization.SpecializationResponse;
import org.example.doansummer2026.dto.specialization.SpecializationCreateRequest;
import org.example.doansummer2026.dto.specialization.SpecializationUpdateRequest;
import org.example.doansummer2026.model.Specialization;
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



