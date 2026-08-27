package org.example.doansummer2026.dto.attendance;
import org.example.doansummer2026.model.AttendanceAdjustment;
import java.time.LocalDateTime;
import java.util.UUID;
public record AdjustmentResponse(UUID id,String staffName,String reason,LocalDateTime requestedCheckIn,LocalDateTime requestedCheckOut,String status,String reviewNote){
 public static AdjustmentResponse from(AttendanceAdjustment a){return new AdjustmentResponse(a.getAdjustmentId(),a.getAttendance().getStaff().getProfile().getFullName(),a.getReason(),a.getRequestedCheckIn(),a.getRequestedCheckOut(),a.getStatus().name(),a.getReviewNote());}}
