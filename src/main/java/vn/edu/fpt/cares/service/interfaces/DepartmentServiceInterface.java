package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.department.DepartmentResponse;
import vn.edu.fpt.cares.dto.department.DepartmentCreateRequest;
import vn.edu.fpt.cares.dto.department.DepartmentUpdateRequest;
import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.model.Department;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Service interface for Department management. */
public interface DepartmentServiceInterface {
    PageResponse<DepartmentResponse> listAll(Pageable pageable);
    PageResponse<DepartmentResponse> list(DepartmentType departmentType, Pageable pageable);
    PageResponse<DepartmentResponse> listMultiple(Pageable pageable, List<DepartmentType> departmentTypes);
    DepartmentResponse get(UUID id);
    DepartmentResponse create(DepartmentCreateRequest req);
    DepartmentResponse update(UUID id, DepartmentUpdateRequest req);
    void delete(UUID id);
    Department findById(UUID id);
}



