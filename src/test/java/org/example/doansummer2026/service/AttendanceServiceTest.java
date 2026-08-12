package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.attendance.AdjustmentRequest;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private StaffScheduleRepository schedules;

    @Mock
    private StaffAttendanceRepository attendances;

    @Mock
    private AttendanceQrTokenRepository tokens;

    @Mock
    private AttendanceAdjustmentRepository adjustments;

    @Mock
    private StaffInfoRepository staffRepo;

    @InjectMocks
    private AttendanceService attendanceService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(
                attendanceService,
                "qrSeconds",
                30L
        );

        ReflectionTestUtils.setField(
                attendanceService,
                "allowedIpPrefix",
                ""
        );
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private StaffInfo staff(UUID id, String code, String fullName) {

        Profile profile = fullName == null
                ? null
                : Profile.builder()
                .profileId(UUID.randomUUID())
                .fullName(fullName)
                .build();

        return StaffInfo.builder()
                .staffId(id)
                .staffCode(code)
                .profile(profile)
                .build();
    }

    private ShiftConfig shift(
            String name,
            String start,
            String end
    ) {
        return ShiftConfig.builder()
                .shiftId(UUID.randomUUID())
                .name(name)
                .startTime(start)
                .endTime(end)
                .build();
    }

    private StaffSchedule schedule(
            UUID id,
            StaffInfo staff,
            LocalDate date,
            ShiftConfig shift,
            ScheduleStatus status
    ) {
        return StaffSchedule.builder()
                .scheduleId(id)
                .staff(staff)
                .workDate(date)
                .shift(shift)
                .status(status)
                .build();
    }

    private StaffInfo testStaff(UUID staffId) {
        Profile profile = Profile.builder()
                .profileId(UUID.randomUUID())
                .fullName("Nguyen Van Test")
                .phone("0912345678")
                .build();

        return StaffInfo.builder()
                .staffId(staffId)
                .staffCode("STF-TEST")
                .profile(profile)
                .systemRole(SystemRole.NURSE)
                .build();
    }
    // =========================================================
    // ISSUE TOKEN
    // =========================================================

    @Test
    void issueToken_ShouldDeactivateOldTokensAndCreateNewToken() {

        UUID managerId = UUID.randomUUID();

        StaffInfo manager =
                staff(managerId, "NV001", "Manager");

        when(staffRepo.findById(managerId))
                .thenReturn(Optional.of(manager));

        when(tokens.save(any(AttendanceQrToken.class)))
                .thenAnswer(i -> i.getArgument(0));

        var result =
                attendanceService.issueToken(managerId);

        assertNotNull(result);
        assertNotNull(result.value());
        assertTrue(result.value().startsWith("ATTENDANCE:"));
        assertNotNull(result.expiresAt());
        assertEquals(30L, result.expiresInSeconds());

        verify(tokens).deactivateAll();

        verify(tokens).save(argThat(token ->
                token.getTokenHash() != null
                        && token.getTokenHash().length() == 64
                        && Boolean.TRUE.equals(token.getActive())
                        && token.getCreatedBy() == manager
                        && token.getExpiresAt() != null
        ));
    }


    @Test
    void issueToken_ShouldThrow_WhenManagerMissing() {

        UUID managerId = UUID.randomUUID();

        when(staffRepo.findById(managerId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> attendanceService.issueToken(managerId)
        );

        verify(tokens).deactivateAll();
        verify(tokens, never()).save(any());
    }


    // =========================================================
    // SCAN - INVALID QR
    // =========================================================

    @Test
    void scan_ShouldReject_WhenQrIsNull() {

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.scan(
                        UUID.randomUUID(),
                        null,
                        null,
                        null
                )
        );

        verifyNoInteractions(tokens);
    }


    @Test
    void scan_ShouldReject_WhenQrHasWrongPrefix() {

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.scan(
                        UUID.randomUUID(),
                        "WRONG:abc",
                        null,
                        null
                )
        );

        verifyNoInteractions(tokens);
    }


    // =========================================================
    // SCAN - IP VALIDATION
    // =========================================================

    @Test
    void scan_ShouldReject_WhenIpIsNullAndPrefixRequired() {

        ReflectionTestUtils.setField(
                attendanceService,
                "allowedIpPrefix",
                "192.168."
        );

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.scan(
                        UUID.randomUUID(),
                        "ATTENDANCE:abc",
                        null,
                        "Chrome"
                )
        );

        verifyNoInteractions(tokens);
    }


    @Test
    void scan_ShouldReject_WhenIpDoesNotMatchPrefix() {

        ReflectionTestUtils.setField(
                attendanceService,
                "allowedIpPrefix",
                "192.168."
        );

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.scan(
                        UUID.randomUUID(),
                        "ATTENDANCE:abc",
                        "10.0.0.1",
                        "Chrome"
                )
        );
    }


    @Test
    void scan_ShouldAcceptIp_WhenPrefixMatchesAfterTrim() {

        ReflectionTestUtils.setField(
                attendanceService,
                "allowedIpPrefix",
                " 192.168. "
        );

        UUID staffId = UUID.randomUUID();

        AttendanceQrToken token =
                AttendanceQrToken.builder()
                        .active(true)
                        .expiresAt(
                                LocalDateTime.now().plusMinutes(1)
                        )
                        .build();

        when(
                tokens.findByTokenHashAndActiveTrue(anyString())
        ).thenReturn(Optional.of(token));

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        eq(staffId),
                        any(LocalDate.class)
                )
        ).thenReturn(List.of());

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.scan(
                        staffId,
                        "ATTENDANCE:abc",
                        "192.168.1.10",
                        "Chrome"
                )
        );
    }


    // =========================================================
    // SCAN - TOKEN
    // =========================================================

    @Test
    void scan_ShouldReject_WhenTokenNotFound() {

        when(
                tokens.findByTokenHashAndActiveTrue(anyString())
        ).thenReturn(Optional.empty());

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.scan(
                        UUID.randomUUID(),
                        "ATTENDANCE:abc",
                        null,
                        null
                )
        );
    }


    @Test
    void scan_ShouldRejectAndDeactivate_WhenTokenExpired() {

        AttendanceQrToken token =
                AttendanceQrToken.builder()
                        .active(true)
                        .expiresAt(
                                LocalDateTime.now().minusSeconds(1)
                        )
                        .build();

        when(
                tokens.findByTokenHashAndActiveTrue(anyString())
        ).thenReturn(Optional.of(token));

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.scan(
                        UUID.randomUUID(),
                        "ATTENDANCE:abc",
                        null,
                        null
                )
        );

        assertFalse(token.getActive());
    }


    // =========================================================
    // CURRENT SCHEDULE
    // =========================================================

    @Test
    void scan_ShouldReject_WhenNoMatchingSchedule() {

        UUID staffId = UUID.randomUUID();

        AttendanceQrToken token =
                AttendanceQrToken.builder()
                        .active(true)
                        .expiresAt(
                                LocalDateTime.now().plusMinutes(1)
                        )
                        .build();

        when(
                tokens.findByTokenHashAndActiveTrue(anyString())
        ).thenReturn(Optional.of(token));

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        eq(staffId),
                        any(LocalDate.class)
                )
        ).thenReturn(List.of());

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.scan(
                        staffId,
                        "ATTENDANCE:abc",
                        null,
                        null
                )
        );
    }


    @Test
    void scan_ShouldIgnoreSchedule_WhenNotScheduledStatus() {

        UUID staffId = UUID.randomUUID();

        StaffInfo staff =
                staff(staffId, "NV01", "A");

        ShiftConfig shift =
                shift(
                        "Ca",
                        "00:00",
                        "23:59"
                );

        StaffSchedule completed =
                schedule(
                        UUID.randomUUID(),
                        staff,
                        LocalDate.now(),
                        shift,
                        ScheduleStatus.COMPLETED
                );

        AttendanceQrToken token =
                AttendanceQrToken.builder()
                        .active(true)
                        .expiresAt(
                                LocalDateTime.now().plusMinutes(1)
                        )
                        .build();

        when(
                tokens.findByTokenHashAndActiveTrue(anyString())
        ).thenReturn(Optional.of(token));

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        eq(staffId),
                        any(LocalDate.class)
                )
        ).thenReturn(List.of(completed));

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.scan(
                        staffId,
                        "ATTENDANCE:abc",
                        null,
                        null
                )
        );
    }


    @Test
    void scan_ShouldIgnoreSchedule_WhenTooEarly() {

        UUID staffId = UUID.randomUUID();

        StaffInfo staff =
                staff(staffId, "NV01", "A");

        LocalTimeHolder future =
                new LocalTimeHolder(
                        LocalDateTime.now().plusHours(2)
                );

        ShiftConfig shift =
                shift(
                        "Future",
                        future.time(),
                        "23:59"
                );

        StaffSchedule schedule =
                schedule(
                        UUID.randomUUID(),
                        staff,
                        LocalDate.now(),
                        shift,
                        ScheduleStatus.SCHEDULED
                );

        AttendanceQrToken token =
                AttendanceQrToken.builder()
                        .active(true)
                        .expiresAt(
                                LocalDateTime.now().plusMinutes(1)
                        )
                        .build();

        when(tokens.findByTokenHashAndActiveTrue(anyString()))
                .thenReturn(Optional.of(token));

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        eq(staffId),
                        any(LocalDate.class)
                )
        ).thenReturn(List.of(schedule));

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.scan(
                        staffId,
                        "ATTENDANCE:abc",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // SCAN - FIRST CHECK IN
    // =========================================================

    @Test
    void scan_ShouldCreateAttendance_WhenFirstScan() {

        UUID staffId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        StaffInfo staff =
                staff(staffId, "NV01", "Nhan vien");

        ShiftConfig shift =
                shift(
                        "Ca ca ngay",
                        "00:00",
                        "23:59"
                );

        StaffSchedule schedule =
                schedule(
                        scheduleId,
                        staff,
                        LocalDate.now(),
                        shift,
                        ScheduleStatus.SCHEDULED
                );

        AttendanceQrToken token =
                AttendanceQrToken.builder()
                        .active(true)
                        .expiresAt(
                                LocalDateTime.now().plusMinutes(2)
                        )
                        .build();

        when(tokens.findByTokenHashAndActiveTrue(anyString()))
                .thenReturn(Optional.of(token));

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        eq(staffId),
                        any(LocalDate.class)
                )
        ).thenReturn(List.of(schedule));

        when(
                attendances.findBySchedule_ScheduleId(scheduleId)
        ).thenReturn(Optional.empty());

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(attendances.save(any(StaffAttendance.class)))
                .thenAnswer(i -> {
                    StaffAttendance a =
                            i.getArgument(0);

                    a.setAttendanceId(UUID.randomUUID());

                    return a;
                });

        var result =
                attendanceService.scan(
                        staffId,
                        "ATTENDANCE:abc",
                        "192.168.1.1",
                        "Chrome"
                );

        assertNotNull(result);

        verify(attendances).save(argThat(a ->
                a.getSchedule() == schedule
                        && a.getStaff() == staff
                        && a.getCheckInAt() != null
                        && "192.168.1.1".equals(a.getCheckInIp())
                        && "Chrome".equals(a.getDeviceInfo())
                        && (
                        a.getStatus() == AttendanceStatus.ON_TIME
                                || a.getStatus() == AttendanceStatus.LATE
                )
        ));
    }


    // =========================================================
    // SCAN - AGENT NULL
    // =========================================================

    @Test
    void scan_ShouldStoreNullDeviceInfo_WhenAgentNull() {

        UUID staffId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        StaffInfo staff =
                staff(staffId, "NV01", "A");

        StaffSchedule schedule =
                schedule(
                        scheduleId,
                        staff,
                        LocalDate.now(),
                        shift(
                                "Ca",
                                "00:00",
                                "23:59"
                        ),
                        ScheduleStatus.SCHEDULED
                );

        AttendanceQrToken token =
                AttendanceQrToken.builder()
                        .active(true)
                        .expiresAt(
                                LocalDateTime.now().plusMinutes(1)
                        )
                        .build();

        when(tokens.findByTokenHashAndActiveTrue(anyString()))
                .thenReturn(Optional.of(token));

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        eq(staffId),
                        any(LocalDate.class)
                )
        ).thenReturn(List.of(schedule));

        when(
                attendances.findBySchedule_ScheduleId(scheduleId)
        ).thenReturn(Optional.empty());

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(attendances.save(any()))
                .thenAnswer(i -> {
                    StaffAttendance a = i.getArgument(0);
                    a.setAttendanceId(UUID.randomUUID());
                    return a;
                });

        attendanceService.scan(
                staffId,
                "ATTENDANCE:abc",
                null,
                null
        );

        verify(attendances).save(argThat(a ->
                a.getDeviceInfo() == null
        ));
    }


    // =========================================================
    // SCAN - LONG AGENT
    // =========================================================

    @Test
    void scan_ShouldLimitDeviceInfoTo500Characters() {

        UUID staffId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        StaffInfo staff =
                staff(staffId, "NV01", "A");

        StaffSchedule schedule =
                schedule(
                        scheduleId,
                        staff,
                        LocalDate.now(),
                        shift(
                                "Ca",
                                "00:00",
                                "23:59"
                        ),
                        ScheduleStatus.SCHEDULED
                );

        AttendanceQrToken token =
                AttendanceQrToken.builder()
                        .active(true)
                        .expiresAt(
                                LocalDateTime.now().plusMinutes(1)
                        )
                        .build();

        when(tokens.findByTokenHashAndActiveTrue(anyString()))
                .thenReturn(Optional.of(token));

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        eq(staffId),
                        any(LocalDate.class)
                )
        ).thenReturn(List.of(schedule));

        when(
                attendances.findBySchedule_ScheduleId(scheduleId)
        ).thenReturn(Optional.empty());

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(attendances.save(any()))
                .thenAnswer(i -> {
                    StaffAttendance a = i.getArgument(0);
                    a.setAttendanceId(UUID.randomUUID());
                    return a;
                });

        String agent =
                "A".repeat(700);

        attendanceService.scan(
                staffId,
                "ATTENDANCE:abc",
                null,
                agent
        );

        verify(attendances).save(argThat(a ->
                a.getDeviceInfo() != null
                        && a.getDeviceInfo().length() == 500
        ));
    }


    // =========================================================
    // SCAN - EXISTING ATTENDANCE WITHOUT CHECK-IN
    // =========================================================

    @Test
    void scan_ShouldFillCheckIn_WhenAttendanceExistsWithoutCheckIn() {

        UUID staffId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        StaffSchedule schedule =
                schedule(
                        scheduleId,
                        staff(staffId, "NV01", "A"),
                        LocalDate.now(),
                        shift(
                                "Ca",
                                "00:00",
                                "23:59"
                        ),
                        ScheduleStatus.SCHEDULED
                );

        StaffAttendance attendance =
                StaffAttendance.builder()
                        .attendanceId(UUID.randomUUID())
                        .schedule(schedule)
                        .staff(schedule.getStaff())
                        .build();

        AttendanceQrToken token =
                AttendanceQrToken.builder()
                        .active(true)
                        .expiresAt(
                                LocalDateTime.now().plusMinutes(1)
                        )
                        .build();

        when(tokens.findByTokenHashAndActiveTrue(anyString()))
                .thenReturn(Optional.of(token));

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        eq(staffId),
                        any(LocalDate.class)
                )
        ).thenReturn(List.of(schedule));

        when(
                attendances.findBySchedule_ScheduleId(scheduleId)
        ).thenReturn(Optional.of(attendance));

        when(attendances.save(attendance))
                .thenReturn(attendance);

        attendanceService.scan(
                staffId,
                "ATTENDANCE:abc",
                "1.1.1.1",
                "Edge"
        );

        assertNotNull(attendance.getCheckInAt());
        assertEquals(
                "1.1.1.1",
                attendance.getCheckInIp()
        );
        assertEquals(
                "Edge",
                attendance.getDeviceInfo()
        );
    }


    // =========================================================
    // SCAN - CHECK OUT
    // =========================================================

    @Test
    void scan_ShouldCheckOut_WhenAlreadyCheckedIn() {

        UUID staffId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        StaffSchedule schedule =
                schedule(
                        scheduleId,
                        staff(staffId, "NV01", "A"),
                        LocalDate.now(),
                        shift(
                                "Ca",
                                "00:00",
                                "23:59"
                        ),
                        ScheduleStatus.SCHEDULED
                );

        StaffAttendance attendance =
                StaffAttendance.builder()
                        .attendanceId(UUID.randomUUID())
                        .schedule(schedule)
                        .staff(schedule.getStaff())
                        .checkInAt(LocalDateTime.now().minusHours(1))
                        .status(AttendanceStatus.WORKING)
                        .build();

        AttendanceQrToken token =
                AttendanceQrToken.builder()
                        .active(true)
                        .expiresAt(
                                LocalDateTime.now().plusMinutes(1)
                        )
                        .build();

        when(tokens.findByTokenHashAndActiveTrue(anyString()))
                .thenReturn(Optional.of(token));

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        eq(staffId),
                        any(LocalDate.class)
                )
        ).thenReturn(List.of(schedule));

        when(
                attendances.findBySchedule_ScheduleId(scheduleId)
        ).thenReturn(Optional.of(attendance));

        when(attendances.save(attendance))
                .thenReturn(attendance);

        attendanceService.scan(
                staffId,
                "ATTENDANCE:abc",
                "1.1.1.1",
                "Chrome"
        );

        assertNotNull(
                attendance.getCheckOutAt()
        );

        assertEquals(
                "1.1.1.1",
                attendance.getCheckOutIp()
        );

        assertEquals(
                ScheduleStatus.COMPLETED,
                schedule.getStatus()
        );

        assertTrue(
                attendance.getStatus() == AttendanceStatus.LEFT_EARLY
                        || attendance.getStatus() == AttendanceStatus.COMPLETED
        );
    }


    // =========================================================
    // SCAN - ALREADY COMPLETE
    // =========================================================

    @Test
    void scan_ShouldReject_WhenAlreadyCheckedInAndOut() {

        UUID staffId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        StaffSchedule schedule =
                schedule(
                        scheduleId,
                        staff(staffId, "NV01", "A"),
                        LocalDate.now(),
                        shift(
                                "Ca",
                                "00:00",
                                "23:59"
                        ),
                        ScheduleStatus.SCHEDULED
                );

        StaffAttendance attendance =
                StaffAttendance.builder()
                        .attendanceId(UUID.randomUUID())
                        .schedule(schedule)
                        .checkInAt(
                                LocalDateTime.now().minusHours(1)
                        )
                        .checkOutAt(
                                LocalDateTime.now()
                        )
                        .build();

        AttendanceQrToken token =
                AttendanceQrToken.builder()
                        .active(true)
                        .expiresAt(
                                LocalDateTime.now().plusMinutes(1)
                        )
                        .build();

        when(tokens.findByTokenHashAndActiveTrue(anyString()))
                .thenReturn(Optional.of(token));

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        eq(staffId),
                        any(LocalDate.class)
                )
        ).thenReturn(List.of(schedule));

        when(
                attendances.findBySchedule_ScheduleId(scheduleId)
        ).thenReturn(Optional.of(attendance));

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.scan(
                        staffId,
                        "ATTENDANCE:abc",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // TODAY
    // =========================================================

    @Test
    void today_ShouldReturnEmpty_WhenNoSchedules() {

        UUID staffId = UUID.randomUUID();

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        staffId,
                        LocalDate.now()
                )
        ).thenReturn(List.of());

        assertTrue(
                attendanceService.today(staffId)
                        .isEmpty()
        );
    }


    @Test
    void today_ShouldSortByShiftStart() {

        UUID staffId = UUID.randomUUID();

        StaffInfo staff =
                staff(staffId, "NV01", "A");

        StaffSchedule afternoon =
                schedule(
                        UUID.randomUUID(),
                        staff,
                        LocalDate.now(),
                        shift(
                                "Chieu",
                                "13:00",
                                "17:00"
                        ),
                        ScheduleStatus.SCHEDULED
                );

        StaffSchedule morning =
                schedule(
                        UUID.randomUUID(),
                        staff,
                        LocalDate.now(),
                        shift(
                                "Sang",
                                "08:00",
                                "12:00"
                        ),
                        ScheduleStatus.SCHEDULED
                );

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        staffId,
                        LocalDate.now()
                )
        ).thenReturn(
                List.of(
                        afternoon,
                        morning
                )
        );

        when(
                attendances.findBySchedule_ScheduleId(any())
        ).thenReturn(Optional.empty());

        var result =
                attendanceService.today(staffId);

        assertEquals(2, result.size());
    }


    @Test
    void today_ShouldUseAttendanceStatus_WhenAttendanceExists() {

        UUID staffId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        StaffSchedule schedule =
                schedule(
                        scheduleId,
                        staff(staffId, "NV01", "A"),
                        LocalDate.now(),
                        shift(
                                "Ca",
                                "08:00",
                                "17:00"
                        ),
                        ScheduleStatus.SCHEDULED
                );

        StaffAttendance attendance =
                StaffAttendance.builder()
                        .attendanceId(UUID.randomUUID())
                        .status(AttendanceStatus.COMPLETED)
                        .checkInAt(
                                LocalDateTime.now().minusHours(8)
                        )
                        .checkOutAt(
                                LocalDateTime.now()
                        )
                        .build();

        when(
                schedules.findAllByStaff_StaffIdAndWorkDate(
                        staffId,
                        LocalDate.now()
                )
        ).thenReturn(List.of(schedule));

        when(
                attendances.findBySchedule_ScheduleId(scheduleId)
        ).thenReturn(Optional.of(attendance));

        var result =
                attendanceService.today(staffId);

        assertEquals(1, result.size());
    }


    // =========================================================
    // MANAGE
    // =========================================================

    @Test
    void manage_ShouldReturnEmpty_WhenNoSchedules() {

        LocalDate date = LocalDate.now();

        when(
                schedules.findAllByWorkDateBetween(
                        date,
                        date
                )
        ).thenReturn(List.of());

        assertTrue(
                attendanceService.manage(date)
                        .isEmpty()
        );
    }


    @Test
    void manage_ShouldUseFullName_WhenProfileAvailable() {

        LocalDate date =
                LocalDate.now();

        StaffInfo staff =
                staff(
                        UUID.randomUUID(),
                        "NV01",
                        "Nguyen Van A"
                );

        StaffSchedule schedule =
                schedule(
                        UUID.randomUUID(),
                        staff,
                        date,
                        shift(
                                "Ca",
                                "08:00",
                                "17:00"
                        ),
                        ScheduleStatus.SCHEDULED
                );

        when(
                schedules.findAllByWorkDateBetween(
                        date,
                        date
                )
        ).thenReturn(List.of(schedule));

        when(
                attendances.findBySchedule_ScheduleId(
                        schedule.getScheduleId()
                )
        ).thenReturn(Optional.empty());

        var result =
                attendanceService.manage(date);

        assertEquals(1, result.size());
    }


    @Test
    void manage_ShouldFallbackToStaffCode_WhenProfileMissing() {

        LocalDate date =
                LocalDate.now();

        StaffInfo staff =
                staff(
                        UUID.randomUUID(),
                        "NV99",
                        null
                );

        StaffSchedule schedule =
                schedule(
                        UUID.randomUUID(),
                        staff,
                        date,
                        shift(
                                "Ca",
                                "08:00",
                                "17:00"
                        ),
                        ScheduleStatus.SCHEDULED
                );

        when(
                schedules.findAllByWorkDateBetween(
                        date,
                        date
                )
        ).thenReturn(List.of(schedule));

        when(
                attendances.findBySchedule_ScheduleId(
                        schedule.getScheduleId()
                )
        ).thenReturn(Optional.empty());

        assertEquals(
                1,
                attendanceService.manage(date)
                        .size()
        );
    }


    // =========================================================
    // REQUEST ADJUSTMENT
    // =========================================================

    @Test
    void request_ShouldThrow_WhenScheduleMissing() {

        UUID scheduleId =
                UUID.randomUUID();

        AdjustmentRequest req =
                mock(AdjustmentRequest.class);

        when(req.scheduleId())
                .thenReturn(scheduleId);

        when(schedules.findById(scheduleId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> attendanceService.request(
                        UUID.randomUUID(),
                        req
                )
        );
    }


    @Test
    void request_ShouldReject_WhenScheduleBelongsToOtherStaff() {

        UUID scheduleId = UUID.randomUUID();

        UUID ownerId = UUID.randomUUID();

        StaffSchedule schedule =
                schedule(
                        scheduleId,
                        staff(ownerId, "OWNER", "Owner"),
                        LocalDate.now(),
                        shift("Ca", "08:00", "17:00"),
                        ScheduleStatus.SCHEDULED
                );

        AdjustmentRequest req =
                mock(AdjustmentRequest.class);

        when(req.scheduleId())
                .thenReturn(scheduleId);

        when(schedules.findById(scheduleId))
                .thenReturn(Optional.of(schedule));

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.request(
                        UUID.randomUUID(),
                        req
                )
        );
    }


    @Test
    void request_ShouldReject_WhenNoRequestedTimes() {

        UUID staffId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        StaffSchedule schedule =
                schedule(
                        scheduleId,
                        staff(staffId, "NV01", "A"),
                        LocalDate.now(),
                        shift("Ca", "08:00", "17:00"),
                        ScheduleStatus.SCHEDULED
                );

        AdjustmentRequest req =
                mock(AdjustmentRequest.class);

        when(req.scheduleId())
                .thenReturn(scheduleId);

        when(schedules.findById(scheduleId))
                .thenReturn(Optional.of(schedule));

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.request(
                        staffId,
                        req
                )
        );
    }


    @Test
    void request_ShouldCreateAttendance_WhenMissing() {

        UUID staffId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        StaffInfo staff =
                staff(staffId, "NV01", "A");

        StaffSchedule schedule =
                schedule(
                        scheduleId,
                        staff,
                        LocalDate.now(),
                        shift("Ca", "08:00", "17:00"),
                        ScheduleStatus.SCHEDULED
                );

        AdjustmentRequest req =
                mock(AdjustmentRequest.class);

        LocalDateTime requestedIn =
                LocalDateTime.now().minusHours(1);

        when(req.scheduleId())
                .thenReturn(scheduleId);

        when(req.requestedCheckIn())
                .thenReturn(requestedIn);

        when(req.reason())
                .thenReturn("  quen check in  ");

        when(schedules.findById(scheduleId))
                .thenReturn(Optional.of(schedule));

        when(
                attendances.findBySchedule_ScheduleId(scheduleId)
        ).thenReturn(Optional.empty());

        when(attendances.save(any(StaffAttendance.class)))
                .thenAnswer(i -> {
                    StaffAttendance a =
                            i.getArgument(0);

                    a.setAttendanceId(UUID.randomUUID());

                    return a;
                });

        when(adjustments.save(any(AttendanceAdjustment.class)))
                .thenAnswer(i -> {
                    AttendanceAdjustment x =
                            i.getArgument(0);

                    x.setAdjustmentId(UUID.randomUUID());

                    return x;
                });

        var result =
                attendanceService.request(
                        staffId,
                        req
                );

        assertNotNull(result);

        verify(attendances).save(argThat(a ->
                a.getSchedule() == schedule
                        && a.getStaff() == staff
                        && a.getStatus()
                        == AttendanceStatus.ADJUSTMENT_PENDING
        ));

        verify(adjustments).save(argThat(x ->
                "quen check in".equals(x.getReason())
                        && requestedIn.equals(
                        x.getRequestedCheckIn()
                )
                        && x.getStatus()
                        == AttendanceAdjustmentStatus.PENDING
        ));
    }


    @Test
    void request_ShouldReuseExistingAttendance() {

        UUID staffId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        StaffSchedule schedule =
                schedule(
                        scheduleId,
                        staff(staffId, "NV01", "A"),
                        LocalDate.now(),
                        shift("Ca", "08:00", "17:00"),
                        ScheduleStatus.SCHEDULED
                );

        StaffAttendance attendance =
                StaffAttendance.builder()
                        .attendanceId(UUID.randomUUID())
                        .schedule(schedule)
                        .staff(schedule.getStaff())
                        .status(AttendanceStatus.WORKING)
                        .build();

        AdjustmentRequest req =
                mock(AdjustmentRequest.class);

        when(req.scheduleId())
                .thenReturn(scheduleId);

        when(req.requestedCheckOut())
                .thenReturn(LocalDateTime.now());

        when(req.reason())
                .thenReturn("reason");

        when(schedules.findById(scheduleId))
                .thenReturn(Optional.of(schedule));

        when(
                attendances.findBySchedule_ScheduleId(scheduleId)
        ).thenReturn(Optional.of(attendance));

        when(adjustments.save(any()))
                .thenAnswer(i -> {
                    AttendanceAdjustment x =
                            i.getArgument(0);

                    x.setAdjustmentId(UUID.randomUUID());

                    return x;
                });

        attendanceService.request(
                staffId,
                req
        );

        assertEquals(
                AttendanceStatus.ADJUSTMENT_PENDING,
                attendance.getStatus()
        );

        verify(attendances, never())
                .save(attendance);
    }


    // =========================================================
    // PENDING
    // =========================================================

    @Test
    void pending_ShouldReturnEmpty_WhenNoPendingAdjustments() {

        when(
                adjustments
                        .findAllByStatusOrderByCreatedAtAsc(
                                AttendanceAdjustmentStatus.PENDING
                        )
        ).thenReturn(List.of());

        assertTrue(
                attendanceService.pending()
                        .isEmpty()
        );
    }


    // =========================================================
    // REVIEW
    // =========================================================

    @Test
    void review_ShouldThrow_WhenAdjustmentMissing() {

        UUID id = UUID.randomUUID();

        when(adjustments.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> attendanceService.review(
                        id,
                        UUID.randomUUID(),
                        true,
                        null
                )
        );
    }


    @Test
    void review_ShouldReject_WhenAlreadyProcessed() {

        UUID id = UUID.randomUUID();

        AttendanceAdjustment adjustment =
                AttendanceAdjustment.builder()
                        .status(
                                AttendanceAdjustmentStatus.APPROVED
                        )
                        .build();

        when(adjustments.findById(id))
                .thenReturn(Optional.of(adjustment));

        assertThrows(
                BadRequestException.class,
                () -> attendanceService.review(
                        id,
                        UUID.randomUUID(),
                        true,
                        null
                )
        );
    }


    @Test
    void review_ShouldApproveAndApplyRequestedTimes() {

        UUID id = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        StaffInfo employee = testStaff(employeeId);
        StaffInfo manager = testStaff(managerId);

        LocalDateTime checkIn = LocalDateTime.now().minusHours(8);
        LocalDateTime checkOut = LocalDateTime.now();

        StaffAttendance attendance =
                StaffAttendance.builder()
                        .attendanceId(UUID.randomUUID())
                        .staff(employee)
                        .build();

        AttendanceAdjustment adjustment =
                AttendanceAdjustment.builder()
                        .adjustmentId(id)
                        .attendance(attendance)
                        .requestedCheckIn(checkIn)
                        .requestedCheckOut(checkOut)
                        .status(AttendanceAdjustmentStatus.PENDING)
                        .build();

        when(adjustments.findById(id))
                .thenReturn(Optional.of(adjustment));

        when(staffRepo.findById(managerId))
                .thenReturn(Optional.of(manager));

        when(adjustments.save(adjustment))
                .thenReturn(adjustment);

        var result = attendanceService.review(
                id,
                managerId,
                true,
                "Approved"
        );

        assertNotNull(result);

        assertEquals(
                AttendanceAdjustmentStatus.APPROVED,
                adjustment.getStatus()
        );

        assertEquals(checkIn, attendance.getCheckInAt());
        assertEquals(checkOut, attendance.getCheckOutAt());

        assertEquals(
                AttendanceStatus.COMPLETED,
                attendance.getStatus()
        );

        assertSame(manager, adjustment.getReviewedBy());
        assertNotNull(adjustment.getReviewedAt());
    }


    @Test
    void review_ShouldRejectWithoutApplyingTimes() {

        UUID id = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        StaffInfo employee = testStaff(UUID.randomUUID());
        StaffInfo manager = testStaff(managerId);

        StaffAttendance attendance =
                StaffAttendance.builder()
                        .attendanceId(UUID.randomUUID())
                        .staff(employee)
                        .checkInAt(LocalDateTime.now().minusHours(1))
                        .build();

        AttendanceAdjustment adjustment =
                AttendanceAdjustment.builder()
                        .adjustmentId(id)
                        .attendance(attendance)
                        .requestedCheckOut(LocalDateTime.now())
                        .status(AttendanceAdjustmentStatus.PENDING)
                        .build();

        when(adjustments.findById(id))
                .thenReturn(Optional.of(adjustment));

        when(staffRepo.findById(managerId))
                .thenReturn(Optional.of(manager));

        when(adjustments.save(adjustment))
                .thenReturn(adjustment);

        attendanceService.review(
                id,
                managerId,
                false,
                "Rejected"
        );

        assertEquals(
                AttendanceAdjustmentStatus.REJECTED,
                adjustment.getStatus()
        );

        assertNull(attendance.getCheckOutAt());

        assertEquals(
                AttendanceStatus.WORKING,
                attendance.getStatus()
        );
    }


    // =========================================================
    // REVIEW - RESULT ABSENT
    // =========================================================

    @Test
    void review_ShouldSetAbsent_WhenNoCheckIn() {

        UUID id = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        StaffInfo employee = testStaff(UUID.randomUUID());
        StaffInfo manager = testStaff(managerId);

        StaffAttendance attendance =
                StaffAttendance.builder()
                        .attendanceId(UUID.randomUUID())
                        .staff(employee)
                        .build();

        AttendanceAdjustment adjustment =
                AttendanceAdjustment.builder()
                        .adjustmentId(id)
                        .attendance(attendance)
                        .status(AttendanceAdjustmentStatus.PENDING)
                        .build();

        when(adjustments.findById(id))
                .thenReturn(Optional.of(adjustment));

        when(staffRepo.findById(managerId))
                .thenReturn(Optional.of(manager));

        when(adjustments.save(adjustment))
                .thenReturn(adjustment);

        attendanceService.review(
                id,
                managerId,
                false,
                null
        );

        assertEquals(
                AttendanceStatus.ABSENT,
                attendance.getStatus()
        );

        assertEquals(
                AttendanceAdjustmentStatus.REJECTED,
                adjustment.getStatus()
        );
    }


    // =========================================================
    // REVIEW - RESULT WORKING
    // =========================================================

    @Test
    void review_ShouldSetWorking_WhenOnlyCheckInExists() {

        UUID id = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        Profile employeeProfile = Profile.builder()
                .profileId(UUID.randomUUID())
                .fullName("Nhan vien test")
                .build();

        StaffInfo employee = StaffInfo.builder()
                .staffId(employeeId)
                .staffCode("STF-EMP")
                .profile(employeeProfile)
                .systemRole(SystemRole.NURSE)
                .build();

        Profile managerProfile = Profile.builder()
                .profileId(UUID.randomUUID())
                .fullName("Quan ly")
                .build();

        StaffInfo manager = StaffInfo.builder()
                .staffId(managerId)
                .staffCode("STF-MANAGER")
                .profile(managerProfile)
                .systemRole(SystemRole.CLINIC_MANAGER)
                .build();

        StaffAttendance attendance =
                StaffAttendance.builder()
                        .attendanceId(UUID.randomUUID())
                        .staff(employee) // QUAN TRỌNG
                        .checkInAt(LocalDateTime.now().minusHours(1))
                        .build();

        AttendanceAdjustment adjustment =
                AttendanceAdjustment.builder()
                        .adjustmentId(id)
                        .attendance(attendance)
                        .reason("Dieu chinh")
                        .status(AttendanceAdjustmentStatus.PENDING)
                        .build();

        when(adjustments.findById(id))
                .thenReturn(Optional.of(adjustment));

        when(staffRepo.findById(managerId))
                .thenReturn(Optional.of(manager));

        when(adjustments.save(adjustment))
                .thenReturn(adjustment);

        var result = attendanceService.review(
                id,
                managerId,
                false,
                null
        );

        assertNotNull(result);

        assertEquals(
                AttendanceStatus.WORKING,
                attendance.getStatus()
        );

        assertEquals(
                AttendanceAdjustmentStatus.REJECTED,
                adjustment.getStatus()
        );

        assertEquals(
                "Nhan vien test",
                result.staffName()
        );
    }


    // =========================================================
    // STAFF LOOKUP FAILURE
    // =========================================================

    @Test
    void review_ShouldThrow_WhenManagerMissing() {

        UUID id = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        AttendanceAdjustment adjustment =
                AttendanceAdjustment.builder()
                        .attendance(
                                StaffAttendance.builder()
                                        .build()
                        )
                        .status(
                                AttendanceAdjustmentStatus.PENDING
                        )
                        .build();

        when(adjustments.findById(id))
                .thenReturn(Optional.of(adjustment));

        when(staffRepo.findById(managerId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> attendanceService.review(
                        id,
                        managerId,
                        true,
                        null
                )
        );
    }


    // =========================================================
    // tiny holder only to generate HH:mm safely
    // =========================================================

    private static class LocalTimeHolder {

        private final LocalDateTime value;

        LocalTimeHolder(LocalDateTime value) {
            this.value = value;
        }

        String time() {
            return value.toLocalTime()
                    .withSecond(0)
                    .withNano(0)
                    .toString();
        }
    }
}