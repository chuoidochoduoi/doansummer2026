package vn.edu.fpt.cares.dto.medicalservice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.cares.enums.ServiceStatus;
import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.enums.Gender;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tao dich vu moi - mac dinh la DRAFT.
 */
public record MedicalServiceCreateRequest(
        @NotBlank @Size(max = 20) String serviceCode,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull DepartmentType departmentType,
        @NotNull @PositiveOrZero BigDecimal price,
        ServiceStatus status,
        Boolean isPointOfCare,
        @Positive Integer durationMinutes,
        @Min(0) @Max(2) Integer workflowPriority,
        Boolean requiresDoctorOrder,
        Boolean requiresReturnToDoctor,
        Boolean requiresSpecimen,
        @Min(0) Integer resultWaitMinutes,
        Boolean allowCustomerBooking,
        @Min(0) @Max(120) Integer minimumAge,
        @Min(0) @Max(120) Integer maximumAge,
        Gender allowedGender,
        UUID departmentId,
        UUID requiredSpecializationId,
        UUID requiredCapabilityId
) {}
