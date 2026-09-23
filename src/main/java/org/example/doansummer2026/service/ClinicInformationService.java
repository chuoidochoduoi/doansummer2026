package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.clinic.ClinicInformationRequest;
import org.example.doansummer2026.dto.clinic.ClinicInformationResponse;
import org.example.doansummer2026.model.ClinicInformation;
import org.example.doansummer2026.repository.ClinicInformationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClinicInformationService {
    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");

    private final ClinicInformationRepository repository;

    @Transactional(readOnly = true)
    public ClinicInformationResponse get() {
        return ClinicInformationResponse.from(repository.findById(SINGLETON_ID).orElseGet(this::defaultValue));
    }

    public ClinicInformationResponse update(ClinicInformationRequest request) {
        ClinicInformation value = repository.findById(SINGLETON_ID).orElseGet(this::defaultValue);
        value.setClinicName(normalizeRequired(request.clinicName()));
        value.setLegalName(normalizeRequired(request.legalName()));
        value.setTaxCode(request.taxCode().trim());
        value.setOperatingLicense(normalizeOptional(request.operatingLicense()));
        value.setShortDescription(normalizeOptional(request.shortDescription()));
        value.setSupportEmail(request.supportEmail().trim().toLowerCase());
        value.setPhone(request.phone().trim().replaceAll("\\s+", " "));
        value.setAddress(normalizeRequired(request.address()));
        value.setWebsiteUrl(normalizeOptional(request.websiteUrl()));
        value.setFacebookUrl(normalizeOptional(request.facebookUrl()));
        value.setYoutubeUrl(normalizeOptional(request.youtubeUrl()));
        value.setZaloUrl(normalizeOptional(request.zaloUrl()));
        value.setLatitude(request.latitude());
        value.setLongitude(request.longitude());
        return ClinicInformationResponse.from(repository.save(value));
    }

    private ClinicInformation defaultValue() {
        ClinicInformation value = ClinicInformation.builder()
                .clinicInformationId(SINGLETON_ID)
                .clinicName("Phòng khám CareS")
                .legalName("Công ty TNHH Phòng khám CareS")
                .taxCode("0101234567")
                .operatingLicense("000123/HNO-GPHD")
                .shortDescription("Phòng khám đa khoa cung cấp dịch vụ chăm sóc sức khỏe chất lượng và thuận tiện.")
                .supportEmail("phongkhamcares@gmail.com")
                .phone("0968161266")
                .address("Thôn 1, Canh Nậu, Thạch Thất, Hà Nội")
                .facebookUrl("https://www.facebook.com/profile.php?id=61593125259676")
                .latitude(new BigDecimal("21.0128000"))
                .longitude(new BigDecimal("105.5259000"))
                .build();
        value.setDeleted(false);
        return value;
    }

    private String normalizeRequired(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replaceAll("\\s+", " ");
    }
}
