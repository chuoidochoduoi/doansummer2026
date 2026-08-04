package org.example.doansummer2026.dto.capability;

import org.example.doansummer2026.model.ServiceCapability;
import java.util.UUID;

public record ServiceCapabilityResponse(UUID capabilityId, String code, String name,
                                        String description, Boolean active) {
    public static ServiceCapabilityResponse from(ServiceCapability value) {
        return new ServiceCapabilityResponse(value.getCapabilityId(), value.getCode(), value.getName(),
                value.getDescription(), value.getActive());
    }
}
