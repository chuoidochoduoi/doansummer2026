package org.example.doansummer2026.dto.vitalSigns;

import org.example.doansummer2026.model.VitalSigns;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VitalSignsResponse(
        UUID vitalId,
        UUID medicalRecordId,
        String bloodPressure,
        Integer heartRate,
        BigDecimal temperature,
        BigDecimal weight,
        BigDecimal height,
        LocalDateTime recordedAt,
        UUID recordedById
) {
    public static VitalSignsResponse from(VitalSigns v) {
        UUID recordId = v.getMedicalRecord() != null ? v.getMedicalRecord().getRecordId() : null;
        UUID recBy = v.getRecordedBy() != null ? v.getRecordedBy().getStaffId() : null;
        return new VitalSignsResponse(v.getVitalId(), recordId, v.getBloodPressure(),
                v.getHeartRate(), v.getTemperature(), v.getWeight(), v.getHeight(),
                v.getRecordedAt(), recBy);
    }
}




