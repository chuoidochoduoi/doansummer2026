package org.example.doansummer2026.dto.schedule;

import org.example.doansummer2026.enums.Shift;

import java.util.UUID;

/**
 * Response cho ca truc (shift).
 */
public record ShiftResponse(
        String id,
        String name,
        String startTime,
        String endTime
) {
    public static ShiftResponse from(Shift shift, int index) {
        String name = switch (shift) {
            case MORNING -> "Ca sáng";
            case AFTERNOON -> "Ca chiều";
            case EVENING -> "Ca tối";
        };
        String startTime = switch (shift) {
            case MORNING -> "08:00";
            case AFTERNOON -> "13:00";
            case EVENING -> "17:00";
        };
        String endTime = switch (shift) {
            case MORNING -> "12:00";
            case AFTERNOON -> "17:00";
            case EVENING -> "21:00";
        };
        return new ShiftResponse(shift.name().toLowerCase(), name, startTime, endTime);
    }
}