package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {

    List<InvoiceItem> findByInvoice_InvoiceId(UUID invoiceId);

    /**
     * Nguon dong dich vu chinh thuc cua hoa don khi dieu phoi sau thanh toan.
     * Khong phu thuoc collection Invoice.items dang LAZY hoac entity vua save.
     */
    @Query("SELECT ii FROM InvoiceItem ii LEFT JOIN FETCH ii.service WHERE ii.invoice.invoiceId = :invoiceId")
    List<InvoiceItem> findAllWithServiceByInvoiceId(@Param("invoiceId") UUID invoiceId);

    /** Tính tong BHYT theo serviceId (cho ServiceStat). */
    @Query("SELECT SUM(ii.bhytFund) FROM InvoiceItem ii WHERE ii.service.serviceId = :serviceId")
    java.math.BigDecimal sumBhytFundByServiceId(@Param("serviceId") UUID serviceId);

    /** Dem so lan goi dich vu (cho totalOrders). */
    @Query("SELECT COUNT(ii) FROM InvoiceItem ii WHERE ii.service.serviceId = :serviceId")
    long countByServiceId(@Param("serviceId") UUID serviceId);

    /** Dem so lan BHYT duoc su dung (bhytQty). */
    @Query("SELECT COUNT(ii) FROM InvoiceItem ii WHERE ii.service.serviceId = :serviceId AND ii.bhytFund > 0")
    long countBhytUsageByServiceId(@Param("serviceId") UUID serviceId);

    @Query(value = """
            SELECT COUNT(*) FROM invoice_item ii
            JOIN invoice i ON i.invoice_id = ii.invoice_id
            JOIN medical_service ms ON ms.service_id = ii.service_id
            WHERE i.visit_id = :visitId AND i.status <> 'CANCELLED'
              AND ms.department_type = 'EXAMINATION'
            """, nativeQuery = true)
    long countExaminationItemsByVisit(@Param("visitId") UUID visitId);

    @Query(value = """
            SELECT COUNT(*) FROM invoice_item ii
            JOIN invoice i ON i.invoice_id = ii.invoice_id
            JOIN medical_service ms ON ms.service_id = ii.service_id
            WHERE i.visit_id = :visitId AND i.invoice_id <> :invoiceId
              AND i.status <> 'CANCELLED' AND ms.department_type = 'EXAMINATION'
            """, nativeQuery = true)
    long countExaminationItemsByVisitExcludingInvoice(
            @Param("visitId") UUID visitId, @Param("invoiceId") UUID invoiceId);

    @Query(value = """
            SELECT DISTINCT ii.service_id FROM invoice_item ii
            JOIN invoice i ON i.invoice_id = ii.invoice_id
            JOIN medical_service ms ON ms.service_id = ii.service_id
            WHERE i.visit_id = :visitId AND i.status <> 'CANCELLED'
              AND ms.department_type = 'EXAMINATION'
              AND (:excludedInvoiceId IS NULL OR i.invoice_id <> :excludedInvoiceId)
            """, nativeQuery = true)
    List<UUID> findDistinctExaminationServiceIdsByVisit(
            @Param("visitId") UUID visitId,
            @Param("excludedInvoiceId") UUID excludedInvoiceId);

    @Query(value = """
            SELECT DISTINCT ii.service_id FROM invoice_item ii
            JOIN invoice i ON i.invoice_id = ii.invoice_id
            WHERE i.visit_id = :visitId AND i.status <> 'CANCELLED'
              AND (:excludedInvoiceId IS NULL OR i.invoice_id <> :excludedInvoiceId)
            """, nativeQuery = true)
    List<UUID> findDistinctActiveServiceIdsByVisit(
            @Param("visitId") UUID visitId,
            @Param("excludedInvoiceId") UUID excludedInvoiceId);

    @Query("""
            SELECT ii FROM InvoiceItem ii
            JOIN FETCH ii.invoice i
            JOIN FETCH i.visit v
            JOIN FETCH ii.service s
            WHERE v.customer.profileId = :profileId
              AND v.checkInTime >= :from
              AND v.checkInTime < :to
              AND v.status <> org.example.doansummer2026.enums.VisitStatus.CANCELLED
              AND i.status <> org.example.doansummer2026.enums.InvoiceStatus.CANCELLED
              AND s.departmentType = org.example.doansummer2026.enums.DepartmentType.EXAMINATION
              AND (:excludedInvoiceId IS NULL OR i.invoiceId <> :excludedInvoiceId)
            ORDER BY v.checkInTime DESC
            """)
    List<InvoiceItem> findSameDayExaminationRegistrations(
            @Param("profileId") UUID profileId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("excludedInvoiceId") UUID excludedInvoiceId);
}
