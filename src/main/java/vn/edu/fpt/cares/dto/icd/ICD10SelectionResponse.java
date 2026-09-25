package vn.edu.fpt.cares.dto.icd;

import vn.edu.fpt.cares.model.Icd10Selection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO tra ve thong tin lua chon ICD-10 trong ho so benh an.
 */
public record ICD10SelectionResponse(
        UUID selectionId,
        String code,
        String codeName,
        String note,
        LocalDateTime createdAt
) {
    public static ICD10SelectionResponse from(Icd10Selection s) {
        return new ICD10SelectionResponse(
                s.getSelectionId(),
                s.getCode(),
                s.getCodeName(),
                s.getNote(),
                s.getCreatedAt()
        );
    }
}