package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.ServiceCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ServiceCapabilityRepository extends JpaRepository<ServiceCapability, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndCapabilityIdNot(String code, UUID capabilityId);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndCapabilityIdNot(String name, UUID capabilityId);
    List<ServiceCapability> findAllByOrderByNameAsc();
}
