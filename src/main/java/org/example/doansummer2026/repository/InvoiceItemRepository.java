package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

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
}


