package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.servicecategory.ServiceCategoryResponse;
import org.example.doansummer2026.dto.servicecategory.ServiceCategoryCreateRequest;
import org.example.doansummer2026.dto.servicecategory.ServiceCategoryUpdateRequest;
import org.example.doansummer2026.model.ServiceCategory;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Service interface for ServiceCategory management. */
public interface ServiceCategoryServiceInterface {
    PageResponse<ServiceCategoryResponse> list(Pageable pageable);
    ServiceCategoryResponse get(UUID id);
    ServiceCategoryResponse create(ServiceCategoryCreateRequest req);
    ServiceCategoryResponse update(UUID id, ServiceCategoryUpdateRequest req);
    void delete(UUID id);
    ServiceCategory findById(UUID id);
}



