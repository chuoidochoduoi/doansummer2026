package org.example.doansummer2026.dto.medicalService;

import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.enums.ServiceStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.Gender;

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
        DepartmentType departmentType,
        BigDecimal price,
        ServiceStatus status,               // DRAFT, ACTIVE, INACTIVE
        Boolean isPointOfCare,
        Integer durationMinutes,
        Integer workflowPriority,
        Boolean requiresDoctorOrder,
        Boolean requiresReturnToDoctor,
        Boolean requiresSpecimen,
        Integer resultWaitMinutes,
        Boolean allowCustomerBooking,
        Integer minimumAge,
        Integer maximumAge,
        Gender allowedGender,
        UUID departmentId,
        String departmentName,
        UUID requiredSpecializationId,
        String requiredSpecializationName,
        UUID requiredCapabilityId,
        String requiredCapabilityName
) {
    public static MedicalServiceResponse from(MedicalService s) {

        UUID deptId = s.getDepartment() != null ? s.getDepartment().getDepartmentId() : null;
        String deptName = s.getDepartment() != null ? s.getDepartment().getName() : null;
        UUID specId = s.getRequiredSpecialization() != null ? s.getRequiredSpecialization().getSpecializationId() : null;
        String specName = s.getRequiredSpecialization() != null ? s.getRequiredSpecialization().getName() : null;

        return new MedicalServiceResponse(
                s.getServiceId(),
                s.getServiceCode(),
                s.getName(),
                s.getDescription(),
                s.getDepartmentType() != null ? s.getDepartmentType().normalized() : null,
                s.getPrice(),
                s.getStatus(),
                s.getIsPointOfCare(),
                s.getDurationMinutes() != null ? s.getDurationMinutes() : 15,
                s.getWorkflowPriority() != null ? s.getWorkflowPriority() : 1,
                Boolean.TRUE.equals(s.getRequiresDoctorOrder()),
                Boolean.TRUE.equals(s.getRequiresReturnToDoctor()),
                Boolean.TRUE.equals(s.getRequiresSpecimen()),
                s.getResultWaitMinutes() != null ? s.getResultWaitMinutes() : 0,
                s.getAllowCustomerBooking() == null || s.getAllowCustomerBooking(),
                s.getMinimumAge(),
                s.getMaximumAge(),
                s.getAllowedGender(),
                deptId,
                deptName,
                specId,
                specName,
                s.getRequiredCapability() != null ? s.getRequiredCapability().getCapabilityId() : null,
                s.getRequiredCapability() != null ? s.getRequiredCapability().getName() : null
        );
    }
}
