package org.example.doansummer2026.dto.medicalService;

import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.enums.ServiceStatus;
import org.example.doansummer2026.enums.ServiceType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response cho API quan ly dich vu y te.
 * Format: Ma DV, Ten dich vu y te, Loai goc, Chuyen khoa hien thi can thiet, Gia niem yet, Trang thai van hanh
 */
public record MedicalServiceResponse(
        UUID serviceId,
        String serviceCode,
        String name,
        String description,
        ServiceType serviceType,
        Integer durationMinutes,
        BigDecimal price,
        ServiceStatus status,               // DRAFT, ACTIVE, INACTIVE
        Boolean isPointOfCare,
        UUID categoryId,
        String categoryName,
        UUID departmentId,
        String departmentName,
        UUID requiredSpecializationId,
        String requiredSpecializationName
) {
    public static MedicalServiceResponse from(MedicalService s) {
        UUID categoryId = s.getCategory() != null ? s.getCategory().getCategoryId() : null;
        String categoryName = s.getCategory() != null ? s.getCategory().getName() : null;
        UUID deptId = s.getDepartment() != null ? s.getDepartment().getDepartmentId() : null;
        String deptName = s.getDepartment() != null ? s.getDepartment().getName() : null;
        UUID specId = s.getRequiredSpecialization() != null ? s.getRequiredSpecialization().getSpecializationId() : null;
        String specName = s.getRequiredSpecialization() != null ? s.getRequiredSpecialization().getName() : null;

        return new MedicalServiceResponse(
                s.getServiceId(),
                s.getServiceCode(),
                s.getName(),
                s.getDescription(),
                s.getServiceType(),
                s.getDurationMinutes(),
                s.getPrice(),
                s.getStatus(),
                s.getIsPointOfCare(),
                categoryId,
                categoryName,
                deptId,
                deptName,
                specId,
                specName
        );
    }
}




