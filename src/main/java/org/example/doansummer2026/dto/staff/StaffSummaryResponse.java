package org.example.doansummer2026.dto.staff;

import java.util.UUID;

public record StaffSummaryResponse(
        UUID staffId,
        String staffCode,
        String fullName,
        String systemRole,
        UUID departmentId,
        UUID specializationId
) {}