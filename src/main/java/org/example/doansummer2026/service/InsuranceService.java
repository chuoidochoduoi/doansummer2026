package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.insurance.InsuranceResponse;
import org.example.doansummer2026.dto.insurance.InsuranceRuleResponse;
import org.example.doansummer2026.repository.InsuranceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InsuranceService {

    private final InsuranceRepository insuranceRepository;

    public List<InsuranceResponse> getAllInsurances() {
        return insuranceRepository.findAll().stream()
                .map(insurance -> new InsuranceResponse(
                        insurance.getInsuranceId(),
                        insurance.getCode(),
                        insurance.getName(),
                        insurance.getDescription(),
                        insurance.getRules().stream()
                                .map(rule -> new InsuranceRuleResponse(
                                        rule.getRuleId(),
                                        rule.getDepartmentType(),
                                        rule.getDiscountPercent()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}
