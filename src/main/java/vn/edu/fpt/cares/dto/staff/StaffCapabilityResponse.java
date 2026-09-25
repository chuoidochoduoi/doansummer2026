package vn.edu.fpt.cares.dto.staff;

import vn.edu.fpt.cares.enums.StaffCapabilityStatus;
import vn.edu.fpt.cares.model.StaffCapability;
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
