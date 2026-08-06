package org.example.doansummer2026.dto.shift;

public record ShiftConfigUpdateRequest(
        String name,
        String startTime,
        String endTime,
        Boolean isActive
) {
}
