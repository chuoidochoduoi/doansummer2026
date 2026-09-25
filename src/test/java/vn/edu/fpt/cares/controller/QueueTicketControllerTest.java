package vn.edu.fpt.cares.controller;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.medicalrecord.MedicalRecordResponse;
import vn.edu.fpt.cares.dto.medicalrecord.MedicalRecordUpdateRequest;
import vn.edu.fpt.cares.dto.queueticket.*;
import vn.edu.fpt.cares.enums.QueueStatus;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.service.QueueTicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueTicketControllerTest {
    @Mock QueueTicketService service;
    private QueueTicketController controller;

    @BeforeEach
    void setUp() {
        controller = new QueueTicketController(service);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void listGetCreateAndUpdateDelegate() {
        UUID id = UUID.randomUUID(), departmentId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 5);
        var pageable = PageRequest.of(0, 10);
        PageResponse<QueueTicketResponse> page = page();
        QueueTicketResponse response = response(id);
        when(service.search(departmentId, date, QueueStatus.WAITING, pageable)).thenReturn(page);
        when(service.get(id)).thenReturn(response);
        assertSame(page, controller.list(departmentId, date, QueueStatus.WAITING, pageable).getBody());
        assertSame(response, controller.get(id).getBody());

        QueueTicketCreateRequest create = mock(QueueTicketCreateRequest.class);
        when(service.create(create)).thenReturn(response);
        assertEquals(201, controller.create(create).getStatusCode().value());
        QueueTicketUpdateRequest update = mock(QueueTicketUpdateRequest.class);
        when(service.update(id, update)).thenReturn(response);
        assertSame(response, controller.update(id, update).getBody());
        assertSame(response, controller.updateQueue(id, update).getBody());
    }

    @Test
    void workflowActionsDelegate() {
        UUID id = UUID.randomUUID();
        QueueTicketResponse ticket = response(id);
        when(service.call(id)).thenReturn(ticket);
        when(service.startExam(id)).thenReturn(ticket);
        when(service.finishParaclinicalQueue(id)).thenReturn(ticket);
        when(service.skip(id)).thenReturn(ticket);
        when(service.returnToQueue(id)).thenReturn(ticket);
        when(service.markTestDone(id)).thenReturn(ticket);
        assertSame(ticket, controller.call(id).getBody());
        assertSame(ticket, controller.startExam(id).getBody());
        assertSame(ticket, controller.finishParaclinicalService(id).getBody());
        assertSame(ticket, controller.skip(id).getBody());
        assertSame(ticket, controller.returnToQueue(id).getBody());
        assertSame(ticket, controller.markTestDone(id).getBody());

        MedicalRecordUpdateRequest update = mock(MedicalRecordUpdateRequest.class);
        MedicalRecordResponse record = mock(MedicalRecordResponse.class);
        ExaminationTransitionResponse transition = mock(ExaminationTransitionResponse.class);
        SameRoomExaminationChainResponse chain = mock(SameRoomExaminationChainResponse.class);
        when(service.completeAndReturnRecord(id, update)).thenReturn(record);
        when(service.completeAndTransition(id, update)).thenReturn(transition);
        when(service.sameRoomChain(id)).thenReturn(chain);
        assertSame(record, controller.complete(id, update).getBody());
        assertSame(transition, controller.completeTransition(id, update).getBody());
        assertSame(chain, controller.sameRoomChain(id).getBody());
        assertEquals(204, controller.delete(id).getStatusCode().value());
        verify(service).delete(id);
    }

    @Test
    void returnToQueue_IsRestrictedToDoctor() throws Exception {
        PreAuthorize authorization = QueueTicketController.class
                .getDeclaredMethod("returnToQueue", UUID.class)
                .getAnnotation(PreAuthorize.class);

        assertNotNull(authorization);
        assertEquals("hasAuthority('ROLE_DOCTOR')", authorization.value());
    }

    @Test
    void inProgressAndWaitingViewsHandleEmptyAndStatusAliases() {
        UUID departmentId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 5);
        var pageable = PageRequest.of(0, 10);
        when(service.getInprogressByDepartment(departmentId)).thenReturn(null);
        assertEquals(204, controller.getInprogress(departmentId).getStatusCode().value());
        QueueTicketResponse current = response(UUID.randomUUID());
        when(service.getInprogressByDepartment(departmentId)).thenReturn(current);
        assertSame(current, controller.getInprogress(departmentId).getBody());

        PageResponse<QueueTicketResponse> page = page();
        when(service.getAllInprogress(pageable)).thenReturn(page);
        when(service.getWaitingByDepartment(departmentId, date, QueueStatus.WAITING, pageable)).thenReturn(page);
        when(service.getWaitingByDepartment(departmentId, date, QueueStatus.WAITING_FOR_TEST, pageable)).thenReturn(page);
        when(service.getWaitingByDepartment(departmentId, date, QueueStatus.TEST_DONE, pageable)).thenReturn(page);
        assertSame(page, controller.getAllInprogress(pageable).getBody());
        assertSame(page, controller.getWaiting(departmentId, date, QueueStatus.WAITING, pageable).getBody());
        assertSame(page, controller.getWaitingForTest(departmentId, date, pageable).getBody());
        assertSame(page, controller.getTestDone(departmentId, date, pageable).getBody());
    }

    @Test
    void legacyQueueMapsNullAllAndEnumAndRejectsInvalidStatus() {
        UUID departmentId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 10);
        PageResponse<QueueTicketResponse> page = page();
        when(service.search(departmentId, null, null, pageable)).thenReturn(page);
        assertSame(page, controller.getQueue(departmentId, null, null, null, pageable).getBody());
        assertSame(page, controller.getQueue(departmentId, "all", "ignored", "ignored", pageable).getBody());
        when(service.search(departmentId, null, QueueStatus.CALLED, pageable)).thenReturn(page);
        assertSame(page, controller.getQueue(departmentId, "CALLED", null, null, pageable).getBody());
        assertThrows(BadRequestException.class,
                () -> controller.getQueue(departmentId, "not-a-status", null, null, pageable));
    }

    private QueueTicketResponse response(UUID id) {
        QueueTicketResponse response = mock(QueueTicketResponse.class);
        lenient().when(response.ticketId()).thenReturn(id);
        return response;
    }

    private PageResponse<QueueTicketResponse> page() {
        return new PageResponse<>(List.of(), 0, 10, 0, 0, true, true);
    }
}
