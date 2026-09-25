package vn.edu.fpt.cares.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.cares.enums.NotificationChannel;
import vn.edu.fpt.cares.enums.NotificationType;

import java.util.UUID;

public record NotificationCreateRequest(
        @NotNull UUID recipientId,
        @NotNull NotificationType notificationType,
        @NotNull NotificationChannel channel,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        @Size(max = 50) String relatedEntity,
        UUID relatedEntityId
) {}




