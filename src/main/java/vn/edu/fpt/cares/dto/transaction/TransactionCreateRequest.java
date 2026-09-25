package vn.edu.fpt.cares.dto.transaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import vn.edu.fpt.cares.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCreateRequest(
        @NotNull UUID invoiceId,
        @NotNull @Positive BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,
        String gatewayReference,
        String note,
        UUID receivedById
) {}




