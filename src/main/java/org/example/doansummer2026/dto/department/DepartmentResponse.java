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
                d.getDepartmentType(),
                d.getDescription(),
                hd,
                d.getNurses() != null ? d.getNurses().stream()
                        .map(n -> new NurseInfo(n.getStaffId(), n.getProfile().getFullName()))
                        .collect(Collectors.toList()) : List.of()
        );
    }

    public record HeadDoctor(UUID staffId, String fullName) {}
    public record NurseInfo(UUID staffId, String fullName) {}
}



