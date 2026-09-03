package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.journey.QueueReturnRequestResponse;
import org.example.doansummer2026.enums.NotificationChannel;
import org.example.doansummer2026.enums.NotificationStatus;
import org.example.doansummer2026.enums.NotificationType;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Notification;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.NotificationRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QueueReturnRequestService {

    public static final String RELATED_ENTITY = "QUEUE_RETURN_REQUEST";
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final NotificationRepository notificationRepository;
    private final QueueTicketRepository queueTicketRepository;
    private final CustomerVisitRepository visitRepository;
    private final StaffInfoRepository staffInfoRepository;
    private final FamilyAccessService familyAccessService;
    private final QueueTicketService queueTicketService;
    private final SimpMessagingTemplate messagingTemplate;

    public QueueReturnRequestResponse requestForCustomer(UUID accountId, UUID visitId) {
        CustomerVisit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám"));
        if (visit.getCustomer() == null) {
            throw new ResourceNotFoundException("Không tìm thấy lượt khám");
        }
        Profile allowed = familyAccessService.resolveActiveProfile(accountId, visit.getCustomer().getProfileId());
        if (!allowed.getProfileId().equals(visit.getCustomer().getProfileId())) {
            throw new ResourceNotFoundException("Không tìm thấy lượt khám");
        }
        return createRequest(visit);
    }

    public QueueReturnRequestResponse requestForGuest(String visitCode, String phone) {
        String normalizedCode = visitCode == null ? "" : visitCode.trim().toUpperCase(Locale.ROOT);
        String normalizedPhone = phone == null ? "" : phone.replaceAll("\\s+", "");
        if (!normalizedCode.matches("VIS-[0-9A-F]{8}") || normalizedPhone.isBlank()) {
            throw new ResourceNotFoundException("Không tìm thấy lượt khám phù hợp");
        }
        CustomerVisit visit = visitRepository
                .findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc(normalizedPhone).stream()
                .filter(item -> normalizedCode.equals(visitCode(item.getVisitId())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám phù hợp"));
        return createRequest(visit);
    }

    private QueueReturnRequestResponse createRequest(CustomerVisit visit) {
        QueueTicket ticket = findSkippedToday(visit.getVisitId());
        List<Notification> existing = pending(ticket.getTicketId());
        if (!existing.isEmpty()) {
            return response(ticket, requestedAt(existing), "PENDING",
                    "Yêu cầu đã được gửi. Vui lòng đến quầy lễ tân để xác nhận có mặt");
        }

        List<Profile> recipients = staffInfoRepository.findAllBySystemRoleIn(
                        List.of(SystemRole.RECEPTIONIST, SystemRole.CLINIC_MANAGER)).stream()
                .map(staff -> staff.getProfile()).filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(Profile::getProfileId, profile -> profile,
                        (first, ignored) -> first, LinkedHashMap::new))
                .values().stream().toList();
        if (recipients.isEmpty()) {
            throw new BadRequestException("Hiện chưa có lễ tân tiếp nhận yêu cầu. Vui lòng đến quầy để được hỗ trợ");
        }

        LocalDateTime now = LocalDateTime.now(CLINIC_ZONE);
        List<Notification> notifications = recipients.stream().map(recipient -> Notification.builder()
                .recipient(recipient)
                .notificationType(NotificationType.GENERAL)
                .channel(NotificationChannel.IN_APP)
                .title("Khách báo đã quay lại")
                .content("Lượt " + visitCode(visit.getVisitId()) + " đang chờ xác nhận có mặt tại quầy")
                .relatedEntity(RELATED_ENTITY)
                .relatedEntityId(ticket.getTicketId())
                .status(NotificationStatus.PENDING)
                .sentAt(now)
                .build()).toList();
        notificationRepository.saveAll(notifications);
        publishReturnRequestsChanged();
        return response(ticket, now, "PENDING",
                "Đã báo lễ tân. Vui lòng đến quầy để xác nhận có mặt");
    }

    @Transactional(readOnly = true)
    public List<QueueReturnRequestResponse> pendingRequests() {
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        Map<UUID, List<Notification>> grouped = notificationRepository
                .findAllByRelatedEntityAndStatusOrderByCreatedAtAsc(RELATED_ENTITY, NotificationStatus.PENDING)
                .stream().filter(item -> item.getRelatedEntityId() != null)
                .collect(java.util.stream.Collectors.groupingBy(Notification::getRelatedEntityId,
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        return grouped.entrySet().stream().map(entry -> queueTicketRepository.findById(entry.getKey())
                        .filter(ticket -> ticket.getStatus() == QueueStatus.SKIPPED)
                        .filter(ticket -> today.equals(ticket.getWorkDate()))
                        .map(ticket -> response(ticket, requestedAt(entry.getValue()), "PENDING", null))
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(QueueReturnRequestResponse::requestedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public QueueReturnRequestResponse confirm(UUID ticketId) {
        List<Notification> requests = pending(ticketId);
        if (requests.isEmpty()) {
            throw new ConflictException("Yêu cầu quay lại không còn chờ xác nhận");
        }
        var restored = queueTicketService.confirmReturnToQueue(ticketId);
        LocalDateTime now = LocalDateTime.now(CLINIC_ZONE);
        requests.forEach(item -> {
            item.setStatus(NotificationStatus.READ);
            item.setReadAt(now);
        });
        notificationRepository.saveAll(requests);
        publishReturnRequestsChanged();
        QueueTicket ticket = queueTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu hàng chờ không tồn tại"));
        return response(ticket, requestedAt(requests), "CONFIRMED",
                "Đã xác nhận có mặt và đưa bệnh nhân về hàng chờ tại " + restored.departmentName());
    }

    public int expireBefore(LocalDate today) {
        List<Notification> pending = notificationRepository
                .findAllByRelatedEntityAndStatusOrderByCreatedAtAsc(RELATED_ENTITY, NotificationStatus.PENDING);
        List<Notification> expired = pending.stream().filter(item -> item.getRelatedEntityId() == null
                        || queueTicketRepository.findById(item.getRelatedEntityId())
                        .map(ticket -> ticket.getWorkDate() == null || ticket.getWorkDate().isBefore(today)
                                || ticket.getStatus() != QueueStatus.SKIPPED)
                        .orElse(true))
                .toList();
        expired.forEach(item -> {
            item.setStatus(NotificationStatus.FAILED);
            item.setFailureReason("Yêu cầu quay lại đã hết hiệu lực");
        });
        notificationRepository.saveAll(expired);
        if (!expired.isEmpty()) publishReturnRequestsChanged();
        return expired.size();
    }

    private QueueTicket findSkippedToday(UUID visitId) {
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        return queueTicketRepository.findAllByVisit_VisitId(visitId).stream()
                .filter(ticket -> ticket.getStatus() == QueueStatus.SKIPPED)
                .filter(ticket -> today.equals(ticket.getWorkDate()))
                .min(Comparator.comparing(QueueTicket::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new BadRequestException(
                        "Lượt khám không có phiếu vắng có thể báo quay lại trong hôm nay"));
    }

    private List<Notification> pending(UUID ticketId) {
        return notificationRepository.findAllByRelatedEntityAndRelatedEntityIdAndStatusOrderByCreatedAtAsc(
                RELATED_ENTITY, ticketId, NotificationStatus.PENDING);
    }

    private LocalDateTime requestedAt(List<Notification> notifications) {
        return notifications.stream().map(item -> item.getSentAt() != null ? item.getSentAt() : item.getCreatedAt())
                .filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
    }

    private QueueReturnRequestResponse response(QueueTicket ticket, LocalDateTime requestedAt,
                                                String status, String message) {
        CustomerVisit visit = ticket.getVisit();
        return new QueueReturnRequestResponse(ticket.getTicketId(), visit.getVisitId(),
                visitCode(visit.getVisitId()),
                visit.getCustomer() != null ? visit.getCustomer().getFullName() : "Người được khám",
                ticket.getDepartment() != null ? ticket.getDepartment().getName() : null,
                ticket.getDepartment() != null ? ticket.getDepartment().getRoomCode() : null,
                ticket.getQueueNumber(), ticket.getCalledAt(), requestedAt, status, message);
    }

    private String visitCode(UUID visitId) {
        return "VIS-" + visitId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private void publishReturnRequestsChanged() {
        try {
            messagingTemplate.convertAndSend("/topic/queue-return-requests", "RETURN_REQUESTS_UPDATED");
        } catch (Exception ignored) {
            // Realtime khong duoc lam rollback yeu cau/thu tu hang cho.
        }
    }
}
