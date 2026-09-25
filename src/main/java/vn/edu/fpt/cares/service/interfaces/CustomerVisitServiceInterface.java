package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.customervisit.CustomerVisitResponse;
import vn.edu.fpt.cares.dto.customervisit.CustomerVisitCreateRequest;
import vn.edu.fpt.cares.dto.customervisit.CustomerVisitUpdateRequest;
import vn.edu.fpt.cares.model.CustomerVisit;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/** Service interface for CustomerVisit management. */
public interface CustomerVisitServiceInterface {
    PageResponse<CustomerVisitResponse> search(UUID customerId, vn.edu.fpt.cares.enums.VisitStatus status,
                                               LocalDateTime from, LocalDateTime to, Pageable pageable);
    CustomerVisitResponse get(UUID id);
    CustomerVisitResponse create(CustomerVisitCreateRequest req);
    CustomerVisitResponse update(UUID id, CustomerVisitUpdateRequest req);
    void delete(UUID id);
    CustomerVisit findById(UUID id);
}



