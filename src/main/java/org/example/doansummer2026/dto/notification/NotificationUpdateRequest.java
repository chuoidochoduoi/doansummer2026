package org.example.doansummer2026.dto.notification;

import org.example.doansummer2026.enums.NotificationStatus;

public record NotificationUpdateRequest(
        NotificationStatus status,
        String failureReason
) {}




