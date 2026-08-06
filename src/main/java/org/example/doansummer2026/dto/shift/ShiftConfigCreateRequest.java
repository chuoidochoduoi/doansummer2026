package org.example.doansummer2026.dto.shift;

import jakarta.validation.constraints.NotBlank;

public record ShiftConfigCreateRequest(
        @NotBlank(message = "Tên ca không được để trống")
        String name,
        
        @NotBlank(message = "Thời gian bắt đầu không được để trống")
        String startTime,
        
        @NotBlank(message = "Thời gian kết thúc không được để trống")
        String endTime,
        
        Boolean isActive
) {
}
