package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
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
    private final QueueTicketRepository queueTicketRepo;
    private final AuditLogService auditLogService;

    /**
     * Chạy vào lúc 00:05 sáng mỗi ngày (Asia/Ho_Chi_Minh).
     * Quét các đối tượng có ngày hẹn/xếp hàng thuộc về ngày hôm trước (hoặc cũ hơn)
     * mà chưa được xử lý xong và đổi trạng thái sang CANCELLED / SKIPPED.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void cleanupEndOfDay() {
        log.info("Starting End-of-Day Cleanup Job...");
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime startOfToday = LocalDateTime.of(today, LocalTime.MIN);

        // 1. Lịch hẹn (Appointments) – PENDING/RESCHEDULED quá ngày → CANCELLED
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

        // 2. Hàng chờ (QueueTicket) – ticket workDate < hôm nay còn active → SKIPPED (vắng mặt)
        List<QueueStatus> activeStatuses = List.of(
                QueueStatus.WAITING,
                QueueStatus.CALLED,
                QueueStatus.BLOCKED,
                QueueStatus.WAITING_FOR_TEST
        );
        List<QueueTicket> overdueTickets = queueTicketRepo.findOverdueActiveTickets(today, activeStatuses);

        for (QueueTicket ticket : overdueTickets) {
            ticket.setStatus(QueueStatus.SKIPPED);
        }
        queueTicketRepo.saveAll(overdueTickets);
        overdueTickets.forEach(ticket -> auditLogService.create(
                new org.example.doansummer2026.dto.auditLog.AuditLogCreateRequest(
                        org.example.doansummer2026.enums.AuditAction.STATUS_CHANGE,
                        "QueueTicket",
                        ticket.getTicketId().toString(),
                        null,
                        "system",
                        "SystemCleanupService",
                        null,
                        null,
                        "Hệ thống đánh vắng mặt phiếu hàng chờ quá ngày chưa hoàn thành"
                )));
        log.info("Skipped {} overdue queue tickets.", overdueTickets.size());

        log.info("End-of-Day Cleanup Job finished successfully.");
    }
}
