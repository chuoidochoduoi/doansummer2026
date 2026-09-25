package vn.edu.fpt.cares.repository;

import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.model.InsuranceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InsuranceRuleRepository extends JpaRepository<InsuranceRule, UUID> {
    Optional<InsuranceRule> findByInsurance_InsuranceIdAndDepartmentType(UUID insuranceId, DepartmentType departmentType);
    java.util.List<InsuranceRule> findByInsurance_InsuranceId(UUID insuranceId);
}
