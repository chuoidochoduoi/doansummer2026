package org.example.doansummer2026.dto.invoice;

import org.example.doansummer2026.model.InvoiceItem;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID itemId,
        UUID serviceId,
        String serviceSnapshot,
        String serviceCodeSnapshot,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal,
        String note
) {
    public static InvoiceItemResponse from(InvoiceItem i) {
        UUID serviceId = i.getService() != null ? i.getService().getServiceId() : null;
        return new InvoiceItemResponse(i.getItemId(), serviceId, i.getServiceSnapshot(),
                i.getServiceCodeSnapshot(), i.getUnitPrice(), i.getQuantity(),
                i.getLineTotal(), i.getNote());
    }
}
