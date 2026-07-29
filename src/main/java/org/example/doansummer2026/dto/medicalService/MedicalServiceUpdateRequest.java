package org.example.doansummer2026.dto.medicalService;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.ServiceStatus;
import org.example.doansummer2026.enums.DepartmentType;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cap nhat dich vu. Chi cap nhat khi status = DRAFT hoac ACTIVE.
 */
public record MedicalServiceUpdateRequest(
        @Size(max = 200) String name,
        @Size(max = 1000) String description,
        DepartmentType departmentType,
        @PositiveOrZero BigDecimal price,
        ServiceStatus status,
        Boolean isPointOfCare,
        UUID departmentId,
        UUID requiredSpecializationId
) {}




