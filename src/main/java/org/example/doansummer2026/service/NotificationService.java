package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.notification.NotificationCreateRequest;
import org.example.doansummer2026.dto.notification.NotificationResponse;
import org.example.doansummer2026.dto.notification.NotificationUpdateRequest;
import org.example.doansummer2026.dto.notification.UnreadCountResponse;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Notification;
import org.example.doansummer2026.enums.NotificationStatus;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.NotificationRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.NotificationServiceInterface;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.enums.NotificationType;
import org.example.doansummer2026.enums.NotificationChannel;
import java.util.List;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService implements NotificationServiceInterface {

    private final NotificationRepository repo;
    private final ProfileRepository profileRepo;
    private final StaffInfoRepository staffRepo;
    private final SimpMessageSendingOperations messagingTemplate;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> search(UUID recipientId, NotificationStatus status,
                                                       Pageable pageable) {
        Page<Notification> page = repo.search(recipientId, status, pageable);
        return PageResponse.from(page, NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(UUID id) {
        return NotificationResponse.from(findById(id));
    }

    public NotificationResponse create(NotificationCreateRequest req) {
        Profile recipient = profileRepo.findById(req.recipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Nguoi nhan khong ton tai: " + req.recipientId()));
        Notification n = Notification.builder()
                .recipient(recipient)
                .notificationType(req.notificationType())
                .channel(req.channel())
                .title(req.title())
                .content(req.content())
                .relatedEntity(req.relatedEntity())
                .relatedEntityId(req.relatedEntityId())
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();
        Notification saved = repo.save(n);
        
        // Broadcast to recipient via WebSocket
        NotificationResponse response = NotificationResponse.from(saved);
        if (recipient.getAccount() != null) {
            messagingTemplate.convertAndSend("/topic/notifications-" + recipient.getAccount().getAccountId(), response);
        }
        
        return response;
    }

    public NotificationResponse update(UUID id, NotificationUpdateRequest req) {
        Notification n = findById(id);
        if (req.status() != null) {
            n.setStatus(req.status());
            if (req.status() == NotificationStatus.SENT && n.getSentAt() == null) {
                n.setSentAt(LocalDateTime.now());
            }
        }
        if (req.failureReason() != null) n.setFailureReason(req.failureReason());
        return NotificationResponse.from(repo.save(n));
    }

    public NotificationResponse send(UUID id) {
        Notification n = findById(id);
        if (n.getStatus() != NotificationStatus.PENDING) {
            throw new BadRequestException("Chi gui notification PENDING; hien tai: " + n.getStatus());
        }
        n.setStatus(NotificationStatus.SENT);
        n.setSentAt(LocalDateTime.now());
        return NotificationResponse.from(repo.save(n));
    }

    public NotificationResponse markRead(UUID id) {
        Notification n = findById(id);
        n.setStatus(NotificationStatus.READ);
        if (n.getReadAt() == null) n.setReadAt(LocalDateTime.now());
        return NotificationResponse.from(repo.save(n));
    }

    public NotificationResponse markFailed(UUID id, String reason) {
        Notification n = findById(id);
        n.setStatus(NotificationStatus.FAILED);
        n.setFailureReason(reason);
        return NotificationResponse.from(repo.save(n));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Thong bao khong ton tai: " + id);
        }
        repo.deleteById(id);
    }

    public Notification findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thong bao khong ton tai: " + id));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID recipientId) {
        return new UnreadCountResponse(recipientId,
                repo.countByRecipient_ProfileIdAndStatus(recipientId, NotificationStatus.SENT));
    }

    public void notifyStaffByRole(SystemRole role, String title, String content, String relatedEntity, UUID relatedEntityId) {
        List<StaffInfo> staffList = staffRepo.findAllBySystemRoleIn(List.of(role));
        for (StaffInfo staff : staffList) {
            if (staff.getProfile() != null) {
                try {
                    create(new NotificationCreateRequest(
                            staff.getProfile().getProfileId(),
                            NotificationType.GENERAL,
                            NotificationChannel.IN_APP,
                            title,
                            content,
                            relatedEntity,
                            relatedEntityId
                    ));
                } catch (Exception e) {}
            }
        }
    }
}




