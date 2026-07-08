package org.example.doansummer2026.dto.department;

import jakarta.validation.constraints.Size;

public record DepartmentUpdateRequest(
        @Size(max = 150) String name,
        @Size(max = 500) String description
) {}