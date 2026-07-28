package org.example.doansummer2026.repository;

import org.example.doansummer2026.enums.Shift;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.StaffSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffScheduleRepository extends JpaRepository<StaffSchedule, UUID>, JpaSpecificationExecutor<StaffSchedule> {

    Page<StaffSchedule> findByStaff_StaffId(UUID staffId, Pageable pageable);

    default Page<StaffSchedule> search(UUID staffId, LocalDate from,
                                        LocalDate to, Shift shift, Pageable pageable) {
        Specification<StaffSchedule> spec = (root, query, cb) -> cb.conjunction();

        if (staffId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("staff").get("staffId"), staffId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("workDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("workDate"), to));
        }
        if (shift != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("shift"), shift));
        }

        return findAll(spec, pageable);
    }

    List<StaffSchedule> findByStaffAndWorkDateBetween(StaffInfo staff, LocalDate from, LocalDate to);

    List<StaffSchedule> findAllByWorkDateBetween(LocalDate from, LocalDate to);

    Optional<StaffSchedule> findByStaffAndWorkDateAndShift(StaffInfo staff, LocalDate workDate, Shift shift);

    void deleteByStaffAndWorkDateAndShift(StaffInfo staff, LocalDate workDate, Shift shift);
}



