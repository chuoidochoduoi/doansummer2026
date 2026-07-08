package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TestRequestRepository extends JpaRepository<TestRequest, UUID> {

    @Query(value = """
            SELECT t FROM TestRequest t
            WHERE (:recordId IS NULL OR t.medicalRecord.recordId = :recordId)
              AND (:departmentId IS NULL OR t.performingDepartment.departmentId = :departmentId)
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
}