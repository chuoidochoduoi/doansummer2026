package org.example.doansummer2026.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.DepartmentType;

public record DepartmentCreateRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        DepartmentType departmentType
) {}