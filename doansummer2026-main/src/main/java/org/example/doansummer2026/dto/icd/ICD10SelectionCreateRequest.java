package org.example.doansummer2026.dto.icd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request them ICD-10 selection vao ho so benh an.
 * Frontend co the gui codeName cho quick lookup, backend se fallback sang tim kiem DB.
 */
public record ICD10SelectionCreateRequest(
        @NotBlank
        @Size(max = 10)
        String code,

        @Size(max = 255)
        String codeName,

        @Size(max = 255)
        String note
) {}