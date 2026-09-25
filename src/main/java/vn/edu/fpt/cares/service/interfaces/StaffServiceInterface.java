package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.staff.StaffResponse;
import vn.edu.fpt.cares.dto.staff.StaffCreateRequest;
import vn.edu.fpt.cares.dto.staff.StaffUpdateRequest;
import vn.edu.fpt.cares.model.StaffInfo;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Service interface for Staff management. */
public interface StaffServiceInterface {
    StaffResponse create(StaffCreateRequest req);
    StaffResponse get(UUID staffId);
    StaffResponse update(UUID staffId, StaffUpdateRequest req);
    void delete(UUID staffId);
    StaffResponse lock(UUID staffId);
    PageResponse<StaffResponse> search(String search, UUID specializationId,
                                        vn.edu.fpt.cares.enums.SystemRole systemRole, Pageable pageable);
    StaffInfo findById(UUID id);
}


