package org.example.doansummer2026.dto.invoice;

import org.example.doansummer2026.model.InvoiceItem;

import java.math.BigDecimal;

/**
 * DTO cho chi tiet dich vu trong phieu thu.
 */
public record ReceiptItemResponse(
        String name,
        String category,
        Integer qty,
        BigDecimal unitPrice,
        Integer bhytRate
) {
    public static ReceiptItemResponse from(InvoiceItem item) {
        String category = item.getService() != null ? item.getService().getName() : "Khám bệnh";
        return new ReceiptItemResponse(
                item.getServiceSnapshot(),
                category,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getBhytFund() != null && item.getLineTotal() != null && item.getLineTotal().compareTo(BigDecimal.ZERO) > 0
                        ? item.getBhytFund().multiply(BigDecimal.valueOf(100))
                                  .divide(item.getLineTotal(), 0, java.math.RoundingMode.HALF_UP).intValue()
                        : 0
        );
    }
}