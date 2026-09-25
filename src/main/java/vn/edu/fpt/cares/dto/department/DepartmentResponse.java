package vn.edu.fpt.cares.dto.department;

import vn.edu.fpt.cares.enums.DepartmentStatus;
import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.model.Department;

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
        List<DoctorInfo> doctors,
        List<NurseInfo> nurses,
        List<DoctorInfo> doctorsOnDuty,
        List<NurseInfo> nursesOnDuty,
        String coverageStatus
) {
    public static DepartmentResponse from(Department d) {
        return from(d, d.getNurses() == null ? List.of() : d.getNurses(), List.of());
    }

    public static DepartmentResponse from(Department d, List<vn.edu.fpt.cares.model.StaffInfo> onDuty) {
        return from(d, d.getNurses() == null ? List.of() : d.getNurses(), onDuty);
    }

    public static DepartmentResponse from(Department d,
                                          List<vn.edu.fpt.cares.model.StaffInfo> members,
                                          List<vn.edu.fpt.cares.model.StaffInfo> onDuty) {
        HeadDoctor hd = d.getHeadDoctor() != null
                ? new HeadDoctor(d.getHeadDoctor().getStaffId(), d.getHeadDoctor().getProfile().getFullName())
                : null;
        List<DoctorInfo> doctors = members.stream()
                .filter(n -> n.getSystemRole() != null && n.getSystemRole().isDoctor())
                .map(n -> new DoctorInfo(n.getStaffId(), n.getProfile().getFullName()))
                .collect(Collectors.toList());
        List<NurseInfo> nurses = members.stream()
                .filter(n -> n.getSystemRole() == vn.edu.fpt.cares.enums.SystemRole.NURSE)
                .map(n -> new NurseInfo(n.getStaffId(), n.getProfile().getFullName()))
                .collect(Collectors.toList());
        List<DoctorInfo> doctorsOnDuty = onDuty.stream()
                .filter(n -> n.getSystemRole() != null && n.getSystemRole().isDoctor())
                .map(n -> new DoctorInfo(n.getStaffId(), n.getProfile().getFullName()))
                .toList();
        List<NurseInfo> nursesOnDuty = onDuty.stream()
                .filter(n -> n.getSystemRole() == vn.edu.fpt.cares.enums.SystemRole.NURSE)
                .map(n -> new NurseInfo(n.getStaffId(), n.getProfile().getFullName()))
                .toList();
        String coverageStatus = doctorsOnDuty.isEmpty()
                ? (onDuty.isEmpty() ? "UNASSIGNED" : "MISSING_DOCTOR") : "COVERED";
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
                doctors,
                nurses,
                doctorsOnDuty,
                nursesOnDuty,
                coverageStatus
        );
    }

    public record HeadDoctor(UUID staffId, String fullName) {}
    public record DoctorInfo(UUID staffId, String fullName) {}
    public record NurseInfo(UUID staffId, String fullName) {}
    public record CapabilityInfo(UUID capabilityId, String code, String name) {}
}
