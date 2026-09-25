package vn.edu.fpt.cares.repository;

import vn.edu.fpt.cares.model.ClinicInformation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClinicInformationRepository extends JpaRepository<ClinicInformation, UUID> {
}
