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

    @Query(value = """
            SELECT v FROM CustomerVisit v
            WHERE v.customer.profileId = :profileId
              AND EXISTS (
                  SELECT m.recordId FROM MedicalRecord m
                  WHERE m.visit = v
                    AND m.status = org.example.doansummer2026.enums.MedicalRecordStatus.COMPLETED
                    AND m.queueTicket IS NOT NULL
                    AND m.queueTicket.department.departmentType = org.example.doansummer2026.enums.DepartmentType.EXAMINATION
              )
              AND (:search = '' OR EXISTS (
                  SELECT sm.recordId FROM MedicalRecord sm
                  WHERE sm.visit = v
                    AND sm.status = org.example.doansummer2026.enums.MedicalRecordStatus.COMPLETED
                    AND (LOWER(sm.recordCode) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(COALESCE(sm.diagnosis, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(COALESCE(sm.conclusion, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(COALESCE(sm.queueTicket.service.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(COALESCE(sm.doctor.profile.fullName, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              ))
            """,
            countQuery = """
            SELECT COUNT(v) FROM CustomerVisit v
            WHERE v.customer.profileId = :profileId
              AND EXISTS (
                  SELECT m.recordId FROM MedicalRecord m
                  WHERE m.visit = v
                    AND m.status = org.example.doansummer2026.enums.MedicalRecordStatus.COMPLETED
                    AND m.queueTicket IS NOT NULL
                    AND m.queueTicket.department.departmentType = org.example.doansummer2026.enums.DepartmentType.EXAMINATION
              )
              AND (:search = '' OR EXISTS (
                  SELECT sm.recordId FROM MedicalRecord sm
                  WHERE sm.visit = v
                    AND sm.status = org.example.doansummer2026.enums.MedicalRecordStatus.COMPLETED
                    AND (LOWER(sm.recordCode) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(COALESCE(sm.diagnosis, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(COALESCE(sm.conclusion, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(COALESCE(sm.queueTicket.service.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(COALESCE(sm.doctor.profile.fullName, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              ))
            """)
    Page<CustomerVisit> findMedicalHistoryVisits(@Param("profileId") UUID profileId,
                                                  @Param("search") String search,
                                                  Pageable pageable);

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



