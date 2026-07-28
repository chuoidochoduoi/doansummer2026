package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID>, JpaSpecificationExecutor<MedicalRecord> {

    Optional<MedicalRecord> findByVisit_VisitId(UUID visitId);

    @EntityGraph("MedicalRecord.withDetails")
    @Query("SELECT m FROM MedicalRecord m WHERE m.visit.visitId = :visitId")
    Optional<MedicalRecord> getWithDetailsByVisitId(@Param("visitId") UUID visitId);

    @Query("SELECT m.recordCode FROM MedicalRecord m WHERE m.recordCode LIKE :prefix ORDER BY m.recordCode DESC LIMIT 1")
    String findTopByRecordCodeStartingWithOrderByRecordCodeDesc(@Param("prefix") String prefix);

    default Page<MedicalRecord> search(UUID doctorId, MedicalRecordStatus status,
                                        LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<MedicalRecord> spec = (root, query, cb) -> cb.conjunction();

        if (doctorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("doctor").get("staffId"), doctorId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return findAll(spec, pageable);
    }
}