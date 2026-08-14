package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.enums.VisitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface CustomerVisitRepository extends JpaRepository<CustomerVisit, UUID>, JpaSpecificationExecutor<CustomerVisit> {

    Optional<CustomerVisit> findByAppointment_AppointmentId(UUID appointmentId);

    /** Khoa luot kham khi tao standalone record CLS, tranh hai thanh toan dong thoi tao trung record. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM CustomerVisit v WHERE v.visitId = :visitId")
    Optional<CustomerVisit> findByIdForUpdate(@Param("visitId") UUID visitId);

    List<CustomerVisit> findAllByCustomer_ProfileIdOrderByCheckInTimeDesc(UUID profileId);
    List<CustomerVisit> findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc(String phone);
    Optional<CustomerVisit> findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
            UUID profileId, List<VisitStatus> statuses);

    default Page<CustomerVisit> search(UUID customerId, VisitStatus status,
                                        LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<CustomerVisit> spec = (root, query, cb) -> cb.conjunction();

        if (customerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customer").get("profileId"), customerId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("checkInTime"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("checkInTime"), to));
        }

        return findAll(spec, pageable);
    }
}



