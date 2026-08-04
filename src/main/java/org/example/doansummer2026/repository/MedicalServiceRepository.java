package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.enums.ServiceStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalServiceRepository extends JpaRepository<MedicalService, UUID>, JpaSpecificationExecutor<MedicalService> {

    boolean existsByName(String name);

    boolean existsByServiceCode(String serviceCode);

    @EntityGraph("MedicalService.withDepartmentAndSpecialization")
    Optional<MedicalService> findById(UUID id);

    default Page<MedicalService> search(String keyword, DepartmentType departmentType,
                                         ServiceStatus status, UUID specializationId,
                                         Pageable pageable) {
        Specification<MedicalService> spec = (root, query, cb) -> cb.conjunction();

        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")
                ));
        }
        if (departmentType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("departmentType"), departmentType));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (specializationId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("requiredSpecialization").get("specializationId"), specializationId));
        }

        return findAll(spec, pageable);
    }

    default Page<MedicalService> searchCustomerBookable(String keyword, DepartmentType departmentType,
                                                         Pageable pageable) {
        Specification<MedicalService> spec = (root, query, cb) -> cb.equal(root.get("status"), ServiceStatus.ACTIVE);
        spec = spec.and((root, query, cb) -> cb.or(
                cb.isNull(root.get("allowCustomerBooking")),
                cb.isTrue(root.get("allowCustomerBooking"))));
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")));
        }
        if (departmentType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("departmentType"), departmentType));
        }
        return findAll(spec, pageable);
    }
}
