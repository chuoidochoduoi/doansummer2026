package org.example.doansummer2026.dto.medicalService;

import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.enums.ServiceType;

import java.math.BigDecimal;
import java.util.UUID;

public record MedicalServiceResponse(
        UUID serviceId,
        String name,
        String description,
        ServiceType serviceType,
        Integer durationMinutes,
        BigDecimal price,
        Boolean isActive,
        Boolean isPointOfCare,
        UUID categoryId,
        String categoryName,
        UUID departmentId,
        String departmentName
) {
    public static MedicalServiceResponse from(MedicalService s) {
        UUID categoryId = s.getCategory() != null ? s.getCategory().getCategoryId() : null;
        String categoryName = s.getCategory() != null ? s.getCategory().getName() : null;
        UUID deptId = s.getDepartment() != null ? s.getDepartment().getDepartmentId() : null;
        String deptName = s.getDepartment() != null ? s.getDepartment().getName() : null;

        return new MedicalServiceResponse(s.getServiceId(), s.getName(), s.getDescription(),
                s.getServiceType(), s.getDurationMinutes(), s.getPrice(), s.getIsActive(),
                s.getIsPointOfCare(), categoryId, categoryName, deptId, deptName);
    }
}
