package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitResponse;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitCreateRequest;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitUpdateRequest;
import org.example.doansummer2026.model.CustomerVisit;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/** Service interface for CustomerVisit management. */
public interface CustomerVisitServiceInterface {
    PageResponse<CustomerVisitResponse> search(UUID customerId, org.example.doansummer2026.enums.VisitStatus status,
                                               LocalDateTime from, LocalDateTime to, Pageable pageable);
    CustomerVisitResponse get(UUID id);
    CustomerVisitResponse create(CustomerVisitCreateRequest req);
    CustomerVisitResponse update(UUID id, CustomerVisitUpdateRequest req);
    void delete(UUID id);
    CustomerVisit findById(UUID id);
}