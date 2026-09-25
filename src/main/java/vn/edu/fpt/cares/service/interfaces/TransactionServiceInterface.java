package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.transaction.TransactionResponse;
import vn.edu.fpt.cares.dto.transaction.TransactionCreateRequest;
import vn.edu.fpt.cares.dto.transaction.TransactionUpdateRequest;
import vn.edu.fpt.cares.model.Transaction;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/** Service interface for Transaction management. */
public interface TransactionServiceInterface {
    PageResponse<TransactionResponse> search(UUID invoiceId, vn.edu.fpt.cares.enums.TransactionStatus status,
                                              LocalDateTime from, LocalDateTime to, Pageable pageable);
    TransactionResponse get(UUID id);
    TransactionResponse create(TransactionCreateRequest req);
    TransactionResponse update(UUID id, TransactionUpdateRequest req);
    TransactionResponse confirm(UUID id);
    TransactionResponse fail(UUID id);
    void delete(UUID id);
    Transaction findById(UUID id);
}



