package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    boolean existsByInvoiceCode(String invoiceCode);

    @org.springframework.data.jpa.repository.Query(
            "SELECT DISTINCT i FROM Invoice i " +
            "LEFT JOIN FETCH i.items it LEFT JOIN FETCH it.service " +
            "WHERE (:customerId IS NULL OR i.customer.profileId = :customerId) " +
            "AND (:status IS NULL OR i.status = :status) " +
            "AND (:from IS NULL OR i.issueDate >= :from) " +
            "AND (:to IS NULL OR i.issueDate <= :to)"
    )
    Page<Invoice> search(@org.springframework.data.repository.query.Param("customerId") UUID customerId,
                         @org.springframework.data.repository.query.Param("status") InvoiceStatus status,
                         @org.springframework.data.repository.query.Param("from") LocalDate from,
                         @org.springframework.data.repository.query.Param("to") LocalDate to,
                         Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.items it LEFT JOIN FETCH it.service WHERE i.invoiceId = :id")
    Optional<Invoice> findById(@org.springframework.data.repository.query.Param("id") UUID id);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.items it LEFT JOIN FETCH it.service LEFT JOIN FETCH i.visit LEFT JOIN FETCH i.medicalRecord LEFT JOIN FETCH i.issuedBy WHERE i.invoiceId = :id")
    Optional<Invoice> getWithDetailsByInvoiceId(@org.springframework.data.repository.query.Param("id") UUID id);

    // Search dung Specification vi filter paymentMethod qua Transaction phuc tap
    // Method nao day duoc trien khai trong InvoiceService.searchForPatientSpec()
}



