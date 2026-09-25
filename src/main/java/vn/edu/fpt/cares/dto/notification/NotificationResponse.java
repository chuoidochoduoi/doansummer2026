package vn.edu.fpt.cares.dto.notification;

import vn.edu.fpt.cares.model.Notification;
import vn.edu.fpt.cares.enums.NotificationChannel;
import vn.edu.fpt.cares.enums.NotificationStatus;
import vn.edu.fpt.cares.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID notificationId,
        UUID recipientId,
        String recipientName,
        NotificationType notificationType,
        NotificationChannel channel,
        String title,
        String content,
        String relatedEntity,
        UUID relatedEntityId,
        NotificationStatus status,
        LocalDateTime sentAt,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        UUID recipientId = n.getRecipient() != null ? n.getRecipient().getProfileId() : null;
        String recipientName = n.getRecipient() != null ? n.getRecipient().getFullName() : null;
        return new NotificationResponse(n.getNotificationId(), recipientId, recipientName,
                n.getNotificationType(), n.getChannel(), n.getTitle(), n.getContent(),
                n.getRelatedEntity(), n.getRelatedEntityId(), n.getStatus(),
                n.getSentAt(), n.getReadAt(), n.getCreatedAt());
    }
}




