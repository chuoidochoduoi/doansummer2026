package vn.edu.fpt.cares.dto.testrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request huy yeu cau xet nghiem.
 */
public record TestRequestCancelRequest(
        @NotBlank @Size(max = 500) String reason
) {}