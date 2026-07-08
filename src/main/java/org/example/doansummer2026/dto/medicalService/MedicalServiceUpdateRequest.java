package org.example.doansummer2026.dto.medicalService;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.ServiceType;

import java.math.BigDecimal;
import java.util.UUID;

public record MedicalServiceUpdateRequest(
        @Size(max = 200) String name,
        @Size(max = 1000) String description,
        ServiceType serviceType,
        @PositiveOrZero Integer durationMinutes,
        @PositiveOrZero BigDecimal price,
        Boolean isActive,
        Boolean isPointOfCare,
        UUID categoryId,
        UUID departmentId
) {}
