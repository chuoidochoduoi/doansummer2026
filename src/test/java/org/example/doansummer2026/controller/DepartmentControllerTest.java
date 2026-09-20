package org.example.doansummer2026.controller;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.department.DepartmentCreateRequest;
import org.example.doansummer2026.dto.department.DepartmentResponse;
import org.example.doansummer2026.dto.department.DepartmentUpdateRequest;
import org.example.doansummer2026.dto.staff.StaffOptionResponse;
import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.service.DepartmentService;
import org.example.doansummer2026.service.StaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DepartmentControllerTest {
    private DepartmentService service;
    private StaffService staffService;
    private DepartmentController controller;
    private Pageable pageable;
    private PageResponse<DepartmentResponse> page;
    private DepartmentResponse response;

    @BeforeEach
    void setUp() {
        service = mock(DepartmentService.class);
        staffService = mock(StaffService.class);
        controller = new DepartmentController(service, staffService);
        pageable = Pageable.unpaged();
        page = mock(PageResponse.class);
        response = mock(DepartmentResponse.class);
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void listEndpointsChooseAllOrSpecifiedDepartmentTypes() {
        when(service.listAll(pageable)).thenReturn(page);
        when(service.listMultiple(any(), anyList())).thenReturn(page);

        assertSame(page, controller.listForAdmin(null, pageable).getBody());
        assertSame(page, controller.listForAdmin(new DepartmentType[0], pageable).getBody());
        assertSame(page, controller.list(null, pageable).getBody());
        assertSame(page, controller.list(new DepartmentType[0], pageable).getBody());

        var types = new DepartmentType[]{DepartmentType.EXAMINATION, DepartmentType.LABORATORY};
        assertSame(page, controller.listForAdmin(types, pageable).getBody());
        assertSame(page, controller.list(types, pageable).getBody());
        assertSame(page, controller.listClinical(pageable).getBody());
        verify(service, times(4)).listAll(pageable);
        verify(service, times(3)).listMultiple(any(), anyList());
    }

    @Test
    void crudAndStatusEndpointsDelegateWithoutChangingPayloads() {
        UUID id = UUID.randomUUID();
        var create = mock(DepartmentCreateRequest.class);
        var update = mock(DepartmentUpdateRequest.class);
        when(response.departmentId()).thenReturn(id);
        when(service.get(id)).thenReturn(response);
        when(service.create(create)).thenReturn(response);
        when(service.update(id, update)).thenReturn(response);
        when(service.updateStatus(id, DepartmentStatus.MAINTENANCE)).thenReturn(response);

        assertSame(response, controller.get(id).getBody());
        assertEquals(201, controller.create(create).getStatusCode().value());
        assertSame(response, controller.update(id, update).getBody());
        assertSame(response, controller.updateStatus(id, Map.of("status", "MAINTENANCE")).getBody());
        assertSame(response, controller.updateStatus(id, Map.of()).getBody());
        assertEquals(204, controller.delete(id).getStatusCode().value());
        verify(service).delete(id);
    }

    @Test
    void staffOptionsAndMyDepartmentAreReturned() {
        var doctor = mock(StaffOptionResponse.class);
        var nurse = mock(StaffOptionResponse.class);
        when(staffService.findAllDoctors()).thenReturn(List.of(doctor));
        when(staffService.findAllNurses()).thenReturn(List.of(nurse));
        when(service.getMyDepartment()).thenReturn(response);

        assertEquals(List.of(doctor), controller.listDoctors().getBody());
        assertEquals(List.of(nurse), controller.listNurses().getBody());
        assertSame(response, controller.getMyDepartment().getBody());
    }
}
