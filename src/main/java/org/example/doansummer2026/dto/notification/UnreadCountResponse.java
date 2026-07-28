package org.example.doansummer2026.dto.notification;

import java.util.UUID;

public record UnreadCountResponse(
        UUID recipientId,
        long unreadCount
) {}




