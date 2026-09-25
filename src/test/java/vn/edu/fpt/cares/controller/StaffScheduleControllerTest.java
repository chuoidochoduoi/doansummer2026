package vn.edu.fpt.cares.controller;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.schedule.*;
import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.enums.ScheduleStatus;
import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.DepartmentRepository;
import vn.edu.fpt.cares.repository.ShiftConfigRepository;
import vn.edu.fpt.cares.service.AuthService;
import vn.edu.fpt.cares.service.StaffScheduleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffScheduleControllerTest {
    @Mock StaffScheduleService service;
    @Mock ShiftConfigRepository shiftRepo;
    @Mock DepartmentRepository departmentRepo;
    @Mock AuthService authService;
    private StaffScheduleController controller;

    @BeforeEach
    void setUp() {
        controller = new StaffScheduleController(service, shiftRepo, departmentRepo, authService);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void searchUsesRequestedStaffForAdminAndCurrentStaffForOthers() {
        UUID requested = UUID.randomUUID();
        UUID current = UUID.randomUUID();
        var pageable = PageRequest.of(0, 10);
        PageResponse<ScheduleResponse> page = mock(PageResponse.class);
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.ADMIN);
        when(service.search(requested, null, null, null, pageable)).thenReturn(page);
        assertSame(page, controller.search(requested, null, null, null, pageable).getBody());

        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.DOCTOR);
        when(authService.currentStaffId()).thenReturn(current);
        when(service.search(current, null, null, null, pageable)).thenReturn(page);
        assertSame(page, controller.search(requested, null, null, null, pageable).getBody());

        when(authService.currentStaffId()).thenReturn(null);
        assertThrows(BadRequestException.class, () -> controller.search(requested, null, null, null, pageable));
    }

    @Test
    void getUsesAdminOrOwnerScopedService() {
        UUID id = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        ScheduleResponse response = response(id);
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.ADMIN);
        when(service.get(id)).thenReturn(response);
        assertSame(response, controller.get(id).getBody());

        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.NURSE);
        when(authService.currentStaffId()).thenReturn(staffId);
        when(service.getForStaff(id, staffId)).thenReturn(response);
        assertSame(response, controller.get(id).getBody());

        when(authService.currentStaffId()).thenReturn(null);
        assertThrows(BadRequestException.class, () -> controller.get(id));
    }

    @Test
    void basicMutationAndGenerationEndpointsDelegate() {
        UUID id = UUID.randomUUID();
        ScheduleResponse response = response(id);
        ScheduleCreateRequest create = new ScheduleCreateRequest(UUID.randomUUID(), LocalDate.now(),
                UUID.randomUUID(), ScheduleStatus.SCHEDULED, false, null, null);
        when(service.create(create)).thenReturn(response);
        assertEquals(201, controller.create(create).getStatusCode().value());

        ScheduleUpdateRequest update = new ScheduleUpdateRequest(null, ScheduleStatus.ABSENT, true, "nghỉ");
        when(service.update(id, update)).thenReturn(response);
        assertSame(response, controller.update(id, update).getBody());
        assertEquals(204, controller.delete(id).getStatusCode().value());
        verify(service).delete(id);

        ScheduleGenerateRequest generate = new ScheduleGenerateRequest(LocalDate.now(), List.of(UUID.randomUUID()), true);
        when(service.generateFromTemplates(generate.weekStart(), generate.staffIds(), true)).thenReturn(List.of(response));
        assertEquals(1, controller.generate(generate).getBody().size());
    }

    @Test
    void managerScheduleFiltersDepartmentAndProfessionalGroup() {
        LocalDate week = LocalDate.of(2026, 9, 3);
        LocalDate monday = week.with(java.time.DayOfWeek.MONDAY);
        UUID departmentId = UUID.randomUUID();
        Department target = Department.builder().departmentId(departmentId)
                .departmentType(DepartmentType.PARACLINICAL).build();
        Department other = Department.builder().departmentId(UUID.randomUUID()).build();
        ShiftConfig shift = shift();
        StaffSchedule doctor = schedule(monday, shift, SystemRole.DOCTOR, target, "Bác sĩ An");
        StaffSchedule nurse = schedule(monday, shift, SystemRole.NURSE, target, "Y tá Bình");
        StaffSchedule receptionist = schedule(monday, shift, SystemRole.RECEPTIONIST, target, "Lễ tân Chi");
        StaffSchedule outside = schedule(monday, shift, SystemRole.DOCTOR, other, "Bác sĩ Dũng");
        when(service.findByWeek(monday, monday.plusDays(6)))
                .thenReturn(List.of(doctor, nurse, receptionist, outside));
        when(shiftRepo.findAllByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(shift));
        when(departmentRepo.findById(departmentId)).thenReturn(Optional.of(target));

        var response = controller.getSchedules(week, departmentId, "PROFESSIONAL").getBody();
        assertNotNull(response);
        assertEquals(2, response.staff().size());
        assertTrue(response.staff().stream().allMatch(item -> "BS".equals(item.role()) || "YT".equals(item.role())));
        assertTrue(response.coverage().values().stream().anyMatch(info -> info.status().equals("COVERED")));
    }

    @Test
    void managerScheduleHandlesGeneralUnknownAndMissingStaffData() {
        LocalDate week = LocalDate.of(2026, 9, 3);
        LocalDate monday = week.with(java.time.DayOfWeek.MONDAY);
        ShiftConfig shift = shift();
        StaffSchedule receptionist = schedule(monday, shift, SystemRole.RECEPTIONIST, null, "Lễ tân");
        StaffSchedule cashier = schedule(monday, shift, SystemRole.CASHIER, null, "Thu ngân");
        StaffSchedule noStaff = StaffSchedule.builder().scheduleId(UUID.randomUUID()).workDate(monday)
                .shift(shift).status(ScheduleStatus.SCHEDULED).build();
        StaffSchedule noRole = StaffSchedule.builder().scheduleId(UUID.randomUUID()).workDate(monday)
                .shift(shift).staff(StaffInfo.builder().staffId(UUID.randomUUID()).build())
                .status(ScheduleStatus.SCHEDULED).build();
        when(service.findByWeek(monday, monday.plusDays(6))).thenReturn(List.of(receptionist, cashier, noStaff, noRole));
        when(shiftRepo.findAllByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(shift));

        var general = controller.getSchedules(week, null, "GENERAL").getBody();
        assertEquals(2, general.staff().size());
        var unknown = controller.getSchedules(week, null, "OTHER").getBody();
        assertEquals(2, unknown.staff().size());
    }

    @Test
    void managerScheduleTreatsMissingOrExaminationDepartmentAsDoctorRequired() {
        LocalDate week = LocalDate.of(2026, 9, 3);
        LocalDate monday = week.with(java.time.DayOfWeek.MONDAY);
        UUID departmentId = UUID.randomUUID();
        ShiftConfig shift = shift();
        when(service.findByWeek(monday, monday.plusDays(6))).thenReturn(List.of());
        when(shiftRepo.findAllByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(shift));
        when(departmentRepo.findById(departmentId)).thenReturn(Optional.empty());
        assertNotNull(controller.getSchedules(week, departmentId, null).getBody());

        Department examination = Department.builder().departmentId(departmentId).departmentType(null).build();
        when(departmentRepo.findById(departmentId)).thenReturn(Optional.of(examination));
        assertNotNull(controller.getSchedules(week, departmentId, null).getBody());
    }

    @Test
    void assignCopyAndShiftUpdateReturnExpectedResponses() {
        LocalDate week = LocalDate.of(2026, 9, 3);
        ScheduleAssignRequest assign = new ScheduleAssignRequest(week, UUID.randomUUID(), "mon",
                UUID.randomUUID(), "add");
        assertEquals(204, controller.assign(assign).getStatusCode().value());
        verify(service).assignStaff(assign);

        ShiftConfig shift = shift();
        LocalDate monday = week.with(java.time.DayOfWeek.MONDAY);
        when(service.copyWeek(monday.minusDays(7), monday)).thenReturn(List.of());
        when(shiftRepo.findAllByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(shift));
        assertNotNull(controller.copy(new ScheduleCopyRequest(week)).getBody());

        ScheduleShiftUpdateRequest shifts = new ScheduleShiftUpdateRequest(List.of(
                new ScheduleShiftUpdateRequest.ShiftItem("morning", "Ca sáng", "08:00", "12:00")));
        assertEquals(204, controller.updateShifts(shifts).getStatusCode().value());
    }

    private ShiftConfig shift() {
        return ShiftConfig.builder().shiftId(UUID.randomUUID()).name("Ca sáng")
                .startTime("08:00").endTime("12:00").isActive(true).build();
    }

    private StaffSchedule schedule(LocalDate date, ShiftConfig shift, SystemRole role,
                                   Department department, String name) {
        StaffInfo staff = StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(role).department(department)
                .profile(Profile.builder().fullName(name).build()).build();
        return StaffSchedule.builder().scheduleId(UUID.randomUUID()).staff(staff).workDate(date).shift(shift)
                .status(ScheduleStatus.SCHEDULED).isCustom(false).build();
    }

    private ScheduleResponse response(UUID id) {
        return new ScheduleResponse(id, UUID.randomUUID(), "STF-01", "Nguyễn Anh Đức", LocalDate.now(),
                null, ScheduleStatus.SCHEDULED, false, null, null);
    }
}
