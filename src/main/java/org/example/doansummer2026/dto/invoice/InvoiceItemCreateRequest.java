package org.example.doansummer2026.dto.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemCreateRequest(
        UUID serviceId,
        @NotBlank @Size(max = 200) String serviceSnapshot,
        @Size(max = 50) String serviceCodeSnapshot,
        @NotNull @PositiveOrZero BigDecimal unitPrice,
        @NotNull @Positive Integer quantity,
        String note
) {}




