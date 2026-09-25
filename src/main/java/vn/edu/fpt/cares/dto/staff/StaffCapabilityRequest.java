package vn.edu.fpt.cares.dto.staff;

import vn.edu.fpt.cares.enums.StaffCapabilityStatus;
import java.time.LocalDate;
import java.util.UUID;

public record StaffCapabilityRequest(
        UUID capabilityId,
        String certificateNumber,
        LocalDate issuedDate,
        LocalDate expiryDate,
        String issuingOrganization,
        StaffCapabilityStatus status
) {}
