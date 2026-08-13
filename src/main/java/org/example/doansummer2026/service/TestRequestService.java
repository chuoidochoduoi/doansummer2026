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
import org.example.doansummer2026.enums.DepartmentStatus;
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

    @org.springframework.beans.factory.annotation.Value("${app.upload.root:uploads}")
    private String uploadRoot;

    private final TestRequestRepository repo;
    private final TestResultRepository resultRepo;
    private final MedicalRecordRepository recordRepo;
    private final org.example.doansummer2026.repository.CustomerVisitRepository visitRepo;
    private final MedicalServiceRepository serviceRepo;
    private final StaffInfoRepository staffRepo;
    private final QueueTicketRepository queueTicketRepo;
    private final org.example.doansummer2026.repository.DepartmentRepository departmentRepo;
    private final org.example.doansummer2026.repository.InvoiceItemRepository invoiceItemRepo;
    private final MedicalRecordService medicalRecordService;
    private final PatientJourneyService patientJourneyService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final AuthService authService;

    public PageResponse<TestRequestResponse> search(UUID recordId, UUID departmentId,
                                                     TestRequestStatus status, String search,
                                                     java.time.LocalDate workDate,
                                                     Pageable pageable) {
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase();
        Page<TestRequest> page = repo.search(recordId, departmentId, status,
                normalizedSearch, workDate, pageable);
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
        return PageResponse.from(page, TestRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public TestRequestResponse get(UUID id) {
        return TestRequestResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public List<TestRequestResponse> listByQueueTicket(UUID ticketId) {
        if (!queueTicketRepo.existsById(ticketId)) {
            throw new ResourceNotFoundException("Không tìm thấy phiếu cận lâm sàng: " + ticketId);
        }
        return repo.findAllByQueueTicket_TicketId(ticketId).stream()
                .sorted(java.util.Comparator.comparing(TestRequest::getCreatedAt))
                .map(TestRequestResponse::from)
                .toList();
    }

    /** Bat dau xu ly cac yeu cau sau khi QueueTicket da chuyen sang IN_PROGRESS. */
    public void startRequestsForQueue(UUID ticketId) {
        repo.findAllByQueueTicket_TicketId(ticketId).stream()
                .filter(request -> request.getStatus() == TestRequestStatus.PENDING)
                .forEach(request -> {
                    request.setStatus(TestRequestStatus.IN_PROGRESS);
                    repo.save(request);
                });
    }

    public TestRequestResponse create(TestRequestCreateRequest req) {
        if (req.invoiceItemId() != null) {
            List<TestRequest> existing = repo.findByInvoiceItem_ItemId(req.invoiceItemId());
            if (!existing.isEmpty()) return TestRequestResponse.from(existing.get(0));
        }
        MedicalRecord record = recordRepo.findById(req.medicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hồ sơ bệnh án không tồn tại: " + req.medicalRecordId()));
        MedicalService service = serviceRepo.findById(req.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + req.serviceId()));
        ensureServiceNotAlreadyRequested(record, service.getServiceId());
        Department dept = selectPerformingDepartment(service);
        StaffInfo requestedBy = staffRepo.findById(req.requestedById())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại: " + req.requestedById()));

        // Link voi InvoiceItem neu co (traceability: Invoice -> TestRequest)
        InvoiceItem invoiceItem = null;
        if (req.invoiceItemId() != null) {
            invoiceItem = invoiceItemRepo.findById(req.invoiceItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Dòng hóa đơn không tồn tại: " + req.invoiceItemId()));
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

    /** Dung cho cac luong tao hoa don tu man kham, noi TestRequest chi duoc sinh sau thanh toan. */
    public void ensureServiceNotAlreadyRequested(UUID medicalRecordId, UUID serviceId) {
        MedicalRecord record = recordRepo.findById(medicalRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ bệnh án không tồn tại: " + medicalRecordId));
        ensureServiceNotAlreadyRequested(record, serviceId);
    }

    /** Tao hang cho sau thanh toan, ke ca luot chi co dich vu can lam sang chua co ho so. */
    public TestRequestResponse createFromPaidInvoice(UUID visitId, UUID medicalRecordId, UUID serviceId,
                                                     UUID requestedById, String notes, UUID invoiceItemId) {
        MedicalService service = serviceRepo.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId));

        /*
         * Idempotency khong dong nghia voi return ngay lap tuc.
         * Mot so luong cu/luong dat dich vu co the da tao TestRequest truoc khi
         * hoa don duoc thanh toan. Ban ghi do chua co QueueTicket; neu return o
         * day thi PAID thanh cong nhung benh nhan khong bao gio vao hang cho.
         */
        TestRequest existingRequest = invoiceItemId == null ? null : repo.findByInvoiceItem_ItemId(invoiceItemId)
                .stream().findFirst().orElse(null);

        Department dept = existingRequest != null && existingRequest.getPerformingDepartment() != null
                ? existingRequest.getPerformingDepartment()
                : selectPerformingDepartment(service);
        StaffInfo requester = resolvePaymentRequester(requestedById, dept);
        if (requester == null) {
            throw new BadRequestException("Phòng " + dept.getName()
                    + " chưa có nhân sự phụ trách để tiếp nhận dịch vụ");
        }

        /*
         * Yeu cau da duoc bac si chi dinh phai giu MedicalRecord goc de sau khi
         * co du ket qua, hanh trinh biet can dua benh nhan quay lai dung bac si.
         * Chi dich vu CLS mua truc tiep (khong co TestRequest truoc thanh toan)
         * moi dung standalone record va tu ket thuc sau khi co du ket qua.
         */
        MedicalRecord record = existingRequest != null ? existingRequest.getMedicalRecord() : null;
        if (record == null && medicalRecordId != null) {
            record = recordRepo.findById(medicalRecordId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Hồ sơ chỉ định cận lâm sàng không tồn tại: " + medicalRecordId));
            if (record.getVisit() == null || !visitId.equals(record.getVisit().getVisitId())) {
                throw new BadRequestException("Hồ sơ chỉ định không thuộc lượt khám của hóa đơn");
            }
        }
        if (record == null) {
            record = getOrCreateStandaloneRecord(visitId, requester, dept);
        }

        if (existingRequest != null) {
            boolean changed = false;
            if (existingRequest.getQueueTicket() == null) {
                QueueTicket repairedQueue = ensureParaclinicalQueue(record, service, dept);
                existingRequest.setQueueTicket(repairedQueue);
                changed = true;
                // Khong doi trang thai cua ket qua da hoan thanh/huy. Cac yeu cau
                // dang cho phai phan anh dung trang thai cua queue vua gan.
                if (existingRequest.getStatus() != TestRequestStatus.COMPLETED
                        && existingRequest.getStatus() != TestRequestStatus.CANCELLED) {
                    existingRequest.setStatus(repairedQueue.getStatus() == QueueStatus.BLOCKED
                            ? TestRequestStatus.BLOCKED : TestRequestStatus.PENDING);
                    changed = true;
                }
            } else if (existingRequest.getStatus() != TestRequestStatus.COMPLETED
                    && existingRequest.getStatus() != TestRequestStatus.CANCELLED) {
                // Du lieu cu co the da gan ticket nhung lech trang thai do ticket
                // bi block/mo sau khi TestRequest duoc tao. Dong bo lai tai diem
                // thanh toan ma khong lam song lai ket qua da xong.
                QueueStatus queueStatus = existingRequest.getQueueTicket().getStatus();
                if (queueStatus == QueueStatus.BLOCKED
                        && existingRequest.getStatus() != TestRequestStatus.BLOCKED) {
                    existingRequest.setStatus(TestRequestStatus.BLOCKED);
                    changed = true;
                } else if (queueStatus != QueueStatus.BLOCKED
                        && existingRequest.getStatus() == TestRequestStatus.BLOCKED) {
                    existingRequest.setStatus(TestRequestStatus.PENDING);
                    changed = true;
                }
            }
            if (changed) {
                existingRequest = repo.save(existingRequest);
                publishLabQueueUpdated(dept.getDepartmentId());
            }
            return TestRequestResponse.from(existingRequest);
        }

        InvoiceItem invoiceItem = invoiceItemId != null ? invoiceItemRepo.findById(invoiceItemId).orElse(null) : null;
        QueueTicket labQueueTicket = ensureParaclinicalQueue(record, service, dept);
        TestRequest request = TestRequest.builder().medicalRecord(record).service(service)
                .performingDepartment(dept).description(notes).requestedBy(requester)
                .status(labQueueTicket.getStatus() == QueueStatus.BLOCKED
                        ? TestRequestStatus.BLOCKED : TestRequestStatus.PENDING)
                .invoiceItem(invoiceItem).queueTicket(labQueueTicket).build();
        TestRequest saved = repo.save(request);
        publishLabQueueUpdated(dept.getDepartmentId());
        notifyNurses(saved);
        return TestRequestResponse.from(saved);
    }

    /**
     * TestRequest sinh tu hoa don la yeu cau he thong sau khi thu ngan xac nhan
     * thanh toan; no khong phai chi dinh cua bac si. Vi vay khong duoc chan luong
     * chi vi phong CLS chua gan headDoctor. Uu tien dung nhan vien thu ngan/nguoi
     * lap hoa don, sau do moi dung bac si phu trach hoac bat ky nhan su cua phong.
     */
    private StaffInfo resolvePaymentRequester(UUID requestedById, Department department) {
        if (requestedById != null) {
            StaffInfo requestedBy = staffRepo.findById(requestedById).orElse(null);
            if (requestedBy != null) return requestedBy;
        }
        if (department.getHeadDoctor() != null) return department.getHeadDoctor();
        return staffRepo.findByDepartment_DepartmentId(department.getDepartmentId()).stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Mot visit chi co duy nhat mot MedicalRecord khong gan QueueTicket de chua
     * toan bo CLS. Khoa visit truoc khi tim/tao de cac InvoiceItem xu ly dong
     * thoi khong sinh ra hai standalone record.
     */
    private MedicalRecord getOrCreateStandaloneRecord(UUID visitId, StaffInfo requester, Department department) {
        if (visitId == null) {
            throw new BadRequestException("Không thể tạo yêu cầu cận lâm sàng khi chưa có lượt khám");
        }
        visitRepo.findByIdForUpdate(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Lượt khám không tồn tại: " + visitId));

        MedicalRecord standalone = recordRepo
                .findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(visitId)
                .orElse(null);
        if (standalone != null) return standalone;

        StaffInfo responsibleStaff = department.getHeadDoctor() != null
                ? department.getHeadDoctor() : requester;
        var created = medicalRecordService.create(
                new org.example.doansummer2026.dto.medicalRecord.MedicalRecordCreateRequest(
                        visitId,
                        responsibleStaff.getStaffId(),
                        "Dich vu can lam sang",
                        null, null, null, null, null, null));
        return recordRepo.findById(created.recordId())
                .orElseThrow(() -> new ResourceNotFoundException("Không thể tạo hồ sơ cận lâm sàng cho lượt khám"));
    }

    private void publishLabQueueUpdated(UUID departmentId) {
        try {
            messagingTemplate.convertAndSend("/topic/department-" + departmentId + "-lab-queue", "LAB_UPDATED");
        } catch (Exception ignored) {
            // Khong de WebSocket lam huy giao dich nghiep vu.
        }
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
                        java.util.List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS, TestRequestStatus.BLOCKED));
                completeStandaloneRecordIfReady(t.getMedicalRecord(), totalTestRequests, incompleteCount);

                QueueTicket queueTicket = queueTicketRepo
                        .findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(
                                visitId,
                                t.getPerformingDepartment().getDepartmentId(),
                                List.of(QueueStatus.DONE, QueueStatus.SKIPPED))
                        .orElse(null);
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
            throw new ResourceNotFoundException("Yêu cầu cận lâm sàng không tồn tại: " + id);
        }
        repo.deleteById(id);
    }

    // Override default method trong interface
    @Override
    public TestRequest findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu cận lâm sàng không tồn tại: " + id));
    }

    // --- TestResult sub-resource ---

    @Transactional(readOnly = true)
    public TestResultResponse getResult(UUID testRequestId) {
        TestRequest t = findById(testRequestId);
        TestResult r = resultRepo.findByTestRequest_TestRequestId(t.getTestRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có kết quả cho yêu cầu này"));
        return TestResultResponse.from(r);
    }

    public TestResultResponse createResult(UUID testRequestId, TestResultCreateRequest req) {
        TestRequest t = findById(testRequestId);
        // Kiem tra neu da COMPLETED thi khong cho tao moi
        if (t.getStatus() == TestRequestStatus.COMPLETED) {
            throw new ConflictException("Yêu cầu cận lâm sàng đã hoàn thành, không thể tạo kết quả mới");
        }
        if (resultRepo.findByTestRequest_TestRequestId(testRequestId).isPresent()) {
            throw new ConflictException("Yêu cầu đã có kết quả; vui lòng dùng chức năng cập nhật");
        }
        StaffInfo performedBy = staffRepo.findById(req.performedById())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại: " + req.performedById()));
        TestResult r = TestResult.builder()
                .testRequest(t)
                .imageUrl(req.imageUrl())
                .conclusion(req.conclusion())
                .sampleId(req.sampleId())
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();
        applySpecimenInformation(t, r, req.sampleId(), req.sampleType(), req.sampleStatus());
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
            throw new BadRequestException("Vui lòng dùng chức năng ký xác nhận của bác sĩ để hoàn thành kết quả");
        }

        // Kiem tra neu da COMPLETED thi khong cho cap nhat (tru khi muon cap nhat lai ket qua)
        if (t.getStatus() == TestRequestStatus.COMPLETED && !Boolean.TRUE.equals(req.complete())) {
            throw new ConflictException("Yêu cầu cận lâm sàng đã hoàn thành, không thể cập nhật kết quả");
        }

        TestResult r = resultRepo.findByTestRequest_TestRequestId(t.getTestRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có kết quả để cập nhật"));
        updateResultFileUrl(r, req.imageUrl());
        if (req.conclusion() != null) r.setConclusion(req.conclusion());
        if (req.sampleId() != null) r.setSampleId(req.sampleId());
        applySpecimenInformation(t, r, req.sampleId(), req.sampleType(), req.sampleStatus());

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
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu cận lâm sàng không tồn tại: " + testRequestId));

        // Kiem tra neu da COMPLETED thi khong cho tao/cap nhat nua
        if (t.getStatus() == TestRequestStatus.COMPLETED) {
            throw new ConflictException("Yêu cầu cận lâm sàng đã hoàn thành, không thể thay đổi kết quả");
        }
        QueueTicket executionQueue = t.getQueueTicket();
        if (executionQueue == null || (executionQueue.getStatus() != QueueStatus.IN_PROGRESS
                && executionQueue.getStatus() != QueueStatus.DONE)) {
            throw new BadRequestException("Chỉ có thể hoàn thành kết quả sau khi bệnh nhân đã vào phòng thực hiện");
        }
        TestResult r;

        if (t.getTestResult() != null) {
            // Neu da co ket qua, cap nhat
            r = t.getTestResult();
            updateResultFileUrl(r, req.imageUrl());
            if (req.conclusion() != null) r.setConclusion(req.conclusion());
            if (req.sampleId() != null) r.setSampleId(req.sampleId());
            applySpecimenInformation(t, r, req.sampleId(), req.sampleType(), req.sampleStatus());
        } else {
            // Tao moi
            StaffInfo performedBy = staffRepo.findById(req.performedById())
                    .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại: " + req.performedById()));
            r = TestResult.builder()
                    .testRequest(t)
                    .imageUrl(req.imageUrl())
                    .conclusion(req.conclusion())
                    .sampleId(req.sampleId())
                    .performedBy(performedBy)
                    .performedAt(LocalDateTime.now())
                    .build();
            applySpecimenInformation(t, r, req.sampleId(), req.sampleType(), req.sampleStatus());
        }

        if (req.sampleStatus() == org.example.doansummer2026.enums.SpecimenStatus.REJECTED || req.sampleStatus() == org.example.doansummer2026.enums.SpecimenStatus.RECOLLECT) {
            throw new BadRequestException("Không thể hoàn thành kết quả khi mẫu vật bị hỏng hoặc cần lấy lại");
        }

        if (r.getConclusion() == null || r.getConclusion().isBlank()) {
            throw new BadRequestException("Vui lòng nhập kết luận của bác sĩ");
        }
        if (r.getImageUrl() == null || r.getImageUrl().isBlank()
                || !r.getImageUrl().toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Vui lòng tải phiếu kết quả định dạng PDF");
        }

        StaffInfo verifier = staffRepo.findById(verifiedById)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bác sĩ xác nhận kết quả"));
        if (verifier.getSystemRole() == null || !verifier.getSystemRole().isDoctor()) {
            throw new BadRequestException("Chỉ bác sĩ mới được ký xác nhận và hoàn thành kết quả");
        }
        Department performingDepartment = t.getPerformingDepartment();
        if (performingDepartment == null || performingDepartment.getHeadDoctor() == null
                || !performingDepartment.getHeadDoctor().getStaffId().equals(verifier.getStaffId())) {
            throw new BadRequestException("Chỉ bác sĩ phụ trách phòng mới được ký và hoàn thành kết quả");
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
                        java.util.List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS, TestRequestStatus.BLOCKED));

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

        publishLabQueueUpdated(t.getPerformingDepartment().getDepartmentId());
        return TestResultResponse.from(r);
    }

    /**
     * Chi ap dung mau vat cho dich vu duoc admin cau hinh requiresSpecimen. Thoi gian va
     * nguoi lay mau chi duoc ghi o lan luu dau tien va luon lay tu tai khoan dang dang nhap.
     */
    private void applySpecimenInformation(TestRequest request, TestResult result,
                                          String sampleId,
                                          org.example.doansummer2026.enums.SpecimenType sampleType,
                                          org.example.doansummer2026.enums.SpecimenStatus sampleStatus) {
        String normalizedSampleId = sampleId == null || sampleId.isBlank() ? null : sampleId.trim();
        boolean hasSpecimenInput = normalizedSampleId != null || sampleType != null || sampleStatus != null;
        boolean specimenService = request.getService() != null
                && Boolean.TRUE.equals(request.getService().getRequiresSpecimen());
        if (!specimenService) {
            if (hasSpecimenInput) {
                throw new BadRequestException("Dịch vụ này không sử dụng mẫu vật");
            }
            result.setSampleId(null);
            result.setSampleType(null);
            result.setSampleStatus(null);
            return;
        }
        if (normalizedSampleId != null) result.setSampleId(normalizedSampleId);
        if (sampleType != null) result.setSampleType(sampleType);
        if (sampleStatus != null) result.setSampleStatus(sampleStatus);
        if (hasSpecimenInput && result.getCollectedAt() == null) {
            StaffInfo collector = authService.currentStaffId() == null ? null
                    : staffRepo.findById(authService.currentStaffId()).orElse(null);
            if (collector == null) {
                throw new BadRequestException("Không tìm thấy nhân viên đang lấy mẫu");
            }
            result.setCollectedAt(LocalDateTime.now());
            result.setCollectedBy(collector);
        }
    }

    /**
     * TestResultResponse tra URL xem PDF qua endpoint bao ve. Khi frontend gui lai URL nay
     * trong luc luu nhap/ky ket qua, giu nguyen duong dan tep goc thay vi ghi de bang URL xem.
     */
    private void updateResultFileUrl(TestResult result, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        String protectedFileUrl = "/api/v1/test-results/" + result.getResultId() + "/file";
        if (!protectedFileUrl.equals(imageUrl)) {
            result.setImageUrl(imageUrl);
        }
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
            throw new BadRequestException("Tệp PDF không được để trống");
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "result.pdf";
        boolean pdfExtension = originalName.toLowerCase().endsWith(".pdf");
        boolean pdfContentType = "application/pdf".equalsIgnoreCase(file.getContentType());
        if (!pdfExtension || !pdfContentType) {
            throw new BadRequestException("Chỉ chấp nhận phiếu kết quả định dạng PDF");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new BadRequestException("Tệp PDF không được vượt quá 10 MB");
        }
        byte[] signature = new byte[5];
        try (var input = file.getInputStream()) {
            if (input.read(signature) != signature.length
                    || !java.util.Arrays.equals(signature, "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
                throw new BadRequestException("Nội dung tệp không phải định dạng PDF hợp lệ");
            }
        }

        // Tao thu muc luu tru neu chua co
        Path uploadDir = Paths.get(uploadRoot, "test-results");
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
            throw new org.example.doansummer2026.exception.ConflictException("Không thể hủy yêu cầu đã hoàn thành");
        }
        if (t.getStatus() == TestRequestStatus.CANCELLED) {
            throw new org.example.doansummer2026.exception.ConflictException("Yêu cầu đã bị hủy");
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
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ bệnh án không tồn tại"));
        StaffInfo requestedBy = staffRepo.findById(req.requestedById())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại"));

        // Link voi InvoiceItem neu co
        InvoiceItem invoiceItem = null;
        if (req.invoiceItemId() != null) {
            invoiceItem = invoiceItemRepo.findById(req.invoiceItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Dòng hóa đơn không tồn tại: " + req.invoiceItemId()));
        }

        InvoiceItem finalInvoiceItem = invoiceItem;
        java.util.List<TestRequest> toCreate = req.serviceIds().stream()
                .distinct()
                .map((java.util.function.Function<java.util.UUID, TestRequest>) serviceId -> {
                    MedicalService service = serviceRepo.findById(serviceId)
                            .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId));
                    ensureServiceNotAlreadyRequested(record, serviceId);
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

    /** Chan chi dinh lap dich vu trong cung CustomerVisit, ke ca khi y lenh truoc do
     * nam o mot MedicalRecord khac cua cung luot kham. */
    private void ensureServiceNotAlreadyRequested(MedicalRecord record, UUID serviceId) {
        if (record == null || record.getVisit() == null) return;
        UUID visitId = record.getVisit().getVisitId();
        if (repo.existsByMedicalRecord_Visit_VisitIdAndService_ServiceIdAndStatusNot(
                visitId, serviceId, TestRequestStatus.CANCELLED)) {
            throw new ConflictException("Dịch vụ này đã được chỉ định trong lượt khám hiện tại.");
        }
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
            if (service.getDepartment() != null) {
                Department configuredDepartment = service.getDepartment();
                if (configuredDepartment.getStatus() == DepartmentStatus.MAINTENANCE) {
                    throw new BadRequestException("Phòng thực hiện dịch vụ " + service.getName()
                            + " hiện không sẵn sàng");
                }
                return configuredDepartment;
            }
            throw new ResourceNotFoundException("Dịch vụ chưa chọn danh mục kỹ thuật: " + service.getServiceId());
        }
        List<Department> candidates = departmentRepo.findEligibleByCapability(
                service.getRequiredCapability().getCapabilityId());
        return candidates.stream()
                .min(java.util.Comparator.comparingLong(department ->
                        repo.countByPerformingDepartment_DepartmentIdAndStatusIn(
                                department.getDepartmentId(),
                                List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS))))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không có phòng đang hoạt động hỗ trợ danh mục kỹ thuật: " + service.getRequiredCapability().getName()));
    }

    /** Tạo một số gọi cho mỗi phòng/lượt; các kỹ thuật cùng phòng được gom chung số. */
    private QueueTicket ensureParaclinicalQueue(MedicalRecord record, MedicalService service, Department department) {
        UUID visitId = record.getVisit().getVisitId();
        // Khoa phong truoc khi kiem tra/tang so de cac request dong thoi khong tao trung ticket/so goi.
        department = departmentRepo.findByIdForUpdate(department.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng thực hiện không tồn tại"));
        QueueTicket existing = queueTicketRepo
                .findTopByVisit_VisitIdAndDepartment_DepartmentIdAndStatusNotInOrderByCreatedAtDesc(
                        visitId,
                        department.getDepartmentId(),
                        List.of(QueueStatus.DONE, QueueStatus.SKIPPED))
                .orElse(null);
        // Tat ca yeu cau can lam sang cung phong trong cung dot dung chung mot so goi.
        if (existing != null) return existing;

        // Chi buoc dau tien cua luot kham duoc mo. Cac phong con lai giu
        // BLOCKED va se duoc PatientJourneyService.activateNext() mo tuan tu.
        boolean hasActiveWorkflowStep = patientJourneyService.hasActiveStep(visitId);
        java.time.LocalDate workDate = java.time.LocalDate.now();
        int nextNumber = queueTicketRepo.findMaxQueueNumberForDay(department.getDepartmentId(), workDate).orElse(0) + 1;
        return queueTicketRepo.save(QueueTicket.builder()
                .visit(record.getVisit()).department(department).service(service)
                .workDate(workDate).queueNumber(nextNumber)
                .status(hasActiveWorkflowStep ? QueueStatus.BLOCKED : QueueStatus.WAITING)
                .build());
    }
}
