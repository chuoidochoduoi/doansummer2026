package org.example.doansummer2026.dto.medicalhistory;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VisitDetailResponsePublishedHistoryTest {

    @Test
    void publishedHistoryOnlyReturnsCompletedRecordsAndKeepsSkippedServicesSeparate() {
        Profile patient = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn An").build();
        CustomerVisit visit = CustomerVisit.builder()
                .visitId(UUID.randomUUID())
                .customer(patient)
                .status(VisitStatus.COMPLETED)
                .checkInTime(LocalDateTime.of(2026, 9, 1, 8, 0))
                .build();
        Department examinationRoom = Department.builder()
                .departmentId(UUID.randomUUID())
                .name("Phòng khám Nội")
                .roomCode("INT-101")
                .departmentType(DepartmentType.EXAMINATION)
                .build();
        MedicalService completedService = MedicalService.builder()
                .serviceId(UUID.randomUUID()).name("Khám Nội tổng quát").build();
        MedicalService draftService = MedicalService.builder()
                .serviceId(UUID.randomUUID()).name("Khám Tim mạch").build();
        MedicalService skippedService = MedicalService.builder()
                .serviceId(UUID.randomUUID()).name("Khám Hô hấp").build();

        QueueTicket completedTicket = queue(visit, examinationRoom, completedService, QueueStatus.DONE);
        QueueTicket draftTicket = queue(visit, examinationRoom, draftService, QueueStatus.IN_PROGRESS);
        QueueTicket skippedTicket = queue(visit, examinationRoom, skippedService, QueueStatus.SKIPPED);
        MedicalRecord completedRecord = MedicalRecord.builder()
                .recordId(UUID.randomUUID()).recordCode("MR-COMPLETE")
                .visit(visit).queueTicket(completedTicket).status(MedicalRecordStatus.COMPLETED).build();
        MedicalRecord draftRecord = MedicalRecord.builder()
                .recordId(UUID.randomUUID()).recordCode("MR-DRAFT")
                .visit(visit).queueTicket(draftTicket).status(MedicalRecordStatus.IN_PROGRESS).build();

        VisitDetailResponse response = VisitDetailResponse.publishedHistory(
                List.of(completedRecord, draftRecord), List.of(), Map.of(), List.of(),
                List.of(completedTicket, draftTicket, skippedTicket));

        assertThat(response.examinations()).extracting(VisitDetailResponse.ExaminationResponse::recordCode)
                .containsExactly("MR-COMPLETE");
        assertThat(response.skippedServices()).extracting(VisitDetailResponse.SkippedServiceResponse::serviceName)
                .containsExactly("Khám Hô hấp");
        assertThat(response.completionStatus()).isEqualTo("PARTIAL");
        assertThat(response.completedExaminationCount()).isEqualTo(1);
    }

    @Test
    void publishedHistoryAddsParentPanelMetadataForPurchasedAnalyte() {
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        Department examinationRoom = Department.builder()
                .departmentId(UUID.randomUUID()).departmentType(DepartmentType.EXAMINATION).build();
        Department labRoom = Department.builder()
                .departmentId(UUID.randomUUID()).departmentType(DepartmentType.LABORATORY).build();
        MedicalRecord record = MedicalRecord.builder()
                .recordId(UUID.randomUUID()).visit(visit).status(MedicalRecordStatus.COMPLETED)
                .queueTicket(queue(visit, examinationRoom,
                        MedicalService.builder().serviceId(UUID.randomUUID()).name("Khám Nội").build(),
                        QueueStatus.DONE))
                .build();
        MedicalService analyte = MedicalService.builder()
                .serviceId(UUID.randomUUID()).serviceCode("AN-CBC-HCT").name("Hematocrit (HCT)").build();
        QueueTicket labTicket = queue(visit, labRoom, analyte, QueueStatus.DONE);
        TestRequest request = TestRequest.builder()
                .testRequestId(UUID.randomUUID()).medicalRecord(record).service(analyte)
                .performingDepartment(labRoom).queueTicket(labTicket)
                .status(TestRequestStatus.COMPLETED).build();

        VisitDetailResponse response = VisitDetailResponse.from(
                List.of(record), List.of(request), Map.of());

        TestResponse test = response.tests().get(0);
        assertThat(test.serviceCode()).isEqualTo("AN-CBC-HCT");
        assertThat(test.panelCode()).isEqualTo("LAB-001");
        assertThat(test.panelName()).isEqualTo("Công thức máu");
        assertThat(test.panelTotalAnalytes()).isEqualTo(24);
        assertThat(test.queueTicketId()).isEqualTo(labTicket.getTicketId());
    }

    private QueueTicket queue(CustomerVisit visit, Department department, MedicalService service, QueueStatus status) {
        return QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .visit(visit)
                .department(department)
                .service(service)
                .status(status)
                .workDate(LocalDate.of(2026, 9, 1))
                .build();
    }
}
