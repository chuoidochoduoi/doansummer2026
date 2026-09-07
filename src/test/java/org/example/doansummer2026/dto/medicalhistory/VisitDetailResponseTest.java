package org.example.doansummer2026.dto.medicalhistory;

import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.model.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VisitDetailResponseTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void returnsNullWhenThereAreNoRecords() {
        assertNull(VisitDetailResponse.from(null, List.of(), Map.of()));
        assertNull(VisitDetailResponse.from(List.of(), List.of(), Map.of(), List.of()));
    }

    @Test
    void mapsACompletePublishedVisitIncludingClinicalFormTestResultAndSkippedService() throws Exception {
        LocalDateTime time = LocalDateTime.of(2026, 9, 5, 9, 30);
        Profile patient = Profile.builder().fullName("Nguyễn Anh Đức").dateOfBirth(LocalDate.of(1990, 2, 3))
                .gender(Gender.MALE).phone("0900000000").address("Hà Nội").allergies(" Penicillin ; penicillin; Tôm ").build();
        Profile doctorProfile = Profile.builder().fullName("Bác sĩ An").build();
        StaffInfo doctor = StaffInfo.builder().staffId(UUID.randomUUID()).profile(doctorProfile).build();
        StaffInfo manager = StaffInfo.builder().profile(Profile.builder().fullName("Quản lý Mai").build()).build();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(patient)
                .checkInTime(time).status(VisitStatus.COMPLETED).build();
        Department examination = Department.builder().name("Phòng Nội").roomCode("INT-101")
                .departmentType(DepartmentType.EXAMINATION).build();
        MedicalService examinationService = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("EX-001").name("Khám Nội").build();
        QueueTicket examinationTicket = QueueTicket.builder().department(examination).service(examinationService).build();
        VitalSigns vitalSigns = VitalSigns.builder().bloodPressure("120/80").heartRate(72)
                .temperature(new BigDecimal("36.7")).weight(new BigDecimal("60")).height(new BigDecimal("170"))
                .recordedAt(time).recordedBy(doctor).build();
        ClinicalFormTemplate template = ClinicalFormTemplate.builder().templateId(UUID.randomUUID())
                .code("EXAM").name("Phiếu khám").context(ClinicalFormContext.EXAMINATION).build();
        ClinicalFormTemplateVersion version = ClinicalFormTemplateVersion.builder().versionId(UUID.randomUUID())
                .template(template).versionNo(2).schemaJson(json.readTree("{\"fields\":[]}")) .build();
        PrescriptionItem medicine = PrescriptionItem.builder().medicineName("Paracetamol").quantity(10)
                .unit("viên").note("Sau ăn").frequencyPerDay(2).build();
        Icd10Selection diagnosis = Icd10Selection.builder().code("J02.9").codeName("Viêm họng cấp").build();
        MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID()).recordCode("MR-001")
                .visit(visit).queueTicket(examinationTicket).doctor(doctor).status(MedicalRecordStatus.COMPLETED)
                .chiefComplaint("Đau họng").clinicalFindings("Họng đỏ").diagnosis("Viêm họng")
                .conclusion("Theo dõi").patientInstruction("Uống nhiều nước").followUpNote("Tái khám khi sốt")
                .prescriptionItems(new LinkedHashSet<>(List.of(medicine))).vitalSigns(vitalSigns)
                .icdSelections(new LinkedHashSet<>(List.of(diagnosis))).formTemplateVersion(version)
                .specialtyData(json.readTree("{\"pain\":2}")).completedAt(time.plusMinutes(20))
                .ratingScore(5).ratingComment("Tốt").ratedAt(time.plusHours(1)).feedbackStatus("RESPONDED")
                .managerResponse("Cảm ơn").respondedAt(time.plusHours(2)).respondedBy(manager).build();
        record.setCreatedAt(time);

        Department lab = Department.builder().name("Xét nghiệm").roomCode("LAB-201")
                .departmentType(DepartmentType.LABORATORY).build();
        MedicalService labService = MedicalService.builder().serviceCode("LAB-001-RBC").name("Hồng cầu").build();
        ClinicalFormTemplateVersion labVersion = ClinicalFormTemplateVersion.builder()
                .schemaJson(json.readTree("{\"fields\":[{\"key\":\"rbc\",\"label\":\"Hồng cầu\",\"unit\":\"T/L\"}],"
                        + "\"sections\":[{\"fields\":[{\"key\":\"wbc\",\"label\":\"Bạch cầu\"}]}]}")) .build();
        UUID resultId = UUID.randomUUID();
        TestResult result = TestResult.builder().resultId(resultId).conclusion("Bình thường").imageUrl("result.pdf")
                .resultData(json.readTree("{\"rbc\":\"3.8\",\"wbc\":\"7\",\"_meta\":{\"flags\":{"
                        + "\"rbc\":{\"status\":\"LOW\",\"referenceRange\":{\"low\":4,\"high\":6}},"
                        + "\"wbc\":{\"status\":\"NORMAL\"}}}}"))
                .formTemplateVersion(labVersion).sampleId("SMP-01").sampleType(SpecimenType.BLOOD)
                .sampleStatus(SpecimenStatus.ACCEPTED).collectedAt(time).collectedBy(doctor)
                .performedAt(time.plusMinutes(30)).performedBy(doctor).build();
        TestRequest request = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record)
                .service(labService).performingDepartment(lab).queueTicket(QueueTicket.builder().ticketId(UUID.randomUUID()).build())
                .status(TestRequestStatus.COMPLETED).testResult(result).build();
        request.setCreatedAt(time);

        QueueTicket skipped = QueueTicket.builder().status(QueueStatus.SKIPPED).workDate(time.toLocalDate())
                .service(MedicalService.builder().serviceId(UUID.randomUUID()).name("Siêu âm").build())
                .department(Department.builder().name("Chẩn đoán hình ảnh").roomCode("IMG-1").build()).build();
        QueueTicket skippedWithoutDetails = QueueTicket.builder().status(QueueStatus.SKIPPED).build();
        QueueTicket waiting = QueueTicket.builder().status(QueueStatus.WAITING).build();

        VisitDetailResponse response = VisitDetailResponse.publishedHistory(List.of(record), List.of(request),
                Map.of(resultId, List.of()), null, List.of(skipped, skippedWithoutDetails, waiting));

        assertAll(
                () -> assertEquals("PARTIAL", response.completionStatus()),
                () -> assertEquals("Nguyễn Anh Đức", response.patientName()),
                () -> assertEquals(2, response.patientAllergies().items().size()),
                () -> assertEquals("Họng đỏ", response.clinicalResult()),
                () -> assertEquals("Paracetamol 10 viên", response.prescription()),
                () -> assertEquals("Quản lý Mai", response.respondedByName()),
                () -> assertEquals(1, response.examinations().size()),
                () -> assertNotNull(response.examinations().get(0).clinicalForm()),
                () -> assertEquals(2, response.tests().get(0).results().size()),
                () -> assertTrue(response.tests().get(0).hasAbnormal()),
                () -> assertEquals("Bác sĩ An", response.tests().get(0).collectedBy()),
                () -> assertEquals(2, response.skippedServices().size()));
    }

    @Test
    void mapsSparseDraftUsingFallbacksAndDoesNotPublishIt() {
        MedicalRecord standalone = MedicalRecord.builder().recordId(UUID.randomUUID()).recordCode("TMP")
                .status(MedicalRecordStatus.DRAFT).prescriptionItems(null).icdSelections(null).build();
        MedicalRecord exam = MedicalRecord.builder().recordId(UUID.randomUUID()).recordCode("MR-DRAFT")
                .queueTicket(QueueTicket.builder().department(Department.builder()
                        .departmentType(DepartmentType.EXAMINATION).build()).build())
                .status(null).vitalSigns(VitalSigns.builder().build()).build();
        TestRequest emptyTest = TestRequest.builder().testRequestId(UUID.randomUUID()).status(null).build();

        VisitDetailResponse regular = VisitDetailResponse.from(List.of(standalone, exam), List.of(emptyTest), Map.of());
        VisitDetailResponse published = VisitDetailResponse.publishedHistory(List.of(standalone, exam), null,
                Map.of(), List.of(), null);

        assertAll(
                () -> assertNull(regular.visitId()),
                () -> assertEquals("IN_PROGRESS", regular.status()),
                () -> assertNull(regular.clinicalResult()),
                () -> assertEquals("Dịch vụ cận lâm sàng", regular.tests().get(0).name()),
                () -> assertNull(regular.tests().get(0).pdfUrl()),
                () -> assertTrue(published.examinations().isEmpty()),
                () -> assertNull(published.symptoms()),
                () -> assertEquals("COMPLETE", published.completionStatus()));
    }

    @Test
    void derivesClinicalSummaryAndPrescriptionNoteFallbacks() {
        Department department = Department.builder().departmentType(DepartmentType.EXAMINATION).build();
        MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID()).recordCode("MR")
                .queueTicket(QueueTicket.builder().department(department).build()).status(MedicalRecordStatus.IN_PROGRESS)
                .vitalSigns(VitalSigns.builder().bloodPressure("110/70").heartRate(60)
                        .temperature(new BigDecimal("37.1")).weight(new BigDecimal("55")).build())
                .prescriptionNote("Dùng thuốc theo đơn").build();

        VisitDetailResponse response = VisitDetailResponse.from(List.of(record), null, Map.of());
        assertEquals("Huyết áp: 110/70 Nhịp tim: 60 Nhiệt độ: 37.1°C Cân nặng: 55kg", response.clinicalResult());
        assertEquals("Dùng thuốc theo đơn", response.prescription());
    }
}
