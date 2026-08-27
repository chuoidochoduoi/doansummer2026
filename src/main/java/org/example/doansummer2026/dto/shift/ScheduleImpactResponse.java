package org.example.doansummer2026.dto.shift;

import java.time.LocalDate;
import java.util.*;

public record ScheduleImpactResponse(
        boolean blocked,
        long appointmentCount,
        long staffScheduleCount,
        Set<UUID> affectedServiceIds,
        List<LocalDate> affectedDates,
        List<UUID> appointmentIds,
        List<UUID> staffScheduleIds
) {}
