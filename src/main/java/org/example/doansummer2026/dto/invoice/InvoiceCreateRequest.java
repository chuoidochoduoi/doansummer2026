package org.example.doansummer2026.dto.invoice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceCreateRequest(
        @NotNull UUID customerId,
        UUID visitId,
        UUID medicalRecordId,
        LocalDate dueDate,
        @PositiveOrZero BigDecimal discount,
        @PositiveOrZero BigDecimal tax,
        String note,
        UUID issuedById,  // StaffInfo ID (nullable - khong bat buoc)
        List<InvoiceItemCreateRequest> items
) {}
