package org.example.doansummer2026.dto.specialization;

import jakarta.validation.constraints.Size;

public record SpecializationUpdateRequest(
        @Size(max = 150) String name,
        @Size(max = 500) String description,
        Boolean active
) {}



