package org.example.doansummer2026.dto.medicalService;

import org.example.doansummer2026.enums.ServiceRelationType;

public record MedicalServiceRelationResponse(
        ServiceRelationType type,
        String targetServiceCode,
        String targetServiceName,
        String message
) {}
