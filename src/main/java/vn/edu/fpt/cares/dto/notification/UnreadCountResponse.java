package vn.edu.fpt.cares.dto.notification;

import java.util.UUID;

public record UnreadCountResponse(
        UUID recipientId,
        long unreadCount
) {}




