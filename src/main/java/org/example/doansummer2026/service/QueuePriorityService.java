package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Mot nguon xep thu tu duy nhat cho hang cho phong kham.
 * queueNumber la so phieu co dinh; waitingPosition moi la vi tri phuc vu dong.
 */
@Service
@RequiredArgsConstructor
public class QueuePriorityService {

    public static final String RETURNING_FROM_TEST = "RETURNING_FROM_TEST";
    public static final String RETURNED_AFTER_ABSENCE = "RETURNED_AFTER_ABSENCE";
    public static final String APPOINTMENT_ON_TIME = "APPOINTMENT_ON_TIME";
    public static final String APPOINTMENT_LATE = "APPOINTMENT_LATE";
    public static final String REGULAR = "REGULAR";

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int APPOINTMENT_GRACE_MINUTES = 15;
    private static final Comparator<QueueTicket> FIFO = Comparator
            .comparing(QueueTicket::getQueueNumber, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(QueueTicket::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(QueueTicket::getTicketId, Comparator.nullsLast(UUID::compareTo));

    private final QueueTicketRepository queueTicketRepository;

    public record PriorityInfo(String category, String label,
                               LocalDateTime appointmentScheduledAt,
                               boolean activeAppointmentPriority) {}

    public record RankedTicket(QueueTicket ticket, Integer waitingPosition,
                               boolean canCall, PriorityInfo priority) {}

    @Transactional(readOnly = true)
    public List<RankedTicket> rank(List<QueueTicket> source) {
        return rank(source, LocalDateTime.now(CLINIC_ZONE));
    }

    /** Package-visible overload giup kiem thu bien thoi gian ma khong sua dong ho he thong. */
    List<RankedTicket> rank(List<QueueTicket> source, LocalDateTime now) {
        List<QueueTicket> tickets = source == null ? List.of() : source.stream()
                .filter(Objects::nonNull)
                .filter(ticket -> ticket.getVisit() != null)
                .toList();
        Map<UUID, UUID> firstExaminationByVisit = firstExaminationTickets(tickets);
        Map<UUID, PriorityInfo> metadata = new HashMap<>();
        tickets.forEach(ticket -> metadata.put(ticket.getTicketId(),
                priorityOf(ticket, firstExaminationByVisit, now)));

        List<QueueTicket> inProgress = tickets.stream()
                .filter(ticket -> ticket.getStatus() == QueueStatus.IN_PROGRESS)
                .sorted(FIFO).toList();
        List<QueueTicket> called = tickets.stream()
                .filter(ticket -> ticket.getStatus() == QueueStatus.CALLED)
                .sorted(FIFO).toList();
        List<QueueTicket> callable = new ArrayList<>(tickets.stream()
                .filter(ticket -> ticket.getStatus() == QueueStatus.WAITING
                        || ticket.getStatus() == QueueStatus.TEST_DONE)
                .sorted(FIFO).toList());

        List<QueueTicket> orderedWaiting = new ArrayList<>();
        // Khách đã được lễ tân xác nhận quay lại đứng ngay sau người đang được
        // gọi/phục vụ, nhưng không làm gián đoạn CALLED hoặc IN_PROGRESS.
        callable.stream().filter(ticket -> RETURNED_AFTER_ABSENCE.equals(
                        metadata.get(ticket.getTicketId()).category()))
                .sorted(FIFO).forEach(orderedWaiting::add);
        callable.removeIf(ticket -> RETURNED_AFTER_ABSENCE.equals(
                metadata.get(ticket.getTicketId()).category()));
        // Neu chua co ai duoc goi, nguoi dang dung dau FIFO duoc giu nguyen vi tri.
        if (called.isEmpty() && !callable.isEmpty()) {
            orderedWaiting.add(callable.remove(0));
        }

        callable.stream().filter(ticket -> ticket.getStatus() == QueueStatus.TEST_DONE)
                .sorted(FIFO).forEach(orderedWaiting::add);
        callable.removeIf(ticket -> ticket.getStatus() == QueueStatus.TEST_DONE);

        List<QueueTicket> appointments = callable.stream()
                .filter(ticket -> metadata.get(ticket.getTicketId()).activeAppointmentPriority())
                .sorted(FIFO).toList();
        List<QueueTicket> regular = callable.stream()
                .filter(ticket -> !metadata.get(ticket.getTicketId()).activeAppointmentPriority())
                .sorted(FIFO).toList();
        for (int appointmentIndex = 0, regularIndex = 0;
             appointmentIndex < appointments.size() || regularIndex < regular.size();) {
            if (appointmentIndex < appointments.size()) {
                orderedWaiting.add(appointments.get(appointmentIndex++));
            }
            if (regularIndex < regular.size()) {
                orderedWaiting.add(regular.get(regularIndex++));
            }
        }

        boolean roomBusy = !inProgress.isEmpty();
        UUID callableId = !called.isEmpty() ? called.get(0).getTicketId()
                : !roomBusy && !orderedWaiting.isEmpty() ? orderedWaiting.get(0).getTicketId() : null;
        Map<UUID, Integer> positions = new LinkedHashMap<>();
        for (int index = 0; index < orderedWaiting.size(); index++) {
            positions.put(orderedWaiting.get(index).getTicketId(), index + 1);
        }

        List<QueueTicket> ordered = new ArrayList<>();
        ordered.addAll(inProgress);
        ordered.addAll(called);
        ordered.addAll(orderedWaiting);
        tickets.stream().filter(ticket -> !ordered.contains(ticket)).sorted(FIFO).forEach(ordered::add);
        return ordered.stream().map(ticket -> new RankedTicket(
                ticket,
                positions.get(ticket.getTicketId()),
                ticket.getTicketId() != null && ticket.getTicketId().equals(callableId),
                metadata.get(ticket.getTicketId()))).toList();
    }

    private Map<UUID, UUID> firstExaminationTickets(List<QueueTicket> candidates) {
        Map<UUID, UUID> result = new HashMap<>();
        candidates.stream().map(QueueTicket::getVisit).filter(Objects::nonNull)
                .map(visit -> visit.getVisitId()).filter(Objects::nonNull).distinct()
                .forEach(visitId -> queueTicketRepository.findAllByVisit_VisitId(visitId).stream()
                        .filter(this::isExamination)
                        .min(FIFO)
                        .ifPresent(ticket -> result.put(visitId, ticket.getTicketId())));
        return result;
    }

    private PriorityInfo priorityOf(QueueTicket ticket, Map<UUID, UUID> firstExaminationByVisit,
                                    LocalDateTime now) {
        if (ticket.getStatus() == QueueStatus.TEST_DONE) {
            return new PriorityInfo(RETURNING_FROM_TEST, "Quay lại bác sĩ", null, true);
        }
        if (ticket.getStatus() == QueueStatus.WAITING && ticket.getCalledAt() != null) {
            return new PriorityInfo(RETURNED_AFTER_ABSENCE, "Đã quay lại", null, true);
        }
        Appointment appointment = ticket.getVisit() != null ? ticket.getVisit().getAppointment() : null;
        if (appointment == null || appointment.getScheduledAt() == null || !isExamination(ticket)
                || !ticket.getTicketId().equals(firstExaminationByVisit.get(ticket.getVisit().getVisitId()))
                || ticket.getWorkDate() == null
                || !ticket.getWorkDate().equals(appointment.getScheduledAt().toLocalDate())) {
            return new PriorityInfo(REGULAR, "Khách trực tiếp", null, false);
        }
        LocalDateTime scheduledAt = appointment.getScheduledAt();
        LocalDateTime checkedInAt = ticket.getVisit().getCheckInTime();
        boolean onTime = checkedInAt != null
                && !checkedInAt.isAfter(scheduledAt.plusMinutes(APPOINTMENT_GRACE_MINUTES));
        if (!onTime) {
            return new PriorityInfo(APPOINTMENT_LATE, "Có lịch · đến muộn", scheduledAt, false);
        }
        boolean active = !now.isBefore(scheduledAt);
        return new PriorityInfo(APPOINTMENT_ON_TIME,
                active ? "Có lịch · ưu tiên" : "Có lịch · chờ đến ca", scheduledAt, active);
    }

    private boolean isExamination(QueueTicket ticket) {
        DepartmentType type = ticket.getService() != null ? ticket.getService().getDepartmentType() : null;
        if (type == null && ticket.getDepartment() != null) type = ticket.getDepartment().getDepartmentType();
        return type != null && type.normalized() == DepartmentType.EXAMINATION;
    }
}
