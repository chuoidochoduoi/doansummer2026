package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.model.TestRequest;
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
    private final InvoiceRepository invoiceRepo;
    private final CustomerVisitRepository customerVisitRepo;
    private final TestRequestRepository testRequestRepo;

    /**
     * Chạy vào lúc 00:05 sáng mỗi ngày.
     * Quét các đối tượng có thời gian tạo/hẹn thuộc về ngày hôm trước (hoặc cũ hơn)
     * và đổi trạng thái sang CANCELLED/SKIPPED nếu chưa được xử lý xong.
     */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void cleanupEndOfDay() {
        log.info("Starting End-of-Day Cleanup Job...");
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        // 1. Lịch hẹn (Appointments)
        List<Appointment> pendingAppointments = appointmentRepo.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING)
                .filter(a -> a.getScheduledAt() != null && a.getScheduledAt().isBefore(startOfToday))
                .toList();

        for (Appointment a : pendingAppointments) {
            a.setStatus(AppointmentStatus.CANCELLED);
        }
        appointmentRepo.saveAll(pendingAppointments);
        log.info("Cancelled {} overdue appointments.", pendingAppointments.size());

        // 2. Hàng chờ (Queue Tickets)
        List<QueueTicket> pendingTickets = queueTicketRepo.findAll().stream()
                .filter(q -> q.getStatus() == QueueStatus.WAITING || q.getStatus() == QueueStatus.CALLED || q.getStatus() == QueueStatus.IN_PROGRESS || q.getStatus() == QueueStatus.WAITING_FOR_TEST || q.getStatus() == QueueStatus.TEST_DONE)
                .filter(q -> q.getCreatedAt() != null && q.getCreatedAt().isBefore(startOfToday))
                .toList();
        
        for (QueueTicket q : pendingTickets) {
            q.setStatus(QueueStatus.SKIPPED);
        }
        queueTicketRepo.saveAll(pendingTickets);
        log.info("Skipped {} overdue queue tickets.", pendingTickets.size());

        // 3. Hóa đơn (Invoices)
        List<Invoice> pendingInvoices = invoiceRepo.findAll().stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PENDING)
                .filter(i -> i.getIssueDate() != null && i.getIssueDate().atStartOfDay().isBefore(startOfToday.toLocalDate().atStartOfDay()))
                .toList();

        for (Invoice i : pendingInvoices) {
            i.setStatus(InvoiceStatus.CANCELLED);
        }
        invoiceRepo.saveAll(pendingInvoices);
        log.info("Cancelled {} overdue invoices.", pendingInvoices.size());

        // 4. Phiên khám (Customer Visits)
        List<CustomerVisit> pendingVisits = customerVisitRepo.findAll().stream()
                .filter(v -> v.getStatus() == VisitStatus.CHECKED_IN || v.getStatus() == VisitStatus.IN_PROGRESS)
                .filter(v -> v.getCreatedAt() != null && v.getCreatedAt().isBefore(startOfToday))
                .toList();

        for (CustomerVisit v : pendingVisits) {
            v.setStatus(VisitStatus.CANCELLED);
        }
        customerVisitRepo.saveAll(pendingVisits);
        log.info("Cancelled {} overdue customer visits.", pendingVisits.size());

        // 5. Phiếu Xét Nghiệm (Test Requests)
        List<TestRequest> pendingTests = testRequestRepo.findAll().stream()
                .filter(t -> t.getStatus() == TestRequestStatus.PENDING || t.getStatus() == TestRequestStatus.IN_PROGRESS || t.getStatus() == TestRequestStatus.BLOCKED)
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isBefore(startOfToday))
                .toList();

        for (TestRequest t : pendingTests) {
            t.setStatus(TestRequestStatus.CANCELLED);
        }
        testRequestRepo.saveAll(pendingTests);
        log.info("Cancelled {} overdue test requests.", pendingTests.size());

        log.info("End-of-Day Cleanup Job finished successfully.");
    }
}
