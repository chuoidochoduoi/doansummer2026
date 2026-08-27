package org.example.doansummer2026.dto.attendance;
import java.time.*;
import java.util.UUID;
public record AttendanceManagementResponse(UUID scheduleId, UUID staffId, String staffCode, String staffName,
 LocalDate workDate, String shift, String status, LocalDateTime checkInAt, LocalDateTime checkOutAt) {}
