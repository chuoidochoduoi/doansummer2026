package org.example.doansummer2026.dto.clinic;

import org.example.doansummer2026.model.ClinicInformation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ClinicInformationResponse(
        UUID clinicInformationId,
        String clinicName,
        String legalName,
        String taxCode,
        String operatingLicense,
        String shortDescription,
        String supportEmail,
        String phone,
        String address,
        String websiteUrl,
        String facebookUrl,
        String youtubeUrl,
        String zaloUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime updatedAt
) {
    public static ClinicInformationResponse from(ClinicInformation value) {
        return new ClinicInformationResponse(
                value.getClinicInformationId(), value.getClinicName(), value.getLegalName(), value.getTaxCode(),
                value.getOperatingLicense(), value.getShortDescription(), value.getSupportEmail(),
                value.getPhone(), value.getAddress(), value.getWebsiteUrl(), value.getFacebookUrl(),
                value.getYoutubeUrl(), value.getZaloUrl(), value.getLatitude(), value.getLongitude(),
                value.getUpdatedAt());
    }
}
