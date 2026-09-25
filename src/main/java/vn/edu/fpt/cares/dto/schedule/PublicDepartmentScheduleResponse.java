package vn.edu.fpt.cares.dto.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PublicDepartmentScheduleResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<ShiftItem> shifts,
        List<DepartmentGroupItem> departments
) {
    public record ShiftItem(
            UUID shiftId,
            String name,
            String startTime,
            String endTime
    ) {}

    public record DepartmentGroupItem(
            String groupId,
            String groupName,
            String groupType,
            List<AvailabilityItem> availability
    ) {}

    public record AvailabilityItem(
            LocalDate date,
            UUID shiftId,
            boolean available
    ) {}
}
