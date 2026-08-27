package org.example.doansummer2026.dto.attendance;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;
public record AdjustmentRequest(@NotNull UUID scheduleId,@NotBlank @Size(max=1000) String reason,LocalDateTime requestedCheckIn,LocalDateTime requestedCheckOut) {}
