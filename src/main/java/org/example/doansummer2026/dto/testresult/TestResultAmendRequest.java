package org.example.doansummer2026.dto.testresult;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestResultAmendRequest(@NotBlank @Size(max = 1000) String reason) {}
