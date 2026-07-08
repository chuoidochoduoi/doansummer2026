package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID>, JpaSpecificationExecutor<Profile> {

    Optional<Profile> findByAccount_AccountId(UUID accountId);

    Optional<Profile> findByPhone(String phone);

    Optional<Profile> findByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    default Page<Profile> search(String keyword, Pageable pageable) {
        Specification<Profile> spec = (root, query, cb) -> cb.conjunction();

        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("fullName")), "%" + keyword.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("phone")), "%" + keyword.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("email")), "%" + keyword.toLowerCase() + "%")
                ));
        }

        return findAll(spec, pageable);
    }
}