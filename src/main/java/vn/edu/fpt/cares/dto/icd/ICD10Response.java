package vn.edu.fpt.cares.dto.icd;

import vn.edu.fpt.cares.model.Icd10Code;

/**
 * DTO tra ve thong tin ICD-10 code.
 */
public record ICD10Response(
        String code,
        String name,
        String description,
        String category
) {
    public static ICD10Response from(Icd10Code c) {
        return new ICD10Response(
                c.getCode(),
                c.getName(),
                c.getDescription(),
                c.getCategory()
        );
    }
}