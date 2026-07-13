package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.testRequest.TestRequestBatchCreateRequest;
import org.example.doansummer2026.dto.testRequest.TestRequestCreateRequest;
import org.example.doansummer2026.dto.testRequest.TestRequestResponse;
import org.example.doansummer2026.dto.testRequest.TestRequestUpdateRequest;
import org.example.doansummer2026.dto.testResult.TestResultCreateRequest;
import org.example.doansummer2026.dto.testResult.TestResultResponse;
import org.example.doansummer2026.dto.testResult.TestResultUpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.model.TestResult;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.example.doansummer2026.repository.TestResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.TestRequestServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TestRequestService implements TestRequestServiceInterface {

    private final TestRequestRepository repo;
    private final TestResultRepository resultRepo;
    private final MedicalRecordRepository recordRepo;
    private final MedicalServiceRepository serviceRepo;
    private final DepartmentRepository departmentRepo;
    private final StaffInfoRepository staffRepo;

    @Transactional(readOnly = true)
    public PageResponse<TestRequestResponse> search(UUID recordId, UUID departmentId,
                                                     TestRequestStatus status, Pageable pageable) {
        Page<TestRequest> page = repo.search(recordId, departmentId, status, pageable);
        return PageResponse.from(page, TestRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public TestRequestResponse get(UUID id) {
        return TestRequestResponse.from(findById(id));
    }

    public TestRequestResponse create(TestRequestCreateRequest req) {
        MedicalRecord record = recordRepo.findById(req.medicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ho so benh an khong ton tai: " + req.medicalRecordId()));
        MedicalService service = serviceRepo.findById(req.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + req.serviceId()));
        Department dept = departmentRepo.findById(req.performingDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Khoa khong ton tai: " + req.performingDepartmentId()));
        StaffInfo requestedBy = staffRepo.findById(req.requestedById())
                .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai: " + req.requestedById()));
        TestRequest t = TestRequest.builder()
                .medicalRecord(record)
                .service(service)
                .performingDepartment(dept)
                .description(req.description())
                .requestedBy(requestedBy)
                .status(TestRequestStatus.PENDING)
                .build();
        return TestRequestResponse.from(repo.save(t));
    }

    public TestRequestResponse update(UUID id, TestRequestUpdateRequest req) {
        TestRequest t = findById(id);
        if (req.status() != null) {
            t.setStatus(req.status());
            if (req.status() == TestRequestStatus.COMPLETED && t.getCompletedAt() == null) {
                t.setCompletedAt(LocalDateTime.now());
            }
        }
        return TestRequestResponse.from(repo.save(t));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Yeu cau xet nghiem khong ton tai: " + id);
        }
        repo.deleteById(id);
    }

    public TestRequest findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yeu cau xet nghiem khong ton tai: " + id));
    }

    // --- TestResult sub-resource ---

    @Transactional(readOnly = true)
    public TestResultResponse getResult(UUID testRequestId) {
        TestRequest t = findById(testRequestId);
        TestResult r = resultRepo.findByTestRequest_TestRequestId(t.getTestRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Chua co ket qua cho yeu cau nay"));
        return TestResultResponse.from(r);
    }

    public TestResultResponse createResult(UUID testRequestId, TestResultCreateRequest req) {
        TestRequest t = findById(testRequestId);
        if (resultRepo.findByTestRequest_TestRequestId(testRequestId).isPresent()) {
            throw new ConflictException("Yeu cau da co ket qua; dung PUT de cap nhat");
        }
        StaffInfo performedBy = staffRepo.findById(req.performedById())
                .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai: " + req.performedById()));
        TestResult r = TestResult.builder()
                .testRequest(t)
                .imageUrl(req.imageUrl())
                .conclusion(req.conclusion())
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();
        resultRepo.save(r);
        t.setStatus(TestRequestStatus.COMPLETED);
        t.setCompletedAt(LocalDateTime.now());
        repo.save(t);
        return TestResultResponse.from(r);
    }

    public TestResultResponse updateResult(UUID testRequestId, TestResultUpdateRequest req) {
        TestRequest t = findById(testRequestId);
        TestResult r = resultRepo.findByTestRequest_TestRequestId(t.getTestRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Chua co ket qua de cap nhat"));
        if (req.imageUrl() != null) r.setImageUrl(req.imageUrl());
        if (req.conclusion() != null) r.setConclusion(req.conclusion());
        TestResult saved = resultRepo.save(r);
        return TestResultResponse.from(saved);
    }

    /**
     * Tao nhieu TestRequest cung luc - bac si chon nhieu dich vu xet nghiem.
     */
    public List<TestRequestResponse> createBatch(TestRequestBatchCreateRequest req) {
        MedicalRecord record = recordRepo.findById(req.medicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("Ho so benh an khong ton tai"));
        Department dept = departmentRepo.findById(req.performingDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Khoa khong ton tai"));
        StaffInfo requestedBy = staffRepo.findById(req.requestedById())
                .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai"));

        List<TestRequest> toCreate = req.serviceIds().stream()
                .map(serviceId -> {
                    MedicalService service = serviceRepo.findById(serviceId)
                            .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
                    return TestRequest.builder()
                            .medicalRecord(record)
                            .service(service)
                            .performingDepartment(dept)
                            .description(req.description())
                            .requestedBy(requestedBy)
                            .status(TestRequestStatus.PENDING)
                            .build();
                })
                .toList();

        return repo.saveAll(toCreate).stream()
                .map(TestRequestResponse::from)
                .toList();
    }
}
