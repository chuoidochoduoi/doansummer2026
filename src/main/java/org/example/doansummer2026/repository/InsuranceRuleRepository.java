package org.example.doansummer2026.repository;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.model.InsuranceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InsuranceRuleRepository extends JpaRepository<InsuranceRule, UUID> {
    Optional<InsuranceRule> findByInsurance_InsuranceIdAndDepartmentType(UUID insuranceId, DepartmentType departmentType);
    java.util.List<InsuranceRule> findByInsurance_InsuranceId(UUID insuranceId);
}
