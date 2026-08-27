package org.example.doansummer2026.dto.contact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequestResolveRequest(
        @NotBlank(message = "Vui lòng nhập ghi chú nội bộ")
        @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
        String internalNote
) {}
