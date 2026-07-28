package org.example.doansummer2026.dto.transaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.example.doansummer2026.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCreateRequest(
        @NotNull UUID invoiceId,
        @NotNull @PositiveOrZero BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,
        String gatewayReference,
        String note,
        UUID receivedById
) {}




