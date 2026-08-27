package org.example.doansummer2026.dto.serviceCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ServiceCategoryCreateRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        UUID parentId
) {}




