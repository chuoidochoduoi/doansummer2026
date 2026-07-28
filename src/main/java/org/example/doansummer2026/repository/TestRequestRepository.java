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
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestRequestRepository extends JpaRepository<TestRequest, UUID> {

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
            """,
            countQuery = """
            SELECT COUNT(t) FROM TestRequest t
            WHERE (:recordId IS NULL OR t.medicalRecord.recordId = :recordId)
              AND (:departmentId IS NULL OR t.performingDepartment.departmentId = :departmentId)
              AND (:status IS NULL OR t.status = :status)
            """)
    Page<TestRequest> search(@Param("recordId") UUID recordId,
                              @Param("departmentId") UUID departmentId,
                              @Param("status") TestRequestStatus status,
                              Pageable pageable);

    /** Tim TestRequest kem theo TestResult va MedicalRecord/Visit - dung cho truong hop completeResult */
    @Query("SELECT t FROM TestRequest t " +
           "LEFT JOIN FETCH t.testResult " +
           "LEFT JOIN FETCH t.medicalRecord mr " +
           "LEFT JOIN FETCH mr.visit v " +
           "WHERE t.testRequestId = :id")
    Optional<TestRequest> findByIdWithResult(@Param("id") UUID id);

    /** Dem so yeu cau xet nghiem chua hoan thanh (PENDING hoac IN_PROGRESS)
     * cua mot medical record de kiem tra khi ket luuan.
     */
    @Query("SELECT COUNT(t) FROM TestRequest t WHERE t.medicalRecord.recordId = :recordId " +
           "AND t.status IN :statuses")
    long countByMedicalRecordAndStatusIn(@Param("recordId") UUID recordId,
                                          @Param("statuses") List<TestRequestStatus> statuses);

    @Query("SELECT COUNT(t) FROM TestRequest t WHERE t.medicalRecord.recordId = :recordId")
    long countByMedicalRecord_MedicalRecordId(@Param("recordId") UUID recordId);

    /** Tim TestRequest da hoan thanh cua Profile qua Visit -> MedicalRecord */
    @Query("SELECT t FROM TestRequest t " +
           "LEFT JOIN FETCH t.service " +
           "LEFT JOIN FETCH t.medicalRecord mr " +
           "LEFT JOIN FETCH mr.visit v " +
           "WHERE v.customer.profileId = :profileId AND t.status = 'COMPLETED'")
    List<TestRequest> findByProfileIdAndStatusCompleted(@Param("profileId") UUID profileId);
}