package org.example.doansummer2026.dto.invoice;

import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.InvoiceItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO cho chi tiet phieu thu (receipt).
 */
public record ReceiptDetailResponse(
        UUID id,
        String invoiceId,
        String description,
        String issuedDate,
        String patientName,
        String patientId,
        String dob,
        String gender,
        String doctor,
        BigDecimal totalService,
        BigDecimal bhytCoverage,
        BigDecimal patientPayment,
        String inWords,
        List<ReceiptItemResponse> items
) {
    public static ReceiptDetailResponse from(Invoice invoice) {
        String patientName = invoice.getCustomer() != null ? invoice.getCustomer().getFullName() : null;
        String patientId = invoice.getCustomer() != null ? String.valueOf(invoice.getCustomer().getProfileId()) : null;
        String dob = invoice.getCustomer() != null && invoice.getCustomer().getDateOfBirth() != null
                ? invoice.getCustomer().getDateOfBirth().toString() : null;
        String gender = null;
        if (invoice.getCustomer() != null && invoice.getCustomer().getGender() != null) {
            gender = switch (invoice.getCustomer().getGender()) {
                case MALE -> "Nam";
                case FEMALE -> "Nữ";
                default -> "Khác";
            };
        }

        // Bỏ trường doctor vì chưa có trong model Appointment
        String doctor = null;
        if (invoice.getMedicalRecord() != null && invoice.getMedicalRecord().getDoctor() != null
                && invoice.getMedicalRecord().getDoctor().getProfile() != null) {
            doctor = invoice.getMedicalRecord().getDoctor().getProfile().getFullName();
        }

        BigDecimal bhytCoverage = invoice.getItems().stream()
                .map(item -> item.getBhytFund() != null ? item.getBhytFund() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal patientPayment = invoice.getTotalAmount().subtract(bhytCoverage);

        String inWords = convertToWords(patientPayment);

        List<ReceiptItemResponse> itemResponses = invoice.getItems().stream()
                .map(ReceiptItemResponse::from)
                .toList();

        return new ReceiptDetailResponse(
                invoice.getInvoiceId(),
                invoice.getInvoiceCode(),
                "Thanh toán dịch vụ khám bệnh",
                invoice.getIssueDate() != null ? invoice.getIssueDate().toString() : null,
                patientName,
                patientId,
                dob,
                gender,
                doctor,
                invoice.getSubtotal(),
                bhytCoverage,
                patientPayment,
                inWords,
                itemResponses
        );
    }

    private static String convertToWords(BigDecimal amount) {
        // Simplified - in production should use proper Vietnamese number to words converter
        long value = amount.longValue();
        if (value == 0) return "không đồng";
        return value + " đồng";
    }
}
