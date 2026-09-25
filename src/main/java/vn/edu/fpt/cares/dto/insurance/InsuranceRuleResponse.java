package vn.edu.fpt.cares.dto.insurance;

import vn.edu.fpt.cares.enums.DepartmentType;

import java.math.BigDecimal;
import java.util.UUID;

public record InsuranceRuleResponse(
        UUID ruleId,
        DepartmentType departmentType,
        BigDecimal discountPercent
) {}
