package org.example.doansummer2026.dto.medicalService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.ServiceType;

import java.math.BigDecimal;
import java.util.UUID;

public record MedicalServiceCreateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull ServiceType serviceType,
        @PositiveOrZero Integer durationMinutes,
        @NotNull @PositiveOrZero BigDecimal price,
        Boolean isActive,
        Boolean isPointOfCare,
        @NotNull UUID categoryId,
        UUID departmentId
) {}
