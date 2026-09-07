package org.example.doansummer2026.controller;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse;
import org.example.doansummer2026.dto.testrequest.*;
import org.example.doansummer2026.dto.testresult.*;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.service.TestRequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestRequestControllerTest {
    @Mock TestRequestService service;
    private TestRequestController controller;

    @BeforeEach
    void setUp() {
        controller = new TestRequestController(service);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void listPanelAndReadEndpointsDelegate() {
        UUID id = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 5);
        var pageable = PageRequest.of(0, 10);
        PageResponse<TestRequestResponse> page = mock(PageResponse.class);
        when(service.search(id, departmentId, TestRequestStatus.PENDING, "máu", date, pageable)).thenReturn(page);
        assertSame(page, controller.list(id, departmentId, TestRequestStatus.PENDING, "máu", date, pageable).getBody());

        PageResponse<LabPanelSummaryResponse> panels = mock(PageResponse.class);
        when(service.searchPanels(id, departmentId, TestRequestStatus.PENDING, "máu", date, pageable)).thenReturn(panels);
        assertSame(panels, controller.panels(id, departmentId, TestRequestStatus.PENDING, "máu", date, pageable).getBody());

        TestRequestResponse response = mock(TestRequestResponse.class);
        LabPanelWorkbenchResponse workbench = mock(LabPanelWorkbenchResponse.class);
        TestRequestActionPermissionsResponse permissions = mock(TestRequestActionPermissionsResponse.class);
        when(service.get(id)).thenReturn(response);
        when(service.getPanelWorkbench(id)).thenReturn(workbench);
        when(service.actionPermissions(id)).thenReturn(permissions);
        when(service.listByVisit(visitId)).thenReturn(List.of(response));
        when(service.listByQueueTicket(id)).thenReturn(List.of(response));
        assertSame(response, controller.get(id).getBody());
        assertSame(workbench, controller.panelWorkbench(id).getBody());
        assertSame(permissions, controller.actionPermissions(id).getBody());
        assertEquals(1, controller.listByVisit(visitId).getBody().size());
        assertEquals(1, controller.listByQueue(id).getBody().size());
    }

    @Test
    void panelSavesCreateBatchUpdateCancelAndTraceabilityDelegate() {
        UUID id = UUID.randomUUID();
        LabPanelResultRequest panelRequest = mock(LabPanelResultRequest.class);
        LabPanelWorkbenchResponse workbench = mock(LabPanelWorkbenchResponse.class);
        when(service.savePanelResult(id, panelRequest, false)).thenReturn(workbench);
        when(service.savePanelResult(id, panelRequest, true)).thenReturn(workbench);
        assertSame(workbench, controller.savePanelResult(id, panelRequest).getBody());
        assertSame(workbench, controller.completePanelResult(id, panelRequest).getBody());

        TestRequestResponse response = mock(TestRequestResponse.class);
        lenient().when(response.testRequestId()).thenReturn(id);
        TestRequestCreateRequest create = mock(TestRequestCreateRequest.class);
        when(service.create(create)).thenReturn(response);
        assertEquals(201, controller.create(create).getStatusCode().value());
        TestRequestBatchCreateRequest batch = mock(TestRequestBatchCreateRequest.class);
        when(service.createBatch(batch)).thenReturn(List.of(response));
        assertEquals(1, controller.createBatch(batch).getBody().size());

        TestRequestUpdateRequest update = mock(TestRequestUpdateRequest.class);
        when(update.status()).thenReturn(TestRequestStatus.IN_PROGRESS);
        when(service.update(id, update)).thenReturn(response);
        var nurse = new UsernamePasswordAuthenticationToken("nurse", "x",
                List.of(new SimpleGrantedAuthority("ROLE_NURSE")));
        assertSame(response, controller.update(id, update, nurse).getBody());

        TestRequestCancelRequest cancel = mock(TestRequestCancelRequest.class);
        when(service.cancel(id, cancel)).thenReturn(response);
        assertSame(response, controller.cancel(id, cancel).getBody());
        assertEquals(204, controller.delete(id).getStatusCode().value());
        verify(service).delete(id);
        when(service.findByInvoiceItem(id)).thenReturn(List.of(response));
        when(service.findByInvoice(id)).thenReturn(List.of(response));
        assertEquals(1, controller.findByInvoiceItem(id).getBody().size());
        assertEquals(1, controller.findByInvoice(id).getBody().size());
    }

    @Test
    void onlyDoctorOrAdminAuthorityCanSetCompletedThroughGenericUpdate() {
        UUID id = UUID.randomUUID();
        TestRequestUpdateRequest request = mock(TestRequestUpdateRequest.class);
        when(request.status()).thenReturn(TestRequestStatus.COMPLETED);
        var nurse = new UsernamePasswordAuthenticationToken("nurse", "x",
                List.of(new SimpleGrantedAuthority("ROLE_NURSE")));
        assertThrows(AccessDeniedException.class, () -> controller.update(id, request, nurse));

        TestRequestResponse response = mock(TestRequestResponse.class);
        when(service.update(id, request)).thenReturn(response);
        for (String authority : List.of("ROLE_DOCTOR", "ADMIN", "ROLE_ADMIN", "GENERAL_DOCTOR", "SPECIALIST_DOCTOR")) {
            var doctor = new UsernamePasswordAuthenticationToken(authority, "x",
                    List.of(new SimpleGrantedAuthority(authority)));
            assertSame(response, controller.update(id, request, doctor).getBody());
        }
    }

    @Test
    void resultRevisionAndClinicalFormEndpointsDelegate() {
        UUID id = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        TestResultResponse result = mock(TestResultResponse.class);
        lenient().when(result.resultId()).thenReturn(UUID.randomUUID());
        ResolvedClinicalFormResponse form = mock(ResolvedClinicalFormResponse.class);
        TestResultRevisionResponse revision = mock(TestResultRevisionResponse.class);
        when(service.getResult(id)).thenReturn(result);
        when(service.getClinicalForm(id)).thenReturn(form);
        when(service.resultHistory(id)).thenReturn(List.of(revision));
        assertSame(result, controller.getResult(id).getBody());
        assertSame(form, controller.getClinicalForm(id).getBody());
        assertEquals(1, controller.resultHistory(id).getBody().size());

        TestResultAmendRequest amend = mock(TestResultAmendRequest.class);
        TestResultUpdateRequest update = mock(TestResultUpdateRequest.class);
        when(service.amendResult(id, amend)).thenReturn(revision);
        when(service.updateAmendment(id, revisionId, update)).thenReturn(revision);
        when(service.signAmendment(id, revisionId)).thenReturn(revision);
        assertSame(revision, controller.amendResult(id, amend).getBody());
        assertSame(revision, controller.updateAmendment(id, revisionId, update).getBody());
        assertSame(revision, controller.signAmendment(id, revisionId).getBody());

        TestResultCreateRequest create = mock(TestResultCreateRequest.class);
        when(service.createResult(id, create)).thenReturn(result);
        when(service.completeResult(id, create)).thenReturn(result);
        when(service.updateResult(id, update)).thenReturn(result);
        assertEquals(201, controller.createResult(id, create).getStatusCode().value());
        assertSame(result, controller.updateResult(id, update).getBody());
        assertSame(result, controller.completeResult(id, create).getBody());
    }

    @Test
    void uploadAndAttachmentEndpointsDelegate() throws Exception {
        UUID id = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "result.pdf", "application/pdf", new byte[]{1});
        when(service.uploadResultFile(id, file)).thenReturn("/test-results/result.pdf");
        var upload = controller.uploadResult(id, file).getBody();
        assertEquals("/test-results/result.pdf", upload.get("imageUrl"));
        assertEquals("result.pdf", upload.get("fileName"));

        List<org.springframework.web.multipart.MultipartFile> files = List.of(file);
        TestResultAttachmentResponse attachment = mock(TestResultAttachmentResponse.class);
        when(service.uploadAttachments(id, revisionId, files)).thenReturn(List.of(attachment));
        when(service.listAttachments(id, revisionId)).thenReturn(List.of(attachment));
        assertEquals(1, controller.uploadAttachments(id, revisionId, files).getBody().size());
        assertEquals(1, controller.listAttachments(id, revisionId).getBody().size());
    }
}
