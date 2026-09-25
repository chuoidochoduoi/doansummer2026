package vn.edu.fpt.cares.controller;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.department.DepartmentCreateRequest;
import vn.edu.fpt.cares.dto.department.DepartmentResponse;
import vn.edu.fpt.cares.dto.department.DepartmentUpdateRequest;
import vn.edu.fpt.cares.dto.staff.StaffOptionResponse;
import vn.edu.fpt.cares.enums.DepartmentStatus;
import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.service.DepartmentService;
import vn.edu.fpt.cares.service.StaffService;
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
