package org.example.doansummer2026.dto.attendance;
import java.time.*;
import java.util.UUID;
public record AttendanceTodayResponse(UUID scheduleId, LocalDate workDate, String shift,
 LocalTime scheduledStart, LocalTime scheduledEnd, UUID attendanceId, String status,
 LocalDateTime checkInAt, LocalDateTime checkOutAt) {}
