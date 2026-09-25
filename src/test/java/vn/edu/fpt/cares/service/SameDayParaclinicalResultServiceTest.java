package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.enums.TestRequestStatus;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SameDayParaclinicalResultServiceTest {
    @Mock MedicalRecordRepository recordRepository;
    @Mock CustomerVisitRepository visitRepository;
    @Mock ProfileRepository profileRepository;
    @Mock TestRequestRepository testRequestRepository;
    @InjectMocks SameDayParaclinicalResultService service;

    @Test
    void findForRecordRejectsUnknownRecord() {
        UUID id = UUID.randomUUID();
        when(recordRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findForRecord(id));
    }

    @Test
    void findForVisitReturnsLatestVerifiedResultPerService() {
        LocalDate date = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        Profile patient = Profile.builder().profileId(UUID.randomUUID()).build();
        CustomerVisit target = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(patient)
                .checkInTime(date.atTime(14, 0)).build();
        when(visitRepository.findById(target.getVisitId())).thenReturn(Optional.of(target));
        MedicalService lab = MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("LAB-001")
                .name("Công thức máu").departmentType(DepartmentType.PARACLINICAL).build();
        TestRequest older = completedRequest(patient, date.atTime(9, 0), lab);
        TestRequest latest = completedRequest(patient, date.atTime(10, 0), lab);
        when(testRequestRepository.findByProfileIdAndStatusCompleted(patient.getProfileId()))
                .thenReturn(List.of(older, latest));

        var results = service.findForVisit(target.getVisitId());

        assertEquals(1, results.size());
        assertEquals(latest.getTestRequestId(), results.get(0).testRequestId());
        assertEquals("Công thức máu", results.get(0).serviceName());
    }

    @Test
    void findForVisitExcludesCurrentVisitAndUnverifiedResults() {
        LocalDate date = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        Profile patient = Profile.builder().profileId(UUID.randomUUID()).build();
        CustomerVisit target = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(patient)
                .checkInTime(date.atTime(14, 0)).build();
        when(visitRepository.findById(target.getVisitId())).thenReturn(Optional.of(target));
        MedicalService lab = MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("LAB-002")
                .name("Đường huyết").departmentType(DepartmentType.PARACLINICAL).build();
        TestRequest request = completedRequest(patient, date.atTime(10, 0), lab);
        request.getMedicalRecord().setVisit(target);
        request.getTestResult().setVerifiedAt(null);
        when(testRequestRepository.findByProfileIdAndStatusCompleted(patient.getProfileId()))
                .thenReturn(List.of(request));

        assertTrue(service.findForVisit(target.getVisitId()).isEmpty());
    }

    @Test
    void hasReusableResultHandlesNullService() {
        assertFalse(service.hasReusableResult(null, null));
    }

    private TestRequest completedRequest(Profile patient, java.time.LocalDateTime completedAt,
                                         MedicalService serviceItem) {
        CustomerVisit sourceVisit = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(patient)
                .checkInTime(completedAt.minusHours(1)).checkOutTime(completedAt.plusHours(1)).build();
        MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID()).visit(sourceVisit).build();
        TestRequest request = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record)
                .service(serviceItem).status(TestRequestStatus.COMPLETED).completedAt(completedAt).build();
        TestResult result = TestResult.builder().resultId(UUID.randomUUID()).testRequest(request)
                .conclusion("Bình thường").verifiedAt(completedAt)
                .verifiedBy(StaffInfo.builder().staffId(UUID.randomUUID()).build()).build();
        request.setTestResult(result);
        return request;
    }
}
