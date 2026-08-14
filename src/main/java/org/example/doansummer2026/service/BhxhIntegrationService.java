package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.insurance.BhxhCheckResponse;
import org.example.doansummer2026.model.Insurance;
import org.example.doansummer2026.repository.InsuranceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class BhxhIntegrationService {

    private final InsuranceRepository insuranceRepository;
    private final RestTemplate restTemplate;
    private final String verifyCardUrl;

    public BhxhIntegrationService(
            InsuranceRepository insuranceRepository,
            RestTemplate restTemplate,
            @Value("${integration.bhxh.verify-card-url:http://localhost:8081/api/verify-card}") String verifyCardUrl
    ) {
        this.insuranceRepository = insuranceRepository;
        this.restTemplate = restTemplate;
        this.verifyCardUrl = verifyCardUrl;
    }

    public BhxhCheckResponse checkBhytCard(String cardNumber) {
        try {
            String url = UriComponentsBuilder.fromUriString(verifyCardUrl)
                    .queryParam("code", cardNumber)
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("isValid"))) {
                return new BhxhCheckResponse(false,
                        response != null && response.get("message") instanceof String message
                                ? message : "Không nhận được kết quả xác minh BHYT",
                        null, null, null, null);
            }

            Insurance insurance = insuranceRepository.findByCode("BHYT").orElse(null);
            if (insurance == null) {
                return new BhxhCheckResponse(false,
                        "Hệ thống chưa cấu hình loại bảo hiểm BHYT",
                        null, null, null, null);
            }

            return new BhxhCheckResponse(
                    true,
                    "Hợp lệ",
                    insurance.getInsuranceId(),
                    insurance.getName(),
                    response.get("fullName") instanceof String name ? name : null,
                    response.get("dateOfBirth") instanceof String dob ? dob : null
            );
        } catch (Exception exception) {
            return new BhxhCheckResponse(false,
                    "Không thể kết nối hệ thống xác minh BHYT. Vui lòng thử lại sau.",
                    null, null, null, null);
        }
    }
}
