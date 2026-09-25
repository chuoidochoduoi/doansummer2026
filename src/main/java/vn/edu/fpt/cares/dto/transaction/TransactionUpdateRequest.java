package vn.edu.fpt.cares.dto.transaction;

import vn.edu.fpt.cares.enums.TransactionStatus;

import java.time.LocalDateTime;

public record TransactionUpdateRequest(
        TransactionStatus status,
        LocalDateTime paidAt,
        String gatewayReference,
        String note
) {}




