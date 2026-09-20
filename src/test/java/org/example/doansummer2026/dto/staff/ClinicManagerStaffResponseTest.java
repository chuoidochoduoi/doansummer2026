package org.example.doansummer2026.dto.staff;

import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.Specialization;
import org.example.doansummer2026.model.StaffInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClinicManagerStaffResponseTest {

    @Test
    void mapsCompleteActiveStaffAndMissingOptionalRelations() {
        Profile profile = Profile.builder()
                .fullName("Nguyễn Minh An")
                .dateOfBirth(LocalDate.of(1990, 1, 2))
                .gender(Gender.MALE)
                .phone("0909000001")
                .email("doctor@example.com")
                .address("Hà Nội")
                .account(Account.builder().isActive(true).build())
                .build();
        StaffInfo complete = StaffInfo.builder()
                .staffId(UUID.randomUUID())
                .staffCode("STF-001")
                .profile(profile)
                .systemRole(SystemRole.DOCTOR)
                .specialization(Specialization.builder().name("Nội khoa").build())
                .department(Department.builder().name("Phòng khám Nội").build())
                .highestDegree("Bác sĩ chuyên khoa I")
                .university("Đại học Y Hà Nội")
                .build();

        ClinicManagerStaffResponse active = ClinicManagerStaffResponse.from(complete);
        assertAll(
                () -> assertEquals("Nguyễn Minh An", active.fullName()),
                () -> assertEquals("Nội khoa", active.specializationName()),
                () -> assertEquals("Phòng khám Nội", active.departmentName()),
                () -> assertEquals("ACTIVE", active.status()));

        StaffInfo missing = StaffInfo.builder().staffId(UUID.randomUUID()).build();
        ClinicManagerStaffResponse inactive = ClinicManagerStaffResponse.from(missing);
        assertAll(
                () -> assertNull(inactive.fullName()),
                () -> assertNull(inactive.dateOfBirth()),
                () -> assertNull(inactive.gender()),
                () -> assertNull(inactive.phone()),
                () -> assertNull(inactive.email()),
                () -> assertNull(inactive.address()),
                () -> assertNull(inactive.specializationName()),
                () -> assertNull(inactive.departmentName()),
                () -> assertEquals("INACTIVE", inactive.status()));

        profile.setAccount(Account.builder().isActive(false).build());
        assertEquals("INACTIVE", ClinicManagerStaffResponse.from(complete).status());
        profile.setAccount(null);
        assertEquals("INACTIVE", ClinicManagerStaffResponse.from(complete).status());
    }
}
