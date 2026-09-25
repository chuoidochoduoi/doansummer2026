package vn.edu.fpt.cares.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.cares.enums.DepartmentStatus;
import vn.edu.fpt.cares.enums.DepartmentType;

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
        List<UUID> doctorIds,
        List<UUID> nurseIds
) {}
