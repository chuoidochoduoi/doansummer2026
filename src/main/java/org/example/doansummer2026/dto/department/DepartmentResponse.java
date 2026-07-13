package org.example.doansummer2026.dto.department;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.model.Department;

import java.util.UUID;

public record DepartmentResponse(
        UUID departmentId,
        String name,
        String description,
        DepartmentType departmentType
) {
    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(d.getDepartmentId(), d.getName(), d.getDescription(), d.getDepartmentType());
    }
}