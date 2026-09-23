package org.example.doansummer2026.dto.medicalhistory;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VisitDetailResponseTest {
    @Test
    void fromReturnsNullWithoutRecords() {
        assertNull(VisitDetailResponse.from(null, List.of()));
        assertNull(VisitDetailResponse.from(List.of(), List.of(), List.of()));
    }

    @Test
    void publishedHistoryIncludesCompletedExaminationVerifiedTestAndSkippedService() {
        Profile patient = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Anh Đức").build();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(patient)
                .checkInTime(LocalDateTime.of(2026, 9, 5, 8, 0)).build();
        Department examRoom = Department.builder().name("Nội khoa").roomCode("INT-103")
                .departmentType(DepartmentType.EXAMINATION).build();
        MedicalService examService = MedicalService.builder().name("Khám Nội tổng quát").build();
        QueueTicket examQueue = QueueTicket.builder().department(examRoom).service(examService)
                .status(QueueStatus.DONE).build();
        MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID()).recordCode("MR-001")
                .visit(visit).queueTicket(examQueue).status(MedicalRecordStatus.COMPLETED)
                .diagnosis("Viêm họng nhẹ").build();

        MedicalService lab = MedicalService.builder().name("Công thức máu").serviceCode("LAB-001")
                .departmentType(DepartmentType.PARACLINICAL).build();
        TestRequest request = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record)
                .service(lab).status(TestRequestStatus.COMPLETED).build();
        request.setTestResult(TestResult.builder().resultId(UUID.randomUUID()).testRequest(request)
                .verifiedAt(LocalDateTime.of(2026, 9, 5, 10, 0)).build());

        QueueTicket skipped = QueueTicket.builder().service(MedicalService.builder().name("Khám Tim mạch").build())
                .status(QueueStatus.SKIPPED).workDate(LocalDate.of(2026, 9, 5)).build();
        VisitDetailResponse response = VisitDetailResponse.publishedHistory(
                List.of(record), List.of(request), List.of(), List.of(examQueue, skipped));

        assertNotNull(response);
        assertEquals(1, response.completedExaminationCount());
        assertEquals(1, response.signedTestCount());
        assertEquals("PARTIAL", response.completionStatus());
        assertEquals("Khám Tim mạch", response.skippedServices().get(0).serviceName());
    }
}
