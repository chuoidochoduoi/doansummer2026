package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.testRequest.*;
import org.example.doansummer2026.dto.testResult.TestResultCreateRequest;
import org.example.doansummer2026.dto.testResult.TestResultResponse;
import org.example.doansummer2026.dto.testResult.TestResultUpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.model.TestResult;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.example.doansummer2026.repository.TestResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.TestRequestServiceInterface;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final StaffInfoRepository staffRepo;
    private final QueueTicketRepository queueTicketRepo;

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
        // Tu dong lay performingDepartment tu MedicalService
        Department dept = service.getDepartment();
        if (dept == null) {
            throw new ResourceNotFoundException("Dich vu chua thiet lap khoa thuc hien: " + req.serviceId());
        }
        StaffInfo requestedBy = staffRepo.findById(req.requestedById())
                .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai: " + req.requestedById()));
        TestRequest t = TestRequest.builder()
                .medicalRecord(record)
                .service(service)
                .performingDepartment(dept)
                .description(req.notes())
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
            // Kiem tra tat ca TestRequest trong medical record de set status TEST_DONE hoac WAITING_FOR_TEST
            if (req.status() == TestRequestStatus.COMPLETED && t.getMedicalRecord() != null && t.getMedicalRecord().getVisit() != null) {
                UUID visitId = t.getMedicalRecord().getVisit().getVisitId();
                long totalTestRequests = repo.countByMedicalRecord_MedicalRecordId(t.getMedicalRecord().getRecordId());
                long incompleteCount = repo.countByMedicalRecordAndStatusIn(
                        t.getMedicalRecord().getRecordId(),
                        java.util.List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS));

                QueueTicket queueTicket = queueTicketRepo.findByVisit_VisitId(visitId).orElse(null);
                if (queueTicket != null) {
                    if (totalTestRequests > 0 && incompleteCount == 0) {
                        // Tat ca test da xong
                        queueTicket.setStatus(QueueStatus.TEST_DONE);
                    } else {
                        // Con test chua xong
                        queueTicket.setStatus(QueueStatus.WAITING_FOR_TEST);
                        queueTicket.setCalledAt(null);
                    }
                    queueTicketRepo.save(queueTicket);
                }
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

    // Override default method trong interface
    @Override
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
        // Kiem tra neu da COMPLETED thi khong cho tao moi
        if (t.getStatus() == TestRequestStatus.COMPLETED) {
            throw new ConflictException("Yeu cau xet nghiem da hoan thanh, khong the tao ket qua moi");
        }
        if (resultRepo.findByTestRequest_TestRequestId(testRequestId).isPresent()) {
            throw new ConflictException("Yeu cau da co ket qua; dung PUT de cap nhat");
        }
        StaffInfo performedBy = staffRepo.findById(req.performedById())
                .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai: " + req.performedById()));
        TestResult r = TestResult.builder()
                .testRequest(t)
                .imageUrl(req.imageUrl())
                .conclusion(req.conclusion())
                .sampleId(req.sampleId())
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();
        resultRepo.save(r);

        return TestResultResponse.from(r);
    }

    public TestResultResponse updateResult(UUID testRequestId, TestResultUpdateRequest req) {
        TestRequest t = findById(testRequestId);

        // Kiem tra neu da COMPLETED thi khong cho cap nhat (tru khi muon cap nhat lai ket qua)
        if (t.getStatus() == TestRequestStatus.COMPLETED && !Boolean.TRUE.equals(req.complete())) {
            throw new ConflictException("Yeu cau xet nghiem da hoan thanh, khong the cap nhat ket qua");
        }

        TestResult r = resultRepo.findByTestRequest_TestRequestId(t.getTestRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Chua co ket qua de cap nhat"));
        if (req.imageUrl() != null) r.setImageUrl(req.imageUrl());
        if (req.conclusion() != null) r.setConclusion(req.conclusion());
        if (req.sampleId() != null) r.setSampleId(req.sampleId());

        // Neu complete=true: chuyen status sang COMPLETED (neu chua phai COMPLETED)
        if (Boolean.TRUE.equals(req.complete()) && t.getStatus() != TestRequestStatus.COMPLETED) {
            t.setStatus(TestRequestStatus.COMPLETED);
            t.setCompletedAt(LocalDateTime.now());
            repo.save(t);

            // Kiem tra tat ca TestRequest trong medical record de set status TEST_DONE hoac WAITING_FOR_TEST
            if (t.getMedicalRecord() != null && t.getMedicalRecord().getVisit() != null) {
                UUID visitId = t.getMedicalRecord().getVisit().getVisitId();
                long totalTestRequests = repo.countByMedicalRecord_MedicalRecordId(t.getMedicalRecord().getRecordId());
                long incompleteCount = repo.countByMedicalRecordAndStatusIn(
                        t.getMedicalRecord().getRecordId(),
                        java.util.List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS));

                QueueTicket queueTicket = queueTicketRepo.findByVisit_VisitId(visitId).orElse(null);
                if (queueTicket != null) {
                    if (totalTestRequests > 0 && incompleteCount == 0) {
                        queueTicket.setStatus(QueueStatus.TEST_DONE);
                    } else {
                        queueTicket.setStatus(QueueStatus.WAITING_FOR_TEST);
                        queueTicket.setCalledAt(null);
                    }
                    queueTicketRepo.save(queueTicket);
                }
            }
        }

        TestResult saved = resultRepo.save(r);
        return TestResultResponse.from(saved);
    }

    /**
     * Hoan thanh ket qua xet nghiem - TAO MOI hoac CAP NHAT ROI CHUYEN STATUS SANG COMPLETED.
     * Phu hop cho truong hop luu nhap + hoan thanh sau.
     */
    public TestResultResponse completeResult(UUID testRequestId, TestResultCreateRequest req) {
        // Dung findByIdWithResult de eager fetch testResult - tranh lazy loading
        TestRequest t = repo.findByIdWithResult(testRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yeu cau xet nghiem khong ton tai: " + testRequestId));

        // Kiem tra neu da COMPLETED thi khong cho tao/cap nhat nua
        if (t.getStatus() == TestRequestStatus.COMPLETED) {
            throw new ConflictException("Yeu cau xet nghiem da hoan thanh, khong the thay doi ket qua");
        }
        TestResult r;

        if (t.getTestResult() != null) {
            // Neu da co ket qua, cap nhat
            r = t.getTestResult();
            if (req.imageUrl() != null) r.setImageUrl(req.imageUrl());
            if (req.conclusion() != null) r.setConclusion(req.conclusion());
            if (req.sampleId() != null) r.setSampleId(req.sampleId());
        } else {
            // Tao moi
            StaffInfo performedBy = staffRepo.findById(req.performedById())
                    .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai: " + req.performedById()));
            r = TestResult.builder()
                    .testRequest(t)
                    .imageUrl(req.imageUrl())
                    .conclusion(req.conclusion())
                    .sampleId(req.sampleId())
                    .performedBy(performedBy)
                    .performedAt(LocalDateTime.now())
                    .build();
        }

        resultRepo.save(r);

        // Chuyen status sang COMPLETED
        t.setStatus(TestRequestStatus.COMPLETED);
        t.setCompletedAt(LocalDateTime.now());
        repo.save(t);

        // Kiem tra tat ca TestRequest trong medical record de set status TEST_DONE hoac WAITING_FOR_TEST
        if (t.getMedicalRecord() != null && t.getMedicalRecord().getVisit() != null) {
            QueueTicket queueTicket = queueTicketRepo.findByVisit_VisitId(t.getMedicalRecord().getVisit().getVisitId()).orElse(null);
            if (queueTicket != null) {
                long totalTestRequests = repo.countByMedicalRecord_MedicalRecordId(t.getMedicalRecord().getRecordId());
                long incompleteCount = repo.countByMedicalRecordAndStatusIn(
                        t.getMedicalRecord().getRecordId(),
                        java.util.List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS));

                if (totalTestRequests > 0 && incompleteCount == 0) {
                    queueTicket.setStatus(QueueStatus.TEST_DONE);
                } else {
                    queueTicket.setStatus(QueueStatus.WAITING_FOR_TEST);
                    queueTicket.setCalledAt(null);
                }
                queueTicketRepo.save(queueTicket);
            }
        }

        return TestResultResponse.from(r);
    }

    /**
     * Upload ket qua xet nghiem - luu file vao local storage.
     * Tra ve URL de truy cap file.
     */
    public String uploadResultFile(UUID testRequestId, MultipartFile file) throws IOException {
        findById(testRequestId); // Kiem tra test request ton tai

        // Tao thu muc luu tru neu chua co
        Path uploadDir = Paths.get("uploads/test-results");
        Files.createDirectories(uploadDir);

        // Tao ten file duy nhat
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path target = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), target);

        // Tra ve URL (trong moi truong dev)
        return "/uploads/test-results/" + fileName;
    }

    /**
     * Huy yeu cau xet nghiem - chi cho PENDING hoac IN_PROGRESS.
     * Mac dinh: khong cho huy neu da COMPLETED.
     */
    public TestRequestResponse cancel(UUID id, TestRequestCancelRequest req) {
        TestRequest t = findById(id);

        // Chi cho phep huy khi PENDING hoac IN_PROGRESS
        if (t.getStatus() == TestRequestStatus.COMPLETED) {
            throw new org.example.doansummer2026.exception.ConflictException("Khong the huy yeu cau da hoan thanh");
        }
        if (t.getStatus() == TestRequestStatus.CANCELLED) {
            throw new org.example.doansummer2026.exception.ConflictException("Yeu cau da bi huy");
        }

        t.setStatus(TestRequestStatus.CANCELLED);
        t.setCancelReason(req.reason());
        repo.save(t);

        return TestRequestResponse.from(t);
    }

    /**
     * Tao nhieu TestRequest cung luc - bac si chon nhieu dich vu xet nghiem.
     * - Bo qua cac dich vu da ton tai trong medical record.
     */
    public List<TestRequestResponse> createBatch(TestRequestBatchCreateRequest req) {
        MedicalRecord record = recordRepo.findById(req.medicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("Ho so benh an khong ton tai"));
        StaffInfo requestedBy = staffRepo.findById(req.requestedById())
                .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai"));

        // Lay cac service da ton tai de loai bo
        Set<UUID> existingServiceIds = record.getTestRequests().stream()
                .map(t -> t.getService().getServiceId())
                .collect(java.util.stream.Collectors.toSet());

        java.util.List<TestRequest> toCreate = req.serviceIds().stream()
                .filter(serviceId -> !existingServiceIds.contains(serviceId))
                .map((java.util.function.Function<java.util.UUID, TestRequest>) serviceId -> {
                    MedicalService service = serviceRepo.findById(serviceId)
                            .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
                    Department dept = service.getDepartment();
                    if (dept == null) {
                        throw new ResourceNotFoundException("Dich vu chua thiet lap khoa thuc hien: " + serviceId);
                    }
                    return TestRequest.builder()
                            .medicalRecord(record)
                            .service(service)
                            .performingDepartment(dept)
                            .description(req.notes())
                            .requestedBy(requestedBy)
                            .status(TestRequestStatus.PENDING)
                            .build();
                })
                .toList();

        return repo.saveAll(toCreate).stream()
                .map(TestRequestResponse::from)
                .toList();
    }

    /** Tim TestRequest da hoan thanh theo profileId (cho hien thi trong profile). */
    @Transactional(readOnly = true)
    public List<TestRequest> findMyCompletedTests(UUID profileId) {
        return repo.findByProfileIdAndStatusCompleted(profileId);
    }
}




