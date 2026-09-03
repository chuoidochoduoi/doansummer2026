package org.example.doansummer2026.dto.journey;

import jakarta.validation.constraints.NotBlank;

public record GuestQueueReturnRequest(
        @NotBlank String visitCode,
        @NotBlank String phone) {
}
