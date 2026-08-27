package org.example.doansummer2026.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.enums.DepartmentType;

import java.util.List;
import java.util.UUID;

public record DepartmentCreateRequest(
        @NotBlank @Size(max = 20) String roomCode,
        @NotBlank @Size(max = 150) String name,
        DepartmentStatus status,
        DepartmentType departmentType,
        UUID specializationId,
        List<UUID> capabilityIds,
        @Size(max = 500) String description,
        UUID headDoctorId,
        List<UUID> nurseIds
) {}

