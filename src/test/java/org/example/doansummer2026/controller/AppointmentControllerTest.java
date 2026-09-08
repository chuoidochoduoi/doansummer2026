package org.example.doansummer2026.controller;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.appointment.*;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.service.AppointmentService;
import org.example.doansummer2026.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {
    @Mock AppointmentService service;
    @Mock AuthService authService;
    private AppointmentController controller;

    @BeforeEach
    void setUp() {
        controller = new AppointmentController(service, authService,
                mock(org.example.doansummer2026.service.StaffDutyService.class));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void listAndGetDelegateAllFilters() {
        UUID customer = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 9, 5, 8, 0);
        LocalDateTime to = from.plusDays(1);
        var pageable = PageRequest.of(1, 10);
        PageResponse<AppointmentResponse> page = mock(PageResponse.class);
        AppointmentResponse response = response(id);
        when(service.search(customer, AppointmentStatus.PENDING, from, to, pageable)).thenReturn(page);
        when(service.get(id)).thenReturn(response);
        assertSame(page, controller.list(customer, AppointmentStatus.PENDING, from, to, pageable).getBody());
        assertSame(response, controller.get(id).getBody());
    }

    @Test
    void customerCanOnlyCreateForOwnAccountWhileStaffMayCreateForAnother() {
        UUID accountId = UUID.randomUUID();
        AppointmentCreateRequest request = mock(AppointmentCreateRequest.class);
        when(request.customerId()).thenReturn(accountId);
        Account customer = Account.builder().accountId(accountId).role(Role.CUSTOMER).build();
        when(authService.currentAccount()).thenReturn(customer);
        AppointmentResponse response = response(UUID.randomUUID());
        when(service.create(request)).thenReturn(response);
        assertEquals(201, controller.create(request).getStatusCode().value());

        when(request.customerId()).thenReturn(UUID.randomUUID());
        assertThrows(BadRequestException.class, () -> controller.create(request));

        Account staff = Account.builder().accountId(UUID.randomUUID()).role(Role.STAFF).build();
        when(authService.currentAccount()).thenReturn(staff);
        when(service.create(request)).thenReturn(response);
        assertEquals(201, controller.create(request).getStatusCode().value());
    }

    @Test
    void guestCreateUpdateAndDeleteDelegate() {
        UUID id = UUID.randomUUID();
        AppointmentResponse response = response(id);
        AppointmentGuestCreateRequest guest = mock(AppointmentGuestCreateRequest.class);
        when(service.createForGuest(guest)).thenReturn(response);
        assertEquals(201, controller.createForGuest(guest).getStatusCode().value());

        AppointmentUpdateRequest update = mock(AppointmentUpdateRequest.class);
        when(service.update(id, update)).thenReturn(response);
        assertSame(response, controller.update(id, update).getBody());
        assertEquals(204, controller.delete(id).getStatusCode().value());
        verify(service).delete(id);
    }

    @Test
    void checkInAlwaysUsesAuthenticatedStaffId() {
        UUID appointmentId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        AppointmentCheckInRequest request = mock(AppointmentCheckInRequest.class);
        AppointmentCheckInResponse response = mock(AppointmentCheckInResponse.class);
        when(authService.currentStaffId()).thenReturn(staffId);
        when(service.checkIn(any())).thenReturn(response);
        assertSame(response, controller.checkIn(appointmentId, request).getBody());
        ArgumentCaptor<AppointmentCheckInRequest> captor = ArgumentCaptor.forClass(AppointmentCheckInRequest.class);
        verify(service).checkIn(captor.capture());
        assertEquals(appointmentId, captor.getValue().appointmentId());
        assertEquals(staffId, captor.getValue().issuedById());
    }

    @Test
    void guestCheckInAlwaysUsesAuthenticatedStaffId() {
        UUID staffId = UUID.randomUUID();
        GuestCheckInRequest request = mock(GuestCheckInRequest.class);
        GuestCheckInResponse response = mock(GuestCheckInResponse.class);
        when(authService.currentStaffId()).thenReturn(staffId);
        when(service.guestCheckIn(any())).thenReturn(response);
        assertSame(response, controller.guestCheckIn(request).getBody());
        ArgumentCaptor<GuestCheckInRequest> captor = ArgumentCaptor.forClass(GuestCheckInRequest.class);
        verify(service).guestCheckIn(captor.capture());
        assertEquals(staffId, captor.getValue().issuedById());
    }

    @Test
    void guestHistoryAndCustomerReadEndpointsDelegateWithAccount() {
        when(service.getGuestHistoryByPhone("0987654321")).thenReturn(List.of(mock(GuestHistoryResponse.class)));
        assertEquals(1, controller.getGuestHistory("0987654321").getBody().size());

        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        when(authService.currentAccount()).thenReturn(Account.builder().accountId(accountId).build());
        var pageable = PageRequest.of(0, 10);
        PageResponse<CustomerAppointmentResponse> page = mock(PageResponse.class);
        when(service.getMyAppointments(eq(accountId), eq(profileId), eq(true), eq("APP"), eq("Nội"),
                eq("CONFIRMED"), any(), any(), eq(pageable))).thenReturn(page);
        assertSame(page, controller.getMyAppointments(profileId, true, "APP", "Nội", "CONFIRMED",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), pageable).getBody());

        CustomerAppointmentDetailResponse detail = mock(CustomerAppointmentDetailResponse.class);
        when(service.getMyAppointmentDetail(accountId, appointmentId)).thenReturn(detail);
        assertSame(detail, controller.getMyAppointmentDetail(appointmentId).getBody());
    }

    @Test
    void customerMutationEndpointsDelegateWithAccount() {
        UUID accountId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        when(authService.currentAccount()).thenReturn(Account.builder().accountId(accountId).build());
        AppointmentResponse response = response(appointmentId);
        CustomerAppointmentCreateRequest create = mock(CustomerAppointmentCreateRequest.class);
        when(service.createMy(accountId, create)).thenReturn(response);
        assertEquals(201, controller.createMy(create).getStatusCode().value());

        GroupAppointmentCreateRequest group = mock(GroupAppointmentCreateRequest.class);
        when(service.createMyGroup(accountId, group)).thenReturn(List.of(response));
        assertEquals(1, controller.createMyGroup(group).getBody().size());

        AppointmentUpdateRequest update = mock(AppointmentUpdateRequest.class);
        CustomerAppointmentDetailResponse detail = mock(CustomerAppointmentDetailResponse.class);
        when(service.updateMyAppointment(accountId, appointmentId, update)).thenReturn(detail);
        assertSame(detail, controller.updateMyAppointment(appointmentId, update).getBody());

        assertEquals(204, controller.cancelMyAppointment(appointmentId).getStatusCode().value());
        verify(service).cancelMyAppointment(accountId, appointmentId);
    }

    private AppointmentResponse response(UUID id) {
        AppointmentResponse value = mock(AppointmentResponse.class);
        lenient().when(value.appointmentId()).thenReturn(id);
        return value;
    }
}
