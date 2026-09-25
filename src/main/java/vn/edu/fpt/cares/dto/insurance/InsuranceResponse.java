package vn.edu.fpt.cares.dto.insurance;

import java.util.List;
import java.util.UUID;

public record InsuranceResponse(
        UUID insuranceId,
        String code,
        String name,
        String description,
        List<InsuranceRuleResponse> rules
) {}
