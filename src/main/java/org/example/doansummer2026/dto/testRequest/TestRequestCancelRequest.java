package org.example.doansummer2026.dto.testRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request huy yeu cau xet nghiem.
 */
public record TestRequestCancelRequest(
        @NotBlank @Size(max = 500) String reason
) {}