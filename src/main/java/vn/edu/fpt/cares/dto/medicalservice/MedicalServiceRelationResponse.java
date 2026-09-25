package vn.edu.fpt.cares.dto.medicalservice;

import vn.edu.fpt.cares.enums.ServiceRelationType;

public record MedicalServiceRelationResponse(
        ServiceRelationType type,
        String targetServiceCode,
        String targetServiceName,
        String message
) {}
