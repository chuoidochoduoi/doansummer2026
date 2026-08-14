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
                // Profile la phia so huu quan he Account -> Profile; Account khong
                // co thuoc tinh nguoc "profile", vi vay khong the join truc tiep.
                var profileExists = q.subquery(Integer.class);
                var profile = profileExists.from(org.example.doansummer2026.model.Profile.class);
                profileExists.select(cb.literal(1)).where(
                        cb.equal(profile.get("account"), r),
                        cb.or(
                                cb.like(cb.lower(profile.get("fullName")), likeKeyword),
                                cb.like(cb.lower(profile.get("phone")), likeKeyword),
                                cb.like(cb.lower(profile.get("email")), likeKeyword)
                        )
                );
                return cb.or(
                        cb.like(cb.lower(r.get("username")), likeKeyword),
                        cb.exists(profileExists)
                );
            });
        }

        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }

        return findAll(spec, pageable);
    }
}



