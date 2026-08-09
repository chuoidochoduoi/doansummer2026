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
import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID>, JpaSpecificationExecutor<MedicalRecord> {

    List<MedicalRecord> findAllByVisit_VisitIdOrderByCreatedAtAsc(UUID visitId);
    @EntityGraph("MedicalRecord.withDetails")
    Optional<MedicalRecord> findFirstByVisit_VisitIdOrderByCreatedAtDesc(UUID visitId);
    Optional<MedicalRecord> findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(UUID visitId);
    Page<MedicalRecord> findByRatingScoreIsNotNull(Pageable pageable);
    @Query("SELECT DISTINCT m FROM MedicalRecord m LEFT JOIN m.feedbackTargets ft WHERE m.ratingScore IS NOT NULL AND (m.doctor.staffId = :doctorId OR ft.staff.staffId = :doctorId)")
    Page<MedicalRecord> findFeedbacksForStaff(@Param("doctorId") UUID doctorId, Pageable pageable);
    Optional<MedicalRecord> findByQueueTicket_TicketId(UUID ticketId);


    @Query(value = """
            SELECT record_code
            FROM medical_record
            WHERE record_code ~ ('^' || :prefix || '[0-9]+$')
            ORDER BY CAST(SUBSTRING(record_code FROM CHAR_LENGTH(:prefix) + 1) AS BIGINT) DESC
            LIMIT 1
            """, nativeQuery = true)
    String findTopByRecordCodeStartingWithOrderByRecordCodeDesc(@Param("prefix") String prefix);

    @Query("""
            SELECT m FROM MedicalRecord m
            WHERE (m.followUpDate IS NOT NULL OR (m.followUpNote IS NOT NULL AND TRIM(m.followUpNote) <> ''))
              AND m.followUpAppointment IS NULL
              AND (:search = ''
                   OR LOWER(m.visit.customer.fullName) LIKE CONCAT('%', :search, '%')
                   OR m.visit.customer.phone LIKE CONCAT('%', :search, '%')
                   OR LOWER(m.recordCode) LIKE CONCAT('%', :search, '%'))
            ORDER BY m.followUpDate ASC
            """)
    Page<MedicalRecord> findPendingFollowUps(@Param("search") String search, Pageable pageable);

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
