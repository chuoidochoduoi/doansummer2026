package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Transaction;
import org.example.doansummer2026.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.transactionId = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") UUID id);

    Optional<Transaction> findByTransactionCode(String transactionCode);

    boolean existsByTransactionCode(String transactionCode);

    List<Transaction> findByInvoice_InvoiceId(UUID invoiceId);

    Optional<Transaction> findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(UUID invoiceId, TransactionStatus status);

    @Query(value = """
            SELECT t FROM Transaction t
            WHERE (:invoiceId IS NULL OR t.invoice.invoiceId = :invoiceId)
              AND (:status IS NULL OR t.status = :status)
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to IS NULL OR t.createdAt <= :to)
            """,
            countQuery = """
            SELECT COUNT(t) FROM Transaction t
            WHERE (:invoiceId IS NULL OR t.invoice.invoiceId = :invoiceId)
              AND (:status IS NULL OR t.status = :status)
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to IS NULL OR t.createdAt <= :to)
            """)
    Page<Transaction> search(@Param("invoiceId") UUID invoiceId,
                              @Param("status") TransactionStatus status,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              Pageable pageable);
}


