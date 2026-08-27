package org.example.doansummer2026.dto.icd;

import jakarta.validation.constraints.Size;

/**
 * Request cap nhat ICD-10 code.
 */
public record ICD10UpdateRequest(
        @Size(max = 255)
        String name,

        String description,

        @Size(max = 100)
        String category
) {}