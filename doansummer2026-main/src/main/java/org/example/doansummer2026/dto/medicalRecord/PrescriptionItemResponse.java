package org.example.doansummer2026.dto.medicalRecord;

import org.example.doansummer2026.model.PrescriptionItem;

import java.util.UUID;

public record PrescriptionItemResponse(
        UUID prescriptionItemId,
        String medicineName,
        Integer quantity,
        String unit,
        String note,
        Integer frequencyPerDay
) {
    public static PrescriptionItemResponse from(PrescriptionItem p) {
        return new PrescriptionItemResponse(
                p.getPrescriptionItemId(),
                p.getMedicineName(),
                p.getQuantity(),
                p.getUnit(),
                p.getNote(),
                p.getFrequencyPerDay()
        );
    }
}



