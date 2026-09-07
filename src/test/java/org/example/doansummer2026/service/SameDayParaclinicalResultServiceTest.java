package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.enums.TestResultRevisionStatus;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.ClinicalFormTemplateVersion;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.model.TestResult;
import org.example.doansummer2026.model.TestResultAttachment;
import org.example.doansummer2026.model.TestResultRevision;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.example.doansummer2026.repository.TestResultAttachmentRepository;
import org.example.doansummer2026.repository.TestResultRevisionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SameDayParaclinicalResultServiceTest {

    @Mock MedicalRecordRepository recordRepository;
    @Mock CustomerVisitRepository visitRepository;
    @Mock ProfileRepository profileRepository;
    @Mock TestRequestRepository testRequestRepository;
    @Mock TestResultRevisionRepository revisionRepository;
    @Mock TestResultAttachmentRepository attachmentRepository;
    @InjectMocks SameDayParaclinicalResultService service;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void entryPointsRejectMissingEntitiesAndReturnEmptyForIncompleteContext() {
        UUID missingRecord = UUID.randomUUID();
        UUID missingVisit = UUID.randomUUID();
        UUID missingProfile = UUID.randomUUID();
        UUID recordWithoutVisit = UUID.randomUUID();
        when(recordRepository.findById(missingRecord)).thenReturn(Optional.empty());
        when(visitRepository.findById(missingVisit)).thenReturn(Optional.empty());
        when(profileRepository.findById(missingProfile)).thenReturn(Optional.empty());
        when(recordRepository.findById(recordWithoutVisit))
                .thenReturn(Optional.of(MedicalRecord.builder().build()));

        assertThrows(ResourceNotFoundException.class, () -> service.findForRecord(missingRecord));
        assertThrows(ResourceNotFoundException.class, () -> service.findForVisit(missingVisit));
        assertThrows(ResourceNotFoundException.class, () -> service.findForCustomerToday(missingProfile));
        assertTrue(service.findForRecord(recordWithoutVisit).isEmpty());
        assertFalse(service.hasReusableResult(null, null));
    }

    @Test
    void visitWithoutPatientOrCheckInHasNoReusableResult() {
        UUID noPatientId = UUID.randomUUID();
        UUID noCheckInId = UUID.randomUUID();
        CustomerVisit noPatient = CustomerVisit.builder().visitId(noPatientId).customer(null)
                .checkInTime(LocalDateTime.of(2026, 9, 5, 8, 0)).build();
        CustomerVisit noCheckIn = CustomerVisit.builder().visitId(noCheckInId)
                .customer(Profile.builder().profileId(UUID.randomUUID()).build()).checkInTime(null).build();
        when(visitRepository.findById(noPatientId)).thenReturn(Optional.of(noPatient));
        when(visitRepository.findById(noCheckInId)).thenReturn(Optional.of(noCheckIn));

        assertTrue(service.findForVisit(noPatientId).isEmpty());
        assertTrue(service.findForVisit(noCheckInId).isEmpty());
        assertFalse(service.hasReusableResult(noPatient, UUID.randomUUID()));
    }

    @Test
    void findForVisitFiltersInvalidRequestsKeepsLatestPerServiceAndMapsSignedData() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 5);
        LocalDateTime cutoff = date.atTime(12, 0);
        Profile patient = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Anh Đức").build();
        CustomerVisit target = visit(patient, date.atTime(8, 0), cutoff);
        CustomerVisit source = visit(patient, date.atTime(7, 0), date.atTime(11, 30));
        MedicalService glucose = service("LAB-GLU", "Đường huyết", DepartmentType.PARACLINICAL);
        MedicalService blood = service("LAB-CBC", "Công thức máu", DepartmentType.LABORATORY);

        TestRequest latest = request(source, glucose, date.atTime(10, 0), true);
        TestRequest olderSameService = request(source, glucose, date.atTime(9, 0), true);
        TestRequest secondService = request(source, blood, date.atTime(9, 30), true);
        TestRequest unsigned = request(source, service("IMG-X", "X-quang", DepartmentType.IMAGING),
                date.atTime(9, 15), true);
        TestRequest signedAfterCutoff = request(source,
                service("LAB-LATE", "Muộn", DepartmentType.PARACLINICAL), date.atTime(11, 0), true);
        TestRequest signedWrongDate = request(source,
                service("LAB-OLD", "Ngày trước", DepartmentType.PARACLINICAL), date.atTime(8, 30), true);

        List<TestRequest> invalid = List.of(
                request(source, glucose, date.atTime(8, 0), false),
                requestWith(source, glucose, TestRequestStatus.COMPLETED, null, true),
                request(source, glucose, date.minusDays(1).atTime(10, 0), true),
                request(source, glucose, cutoff.plusMinutes(1), true),
                requestWith(null, glucose, TestRequestStatus.COMPLETED, date.atTime(8, 0), true),
                requestWith(CustomerVisit.builder().visitId(UUID.randomUUID()).customer(patient)
                                .checkInTime(date.atTime(7, 0)).build(), glucose,
                        TestRequestStatus.COMPLETED, date.atTime(8, 1), true),
                request(target, glucose, date.atTime(8, 2), true),
                request(source, null, date.atTime(8, 3), true),
                request(source, service("BAD", "Thiếu loại", null), date.atTime(8, 4), true),
                request(source, service("EX", "Khám", DepartmentType.EXAMINATION), date.atTime(8, 5), true),
                requestWith(source, glucose, TestRequestStatus.COMPLETED, date.atTime(8, 6), false));

        TestResultRevision latestRevision = signedRevision(latest, date.atTime(10, 5), true, true);
        latestRevision.setSignedBy(staff("BS Ký"));
        latest.getMedicalRecord().setDoctor(staff("BS Chỉ định"));
        latest.getMedicalRecord().setQueueTicket(QueueTicket.builder()
                .service(service("EX-INT", "Khám Nội", DepartmentType.EXAMINATION)).build());
        latest.setPerformingDepartment(Department.builder().departmentId(UUID.randomUUID())
                .name("Xét nghiệm sinh hóa").build());
        TestResultAttachment attachment = TestResultAttachment.builder().attachmentId(UUID.randomUUID())
                .revision(latestRevision).originalName("ket-qua.pdf").contentType("application/pdf")
                .fileSize(1234L).displayOrder(1).build();

        TestResultRevision olderRevision = signedRevision(olderSameService, date.atTime(9, 5), false, false);
        TestResultRevision secondRevision = signedRevision(secondService, date.atTime(9, 35), false, false);
        TestResultRevision unsignedRevision = signedRevision(unsigned, null, false, false);
        TestResultRevision lateRevision = signedRevision(signedAfterCutoff, cutoff.plusMinutes(1), false, false);
        TestResultRevision wrongDateRevision = signedRevision(signedWrongDate,
                date.minusDays(1).atTime(10, 0), false, false);

        when(visitRepository.findById(target.getVisitId())).thenReturn(Optional.of(target));
        when(testRequestRepository.findByProfileIdAndStatusCompleted(patient.getProfileId()))
                .thenReturn(concat(List.of(olderSameService, latest, secondService, unsigned,
                        signedAfterCutoff, signedWrongDate), invalid));
        stubRevision(latest, latestRevision);
        stubRevision(olderSameService, olderRevision);
        stubRevision(secondService, secondRevision);
        stubRevision(unsigned, unsignedRevision);
        stubRevision(signedAfterCutoff, lateRevision);
        stubRevision(signedWrongDate, wrongDateRevision);
        when(attachmentRepository.findByRevision_RevisionIdOrderByDisplayOrder(latestRevision.getRevisionId()))
                .thenReturn(List.of(attachment));

        var result = service.findForVisit(target.getVisitId());

        assertEquals(2, result.size());
        var first = result.get(0);
        assertAll(
                () -> assertEquals(latest.getTestRequestId(), first.testRequestId()),
                () -> assertEquals("Đường huyết", first.serviceName()),
                () -> assertEquals("Xét nghiệm sinh hóa", first.performingDepartmentName()),
                () -> assertEquals("BS Ký", first.verifiedByName()),
                () -> assertEquals("Khám Nội", first.sourceExaminationServiceName()),
                () -> assertEquals("BS Chỉ định", first.sourceDoctorName()),
                () -> assertEquals(1, first.attachments().size()),
                () -> assertEquals(3, first.results().size()),
                () -> assertEquals("Glucose", first.results().get(0).name()),
                () -> assertEquals("4 - 6", first.results().get(0).referenceRange()),
                () -> assertEquals("HIGH", first.results().get(0).assessment()),
                () -> assertEquals("VIS-" + source.getVisitId().toString().substring(0, 8).toUpperCase(),
                        first.sourceVisitCode()),
                () -> assertTrue(first.readOnly()));
        assertEquals(secondService.getTestRequestId(), result.get(1).testRequestId());
        assertTrue(service.hasReusableResult(target, glucose.getServiceId()));
        assertFalse(service.hasReusableResult(target, UUID.randomUUID()));
    }

    @Test
    void findForCustomerTodayUsesClinicDateAndNullContextFieldsAreAllowed() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Profile patient = Profile.builder().profileId(UUID.randomUUID()).build();
        CustomerVisit source = visit(patient, today.atStartOfDay(), null);
        MedicalService lab = service("LAB-001", "Xét nghiệm", DepartmentType.PARACLINICAL);
        TestRequest request = request(source, lab, today.atTime(1, 0), true);
        request.setPerformingDepartment(null);
        request.getMedicalRecord().setQueueTicket(null);
        request.getMedicalRecord().setDoctor(null);
        TestResultRevision revision = signedRevision(request, today.atTime(1, 5), false, false);
        revision.setSignedBy(StaffInfo.builder().staffId(UUID.randomUUID()).profile(null).build());
        when(profileRepository.findById(patient.getProfileId())).thenReturn(Optional.of(patient));
        when(testRequestRepository.findByProfileIdAndStatusCompleted(patient.getProfileId()))
                .thenReturn(List.of(request));
        stubRevision(request, revision);

        var result = service.findForCustomerToday(patient.getProfileId());

        assertEquals(1, result.size());
        assertAll(
                () -> assertNull(result.get(0).performingDepartmentId()),
                () -> assertNull(result.get(0).verifiedByName()),
                () -> assertNull(result.get(0).sourceExaminationServiceName()),
                () -> assertNull(result.get(0).sourceDoctorId()),
                () -> assertTrue(result.get(0).results().isEmpty()));
    }

    private TestResultRevision signedRevision(TestRequest request, LocalDateTime signedAt,
                                              boolean structured, boolean withTemplate) throws Exception {
        var data = structured ? mapper.readTree("""
                {"glucose":7.2,"hgb":13,"note":"ổn","_meta":{"flags":{
                  "glucose":{"status":"HIGH","referenceRange":{"low":4,"high":6}},
                  "hgb":{"referenceRange":{"low":12}},"note":{"status":"NORMAL"}}}}
                """) : mapper.readTree("{}");
        var schema = mapper.readTree("""
                {"fields":[
                  {"key":"glucose","label":"Glucose","unit":"mmol/L"},
                  {"key":"hgb","label":"HGB"},
                  {"key":"missing","label":"Không có"}],
                 "sections":[{"fields":[{"key":"note","label":"Ghi chú"}]},{"title":"Rỗng"}]}
                """);
        ClinicalFormTemplateVersion version = withTemplate
                ? ClinicalFormTemplateVersion.builder().versionId(UUID.randomUUID()).schemaJson(schema).build()
                : null;
        return TestResultRevision.builder().revisionId(UUID.randomUUID()).revisionNo(1)
                .status(TestResultRevisionStatus.SIGNED).testResult(request.getTestResult())
                .resultData(data).templateVersion(version).conclusion("Kết luận").signedAt(signedAt).build();
    }

    private void stubRevision(TestRequest request, TestResultRevision revision) {
        when(revisionRepository.findFirstByTestResult_ResultIdAndStatusOrderByRevisionNoDesc(
                request.getTestResult().getResultId(), TestResultRevisionStatus.SIGNED))
                .thenReturn(Optional.ofNullable(revision));
        if (revision != null && revision.getSignedAt() != null) {
            when(attachmentRepository.findByRevision_RevisionIdOrderByDisplayOrder(revision.getRevisionId()))
                    .thenReturn(List.of());
        }
    }

    private TestRequest request(CustomerVisit sourceVisit, MedicalService medicalService,
                                LocalDateTime completedAt, boolean withResult) {
        return requestWith(sourceVisit, medicalService, TestRequestStatus.COMPLETED, completedAt, withResult);
    }

    private TestRequest requestWith(CustomerVisit sourceVisit, MedicalService medicalService,
                                    TestRequestStatus status, LocalDateTime completedAt, boolean withResult) {
        MedicalRecord record = sourceVisit == null ? null : MedicalRecord.builder().recordId(UUID.randomUUID())
                .recordCode("MR-001").visit(sourceVisit).build();
        TestRequest request = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record)
                .service(medicalService).status(status).completedAt(completedAt).build();
        if (withResult) {
            request.setTestResult(TestResult.builder().resultId(UUID.randomUUID()).testRequest(request).build());
        }
        return request;
    }

    private MedicalService service(String code, String name, DepartmentType type) {
        return MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode(code)
                .name(name).departmentType(type).build();
    }

    private CustomerVisit visit(Profile patient, LocalDateTime checkIn, LocalDateTime checkOut) {
        return CustomerVisit.builder().visitId(UUID.randomUUID()).customer(patient)
                .checkInTime(checkIn).checkOutTime(checkOut).build();
    }

    private StaffInfo staff(String name) {
        return StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().profileId(UUID.randomUUID()).fullName(name).build()).build();
    }

    private List<TestRequest> concat(List<TestRequest> first, List<TestRequest> second) {
        java.util.ArrayList<TestRequest> result = new java.util.ArrayList<>(first);
        result.addAll(second);
        return result;
    }
}
