package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
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
import org.example.doansummer2026.dto.testResult.TestResultRevisionResponse;
import org.example.doansummer2026.dto.testResult.TestResultAmendRequest;
import org.example.doansummer2026.dto.testResult.TestResultAttachmentResponse;
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
import org.example.doansummer2026.enums.TestResultRevisionStatus;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TestRequestService implements TestRequestServiceInterface {

    private static final java.time.ZoneId CLINIC_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    @org.springframework.beans.factory.annotation.Value("${app.upload.root:uploads}")
    private String uploadRoot;

    private final TestRequestRepository repo;
    private final TestResultRepository resultRepo;
    private final org.example.doansummer2026.repository.TestResultRevisionRepository revisionRepo;
    private final org.example.doansummer2026.repository.TestResultAttachmentRepository attachmentRepo;
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
    private final ClinicalFormTemplateService clinicalFormTemplateService;
    private final ClinicalFormEngine clinicalFormEngine;
    private final SameDayParaclinicalResultService sameDayParaclinicalResultService;
    private final StaffDutyService staffDutyService;

    @Transactional(readOnly = true)
    public PageResponse<TestRequestResponse> search(UUID recordId, UUID departmentId,
                                                     TestRequestStatus status, String search,
                                                     java.time.LocalDate workDate,
                                                     Pageable pageable) {
        departmentId = restrictSearchScope(recordId, departmentId);
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase();
        Page<TestRequest> page = repo.search(recordId, departmentId, status,
                normalizedSearch, workDate, pageable);
        return PageResponse.from(page, TestRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TestRequestResponse> listByVisit(UUID visitId) {
        visitRepo.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Lượt khám không tồn tại: " + visitId));
        List<TestRequest> requests = repo.findAllByVisitIdWithDetails(visitId);
        ensureCurrentStaffCanViewAny(requests);
        return requests.stream()
                .map(TestRequestResponse::from)
                .toList();
    }

    /**
     * Gan yeu cau CLS da dat va thanh toan truoc vao ho so dang kham.
     * Khong tao TestRequest/Invoice moi. Tra ve false neu dich vu chua duoc dat truoc.
     */
    public boolean attachPrepaidRequestToExamination(UUID visitId, UUID medicalRecordId,
                                                      UUID serviceId, UUID doctorId, String notes) {
        MedicalRecord targetRecord = recordRepo.findById(medicalRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ khám: " + medicalRecordId));
        if (targetRecord.getVisit() == null || !visitId.equals(targetRecord.getVisit().getVisitId())) {
            throw new BadRequestException("Hồ sơ khám không thuộc lượt khám hiện tại");
        }

        TestRequest existing = repo.findAllByVisitIdWithDetails(visitId).stream()
                .filter(request -> request.getService() != null
                        && serviceId.equals(request.getService().getServiceId()))
                .filter(request -> request.getStatus() != TestRequestStatus.CANCELLED)
                .findFirst()
                .orElse(null);
        if (existing == null) return false;

        MedicalRecord existingRecord = existing.getMedicalRecord();
        if (existingRecord != null && medicalRecordId.equals(existingRecord.getRecordId())) {
            // Dich vu da dat truoc va da duoc gan tu dong vao dung ho so kham.
            // Xem nhu da xu ly de bac si chon lai tren giao dien khong tao trung/thu tien lai.
            return true;
        }
        if (existingRecord != null && existingRecord.getQueueTicket() != null) {
            throw new org.example.doansummer2026.exception.ConflictException(
                    "Dịch vụ cận lâm sàng đã được một phòng khám khác chỉ định");
        }
        StaffInfo doctor = staffRepo.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bác sĩ chỉ định"));
        existing.setMedicalRecord(targetRecord);
        existing.setRequestedBy(doctor);
        if (notes != null && !notes.isBlank()) existing.setDescription(notes.trim());
        repo.save(existing);
        return true;
    }

    @Transactional(readOnly = true)
    public TestRequestResponse get(UUID id) {
        TestRequest request = findById(id);
        ensureCurrentStaffCanView(request);
        return TestRequestResponse.from(request);
    }

    @Transactional(readOnly = true)
    public TestRequestActionPermissionsResponse actionPermissions(UUID id) {
        TestRequest request = findById(id);
        ensureCurrentStaffCanView(request);

        UUID staffId = authService.currentStaffId();
        StaffInfo actor = staffId == null ? null : staffRepo.findById(staffId).orElse(null);
        Department department = request.getPerformingDepartment();
        boolean responsibleDoctor = isResponsibleDoctor(department, actor);
        boolean assignedNurse = isAssignedNurse(department, actor);
        boolean finished = request.getStatus() == TestRequestStatus.COMPLETED
                || request.getStatus() == TestRequestStatus.CANCELLED;
        QueueTicket queue = request.getQueueTicket();
        boolean executionStarted = queue != null && (queue.getStatus() == QueueStatus.IN_PROGRESS
                || queue.getStatus() == QueueStatus.DONE);
        boolean canEdit = !finished && executionStarted && (responsibleDoctor || assignedNurse);
        boolean canCancel = responsibleDoctor && java.util.Set.of(
                TestRequestStatus.BLOCKED, TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS)
                .contains(request.getStatus());

        return new TestRequestActionPermissionsResponse(
                true,
                canEdit,
                canEdit,
                responsibleDoctor && !finished && executionStarted,
                canCancel
        );
    }

    @Transactional(readOnly = true)
    public List<TestRequestResponse> listByQueueTicket(UUID ticketId) {
        if (!queueTicketRepo.existsById(ticketId)) {
            throw new ResourceNotFoundException("Không tìm thấy phiếu cận lâm sàng: " + ticketId);
        }
        List<TestRequest> requests = repo.findAllByQueueTicket_TicketId(ticketId);
        ensureCurrentStaffCanViewAny(requests);
        return requests.stream()
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

    /** Tam khoa cac ky thuat khi benh nhan vang o buoc goi. */
    public void blockRequestsForQueue(UUID ticketId) {
        repo.findAllByQueueTicket_TicketId(ticketId).stream()
                .filter(request -> request.getStatus() == TestRequestStatus.PENDING)
                .forEach(request -> {
                    request.setStatus(TestRequestStatus.BLOCKED);
                    repo.save(request);
                });
    }

    /** Dong bo ky thuat khi dua benh nhan vang quay lai workflow. */
    public void restoreRequestsForQueue(UUID ticketId, boolean queueBlocked) {
        repo.findAllByQueueTicket_TicketId(ticketId).stream()
                .filter(request -> request.getStatus() == TestRequestStatus.BLOCKED)
                .forEach(request -> {
                    request.setStatus(queueBlocked ? TestRequestStatus.BLOCKED : TestRequestStatus.PENDING);
                    repo.save(request);
                });
    }

    @Transactional(readOnly = true)
    public boolean hasIncompleteRequestsForRecord(UUID recordId) {
        return repo.countByMedicalRecordAndStatusIn(recordId,
                java.util.List.of(TestRequestStatus.BLOCKED, TestRequestStatus.PENDING,
                        TestRequestStatus.IN_PROGRESS)) > 0;
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
        requireParaclinicalService(service);
        ensureNoSignedSameDayResult(record, service);
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
        requireParaclinicalService(service);
        if (visitId != null) {
            var targetVisit = visitRepo.findById(visitId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lượt khám không tồn tại: " + visitId));
            if (sameDayParaclinicalResultService.hasReusableResult(targetVisit, serviceId)) {
                throw new ConflictException(
                        "Dịch vụ cận lâm sàng này đã có kết quả được ký trong ngày; hãy sử dụng kết quả tham chiếu"
                );
            }
        }

        /*
         * Idempotency khong dong nghia voi return ngay lap tuc.
         * Mot so luong cu/luong dat dich vu co the da tao TestRequest truoc khi
         * hoa don duoc thanh toan. Ban ghi do chua co QueueTicket; neu return o
         * day thi PAID thanh cong nhung benh nhan khong bao gio vao hang cho.
         */
        TestRequest existingRequest = invoiceItemId == null ? null
                : repo.findTopByInvoiceItem_ItemIdOrderByCreatedAtAsc(invoiceItemId).orElse(null);
        if (existingRequest != null && existingRequest.getStatus() == TestRequestStatus.CANCELLED) {
            existingRequest = null;
        }
        if (existingRequest == null && visitId != null) {
            // Chan trung theo nghiep vu visit + service, khong chi theo invoice
            // item. Request da huy khong chan chi dinh/mua lai dich vu.
            existingRequest = repo
                    .findTopByMedicalRecord_Visit_VisitIdAndService_ServiceIdAndStatusNotOrderByCreatedAtAsc(
                            visitId, serviceId, TestRequestStatus.CANCELLED)
                    .orElse(null);
        }

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
            if (existingRequest.getInvoiceItem() == null && invoiceItemId != null) {
                InvoiceItem paidItem = invoiceItemRepo.findById(invoiceItemId).orElse(null);
                if (paidItem != null) {
                    existingRequest.setInvoiceItem(paidItem);
                    changed = true;
                }
            }
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
                } else if (java.util.List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_PROGRESS)
                        .contains(queueStatus)
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
     * lap hoa don, sau do moi dung nhan su dang truc hoac bat ky nhan su cua phong.
     */
    private StaffInfo resolvePaymentRequester(UUID requestedById, Department department) {
        if (requestedById != null) {
            StaffInfo requestedBy = staffRepo.findById(requestedById).orElse(null);
            if (requestedBy != null) return requestedBy;
        }
        StaffInfo onDuty = staffDutyService.findOnDutyStaff(department, LocalDateTime.now(CLINIC_ZONE))
                .stream().findFirst().orElse(null);
        if (onDuty != null) return onDuty;
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

        StaffInfo responsibleStaff = staffDutyService.findOnDutyStaff(
                        department, LocalDateTime.now(CLINIC_ZONE)).stream()
                .filter(staff -> staff.getSystemRole() != null && staff.getSystemRole().isDoctor())
                .findFirst().orElse(requester);
        if (responsibleStaff == null) {
            throw new BadRequestException("Phòng cận lâm sàng chưa có nhân sự trực để tiếp nhận yêu cầu");
        }
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
    
    private void publishExaminationQueueUpdated(QueueTicket queueTicket) {
        if (queueTicket == null || queueTicket.getDepartment() == null) return;
        try {
            messagingTemplate.convertAndSend(
                    "/topic/department-" + queueTicket.getDepartment().getDepartmentId() + "-queue",
                    "QUEUE_UPDATED");
        } catch (Exception ignored) {
            // Khong de WebSocket lam huy giao dich nghiep vu.
        }
    }

    private void notifyNurses(TestRequest t) {
        String patientName = t.getMedicalRecord() != null && t.getMedicalRecord().getVisit() != null && t.getMedicalRecord().getVisit().getCustomer() != null ? t.getMedicalRecord().getVisit().getCustomer().getFullName() : "Khách";
        String serviceName = t.getService() != null ? t.getService().getName() : "Cận lâm sàng";
        String content = String.format("Có yêu cầu mới (%s) cần thực hiện cho bệnh nhân %s", serviceName, patientName);
        
        List<StaffInfo> labStaff = staffDutyService.findOnDutyStaff(
                t.getPerformingDepartment(), LocalDateTime.now(CLINIC_ZONE));
        for (StaffInfo staff : labStaff) {
            boolean clinicalStaff = staff.getSystemRole() != null
                    && (staff.getSystemRole().isDoctor()
                    || staff.getSystemRole() == org.example.doansummer2026.enums.SystemRole.NURSE);
            if (clinicalStaff && staff.getProfile() != null) {
                try {
                    notificationService.create(new org.example.doansummer2026.dto.notification.NotificationCreateRequest(
                            staff.getProfile().getProfileId(),
                            org.example.doansummer2026.enums.NotificationType.GENERAL,
                            org.example.doansummer2026.enums.NotificationChannel.IN_APP,
                            "Yêu cầu cận lâm sàng mới",
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
        ensureCurrentStaffCanOperate(t);
        if (req.status() != null) {
            throw new BadRequestException(
                    "Trạng thái yêu cầu được cập nhật tự động theo hàng chờ; vui lòng dùng đúng thao tác gọi, bắt đầu, hoàn thành hoặc hủy");
        }
        TestRequest saved = repo.save(t);
        try {
            messagingTemplate.convertAndSend("/topic/department-" + saved.getPerformingDepartment().getDepartmentId() + "-lab-queue", "LAB_UPDATED");
        } catch (Exception e) {}
        
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
        cancel(id, new TestRequestCancelRequest("Hủy yêu cầu thay cho thao tác xóa"));
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
        ensureCurrentStaffCanView(t);
        TestResult r = resultRepo.findByTestRequest_TestRequestId(t.getTestRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có kết quả cho yêu cầu này"));
        return TestResultResponse.from(r);
    }

    @Transactional(readOnly = true)
    public org.example.doansummer2026.dto.clinicalForm.ResolvedClinicalFormResponse getClinicalForm(UUID testRequestId) {
        TestRequest request = findById(testRequestId);
        ensureCurrentStaffCanView(request);
        if (request.getService() == null) throw new ResourceNotFoundException("Yêu cầu chưa gắn dịch vụ");
        TestResult result = resultRepo.findByTestRequest_TestRequestId(testRequestId).orElse(null);
        var version = result != null && result.getFormTemplateVersion() != null
                ? result.getFormTemplateVersion()
                : clinicalFormTemplateService.resolveVersion(request.getService().getServiceId(), null);
        return clinicalFormTemplateService.resolvedResponse(version, result == null ? null : result.getResultData());
    }

    public TestResultResponse createResult(UUID testRequestId, TestResultCreateRequest req) {
        TestRequest t = findById(testRequestId);
        ensureCurrentStaffCanOperate(t);
        ensureExecutionStarted(t);
        // Kiem tra neu da COMPLETED thi khong cho tao moi
        if (t.getStatus() == TestRequestStatus.COMPLETED) {
            throw new ConflictException("Yêu cầu cận lâm sàng đã hoàn thành, không thể tạo kết quả mới");
        }
        if (resultRepo.findByTestRequest_TestRequestId(testRequestId).isPresent()) {
            throw new ConflictException("Yêu cầu đã có kết quả; vui lòng dùng chức năng cập nhật");
        }
        StaffInfo performedBy = resolveCurrentPerformer(req.performedById());
        TestResult r = TestResult.builder()
                .testRequest(t)
                .imageUrl(req.imageUrl())
                .conclusion(req.conclusion())
                .sampleId(req.sampleId())
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();
        applyStructuredResult(t, r, req.formTemplateVersionId(), req.resultData(), false);
        applySpecimenInformation(t, r, req.sampleId(), req.sampleType(), req.sampleStatus());
        resultRepo.save(r);
        saveDraftRevision(r, performedBy, null);

        if (t.getStatus() == TestRequestStatus.PENDING) {
            t.setStatus(TestRequestStatus.IN_PROGRESS);
            repo.save(t);
        }

        return TestResultResponse.from(r);
    }

    public TestResultResponse updateResult(UUID testRequestId, TestResultUpdateRequest req) {
        TestRequest t = findById(testRequestId);
        ensureCurrentStaffCanOperate(t);
        ensureExecutionStarted(t);

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
        applyStructuredResult(t, r, req.formTemplateVersionId(), req.resultData(), false);
        applySpecimenInformation(t, r, req.sampleId(), req.sampleType(), req.sampleStatus());

        if (t.getStatus() == TestRequestStatus.PENDING) {
            t.setStatus(TestRequestStatus.IN_PROGRESS);
            repo.save(t);
        }

        TestResult saved = resultRepo.save(r);
        saveDraftRevision(saved, saved.getPerformedBy(), null);
        return TestResultResponse.from(saved);
    }

    /**
     * Hoan thanh ket qua xet nghiem - TAO MOI hoac CAP NHAT ROI CHUYEN STATUS SANG COMPLETED.
     * Phu hop cho truong hop luu nhap + hoan thanh sau.
     */
    public TestResultResponse completeResult(UUID testRequestId, TestResultCreateRequest req) {
        // Dung findByIdWithResult de eager fetch testResult - tranh lazy loading
        TestRequest t = repo.findByIdForUpdate(testRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu cận lâm sàng không tồn tại: " + testRequestId));
        StaffInfo verifier = requireResponsibleDoctorLocked(t);

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
            applyStructuredResult(t, r, req.formTemplateVersionId(), req.resultData(), true);
            applySpecimenInformation(t, r, req.sampleId(), req.sampleType(), req.sampleStatus());
        } else {
            // Tao moi
            StaffInfo performedBy = resolveCurrentPerformer(req.performedById());
            r = TestResult.builder()
                    .testRequest(t)
                    .imageUrl(req.imageUrl())
                    .conclusion(req.conclusion())
                    .sampleId(req.sampleId())
                    .performedBy(performedBy)
                    .performedAt(LocalDateTime.now())
                    .build();
            applyStructuredResult(t, r, req.formTemplateVersionId(), req.resultData(), true);
            applySpecimenInformation(t, r, req.sampleId(), req.sampleType(), req.sampleStatus());
        }

        if (req.sampleStatus() == org.example.doansummer2026.enums.SpecimenStatus.REJECTED || req.sampleStatus() == org.example.doansummer2026.enums.SpecimenStatus.RECOLLECT) {
            throw new BadRequestException("Không thể hoàn thành kết quả khi mẫu vật bị hỏng hoặc cần lấy lại");
        }

        if (r.getConclusion() == null || r.getConclusion().isBlank()) {
            throw new BadRequestException("Vui lòng nhập kết luận của bác sĩ");
        }
        boolean hasRevisionAttachment = r.getResultId() != null && revisionRepo
                .findFirstByTestResult_ResultIdOrderByRevisionNoDesc(r.getResultId())
                .map(revision -> attachmentRepo.countByRevision_RevisionId(revision.getRevisionId()) > 0).orElse(false);
        if (r.getResultData() == null && (r.getImageUrl() == null || r.getImageUrl().isBlank()) && !hasRevisionAttachment)
            throw new BadRequestException("Vui lòng nhập kết quả có cấu trúc hoặc tải tệp kết quả");

        r.setVerifiedBy(verifier);
        r.setVerifiedAt(LocalDateTime.now());

        resultRepo.save(r);
        signLatestRevision(r, verifier, null);

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
            // Chi dua benh nhan ve dung phong kham da chi dinh yeu cau nay.
            // Khong tim "phong dang cho" dau tien cua visit vi mot lich hen co
            // the co nhieu benh an kham doc lap.
            QueueTicket queueTicket = t.getMedicalRecord().getQueueTicket();
            if (queueTicket != null && queueTicket.getStatus() != QueueStatus.WAITING_FOR_TEST
                    && queueTicket.getStatus() != QueueStatus.TEST_DONE) {
                queueTicket = null;
            }
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
                publishExaminationQueueUpdated(queueTicket);
            }
            if (queueTicket == null || queueTicket.getStatus() != QueueStatus.TEST_DONE)
                patientJourneyService.activateNext(t.getMedicalRecord().getVisit().getVisitId());
        }

        publishLabQueueUpdated(t.getPerformingDepartment().getDepartmentId());
        return TestResultResponse.from(r);
    }

    private void applyStructuredResult(TestRequest request, TestResult result,
                                       UUID requestedVersionId, JsonNode input,
                                       boolean requireComplete) {
        if (input == null && requestedVersionId == null && !requireComplete) return;
        if (request.getService() == null) throw new BadRequestException("Yêu cầu chưa gắn dịch vụ để xác định biểu mẫu");
        JsonNode effectiveInput = input != null ? input : result.getResultData();
        UUID effectiveVersionId = requestedVersionId != null ? requestedVersionId
                : result.getFormTemplateVersion() == null ? null : result.getFormTemplateVersion().getVersionId();
        var version = clinicalFormTemplateService.resolveVersion(
                request.getService().getServiceId(), effectiveVersionId);
        var patient = request.getMedicalRecord() == null || request.getMedicalRecord().getVisit() == null
                ? null : request.getMedicalRecord().getVisit().getCustomer();
        JsonNode normalized = clinicalFormEngine.validateAndEnrich(version.getSchemaJson(), effectiveInput,
                patient == null ? null : patient.getDateOfBirth(),
                patient == null ? null : patient.getGender(), LocalDate.now(CLINIC_ZONE), requireComplete);
        result.setFormTemplateVersion(version);
        result.setResultData(normalized);
    }

    private org.example.doansummer2026.model.TestResultRevision saveDraftRevision(
            TestResult result, StaffInfo enteredBy, String amendmentReason) {
        var current = revisionRepo.findFirstByTestResult_ResultIdOrderByRevisionNoDesc(result.getResultId()).orElse(null);
        if (current != null && current.getStatus() == TestResultRevisionStatus.DRAFT) {
            current.setResultData(result.getResultData());
            current.setConclusion(result.getConclusion());
            current.setTemplateVersion(result.getFormTemplateVersion());
            if (amendmentReason != null) current.setAmendmentReason(amendmentReason);
            return revisionRepo.save(current);
        }
        int nextNo = current == null ? 1 : current.getRevisionNo() + 1;
        return revisionRepo.save(org.example.doansummer2026.model.TestResultRevision.builder()
                .testResult(result).revisionNo(nextNo).status(TestResultRevisionStatus.DRAFT)
                .resultData(result.getResultData()).conclusion(result.getConclusion())
                .templateVersion(result.getFormTemplateVersion()).amendmentReason(amendmentReason)
                .enteredBy(enteredBy).build());
    }

    private void signLatestRevision(TestResult result, StaffInfo signer, String amendmentReason) {
        var draft = saveDraftRevision(result, result.getPerformedBy(), amendmentReason);
        revisionRepo.findFirstByTestResult_ResultIdAndStatusOrderByRevisionNoDesc(
                result.getResultId(), TestResultRevisionStatus.SIGNED).ifPresent(previous -> {
            previous.setStatus(TestResultRevisionStatus.SUPERSEDED);
            revisionRepo.save(previous);
        });
        draft.setResultData(result.getResultData());
        draft.setConclusion(result.getConclusion());
        draft.setTemplateVersion(result.getFormTemplateVersion());
        draft.setStatus(TestResultRevisionStatus.SIGNED);
        draft.setSignedBy(signer);
        draft.setSignedAt(LocalDateTime.now());
        revisionRepo.save(draft);
    }

    @Transactional(readOnly = true)
    public List<TestResultRevisionResponse> resultHistory(UUID testRequestId) {
        TestRequest request = findById(testRequestId);
        ensureCurrentStaffCanView(request);
        TestResult result = resultRepo.findByTestRequest_TestRequestId(testRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có kết quả cho yêu cầu này"));
        return revisionRepo.findByTestResult_ResultIdOrderByRevisionNoDesc(result.getResultId())
                .stream().map(TestResultRevisionResponse::from).toList();
    }

    public TestResultRevisionResponse amendResult(UUID testRequestId, TestResultAmendRequest req) {
        TestRequest request = repo.findByIdForUpdate(testRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu cận lâm sàng không tồn tại: " + testRequestId));
        requireResponsibleDoctorLocked(request);
        if (request.getStatus() != TestRequestStatus.COMPLETED)
            throw new ConflictException("Chỉ kết quả đã ký mới cần lập bản đính chính");
        TestResult result = resultRepo.findByTestRequest_TestRequestId(testRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có kết quả để đính chính"));
        var latest = revisionRepo.findFirstByTestResult_ResultIdOrderByRevisionNoDesc(result.getResultId()).orElse(null);
        if (latest != null && latest.getStatus() == TestResultRevisionStatus.DRAFT)
            throw new ConflictException("Đã tồn tại một bản đính chính chưa ký");
        StaffInfo actor = authService.currentStaffId() == null ? result.getPerformedBy()
                : staffRepo.findById(authService.currentStaffId()).orElse(result.getPerformedBy());
        return TestResultRevisionResponse.from(saveDraftRevision(result, actor, req.reason().trim()));
    }

    public TestResultRevisionResponse updateAmendment(UUID testRequestId, UUID revisionId,
                                                       TestResultUpdateRequest req) {
        TestRequest request = repo.findByIdForUpdate(testRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu cận lâm sàng không tồn tại: " + testRequestId));
        requireResponsibleDoctorLocked(request);
        var revision = findDraftRevision(request, revisionId);
        if (req.conclusion() != null) revision.setConclusion(req.conclusion());
        if (req.resultData() != null || req.formTemplateVersionId() != null) {
            var version = clinicalFormTemplateService.resolveVersion(
                    request.getService().getServiceId(), req.formTemplateVersionId());
            var patient = request.getMedicalRecord() == null || request.getMedicalRecord().getVisit() == null
                    ? null : request.getMedicalRecord().getVisit().getCustomer();
            revision.setResultData(clinicalFormEngine.validateAndEnrich(version.getSchemaJson(), req.resultData(),
                    patient == null ? null : patient.getDateOfBirth(), patient == null ? null : patient.getGender(),
                    LocalDate.now(CLINIC_ZONE), false));
            revision.setTemplateVersion(version);
        }
        return TestResultRevisionResponse.from(revisionRepo.save(revision));
    }

    public TestResultRevisionResponse signAmendment(UUID testRequestId, UUID revisionId) {
        TestRequest request = repo.findByIdForUpdate(testRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu cận lâm sàng không tồn tại: " + testRequestId));
        StaffInfo signer = requireResponsibleDoctorLocked(request);
        var revision = findDraftRevision(request, revisionId);
        if (revision.getConclusion() == null || revision.getConclusion().isBlank())
            throw new BadRequestException("Vui lòng nhập kết luận đính chính");
        TestResult result = revision.getTestResult();
        var patient = request.getMedicalRecord() == null || request.getMedicalRecord().getVisit() == null
                ? null : request.getMedicalRecord().getVisit().getCustomer();
        revision.setResultData(clinicalFormEngine.validateAndEnrich(
                revision.getTemplateVersion().getSchemaJson(), revision.getResultData(),
                patient == null ? null : patient.getDateOfBirth(),
                patient == null ? null : patient.getGender(), LocalDate.now(CLINIC_ZONE), true));
        revisionRepo.findFirstByTestResult_ResultIdAndStatusOrderByRevisionNoDesc(
                result.getResultId(), TestResultRevisionStatus.SIGNED).ifPresent(previous -> {
            previous.setStatus(TestResultRevisionStatus.SUPERSEDED);
            revisionRepo.save(previous);
        });
        revision.setStatus(TestResultRevisionStatus.SIGNED);
        revision.setSignedBy(signer);
        revision.setSignedAt(LocalDateTime.now());
        revisionRepo.save(revision);
        result.setResultData(revision.getResultData());
        result.setConclusion(revision.getConclusion());
        result.setFormTemplateVersion(revision.getTemplateVersion());
        result.setVerifiedBy(signer);
        result.setVerifiedAt(LocalDateTime.now());
        resultRepo.save(result);
        return TestResultRevisionResponse.from(revision);
    }

    private org.example.doansummer2026.model.TestResultRevision findDraftRevision(
            TestRequest request, UUID revisionId) {
        var revision = revisionRepo.findById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản đính chính"));
        if (revision.getTestResult() == null || revision.getTestResult().getTestRequest() == null
                || !request.getTestRequestId().equals(revision.getTestResult().getTestRequest().getTestRequestId()))
            throw new BadRequestException("Bản đính chính không thuộc yêu cầu này");
        if (revision.getStatus() != TestResultRevisionStatus.DRAFT)
            throw new ConflictException("Bản đính chính đã ký không thể sửa trực tiếp");
        return revision;
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
        TestRequest request = findById(testRequestId);
        ensureCurrentStaffCanOperate(request);
        ensureExecutionStarted(request);

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

    public List<TestResultAttachmentResponse> uploadAttachments(UUID testRequestId, UUID revisionId,
                                                                 List<MultipartFile> files) throws IOException {
        TestRequest request = findById(testRequestId);
        ensureCurrentStaffCanOperate(request);
        ensureExecutionStarted(request);
        TestResult result = resultRepo.findByTestRequest_TestRequestId(testRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Hãy lưu nháp kết quả trước khi tải tệp"));
        var revision = revisionRepo.findById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản kết quả"));
        if (!revision.getTestResult().getResultId().equals(result.getResultId()))
            throw new BadRequestException("Phiên bản kết quả không thuộc yêu cầu này");
        if (revision.getStatus() != TestResultRevisionStatus.DRAFT)
            throw new ConflictException("Không thể thêm tệp vào kết quả đã ký");
        if (files == null || files.isEmpty()) throw new BadRequestException("Vui lòng chọn ít nhất một tệp");
        long existing = attachmentRepo.countByRevision_RevisionId(revisionId);
        if (existing + files.size() > 10) throw new BadRequestException("Mỗi kết quả chỉ được tối đa 10 tệp");

        Path uploadDir = Paths.get(uploadRoot, "test-results", "attachments").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        List<TestResultAttachmentResponse> saved = new java.util.ArrayList<>();
        int order = (int) existing;
        for (MultipartFile file : files) {
            ValidatedUpload valid = validateAttachment(file);
            String extension = switch (valid.contentType()) {
                case "application/pdf" -> ".pdf";
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> throw new BadRequestException("Định dạng tệp không hợp lệ");
            };
            String storedName = UUID.randomUUID() + extension;
            Path target = uploadDir.resolve(storedName).normalize();
            if (!target.startsWith(uploadDir)) throw new BadRequestException("Tên tệp không an toàn");
            Files.copy(file.getInputStream(), target);
            var attachment = attachmentRepo.save(org.example.doansummer2026.model.TestResultAttachment.builder()
                    .revision(revision).storagePath(target.toString()).originalName(valid.originalName())
                    .contentType(valid.contentType()).fileSize(file.getSize()).displayOrder(order++).build());
            saved.add(TestResultAttachmentResponse.from(attachment));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TestResultAttachmentResponse> listAttachments(UUID testRequestId, UUID revisionId) {
        TestRequest request = findById(testRequestId);
        ensureCurrentStaffCanView(request);
        var revision = revisionRepo.findById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản kết quả"));
        if (revision.getTestResult() == null || revision.getTestResult().getTestRequest() == null
                || !testRequestId.equals(revision.getTestResult().getTestRequest().getTestRequestId()))
            throw new BadRequestException("Phiên bản kết quả không thuộc yêu cầu này");
        return attachmentRepo.findByRevision_RevisionIdOrderByDisplayOrder(revisionId)
                .stream().map(TestResultAttachmentResponse::from).toList();
    }

    private ValidatedUpload validateAttachment(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new BadRequestException("Tệp không được để trống");
        if (file.getSize() > 10L * 1024 * 1024) throw new BadRequestException("Mỗi tệp không được vượt quá 10 MB");
        String original = Paths.get(file.getOriginalFilename() == null ? "result" : file.getOriginalFilename())
                .getFileName().toString().replaceAll("[\\r\\n]", "_");
        byte[] head = new byte[12];
        int read;
        try (var input = file.getInputStream()) { read = input.read(head); }
        String detected;
        if (read >= 5 && new String(head, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) detected = "application/pdf";
        else if (read >= 3 && (head[0] & 0xff) == 0xff && (head[1] & 0xff) == 0xd8 && (head[2] & 0xff) == 0xff) detected = "image/jpeg";
        else if (read >= 8 && java.util.Arrays.equals(java.util.Arrays.copyOf(head, 8), new byte[]{(byte)137,80,78,71,13,10,26,10})) detected = "image/png";
        else if (read >= 12 && new String(head, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                && new String(head, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) detected = "image/webp";
        else throw new BadRequestException("Chỉ chấp nhận PDF, JPEG, PNG hoặc WebP hợp lệ");
        if (file.getContentType() != null && !file.getContentType().equalsIgnoreCase(detected)
                && !(detected.equals("image/jpeg") && file.getContentType().equalsIgnoreCase("image/jpg")))
            throw new BadRequestException("Loại MIME không khớp với nội dung tệp");
        return new ValidatedUpload(original, detected);
    }

    private record ValidatedUpload(String originalName, String contentType) {}

    /** Huy yeu cau chi dinh khi phong thuc hien chua bat dau xu ly. */
    public TestRequestResponse cancel(UUID id, TestRequestCancelRequest req) {
        TestRequest t = repo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu cận lâm sàng không tồn tại: " + id));
        requireResponsibleDoctorLocked(t);

        if (t.getStatus() == TestRequestStatus.CANCELLED) {
            throw new org.example.doansummer2026.exception.ConflictException("Yêu cầu đã bị hủy");
        }
        if (t.getStatus() != TestRequestStatus.PENDING
                && t.getStatus() != TestRequestStatus.BLOCKED
                && t.getStatus() != TestRequestStatus.IN_PROGRESS) {
            throw new org.example.doansummer2026.exception.ConflictException(
                    "Chỉ có thể hủy yêu cầu trước khi kết quả được ký");
        }

        t.setStatus(TestRequestStatus.CANCELLED);
        t.setCancelReason(req.reason());
        repo.save(t);

        QueueTicket paraclinicalQueue = t.getQueueTicket();
        boolean closedCancelledQueue = false;
        if (paraclinicalQueue != null) {
            long remainingInQueue = repo.countByQueueTicket_TicketIdAndStatusIn(
                    paraclinicalQueue.getTicketId(),
                    java.util.List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS,
                            TestRequestStatus.BLOCKED));
            if (remainingInQueue == 0) {
                // SKIPPED danh cho benh nhan vang va se tam khoa ca hanh trinh.
                // Tat ca ky thuat bi huy nghia la buoc nay da ket thuc, phai mo
                // phong ke tiep thay vi bat nguoi dung "quay lai hang cho".
                paraclinicalQueue.setStatus(QueueStatus.DONE);
                paraclinicalQueue.setCompletedAt(LocalDateTime.now());
                queueTicketRepo.save(paraclinicalQueue);
                closedCancelledQueue = true;
            }
        }

        MedicalRecord sourceRecord = t.getMedicalRecord();
        if (sourceRecord != null && sourceRecord.getVisit() != null) {
            long remainingInRecord = repo.countByMedicalRecordAndStatusIn(
                    sourceRecord.getRecordId(),
                    java.util.List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS,
                            TestRequestStatus.BLOCKED));
            QueueTicket sourceQueue = sourceRecord.getQueueTicket();
            if (remainingInRecord == 0 && sourceQueue != null
                    && sourceQueue.getStatus() == QueueStatus.WAITING_FOR_TEST) {
                // Khong con yeu cau CLS can cho: dua benh nhan ve lai phong
                // nguon de bac si tiep tuc ket luan hoac chi dinh lai.
                sourceQueue.setStatus(QueueStatus.TEST_DONE);
                sourceQueue.setCalledAt(null);
                queueTicketRepo.save(sourceQueue);
            } else if (remainingInRecord == 0 && sourceQueue == null) {
                completeStandaloneRecordIfReady(sourceRecord,
                        repo.countByMedicalRecord_MedicalRecordId(sourceRecord.getRecordId()), 0);
            }
            if (closedCancelledQueue || (remainingInRecord == 0 && sourceQueue == null)) {
                patientJourneyService.activateNext(sourceRecord.getVisit().getVisitId());
            }
        }

        if (t.getPerformingDepartment() != null) {
            publishLabQueueUpdated(t.getPerformingDepartment().getDepartmentId());
        }

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
                    requireParaclinicalService(service);
                    ensureNoSignedSameDayResult(record, service);
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

    private void requireParaclinicalService(MedicalService service) {
        if (service == null || service.getDepartmentType() == null
                || !service.getDepartmentType().isParaclinical()) {
            throw new BadRequestException(
                    "Chỉ được tạo yêu cầu cho dịch vụ cận lâm sàng; dịch vụ khám phải được lễ tân tạo lượt riêng"
            );
        }
    }

    private void ensureNoSignedSameDayResult(MedicalRecord record, MedicalService service) {
        if (record != null && record.getVisit() != null
                && sameDayParaclinicalResultService.hasReusableResult(
                        record.getVisit(), service.getServiceId())) {
            throw new ConflictException(
                    "Dịch vụ cận lâm sàng này đã có kết quả được ký trong ngày; hãy sử dụng kết quả tham chiếu"
            );
        }
    }

    /**
     * Danh sach tong hop chi duoc xem theo ho so do bac si phu trach, hoac theo
     * phong ma nhan vien dang duoc phan cong. Khong tin departmentId do frontend
     * gui len vi co the doi UUID de doc yeu cau cua phong khac.
     */
    private UUID restrictSearchScope(UUID recordId, UUID requestedDepartmentId) {
        if (isCurrentAdmin()) return requestedDepartmentId;

        UUID staffId = authService.currentStaffId();
        StaffInfo staff = staffId == null ? null : staffRepo.findById(staffId).orElse(null);
        if (staff == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Không xác định được nhân viên đang đăng nhập");
        }

        if (recordId != null) {
            MedicalRecord record = recordRepo.findById(recordId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ khám"));
            if (record.getDoctor() != null && staffId.equals(record.getDoctor().getStaffId())) {
                return requestedDepartmentId;
            }
        }

        Department assignedDepartment = staff.getDepartment();
        if (assignedDepartment == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Nhân viên chưa được phân công phòng");
        }
        UUID assignedDepartmentId = assignedDepartment.getDepartmentId();
        if (requestedDepartmentId != null && !assignedDepartmentId.equals(requestedDepartmentId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Không được xem yêu cầu cận lâm sàng của phòng khác");
        }
        return assignedDepartmentId;
    }

    private void ensureCurrentStaffCanViewAny(List<TestRequest> requests) {
        if (isCurrentAdmin() || requests == null || requests.isEmpty()) return;
        boolean allowed = requests.stream().anyMatch(this::canCurrentStaffView);
        if (!allowed) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Không được xem yêu cầu cận lâm sàng này");
        }
    }

    private void ensureCurrentStaffCanView(TestRequest request) {
        if (isCurrentAdmin() || canCurrentStaffView(request)) return;
        throw new org.springframework.security.access.AccessDeniedException(
                "Không được xem yêu cầu cận lâm sàng này");
    }

    private boolean canCurrentStaffView(TestRequest request) {
        UUID staffId = authService.currentStaffId();
        if (staffId == null || request == null) return false;

        boolean requester = request.getRequestedBy() != null
                && staffId.equals(request.getRequestedBy().getStaffId());
        boolean recordDoctor = request.getMedicalRecord() != null
                && request.getMedicalRecord().getDoctor() != null
                && staffId.equals(request.getMedicalRecord().getDoctor().getStaffId());
        Department department = request.getPerformingDepartment();
        StaffInfo actor = staffRepo.findById(staffId).orElse(null);
        boolean departmentMember = department != null && actor != null
                && actor.getDepartment() != null
                && department.getDepartmentId().equals(actor.getDepartment().getDepartmentId());
        return requester || recordDoctor || departmentMember;
    }

    private void ensureCurrentStaffCanOperate(TestRequest request) {
        UUID staffId = authService.currentStaffId();
        Department department = request.getPerformingDepartment();
        if (staffId == null || department == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Không xác định được nhân viên hoặc phòng thực hiện");
        }
        staffDutyService.requireCurrentStaffOnDuty(department, false);
    }

    private void ensureExecutionStarted(TestRequest request) {
        QueueTicket queue = request.getQueueTicket();
        if (queue == null || (queue.getStatus() != QueueStatus.IN_PROGRESS
                && queue.getStatus() != QueueStatus.DONE)) {
            throw new BadRequestException(
                    "Chỉ được nhập hoặc tải kết quả sau khi bệnh nhân đã bắt đầu thực hiện tại phòng");
        }
    }

    private StaffInfo resolveCurrentPerformer(UUID requestedPerformerId) {
        UUID currentStaffId = authService.currentStaffId();
        if (currentStaffId == null || requestedPerformerId == null
                || !currentStaffId.equals(requestedPerformerId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Người thực hiện phải là tài khoản nhân viên đang đăng nhập");
        }
        return staffRepo.findById(currentStaffId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên thực hiện"));
    }

    private StaffInfo requireResponsibleDoctorLocked(TestRequest request) {
        UUID staffId = authService.currentStaffId();
        Department currentDepartment = request.getPerformingDepartment();
        if (staffId == null || currentDepartment == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Chỉ bác sĩ trực tại phòng thực hiện mới được phép thao tác");
        }
        Department lockedDepartment = departmentRepo.findByIdForUpdate(currentDepartment.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng thực hiện không tồn tại"));
        return staffDutyService.requireCurrentStaffOnDuty(lockedDepartment, true);
    }

    private boolean isResponsibleDoctor(Department department, StaffInfo actor) {
        return department != null && actor != null && actor.getSystemRole() != null
                && actor.getSystemRole().isDoctor()
                && actor.getDepartment() != null
                && department.getDepartmentId().equals(actor.getDepartment().getDepartmentId());
    }

    private boolean isAssignedNurse(Department department, StaffInfo actor) {
        return department != null && actor != null
                && actor.getSystemRole() == org.example.doansummer2026.enums.SystemRole.NURSE
                && actor.getDepartment() != null
                && department.getDepartmentId().equals(actor.getDepartment().getDepartmentId());
    }

    private boolean isCurrentAdmin() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    /** Tim TestRequest da hoan thanh theo profileId (cho hien thi trong profile). */
    @Transactional(readOnly = true)
    public List<TestRequest> findMyCompletedTests(UUID profileId) {
        return repo.findByProfileIdAndStatusCompleted(profileId);
    }

    /** Tim TestRequest theo InvoiceItem (traceability: Invoice -> InvoiceItem -> TestRequest). */
    @Transactional(readOnly = true)
    public List<TestRequestResponse> findByInvoiceItem(UUID itemId) {
        List<TestRequest> requests = repo.findByInvoiceItem_ItemId(itemId);
        ensureCurrentStaffCanViewAny(requests);
        return requests.stream()
                .map(TestRequestResponse::from)
                .toList();
    }

    /** Tim TestRequest theo Invoice (traceability: Invoice -> InvoiceItem -> TestRequest). */
    @Transactional(readOnly = true)
    public List<TestRequestResponse> findByInvoice(UUID invoiceId) {
        List<TestRequest> requests = repo.findByInvoiceId(invoiceId);
        ensureCurrentStaffCanViewAny(requests);
        return requests.stream()
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
        // Khoa chung Visit truoc khi khoa phong. Hai phong khac nhau cua cung
        // luot khong the cung duoc mo WAITING khi callback den dong thoi.
        visitRepo.findByIdForUpdate(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Lượt khám không tồn tại: " + visitId));
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
        boolean hasActiveWorkflowStep = patientJourneyService.hasActiveStep(visitId)
                || queueTicketRepo.findAllByVisit_VisitId(visitId).stream()
                .anyMatch(queue -> queue.getStatus() == QueueStatus.SKIPPED);
        java.time.LocalDate workDate = java.time.LocalDate.now(CLINIC_ZONE);
        int nextNumber = queueTicketRepo.findMaxQueueNumberForDay(department.getDepartmentId(), workDate).orElse(0) + 1;
        return queueTicketRepo.save(QueueTicket.builder()
                .visit(record.getVisit()).department(department).service(service)
                .workDate(workDate).queueNumber(nextNumber)
                .status(hasActiveWorkflowStep ? QueueStatus.BLOCKED : QueueStatus.WAITING)
                .build());
    }
}
