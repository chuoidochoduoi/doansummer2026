package org.example.doansummer2026.repository;

import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.model.QueueTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueTicketRepository extends JpaRepository<QueueTicket, UUID>, JpaSpecificationExecutor<QueueTicket> {

    Optional<QueueTicket> findByVisit_VisitId(UUID visitId);

    @Query("SELECT MAX(q.queueNumber) FROM QueueTicket q WHERE q.department.departmentId = :departmentId AND q.workDate = :workDate")
    Optional<Integer> findMaxQueueNumberForDay(@Param("departmentId") UUID departmentId,
                                                @Param("workDate") LocalDate workDate);

    default Page<QueueTicket> search(UUID departmentId, LocalDate workDate,
                                      QueueStatus status, Pageable pageable) {
        Specification<QueueTicket> spec = (root, query, cb) -> cb.conjunction();

        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("departmentId"), departmentId));
        }
        if (workDate != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("workDate"), workDate));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return findAll(spec, pageable);
    }
}