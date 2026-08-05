package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    boolean existsByInvoiceCode(String invoiceCode);
    List<Invoice> findAllByVisit_VisitId(UUID visitId);
    List<Invoice> findAllByMedicalRecord_RecordId(UUID recordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.invoiceId = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") UUID id);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.items it LEFT JOIN FETCH it.service WHERE i.invoiceId = :id")
    Optional<Invoice> findById(@org.springframework.data.repository.query.Param("id") UUID id);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.items it LEFT JOIN FETCH it.service LEFT JOIN FETCH i.visit LEFT JOIN FETCH i.medicalRecord LEFT JOIN FETCH i.issuedBy WHERE i.invoiceId = :id")
    Optional<Invoice> getWithDetailsByInvoiceId(@org.springframework.data.repository.query.Param("id") UUID id);

    // Search dung Specification vi filter paymentMethod qua Transaction phuc tap
    // Method nao day duoc trien khai trong InvoiceService.searchForPatientSpec()
}
