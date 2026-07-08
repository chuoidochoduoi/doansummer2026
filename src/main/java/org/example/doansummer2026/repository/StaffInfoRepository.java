package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.enums.SystemRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffInfoRepository extends JpaRepository<StaffInfo, UUID>, JpaSpecificationExecutor<StaffInfo> {

    Optional<StaffInfo> findByProfile_ProfileId(UUID profileId);

    Optional<StaffInfo> findByStaffCode(String staffCode);

    Optional<StaffInfo> findByProfile_Account_Username(String username);

    boolean existsByNationalId(String nationalId);

    boolean existsByLicenseNumber(String licenseNumber);

    default Page<StaffInfo> search(UUID departmentId, UUID specializationId,
                                    SystemRole systemRole, Pageable pageable) {
        Specification<StaffInfo> spec = (root, query, cb) -> cb.conjunction();

        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("departmentId"), departmentId));
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