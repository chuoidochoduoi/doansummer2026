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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueTicketRepository extends JpaRepository<QueueTicket, UUID>, JpaSpecificationExecutor<QueueTicket> {

    Optional<QueueTicket> findByVisit_VisitId(UUID visitId);

    @Query("SELECT MAX(q.queueNumber) FROM QueueTicket q WHERE q.department.departmentId = :departmentId AND q.workDate = :workDate")
    Optional<Integer> findMaxQueueNumberForDay(@Param("departmentId") UUID departmentId,
                                                @Param("workDate") LocalDate workDate);

    @Query("SELECT COUNT(q) FROM QueueTicket q WHERE q.department.departmentId = :departmentId AND q.status = org.example.doansummer2026.enums.QueueStatus.IN_PROGRESS")
    long countInprogressByDepartment(@Param("departmentId") UUID departmentId);

    @Query("SELECT COUNT(q) FROM QueueTicket q WHERE q.department.departmentId = :departmentId AND q.status IN (org.example.doansummer2026.enums.QueueStatus.WAITING, org.example.doansummer2026.enums.QueueStatus.CALLED)")
    long countWaitingByDepartment(@Param("departmentId") UUID departmentId);

    Optional<QueueTicket> findTopByDepartment_DepartmentIdAndStatusOrderByCreatedAtAsc(
            @Param("departmentId") UUID departmentId,
            @Param("status") QueueStatus status);

    Page<QueueTicket> findAllByStatus(@Param("status") QueueStatus status,
                                       Pageable pageable);

    Page<QueueTicket> findByDepartment_DepartmentIdAndStatusIn(
            @Param("departmentId") UUID departmentId,
            @Param("statuses") List<QueueStatus> statuses,
            Pageable pageable);

    Page<QueueTicket> findByDepartment_DepartmentIdAndWorkDateAndStatusIn(
            @Param("departmentId") UUID departmentId,
            @Param("workDate") LocalDate workDate,
            @Param("statuses") List<QueueStatus> statuses,
            Pageable pageable);

    Page<QueueTicket> findByDepartment_DepartmentIdAndStatus(
            @Param("departmentId") UUID departmentId,
            @Param("status") QueueStatus status,
            Pageable pageable);

    Page<QueueTicket> findByDepartment_DepartmentIdAndWorkDateAndStatus(
            @Param("departmentId") UUID departmentId,
            @Param("workDate") LocalDate workDate,
            @Param("status") QueueStatus status,
            Pageable pageable);

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