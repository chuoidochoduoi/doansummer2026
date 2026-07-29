package org.example.doansummer2026.dto.icd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request tao moi ICD-10 code.
 */
public record ICD10CreateRequest(
        @NotBlank
        @Size(max = 10)
        String code,

        @NotBlank
        @Size(max = 255)
        String name,

        String description,

        @Size(max = 100)
        String category
) {}