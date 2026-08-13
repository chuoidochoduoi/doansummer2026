package org.example.doansummer2026.repository;

import org.example.doansummer2026.enums.StaffCapabilityStatus;
import org.example.doansummer2026.model.StaffCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface StaffCapabilityRepository extends JpaRepository<StaffCapability, UUID> {
    List<StaffCapability> findAllByStaff_StaffId(UUID staffId);
    boolean existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(UUID staffId, UUID capabilityId, StaffCapabilityStatus status);
    void deleteAllByStaff_StaffId(UUID staffId);

    @Query(value = "SELECT COUNT(*) FROM staff_capability " +
            "WHERE capability_id = :capabilityId AND deleted = false", nativeQuery = true)
    long countActiveReferencesToCapability(@Param("capabilityId") UUID capabilityId);
}
