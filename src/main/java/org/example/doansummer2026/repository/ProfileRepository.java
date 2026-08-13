package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID>, JpaSpecificationExecutor<Profile> {

    Optional<Profile> findFirstByAccount_AccountId(UUID accountId);

    Optional<Profile> findFirstByPhone(String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profile p where p.profileId = :id")
    Optional<Profile> findByIdForUpdate(UUID id);
    Optional<Profile> findFirstByPhoneIn(Collection<String> phones);

    Optional<Profile> findFirstByEmail(String email);

    Optional<Profile> findFirstByEmailIgnoreCase(String email);

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



