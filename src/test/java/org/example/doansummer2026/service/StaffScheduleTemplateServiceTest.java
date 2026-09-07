package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.scheduletemplate.ScheduleTemplateRequest;
import org.example.doansummer2026.dto.scheduletemplate.ScheduleTemplateResponse;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.ShiftConfig;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.StaffScheduleTemplate;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.repository.StaffScheduleRepository;
import org.example.doansummer2026.repository.StaffScheduleTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffScheduleTemplateServiceTest {
    @Mock StaffScheduleTemplateRepository repo;
    @Mock StaffScheduleRepository scheduleRepo;
    @Mock ShiftConfigRepository shiftRepo;
    @Mock StaffService staffService;
    private StaffScheduleTemplateService service;
    private UUID staffId;
    private UUID shiftId;
    private StaffInfo staff;
    private ShiftConfig shift;

    @BeforeEach
    void setUp() {
        service = new StaffScheduleTemplateService(repo, scheduleRepo, shiftRepo, staffService);
        staffId = UUID.randomUUID();
        shiftId = UUID.randomUUID();
        staff = StaffInfo.builder().staffId(staffId).staffCode("BS-001").build();
        shift = ShiftConfig.builder().shiftId(shiftId).name("Ca sáng").startTime("08:00")
                .endTime("12:00").isActive(true).build();
    }

    @Test
    void createDefaultsActiveAndMapsResponse() {
        when(staffService.findById(staffId)).thenReturn(staff);
        when(repo.findByStaffAndDayOfWeek(staff, DayOfWeek.MONDAY)).thenReturn(Optional.empty());
        when(shiftRepo.findById(shiftId)).thenReturn(Optional.of(shift));
        when(repo.save(any())).thenAnswer(invocation -> {
            StaffScheduleTemplate value = invocation.getArgument(0);
            value.setTemplateId(UUID.randomUUID());
            return value;
        });
        ScheduleTemplateResponse result = service.create(
                new ScheduleTemplateRequest(staffId, DayOfWeek.MONDAY, shiftId, null));
        assertEquals(staffId, result.staffId());
        assertEquals(DayOfWeek.MONDAY, result.dayOfWeek());
        assertTrue(result.isActive());
        assertEquals(shiftId.toString(), result.shift().id());
    }

    @Test
    void createRejectsDuplicateMissingAndInactiveShift() {
        when(staffService.findById(staffId)).thenReturn(staff);
        StaffScheduleTemplate duplicate = template(UUID.randomUUID(), DayOfWeek.MONDAY, shift, true);
        when(repo.findByStaffAndDayOfWeek(staff, DayOfWeek.MONDAY)).thenReturn(Optional.of(duplicate));
        assertThrows(ConflictException.class, () -> service.create(
                new ScheduleTemplateRequest(staffId, DayOfWeek.MONDAY, shiftId, true)));

        when(repo.findByStaffAndDayOfWeek(staff, DayOfWeek.TUESDAY)).thenReturn(Optional.empty());
        when(shiftRepo.findById(shiftId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.create(
                new ScheduleTemplateRequest(staffId, DayOfWeek.TUESDAY, shiftId, true)));

        UUID inactiveId = UUID.randomUUID();
        ShiftConfig inactive = ShiftConfig.builder().shiftId(inactiveId).isActive(false).build();
        when(repo.findByStaffAndDayOfWeek(staff, DayOfWeek.WEDNESDAY)).thenReturn(Optional.empty());
        when(shiftRepo.findById(inactiveId)).thenReturn(Optional.of(inactive));
        assertThrows(ConflictException.class, () -> service.create(
                new ScheduleTemplateRequest(staffId, DayOfWeek.WEDNESDAY, inactiveId, false)));
    }

    @Test
    void updateCanKeepExistingValuesAndChangeActiveFlag() {
        UUID id = UUID.randomUUID();
        StaffScheduleTemplate existing = template(id, DayOfWeek.MONDAY, shift, true);
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.findByStaffAndDayOfWeek(staff, DayOfWeek.MONDAY)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenReturn(existing);
        ScheduleTemplateResponse result = service.update(id,
                new ScheduleTemplateRequest(null, null, null, false));
        assertFalse(result.isActive());
        assertSame(shift, existing.getShift());
        verifyNoInteractions(staffService, shiftRepo);
    }

    @Test
    void updateChangesStaffDayAndShift() {
        UUID id = UUID.randomUUID();
        StaffScheduleTemplate existing = template(id, DayOfWeek.MONDAY, shift, true);
        UUID newStaffId = UUID.randomUUID();
        UUID newShiftId = UUID.randomUUID();
        StaffInfo newStaff = StaffInfo.builder().staffId(newStaffId).staffCode("BS-002").build();
        ShiftConfig newShift = ShiftConfig.builder().shiftId(newShiftId).name("Ca chiều")
                .startTime("13:00").endTime("17:00").isActive(true).build();
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(staffService.findById(newStaffId)).thenReturn(newStaff);
        when(repo.findByStaffAndDayOfWeek(newStaff, DayOfWeek.FRIDAY)).thenReturn(Optional.empty());
        when(shiftRepo.findById(newShiftId)).thenReturn(Optional.of(newShift));
        when(repo.save(existing)).thenReturn(existing);
        ScheduleTemplateResponse result = service.update(id,
                new ScheduleTemplateRequest(newStaffId, DayOfWeek.FRIDAY, newShiftId, null));
        assertEquals(newStaffId, result.staffId());
        assertEquals(DayOfWeek.FRIDAY, result.dayOfWeek());
        assertEquals(newShiftId.toString(), result.shift().id());
    }

    @Test
    void updateRejectsDuplicateAndInactiveShift() {
        UUID id = UUID.randomUUID();
        StaffScheduleTemplate existing = template(id, DayOfWeek.MONDAY, shift, true);
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.findByStaffAndDayOfWeek(staff, DayOfWeek.TUESDAY))
                .thenReturn(Optional.of(template(UUID.randomUUID(), DayOfWeek.TUESDAY, shift, true)));
        assertThrows(ConflictException.class, () -> service.update(id,
                new ScheduleTemplateRequest(null, DayOfWeek.TUESDAY, null, null)));

        when(repo.findByStaffAndDayOfWeek(staff, DayOfWeek.MONDAY)).thenReturn(Optional.of(existing));
        UUID inactiveId = UUID.randomUUID();
        when(shiftRepo.findById(inactiveId)).thenReturn(Optional.of(
                ShiftConfig.builder().shiftId(inactiveId).isActive(false).build()));
        assertThrows(ConflictException.class, () -> service.update(id,
                new ScheduleTemplateRequest(null, null, inactiveId, null)));
    }

    @Test
    void deleteSoftDisablesUsedTemplateAndDeletesUnusedOne() {
        UUID usedId = UUID.randomUUID();
        StaffScheduleTemplate used = template(usedId, DayOfWeek.MONDAY, shift, true);
        when(repo.findById(usedId)).thenReturn(Optional.of(used));
        when(scheduleRepo.countByTemplate_TemplateId(usedId)).thenReturn(2L);
        service.delete(usedId);
        assertFalse(used.getIsActive());
        verify(repo).save(used);

        UUID unusedId = UUID.randomUUID();
        StaffScheduleTemplate unused = template(unusedId, DayOfWeek.TUESDAY, shift, true);
        when(repo.findById(unusedId)).thenReturn(Optional.of(unused));
        when(scheduleRepo.countByTemplate_TemplateId(unusedId)).thenReturn(0L);
        service.delete(unusedId);
        verify(repo).delete(unused);
    }

    @Test
    void listGetAndMissingIdAreHandled() {
        UUID id = UUID.randomUUID();
        StaffScheduleTemplate value = template(id, DayOfWeek.MONDAY, shift, true);
        when(staffService.findById(staffId)).thenReturn(staff);
        when(repo.findByStaff(staff)).thenReturn(List.of(value));
        assertEquals(1, service.listByStaff(staffId).size());

        when(repo.findById(id)).thenReturn(Optional.of(value));
        assertEquals(id, service.get(id).templateId());

        UUID missing = UUID.randomUUID();
        when(repo.findById(missing)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(missing));
    }

    private StaffScheduleTemplate template(UUID id, DayOfWeek day, ShiftConfig selectedShift, boolean active) {
        return StaffScheduleTemplate.builder().templateId(id).staff(staff).dayOfWeek(day)
                .shift(selectedShift).isActive(active).build();
    }
}
