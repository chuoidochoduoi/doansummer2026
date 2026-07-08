package org.example.doansummer2026.dto.schedule;

import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.enums.Shift;
import org.example.doansummer2026.model.StaffSchedule;

import java.time.LocalDate;
import java.util.UUID;

public record ScheduleResponse(
        UUID scheduleId,
        UUID staffId,
        String staffCode,
        String fullName,
        LocalDate workDate,
        Shift shift,
        ScheduleStatus status,
        Boolean isCustom,
        String note,
        UUID templateId
) {
    public static ScheduleResponse from(StaffSchedule s) {
        String code = s.getStaff() != null ? s.getStaff().getStaffCode() : null;
        String name = (s.getStaff() != null && s.getStaff().getProfile() != null)
                ? s.getStaff().getProfile().getFullName() : null;
        return new ScheduleResponse(
                s.getScheduleId(),
                s.getStaff() != null ? s.getStaff().getStaffId() : null,
                code,
                name,
                s.getWorkDate(),
                s.getShift(),
                s.getStatus(),
                s.getIsCustom(),
                s.getNote(),
                s.getTemplate() != null ? s.getTemplate().getTemplateId() : null
        );
    }
}