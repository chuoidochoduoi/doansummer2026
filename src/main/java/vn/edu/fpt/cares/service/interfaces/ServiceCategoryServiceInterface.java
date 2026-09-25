package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.servicecategory.ServiceCategoryResponse;
import vn.edu.fpt.cares.dto.servicecategory.ServiceCategoryCreateRequest;
import vn.edu.fpt.cares.dto.servicecategory.ServiceCategoryUpdateRequest;
import vn.edu.fpt.cares.model.ServiceCategory;
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



