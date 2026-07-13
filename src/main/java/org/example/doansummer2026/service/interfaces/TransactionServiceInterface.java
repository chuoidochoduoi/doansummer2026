package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.transaction.TransactionResponse;
import org.example.doansummer2026.dto.transaction.TransactionCreateRequest;
import org.example.doansummer2026.dto.transaction.TransactionUpdateRequest;
import org.example.doansummer2026.model.Transaction;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/** Service interface for Transaction management. */
public interface TransactionServiceInterface {
    PageResponse<TransactionResponse> search(UUID invoiceId, org.example.doansummer2026.enums.TransactionStatus status,
                                              LocalDateTime from, LocalDateTime to, Pageable pageable);
    TransactionResponse get(UUID id);
    TransactionResponse create(TransactionCreateRequest req);
    TransactionResponse update(UUID id, TransactionUpdateRequest req);
    TransactionResponse confirm(UUID id);
    TransactionResponse fail(UUID id);
    void delete(UUID id);
    Transaction findById(UUID id);
}