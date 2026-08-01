package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

    Optional<Account> findFirstByUsername(String username);

    boolean existsByUsername(String username);

    Page<Account> findByRole(Role role, Pageable pageable);

    default Page<Account> searchCustomers(String keyword, Boolean isActive, Pageable pageable) {
        Specification<Account> spec = (root, query, cb) -> cb.equal(root.get("role"), Role.CUSTOMER);

        if (keyword != null && !keyword.trim().isEmpty()) {
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((r, q, cb) -> {
                var profileJoin = r.join("profile", jakarta.persistence.criteria.JoinType.LEFT);
                return cb.or(
                        cb.like(cb.lower(profileJoin.get("fullName")), likeKeyword),
                        cb.like(cb.lower(r.get("username")), likeKeyword)
                );
            });
        }

        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }

        return findAll(spec, pageable);
    }
}



