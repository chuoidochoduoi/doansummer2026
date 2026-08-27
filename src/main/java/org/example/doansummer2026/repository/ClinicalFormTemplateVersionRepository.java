package org.example.doansummer2026.repository;

import org.example.doansummer2026.enums.ClinicalTemplateStatus;
import org.example.doansummer2026.model.ClinicalFormTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicalFormTemplateVersionRepository extends JpaRepository<ClinicalFormTemplateVersion, UUID> {
    List<ClinicalFormTemplateVersion> findByTemplate_TemplateIdOrderByVersionNoDesc(UUID templateId);
    Optional<ClinicalFormTemplateVersion> findFirstByTemplate_TemplateIdOrderByVersionNoDesc(UUID templateId);
    Optional<ClinicalFormTemplateVersion> findFirstByTemplate_TemplateIdAndStatusAndEffectiveFromLessThanEqualOrderByVersionNoDesc(
            UUID templateId, ClinicalTemplateStatus status, LocalDate date);
    Optional<ClinicalFormTemplateVersion> findFirstByTemplate_TemplateIdAndStatusOrderByVersionNoDesc(
            UUID templateId, ClinicalTemplateStatus status);
    List<ClinicalFormTemplateVersion> findByStatusAndEffectiveFromLessThanEqual(
            ClinicalTemplateStatus status, LocalDate date);
}
