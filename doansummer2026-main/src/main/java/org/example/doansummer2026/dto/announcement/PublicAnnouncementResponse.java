package org.example.doansummer2026.dto.announcement;

import org.example.doansummer2026.model.PublicAnnouncement;

import java.time.LocalDateTime;
import java.util.UUID;

public record PublicAnnouncementResponse(
        UUID announcementId,
        String title,
        String content,
        Boolean published,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        UUID createdByAccountId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean currentlyVisible
) {
    public static PublicAnnouncementResponse from(PublicAnnouncement value) {
        LocalDateTime now = LocalDateTime.now();
        boolean visible = Boolean.TRUE.equals(value.getPublished())
                && (value.getStartsAt() == null || !value.getStartsAt().isAfter(now))
                && (value.getEndsAt() == null || !value.getEndsAt().isBefore(now));
        return new PublicAnnouncementResponse(
                value.getAnnouncementId(), value.getTitle(), value.getContent(),
                value.getPublished(), value.getStartsAt(), value.getEndsAt(),
                value.getCreatedByAccountId(), value.getCreatedAt(), value.getUpdatedAt(), visible
        );
    }
}
