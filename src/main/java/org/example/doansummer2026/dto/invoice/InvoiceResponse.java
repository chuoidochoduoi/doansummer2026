package org.example.doansummer2026.dto.invoice;

import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID invoiceId,
        String invoiceCode,
        UUID customerId,
        String customerName,
        UUID visitId,
        UUID medicalRecordId,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal tax,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balance,
        InvoiceStatus status,
        String note,
        UUID issuedById,
        String issuedByName,
        List<InvoiceItemResponse> items,
        List<UUID> transactionIds
) {
    public static InvoiceResponse from(Invoice i) {
        return from(i, List.of());
    }

    public static InvoiceResponse from(Invoice i, List<UUID> transactionIds) {
        UUID customerId = i.getCustomer() != null ? i.getCustomer().getProfileId() : null;
        String customerName = i.getCustomer() != null ? i.getCustomer().getFullName() : null;
        UUID visitId = i.getVisit() != null ? i.getVisit().getVisitId() : null;
        UUID recordId = i.getMedicalRecord() != null ? i.getMedicalRecord().getRecordId() : null;
        UUID issuedById = i.getIssuedBy() != null ? i.getIssuedBy().getStaffId() : null;
        String issuedByName = i.getIssuedBy() != null ? i.getIssuedBy().getStaffCode() : null;
        BigDecimal balance = i.getTotalAmount().subtract(i.getPaidAmount());
        List<InvoiceItemResponse> items = i.getItems().stream().map(InvoiceItemResponse::from).toList();
        return new InvoiceResponse(i.getInvoiceId(), i.getInvoiceCode(), customerId, customerName,
                visitId, recordId, i.getIssueDate(), i.getDueDate(),
                i.getSubtotal(), i.getDiscount(), i.getTax(), i.getTotalAmount(), i.getPaidAmount(),
                balance, i.getStatus(), i.getNote(), issuedById, issuedByName, items, transactionIds);
    }
}




