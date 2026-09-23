package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.testrequest.TestRequestCancelRequest;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestRequestAuthorizationServiceTest {
    @Mock TestRequestRepository repo;
    @Mock TestResultRepository resultRepo;
    @Mock MedicalRecordRepository recordRepo;
    @Mock CustomerVisitRepository visitRepo;
    @Mock MedicalServiceRepository serviceRepo;
    @Mock StaffInfoRepository staffRepo;
    @Mock QueueTicketRepository queueTicketRepo;
    @Mock DepartmentRepository departmentRepo;
    @Mock InvoiceItemRepository invoiceItemRepo;
    @Mock MedicalRecordService medicalRecordService;
    @Mock PatientJourneyService patientJourneyService;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock NotificationService notificationService;
    @Mock AuthService authService;
    @Mock ClinicalFormEngine clinicalFormEngine;
    @Mock FixedClinicalFormService fixedClinicalFormService;
    @Mock StaffDutyService staffDutyService;

    @InjectMocks TestRequestService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestingDoctorCanViewButCannotMutateResult() {
        StaffInfo requester = doctor();
        Department department = departmentWithHead(doctor());
        TestRequest request = activeRequest(department, requester);
        stubView(request, requester);

        var permissions = service.actionPermissions(request.getTestRequestId());

        assertTrue(permissions.canView());
        assertFalse(permissions.canEditResult());
        assertFalse(permissions.canUpload());
        assertFalse(permissions.canSign());
        assertFalse(permissions.canCancel());
    }

    @Test
    void responsibleDoctorReceivesAllActiveLabActions() {
        StaffInfo head = doctor();
        Department department = departmentWithHead(head);
        TestRequest request = activeRequest(department, doctor());
        stubView(request, head);

        var permissions = service.actionPermissions(request.getTestRequestId());

        assertTrue(permissions.canEditResult());
        assertTrue(permissions.canUpload());
        assertTrue(permissions.canSign());
        assertTrue(permissions.canCancel());
    }

    @Test
    void assignedNurseCanDraftAndUploadButCannotSignOrCancel() {
        StaffInfo nurse = staff(SystemRole.NURSE);
        Department department = departmentWithHead(doctor());
        nurse.setDepartment(department);
        department.setNurses(new ArrayList<>(java.util.List.of(nurse)));
        TestRequest request = activeRequest(department, doctor());
        stubView(request, nurse);

        var permissions = service.actionPermissions(request.getTestRequestId());

        assertTrue(permissions.canEditResult());
        assertTrue(permissions.canUpload());
        assertFalse(permissions.canSign());
        assertFalse(permissions.canCancel());
    }

    @Test
    void requestingDoctorIsRejectedBeforeResultCanBeSigned() {
        StaffInfo requester = doctor();
        Department department = departmentWithHead(doctor());
        TestRequest request = activeRequest(department, requester);
        stubLockedActor(request, requester);
        when(staffDutyService.requireCurrentStaffOnDuty(department, true))
                .thenThrow(new BadRequestException("Nhân sự không thuộc phòng thực hiện"));

        assertThrows(BadRequestException.class,
                () -> service.completeResult(request.getTestRequestId(), null));
        verifyNoInteractions(resultRepo);
        assertEquals(TestRequestStatus.IN_PROGRESS, request.getStatus());
    }

    @Test
    void requestingDoctorCannotCancelLabRequest() {
        StaffInfo requester = doctor();
        Department department = departmentWithHead(doctor());
        TestRequest request = activeRequest(department, requester);
        stubLockedActor(request, requester);
        when(staffDutyService.requireCurrentStaffOnDuty(department, true))
                .thenThrow(new BadRequestException("Nhân sự không thuộc phòng thực hiện"));

        assertThrows(BadRequestException.class,
                () -> service.cancel(request.getTestRequestId(), new TestRequestCancelRequest("Không thực hiện")));
        verify(repo, never()).save(any());
    }

    @Test
    void responsibleDoctorCanCancelInProgressRequest() {
        StaffInfo head = doctor();
        Department department = departmentWithHead(head);
        TestRequest request = activeRequest(department, doctor());
        stubLockedActor(request, head);
        when(staffDutyService.requireCurrentStaffOnDuty(department, true)).thenReturn(head);
        when(repo.save(request)).thenReturn(request);

        var response = service.cancel(request.getTestRequestId(),
                new TestRequestCancelRequest("Mẫu không đủ điều kiện xử lý"));

        assertEquals(TestRequestStatus.CANCELLED, response.status());
        assertEquals("Mẫu không đủ điều kiện xử lý", request.getCancelReason());
        assertEquals(QueueStatus.DONE, request.getQueueTicket().getStatus());
        verify(queueTicketRepo).save(request.getQueueTicket());
        verify(staffDutyService).requireCurrentStaffOnDuty(department, true);
    }

    @ParameterizedTest
    @EnumSource(value = TestRequestStatus.class, names = {"COMPLETED", "CANCELLED"})
    void finishedRequestsRemainReadableButExposeNoMutationActions(TestRequestStatus status) {
        StaffInfo doctor = doctor();
        TestRequest request = activeRequest(departmentWithHead(doctor), doctor());
        request.setStatus(status);
        stubView(request, doctor);

        var permissions = service.actionPermissions(request.getTestRequestId());

        assertTrue(permissions.canView());
        assertFalse(permissions.canEditResult());
        assertFalse(permissions.canUpload());
        assertFalse(permissions.canSign());
        assertFalse(permissions.canCancel());
    }

    @ParameterizedTest
    @EnumSource(value = QueueStatus.class, names = {"BLOCKED", "WAITING", "CALLED"})
    void resultEntryRequiresPatientToHaveEnteredRoom(QueueStatus status) {
        StaffInfo doctor = doctor();
        TestRequest request = activeRequest(departmentWithHead(doctor), doctor());
        request.getQueueTicket().setStatus(status);
        stubView(request, doctor);

        var permissions = service.actionPermissions(request.getTestRequestId());

        assertTrue(permissions.canView());
        assertFalse(permissions.canEditResult());
        assertFalse(permissions.canUpload());
        assertFalse(permissions.canSign());
        assertTrue(permissions.canCancel());
    }

    @Test
    void unrelatedDoctorCannotViewRequest() {
        StaffInfo stranger = doctor();
        TestRequest request = activeRequest(departmentWithHead(doctor()), doctor());
        stubView(request, stranger);

        assertThrows(AccessDeniedException.class,
                () -> service.actionPermissions(request.getTestRequestId()));
        verifyNoInteractions(resultRepo);
    }

    private void stubView(TestRequest request, StaffInfo actor) {
        when(repo.findById(request.getTestRequestId())).thenReturn(Optional.of(request));
        when(authService.currentStaffId()).thenReturn(actor.getStaffId());
        when(staffRepo.findById(actor.getStaffId())).thenReturn(Optional.of(actor));
    }

    private void stubLockedActor(TestRequest request, StaffInfo actor) {
        when(repo.findByIdForUpdate(request.getTestRequestId())).thenReturn(Optional.of(request));
        when(departmentRepo.findByIdForUpdate(request.getPerformingDepartment().getDepartmentId()))
                .thenReturn(Optional.of(request.getPerformingDepartment()));
        when(authService.currentStaffId()).thenReturn(actor.getStaffId());
    }

    private TestRequest activeRequest(Department department, StaffInfo requester) {
        QueueTicket queue = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.IN_PROGRESS)
                .build();
        return TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .performingDepartment(department)
                .requestedBy(requester)
                .queueTicket(queue)
                .status(TestRequestStatus.IN_PROGRESS)
                .build();
    }

    private Department departmentWithHead(StaffInfo head) {
        Department department = Department.builder()
                .departmentId(UUID.randomUUID())
                .roomCode("LAB-" + UUID.randomUUID())
                .name("Phòng xét nghiệm " + UUID.randomUUID())
                .headDoctor(head)
                .nurses(new ArrayList<>())
                .build();
        head.setDepartment(department);
        return department;
    }

    private StaffInfo doctor() {
        return staff(SystemRole.DOCTOR);
    }

    private StaffInfo staff(SystemRole role) {
        return StaffInfo.builder()
                .staffId(UUID.randomUUID())
                .staffCode("STF-" + UUID.randomUUID())
                .systemRole(role)
                .build();
    }
}
