package org.example.doansummer2026.dto.attendance;
import org.example.doansummer2026.model.StaffAttendance;
import java.time.*;
import java.util.UUID;
public record AttendanceResponse(UUID attendanceId,UUID scheduleId,UUID staffId,String staffName,LocalDate workDate,String shift,
 String status,LocalDateTime checkInAt,LocalDateTime checkOutAt) {
 public static AttendanceResponse from(StaffAttendance a){return new AttendanceResponse(a.getAttendanceId(),a.getSchedule().getScheduleId(),a.getStaff().getStaffId(),
  a.getStaff().getProfile()!=null?a.getStaff().getProfile().getFullName():a.getStaff().getStaffCode(),a.getSchedule().getWorkDate(),a.getSchedule().getShift().name(),
  a.getStatus().name(),a.getCheckInAt(),a.getCheckOutAt());}
}
