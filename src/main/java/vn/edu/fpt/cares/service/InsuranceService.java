package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.dto.insurance.InsuranceResponse;
import vn.edu.fpt.cares.dto.insurance.InsuranceRuleResponse;
import vn.edu.fpt.cares.repository.InsuranceRepository;
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
