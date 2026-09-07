package org.example.doansummer2026.service;

import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftConfigServiceTest {
    @Mock ShiftConfigRepository shiftConfigRepository;
    @Mock ShiftVersionRepository shiftVersionRepository;
    @InjectMocks ShiftConfigService service;
    final LocalDate today = LocalDate.of(2026,9,4);
    final ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
    ShiftConfig shift(String name) {
        return ShiftConfig.builder().shiftId(UUID.randomUUID()).name(name).startTime("00:00").endTime("08:00").isActive(true).build();
    }
    @Test void initializationCreatesThreeFixedShiftsAndBaselineVersions() {
        when(shiftConfigRepository.save(any())).thenAnswer(call -> {
            ShiftConfig shift = call.getArgument(0);
            if(shift.getShiftId()==null) shift.setShiftId(UUID.randomUUID());
            return shift;
        });
        service.initDefaultShifts();
        ArgumentCaptor<ShiftVersion> versions = ArgumentCaptor.forClass(ShiftVersion.class);
        verify(shiftVersionRepository,times(3)).save(versions.capture());
        assertEquals(List.of("Ca Sáng","Ca Chiều","Ca Tối"),versions.getAllValues().stream().map(v -> v.getShift().getName()).toList());
        assertEquals(List.of(LocalTime.MIDNIGHT,LocalTime.of(8,0),LocalTime.of(16,0)),versions.getAllValues().stream().map(ShiftVersion::getStartTime).toList());
        assertEquals(LocalTime.of(23,59,59),versions.getAllValues().get(2).getEndTime());
        assertTrue(versions.getAllValues().stream().allMatch(v -> v.getEffectiveFrom().equals(LocalDate.of(1970,1,1))));
    }

    @ParameterizedTest @ValueSource(strings={"current","old","future","customVersions"})
    void initializationPreservesExistingVersionHistory(String state) {
        ShiftConfig morning = shift("Ca Sáng"), afternoon = shift("Ca Chiều"), evening = shift("Ca Tối");
        List<ShiftConfig> shifts = List.of(morning,afternoon,evening);
        for(ShiftConfig shift : shifts) when(shiftConfigRepository.findFirstByNameIgnoreCase(shift.getName())).thenReturn(Optional.of(shift));
        morning.setIsActive(false);
        LocalDate effective = state.equals("future") ? today.plusDays(1) : today.minusYears(1);
        ShiftVersion baseline = ShiftVersion.builder().shift(morning)
                .startTime(state.equals("current") ? LocalTime.MIDNIGHT : LocalTime.of(7,0)).endTime(LocalTime.of(8,0)).effectiveFrom(effective).build();
        List<ShiftVersion> originals = state.equals("customVersions") ? List.of(baseline,new ShiftVersion()) : List.of(baseline);
        when(shiftVersionRepository.findAllByShift_ShiftIdOrderByEffectiveFromDesc(morning.getShiftId())).thenReturn(originals);
        // The remaining shifts already have user-controlled version history.
        when(shiftVersionRepository.findAllByShift_ShiftIdOrderByEffectiveFromDesc(afternoon.getShiftId())).thenReturn(List.of(new ShiftVersion(),new ShiftVersion()));
        when(shiftVersionRepository.findAllByShift_ShiftIdOrderByEffectiveFromDesc(evening.getShiftId())).thenReturn(List.of(new ShiftVersion(),new ShiftVersion()));
        try(var dates = mockStatic(LocalDate.class,CALLS_REAL_METHODS)) {
            dates.when(() -> LocalDate.now(zone)).thenReturn(today);
            service.initDefaultShifts();
        }
        assertTrue(morning.getIsActive()); assertEquals("16:00",afternoon.getEndTime()); assertEquals("23:59:59",evening.getEndTime());
        if(state.equals("old")) {
            ArgumentCaptor<ShiftVersion> saved = ArgumentCaptor.forClass(ShiftVersion.class);
            verify(shiftVersionRepository,times(2)).save(saved.capture());
            assertSame(baseline,saved.getAllValues().get(0)); assertEquals(today.minusDays(1),baseline.getEffectiveTo());
            assertEquals(effective,baseline.getEffectiveFrom()); assertEquals(LocalTime.of(7,0),baseline.getStartTime());
            assertEquals(today,saved.getAllValues().get(1).getEffectiveFrom()); assertEquals(LocalTime.MIDNIGHT,saved.getAllValues().get(1).getStartTime());
        } else { verify(shiftVersionRepository,never()).save(any()); assertNull(baseline.getEffectiveTo()); }
    }

    @Test void listsUseFixedOrderAndCurrentEffectiveHours() {
        ShiftConfig morning = shift("Ca Sáng"), evening = shift("Ca Tối"), unrelated = shift("Ca cũ");
        when(shiftConfigRepository.findAll()).thenReturn(List.of(evening,unrelated,morning));
        ShiftVersion effective = ShiftVersion.builder().startTime(LocalTime.of(6,0)).endTime(LocalTime.of(11,0)).build();
        when(shiftVersionRepository.findEffective(morning.getShiftId(),today)).thenReturn(List.of(effective));
        try(var dates = mockStatic(LocalDate.class,CALLS_REAL_METHODS)) {
            dates.when(() -> LocalDate.now(zone)).thenReturn(today);
            var result = service.getAllActiveShifts();
            assertEquals(List.of("Ca Sáng","Ca Tối"),result.stream().map(r -> r.name()).toList());
            assertEquals("06:00",result.get(0).startTime()); assertEquals("11:00",result.get(0).endTime());
            assertEquals(evening.getStartTime(),result.get(1).startTime());
            assertEquals(result,service.getAllShifts());
        }
        assertFalse(ShiftConfigService.isFixedShift(null)); assertFalse(ShiftConfigService.isFixedShift(unrelated));
        assertEquals(Integer.MAX_VALUE,ShiftConfigService.fixedOrder(unrelated)); assertEquals(Integer.MAX_VALUE,ShiftConfigService.fixedOrder(null));
    }
}
