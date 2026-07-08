package org.example.doansummer2026.dto.staff;

import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.SystemRole;

import java.util.UUID;

public record StaffUpdateRequest(
        UUID departmentId,
        UUID specializationId,
        SystemRole systemRole,
        @Size(max = 20) String nationalId,
        @Size(max = 30) String bankAccount,
        @Size(max = 100) String highestDegree,
        @Size(max = 200) String university,
        @Size(max = 50) String licenseNumber
) {}