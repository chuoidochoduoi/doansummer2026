package vn.edu.fpt.cares.dto.scheduletemplate;

import vn.edu.fpt.cares.dto.schedule.ShiftResponse;
import vn.edu.fpt.cares.model.StaffScheduleTemplate;

import java.time.DayOfWeek;
import java.util.UUID;

public record ScheduleTemplateResponse(
        UUID templateId,
        UUID staffId,
        String staffCode,
        DayOfWeek dayOfWeek,
        ShiftResponse shift,
        Boolean isActive
) {
    public static ScheduleTemplateResponse from(StaffScheduleTemplate t) {
        return new ScheduleTemplateResponse(
                t.getTemplateId(),
                t.getStaff() != null ? t.getStaff().getStaffId() : null,
                t.getStaff() != null ? t.getStaff().getStaffCode() : null,
                t.getDayOfWeek(),
                ShiftResponse.from(t.getShift()),
                t.getIsActive()
        );
    }
}



