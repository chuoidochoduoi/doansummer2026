package org.example.doansummer2026.dto.invoice;

import org.example.doansummer2026.enums.PaymentMethod;
import org.example.doansummer2026.enums.TransactionStatus;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Du lieu day du de in phieu thu tai quay, khong lam thay doi model hoa don. */
public record ReceiptPrintResponse(
        UUID invoiceId,
        String invoiceCode,
        String receiptNumber,
        LocalDateTime issuedAt,
        LocalDateTime paidAt,
        String patientCode,
        String patientName,
        String patientPhone,
        String patientAddress,
        String dateOfBirth,
        String gender,
        String bhytCode,
        String paymentMethod,
        String cashierName,
        BigDecimal subtotal,
        BigDecimal bhytAmount,
        BigDecimal tax,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balance,
        String note,
        List<InvoiceItemResponse> items
) {
    public static ReceiptPrintResponse from(Invoice invoice, Transaction payment) {
        var patient = invoice.getCustomer();
        String gender = patient == null || patient.getGender() == null ? null : switch (patient.getGender()) {
            case MALE -> "Nam";
            case FEMALE -> "Nu";
            default -> "Khac";
        };
        PaymentMethod method = payment != null ? payment.getPaymentMethod() : null;
        String cashierName = payment != null && payment.getReceivedBy() != null
                && payment.getReceivedBy().getProfile() != null
                ? payment.getReceivedBy().getProfile().getFullName() : null;
        BigDecimal paidAmount = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal bhytAmount = invoice.getItems().stream()
                .map(item -> item.getBhytFund() != null ? item.getBhytFund() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReceiptPrintResponse(
                invoice.getInvoiceId(),
                invoice.getInvoiceCode(),
                payment != null ? payment.getTransactionCode() : invoice.getInvoiceCode(),
                invoice.getCreatedAt(),
                payment != null ? payment.getPaidAt() : null,
                patient != null ? patient.getPatientCode() : null,
                patient != null ? patient.getFullName() : null,
                patient != null ? patient.getPhone() : null,
                patient != null ? patient.getAddress() : null,
                patient != null && patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : null,
                gender,
                patient != null ? patient.getInsuranceId() : null,
                paymentMethodLabel(method),
                cashierName,
                invoice.getSubtotal(), bhytAmount, invoice.getTax(), totalAmount, paidAmount,
                totalAmount.subtract(paidAmount), invoice.getNote(),
                invoice.getItems().stream().map(InvoiceItemResponse::from).toList()
        );
    }

    private static String paymentMethodLabel(PaymentMethod method) {
        if (method == null) return null;
        return switch (method) {
            case CASH -> "Tien mat";
            case CARD -> "The ngan hang";
            case BANK_TRANSFER -> "Chuyen khoan";
            case MOMO -> "Vi MoMo";
            case VNPAY -> "VNPay";
            case ZALOPAY -> "ZaloPay";
            case INSURANCE -> "Bao hiem";
            case OTHER -> "Khac";
        };
    }
}
