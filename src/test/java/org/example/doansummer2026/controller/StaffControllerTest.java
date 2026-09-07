package org.example.doansummer2026.controller;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.schedule.MyScheduleResponse;
import org.example.doansummer2026.dto.staff.*;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.service.AuthService;
import org.example.doansummer2026.service.StaffScheduleService;
import org.example.doansummer2026.service.StaffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffControllerTest {
    @Mock StaffService staffService;
    @Mock StaffScheduleService scheduleService;
    @Mock ShiftConfigRepository shiftRepo;
    @Mock AuthService authService;
    private StaffController controller;

    @BeforeEach
    void setUp() {
        controller = new StaffController(staffService, scheduleService, shiftRepo, authService);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void listEndpointsDelegateToService() {
        var page = mock(PageResponse.class);
        when(staffService.searchForClinicManager("an", PageRequest.of(0, 10))).thenReturn(page);
        assertSame(page, controller.searchForClinicManager("an", PageRequest.of(0, 10)).getBody());

        UUID id = UUID.randomUUID();
        ClinicManagerStaffResponse managerResponse = mock(ClinicManagerStaffResponse.class);
        when(staffService.getForClinicManager(id)).thenReturn(managerResponse);
        assertSame(managerResponse, controller.getForClinicManager(id).getBody());

        var staffPage = mock(PageResponse.class);
        when(staffService.search("bs", null, SystemRole.DOCTOR, PageRequest.of(1, 5))).thenReturn(staffPage);
        assertSame(staffPage, controller.search("bs", null, SystemRole.DOCTOR, PageRequest.of(1, 5)).getBody());

        List<StaffOptionResponse> options = List.of(mock(StaffOptionResponse.class));
        when(staffService.listForSchedule(SystemRole.NURSE)).thenReturn(options);
        when(staffService.getPublicActiveDoctors()).thenReturn(options);
        assertSame(options, controller.list(SystemRole.NURSE).getBody());
        assertSame(options, controller.getPublicDoctors().getBody());
    }

    @Test
    void capabilityAccessAllowsManagersAndOwnerButDeniesOtherStaff() {
        UUID id = UUID.randomUUID();
        List<StaffCapabilityResponse> responses = List.of(mock(StaffCapabilityResponse.class));
        when(staffService.listCapabilities(id)).thenReturn(responses);
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.ADMIN);
        assertSame(responses, controller.listCapabilities(id));

        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.CLINIC_MANAGER);
        assertSame(responses, controller.listCapabilities(id));

        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.DOCTOR);
        when(authService.currentStaffId()).thenReturn(id);
        assertSame(responses, controller.listCapabilities(id));

        when(authService.currentStaffId()).thenReturn(UUID.randomUUID());
        assertThrows(AccessDeniedException.class, () -> controller.listCapabilities(id));

        List<StaffCapabilityRequest> requests = List.of(mock(StaffCapabilityRequest.class));
        when(staffService.replaceCapabilities(id, requests)).thenReturn(responses);
        assertSame(responses, controller.replaceCapabilities(id, requests));
    }

    @Test
    void myScheduleHandlesMissingAndPresentStaff() {
        LocalDate week = LocalDate.of(2026, 9, 3);
        when(authService.currentStaffId()).thenReturn(null);
        MyScheduleResponse empty = controller.mySchedule(week).getBody();
        assertNotNull(empty);
        assertTrue(empty.shifts().isEmpty());

        UUID id = UUID.randomUUID();
        when(authService.currentStaffId()).thenReturn(id);
        when(scheduleService.findByStaffAndWeek(eq(id), any(), any())).thenReturn(List.of());
        when(shiftRepo.findAll()).thenReturn(List.of());
        MyScheduleResponse response = controller.mySchedule(week).getBody();
        assertNotNull(response);
        verify(scheduleService).findByStaffAndWeek(id, week.with(java.time.DayOfWeek.MONDAY),
                week.with(java.time.DayOfWeek.MONDAY).plusDays(6));
    }

    @Test
    void getAllowsManagersAndOwnerButDeniesOtherStaff() {
        UUID id = UUID.randomUUID();
        StaffResponse response = response(id, StaffResponse.SystemRoleBrief.DOCTOR);
        when(staffService.get(id)).thenReturn(response);
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.ADMIN);
        assertSame(response, controller.get(id).getBody());

        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.CLINIC_MANAGER);
        assertSame(response, controller.get(id).getBody());

        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.DOCTOR);
        when(authService.currentStaffId()).thenReturn(id);
        assertSame(response, controller.get(id).getBody());

        when(authService.currentStaffId()).thenReturn(UUID.randomUUID());
        assertThrows(AccessDeniedException.class, () -> controller.get(id));
    }

    @Test
    void getByAccountChecksOwnership() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().accountId(accountId).build();
        StaffResponse response = response(UUID.randomUUID(), StaffResponse.SystemRoleBrief.NURSE);
        when(authService.currentAccount()).thenReturn(account);
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.NURSE);
        when(staffService.getByAccountId(accountId)).thenReturn(response);
        assertSame(response, controller.getByAccount(accountId).getBody());
        assertThrows(AccessDeniedException.class, () -> controller.getByAccount(UUID.randomUUID()));

        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.ADMIN);
        UUID other = UUID.randomUUID();
        when(staffService.getByAccountId(other)).thenReturn(response);
        assertSame(response, controller.getByAccount(other).getBody());
    }

    @Test
    void updateOwnProfessionalRequiresCurrentStaff() {
        StaffProfessionalUpdateRequest request = new StaffProfessionalUpdateRequest("Bác sĩ CKI", "Đại học Y");
        when(authService.currentStaffId()).thenReturn(null);
        assertThrows(BadRequestException.class, () -> controller.updateOwnProfessionalInfo(request));

        UUID id = UUID.randomUUID();
        StaffResponse response = response(id, StaffResponse.SystemRoleBrief.DOCTOR);
        when(authService.currentStaffId()).thenReturn(id);
        when(staffService.updateOwnProfessionalInfo(id, request)).thenReturn(response);
        assertSame(response, controller.updateOwnProfessionalInfo(request).getBody());
    }

    @Test
    void clinicManagerCannotCreateOrUpdatePrivilegedRoles() {
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.CLINIC_MANAGER);
        StaffCreateRequest create = mock(StaffCreateRequest.class);
        when(create.systemRole()).thenReturn(SystemRole.ADMIN);
        assertThrows(AccessDeniedException.class, () -> controller.create(create));

        UUID id = UUID.randomUUID();
        when(staffService.get(id)).thenReturn(response(id, StaffResponse.SystemRoleBrief.CLINIC_MANAGER));
        assertThrows(AccessDeniedException.class, () -> controller.update(id, mock(StaffUpdateRequest.class)));
        verify(staffService, never()).update(any(), any());
    }

    @Test
    void createUpdateDeleteAndLockDelegateForAllowedRole() {
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.ADMIN);
        UUID id = UUID.randomUUID();
        StaffResponse response = response(id, StaffResponse.SystemRoleBrief.DOCTOR);
        StaffCreateRequest create = mock(StaffCreateRequest.class);
        when(create.systemRole()).thenReturn(SystemRole.DOCTOR);
        when(staffService.create(create)).thenReturn(response);
        assertEquals(201, controller.create(create).getStatusCode().value());

        StaffUpdateRequest update = mock(StaffUpdateRequest.class);
        when(staffService.get(id)).thenReturn(response);
        when(staffService.update(id, update)).thenReturn(response);
        assertSame(response, controller.update(id, update).getBody());

        assertEquals(204, controller.delete(id).getStatusCode().value());
        verify(staffService).delete(id);
        when(staffService.lock(id)).thenReturn(response);
        assertSame(response, controller.lock(id).getBody());
    }

    private StaffResponse response(UUID id, StaffResponse.SystemRoleBrief role) {
        return new StaffResponse(id, "STF-001", null, null, null, null, role, null, null);
    }
}
