package org.example.doansummer2026.dto.staff;

import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.SystemRole;

import java.util.UUID;

public record StaffUpdateRequest(
        // NOTE: departmentId da duoc xoa - chi dung head_doctor_id o Department
        // specializationId: can thiet cho SPECIALIST_DOCTOR
        UUID specializationId,
        SystemRole systemRole,
        @Size(max = 20) String nationalId,
        @Size(max = 30) String bankAccount,
        @Size(max = 100) String highestDegree,
        @Size(max = 200) String university,
        @Size(max = 50) String licenseNumber
) {}



