package org.example.doansummer2026.dto.schedule;

import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.ShiftConfig;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.StaffSchedule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicManagerScheduleResponseTest {

    private final LocalDate monday = LocalDate.of(2026, 8, 31);
    private final ShiftConfig morning = ShiftConfig.builder()
            .shiftId(UUID.fromString("70000001-1111-1111-1111-111111111111"))
            .name("Ca Sáng")
            .startTime("00:00")
            .endTime("08:00")
            .isActive(true)
            .build();

    @Test
    void paraclinicalRoomAcceptsScheduledProfessionalStaff() {
        var nurseSchedule = schedule(SystemRole.NURSE, monday, "Kỹ thuật viên xét nghiệm");

        var response = ClinicManagerScheduleResponse.from(
                List.of(nurseSchedule), monday, List.of(morning), "PROFESSIONAL", false);

        var coverage = response.coverage().get(key(DayOfWeek.MONDAY));
        assertThat(coverage.status()).isEqualTo("COVERED");
        assertThat(coverage.missingRoles()).isEmpty();
    }

    @Test
    void examinationRoomStillRequiresDoctor() {
        var nurseSchedule = schedule(SystemRole.NURSE, monday, "Điều dưỡng phòng khám");

        var response = ClinicManagerScheduleResponse.from(
                List.of(nurseSchedule), monday, List.of(morning), "PROFESSIONAL", true);

        var coverage = response.coverage().get(key(DayOfWeek.MONDAY));
        assertThat(coverage.status()).isEqualTo("MISSING_DOCTOR");
        assertThat(coverage.missingRoles()).containsExactly("DOCTOR");
    }

    @Test
    void sundayAlsoRequiresReceptionistAndCashier() {
        var response = ClinicManagerScheduleResponse.from(
                List.of(), monday, List.of(morning), "GENERAL", true);

        var coverage = response.coverage().get(key(DayOfWeek.SUNDAY));
        assertThat(coverage.status()).isEqualTo("UNASSIGNED");
        assertThat(coverage.missingRoles()).containsExactly("RECEPTIONIST", "CASHIER");
    }

    private StaffSchedule schedule(SystemRole role, LocalDate workDate, String fullName) {
        var profile = Profile.builder().fullName(fullName).build();
        var staff = StaffInfo.builder()
                .staffId(UUID.randomUUID())
                .profile(profile)
                .systemRole(role)
                .build();
        return StaffSchedule.builder()
                .scheduleId(UUID.randomUUID())
                .staff(staff)
                .workDate(workDate)
                .shift(morning)
                .status(ScheduleStatus.SCHEDULED)
                .build();
    }

    private String key(DayOfWeek dayOfWeek) {
        return ClinicManagerScheduleResponse.toKey(morning.getShiftId().toString(), dayOfWeek);
    }
}
