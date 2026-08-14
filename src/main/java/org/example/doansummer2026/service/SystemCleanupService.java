package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemCleanupService {

    private final AppointmentRepository appointmentRepo;
    private final AuditLogService auditLogService;

    /**
     * Chạy vào lúc 00:05 sáng mỗi ngày.
     * Quét các đối tượng có thời gian tạo/hẹn thuộc về ngày hôm trước (hoặc cũ hơn)
     * và đổi trạng thái sang CANCELLED/SKIPPED nếu chưa được xử lý xong.
     */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void cleanupEndOfDay() {
        log.info("Starting End-of-Day Cleanup Job...");
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime startOfToday = LocalDateTime.of(today, LocalTime.MIN);

        // 1. Lịch hẹn (Appointments)
        List<Appointment> pendingAppointments = appointmentRepo.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING
                        || a.getStatus() == AppointmentStatus.RESCHEDULED)
                .filter(a -> a.getScheduledAt() != null && a.getScheduledAt().isBefore(startOfToday))
                .toList();

        for (Appointment a : pendingAppointments) {
            a.setStatus(AppointmentStatus.CANCELLED);
            a.setCancelReason("Lịch hẹn đã quá ngày nhưng chưa được check-in");
        }
        appointmentRepo.saveAll(pendingAppointments);
        pendingAppointments.forEach(appointment -> auditLogService.create(
                new org.example.doansummer2026.dto.auditLog.AuditLogCreateRequest(
                        org.example.doansummer2026.enums.AuditAction.STATUS_CHANGE,
                        "Appointment",
                        appointment.getAppointmentId().toString(),
                        null,
                        "system",
                        "SystemCleanupService",
                        null,
                        null,
                        "Hệ thống hủy lịch hẹn quá hạn chưa check-in"
                )));
        log.info("Cancelled {} overdue appointments.", pendingAppointments.size());

        // Không tự động đổi trạng thái hàng chờ. WAITING/CALLED/BLOCKED có thể
        // còn gắn với TestRequest hoặc bước khám tiếp theo; tự đánh vắng ở đây
        // sẽ làm hành trình bệnh nhân kẹt mà không có người chịu trách nhiệm.
        // Việc vắng/hủy/điều phối lại phải đi qua API nghiệp vụ có audit log.

        log.info("End-of-Day Cleanup Job finished successfully.");
    }
}
