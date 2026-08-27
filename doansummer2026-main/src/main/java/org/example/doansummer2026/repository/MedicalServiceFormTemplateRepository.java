package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.MedicalServiceFormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalServiceFormTemplateRepository extends JpaRepository<MedicalServiceFormTemplate, UUID> {
    Optional<MedicalServiceFormTemplate> findByService_ServiceId(UUID serviceId);
    List<MedicalServiceFormTemplate> findByTemplate_TemplateId(UUID templateId);
    void deleteByTemplate_TemplateId(UUID templateId);
}
