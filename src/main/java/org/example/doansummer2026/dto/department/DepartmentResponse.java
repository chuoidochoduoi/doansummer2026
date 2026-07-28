package org.example.doansummer2026.dto.department;

import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.model.Department;

import java.util.UUID;

public record DepartmentResponse(
        UUID departmentId,
        String roomCode,
        String name,
        DepartmentStatus status,
        DepartmentType departmentType,
        String description,
        HeadDoctor headDoctor
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
                hd
        );
    }

    public record HeadDoctor(UUID staffId, String fullName) {}
}



