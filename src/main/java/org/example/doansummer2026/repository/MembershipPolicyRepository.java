package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.MembershipPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MembershipPolicyRepository extends JpaRepository<MembershipPolicy, UUID> {
    Optional<MembershipPolicy> findFirstByActiveTrueOrderByCreatedAtDesc();
}
