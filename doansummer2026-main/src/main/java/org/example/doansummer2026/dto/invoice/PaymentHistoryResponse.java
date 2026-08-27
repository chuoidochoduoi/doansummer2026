package org.example.doansummer2026.dto.invoice;

import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.enums.PaymentMethod;
import org.example.doansummer2026.enums.TransactionStatus;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * DTO cho lich su thanh toan cua benh nhan.
 */
public record PaymentHistoryResponse(
        UUID id,
        String invoiceId,
        String description,
        String settlementDate,
        String settlementTime,
        BigDecimal amount,
        String paymentMethod,
        String status
) {
    public static PaymentHistoryResponse from(Invoice invoice, Transaction latestTx) {
        String description = "Thanh toán dịch vụ khám bệnh";
        String settlementDate = null;
        String settlementTime = null;

        if (latestTx != null && latestTx.getPaidAt() != null) {
            settlementDate = latestTx.getPaidAt().toLocalDate().toString();
            settlementTime = latestTx.getPaidAt().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (invoice.getIssueDate() != null) {
            settlementDate = invoice.getIssueDate().toString();
        }

        String paymentMethodStr = null;
        if (latestTx != null && latestTx.getPaymentMethod() != null) {
            paymentMethodStr = latestTx.getPaymentMethod().getDisplayName();
        }

        String statusStr = "pending";
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            statusStr = "paid";
        } else if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            statusStr = "cancelled";
        }

        return new PaymentHistoryResponse(
                invoice.getInvoiceId(),
                invoice.getInvoiceCode(),
                description,
                settlementDate,
                settlementTime,
                invoice.getTotalAmount(),
                paymentMethodStr,
                statusStr
        );
    }
}