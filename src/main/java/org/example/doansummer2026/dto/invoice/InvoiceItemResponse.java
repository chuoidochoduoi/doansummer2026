package org.example.doansummer2026.dto.invoice;

import org.example.doansummer2026.model.InvoiceItem;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID itemId,
        UUID serviceId,
        String serviceName,
        String serviceSnapshot,
        String serviceCodeSnapshot,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal,
        BigDecimal bhytRate,
        BigDecimal bhytAmount,
        BigDecimal patientAmount,
        String note
) {
    public static InvoiceItemResponse from(InvoiceItem i) {
        UUID serviceId = i.getService() != null ? i.getService().getServiceId() : null;
        String serviceName = i.getService() != null ? i.getService().getName() : i.getServiceSnapshot();
        return new InvoiceItemResponse(i.getItemId(), serviceId, serviceName, i.getServiceSnapshot(),
                i.getServiceCodeSnapshot(), i.getUnitPrice(), i.getQuantity(),
                i.getLineTotal(), i.getDiscountPercent(), i.getBhytFund(), i.getFinalPrice(), i.getNote());
    }
}




