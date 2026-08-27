package org.example.doansummer2026.dto.medicalHistory;

import org.example.doansummer2026.model.Icd10Selection;

/**
 * DTO cho chuan doan (ICD-10).
 */
public record DiagnosisResponse(
        String code,
        String label
) {
    public static DiagnosisResponse from(Icd10Selection selection) {
        return new DiagnosisResponse(
                selection.getCode(),
                selection.getCodeName()
        );
    }
}