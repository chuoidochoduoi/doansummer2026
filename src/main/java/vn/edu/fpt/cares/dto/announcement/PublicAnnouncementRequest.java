package vn.edu.fpt.cares.dto.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record PublicAnnouncementRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 3000) String content,
        Boolean published,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {}
