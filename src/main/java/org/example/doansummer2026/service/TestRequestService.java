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
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.model.InvoiceItem;
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
    private final org.example.doansummer2026.repository.DepartmentRepository departmentRepo;
    private final org.example.doansummer2026.repository.InvoiceItemRepository invoiceItemRepo;
    private final MedicalRecordService medicalRecordService;
    private final PatientJourneyService patientJourneyService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public PageResponse<TestRequestResponse> search(UUID recordId, UUID departmentId,
                                                     TestRequestStatus status, String search,
                                                     Pageable pageable) {
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase();
        Page<TestRequest> page = repo.search(recordId, departmentId, status,
                normalizedSearch, pageable);
        // Tạo bù số phòng cho dữ liệu cũ được sinh trước khi có queue cận lâm sàng.
        page.getContent().stream()
                .filter(test -> test.getQueueTicket() == null)
                .filter(test -> test.getStatus() == TestRequestStatus.PENDING || test.getStatus() == TestRequestStatus.IN_PROGRESS)
                .filter(test -> test.getMedicalRecord() != null && test.getMedicalRecord().getVisit() != null && test.getPerformingDepartment() != null)
                .forEach(test -> {
                    QueueTicket queue = ensureParaclinicalQueue(test.getMedicalRecord(), test.getService(), test.getPerformingDepartment());
                    if (test.getStatus() == TestRequestStatus.IN_PROGRESS && queue.getStatus() == QueueStatus.WAITING) {
                        queue.setStatus(QueueStatus.IN_PROGRESS);
                        queueTicketRepo.save(queue);
                    }
                    test.setQueueTicket(queue);
                    repo.save(test);
                });
        // Phiếu khám WAITING_FOR_TEST là bước tạm treo, không được chặn bệnh nhân sang phòng lab.
        page.getContent().stream()
                .map(TestRequest::getQueueTicket)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(
                        QueueTicket::getTicketId, queue -> queue, (left, right) -> left))
                .values().forEach(queue -> {
                    List<TestRequest> groupedTests = repo.findAllByQueueTicket_TicketId(queue.getTicketId());
                    boolean inProgress = groupedTests.stream()
                            .anyMatch(test -> test.getStatus() == TestRequestStatus.IN_PROGRESS);
                    boolean available = inProgress || groupedTests.stream()
                            .anyMatch(test -> test.getStatus() == TestRequestStatus.PENDING);
                    if (queue.getStatus() == QueueStatus.BLOCKED && available) {
                        queue.setStatus(inProgress ? QueueStatus.IN_PROGRESS : QueueStatus.WAITING);
                        queueTicketRepo.save(queue);
                        groupedTests.stream()
                                .filter(test -> test.getStatus() == TestRequestStatus.BLOCKED)
                                .forEach(test -> {
                                    test.setStatus(TestRequestStatus.PENDING);
                                    repo.save(test);
                                });
                    }
                });
        page.getContent().stream()
                .filter(test -> test.getStatus() == TestRequestStatus.BLOCKED)
                .filter(test -> test.getMedicalRecord() != null && test.getMedicalRecord().getVisit() != null)
                .map(test -> test.getMedicalRecord().getVisit().getVisitId()).distinct()
                .forEach(patientJourneyService::activateNext);
        return PageResponse.from(page, TestRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public TestRequestResponse get(UUID id) {
        return TestRequestResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public List<TestRequestResponse> listByQueueTicket(UUID ticketId) {
        if (!queueTicketRepo.existsById(ticketId)) {
            throw new ResourceNotFoundException("Khong tim thay phieu so can lam sang: " + ticketId);
        }
        return repo.findAllByQueueTicket_TicketId(ticketId).stream()
                .sorted(java.util.Comparator.comparing(TestRequest::getCreatedAt))
                .map(TestRequestResponse::from)
                .toList();
    }

    public TestRequestResponse create(TestRequestCreateRequest req) {
        if (req.invoiceItemId() != null) {
            List<TestRequest> existing = repo.findByInvoiceItem_ItemId(req.invoiceItemId());
            if (!existing.isEmpty()) return TestRequestResponse.from(existing.get(0));
        }
        MedicalRecord record = recordRepo.findById(req.medicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ho so benh an khong ton tai: " + req.medicalRecordId()));
        MedicalService service = serviceRepo.findById(req.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + req.serviceId()));
        Department dept = selectPerformingDepartment(service);
        StaffInfo requestedBy = staffRepo.findById(req.requestedById())
                .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai: " + req.requestedById()));

        // Link voi InvoiceItem neu co (traceability: Invoice -> TestRequest)
        InvoiceItem invoiceItem = null;
        if (req.invoiceItemId() != null) {
            invoiceItem = invoiceItemRepo.findById(req.invoiceItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "InvoiceItem khong ton tai: " + req.invoiceItemId()));
        }

        TestRequest t = TestRequest.builder()
                .medicalRecord(record)
                .service(service)
                .performingDepartment(dept)
                .description(req.notes())
                .requestedBy(requestedBy)
                .status(TestRequestStatus.PENDING)
                .invoiceItem(invoiceItem)
                .build();
        TestRequest saved = repo.save(t);
        
        String patientName = record.getVisit() != null && record.getVisit().getCustomer() != null ? record.getVisit().getCustomer().getFullName() : "Khách";
        notificationService.notifyStaffByRole(
            org.example.doansummer2026.enums.SystemRole.CASHIER,
            "Yêu cầu cận lâm sàng mới",
            String.format("Bệnh nhân %s có chỉ định mới (%s), vui lòng thu phí.", patientName, service.getName()),
            "TestRequest",
            saved.getTestRequestId()
        );
        
        return TestRequestResponse.from(saved);
    }

    /** Tao hang cho sau thanh toan, ke ca luot chi co dich vu can lam sang chua co ho so. */
    public TestRequestResponse createFromPaidInvoice(UUID visitId, UUID medicalRecordId, UUID serviceId,
                                                     UUID requestedById, String notes, UUID invoiceItemId) {
        if (invoiceItemId != null) {
            List<TestRequest> existing = repo.findByInvoiceItem_ItemId(invoiceItemId);
            if (!existing.isEmpty()) return TestRequestResponse.from(existing.get(0));
        }
        MedicalService service = serviceRepo.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
        Department dept = selectPerformingDepartment(service);
        StaffInfo requester = requestedById != null ? staffRepo.findById(requestedById).orElse(null) : null;
        if (requester == null) requester = dept.getHeadDoctor();
        if (requester == null) {
            throw new BadRequestException("Phong " + dept.getName() + " chua co bac si phu trach de tiep nhan dich vu");
        }

        MedicalRecord record = medicalRecordId != null ? recordRepo.findById(medicalRecordId).orElse(null) : null;
        if (record == null && visitId != null)
            record = recordRepo.findFirstByVisit_VisitIdOrderByCreatedAtDesc(visitId).orElse(null);
        if (record == null) {
            var created = medicalRecordService.create(new org.example.doansummer2026.dto.medicalRecord.MedicalRecordCreateRequest(
                    visitId, dept.getHeadDoctor() != null ? dept.getHeadDoctor().getStaffId() : requester.getStaffId(),
                    "Dich vu can lam sang", null, null, null, null, null, null));
            record = recordRepo.findById(created.recordId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khong the tao ho so cho luot kham"));
        }
        InvoiceItem invoiceItem = invoiceItemId != null ? invoiceItemRepo.findById(invoiceItemId).orElse(null) : null;
        QueueTicket labQueueTicket = ensureParaclinicalQueue(record, service, dept);
        TestRequest request = TestRequest.builder().medicalRecord(record).service(service)
                .performingDepartment(dept).description(notes).requestedBy(requester)
                .status(TestRequestStatus.PENDING).invoiceItem(invoiceItem).queueTicket(labQueueTicket).build();
        TestRequest saved = repo.save(request);
        try {
            messagingTemplate.convertAndSend("/topic/department-" + dept.getDepartmentId() + "-lab-queue", "LAB_UPDATED");
        } catch (Exception e) {}
        notifyNurses(saved);
        return TestRequestResponse.from(saved);
    }
    
    private void notifyNurses(TestRequest t) {
        String patientName = t.getMedicalRecord() != null && t.getMedicalRecord().getVisit() != null && t.getMedicalRecord().getVisit().getCustomer() != null ? t.getMedicalRecord().getVisit().getCustomer().getFullName() : "Khach";
        String serviceName = t.getService() != null ? t.getService().getName() : "Can lam sang";
        String content = String.format("Co y lenh moi (%s) can thuc hien cho benh nhan %s", serviceName, patientName);
        
        List<StaffInfo> labStaff = staffRepo.findByDepartment_DepartmentId(t.getPerformingDepartment().getDepartmentId());
        for (StaffInfo staff : labStaff) {
            if (staff.getSystemRole() == org.example.doansummer2026.enums.SystemRole.NURSE && staff.getProfile() != null) {
                try {
                    notificationService.create(new org.example.doansummer2026.dto.notification.NotificationCreateRequest(
                            staff.getProfile().getProfileId(),
                            org.example.doansummer2026.enums.NotificationType.GENERAL,
                            org.example.doansummer2026.enums.NotificationChannel.IN_APP,
                            "Y lenh moi",
                            content,
                            "TestRequest",
                            t.getTestRequestId()
                    ));
                } catch (Exception e) {}
            }
        }
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
                completeStandaloneRecordIfReady(t.getMedicalRecord(), totalTestRequests, incompleteCount);

                QueueTicket queueTicket = queueTicketRepo.findByVisit_VisitIdAndDepartment_DepartmentId(
                        visitId, t.getPerformingDepartment().getDepartmentId()).orElse(null);
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
                if (incompleteCount == 0 && (queueTicket == null || queueTicket.getStatus() != QueueStatus.TEST_DONE))
                    patientJourneyService.activateNext(visitId);
            }
        }
        TestRequest saved = repo.save(t);
        try {
            messagingTemplate.convertAndSend("/topic/department-" + saved.getPerformingDepartment().getDepartmentId() + "-lab-queue", "LAB_UPDATED");
        } catch (Exception e) {}
        
        if (req.status() == TestRequestStatus.COMPLETED) {
            notifyDoctorResult(saved);
        }
        
        return TestRequestResponse.from(saved);
    }
    
    private void notifyDoctorResult(TestRequest t) {
        if (t.getRequestedBy() == null || t.getRequestedBy().getProfile() == null) return;
        String patientName = t.getMedicalRecord() != null && t.getMedicalRecord().getVisit() != null && t.getMedicalRecord().getVisit().getCustomer() != null ? t.getMedicalRecord().getVisit().getCustomer().getFullName() : "Khach";
        String serviceName = t.getService() != null ? t.getService().getName() : "Can lam sang";
        String content = String.format("Benh nhan %s da co ket qua %s", patientName, serviceName);
        
        try {
            notificationService.create(new org.example.doansummer2026.dto.notification.NotificationCreateRequest(
                    t.getRequestedBy().getProfile().getProfileId(),
                    org.example.doansummer2026.enums.NotificationType.GENERAL,
                    org.example.doansummer2026.enums.NotificationChannel.IN_APP,
                    "Ket qua xet nghiem",
                    content,
                    "TestRequest",
                    t.getTestRequestId()
            ));
        } catch (Exception e) {}
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

        if (t.getStatus() == TestRequestStatus.PENDING) {
            t.setStatus(TestRequestStatus.IN_PROGRESS);
            repo.save(t);
        }

        return TestResultResponse.from(r);
    }

    public TestResultResponse updateResult(UUID testRequestId, TestResultUpdateRequest req) {
        TestRequest t = findById(testRequestId);

        if (Boolean.TRUE.equals(req.complete())) {
            throw new BadRequestException("Vui long dung chuc nang ky xac nhan cua bac si de hoan thanh ket qua");
        }

        // Kiem tra neu da COMPLETED thi khong cho cap nhat (tru khi muon cap nhat lai ket qua)
        if (t.getStatus() == TestRequestStatus.COMPLETED && !Boolean.TRUE.equals(req.complete())) {
            throw new ConflictException("Yeu cau xet nghiem da hoan thanh, khong the cap nhat ket qua");
        }

        TestResult r = resultRepo.findByTestRequest_TestRequestId(t.getTestRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Chua co ket qua de cap nhat"));
        if (req.imageUrl() != null) r.setImageUrl(req.imageUrl());
        if (req.conclusion() != null) r.setConclusion(req.conclusion());
        if (req.sampleId() != null) r.setSampleId(req.sampleId());

        if (t.getStatus() == TestRequestStatus.PENDING) {
            t.setStatus(TestRequestStatus.IN_PROGRESS);
            repo.save(t);
        }

        TestResult saved = resultRepo.save(r);
        return TestResultResponse.from(saved);
    }

    /**
     * Hoan thanh ket qua xet nghiem - TAO MOI hoac CAP NHAT ROI CHUYEN STATUS SANG COMPLETED.
     * Phu hop cho truong hop luu nhap + hoan thanh sau.
     */
    public TestResultResponse completeResult(UUID testRequestId, TestResultCreateRequest req, UUID verifiedById) {
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

        if (r.getConclusion() == null || r.getConclusion().isBlank()) {
            throw new BadRequestException("Vui long nhap ket luan cua bac si");
        }
        if (r.getImageUrl() == null || r.getImageUrl().isBlank()
                || !r.getImageUrl().toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Vui long tai phieu ket qua dinh dang PDF");
        }

        StaffInfo verifier = staffRepo.findById(verifiedById)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay bac si xac nhan ket qua"));
        if (verifier.getSystemRole() == null || !verifier.getSystemRole().isDoctor()) {
            throw new BadRequestException("Chi bac si moi duoc ky xac nhan va hoan thanh ket qua");
        }
        Department performingDepartment = t.getPerformingDepartment();
        if (performingDepartment == null || performingDepartment.getHeadDoctor() == null
                || !performingDepartment.getHeadDoctor().getStaffId().equals(verifier.getStaffId())) {
            throw new BadRequestException("Chi bac si phu trach phong moi duoc ky va hoan thanh ket qua");
        }
        r.setVerifiedBy(verifier);
        r.setVerifiedAt(LocalDateTime.now());

        resultRepo.save(r);

        // Chuyen status sang COMPLETED
        t.setStatus(TestRequestStatus.COMPLETED);
        t.setCompletedAt(LocalDateTime.now());
        repo.save(t);

        // Hoàn thành phiếu gọi số cận lâm sàng khi mọi kỹ thuật trong cùng phiếu đã xong.
        if (t.getQueueTicket() != null) {
            UUID labTicketId = t.getQueueTicket().getTicketId();
            long remainingInLabQueue = repo.countByQueueTicket_TicketIdAndStatusIn(
                    labTicketId, java.util.List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS, TestRequestStatus.BLOCKED));
            if (remainingInLabQueue == 0) {
                t.getQueueTicket().setStatus(QueueStatus.DONE);
                t.getQueueTicket().setCompletedAt(LocalDateTime.now());
                queueTicketRepo.save(t.getQueueTicket());
            }
        }

        // Kiem tra tat ca TestRequest trong medical record de set status TEST_DONE hoac WAITING_FOR_TEST
        if (t.getMedicalRecord() != null && t.getMedicalRecord().getVisit() != null) {
            QueueTicket queueTicket = queueTicketRepo.findAllByVisit_VisitId(t.getMedicalRecord().getVisit().getVisitId()).stream()
                    .filter(qt -> qt.getStatus() == QueueStatus.WAITING_FOR_TEST || qt.getStatus() == QueueStatus.TEST_DONE)
                    .findFirst()
                    .orElse(null);
            if (queueTicket != null) {
                long totalTestRequests = repo.countByMedicalRecord_MedicalRecordId(t.getMedicalRecord().getRecordId());
                long incompleteCount = repo.countByMedicalRecordAndStatusIn(
                        t.getMedicalRecord().getRecordId(),
                        java.util.List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS));

                completeStandaloneRecordIfReady(t.getMedicalRecord(), totalTestRequests, incompleteCount);
                if (totalTestRequests > 0 && incompleteCount == 0) {
                    queueTicket.setStatus(QueueStatus.TEST_DONE);
                } else {
                    queueTicket.setStatus(QueueStatus.WAITING_FOR_TEST);
                    queueTicket.setCalledAt(null);
                }
                queueTicketRepo.save(queueTicket);
            }
            if (queueTicket == null || queueTicket.getStatus() != QueueStatus.TEST_DONE)
                patientJourneyService.activateNext(t.getMedicalRecord().getVisit().getVisitId());
        }

        return TestResultResponse.from(r);
    }

    /** Lượt chỉ làm cận lâm sàng không quay lại phòng khám, nên tự đóng hồ sơ khi đủ kết quả. */
    private void completeStandaloneRecordIfReady(MedicalRecord record, long total, long incomplete) {
        if (record != null && record.getQueueTicket() == null && total > 0 && incomplete == 0
                && record.getStatus() != MedicalRecordStatus.COMPLETED) {
            record.setStatus(MedicalRecordStatus.COMPLETED);
            record.setCompletedAt(LocalDateTime.now());
            recordRepo.save(record);
        }
    }

    /**
     * Upload ket qua xet nghiem - luu file vao local storage.
     * Tra ve URL de truy cap file.
     */
    public String uploadResultFile(UUID testRequestId, MultipartFile file) throws IOException {
        findById(testRequestId); // Kiem tra test request ton tai

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Tep PDF khong duoc de trong");
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "result.pdf";
        boolean pdfExtension = originalName.toLowerCase().endsWith(".pdf");
        boolean pdfContentType = "application/pdf".equalsIgnoreCase(file.getContentType());
        if (!pdfExtension || !pdfContentType) {
            throw new BadRequestException("Chi chap nhan phieu ket qua dinh dang PDF");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new BadRequestException("Tep PDF khong duoc vuot qua 10 MB");
        }

        // Tao thu muc luu tru neu chua co
        Path uploadDir = Paths.get("uploads/test-results");
        Files.createDirectories(uploadDir);

        // Tao ten file duy nhat
        String safeName = Paths.get(originalName).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = System.currentTimeMillis() + "_" + safeName;
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
     * - invoiceItemId: lien ket voi InvoiceItem tu hoa don (de trace luong Invoice -> TestRequest).
     */
    public List<TestRequestResponse> createBatch(TestRequestBatchCreateRequest req) {
        MedicalRecord record = recordRepo.findById(req.medicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("Ho so benh an khong ton tai"));
        StaffInfo requestedBy = staffRepo.findById(req.requestedById())
                .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai"));

        // Link voi InvoiceItem neu co
        InvoiceItem invoiceItem = null;
        if (req.invoiceItemId() != null) {
            invoiceItem = invoiceItemRepo.findById(req.invoiceItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "InvoiceItem khong ton tai: " + req.invoiceItemId()));
        }

        // Lay cac service da ton tai de loai bo
        Set<UUID> existingServiceIds = record.getTestRequests().stream()
                .map(t -> t.getService().getServiceId())
                .collect(java.util.stream.Collectors.toSet());

        InvoiceItem finalInvoiceItem = invoiceItem;
        java.util.List<TestRequest> toCreate = req.serviceIds().stream()
                .filter(serviceId -> !existingServiceIds.contains(serviceId))
                .map((java.util.function.Function<java.util.UUID, TestRequest>) serviceId -> {
                    MedicalService service = serviceRepo.findById(serviceId)
                            .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
                    Department dept = selectPerformingDepartment(service);
                    return TestRequest.builder()
                            .medicalRecord(record)
                            .service(service)
                            .performingDepartment(dept)
                            .description(req.notes())
                            .requestedBy(requestedBy)
                            .status(TestRequestStatus.PENDING)
                            .invoiceItem(finalInvoiceItem)
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

    /** Tim TestRequest theo InvoiceItem (traceability: Invoice -> InvoiceItem -> TestRequest). */
    @Transactional(readOnly = true)
    public List<TestRequestResponse> findByInvoiceItem(UUID itemId) {
        return repo.findByInvoiceItem_ItemId(itemId).stream()
                .map(TestRequestResponse::from)
                .toList();
    }

    /** Tim TestRequest theo Invoice (traceability: Invoice -> InvoiceItem -> TestRequest). */
    @Transactional(readOnly = true)
    public List<TestRequestResponse> findByInvoice(UUID invoiceId) {
        return repo.findByInvoiceId(invoiceId).stream()
                .map(TestRequestResponse::from)
                .toList();
    }

    /** Điểm mở rộng cho AI: hiện dùng rule cứng + tải hàng đợi, sau này có thể thay bộ xếp hạng. */
    private Department selectPerformingDepartment(MedicalService service) {
        if (service.getRequiredCapability() == null) {
            if (service.getDepartment() != null) return service.getDepartment();
            throw new ResourceNotFoundException("Dich vu chua chon nang luc thuc hien: " + service.getServiceId());
        }
        List<Department> candidates = departmentRepo.findEligibleByCapability(
                service.getRequiredCapability().getCapabilityId());
        return candidates.stream()
                .min(java.util.Comparator.comparingLong(department ->
                        repo.countByPerformingDepartment_DepartmentIdAndStatusIn(
                                department.getDepartmentId(),
                                List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS))))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong co phong dang hoat dong ho tro nang luc: " + service.getRequiredCapability().getName()));
    }

    /** Tạo một số gọi cho mỗi phòng/lượt; các kỹ thuật cùng phòng được gom chung số. */
    private QueueTicket ensureParaclinicalQueue(MedicalRecord record, MedicalService service, Department department) {
        UUID visitId = record.getVisit().getVisitId();
        QueueTicket existing = queueTicketRepo.findByVisit_VisitIdAndDepartment_DepartmentId(visitId, department.getDepartmentId()).orElse(null);
        if (existing != null && existing.getStatus() != QueueStatus.DONE && existing.getStatus() != QueueStatus.SKIPPED) return existing;
        java.time.LocalDate workDate = java.time.LocalDate.now();
        int nextNumber = queueTicketRepo.findMaxQueueNumberForDay(department.getDepartmentId(), workDate).orElse(0) + 1;
        return queueTicketRepo.save(QueueTicket.builder()
                .visit(record.getVisit()).department(department).service(service)
                .workDate(workDate).queueNumber(nextNumber).status(QueueStatus.WAITING).build());
    }
}
