package org.example.doansummer2026.dto.insurance;

import org.example.doansummer2026.enums.DepartmentType;

import java.math.BigDecimal;
import java.util.UUID;

public record InsuranceRuleResponse(
        UUID ruleId,
        DepartmentType departmentType,
        BigDecimal discountPercent
) {}
