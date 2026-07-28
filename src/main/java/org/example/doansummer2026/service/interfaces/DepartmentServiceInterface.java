package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.department.DepartmentResponse;
import org.example.doansummer2026.dto.department.DepartmentCreateRequest;
import org.example.doansummer2026.dto.department.DepartmentUpdateRequest;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.model.Department;
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



