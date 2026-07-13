package org.example.doansummer2026.dto.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Yeu cau tao hoa don moi.
 * - customerId (optional): null cho khach vang lai.
 */
public record InvoiceCreateRequest(
        UUID customerId,
        UUID visitId,
        UUID medicalRecordId,
        LocalDate dueDate,
        BigDecimal discount,
        BigDecimal tax,
        String note,
        UUID issuedById,
        List<InvoiceItemCreateRequest> items
) {}