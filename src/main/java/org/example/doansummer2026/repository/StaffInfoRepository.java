package org.example.doansummer2026.repository;

import jakarta.persistence.LockModeType;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.enums.SystemRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffInfoRepository extends JpaRepository<StaffInfo, UUID>, JpaSpecificationExecutor<StaffInfo> {

    Optional<StaffInfo> findFirstByProfile_ProfileId(UUID profileId);

    Optional<StaffInfo> findFirstByStaffCode(String staffCode);

    Optional<StaffInfo> findFirstByProfile_Account_Username(String username);

    Optional<StaffInfo> findFirstByProfile_Account_AccountId(UUID accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT staff FROM StaffInfo staff "
            + "JOIN FETCH staff.profile profile "
            + "LEFT JOIN FETCH profile.account "
            + "WHERE staff.staffId = :staffId")
    Optional<StaffInfo> findByIdForScheduleUpdate(@Param("staffId") UUID staffId);

    Page<StaffInfo> findBySystemRole(SystemRole systemRole, Pageable pageable);

    List<StaffInfo> findByDepartment_DepartmentId(UUID departmentId);

    boolean existsByNationalId(String nationalId);

    boolean existsByLicenseNumber(String licenseNumber);

    /** Tim tat ca staff theo danh sach systemRole (dung cho lay danh sach bac si). */
    List<StaffInfo> findAllBySystemRoleIn(List<SystemRole> systemRoles);

    /** Lay danh sach tat ca staff (cho Schedule). */
    List<StaffInfo> findAll();

    @Query(value = """
            SELECT (SELECT COUNT(*) FROM department d
                    WHERE d.deleted = false AND d.head_doctor_id = :staffId)
                 + (SELECT COUNT(*) FROM staff_info si
                    WHERE si.deleted = false AND si.staff_id = :staffId AND si.department_id IS NOT NULL)
                 + (SELECT COUNT(*) FROM staff_schedule ss
                    WHERE ss.staff_id = :staffId AND ss.work_date >= CURRENT_DATE AND ss.status = 'SCHEDULED')
                 + (SELECT COUNT(*) FROM medical_record mr
                    WHERE mr.deleted = false AND mr.doctor_id = :staffId AND mr.status IN ('IN_PROGRESS', 'DRAFT'))
            """, nativeQuery = true)
    long countBlockingLockReferences(@Param("staffId") UUID staffId);

    default Page<StaffInfo> search(String keyword, UUID specializationId,
                                    SystemRole systemRole, Pageable pageable) {
        Specification<StaffInfo> spec = (root, query, cb) -> cb.conjunction();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((r, q, cb) -> {
                var profileJoin = r.join("profile", jakarta.persistence.criteria.JoinType.LEFT);
                var accountJoin = profileJoin.join("account", jakarta.persistence.criteria.JoinType.LEFT);
                return cb.or(
                        cb.like(cb.lower(profileJoin.get("fullName")), likeKeyword),
                        cb.like(cb.lower(accountJoin.get("username")), likeKeyword),
                        cb.like(cb.lower(r.get("staffCode")), likeKeyword)
                );
            });
        }

        if (specializationId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("specialization").get("specializationId"), specializationId));
        }
        if (systemRole != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("systemRole"), systemRole));
        }

        return findAll(spec, pageable);
    }
}

