package org.example.doansummer2026.dto.staff;

import org.example.doansummer2026.enums.StaffCapabilityStatus;
import org.example.doansummer2026.model.StaffCapability;
import java.time.LocalDate;
import java.util.UUID;

public record StaffCapabilityResponse(UUID staffCapabilityId, UUID capabilityId, String capabilityCode,
        String capabilityName, String certificateNumber, LocalDate issuedDate, LocalDate expiryDate,
        String issuingOrganization, StaffCapabilityStatus status) {
    public static StaffCapabilityResponse from(StaffCapability value) {
        return new StaffCapabilityResponse(value.getStaffCapabilityId(), value.getCapability().getCapabilityId(),
                value.getCapability().getCode(), value.getCapability().getName(), value.getCertificateNumber(),
                value.getIssuedDate(), value.getExpiryDate(), value.getIssuingOrganization(), value.getStatus());
    }
}
