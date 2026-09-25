package vn.edu.fpt.cares.repository;

import vn.edu.fpt.cares.model.MembershipPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MembershipPolicyRepository extends JpaRepository<MembershipPolicy, UUID> {
    Optional<MembershipPolicy> findFirstByActiveTrueOrderByCreatedAtDesc();
}
