package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestRequestRepository extends JpaRepository<TestRequest, UUID> {
    List<TestRequest> findAllByMedicalRecord_Visit_VisitId(UUID visitId);

    /**
     * Tat ca yeu cau CLS cua mot luot kham, bao gom ca yeu cau dang cho ket qua.
     * Khong lay qua mot MedicalRecord cu the, vi mot visit co the co record CLS
     * standalone va nhieu record kham chuyen khoa.
     */
    @Query("""
            SELECT t FROM TestRequest t
            LEFT JOIN FETCH t.service
            LEFT JOIN FETCH t.performingDepartment
            LEFT JOIN FETCH t.testResult tr
            LEFT JOIN FETCH tr.performedBy performer
            LEFT JOIN FETCH performer.profile
            LEFT JOIN FETCH tr.collectedBy collector
            LEFT JOIN FETCH collector.profile
            WHERE t.medicalRecord.visit.visitId = :visitId
            ORDER BY t.createdAt ASC
            """)
    List<TestRequest> findAllByVisitIdWithDetails(@Param("visitId") UUID visitId);

    /**
     * Moi dich vu can lam sang chi duoc chi dinh mot lan trong cung mot luot kham.
     * Ban ghi da huy khong chan viec chi dinh lai.
     */
    boolean existsByMedicalRecord_Visit_VisitIdAndService_ServiceIdAndStatusNot(
            UUID visitId, UUID serviceId, TestRequestStatus status);

    @Query(value = """
            SELECT DISTINCT t FROM TestRequest t
            LEFT JOIN FETCH t.medicalRecord mr
            LEFT JOIN FETCH mr.visit v
            LEFT JOIN FETCH v.customer c
            LEFT JOIN FETCH t.service s
            LEFT JOIN FETCH t.performingDepartment d
            WHERE (:recordId IS NULL OR mr.recordId = :recordId)
              AND (:departmentId IS NULL OR d.departmentId = :departmentId)
              AND (:status IS NULL OR t.status = :status)
              AND (CAST(:workDate AS date) IS NULL OR CAST(t.createdAt AS date) = :workDate)
              AND (:search = '' OR LOWER(c.fullName) LIKE CONCAT('%', :search, '%')
                   OR LOWER(c.phone) LIKE CONCAT('%', :search, '%')
                   OR LOWER(s.name) LIKE CONCAT('%', :search, '%'))
            """,
            countQuery = """
            SELECT COUNT(t) FROM TestRequest t
            WHERE (:recordId IS NULL OR t.medicalRecord.recordId = :recordId)
              AND (:departmentId IS NULL OR t.performingDepartment.departmentId = :departmentId)
              AND (:status IS NULL OR t.status = :status)
              AND (CAST(:workDate AS date) IS NULL OR CAST(t.createdAt AS date) = :workDate)
              AND (:search = '' OR LOWER(t.medicalRecord.visit.customer.fullName) LIKE CONCAT('%', :search, '%')
                   OR LOWER(t.medicalRecord.visit.customer.phone) LIKE CONCAT('%', :search, '%')
                   OR LOWER(t.service.name) LIKE CONCAT('%', :search, '%'))
            """)
    Page<TestRequest> search(@Param("recordId") UUID recordId,
                              @Param("departmentId") UUID departmentId,
                              @Param("status") TestRequestStatus status,
                              @Param("search") String search,
                              @Param("workDate") LocalDate workDate,
                              Pageable pageable);

    /** Tim TestRequest kem theo TestResult va MedicalRecord/Visit - dung cho truong hop completeResult */
    @Query("SELECT t FROM TestRequest t " +
           "LEFT JOIN FETCH t.testResult " +
           "LEFT JOIN FETCH t.medicalRecord mr " +
           "LEFT JOIN FETCH mr.visit v " +
           "WHERE t.testRequestId = :id")
    Optional<TestRequest> findByIdWithResult(@Param("id") UUID id);

    /** Khoa yeu cau khi bac si ky ket qua, tranh hai phien cung hoan thanh mot ket qua. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TestRequest t WHERE t.testRequestId = :id")
    Optional<TestRequest> findByIdForUpdate(@Param("id") UUID id);

    /** Dem so yeu cau xet nghiem chua hoan thanh (PENDING hoac IN_PROGRESS)
     * cua mot medical record de kiem tra khi ket luuan.
     */
    @Query("SELECT COUNT(t) FROM TestRequest t WHERE t.medicalRecord.recordId = :recordId " +
           "AND t.status IN :statuses")
    long countByMedicalRecordAndStatusIn(@Param("recordId") UUID recordId,
                                          @Param("statuses") List<TestRequestStatus> statuses);

    @Query("SELECT COUNT(t) FROM TestRequest t WHERE t.medicalRecord.recordId = :recordId")
    long countByMedicalRecord_MedicalRecordId(@Param("recordId") UUID recordId);

    long countByPerformingDepartment_DepartmentIdAndStatusIn(UUID departmentId, List<TestRequestStatus> statuses);

    long countByQueueTicket_TicketIdAndStatusIn(UUID ticketId, List<TestRequestStatus> statuses);
    long countByQueueTicket_TicketId(UUID ticketId);
    List<TestRequest> findAllByQueueTicket_TicketId(UUID ticketId);

    /** Tim TestRequest da hoan thanh cua Profile qua Visit -> MedicalRecord */
    @Query("SELECT t FROM TestRequest t " +
           "LEFT JOIN FETCH t.service " +
           "LEFT JOIN FETCH t.medicalRecord mr " +
           "LEFT JOIN FETCH mr.visit v " +
           "WHERE v.customer.profileId = :profileId AND t.status = 'COMPLETED'")
    List<TestRequest> findByProfileIdAndStatusCompleted(@Param("profileId") UUID profileId);

    /** Tim TestRequest theo InvoiceItem (traceability: Invoice -> InvoiceItem -> TestRequest). */
    List<TestRequest> findByInvoiceItem_ItemId(UUID itemId);

    /** Tim TestRequest theo Invoice (qua InvoiceItem). */
    @Query("SELECT t FROM TestRequest t " +
           "LEFT JOIN FETCH t.service " +
           "LEFT JOIN FETCH t.invoiceItem ii " +
           "LEFT JOIN FETCH ii.invoice i " +
           "WHERE i.invoiceId = :invoiceId")
    List<TestRequest> findByInvoiceId(@Param("invoiceId") UUID invoiceId);
}
