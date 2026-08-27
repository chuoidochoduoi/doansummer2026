package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.ClinicalFormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClinicalFormTemplateRepository extends JpaRepository<ClinicalFormTemplate, UUID> {
    Optional<ClinicalFormTemplate> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
