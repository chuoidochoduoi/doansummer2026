package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.enums.*;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShiftScheduleResolver {
    private final ShiftVersionRepository versionRepository;
    private final ClinicScheduleExceptionRepository exceptionRepository;

    public record ResolvedShift(ShiftConfig shift, ShiftVersion version, LocalTime startTime,
                                LocalTime endTime, ShiftTimeSource source,
                                ShiftUnavailableReason unavailableReason) {
        public boolean available() { return unavailableReason == null; }
    }

    public ResolvedShift resolve(ShiftConfig shift, LocalDate date) {
        if (shift == null || !Boolean.TRUE.equals(shift.getIsActive())) {
            return new ResolvedShift(shift, null, null, null, ShiftTimeSource.NORMAL,
                    ShiftUnavailableReason.SHIFT_OFF);
        }
        List<ClinicScheduleException> exceptions = exceptionRepository.findAllByWorkDate(date);
        if (exceptions.stream().anyMatch(e -> e.getType() == ClinicScheduleExceptionType.CLOSED_DAY)) {
            return new ResolvedShift(shift, null, null, null, ShiftTimeSource.NORMAL,
                    ShiftUnavailableReason.CLINIC_CLOSED);
        }
        Optional<ClinicScheduleException> shiftException = exceptions.stream()
                .filter(e -> e.getShift() != null && e.getShift().getShiftId().equals(shift.getShiftId()))
                .findFirst();
        if (shiftException.isPresent() && shiftException.get().getType() == ClinicScheduleExceptionType.SHIFT_OFF) {
            return new ResolvedShift(shift, null, null, null, ShiftTimeSource.NORMAL,
                    ShiftUnavailableReason.SHIFT_OFF);
        }

        ShiftVersion version = versionRepository.findEffective(shift.getShiftId(), date).stream().findFirst().orElse(null);
        LocalTime start = version == null ? parseLegacy(shift.getStartTime()) : version.getStartTime();
        LocalTime end = version == null ? parseLegacy(shift.getEndTime()) : version.getEndTime();
        if (start == null || end == null) {
            return new ResolvedShift(shift, null, null, null, ShiftTimeSource.NORMAL,
                    ShiftUnavailableReason.NO_ACTIVE_SHIFT_VERSION);
        }
        if (shiftException.isPresent()
                && shiftException.get().getType() == ClinicScheduleExceptionType.SPECIAL_HOURS) {
            ClinicScheduleException special = shiftException.get();
            return new ResolvedShift(shift, version, special.getSpecialStartTime(), special.getSpecialEndTime(),
                    ShiftTimeSource.SPECIAL, null);
        }
        return new ResolvedShift(shift, version, start, end, ShiftTimeSource.NORMAL, null);
    }

    private LocalTime parseLegacy(String value) {
        try { return value == null ? null : LocalTime.parse(value); }
        catch (RuntimeException ignored) { return null; }
    }
}
