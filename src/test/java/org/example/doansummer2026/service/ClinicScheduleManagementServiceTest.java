package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.shift.ClinicScheduleExceptionRequest;
import org.example.doansummer2026.dto.shift.ShiftVersionCreateRequest;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.ClinicScheduleExceptionType;
import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.enums.ShiftTimeSource;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.ClinicScheduleException;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.ShiftConfig;
import org.example.doansummer2026.model.ShiftVersion;
import org.example.doansummer2026.model.StaffSchedule;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.ClinicScheduleExceptionRepository;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.repository.ShiftVersionRepository;
import org.example.doansummer2026.repository.StaffScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicScheduleManagementServiceTest {

    @Mock ShiftConfigRepository shiftRepository;
    @Mock ShiftVersionRepository versionRepository;
    @Mock ClinicScheduleExceptionRepository exceptionRepository;
    @Mock StaffScheduleRepository scheduleRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock AccountRepository accountRepository;
    @Mock ShiftScheduleResolver resolver;
    @InjectMocks ClinicScheduleManagementService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ensureInitialVersionsCreatesOnlyMissingFixedVersions() {
        ShiftConfig morning = shift(ShiftConfigService.MORNING, "00:00", "08:00");
        ShiftConfig afternoon = shift(ShiftConfigService.AFTERNOON, "08:00", "16:00");
        ShiftConfig custom = shift("Ca tùy chỉnh", "09:00", "11:00");
        when(shiftRepository.findAll()).thenReturn(List.of(morning, afternoon, custom));
        when(versionRepository.existsByShift_ShiftId(morning.getShiftId())).thenReturn(false);
        when(versionRepository.existsByShift_ShiftId(afternoon.getShiftId())).thenReturn(true);

        service.ensureInitialVersions();

        verify(versionRepository).save(argThat(value -> value.getShift() == morning
                && LocalTime.MIDNIGHT.equals(value.getStartTime())
                && LocalTime.of(8, 0).equals(value.getEndTime())
                && LocalDate.of(1970, 1, 1).equals(value.getEffectiveFrom())));
        verify(versionRepository, never()).existsByShift_ShiftId(custom.getShiftId());
    }

    @Test
    void historyMapsVersionsAndRequireShiftValidatesMissingAndCustom() {
        ShiftConfig morning = shift(ShiftConfigService.MORNING, "00:00", "08:00");
        ShiftVersion version = version(morning, LocalDate.of(2026, 1, 1), null,
                LocalTime.MIDNIGHT, LocalTime.of(8, 0));
        when(shiftRepository.findById(morning.getShiftId())).thenReturn(Optional.of(morning));
        when(versionRepository.findAllByShift_ShiftIdOrderByEffectiveFromDesc(morning.getShiftId()))
                .thenReturn(List.of(version));

        var result = service.history(morning.getShiftId());

        assertEquals(1, result.size());
        assertEquals(version.getShiftVersionId(), result.get(0).shiftVersionId());

        UUID missing = UUID.randomUUID();
        UUID customId = UUID.randomUUID();
        when(shiftRepository.findById(missing)).thenReturn(Optional.empty());
        when(shiftRepository.findById(customId)).thenReturn(Optional.of(
                ShiftConfig.builder().shiftId(customId).name("Khác").build()));
        assertThrows(ResourceNotFoundException.class, () -> service.history(missing));
        assertThrows(BadRequestException.class, () -> service.history(customId));
    }

    @Test
    void previewVersionImpactCollectsAppointmentsSchedulesServicesAndDates() {
        LocalDate from = future(5);
        ShiftConfig morning = shift(ShiftConfigService.MORNING, "00:00", "08:00");
        ShiftConfig afternoon = shift(ShiftConfigService.AFTERNOON, "08:00", "16:00");
        MedicalService serviceA = MedicalService.builder().serviceId(UUID.randomUUID()).build();
        MedicalService serviceB = MedicalService.builder().serviceId(UUID.randomUUID()).build();
        Appointment byVersion = appointment(from.atTime(7, 0), morning, null, serviceA);
        Appointment byLegacyName = appointment(from.plusDays(1).atTime(7, 0), null, morning.getName(), serviceB);
        Appointment other = appointment(from.atTime(9, 0), afternoon, null, serviceB);
        StaffSchedule scheduled = staffSchedule(from.plusDays(2), morning, ScheduleStatus.SCHEDULED);
        StaffSchedule absent = staffSchedule(from.plusDays(3), morning, ScheduleStatus.ABSENT);
        StaffSchedule noShift = StaffSchedule.builder().scheduleId(UUID.randomUUID()).workDate(from)
                .status(ScheduleStatus.SCHEDULED).build();
        when(shiftRepository.findById(morning.getShiftId())).thenReturn(Optional.of(morning));
        when(scheduleRepository.findAllByWorkDateBetween(from, LocalDate.of(9999, 12, 31)))
                .thenReturn(List.of(scheduled, absent, noShift));
        when(appointmentRepository.findActiveBetween(from.atStartOfDay(),
                LocalDateTime.of(9999, 12, 31, 23, 59),
                List.of(AppointmentStatus.PENDING, AppointmentStatus.RESCHEDULED)))
                .thenReturn(List.of(byVersion, byLegacyName, other));

        var result = service.previewVersionImpact(morning.getShiftId(), from);

        assertAll(
                () -> assertTrue(result.blocked()),
                () -> assertEquals(2, result.appointmentCount()),
                () -> assertEquals(1, result.staffScheduleCount()),
                () -> assertEquals(new HashSet<>(List.of(serviceA.getServiceId(), serviceB.getServiceId())),
                        result.affectedServiceIds()),
                () -> assertEquals(3, result.affectedDates().size()),
                () -> assertEquals(2, result.appointmentIds().size()),
                () -> assertEquals(List.of(scheduled.getScheduleId()), result.staffScheduleIds()));
    }

    @Test
    void createVersionClosesLatestNormalizesReasonAndRecordsActor() {
        LocalDate effective = future(10);
        ShiftConfig morning = shift(ShiftConfigService.MORNING, "00:00", "08:00");
        ShiftVersion latest = version(morning, effective.minusDays(30), null,
                LocalTime.MIDNIGHT, LocalTime.of(8, 0));
        UUID accountId = UUID.randomUUID();
        authenticate("manager", true);
        when(accountRepository.findFirstByUsername("manager"))
                .thenReturn(Optional.of(Account.builder().accountId(accountId).username("manager").build()));
        when(shiftRepository.findById(morning.getShiftId())).thenReturn(Optional.of(morning));
        when(versionRepository.findAllByShiftForUpdate(morning.getShiftId())).thenReturn(List.of(latest));
        when(versionRepository.findAll()).thenReturn(List.of());
        when(versionRepository.save(any())).thenAnswer(invocation -> {
            ShiftVersion saved = invocation.getArgument(0);
            saved.setShiftVersionId(UUID.randomUUID());
            return saved;
        });

        var response = service.createVersion(morning.getShiftId(), new ShiftVersionCreateRequest(
                LocalTime.of(0, 30), LocalTime.of(8, 30), effective, "  Điều chỉnh   mùa hè "));

        assertAll(
                () -> assertEquals(effective.minusDays(1), latest.getEffectiveTo()),
                () -> assertEquals("Điều chỉnh mùa hè", response.changeReason()),
                () -> assertEquals(accountId, response.createdBy()),
                () -> assertEquals(LocalTime.of(0, 30), response.startTime()));
    }

    @Test
    void createVersionRejectsInvalidDateTimeReasonImpactAndOrdering() {
        LocalDate effective = future(10);
        ShiftConfig morning = shift(ShiftConfigService.MORNING, "00:00", "08:00");
        when(shiftRepository.findById(morning.getShiftId())).thenReturn(Optional.of(morning));

        assertThrows(BadRequestException.class, () -> service.createVersion(morning.getShiftId(),
                new ShiftVersionCreateRequest(LocalTime.NOON, LocalTime.NOON, effective, "Lý do")));
        assertThrows(BadRequestException.class, () -> service.createVersion(morning.getShiftId(),
                new ShiftVersionCreateRequest(null, LocalTime.NOON, effective, "Lý do")));
        assertThrows(BadRequestException.class, () -> service.createVersion(morning.getShiftId(),
                new ShiftVersionCreateRequest(LocalTime.NOON, null, effective, "Lý do")));
        assertThrows(BadRequestException.class, () -> service.createVersion(morning.getShiftId(),
                new ShiftVersionCreateRequest(LocalTime.of(8, 0), LocalTime.NOON,
                        LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")), "Lý do")));
        assertThrows(BadRequestException.class, () -> service.createVersion(morning.getShiftId(),
                new ShiftVersionCreateRequest(LocalTime.of(8, 0), LocalTime.NOON, effective, "   ")));

        StaffSchedule impacted = staffSchedule(effective, morning, ScheduleStatus.SCHEDULED);
        when(scheduleRepository.findAllByWorkDateBetween(effective, LocalDate.of(9999, 12, 31)))
                .thenReturn(List.of(impacted));
        assertThrows(ConflictException.class, () -> service.createVersion(morning.getShiftId(),
                new ShiftVersionCreateRequest(LocalTime.of(8, 0), LocalTime.NOON, effective, "Lý do")));

        when(scheduleRepository.findAllByWorkDateBetween(effective, LocalDate.of(9999, 12, 31)))
                .thenReturn(List.of());
        when(versionRepository.findAllByShiftForUpdate(morning.getShiftId())).thenReturn(List.of(
                version(morning, effective, null, LocalTime.MIDNIGHT, LocalTime.of(8, 0))));
        assertThrows(ConflictException.class, () -> service.createVersion(morning.getShiftId(),
                new ShiftVersionCreateRequest(LocalTime.of(8, 0), LocalTime.NOON, effective, "Lý do")));
    }

    @Test
    void createVersionRejectsOverlapWithActiveOtherShiftButIgnoresIrrelevantVersions() {
        LocalDate effective = future(15);
        ShiftConfig morning = shift(ShiftConfigService.MORNING, "00:00", "08:00");
        ShiftConfig afternoon = shift(ShiftConfigService.AFTERNOON, "08:00", "16:00");
        ShiftConfig custom = shift("Ca khác", "08:00", "12:00");
        ShiftVersion nonFixed = version(custom, effective.minusDays(1), null,
                LocalTime.of(8, 0), LocalTime.NOON);
        ShiftVersion same = version(morning, effective.minusDays(1), null,
                LocalTime.MIDNIGHT, LocalTime.of(8, 0));
        ShiftVersion ended = version(afternoon, effective.minusDays(20), effective.minusDays(1),
                LocalTime.of(8, 0), LocalTime.of(16, 0));
        ShiftVersion overlapping = version(afternoon, effective.minusDays(1), null,
                LocalTime.of(8, 0), LocalTime.of(16, 0));
        when(shiftRepository.findById(morning.getShiftId())).thenReturn(Optional.of(morning));
        when(versionRepository.findAllByShiftForUpdate(morning.getShiftId())).thenReturn(List.of());
        when(versionRepository.findAll()).thenReturn(List.of(nonFixed, same, ended, overlapping));

        assertThrows(ConflictException.class, () -> service.createVersion(morning.getShiftId(),
                new ShiftVersionCreateRequest(LocalTime.of(7, 30), LocalTime.of(8, 30),
                        effective, "Trùng ca chiều")));
    }

    @Test
    void exceptionsValidateRangeAndPreviewImpactForWholeDayOrShift() {
        LocalDate date = future(5);
        ShiftConfig morning = shift(ShiftConfigService.MORNING, "00:00", "08:00");
        ClinicScheduleException exception = ClinicScheduleException.builder().exceptionId(UUID.randomUUID())
                .workDate(date).shift(morning).type(ClinicScheduleExceptionType.SHIFT_OFF).reason("Nghỉ").build();
        when(exceptionRepository.findAllByWorkDateBetweenOrderByWorkDateAsc(date, date))
                .thenReturn(List.of(exception));

        assertEquals(1, service.exceptions(date, date).size());
        assertThrows(BadRequestException.class, () -> service.exceptions(null, date));
        assertThrows(BadRequestException.class, () -> service.exceptions(date, null));
        assertThrows(BadRequestException.class, () -> service.exceptions(date, date.minusDays(1)));

        StaffSchedule schedule = staffSchedule(date, morning, ScheduleStatus.SCHEDULED);
        when(shiftRepository.findById(morning.getShiftId())).thenReturn(Optional.of(morning));
        when(scheduleRepository.findAllByWorkDateAndStatus(date, ScheduleStatus.SCHEDULED))
                .thenReturn(List.of(schedule));
        var shifted = service.previewExceptionImpact(new ClinicScheduleExceptionRequest(date,
                morning.getShiftId(), ClinicScheduleExceptionType.SHIFT_OFF, null, null, "Nghỉ"));
        var all = service.previewExceptionImpact(new ClinicScheduleExceptionRequest(date,
                null, ClinicScheduleExceptionType.CLOSED_DAY, null, null, "Nghỉ"));
        assertEquals(1, shifted.staffScheduleCount());
        assertEquals(1, all.staffScheduleCount());
    }

    @Test
    void createClosedDayDeletesExistingExceptionsAndNormalizesReason() {
        LocalDate date = future(8);
        ClinicScheduleException existing = ClinicScheduleException.builder().exceptionId(UUID.randomUUID())
                .workDate(date).type(ClinicScheduleExceptionType.SHIFT_OFF).build();
        when(exceptionRepository.findAllByWorkDateForUpdate(date)).thenReturn(List.of(existing));
        when(exceptionRepository.save(any())).thenAnswer(invocation -> {
            ClinicScheduleException saved = invocation.getArgument(0);
            saved.setExceptionId(UUID.randomUUID());
            return saved;
        });

        var response = service.createException(new ClinicScheduleExceptionRequest(date, null,
                ClinicScheduleExceptionType.CLOSED_DAY, null, null, "  Bảo   trì toàn viện "));

        assertEquals("Bảo trì toàn viện", response.reason());
        assertNull(response.shiftId());
        verify(exceptionRepository).delete(existing);
    }

    @Test
    void createExceptionRejectsInvalidShapesExistingClosedDayAndImpact() {
        LocalDate date = future(9);
        ShiftConfig morning = shift(ShiftConfigService.MORNING, "00:00", "08:00");
        when(shiftRepository.findById(morning.getShiftId())).thenReturn(Optional.of(morning));

        assertThrows(BadRequestException.class, () -> service.createException(new ClinicScheduleExceptionRequest(
                date, morning.getShiftId(), ClinicScheduleExceptionType.CLOSED_DAY, null, null, "Nghỉ")));
        assertThrows(BadRequestException.class, () -> service.createException(new ClinicScheduleExceptionRequest(
                date, null, ClinicScheduleExceptionType.SHIFT_OFF, null, null, "Nghỉ")));
        assertThrows(BadRequestException.class, () -> service.createException(new ClinicScheduleExceptionRequest(
                date, morning.getShiftId(), ClinicScheduleExceptionType.SHIFT_OFF,
                LocalTime.of(1, 0), null, "Nghỉ")));
        assertThrows(BadRequestException.class, () -> service.createException(new ClinicScheduleExceptionRequest(
                date, morning.getShiftId(), ClinicScheduleExceptionType.SPECIAL_HOURS,
                LocalTime.of(8, 0), LocalTime.of(8, 0), "Đổi giờ")));

        ClinicScheduleException closed = ClinicScheduleException.builder()
                .type(ClinicScheduleExceptionType.CLOSED_DAY).workDate(date).build();
        when(exceptionRepository.findAllByWorkDateForUpdate(date)).thenReturn(List.of(closed));
        assertThrows(ConflictException.class, () -> service.createException(new ClinicScheduleExceptionRequest(
                date, morning.getShiftId(), ClinicScheduleExceptionType.SHIFT_OFF, null, null, "Nghỉ")));

        when(exceptionRepository.findAllByWorkDateForUpdate(date)).thenReturn(List.of());
        when(scheduleRepository.findAllByWorkDateAndStatus(date, ScheduleStatus.SCHEDULED))
                .thenReturn(List.of(staffSchedule(date, morning, ScheduleStatus.SCHEDULED)));
        assertThrows(ConflictException.class, () -> service.createException(new ClinicScheduleExceptionRequest(
                date, morning.getShiftId(), ClinicScheduleExceptionType.SHIFT_OFF, null, null, "Nghỉ")));
    }

    @Test
    void specialHoursRejectOverlapAndSuccessReplacesSameShiftOnly() {
        LocalDate date = future(12);
        ShiftConfig morning = shift(ShiftConfigService.MORNING, "00:00", "08:00");
        ShiftConfig afternoon = shift(ShiftConfigService.AFTERNOON, "08:00", "16:00");
        ShiftConfig custom = shift("Khác", "08:00", "10:00");
        when(shiftRepository.findById(morning.getShiftId())).thenReturn(Optional.of(morning));
        when(exceptionRepository.findAllByWorkDateForUpdate(date)).thenReturn(List.of());
        when(shiftRepository.findAllByIsActiveTrueOrderByStartTimeAsc())
                .thenReturn(List.of(custom, morning, afternoon));
        when(resolver.resolve(afternoon, date)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                afternoon, null, LocalTime.of(8, 0), LocalTime.of(16, 0), ShiftTimeSource.NORMAL, null));

        assertThrows(ConflictException.class, () -> service.createException(new ClinicScheduleExceptionRequest(
                date, morning.getShiftId(), ClinicScheduleExceptionType.SPECIAL_HOURS,
                LocalTime.of(7, 0), LocalTime.of(9, 0), "Đổi giờ")));

        ClinicScheduleException sameShift = ClinicScheduleException.builder().exceptionId(UUID.randomUUID())
                .workDate(date).shift(morning).type(ClinicScheduleExceptionType.SHIFT_OFF).build();
        ClinicScheduleException noShift = ClinicScheduleException.builder().exceptionId(UUID.randomUUID())
                .workDate(date).shift(null).type(ClinicScheduleExceptionType.SHIFT_OFF).build();
        when(exceptionRepository.findAllByWorkDateForUpdate(date)).thenReturn(List.of(sameShift, noShift));
        when(resolver.resolve(afternoon, date)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                afternoon, null, LocalTime.of(8, 0), LocalTime.of(16, 0), ShiftTimeSource.NORMAL,
                org.example.doansummer2026.enums.ShiftUnavailableReason.SHIFT_OFF));
        when(exceptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createException(new ClinicScheduleExceptionRequest(
                date, morning.getShiftId(), ClinicScheduleExceptionType.SPECIAL_HOURS,
                LocalTime.of(6, 0), LocalTime.of(7, 0), " Mở sớm "));

        assertEquals(LocalTime.of(6, 0), response.specialStartTime());
        verify(exceptionRepository).delete(sameShift);
        verify(exceptionRepository, never()).delete(noShift);
    }

    @Test
    void reopenRejectsMissingAndPastButDeletesFutureException() {
        UUID missingId = UUID.randomUUID();
        UUID pastId = UUID.randomUUID();
        UUID futureId = UUID.randomUUID();
        ClinicScheduleException past = ClinicScheduleException.builder().exceptionId(pastId)
                .workDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))).build();
        ClinicScheduleException future = ClinicScheduleException.builder().exceptionId(futureId)
                .workDate(future(3)).build();
        when(exceptionRepository.findById(missingId)).thenReturn(Optional.empty());
        when(exceptionRepository.findById(pastId)).thenReturn(Optional.of(past));
        when(exceptionRepository.findById(futureId)).thenReturn(Optional.of(future));

        assertThrows(ResourceNotFoundException.class, () -> service.reopen(missingId));
        assertThrows(ConflictException.class, () -> service.reopen(pastId));
        service.reopen(futureId);

        verify(exceptionRepository).delete(future);
        verify(exceptionRepository, never()).delete(past);
    }

    private void authenticate(String username, boolean authenticated) {
        var token = mock(UsernamePasswordAuthenticationToken.class);
        when(token.isAuthenticated()).thenReturn(authenticated);
        when(token.getName()).thenReturn(username);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private LocalDate future(int days) {
        return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(days);
    }

    private ShiftConfig shift(String name, String start, String end) {
        return ShiftConfig.builder().shiftId(UUID.randomUUID()).name(name)
                .startTime(start).endTime(end).isActive(true).build();
    }

    private ShiftVersion version(ShiftConfig shift, LocalDate from, LocalDate to,
                                 LocalTime start, LocalTime end) {
        return ShiftVersion.builder().shiftVersionId(UUID.randomUUID()).shift(shift)
                .effectiveFrom(from).effectiveTo(to).startTime(start).endTime(end)
                .changeReason("Thay đổi").build();
    }

    private StaffSchedule staffSchedule(LocalDate date, ShiftConfig shift, ScheduleStatus status) {
        return StaffSchedule.builder().scheduleId(UUID.randomUUID()).workDate(date)
                .shift(shift).status(status).build();
    }

    private Appointment appointment(LocalDateTime at, ShiftConfig shift, String legacyName,
                                    MedicalService medicalService) {
        ShiftVersion version = shift == null ? null : version(shift, at.toLocalDate().minusDays(1),
                null, LocalTime.MIDNIGHT, LocalTime.of(8, 0));
        return Appointment.builder().appointmentId(UUID.randomUUID()).scheduledAt(at)
                .shiftVersion(version).shiftName(legacyName)
                .services(new HashSet<>(List.of(medicalService))).build();
    }
}
