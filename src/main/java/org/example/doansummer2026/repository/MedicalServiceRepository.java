package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.enums.ServiceType;
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

    @EntityGraph("MedicalService.withDepartment")
    Optional<MedicalService> findById(UUID id);

    default Page<MedicalService> search(String keyword, UUID categoryId,
                                         ServiceType serviceType, Boolean isActive,
                                         Pageable pageable) {
        Specification<MedicalService> spec = (root, query, cb) -> cb.conjunction();

        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")
                ));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("categoryId"), categoryId));
        }
        if (serviceType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("serviceType"), serviceType));
        }
        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }

        return findAll(spec, pageable);
    }
}