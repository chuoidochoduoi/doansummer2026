package org.example.doansummer2026.dto.medicalservice;

import java.util.List;
import java.util.UUID;

public record ServiceSelectionResolutionResponse(
        List<UUID> selectedServiceIds,
        List<MedicalServiceResponse> selectedServices,
        List<Adjustment> removedServices,
        List<String> warnings,
        List<String> conflicts
) {
    public record Adjustment(
            UUID serviceId,
            String serviceCode,
            String serviceName,
            UUID coveredByServiceId,
            String coveredByServiceCode,
            String coveredByServiceName,
            String reason
    ) {}
}
