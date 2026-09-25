package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.notification.NotificationResponse;
import vn.edu.fpt.cares.dto.notification.NotificationCreateRequest;
import vn.edu.fpt.cares.dto.notification.NotificationUpdateRequest;
import vn.edu.fpt.cares.dto.notification.UnreadCountResponse;
import vn.edu.fpt.cares.enums.NotificationStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Service interface for Notification management. */
public interface NotificationServiceInterface {
    PageResponse<NotificationResponse> search(UUID recipientId, NotificationStatus status, Pageable pageable);
    NotificationResponse get(UUID id);
    NotificationResponse create(NotificationCreateRequest req);
    NotificationResponse update(UUID id, NotificationUpdateRequest req);
    void delete(UUID id);
    UnreadCountResponse unreadCount(UUID recipientId);
    NotificationResponse send(UUID id);
    NotificationResponse markRead(UUID id);
    NotificationResponse markFailed(UUID id);
}



