package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.enums.*;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftScheduleResolverTest {
    @Mock ShiftVersionRepository versionRepository;
    @Mock ClinicScheduleExceptionRepository exceptionRepository;
    @InjectMocks ShiftScheduleResolver resolver;

    private final LocalDate date = LocalDate.of(2026, 9, 10);
    private ShiftConfig shift;
    private ShiftVersion version;

    @BeforeEach
    void setUp() {
        shift = ShiftConfig.builder().shiftId(UUID.randomUUID()).name("Morning")
                .startTime("07:30").endTime("11:30").isActive(true).build();
        version = ShiftVersion.builder().shiftVersionId(UUID.randomUUID()).shift(shift)
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.NOON)
                .effectiveFrom(date.minusDays(1)).build();
    }

    @Test
    void resolvesEffectiveVersion() {
        when(exceptionRepository.findAllByWorkDate(date)).thenReturn(List.of());
        when(versionRepository.findEffective(shift.getShiftId(), date)).thenReturn(List.of(version));
        var result = resolver.resolve(shift, date);
        assertTrue(result.available());
        assertEquals(LocalTime.of(8, 0), result.startTime());
        assertEquals(ShiftTimeSource.NORMAL, result.source());
    }

    @Test
    void closedDayHasHighestPriority() {
        var closed = ClinicScheduleException.builder().workDate(date)
                .type(ClinicScheduleExceptionType.CLOSED_DAY).reason("Holiday").build();
        when(exceptionRepository.findAllByWorkDate(date)).thenReturn(List.of(closed));
        var result = resolver.resolve(shift, date);
        assertFalse(result.available());
        assertEquals(ShiftUnavailableReason.CLINIC_CLOSED, result.unavailableReason());
        verifyNoInteractions(versionRepository);
    }

    @Test
    void shiftOffDisablesOnlySelectedShift() {
        var off = ClinicScheduleException.builder().workDate(date).shift(shift)
                .type(ClinicScheduleExceptionType.SHIFT_OFF).reason("Maintenance").build();
        when(exceptionRepository.findAllByWorkDate(date)).thenReturn(List.of(off));
        var result = resolver.resolve(shift, date);
        assertEquals(ShiftUnavailableReason.SHIFT_OFF, result.unavailableReason());
    }

    @Test
    void specialHoursOverrideVersion() {
        var special = ClinicScheduleException.builder().workDate(date).shift(shift)
                .type(ClinicScheduleExceptionType.SPECIAL_HOURS)
                .specialStartTime(LocalTime.of(9, 0)).specialEndTime(LocalTime.of(10, 30))
                .reason("Special day").build();
        when(exceptionRepository.findAllByWorkDate(date)).thenReturn(List.of(special));
        when(versionRepository.findEffective(shift.getShiftId(), date)).thenReturn(List.of(version));
        var result = resolver.resolve(shift, date);
        assertTrue(result.available());
        assertEquals(ShiftTimeSource.SPECIAL, result.source());
        assertEquals(LocalTime.of(9, 0), result.startTime());
        assertEquals(LocalTime.of(10, 30), result.endTime());
    }

    @Test
    void nullAndInactiveShiftsAreUnavailableWithoutRepositoryAccess() {
        assertEquals(ShiftUnavailableReason.SHIFT_OFF, resolver.resolve(null, date).unavailableReason());
        shift.setIsActive(false);
        assertEquals(ShiftUnavailableReason.SHIFT_OFF, resolver.resolve(shift, date).unavailableReason());
        verifyNoInteractions(exceptionRepository, versionRepository);
    }

    @Test
    void legacyTimesAreUsedWhenNoVersionExists() {
        when(exceptionRepository.findAllByWorkDate(date)).thenReturn(List.of());
        when(versionRepository.findEffective(shift.getShiftId(), date)).thenReturn(List.of());

        var result = resolver.resolve(shift, date);

        assertTrue(result.available());
        assertEquals(LocalTime.of(7, 30), result.startTime());
        assertEquals(LocalTime.of(11, 30), result.endTime());
        assertNull(result.version());
    }

    @Test
    void invalidOrMissingLegacyTimeMakesShiftUnavailable() {
        when(exceptionRepository.findAllByWorkDate(date)).thenReturn(List.of());
        when(versionRepository.findEffective(shift.getShiftId(), date)).thenReturn(List.of());
        shift.setStartTime("invalid");

        var invalid = resolver.resolve(shift, date);
        assertEquals(ShiftUnavailableReason.NO_ACTIVE_SHIFT_VERSION, invalid.unavailableReason());

        shift.setStartTime("07:30");
        shift.setEndTime(null);
        var missing = resolver.resolve(shift, date);
        assertEquals(ShiftUnavailableReason.NO_ACTIVE_SHIFT_VERSION, missing.unavailableReason());
    }

    @Test
    void exceptionForAnotherShiftDoesNotAffectResolution() {
        ShiftConfig another = ShiftConfig.builder().shiftId(UUID.randomUUID()).isActive(true).build();
        var unrelated = ClinicScheduleException.builder().workDate(date).shift(another)
                .type(ClinicScheduleExceptionType.SHIFT_OFF).build();
        when(exceptionRepository.findAllByWorkDate(date)).thenReturn(List.of(unrelated));
        when(versionRepository.findEffective(shift.getShiftId(), date)).thenReturn(List.of(version));

        assertTrue(resolver.resolve(shift, date).available());
    }

    @Test
    void nullShiftExceptionAndUnknownSameShiftExceptionDoNotOverrideNormalHours() {
        var withoutShift = ClinicScheduleException.builder().workDate(date).type(null).build();
        var sameShiftWithoutType = ClinicScheduleException.builder().workDate(date)
                .shift(shift).type(null).build();
        when(exceptionRepository.findAllByWorkDate(date))
                .thenReturn(List.of(withoutShift), List.of(sameShiftWithoutType));
        when(versionRepository.findEffective(shift.getShiftId(), date)).thenReturn(List.of(version));

        assertEquals(ShiftTimeSource.NORMAL, resolver.resolve(shift, date).source());
        assertEquals(ShiftTimeSource.NORMAL, resolver.resolve(shift, date).source());
    }
}
