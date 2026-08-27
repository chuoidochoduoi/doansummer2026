package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Icd10Code;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public interface Icd10CodeRepository extends JpaRepository<Icd10Code, String>, JpaSpecificationExecutor<Icd10Code> {

    boolean existsByCode(String code);

    boolean existsByName(String name);

    default Page<Icd10Code> search(String keyword, String category, Pageable pageable) {
        Specification<Icd10Code> spec = (root, query, cb) -> cb.conjunction();

        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("code")), "%" + keyword.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")
                    ));
        }
        if (category != null && !category.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }

        return findAll(spec, pageable);
    }
}