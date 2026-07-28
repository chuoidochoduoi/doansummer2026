package org.example.doansummer2026.dto.transaction;

import org.example.doansummer2026.enums.TransactionStatus;

import java.time.LocalDateTime;

public record TransactionUpdateRequest(
        TransactionStatus status,
        LocalDateTime paidAt,
        String gatewayReference,
        String note
) {}




