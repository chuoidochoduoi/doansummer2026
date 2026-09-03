package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemCleanupService {

    private final AppointmentRepository appointmentRepo;
    private final QueueTicketRepository queueTicketRepo;
    private final CustomerVisitRepository customerVisitRepo;
    private final AuditLogService auditLogService;
    private final QueueReturnRequestService queueReturnRequestService;

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

        // 2. Chi danh dau vang cac buoc chua phat sinh xu ly chuyen mon.
        // IN_PROGRESS, WAITING_FOR_TEST va TEST_DONE phai duoc giu de nhan vien xu ly ton dong.
        List<QueueStatus> activeStatuses = List.of(
                QueueStatus.WAITING,
                QueueStatus.CALLED,
                QueueStatus.BLOCKED
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
                        "Hệ thống đánh vắng mặt phiếu hàng chờ quá ngày chưa bắt đầu chuyên môn"
                )));
        log.info("Skipped {} overdue queue tickets.", overdueTickets.size());

        // 3. Dong VIS cua nhung khach da bo cac buoc con lai trong ngay cu.
        // Luot da phat sinh chuyen mon van de mo de nhan vien hoan tat dung quy trinh.
        LocalDateTime closedAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        // Bao gom ca phieu da duoc danh vang tu truoc nua dem; neu chi quet
        // WAITING/CALLED/BLOCKED thi cac VIS nay se bi treo vo thoi han.
        List<QueueTicket> oldSkippedTickets = queueTicketRepo.findOverdueActiveTickets(
                today, List.of(QueueStatus.SKIPPED));
        List<CustomerVisit> visitsToClose = oldSkippedTickets.stream()
                .map(QueueTicket::getVisit).filter(Objects::nonNull)
                .filter(visit -> visit.getStatus() != VisitStatus.CANCELLED
                        && visit.getStatus() != VisitStatus.COMPLETED)
                .distinct()
                .filter(visit -> queueTicketRepo.findAllByVisit_VisitId(visit.getVisitId()).stream()
                        .noneMatch(ticket -> ticket.getStatus() == QueueStatus.IN_PROGRESS
                                || ticket.getStatus() == QueueStatus.WAITING_FOR_TEST
                                || ticket.getStatus() == QueueStatus.TEST_DONE))
                .peek(visit -> {
                    boolean completedAnyService = queueTicketRepo
                            .findAllByVisit_VisitId(visit.getVisitId()).stream()
                            .anyMatch(ticket -> ticket.getStatus() == QueueStatus.DONE);
                    visit.setStatus(completedAnyService ? VisitStatus.COMPLETED : VisitStatus.CANCELLED);
                    visit.setCheckOutTime(closedAt);
                })
                .toList();
        customerVisitRepo.saveAll(visitsToClose);
        visitsToClose.forEach(visit -> auditLogService.create(
                new org.example.doansummer2026.dto.auditLog.AuditLogCreateRequest(
                        org.example.doansummer2026.enums.AuditAction.STATUS_CHANGE,
                        "CustomerVisit",
                        visit.getVisitId().toString(),
                        null,
                        "system",
                        "SystemCleanupService",
                        null,
                        null,
                        visit.getStatus() == VisitStatus.COMPLETED
                                ? "Hệ thống đóng lượt khám đã hoàn thành một phần; dịch vụ còn lại bị bỏ lượt"
                                : "Hệ thống hủy lượt khám quá ngày chưa thực hiện dịch vụ"
                )));
        log.info("Closed {} overdue visits without active clinical work.", visitsToClose.size());

        int expiredReturnRequests = queueReturnRequestService.expireBefore(today);
        log.info("Expired {} queue return requests.", expiredReturnRequests);

        log.info("End-of-Day Cleanup Job finished successfully.");
    }
}
