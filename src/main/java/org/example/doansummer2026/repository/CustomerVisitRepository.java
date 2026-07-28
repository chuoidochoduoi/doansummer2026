package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.enums.VisitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerVisitRepository extends JpaRepository<CustomerVisit, UUID>, JpaSpecificationExecutor<CustomerVisit> {

    Optional<CustomerVisit> findByAppointment_AppointmentId(UUID appointmentId);

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



