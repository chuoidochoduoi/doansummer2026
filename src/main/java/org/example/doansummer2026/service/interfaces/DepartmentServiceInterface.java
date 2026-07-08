package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.department.DepartmentResponse;
import org.example.doansummer2026.dto.department.DepartmentCreateRequest;
import org.example.doansummer2026.dto.department.DepartmentUpdateRequest;
import org.example.doansummer2026.model.Department;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Service interface for Department management. */
public interface DepartmentServiceInterface {
    PageResponse<DepartmentResponse> list(Pageable pageable);
    DepartmentResponse get(UUID id);
    DepartmentResponse create(DepartmentCreateRequest req);
    DepartmentResponse update(UUID id, DepartmentUpdateRequest req);
    void delete(UUID id);
    Department findById(UUID id);
}