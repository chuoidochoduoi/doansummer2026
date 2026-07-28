package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.notification.NotificationResponse;
import org.example.doansummer2026.dto.notification.NotificationCreateRequest;
import org.example.doansummer2026.dto.notification.NotificationUpdateRequest;
import org.example.doansummer2026.dto.notification.UnreadCountResponse;
import org.example.doansummer2026.enums.NotificationStatus;
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
    NotificationResponse markFailed(UUID id, String reason);
}



