package org.example.doansummer2026.dto.transaction;

import org.example.doansummer2026.enums.PaymentMethod;
import org.example.doansummer2026.model.Transaction;
import org.example.doansummer2026.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        UUID invoiceId,
        String invoiceCode,
        String transactionCode,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        TransactionStatus status,
        LocalDateTime paidAt,
        String gatewayReference,
        String note,
        UUID receivedById,
        String receivedByName,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction t) {
        UUID invoiceId = t.getInvoice() != null ? t.getInvoice().getInvoiceId() : null;
        String invoiceCode = t.getInvoice() != null ? t.getInvoice().getInvoiceCode() : null;
        UUID recById = t.getReceivedBy() != null ? t.getReceivedBy().getStaffId() : null;
        String recByName = t.getReceivedBy() != null ? t.getReceivedBy().getStaffCode() : null;
        return new TransactionResponse(t.getTransactionId(), invoiceId, invoiceCode,
                t.getTransactionCode(), t.getAmount(), t.getPaymentMethod(), t.getStatus(),
                t.getPaidAt(), t.getGatewayReference(), t.getNote(), recById, recByName, t.getCreatedAt());
    }
}




