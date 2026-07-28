package org.example.doansummer2026.dto.invoice;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceUpdateRequest(
        LocalDate dueDate,
        @PositiveOrZero BigDecimal discount,
        @PositiveOrZero BigDecimal tax,
        String note,
        List<InvoiceItemCreateRequest> items
) {}




