package vn.edu.fpt.cares.dto.specialization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecializationCreateRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        Boolean active
) {}



