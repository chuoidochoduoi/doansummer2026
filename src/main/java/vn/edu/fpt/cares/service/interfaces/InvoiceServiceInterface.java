package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.invoice.InvoiceResponse;
import vn.edu.fpt.cares.dto.invoice.InvoiceCreateRequest;
import vn.edu.fpt.cares.dto.invoice.InvoiceUpdateRequest;
import vn.edu.fpt.cares.model.Invoice;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

/** Service interface for Invoice management. */
public interface InvoiceServiceInterface {
    PageResponse<InvoiceResponse> search(UUID customerId, vn.edu.fpt.cares.enums.InvoiceStatus status,
                                          String search, String category,
                                          LocalDate from, LocalDate to, Pageable pageable);
    InvoiceResponse get(UUID id);
    InvoiceResponse create(InvoiceCreateRequest req);
    InvoiceResponse update(UUID id, InvoiceUpdateRequest req);
    InvoiceResponse issue(UUID id);
    InvoiceResponse cancel(UUID id);
    InvoiceResponse pay(UUID id, UUID receivedById);
    void delete(UUID id);
    Invoice findById(UUID id);
    void recalculatePaidAmount(UUID id);
}



