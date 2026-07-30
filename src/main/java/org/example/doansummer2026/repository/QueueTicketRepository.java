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

    /**
     * Tim queue ticket theo visit + department (1 visit co the co nhieu ticket cho cac khoa khac nhau).
     * Dung khi biet ro department can cap nhat (vi du: performingDepartment cua TestRequest).
     */
    Optional<QueueTicket> findByVisit_VisitIdAndDepartment_DepartmentId(UUID visitId, UUID departmentId);

    /**
     * Lay tat ca queue ticket cua 1 visit (1 visit co the co nhieu ticket cho cac khoa khac nhau).
     * Dung khi can duyet de tim ticket dung (vi du: ticket dang IN_PROGRESS).
     */
    List<QueueTicket> findAllByVisit_VisitId(UUID visitId);

    @Query("SELECT MAX(q.queueNumber) FROM QueueTicket q WHERE q.department.departmentId = :departmentId AND q.workDate = :workDate")
    Optional<Integer> findMaxQueueNumberForDay(@Param("departmentId") UUID departmentId,
                                                @Param("workDate") LocalDate workDate);

    @Query("SELECT COUNT(q) FROM QueueTicket q WHERE q.department.departmentId = :departmentId AND q.status = org.example.doansummer2026.enums.QueueStatus.IN_PROGRESS")
    long countInprogressByDepartment(@Param("departmentId") UUID departmentId);

    @Query("SELECT COUNT(q) FROM QueueTicket q WHERE q.department.departmentId = :departmentId AND q.status IN (org.example.doansummer2026.enums.QueueStatus.WAITING, org.example.doansummer2026.enums.QueueStatus.CALLED)")
    long countWaitingByDepartment(@Param("departmentId") UUID departmentId);

    @Query("SELECT COUNT(q) FROM QueueTicket q WHERE q.department.departmentId = :departmentId AND q.status NOT IN (org.example.doansummer2026.enums.QueueStatus.DONE, org.example.doansummer2026.enums.QueueStatus.SKIPPED)")
    long countActiveTicketsByDepartment(@Param("departmentId") UUID departmentId);


    /**
     * Dem benh nhan cho xet nghiem (WAITING_FOR_TEST).
     * Bay loi dau han cho bac si xet nghiem.
     */
    @Query("SELECT COUNT(q) FROM QueueTicket q WHERE q.department.departmentId = :departmentId AND q.status = org.example.doansummer2026.enums.QueueStatus.WAITING_FOR_TEST")
    long countWaitingForTestByDepartment(@Param("departmentId") UUID departmentId);

    /**
     * Dem benh nhan da hoan thanh xet nghiem (TEST_DONE).
     */
    @Query("SELECT COUNT(q) FROM QueueTicket q WHERE q.department.departmentId = :departmentId AND q.status = org.example.doansummer2026.enums.QueueStatus.TEST_DONE")
    long countTestDoneByDepartment(@Param("departmentId") UUID departmentId);

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

    /**
     * Lay danh sach cho phong, uu tien TEST_DONE/WAITING_FOR_TEST truoc WAITING/CALLED.
     * Mac dinh lay ca 4 trang thai, sap xep TEST_DONE/WAITING_FOR_TEST len dau.
     */
    @Query("SELECT q FROM QueueTicket q WHERE q.department.departmentId = :departmentId " +
           "AND q.workDate = :workDate " +
           "AND q.status IN :statuses " +
           "ORDER BY CASE WHEN q.status = org.example.doansummer2026.enums.QueueStatus.TEST_DONE THEN 0 " +
                       "WHEN q.status = org.example.doansummer2026.enums.QueueStatus.WAITING_FOR_TEST THEN 1 " +
                       "WHEN q.status = org.example.doansummer2026.enums.QueueStatus.WAITING THEN 2 " +
                       "WHEN q.status = org.example.doansummer2026.enums.QueueStatus.CALLED THEN 3 " +
                       "ELSE 4 END, q.createdAt ASC")
    Page<QueueTicket> findWaitingPrioritized(@Param("departmentId") UUID departmentId,
                                            @Param("workDate") LocalDate workDate,
                                            @Param("statuses") List<QueueStatus> statuses,
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



