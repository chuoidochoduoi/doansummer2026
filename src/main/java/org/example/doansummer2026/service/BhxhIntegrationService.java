package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.insurance.BhxhCheckResponse;
import org.example.doansummer2026.model.Insurance;
import org.example.doansummer2026.repository.InsuranceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Service
public class BhxhIntegrationService {

    private final InsuranceRepository insuranceRepository;
    private final RestTemplate restTemplate;
    
    // Đọc từ properties hoặc gán cứng cho demo
    private final String mockBhxhUrl;

    public BhxhIntegrationService(
            InsuranceRepository insuranceRepository,
            RestTemplate restTemplate,
            @Value("${integration.bhxh.verify-card-url:http://localhost:8081/api/verify-card}") String mockBhxhUrl
    ) {
        this.insuranceRepository = insuranceRepository;
        this.restTemplate = restTemplate;
        this.mockBhxhUrl = mockBhxhUrl;
    }

    public BhxhCheckResponse checkBhytCard(String cardNumber) {
        try {
            // 1. Gọi sang Mock BHXH Service (Microservice)
            String url = UriComponentsBuilder.fromUriString(mockBhxhUrl)
                    .queryParam("code", cardNumber)
                    .toUriString();
                    
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null || !Boolean.TRUE.equals(response.get("isValid"))) {
                return new BhxhCheckResponse(false, 
                        response != null ? (String) response.get("message") : "Không thể kết nối Cổng GĐBHYT", 
                        null, null, null, null);
            }
            
            // 2. Lấy kết quả hợp lệ, mapping với DB của phòng khám
            String insuranceName = (String) response.get("insuranceName");
            String fullName = (String) response.get("fullName");
            String dob = (String) response.get("dateOfBirth");
            
            // Tìm cấu hình BHYT trong CSDL phòng khám (mã mặc định là BHYT)
            Optional<Insurance> optIns = insuranceRepository.findByCode("BHYT");
            Insurance insurance;
            if (optIns.isEmpty()) {
                // Tự động tạo nếu chưa có để demo trơn tru
                insurance = Insurance.builder()
                        .code("BHYT")
                        .name("Bảo hiểm Y tế Nhà nước")
                        .description("Tự động tạo từ quá trình kiểm tra BHYT")
                        .build();
                insurance = insuranceRepository.save(insurance);
            } else {
                insurance = optIns.get();
            }
            
            return new BhxhCheckResponse(true, "Hợp lệ", insurance.getInsuranceId(), insurance.getName(), fullName, dob);

        } catch (Exception e) {
            return new BhxhCheckResponse(false, "Lỗi kết nối tới Hệ thống BHXH: " + e.getMessage(), null, null, null, null);
        }
    }
}
