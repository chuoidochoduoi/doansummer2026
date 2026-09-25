package vn.edu.fpt.cares.dto.shift;

import vn.edu.fpt.cares.enums.ShiftTimeSource;
import vn.edu.fpt.cares.enums.ShiftUnavailableReason;
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
