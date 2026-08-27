package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.ClinicInformation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClinicInformationRepository extends JpaRepository<ClinicInformation, UUID> {
}
