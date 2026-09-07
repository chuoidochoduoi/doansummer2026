package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.testrequest.*;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.*;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestRequestWorkflowTest {
    @Mock TestRequestRepository repo;
    @Mock TestResultRepository resultRepo;
    @Mock TestResultRevisionRepository revisionRepo;
    @Mock TestResultAttachmentRepository attachmentRepo;
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
    @Mock ClinicalFormTemplateService clinicalFormTemplateService;
    @Mock ClinicalFormEngine clinicalFormEngine;
    @Mock SameDayParaclinicalResultService sameDayParaclinicalResultService;
    @Mock StaffDutyService staffDutyService;
    @Mock MedicalServiceSelectionPolicyService serviceSelectionPolicyService;
    @InjectMocks TestRequestService service;
    final Department room = Department.builder().departmentId(UUID.randomUUID()).name("Phòng xét nghiệm")
            .departmentType(DepartmentType.PARACLINICAL).status(DepartmentStatus.AVAILABLE).build();
    final MedicalService lab = MedicalService.builder().serviceId(UUID.randomUUID()).name("Đường huyết")
            .serviceCode("LAB-002").department(room).departmentType(DepartmentType.PARACLINICAL).build();
    final CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(Profile.builder().fullName("Nguyễn Minh An").build()).build();
    final StaffInfo doctor = StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(SystemRole.DOCTOR).build();
    final MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID()).visit(visit).doctor(doctor)
            .queueTicket(QueueTicket.builder().ticketId(UUID.randomUUID()).build()).build();
    TestRequest request(TestRequestStatus status) {
        return TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record).service(lab)
                .performingDepartment(room).requestedBy(doctor).status(status).build();
    }
    void save() { when(repo.save(any())).thenAnswer(call -> call.getArgument(0)); }

    @ParameterizedTest @EnumSource(TestRequestStatus.class)
    void startChangesOnlyPendingRequests(TestRequestStatus status) {
        TestRequest request=request(status); UUID id=UUID.randomUUID();
        when(repo.findAllByQueueTicket_TicketId(id)).thenReturn(List.of(request));
        service.startRequestsForQueue(id);
        assertEquals(status==TestRequestStatus.PENDING?TestRequestStatus.IN_PROGRESS:status,request.getStatus());
        verify(repo,times(status==TestRequestStatus.PENDING?1:0)).save(request);
    }
    @ParameterizedTest @EnumSource(TestRequestStatus.class)
    void absenceBlocksOnlyPendingRequests(TestRequestStatus status) {
        TestRequest request=request(status); UUID id=UUID.randomUUID();
        when(repo.findAllByQueueTicket_TicketId(id)).thenReturn(List.of(request)); service.blockRequestsForQueue(id);
        assertEquals(status==TestRequestStatus.PENDING?TestRequestStatus.BLOCKED:status,request.getStatus());
        verify(repo,times(status==TestRequestStatus.PENDING?1:0)).save(request);
    }
    @ParameterizedTest @CsvSource({"BLOCKED,true,BLOCKED","BLOCKED,false,PENDING","COMPLETED,false,COMPLETED","CANCELLED,false,CANCELLED","IN_PROGRESS,false,IN_PROGRESS","PENDING,false,PENDING"})
    void returningPatientDoesNotResurrectCompletedOrCancelledTests(TestRequestStatus initial,boolean blocked,TestRequestStatus expected) {
        TestRequest request=request(initial); UUID id=UUID.randomUUID();
        when(repo.findAllByQueueTicket_TicketId(id)).thenReturn(List.of(request)); service.restoreRequestsForQueue(id,blocked);
        assertEquals(expected,request.getStatus()); verify(repo,times(initial==TestRequestStatus.BLOCKED?1:0)).save(request);
    }
    @ParameterizedTest @ValueSource(longs={0,1,4})
    void unfinishedCountIncludesBlockedPendingAndInProgress(long count) {
        when(repo.countByMedicalRecordAndStatusIn(record.getRecordId(),List.of(TestRequestStatus.BLOCKED,TestRequestStatus.PENDING,TestRequestStatus.IN_PROGRESS))).thenReturn(count);
        assertEquals(count>0,service.hasIncompleteRequestsForRecord(record.getRecordId()));
    }

    @ParameterizedTest @ValueSource(strings={"attach","alreadyAttached","cancelled","otherRecord","wrongVisit","missing"})
    void prepaidRequestAttachmentDoesNotCreateDuplicateOrCrossVisits(String state) {
        when(recordRepo.findById(record.getRecordId())).thenReturn(Optional.of(record));
        if(state.equals("wrongVisit")) {
            assertThrows(BadRequestException.class, () -> service.attachPrepaidRequestToExamination(UUID.randomUUID(),record.getRecordId(),lab.getServiceId(),doctor.getStaffId(),"Ghi chú"));
        } else {
            TestRequest existing=request(state.equals("cancelled")?TestRequestStatus.CANCELLED:TestRequestStatus.PENDING);
            if(!state.equals("alreadyAttached")) existing.setMedicalRecord(MedicalRecord.builder().recordId(UUID.randomUUID()).visit(visit)
                    .queueTicket(state.equals("otherRecord")?new QueueTicket():null).build());
            when(repo.findAllByVisitIdWithDetails(visit.getVisitId())).thenReturn(state.equals("missing")?List.of():List.of(existing));
            if(state.equals("attach")) when(staffRepo.findById(doctor.getStaffId())).thenReturn(Optional.of(doctor));
            if(state.equals("otherRecord")) assertThrows(ConflictException.class, () -> service.attachPrepaidRequestToExamination(visit.getVisitId(),record.getRecordId(),lab.getServiceId(),doctor.getStaffId(),null));
            else {
                assertEquals(Set.of("attach","alreadyAttached").contains(state),service.attachPrepaidRequestToExamination(visit.getVisitId(),record.getRecordId(),lab.getServiceId(),doctor.getStaffId(),"  Ghi chú  "));
                if(state.equals("attach")) { assertSame(record,existing.getMedicalRecord()); assertSame(doctor,existing.getRequestedBy()); assertEquals("Ghi chú",existing.getDescription()); verify(repo).save(existing); }
            }
        }
        if(!state.equals("attach")) verify(repo,never()).save(any());
        verifyNoInteractions(invoiceItemRepo,queueTicketRepo,medicalRecordService);
    }

    @ParameterizedTest @CsvSource({"BLOCKED,PENDING,BLOCKED,true","WAITING,BLOCKED,PENDING,true","CALLED,BLOCKED,PENDING,true","IN_PROGRESS,BLOCKED,PENDING,true","WAITING,PENDING,PENDING,false","DONE,COMPLETED,COMPLETED,false","BLOCKED,COMPLETED,COMPLETED,false"})
    void repeatedPaymentSynchronizesPendingRequestWithoutDuplicatingOrResettingCompletedResult(
            QueueStatus queueStatus,TestRequestStatus initial,TestRequestStatus expected,boolean writes) {
        UUID itemId=UUID.randomUUID(); TestRequest existing=request(initial);
        existing.setInvoiceItem(InvoiceItem.builder().itemId(itemId).build());
        QueueTicket queue=QueueTicket.builder().ticketId(UUID.randomUUID()).status(queueStatus).build(); existing.setQueueTicket(queue);
        when(serviceRepo.findById(lab.getServiceId())).thenReturn(Optional.of(lab)); when(visitRepo.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(repo.findTopByInvoiceItem_ItemIdOrderByCreatedAtAsc(itemId)).thenReturn(Optional.of(existing));
        when(staffRepo.findById(doctor.getStaffId())).thenReturn(Optional.of(doctor)); if(writes) save();
        var result=service.createFromPaidInvoice(visit.getVisitId(),null,lab.getServiceId(),doctor.getStaffId(),null,itemId);
        assertEquals(existing.getTestRequestId(),result.testRequestId()); assertEquals(expected,result.status()); assertSame(record,existing.getMedicalRecord());
        verify(repo,times(writes?1:0)).save(existing); verifyNoInteractions(medicalRecordService,queueTicketRepo);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void paidExistingRequestWithoutQueueIsRepairedAndOriginalRecordPreserved(boolean blocked) {
        UUID itemId=UUID.randomUUID(); TestRequest existing=request(TestRequestStatus.PENDING);
        existing.setInvoiceItem(InvoiceItem.builder().itemId(itemId).build());
        QueueTicket queue=QueueTicket.builder().ticketId(UUID.randomUUID()).department(room).status(blocked?QueueStatus.BLOCKED:QueueStatus.WAITING).build();
        when(serviceRepo.findById(lab.getServiceId())).thenReturn(Optional.of(lab)); when(visitRepo.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(repo.findTopByInvoiceItem_ItemIdOrderByCreatedAtAsc(itemId)).thenReturn(Optional.of(existing));
        when(staffRepo.findById(doctor.getStaffId())).thenReturn(Optional.of(doctor));
        when(visitRepo.findByIdForUpdate(visit.getVisitId())).thenReturn(Optional.of(visit)); when(departmentRepo.findByIdForUpdate(room.getDepartmentId())).thenReturn(Optional.of(room));
        when(queueTicketRepo.findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(visit.getVisitId(),room.getDepartmentId(),List.of(QueueStatus.DONE,QueueStatus.SKIPPED))).thenReturn(Optional.of(queue)); save();
        var result=service.createFromPaidInvoice(visit.getVisitId(),UUID.randomUUID(),lab.getServiceId(),doctor.getStaffId(),null,itemId);
        assertEquals(queue.getTicketId(),result.queueTicketId()); assertEquals(blocked?TestRequestStatus.BLOCKED:TestRequestStatus.PENDING,result.status());
        assertSame(record,existing.getMedicalRecord()); verify(queueTicketRepo,never()).save(any()); verifyNoInteractions(medicalRecordService);
        var order=inOrder(visitRepo,departmentRepo); order.verify(visitRepo).findById(visit.getVisitId()); order.verify(visitRepo).findByIdForUpdate(visit.getVisitId()); order.verify(departmentRepo).findByIdForUpdate(room.getDepartmentId());
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void paidNewRequestCreatesOnlyOneQueueWithWorkflowState(boolean activeStep) {
        when(serviceRepo.findById(lab.getServiceId())).thenReturn(Optional.of(lab)); when(visitRepo.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(recordRepo.findById(record.getRecordId())).thenReturn(Optional.of(record)); when(staffRepo.findById(doctor.getStaffId())).thenReturn(Optional.of(doctor));
        when(visitRepo.findByIdForUpdate(visit.getVisitId())).thenReturn(Optional.of(visit)); when(departmentRepo.findByIdForUpdate(room.getDepartmentId())).thenReturn(Optional.of(room));
        when(patientJourneyService.hasActiveStep(visit.getVisitId())).thenReturn(activeStep);
        LocalDate today=LocalDate.of(2026,9,4); ZoneId zone=ZoneId.of("Asia/Ho_Chi_Minh");
        when(queueTicketRepo.findMaxQueueNumberForDay(room.getDepartmentId(),today)).thenReturn(Optional.of(8));
        when(queueTicketRepo.save(any())).thenAnswer(call -> {QueueTicket q=call.getArgument(0); q.setTicketId(UUID.randomUUID()); return q;}); save();
        try(var dates=mockStatic(LocalDate.class,CALLS_REAL_METHODS)) {
            dates.when(() -> LocalDate.now(zone)).thenReturn(today);
            var result=service.createFromPaidInvoice(visit.getVisitId(),record.getRecordId(),lab.getServiceId(),doctor.getStaffId(),"Ghi chú",null);
            assertEquals(activeStep?TestRequestStatus.BLOCKED:TestRequestStatus.PENDING,result.status()); assertEquals(9,result.queueNumber()); assertEquals(record.getRecordId(),result.medicalRecordId());
        }
        ArgumentCaptor<QueueTicket> queue=ArgumentCaptor.forClass(QueueTicket.class); verify(queueTicketRepo).save(queue.capture());
        assertEquals(today,queue.getValue().getWorkDate()); assertSame(visit,queue.getValue().getVisit());
    }

    @ParameterizedTest @ValueSource(strings={"create","update","complete","upload","attachments","updateAmendment","signAmendment"})
    void cancelledRequestCannotWriteOrSignResults(String operation) {
        TestRequest cancelled=request(TestRequestStatus.CANCELLED);
        cancelled.setQueueTicket(QueueTicket.builder().ticketId(UUID.randomUUID()).department(room).status(QueueStatus.DONE).build());
        lenient().when(repo.findById(cancelled.getTestRequestId())).thenReturn(Optional.of(cancelled));
        lenient().when(repo.findByIdForUpdate(cancelled.getTestRequestId())).thenReturn(Optional.of(cancelled));
        lenient().when(authService.currentStaffId()).thenReturn(doctor.getStaffId());
        lenient().when(staffRepo.findById(doctor.getStaffId())).thenReturn(Optional.of(doctor));
        lenient().when(departmentRepo.findByIdForUpdate(room.getDepartmentId())).thenReturn(Optional.of(room));
        lenient().when(staffDutyService.requireCurrentStaffOnDuty(room,true)).thenReturn(doctor);
        var values=new tools.jackson.databind.json.JsonMapper().readTree("{}");
        var version=ClinicalFormTemplateVersion.builder().versionId(UUID.randomUUID()).build();
        if(!operation.equals("create")) {
            TestResult existing=TestResult.builder().resultId(UUID.randomUUID()).testRequest(cancelled)
                    .performedBy(doctor).conclusion("Kết quả nháp trước khi hủy").resultData(values).formTemplateVersion(version).build();
            cancelled.setTestResult(existing);
            lenient().when(resultRepo.findByTestRequest_TestRequestId(cancelled.getTestRequestId())).thenReturn(Optional.of(existing));
        }
        lenient().when(clinicalFormTemplateService.resolveVersion(eq(lab.getServiceId()),any())).thenReturn(version);
        lenient().when(clinicalFormTemplateService.schemaForService(lab.getServiceCode(),version)).thenReturn(values);
        lenient().when(clinicalFormEngine.validateAndEnrich(eq(values),eq(values),isNull(),isNull(),any(LocalDate.class),eq(true))).thenReturn(values);
        lenient().when(resultRepo.save(any())).thenAnswer(call -> {TestResult r=call.getArgument(0); if(r.getResultId()==null) r.setResultId(UUID.randomUUID()); return r;});
        lenient().when(revisionRepo.save(any())).thenAnswer(call -> call.getArgument(0));
        var payload=new org.example.doansummer2026.dto.testresult.TestResultCreateRequest(cancelled.getTestRequestId(),null,"Kết luận",null,null,null,doctor.getStaffId());
        assertAll(
                () -> assertThrows(ConflictException.class, () -> {
                    switch(operation) {
                        case "create" -> service.createResult(cancelled.getTestRequestId(),payload);
                        case "update" -> service.updateResult(cancelled.getTestRequestId(),new org.example.doansummer2026.dto.testresult.TestResultUpdateRequest(null,"Kết luận mới",null,null,null,false));
                        case "upload" -> service.uploadResultFile(cancelled.getTestRequestId(),null);
                        case "attachments" -> service.uploadAttachments(cancelled.getTestRequestId(),UUID.randomUUID(),List.of());
                        case "updateAmendment" -> service.updateAmendment(cancelled.getTestRequestId(),UUID.randomUUID(),new org.example.doansummer2026.dto.testresult.TestResultUpdateRequest(null,"Kết luận mới",null,null,null,false));
                        case "signAmendment" -> service.signAmendment(cancelled.getTestRequestId(),UUID.randomUUID());
                        default -> service.completeResult(cancelled.getTestRequestId(),payload);
                    }
                }),
                () -> verify(resultRepo,never()).save(any()),
                () -> verify(revisionRepo,never()).save(any()),
                () -> verifyNoInteractions(attachmentRepo),
                () -> assertEquals(TestRequestStatus.CANCELLED,cancelled.getStatus()));
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void cancelledPanelMemberRejectsWholeBatchBeforeAnyResultWrite(boolean complete) {
        lab.setServiceCode("AN-CBC-RBC");
        TestRequest active=request(TestRequestStatus.IN_PROGRESS);
        TestRequest cancelled=request(TestRequestStatus.CANCELLED);
        cancelled.setService(MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("AN-CBC-HGB").build());
        QueueTicket queue=QueueTicket.builder().ticketId(UUID.randomUUID()).department(room).status(QueueStatus.IN_PROGRESS).build();
        active.setQueueTicket(queue); cancelled.setQueueTicket(queue);
        when(repo.findById(active.getTestRequestId())).thenReturn(Optional.of(active));
        when(authService.currentStaffId()).thenReturn(doctor.getStaffId());
        when(repo.findAllByQueueTicket_TicketId(queue.getTicketId())).thenReturn(List.of(active,cancelled));
        assertThrows(ConflictException.class, () -> service.savePanelResult(active.getTestRequestId(),null,complete));
        verifyNoInteractions(resultRepo,revisionRepo,attachmentRepo,clinicalFormEngine);
        verify(repo,never()).save(any());
        assertEquals(TestRequestStatus.CANCELLED,cancelled.getStatus());
        assertEquals(TestRequestStatus.IN_PROGRESS,active.getStatus());
    }

    @ParameterizedTest @ValueSource(strings={"PENDING","IN_PROGRESS"})
    void draftResultPreservesRequestAndCreatesUnsignedRevision(String status) {
        TestRequest target=request(TestRequestStatus.valueOf(status));
        target.setQueueTicket(QueueTicket.builder().ticketId(UUID.randomUUID()).status(QueueStatus.IN_PROGRESS).build());
        when(repo.findById(target.getTestRequestId())).thenReturn(Optional.of(target));
        when(authService.currentStaffId()).thenReturn(doctor.getStaffId());
        when(staffRepo.findById(doctor.getStaffId())).thenReturn(Optional.of(doctor));
        when(resultRepo.save(any())).thenAnswer(call -> {TestResult result=call.getArgument(0); result.setResultId(UUID.randomUUID()); return result;});
        when(revisionRepo.save(any())).thenAnswer(call -> call.getArgument(0));
        var payload=new org.example.doansummer2026.dto.testresult.TestResultCreateRequest(target.getTestRequestId(),null,"Kết quả đang nhập",null,null,null,doctor.getStaffId());
        var response=service.createResult(target.getTestRequestId(),payload);
        assertNotNull(response);
        ArgumentCaptor<TestResult> result=ArgumentCaptor.forClass(TestResult.class);
        verify(resultRepo).save(result.capture());
        assertSame(target,result.getValue().getTestRequest());
        assertSame(doctor,result.getValue().getPerformedBy());
        assertNull(result.getValue().getVerifiedAt());
        assertEquals("Kết quả đang nhập",result.getValue().getConclusion());
        ArgumentCaptor<TestResultRevision> revision=ArgumentCaptor.forClass(TestResultRevision.class);
        verify(revisionRepo).save(revision.capture());
        assertEquals(TestResultRevisionStatus.DRAFT,revision.getValue().getStatus());
        assertEquals(1,revision.getValue().getRevisionNo());
        assertNull(revision.getValue().getSignedAt());
        assertEquals(TestRequestStatus.IN_PROGRESS,target.getStatus());
        verify(repo,times(status.equals("PENDING")?1:0)).save(target);
        verifyNoInteractions(queueTicketRepo,patientJourneyService);
    }

    @ParameterizedTest @EnumSource(value=TestRequestStatus.class, names={"COMPLETED"}, mode=EnumSource.Mode.EXCLUDE)
    void initialDraftCannotBeSignedThroughAmendmentEndpoint(TestRequestStatus status) {
        TestRequest target=request(status);
        target.setQueueTicket(QueueTicket.builder().ticketId(UUID.randomUUID()).department(room).status(QueueStatus.IN_PROGRESS).build());
        var data=new tools.jackson.databind.json.JsonMapper().readTree("{\"glucose\":5.2}");
        var version=ClinicalFormTemplateVersion.builder().versionId(UUID.randomUUID()).build();
        TestResult result=TestResult.builder().resultId(UUID.randomUUID()).testRequest(target).performedBy(doctor)
                .resultData(data).formTemplateVersion(version).conclusion("Trong khoảng tham chiếu").build();
        target.setTestResult(result);
        TestResultRevision draft=TestResultRevision.builder().revisionId(UUID.randomUUID()).revisionNo(1)
                .testResult(result).status(TestResultRevisionStatus.DRAFT).resultData(data).templateVersion(version)
                .conclusion("Trong khoảng tham chiếu").enteredBy(doctor).build();
        when(repo.findByIdForUpdate(target.getTestRequestId())).thenReturn(Optional.of(target));
        when(authService.currentStaffId()).thenReturn(doctor.getStaffId());
        when(departmentRepo.findByIdForUpdate(room.getDepartmentId())).thenReturn(Optional.of(room));
        when(staffDutyService.requireCurrentStaffOnDuty(room,true)).thenReturn(doctor);
        lenient().when(revisionRepo.findById(draft.getRevisionId())).thenReturn(Optional.of(draft));
        lenient().when(clinicalFormTemplateService.schemaForService(lab.getServiceCode(),version)).thenReturn(data);
        lenient().when(clinicalFormEngine.validateAndEnrich(eq(data),eq(data),isNull(),isNull(),any(LocalDate.class),eq(true))).thenReturn(data);
        lenient().when(revisionRepo.save(any())).thenAnswer(call -> call.getArgument(0));
        assertAll(
                () -> assertThrows(ConflictException.class, () -> service.signAmendment(target.getTestRequestId(),draft.getRevisionId())),
                () -> assertEquals(TestResultRevisionStatus.DRAFT,draft.getStatus()),
                () -> assertNull(result.getVerifiedAt()),
                () -> verify(resultRepo,never()).save(any()),
                () -> verify(revisionRepo,never()).save(any()));
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void completedResultAllowsOnlyAmendmentDraft(boolean sign) {
        TestRequest target=request(TestRequestStatus.COMPLETED);
        var data=new tools.jackson.databind.json.JsonMapper().readTree("{\"glucose\":5.2}");
        var version=ClinicalFormTemplateVersion.builder().versionId(UUID.randomUUID()).build();
        TestResult result=TestResult.builder().resultId(UUID.randomUUID()).testRequest(target).performedBy(doctor)
                .conclusion("Kết luận cũ").resultData(data).formTemplateVersion(version).build();
        TestResultRevision draft=TestResultRevision.builder().revisionId(UUID.randomUUID()).revisionNo(2)
                .testResult(result).status(TestResultRevisionStatus.DRAFT).amendmentReason("Bổ sung kết luận")
                .conclusion("Kết luận mới").resultData(data).templateVersion(version).build();
        when(repo.findByIdForUpdate(target.getTestRequestId())).thenReturn(Optional.of(target));
        when(authService.currentStaffId()).thenReturn(doctor.getStaffId());
        when(departmentRepo.findByIdForUpdate(room.getDepartmentId())).thenReturn(Optional.of(room));
        when(staffDutyService.requireCurrentStaffOnDuty(room,true)).thenReturn(doctor);
        when(revisionRepo.findById(draft.getRevisionId())).thenReturn(Optional.of(draft));
        when(revisionRepo.save(any())).thenAnswer(call -> call.getArgument(0));
        if(sign) {
            var previous=TestResultRevision.builder().status(TestResultRevisionStatus.SIGNED).revisionNo(1).testResult(result).build();
            when(revisionRepo.findFirstByTestResult_ResultIdAndStatusOrderByRevisionNoDesc(result.getResultId(),TestResultRevisionStatus.SIGNED)).thenReturn(Optional.of(previous));
            when(clinicalFormTemplateService.schemaForService(lab.getServiceCode(),version)).thenReturn(data);
            when(clinicalFormEngine.validateAndEnrich(eq(data),eq(data),isNull(),isNull(),any(LocalDate.class),eq(true))).thenReturn(data);
            service.signAmendment(target.getTestRequestId(),draft.getRevisionId());
            assertEquals(TestResultRevisionStatus.SUPERSEDED,previous.getStatus());
            assertEquals(TestResultRevisionStatus.SIGNED,draft.getStatus());
            assertSame(doctor,draft.getSignedBy()); assertNotNull(draft.getSignedAt());
            assertEquals("Kết luận mới",result.getConclusion()); verify(resultRepo).save(result);
        } else {
            service.updateAmendment(target.getTestRequestId(),draft.getRevisionId(),new org.example.doansummer2026.dto.testresult.TestResultUpdateRequest(null,"Kết luận bổ sung",null,null,null,false));
            assertEquals("Kết luận bổ sung",draft.getConclusion());
            assertEquals(TestResultRevisionStatus.DRAFT,draft.getStatus());
            assertEquals("Kết luận cũ",result.getConclusion()); verify(resultRepo,never()).save(any());
        }
        assertEquals(TestRequestStatus.COMPLETED,target.getStatus());
        verifyNoInteractions(queueTicketRepo,patientJourneyService);
    }

    @ParameterizedTest @CsvSource({"update,missing","sign,missing","update,foreign","sign,foreign","update,signed","sign,signed","update,initial","sign,initial","update,blank","sign,blank","sign,conclusion"})
    void amendmentRejectsInvalidRevisionWithoutWriting(String operation,String reason) {
        TestRequest target=request(TestRequestStatus.COMPLETED);
        var result=TestResult.builder().resultId(UUID.randomUUID()).testRequest(target).build();
        var draft=TestResultRevision.builder().revisionId(UUID.randomUUID()).testResult(result)
                .status(TestResultRevisionStatus.DRAFT).amendmentReason("Sửa kết luận").conclusion("Kết luận mới").build();
        if(reason.equals("foreign")) result.setTestRequest(request(TestRequestStatus.COMPLETED));
        if(reason.equals("signed")) draft.setStatus(TestResultRevisionStatus.SIGNED);
        if(reason.equals("initial")) draft.setAmendmentReason(null);
        if(reason.equals("blank")) draft.setAmendmentReason(" ");
        if(reason.equals("conclusion")) draft.setConclusion(" ");
        when(repo.findByIdForUpdate(target.getTestRequestId())).thenReturn(Optional.of(target));
        when(authService.currentStaffId()).thenReturn(doctor.getStaffId());
        when(departmentRepo.findByIdForUpdate(room.getDepartmentId())).thenReturn(Optional.of(room));
        when(staffDutyService.requireCurrentStaffOnDuty(room,true)).thenReturn(doctor);
        when(revisionRepo.findById(draft.getRevisionId())).thenReturn(reason.equals("missing")?Optional.empty():Optional.of(draft));
        Class<? extends RuntimeException> error=reason.equals("missing")?ResourceNotFoundException.class:
                Set.of("foreign","conclusion").contains(reason)?BadRequestException.class:ConflictException.class;
        assertThrows(error, () -> {
            if(operation.equals("sign")) service.signAmendment(target.getTestRequestId(),draft.getRevisionId());
            else service.updateAmendment(target.getTestRequestId(),draft.getRevisionId(),new org.example.doansummer2026.dto.testresult.TestResultUpdateRequest(null,"Không được lưu",null,null,null,false));
        });
        verify(revisionRepo,never()).save(any()); verifyNoInteractions(resultRepo,clinicalFormEngine,queueTicketRepo);
    }

    @ParameterizedTest @CsvSource({"REJECTED,,false","RECOLLECT,,false","ACCEPTED,REJECTED,false","ACCEPTED,RECOLLECT,false","REJECTED,ACCEPTED,true","RECOLLECT,ACCEPTED,true","ACCEPTED,,true"})
    void signingCannotIgnorePreviouslyRejectedSpecimen(SpecimenStatus specimenStatus, SpecimenStatus submittedStatus, boolean allowed) {
        lab.setRequiresSpecimen(true);
        TestRequest target=request(TestRequestStatus.IN_PROGRESS);
        target.setQueueTicket(QueueTicket.builder().ticketId(UUID.randomUUID()).department(room).status(QueueStatus.IN_PROGRESS).build());
        var data=new tools.jackson.databind.json.JsonMapper().readTree("{\"glucose\":5.2}");
        var version=ClinicalFormTemplateVersion.builder().versionId(UUID.randomUUID()).build();
        TestResult result=TestResult.builder().resultId(UUID.randomUUID()).testRequest(target).performedBy(doctor)
                .resultData(data).formTemplateVersion(version).conclusion("Kết quả nháp")
                .sampleId("SMP-DEMO").sampleType(SpecimenType.BLOOD).sampleStatus(specimenStatus)
                .collectedAt(LocalDateTime.of(2026,9,4,8,0)).collectedBy(doctor).build();
        target.setTestResult(result);
        when(repo.findByIdForUpdate(target.getTestRequestId())).thenReturn(Optional.of(target));
        when(authService.currentStaffId()).thenReturn(doctor.getStaffId());
        when(departmentRepo.findByIdForUpdate(room.getDepartmentId())).thenReturn(Optional.of(room));
        when(staffDutyService.requireCurrentStaffOnDuty(room,true)).thenReturn(doctor);
        when(clinicalFormTemplateService.resolveVersion(lab.getServiceId(),version.getVersionId())).thenReturn(version);
        when(clinicalFormTemplateService.schemaForService(lab.getServiceCode(),version)).thenReturn(data);
        when(clinicalFormEngine.validateAndEnrich(eq(data),eq(data),isNull(),isNull(),any(LocalDate.class),eq(true))).thenReturn(data);
        lenient().when(revisionRepo.save(any())).thenAnswer(call -> call.getArgument(0));
        var payload=new org.example.doansummer2026.dto.testresult.TestResultCreateRequest(target.getTestRequestId(),null,"Kết luận",null,null,submittedStatus,doctor.getStaffId());
        if (allowed) {
            service.completeResult(target.getTestRequestId(),payload);
            assertEquals(SpecimenStatus.ACCEPTED,result.getSampleStatus());
            assertEquals(TestRequestStatus.COMPLETED,target.getStatus());
            assertEquals(QueueStatus.DONE,target.getQueueTicket().getStatus());
            assertSame(doctor,result.getVerifiedBy()); assertNotNull(result.getVerifiedAt());
            verify(resultRepo).save(result);
            ArgumentCaptor<TestResultRevision> revision=ArgumentCaptor.forClass(TestResultRevision.class);
            verify(revisionRepo,times(2)).save(revision.capture());
            assertEquals(TestResultRevisionStatus.SIGNED,revision.getValue().getStatus());
            verify(patientJourneyService).refreshWaitingExaminationsAfterTestCompletion(visit.getVisitId());
        } else assertAll(
                () -> assertThrows(BadRequestException.class, () -> service.completeResult(target.getTestRequestId(),payload)),
                () -> assertEquals(TestRequestStatus.IN_PROGRESS,target.getStatus()),
                () -> assertNull(result.getVerifiedAt()),
                () -> verify(resultRepo,never()).save(any()),
                () -> verify(revisionRepo,never()).save(any()));
    }

    @Test void createUsesExactServiceRecordAndInvoiceItemWithoutStartingLabQueue() {
        UUID itemId=UUID.randomUUID(); InvoiceItem item=InvoiceItem.builder().itemId(itemId).build();
        when(recordRepo.findById(record.getRecordId())).thenReturn(Optional.of(record)); when(serviceRepo.findById(lab.getServiceId())).thenReturn(Optional.of(lab));
        when(staffRepo.findById(doctor.getStaffId())).thenReturn(Optional.of(doctor)); when(invoiceItemRepo.findById(itemId)).thenReturn(Optional.of(item)); save();
        var result=service.create(new TestRequestCreateRequest(record.getRecordId(),lab.getServiceId(),doctor.getStaffId(),"Nhịn ăn",itemId));
        assertEquals(record.getRecordId(),result.medicalRecordId()); assertEquals(itemId,result.invoiceItemId()); assertEquals(TestRequestStatus.PENDING,result.status());
        assertEquals("Nhịn ăn",result.description()); verifyNoInteractions(queueTicketRepo);
        verify(serviceSelectionPolicyService).validateAgainstExisting(List.of(lab.getServiceId()),List.of());
    }

    @Test void repeatedCreateForSameInvoiceItemReturnsExistingRequest() {
        UUID itemId=UUID.randomUUID(); TestRequest existing=request(TestRequestStatus.COMPLETED);
        when(repo.findByInvoiceItem_ItemId(itemId)).thenReturn(List.of(existing));
        assertEquals(existing.getTestRequestId(),service.create(new TestRequestCreateRequest(record.getRecordId(),lab.getServiceId(),doctor.getStaffId(),null,itemId)).testRequestId());
        verify(repo,never()).save(any()); verifyNoInteractions(recordRepo,serviceRepo,notificationService);
    }

    @Test
    void panelSearchGroupsPurchasedAnalytesAndKeepsStandaloneServicesSeparate() {
        StaffInfo roomStaff=StaffInfo.builder().staffId(UUID.randomUUID()).department(room).build();
        when(authService.currentStaffId()).thenReturn(roomStaff.getStaffId());
        when(staffRepo.findById(roomStaff.getStaffId())).thenReturn(Optional.of(roomStaff));
        QueueTicket shared=QueueTicket.builder().ticketId(UUID.randomUUID()).queueNumber(8)
                .status(QueueStatus.IN_PROGRESS).department(room).visit(visit).build();
        MedicalService rbc=MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("AN-CBC-RBC")
                .name("RBC").department(room).build();
        MedicalService hgb=MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("AN-CBC-HGB")
                .name("HGB").department(room).build();
        TestRequest first=request(TestRequestStatus.COMPLETED); first.setService(rbc); first.setQueueTicket(shared);
        first.setCreatedAt(LocalDateTime.of(2026,9,5,9,0));
        TestRequest second=request(TestRequestStatus.IN_PROGRESS); second.setService(hgb); second.setQueueTicket(shared);
        second.setCreatedAt(LocalDateTime.of(2026,9,5,9,5));
        TestRequest standalone=request(TestRequestStatus.PENDING);
        standalone.setService(MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("IMG-001")
                .name("Siêu âm").department(room).build());
        standalone.setQueueTicket(null); standalone.setCreatedAt(null);
        when(repo.search(isNull(),eq(room.getDepartmentId()),isNull(),eq("máu"),isNull(),eq(Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of(first,second,standalone)));

        var result=service.searchPanels(null,room.getDepartmentId(),null,"  MÁU  ",null,PageRequest.of(0,1));

        assertAll(() -> assertEquals(2,result.totalElements()), () -> assertEquals(2,result.totalPages()),
                () -> assertEquals(1,result.content().size()), () -> assertTrue(result.first()),
                () -> assertFalse(result.last()), () -> assertEquals("Công thức máu",result.content().get(0).panelName()),
                () -> assertEquals(2,result.content().get(0).purchasedCount()),
                () -> assertEquals(1,result.content().get(0).completedCount()),
                () -> assertEquals(TestRequestStatus.IN_PROGRESS,result.content().get(0).status()));
    }

    @Test
    void panelSearchRecognizesFullPanelAndHandlesEmptyUnpagedResult() {
        StaffInfo roomStaff=StaffInfo.builder().staffId(UUID.randomUUID()).department(room).build();
        when(authService.currentStaffId()).thenReturn(roomStaff.getStaffId());
        when(staffRepo.findById(roomStaff.getStaffId())).thenReturn(Optional.of(roomStaff));
        MedicalService fullPanel=MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("LAB-001")
                .name("Công thức máu").department(room).build();
        TestRequest completed=request(TestRequestStatus.COMPLETED); completed.setService(fullPanel);
        completed.setQueueTicket(QueueTicket.builder().ticketId(UUID.randomUUID()).status(QueueStatus.DONE)
                .department(room).visit(visit).build());
        when(repo.search(isNull(),eq(room.getDepartmentId()),eq(TestRequestStatus.COMPLETED),eq(""),isNull(),eq(Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of(completed)));
        var full=service.searchPanels(null,room.getDepartmentId(),TestRequestStatus.COMPLETED,null,null,Pageable.unpaged());
        assertAll(() -> assertEquals(1,full.content().size()),
                () -> assertEquals(24,full.content().get(0).purchasedCount()),
                () -> assertEquals(24,full.content().get(0).completedCount()),
                () -> assertTrue(full.content().get(0).grouped()));

        when(repo.search(isNull(),eq(room.getDepartmentId()),isNull(),eq(""),isNull(),eq(Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of()));
        var empty=service.searchPanels(null,room.getDepartmentId(),null,null,null,Pageable.unpaged());
        assertEquals(0,empty.size());
        assertEquals(1,empty.totalPages());
        assertTrue(empty.first());
        assertTrue(empty.last());
    }

    @Test
    void panelWorkbenchMergesPurchasedValuesOmissionsAndSharedSample() {
        StaffInfo roomStaff=StaffInfo.builder().staffId(UUID.randomUUID()).department(room).build();
        when(authService.currentStaffId()).thenReturn(roomStaff.getStaffId());
        when(staffRepo.findById(roomStaff.getStaffId())).thenReturn(Optional.of(roomStaff));
        QueueTicket queue=QueueTicket.builder().ticketId(UUID.randomUUID()).queueNumber(3)
                .status(QueueStatus.IN_PROGRESS).department(room).visit(visit).build();
        MedicalService rbc=MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("AN-CBC-RBC")
                .name("RBC").department(room).build();
        MedicalService hgb=MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("AN-CBC-HGB")
                .name("HGB").department(room).build();
        TestRequest first=request(TestRequestStatus.COMPLETED); first.setService(rbc); first.setQueueTicket(queue);
        TestRequest second=request(TestRequestStatus.IN_PROGRESS); second.setService(hgb); second.setQueueTicket(queue);
        var data1=new tools.jackson.databind.json.JsonMapper().readTree(
                "{\"rbc\":4.5,\"_omissions\":{\"rbc\":{\"reasonCode\":\"OTHER\"}}}");
        var data2=new tools.jackson.databind.json.JsonMapper().readTree(
                "{\"hgb\":130,\"_omissions\":{\"hgb\":{\"reasonCode\":\"EQUIPMENT_ERROR\"}}}");
        TestResult sample=TestResult.builder().resultId(UUID.randomUUID()).sampleId("SMP-1")
                .sampleType(SpecimenType.BLOOD).sampleStatus(SpecimenStatus.ACCEPTED).conclusion("Ổn")
                .resultData(data1).build();
        first.setTestResult(sample);
        second.setTestResult(TestResult.builder().resultId(UUID.randomUUID()).resultData(data2).build());
        when(repo.findById(first.getTestRequestId())).thenReturn(Optional.of(first));
        when(repo.findAllByQueueTicket_TicketId(queue.getTicketId())).thenReturn(List.of(first,second));
        MedicalService panelService=MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("LAB-001").build();
        when(serviceRepo.findByServiceCode("LAB-001")).thenReturn(Optional.of(panelService));
        ClinicalFormTemplateVersion version=ClinicalFormTemplateVersion.builder().versionId(UUID.randomUUID()).build();
        when(clinicalFormTemplateService.resolveVersion(panelService.getServiceId(),null)).thenReturn(version);

        var response=service.getPanelWorkbench(first.getTestRequestId());

        assertAll(() -> assertEquals("SMP-1",response.sampleId()),
                () -> assertEquals("BLOOD",response.sampleType()),
                () -> assertEquals("ACCEPTED",response.sampleStatus()),
                () -> assertEquals(2,response.purchasedCount()),
                () -> assertEquals(1,response.completedCount()),
                () -> assertEquals(4.5,response.resultData().path("rbc").asDouble()),
                () -> assertEquals(130,response.resultData().path("hgb").asInt()),
                () -> assertTrue(response.resultData().path("_omissions").has("rbc")),
                () -> assertTrue(response.resultData().path("_omissions").has("hgb")),
                () -> assertEquals(24,response.analytes().size()),
                () -> assertEquals(2,response.analytes().stream().filter(a -> a.purchased()).count()));
    }

    @Test
    void panelWorkbenchRejectsStandaloneServiceAndMissingPanelConfiguration() {
        StaffInfo roomStaff=StaffInfo.builder().staffId(UUID.randomUUID()).department(room).build();
        when(authService.currentStaffId()).thenReturn(roomStaff.getStaffId());
        when(staffRepo.findById(roomStaff.getStaffId())).thenReturn(Optional.of(roomStaff));
        TestRequest standalone=request(TestRequestStatus.PENDING);
        standalone.setService(MedicalService.builder().serviceCode("IMG-001").build());
        when(repo.findById(standalone.getTestRequestId())).thenReturn(Optional.of(standalone));
        assertThrows(BadRequestException.class,()->service.getPanelWorkbench(standalone.getTestRequestId()));

        TestRequest panel=request(TestRequestStatus.PENDING);
        panel.setService(MedicalService.builder().serviceCode("LAB-001").build());
        when(repo.findById(panel.getTestRequestId())).thenReturn(Optional.of(panel));
        when(repo.findAllByVisitIdWithDetails(visit.getVisitId())).thenReturn(List.of(panel));
        when(serviceRepo.findByServiceCode("LAB-001")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.getPanelWorkbench(panel.getTestRequestId()));
    }

    @ParameterizedTest @ValueSource(strings={"missingRecord","missingService","examination","noType","alreadySigned","alreadyRequested","noRoom","maintenance","missingRequester"})
    void createRejectsInvalidOrRepeatedClinicalOrdersWithoutWriting(String reason) {
        if(!reason.equals("missingRecord")) when(recordRepo.findById(record.getRecordId())).thenReturn(Optional.of(record));
        if(!Set.of("missingRecord","missingService").contains(reason)) when(serviceRepo.findById(lab.getServiceId())).thenReturn(Optional.of(lab));
        if(reason.equals("examination")) lab.setDepartmentType(DepartmentType.EXAMINATION);
        if(reason.equals("noType")) lab.setDepartmentType(null);
        if(reason.equals("alreadySigned")) when(sameDayParaclinicalResultService.hasReusableResult(visit,lab.getServiceId())).thenReturn(true);
        if(reason.equals("alreadyRequested")) when(repo.existsByMedicalRecord_Visit_VisitIdAndService_ServiceIdAndStatusNot(visit.getVisitId(),lab.getServiceId(),TestRequestStatus.CANCELLED)).thenReturn(true);
        if(reason.equals("noRoom")) lab.setDepartment(null);
        if(reason.equals("maintenance")) room.setStatus(DepartmentStatus.MAINTENANCE);
        RuntimeException error=assertThrows(RuntimeException.class, () -> service.create(new TestRequestCreateRequest(record.getRecordId(),lab.getServiceId(),doctor.getStaffId(),null,null)));
        assertTrue(error instanceof BadRequestException || error instanceof ResourceNotFoundException || error instanceof ConflictException);
        verify(repo,never()).save(any()); verifyNoInteractions(queueTicketRepo,notificationService);
    }
}
