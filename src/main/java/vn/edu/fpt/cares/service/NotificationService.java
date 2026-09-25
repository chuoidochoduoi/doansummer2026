package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.notification.NotificationCreateRequest;
import vn.edu.fpt.cares.dto.notification.NotificationResponse;
import vn.edu.fpt.cares.dto.notification.NotificationUpdateRequest;
import vn.edu.fpt.cares.dto.notification.UnreadCountResponse;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.Notification;
import vn.edu.fpt.cares.enums.NotificationStatus;
import vn.edu.fpt.cares.model.Profile;
import vn.edu.fpt.cares.repository.NotificationRepository;
import vn.edu.fpt.cares.repository.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.cares.service.interfaces.NotificationServiceInterface;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import vn.edu.fpt.cares.repository.StaffInfoRepository;
import vn.edu.fpt.cares.model.StaffInfo;
import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.enums.NotificationType;
import vn.edu.fpt.cares.enums.NotificationChannel;
import java.util.List;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
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
                .orElseThrow(() -> new ResourceNotFoundException("Người nhận không tồn tại: " + req.recipientId()));
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
        return NotificationResponse.from(repo.save(n));
    }

    public NotificationResponse send(UUID id) {
        Notification n = findById(id);
        if (n.getStatus() != NotificationStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể gửi thông báo đang chờ gửi; trạng thái hiện tại: " + n.getStatus());
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

    public NotificationResponse markFailed(UUID id) {
        Notification n = findById(id);
        n.setStatus(NotificationStatus.FAILED);
        return NotificationResponse.from(repo.save(n));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Thông báo không tồn tại: " + id);
        }
        repo.deleteById(id);
    }

    public Notification findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thông báo không tồn tại: " + id));
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
                } catch (Exception e) {
                    log.warn("Không thể gửi thông báo {} tới nhân sự {}", relatedEntity, staff.getStaffId(), e);
                }
            }
        }
    }
}




