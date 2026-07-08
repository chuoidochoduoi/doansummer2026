package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.VitalSigns;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VitalSignsRepository extends JpaRepository<VitalSigns, UUID> {

    Optional<VitalSigns> findByMedicalRecord_RecordId(UUID medicalRecordId);
}