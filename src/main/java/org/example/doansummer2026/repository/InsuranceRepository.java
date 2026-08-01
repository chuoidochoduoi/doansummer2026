package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Insurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InsuranceRepository extends JpaRepository<Insurance, UUID> {
    Optional<Insurance> findByCode(String code);
}
