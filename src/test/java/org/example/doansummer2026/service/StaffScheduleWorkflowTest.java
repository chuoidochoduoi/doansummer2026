package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.schedule.*;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.*;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffScheduleWorkflowTest {
    @Mock StaffScheduleRepository scheduleRepo;
    @Mock StaffScheduleTemplateRepository templateRepo;
    @Mock ShiftConfigRepository shiftConfigRepo;
    @Mock StaffService staffService;
    @Mock NotificationService notificationService;
    @Mock StaffInfoRepository staffInfoRepository;
    @Mock ShiftScheduleResolver shiftScheduleResolver;
    @Mock ServiceAvailabilityService serviceAvailabilityService;
    @Mock AppointmentRepository appointmentRepository;
    @Mock StaffDutyService staffDutyService;
    @InjectMocks StaffScheduleService service;
    final LocalDate today = LocalDate.of(2026, 9, 4);
    final LocalDate tomorrow = today.plusDays(1);
    final ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
    MockedStatic<LocalDate> dates;
    @BeforeEach void fixDate() {
        dates = mockStatic(LocalDate.class, CALLS_REAL_METHODS);
        dates.when(() -> LocalDate.now(zone)).thenReturn(today);
    }
    @AfterEach void restoreDate() { dates.close(); }
    StaffInfo staff() {
        return StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(SystemRole.RECEPTIONIST)
                .profile(Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Minh An")
                        .account(Account.builder().isActive(true).build()).build()).build();
    }
    ShiftConfig shift() {
        return ShiftConfig.builder().shiftId(UUID.randomUUID()).name("Ca Sáng")
                .startTime("08:00").endTime("12:00").isActive(true).build();
    }
    void open(ShiftConfig shift, LocalDate date) {
        when(shiftScheduleResolver.resolve(shift, date)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                shift, null, LocalTime.parse(shift.getStartTime()), LocalTime.parse(shift.getEndTime()), ShiftTimeSource.NORMAL, null));
    }
    void lookup(StaffInfo staff, ShiftConfig shift) {
        when(staffInfoRepository.findByIdForScheduleUpdate(staff.getStaffId())).thenReturn(Optional.of(staff));
        when(shiftConfigRepo.findByIdForScheduleUpdate(shift.getShiftId())).thenReturn(Optional.of(shift));
    }
    StaffSchedule schedule(StaffInfo staff, ShiftConfig shift, LocalDate date) {
        return StaffSchedule.builder().scheduleId(UUID.randomUUID()).staff(staff).shift(shift)
                .workDate(date).status(ScheduleStatus.SCHEDULED).isCustom(false).build();
    }
    ScheduleCreateRequest request(StaffInfo staff, ShiftConfig shift, LocalDate date) {
        return new ScheduleCreateRequest(staff.getStaffId(), date, shift.getShiftId(), null, null, null, "Trực quầy");
    }
    void save() { when(scheduleRepo.save(any())).thenAnswer(call -> call.getArgument(0)); }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void createPersistsResolvedHoursEvenIfNotificationFails(boolean notificationFails) {
        StaffInfo staff = staff(); ShiftConfig shift = shift(); lookup(staff, shift); open(shift, tomorrow); save();
        if (notificationFails) doThrow(new IllegalStateException("notification unavailable")).when(notificationService).create(any());
        var result = service.create(request(staff, shift, tomorrow));
        assertEquals(staff.getStaffId(), result.staffId()); assertEquals(ScheduleStatus.SCHEDULED, result.status());
        assertFalse(result.isCustom()); assertEquals("Trực quầy", result.note());
        ArgumentCaptor<StaffSchedule> captured = ArgumentCaptor.forClass(StaffSchedule.class);
        verify(scheduleRepo).save(captured.capture());
        assertEquals(LocalTime.of(8,0), captured.getValue().getActualStartTime());
        assertEquals(LocalTime.of(12,0), captured.getValue().getActualEndTime());
    }

    @ParameterizedTest @ValueSource(strings={"past","missingStaff","missingShift","inactiveShift","nonFixed","closed","duplicate","overlap","missingRoom","otherDoctor"})
    void createRejectsUnavailableAssignmentsBeforeSaving(String reason) {
        StaffInfo staff = staff(); ShiftConfig shift = shift(); LocalDate date = reason.equals("past") ? today.minusDays(1) : tomorrow;
        if (!Set.of("past","missingStaff").contains(reason)) when(staffInfoRepository.findByIdForScheduleUpdate(staff.getStaffId())).thenReturn(Optional.of(staff));
        if (!Set.of("past","missingStaff","missingShift").contains(reason)) when(shiftConfigRepo.findByIdForScheduleUpdate(shift.getShiftId())).thenReturn(Optional.of(shift));
        if (reason.equals("inactiveShift")) shift.setIsActive(false);
        if (reason.equals("nonFixed")) shift.setName("Ca tùy ý");
        if (reason.equals("closed")) when(shiftScheduleResolver.resolve(shift, date)).thenReturn(new ShiftScheduleResolver.ResolvedShift(shift,null,null,null,ShiftTimeSource.NORMAL,ShiftUnavailableReason.CLINIC_CLOSED));
        if (Set.of("duplicate","overlap","missingRoom","otherDoctor").contains(reason)) open(shift,date);
        if (reason.equals("duplicate")) when(scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(),date)).thenReturn(List.of(schedule(staff,shift,date)));
        if (reason.equals("overlap")) {
            ShiftConfig other = shift(); other.setName("Ca Chiều"); other.setStartTime("11:00"); other.setEndTime("16:00"); open(other,date);
            when(scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(),date)).thenReturn(List.of(schedule(staff,other,date)));
        }
        if (reason.equals("missingRoom")) staff.setSystemRole(SystemRole.NURSE);
        if (reason.equals("otherDoctor")) {
            staff.setSystemRole(SystemRole.DOCTOR); staff.setDepartment(Department.builder().departmentId(UUID.randomUUID()).build());
            StaffInfo other = staff(); other.setSystemRole(SystemRole.DOCTOR); other.setDepartment(staff.getDepartment());
            when(scheduleRepo.findAllByWorkDateAndShift_ShiftIdAndStatus(date,shift.getShiftId(),ScheduleStatus.SCHEDULED)).thenReturn(List.of(schedule(other,shift,date)));
        }
        RuntimeException failure = assertThrows(RuntimeException.class, () -> service.create(request(staff,shift,date)));
        assertTrue(reason.startsWith("missingStaff") || reason.equals("missingShift") ? failure instanceof ResourceNotFoundException : failure instanceof ConflictException);
        verify(scheduleRepo,never()).save(any()); verifyNoInteractions(notificationService);
    }

    @ParameterizedTest @CsvSource({"08:00,12:00,12:00,16:00","12:00,16:00,08:00,12:00"})
    void touchingShiftBoundariesDoNotOverlap(String start,String end,String otherStart,String otherEnd) {
        StaffInfo staff = staff(); staff.setProfile(null); ShiftConfig shift = shift();
        shift.setStartTime(start); shift.setEndTime(end); ShiftConfig other = shift(); other.setStartTime(otherStart); other.setEndTime(otherEnd);
        lookup(staff,shift); open(shift,tomorrow); open(other,tomorrow); save();
        when(scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(),tomorrow)).thenReturn(List.of(schedule(staff,other,tomorrow)));
        service.create(request(staff,shift,tomorrow)); verify(scheduleRepo).save(any()); verifyNoInteractions(notificationService);
    }

    @ParameterizedTest @ValueSource(strings={"mon","TUE","wed","thu","fri","sat","sun"})
    void assignResolvesDayAndRepeatedAddIsIdempotent(String day) {
        StaffInfo staff = staff(); ShiftConfig shift = shift(); lookup(staff,shift);
        LocalDate week = today.plusWeeks(1).with(DayOfWeek.MONDAY);
        int index = List.of("mon","tue","wed","thu","fri","sat","sun").indexOf(day.toLowerCase());
        LocalDate date = week.plusDays(index); open(shift,date);
        StaffSchedule existing = schedule(staff,shift,date);
        when(scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(),date)).thenReturn(List.of(),List.of(),List.of(existing));
        var req = new ScheduleAssignRequest(week,shift.getShiftId(),day,staff.getStaffId(),"add");
        service.assignStaff(req); service.assignStaff(req);
        ArgumentCaptor<StaffSchedule> capture = ArgumentCaptor.forClass(StaffSchedule.class);
        verify(scheduleRepo).save(capture.capture()); assertEquals(date,capture.getValue().getWorkDate()); assertTrue(capture.getValue().getIsCustom());
    }

    @ParameterizedTest @ValueSource(strings={"noProfile","noAccount","inactive"})
    void assignRejectsInactiveStaff(String reason) {
        StaffInfo staff = staff(); ShiftConfig shift = shift(); lookup(staff,shift); open(shift,tomorrow);
        if (reason.equals("noProfile")) staff.setProfile(null);
        else if (reason.equals("noAccount")) staff.getProfile().setAccount(null);
        else staff.getProfile().getAccount().setIsActive(false);
        assertThrows(ConflictException.class, () -> service.assignStaff(new ScheduleAssignRequest(tomorrow,shift.getShiftId(),"sat",staff.getStaffId(),"add")));
        verify(scheduleRepo,never()).save(any());
    }

    @Test void removeFutureAssignmentDeletesAllDuplicatesAndUnknownActionIsRejected() {
        StaffInfo staff = staff(); ShiftConfig shift = shift(); lookup(staff,shift);
        when(scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(),tomorrow)).thenReturn(List.of(schedule(staff,shift,tomorrow),schedule(staff,shift,tomorrow)));
        service.assignStaff(new ScheduleAssignRequest(tomorrow,shift.getShiftId(),"sat",staff.getStaffId(),"REMOVE"));
        verify(scheduleRepo).deleteByStaffAndWorkDateAndShift(staff,tomorrow,shift);
        assertThrows(ConflictException.class, () -> service.assignStaff(new ScheduleAssignRequest(tomorrow,shift.getShiftId(),"sat",staff.getStaffId(),"other")));
        assertThrows(IllegalArgumentException.class, () -> service.assignStaff(new ScheduleAssignRequest(tomorrow,shift.getShiftId(),"bad",staff.getStaffId(),"add")));
    }

    @ParameterizedTest @ValueSource(strings={"today","past","missingDate","completed"})
    void deleteOnlyAllowsFutureScheduledRows(String reason) {
        StaffSchedule row = schedule(staff(),shift(),tomorrow);
        if(reason.equals("today")) row.setWorkDate(today);
        if(reason.equals("past")) row.setWorkDate(today.minusDays(1));
        if(reason.equals("missingDate")) row.setWorkDate(null);
        if(reason.equals("completed")) row.setStatus(ScheduleStatus.COMPLETED);
        when(scheduleRepo.findById(row.getScheduleId())).thenReturn(Optional.of(row));
        assertThrows(ConflictException.class, () -> service.delete(row.getScheduleId())); verify(scheduleRepo,never()).deleteById(any());
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void lastQualifiedStaffCannotBeRemovedIfBookedServiceLosesCoverage(boolean coveredAfter) {
        StaffInfo staff = staff(); ShiftConfig shift = shift(); StaffSchedule row = schedule(staff,shift,tomorrow);
        when(scheduleRepo.findById(row.getScheduleId())).thenReturn(Optional.of(row));
        MedicalService medicalService = MedicalService.builder().serviceId(UUID.randomUUID()).name("Khám Nội").build();
        Appointment appointment = Appointment.builder().shiftName(shift.getName()).services(new HashSet<>(List.of(medicalService))).build();
        when(appointmentRepository.findActiveBetween(eq(tomorrow.atStartOfDay()),eq(tomorrow.plusDays(1).atStartOfDay()),anyList())).thenReturn(List.of(appointment));
        when(serviceAvailabilityService.evaluate(medicalService,tomorrow,shift,false)).thenReturn(new ServiceAvailabilityService.Evaluation(true,null,List.of(staff)));
        when(serviceAvailabilityService.evaluate(medicalService,tomorrow,shift,false,staff.getStaffId())).thenReturn(new ServiceAvailabilityService.Evaluation(coveredAfter,null,List.of()));
        if(coveredAfter) { service.delete(row.getScheduleId()); verify(scheduleRepo).deleteById(row.getScheduleId()); }
        else { assertThrows(ConflictException.class, () -> service.delete(row.getScheduleId())); verify(scheduleRepo,never()).deleteById(any()); verifyNoInteractions(notificationService); }
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void updateNotesAndFutureShiftPreserveOtherFields(boolean changeShift) {
        StaffInfo staff = staff(); ShiftConfig original = shift(), replacement = shift();
        replacement.setName("Ca Chiều"); replacement.setStartTime("12:00"); replacement.setEndTime("16:00");
        StaffSchedule row = schedule(staff,original,tomorrow); save();
        when(scheduleRepo.findByIdForUpdate(row.getScheduleId())).thenReturn(Optional.of(row));
        if(changeShift) {
            when(shiftConfigRepo.findByIdForScheduleUpdate(replacement.getShiftId())).thenReturn(Optional.of(replacement));
            open(replacement,tomorrow);
            when(scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(),tomorrow)).thenReturn(List.of(row));
        }
        var result = service.update(row.getScheduleId(),new ScheduleUpdateRequest(changeShift ? replacement.getShiftId() : null,ScheduleStatus.SCHEDULED,true,"Đổi ghi chú"));
        assertEquals("Đổi ghi chú", result.note()); assertTrue(result.isCustom());
        assertSame(changeShift ? replacement : original,row.getShift());
        assertEquals(tomorrow,result.workDate()); verify(scheduleRepo).save(row);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void templateGenerationSkipsExistingUnlessOverrideRequested(boolean override) {
        StaffInfo staff = staff(); ShiftConfig shift = shift(); LocalDate week = today.plusWeeks(1).with(DayOfWeek.MONDAY);
        StaffScheduleTemplate template = StaffScheduleTemplate.builder().templateId(UUID.randomUUID()).staff(staff).shift(shift).dayOfWeek(DayOfWeek.MONDAY).isActive(true).build();
        StaffSchedule existing = schedule(staff,shift,week);
        when(staffService.findById(staff.getStaffId())).thenReturn(staff);
        when(templateRepo.findByStaff(staff)).thenReturn(List.of(template));
        when(scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(),week)).thenReturn(List.of(existing));
        when(scheduleRepo.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        if(override) open(shift,week);
        var result = service.generateFromTemplates(week.plusDays(2),List.of(staff.getStaffId()),override);
        assertEquals(override ? 1 : 0,result.size());
        verify(scheduleRepo).findAllByWorkDateBetweenForUpdate(week,week.plusDays(6));
        if(override) { verify(scheduleRepo).deleteAll(List.of(existing)); verify(scheduleRepo).flush(); assertEquals(template.getTemplateId(),result.get(0).templateId()); }
        else verify(scheduleRepo,never()).deleteAll(any());
    }

    @Test void templateGenerationUsesActiveTemplatesAndNormalizesWeek() {
        StaffInfo staff = staff(); ShiftConfig shift = shift(); LocalDate week = today.plusWeeks(1).with(DayOfWeek.MONDAY);
        StaffScheduleTemplate active = StaffScheduleTemplate.builder().staff(staff).shift(shift).dayOfWeek(DayOfWeek.SUNDAY).isActive(true).build();
        StaffScheduleTemplate inactive = StaffScheduleTemplate.builder().staff(staff).isActive(false).build();
        when(templateRepo.findAll()).thenReturn(List.of(active,inactive)); when(templateRepo.findByStaff(staff)).thenReturn(List.of(inactive,active));
        open(shift,week.plusDays(6)); when(scheduleRepo.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.generateFromTemplates(week.plusDays(3),null,null);
        assertEquals(1,result.size()); assertEquals(week.plusDays(6),result.get(0).workDate()); assertFalse(result.get(0).isCustom());
        verify(templateRepo).findByStaff(staff); verifyNoInteractions(staffService);
    }

    @Test void copyWeekDeduplicatesAndSkipsInactiveStaffWithoutMutatingSource() {
        LocalDate source = today.with(DayOfWeek.MONDAY), target = source.plusWeeks(1);
        StaffInfo staff = staff(); ShiftConfig shift = shift(); StaffSchedule original = schedule(staff,shift,source.plusDays(1)); original.setNote("Trực bổ sung");
        StaffSchedule duplicate = schedule(staff,shift,source.plusDays(1));
        StaffInfo inactive = staff(); inactive.getProfile().getAccount().setIsActive(false);
        when(scheduleRepo.findAllByWorkDateBetweenForUpdate(source,source.plusDays(6))).thenReturn(List.of(original,duplicate,schedule(inactive,shift,source)));
        when(scheduleRepo.saveAll(any())).thenAnswer(call -> call.getArgument(0)); open(shift,target.plusDays(1));
        var result = service.copyWeek(source.plusDays(2),target.plusDays(3));
        assertEquals(1,result.size()); assertEquals(target.plusDays(1),result.get(0).getWorkDate());
        assertTrue(result.get(0).getIsCustom()); assertEquals("Trực bổ sung",result.get(0).getNote());
        assertEquals(source.plusDays(1),original.getWorkDate()); assertNotSame(original,result.get(0));
        verify(scheduleRepo).deleteAll(List.of()); verify(scheduleRepo).flush();
    }

    @ParameterizedTest @ValueSource(strings={"same","past","empty"})
    void copyRejectsInvalidWeeksWithoutDeletingDestination(String condition) {
        LocalDate source = today.with(DayOfWeek.MONDAY);
        LocalDate target = condition.equals("same") ? source : condition.equals("past") ? source.minusWeeks(1) : source.plusWeeks(1);
        assertThrows(ConflictException.class, () -> service.copyWeek(source,target)); verify(scheduleRepo,never()).deleteAll(any());
    }

    @Test void templateOverrideMustNotReplaceCompletedHistoricalShift() {
        StaffInfo staff = staff(); ShiftConfig shift = shift();
        LocalDate pastWeek = today.with(DayOfWeek.MONDAY).minusWeeks(1);
        StaffSchedule completed = schedule(staff,shift,pastWeek);
        completed.setStatus(ScheduleStatus.COMPLETED);
        StaffScheduleTemplate template = StaffScheduleTemplate.builder().templateId(UUID.randomUUID())
                .staff(staff).shift(shift).dayOfWeek(DayOfWeek.MONDAY).isActive(true).build();
        // These reads/saves are fallback stubs: a correct early guard may not reach them.
        lenient().when(staffService.findById(staff.getStaffId())).thenReturn(staff);
        lenient().when(templateRepo.findByStaff(staff)).thenReturn(List.of(template));
        lenient().when(scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(),pastWeek))
                .thenReturn(List.of(completed),List.of());
        lenient().when(shiftScheduleResolver.resolve(shift,pastWeek)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                shift,null,LocalTime.of(8,0),LocalTime.of(12,0),ShiftTimeSource.NORMAL,null));
        lenient().when(scheduleRepo.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        assertAll(
                () -> assertThrows(ConflictException.class,
                        () -> service.generateFromTemplates(pastWeek,List.of(staff.getStaffId()),true)),
                () -> verify(scheduleRepo,never()).deleteAll(any()),
                () -> verify(scheduleRepo,never()).saveAll(any()));
    }

    @ParameterizedTest @CsvSource({"07:59,true", "08:00,false", "09:00,false"})
    void generateTodayOnlyBeforeResolvedShiftStart(String clock, boolean allowed) {
        StaffInfo staff = staff(); ShiftConfig shift = shift();
        StaffScheduleTemplate template = StaffScheduleTemplate.builder().staff(staff).shift(shift)
                .dayOfWeek(today.getDayOfWeek()).isActive(true).build();
        when(staffService.findById(staff.getStaffId())).thenReturn(staff);
        when(templateRepo.findByStaff(staff)).thenReturn(List.of(template)); open(shift,today);
        LocalTime now = LocalTime.parse(clock);
        try (var time = mockStatic(LocalTime.class,CALLS_REAL_METHODS)) {
            time.when(() -> LocalTime.now(zone)).thenReturn(now);
            if (allowed) {
                when(scheduleRepo.saveAll(any())).thenAnswer(call -> call.getArgument(0));
                assertEquals(today,service.generateFromTemplates(today,List.of(staff.getStaffId()),true).get(0).workDate());
            } else {
                assertThrows(ConflictException.class, () -> service.generateFromTemplates(today,List.of(staff.getStaffId()),true));
                verify(scheduleRepo,never()).saveAll(any());
            }
        }
        verify(scheduleRepo,never()).deleteAll(any());
    }

    @ParameterizedTest @EnumSource(value=ScheduleStatus.class,names={"COMPLETED","ABSENT"})
    void overrideCannotResetProcessedStatusEvenOnFutureDate(ScheduleStatus status) {
        StaffInfo staff = staff(); ShiftConfig shift = shift(); StaffSchedule row = schedule(staff,shift,tomorrow); row.setStatus(status);
        StaffScheduleTemplate template = StaffScheduleTemplate.builder().staff(staff).shift(shift).dayOfWeek(tomorrow.getDayOfWeek()).isActive(true).build();
        when(staffService.findById(staff.getStaffId())).thenReturn(staff); when(templateRepo.findByStaff(staff)).thenReturn(List.of(template));
        when(scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(),tomorrow)).thenReturn(List.of(row));
        assertThrows(ConflictException.class, () -> service.generateFromTemplates(tomorrow,List.of(staff.getStaffId()),true));
        verify(scheduleRepo,never()).deleteAll(any()); verify(scheduleRepo,never()).saveAll(any());
    }

    @Test void invalidLaterTemplateDoesNotDeleteEarlierValidReplacement() {
        StaffInfo staff = staff(); ShiftConfig shift = shift();
        StaffSchedule future = schedule(staff,shift,tomorrow);
        LocalDate yesterday = today.minusDays(1);
        StaffScheduleTemplate valid = StaffScheduleTemplate.builder().staff(staff).shift(shift).dayOfWeek(tomorrow.getDayOfWeek()).isActive(true).build();
        StaffScheduleTemplate past = StaffScheduleTemplate.builder().staff(staff).shift(shift).dayOfWeek(yesterday.getDayOfWeek()).isActive(true).build();
        when(staffService.findById(staff.getStaffId())).thenReturn(staff); when(templateRepo.findByStaff(staff)).thenReturn(List.of(valid,past));
        when(scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(),tomorrow)).thenReturn(List.of(future));
        open(shift,tomorrow); open(shift,yesterday);
        assertThrows(ConflictException.class, () -> service.generateFromTemplates(today,List.of(staff.getStaffId()),true));
        verify(scheduleRepo,never()).deleteAll(any()); verify(scheduleRepo,never()).flush(); verify(scheduleRepo,never()).saveAll(any());
    }

    @Test void overrideIgnoresAllExactDuplicatesButNotAnotherOverlappingShift() {
        StaffInfo staff = staff(); ShiftConfig shift = shift(), other = shift();
        StaffSchedule first = schedule(staff,shift,tomorrow), duplicate = schedule(staff,shift,tomorrow), overlap = schedule(staff,other,tomorrow);
        StaffScheduleTemplate template = StaffScheduleTemplate.builder().staff(staff).shift(shift).dayOfWeek(tomorrow.getDayOfWeek()).isActive(true).build();
        when(staffService.findById(staff.getStaffId())).thenReturn(staff); when(templateRepo.findByStaff(staff)).thenReturn(List.of(template));
        open(shift,tomorrow); open(other,tomorrow);
        when(scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(),tomorrow)).thenReturn(List.of(first,duplicate,overlap));
        assertThrows(ConflictException.class, () -> service.generateFromTemplates(tomorrow,List.of(staff.getStaffId()),true));
        verify(scheduleRepo,never()).deleteAll(any()); verify(scheduleRepo,never()).saveAll(any());
    }

    @Test void ownScheduleReadEnforcesStaffIdAndLookupNotFound() {
        StaffSchedule row = schedule(staff(),shift(),tomorrow);
        when(scheduleRepo.findById(row.getScheduleId())).thenReturn(Optional.of(row));
        assertEquals(row.getScheduleId(),service.get(row.getScheduleId()).scheduleId());
        assertEquals(row.getScheduleId(),service.getForStaff(row.getScheduleId(),row.getStaff().getStaffId()).scheduleId());
        assertThrows(ConflictException.class, () -> service.getForStaff(row.getScheduleId(),UUID.randomUUID()));
        row.setStaff(null); assertThrows(ConflictException.class, () -> service.getForStaff(row.getScheduleId(),UUID.randomUUID()));
        assertThrows(ResourceNotFoundException.class, () -> service.get(UUID.randomUUID()));
        assertThrows(ResourceNotFoundException.class, () -> service.delete(UUID.randomUUID()));
        assertThrows(ResourceNotFoundException.class, () -> service.update(UUID.randomUUID(),new ScheduleUpdateRequest(null,null,null,null)));
    }
}
