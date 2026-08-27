package org.example.doansummer2026.dto.staff;

import org.example.doansummer2026.enums.StaffCapabilityStatus;
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
