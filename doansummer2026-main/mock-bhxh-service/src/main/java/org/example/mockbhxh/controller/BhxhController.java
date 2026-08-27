package org.example.mockbhxh.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class BhxhController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "mock-bhxh-service");
    }

    @GetMapping("/api/verify-card")
    public Map<String, Object> verifyCard(@RequestParam String code) {
        Map<String, Object> response = new LinkedHashMap<>();
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();

        if (normalizedCode.length() < 10) {
            response.put("isValid", false);
            response.put("message", "Mã thẻ không hợp lệ (phải có ít nhất 10 ký tự)");
            return response;
        }

        int coveragePercentage = coveragePercentage(normalizedCode);
        response.put("isValid", true);
        response.put("message", "Thẻ BHYT hợp lệ");
        response.put("insuranceName", "Bảo hiểm Y tế Nhà nước");
        response.put("fullName", "Bệnh nhân BHYT mô phỏng");
        response.put("dateOfBirth", LocalDate.of(1990, 1, 1).toString());
        response.put("coveragePercentage", coveragePercentage);
        response.put("validFrom", LocalDate.now().minusYears(1).toString());
        response.put("validTo", LocalDate.now().plusYears(1).toString());
        return response;
    }

    private int coveragePercentage(String code) {
        char benefitLevel = code.length() >= 3 ? code.charAt(2) : '4';
        if (benefitLevel == '1' || benefitLevel == '2' || benefitLevel == '5') {
            return 100;
        }
        if (benefitLevel == '3') {
            return 95;
        }
        return 80;
    }
}
