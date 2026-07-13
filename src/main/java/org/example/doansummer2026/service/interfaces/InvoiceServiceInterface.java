package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.invoice.InvoiceResponse;
import org.example.doansummer2026.dto.invoice.InvoiceCreateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceUpdateRequest;
import org.example.doansummer2026.model.Invoice;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

/** Service interface for Invoice management. */
public interface InvoiceServiceInterface {
    PageResponse<InvoiceResponse> search(UUID customerId, org.example.doansummer2026.enums.InvoiceStatus status,
                                          LocalDate from, LocalDate to, Pageable pageable);
    InvoiceResponse get(UUID id);
    InvoiceResponse create(InvoiceCreateRequest req);
    InvoiceResponse update(UUID id, InvoiceUpdateRequest req);
    InvoiceResponse issue(UUID id);
    InvoiceResponse cancel(UUID id);
    InvoiceResponse pay(UUID id);
    void delete(UUID id);
    Invoice findById(UUID id);
    void recalculatePaidAmount(UUID id);
}