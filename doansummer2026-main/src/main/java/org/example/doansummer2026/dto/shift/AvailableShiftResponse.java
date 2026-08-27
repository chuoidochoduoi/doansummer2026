package org.example.doansummer2026.dto.shift;

import org.example.doansummer2026.enums.ShiftTimeSource;
import org.example.doansummer2026.enums.ShiftUnavailableReason;
import java.time.LocalTime;
import java.util.*;

public record AvailableShiftResponse(
        UUID shiftId,
        UUID shiftVersionId,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        boolean available,
        ShiftTimeSource timeSource,
        ShiftUnavailableReason unavailableReasonCode,
        Map<UUID, ShiftUnavailableReason> serviceUnavailableReasons
) {}
