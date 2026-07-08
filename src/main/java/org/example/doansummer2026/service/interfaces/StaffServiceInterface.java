package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.staff.StaffResponse;
import org.example.doansummer2026.dto.staff.StaffCreateRequest;
import org.example.doansummer2026.dto.staff.StaffUpdateRequest;
import org.example.doansummer2026.model.StaffInfo;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Service interface for Staff management. */
public interface StaffServiceInterface {
    StaffResponse create(StaffCreateRequest req);
    StaffResponse get(UUID staffId);
    StaffResponse update(UUID staffId, StaffUpdateRequest req);
    void delete(UUID staffId);
    PageResponse<StaffResponse> search(UUID departmentId, UUID specializationId,
                                        org.example.doansummer2026.enums.SystemRole systemRole, Pageable pageable);
    StaffInfo findById(UUID id);
}