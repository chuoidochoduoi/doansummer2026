package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.enums.SystemRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffInfoRepository extends JpaRepository<StaffInfo, UUID>, JpaSpecificationExecutor<StaffInfo> {

    Optional<StaffInfo> findByProfile_ProfileId(UUID profileId);

    Optional<StaffInfo> findByStaffCode(String staffCode);

    Optional<StaffInfo> findByProfile_Account_Username(String username);

    Optional<StaffInfo> findByProfile_Account_AccountId(UUID accountId);

    Page<StaffInfo> findBySystemRole(SystemRole systemRole, Pageable pageable);

    boolean existsByNationalId(String nationalId);

    boolean existsByLicenseNumber(String licenseNumber);

    /** Tim tat ca staff theo danh sach systemRole (dung cho lay danh sach bac si). */
    List<StaffInfo> findAllBySystemRoleIn(List<SystemRole> systemRoles);

    /** Lay danh sach tat ca staff (cho Schedule). */
    List<StaffInfo> findAll();

    default Page<StaffInfo> search(UUID specializationId,
                                    SystemRole systemRole, Pageable pageable) {
        Specification<StaffInfo> spec = (root, query, cb) -> cb.conjunction();

        if (specializationId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("specialization").get("specializationId"), specializationId));
        }
        if (systemRole != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("systemRole"), systemRole));
        }

        return findAll(spec, pageable);
    }
}



