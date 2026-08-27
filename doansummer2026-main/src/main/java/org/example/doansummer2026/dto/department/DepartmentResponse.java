package org.example.doansummer2026.dto.department;

import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.model.Department;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record DepartmentResponse(
        UUID departmentId,
        String roomCode,
        String name,
        DepartmentStatus status,
        DepartmentType departmentType,
        UUID specializationId,
        String specializationName,
        List<CapabilityInfo> capabilities,
        String description,
        HeadDoctor headDoctor,
        List<NurseInfo> nurses
) {
    public static DepartmentResponse from(Department d) {
        HeadDoctor hd = d.getHeadDoctor() != null
                ? new HeadDoctor(d.getHeadDoctor().getStaffId(), d.getHeadDoctor().getProfile().getFullName())
                : null;
        return new DepartmentResponse(
                d.getDepartmentId(),
                d.getRoomCode(),
                d.getName(),
                d.getStatus(),
                d.getDepartmentType() != null ? d.getDepartmentType().normalized() : null,
                d.getSpecialization() != null ? d.getSpecialization().getSpecializationId() : null,
                d.getSpecialization() != null ? d.getSpecialization().getName() : null,
                d.getCapabilities() == null ? List.of() : d.getCapabilities().stream()
                        .map(c -> new CapabilityInfo(c.getCapabilityId(), c.getCode(), c.getName())).toList(),
                d.getDescription(),
                hd,
                d.getNurses() != null ? d.getNurses().stream()
                        .map(n -> new NurseInfo(n.getStaffId(), n.getProfile().getFullName()))
                        .collect(Collectors.toList()) : List.of()
        );
    }

    public record HeadDoctor(UUID staffId, String fullName) {}
    public record NurseInfo(UUID staffId, String fullName) {}
    public record CapabilityInfo(UUID capabilityId, String code, String name) {}
}
