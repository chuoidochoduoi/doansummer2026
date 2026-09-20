package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.dto.queueticket.QueueTicketCreateRequest;
import org.example.doansummer2026.dto.queueticket.ExaminationTransitionResponse;
import org.example.doansummer2026.dto.queueticket.QueueTicketResponse;
import org.example.doansummer2026.dto.queueticket.SameRoomExaminationChainResponse;
import org.example.doansummer2026.dto.queueticket.QueueTicketUpdateRequest;
import org.example.doansummer2026.dto.medicalrecord.MedicalRecordResponse;
import org.example.doansummer2026.dto.medicalrecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.dto.medicalrecord.TestRequestInExaminationRequest;
import org.example.doansummer2026.dto.icd.ICD10SelectionCreateRequest;
import org.example.doansummer2026.dto.testrequest.TestRequestCreateRequest;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.Icd10Selection;
import org.example.doansummer2026.model.Icd10Code;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.InvoiceItemRepository;
import org.example.doansummer2026.repository.Icd10CodeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.QueueTicketServiceInterface;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.example.doansummer2026.service.interfaces.InvoiceServiceInterface;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class QueueTicketService implements QueueTicketServiceInterface {

    private static final java.time.ZoneId CLINIC_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    private final QueueTicketRepository repo;
    private final CustomerVisitRepository visitRepo;
    private final DepartmentRepository departmentRepo;
    private final MedicalServiceRepository serviceRepo;
    private final MedicalRecordRepository recordRepo;
    private final StaffInfoRepository staffRepo;
    private final ProfileRepository profileRepo;
    private final InvoiceItemRepository invoiceItemRepo;
    private final Icd10CodeRepository icd10Repo;
    private final TestRequestService testRequestService;
    private final PatientJourneyService patientJourneyService;
    private final MedicalRecordService medicalRecordService;
    private final org.example.doansummer2026.repository.InvoiceRepository invoiceRepo;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final StaffDutyService staffDutyService;
    private final org.example.doansummer2026.repository.TestRequestRepository testRequestRepository;
    private final MedicalServiceSelectionPolicyService serviceSelectionPolicyService;
    private final QueuePriorityService queuePriorityService;

    @Autowired
    @Lazy
    private InvoiceServiceInterface invoiceService;

    @Transactional(readOnly = true)
    public PageResponse<QueueTicketResponse> search(UUID departmentId, LocalDate workDate,
                                                     QueueStatus status, Pageable pageable) {
        Page<QueueTicket> page = repo.search(departmentId, workDate, status, pageable);
        Map<UUID, QueuePriorityService.RankedTicket> ranking = departmentId == null || workDate == null
                ? Map.of() : rankingByTicket(departmentId, workDate);
        return PageResponse.from(page, q -> applyRanking(
                toResponse(q, getRecordId(q), null, null), ranking.get(q.getTicketId())));
    }

    @Transactional(readOnly = true)
    public QueueTicketResponse get(UUID id) {
        return toResponse(findById(id));
    }

    public QueueTicketResponse create(QueueTicketCreateRequest req) {
        // Khoa Visit de moi invoice/callback cua cung luot dung chung mot workflow.
        CustomerVisit visit = visitRepo.findByIdForUpdate(req.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Lượt khám không tồn tại: " + req.visitId()));
        var existing = repo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(
                req.visitId(), req.serviceId());
        if (existing.isPresent()) return toResponse(existing.get());
        Department dept = departmentRepo.findByIdForUpdate(req.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại: " + req.departmentId()));
        // Kiem tra lai sau khi khoa phong. Neu hai thanh toan den cung luc,
        // request thu hai phai tai su dung phieu request thu nhat vua tao.
        existing = repo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(
                req.visitId(), req.serviceId());
        if (existing.isPresent()) return toResponse(existing.get());
        org.example.doansummer2026.model.MedicalService service = serviceRepo.findById(req.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + req.serviceId()));
        if (service.getDepartmentType() != null
                && service.getDepartmentType().normalized()
                == org.example.doansummer2026.enums.DepartmentType.EXAMINATION) {
            if (visit.getCustomer() != null) {
                profileRepo.findByIdForUpdate(visit.getCustomer().getProfileId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ bệnh nhân"));
                repo.findSameDayPatientExaminationTickets(
                                visit.getCustomer().getProfileId(),
                                req.workDate() != null ? req.workDate() : LocalDate.now(CLINIC_ZONE))
                        .stream()
                        .filter(ticket -> ticket.getService() != null
                                && invoiceItemRepo.findDistinctExaminationServiceIdsByVisit(
                                        ticket.getVisit().getVisitId(), null)
                                        .contains(ticket.getService().getServiceId())
                                && service.getServiceId().equals(ticket.getService().getServiceId())
                                && ticket.getVisit() != null
                                && !visit.getVisitId().equals(ticket.getVisit().getVisitId()))
                        .findFirst()
                        .ifPresent(ticket -> {
                            String visitCode = "VIS-" + ticket.getVisit().getVisitId().toString()
                                    .replace("-", "").substring(0, 8).toUpperCase();
                            throw new ConflictException("Dịch vụ " + service.getName()
                                    + " đã được đăng ký hôm nay trong lượt " + visitCode);
                        });
            }
        }
        LocalDate workDate = req.workDate() != null ? req.workDate() : LocalDate.now(CLINIC_ZONE);
        Integer max = repo.findMaxQueueNumberForDay(req.departmentId(), workDate).orElse(0);
        // Quyet dinh trang thai truoc khi luu/phat thong bao. Khong tao WAITING
        // roi moi doi sang BLOCKED, vi bac si co the nhan thong bao sai cho mot
        // buoc chua den luot trong lich hen nhieu dich vu.
        boolean workflowAlreadyActive = patientJourneyService.hasActiveStep(visit.getVisitId())
                || repo.findAllByVisit_VisitId(visit.getVisitId()).stream()
                .anyMatch(ticket -> ticket.getStatus() == QueueStatus.SKIPPED);
        QueueTicket q = QueueTicket.builder()
                .visit(visit)
                .department(dept)
                .service(service)
                .workDate(workDate)
                .queueNumber(max + 1)
                .status(workflowAlreadyActive ? QueueStatus.BLOCKED : QueueStatus.WAITING)
                .build();
        QueueTicket saved = repo.save(q);
        updateDepartmentStatus(dept.getDepartmentId());
        if (saved.getStatus() == QueueStatus.WAITING
                && dept.getDepartmentType() == org.example.doansummer2026.enums.DepartmentType.EXAMINATION) {
            notifyDoctors(saved);
        }
        return toResponse(saved);
    }
    
    private void notifyDoctors(QueueTicket q) {
        if (q.getDepartment() == null) return;
        String patientName = q.getVisit() != null && q.getVisit().getAppointment() != null ? q.getVisit().getAppointment().getGuestFullName() : "Khách";
        if (q.getVisit() != null && q.getVisit().getCustomer() != null) {
            patientName = q.getVisit().getCustomer().getFullName();
        }
        String roomName = q.getDepartment().getName();
        String content = String.format("Có bệnh nhân mới (Tên: %s) xếp hàng chờ khám tại phòng %s", patientName, roomName);
        
        for (StaffInfo staff : staffDutyService.findOnDutyStaff(q.getDepartment(), LocalDateTime.now(CLINIC_ZONE))) {
            if (staff.getSystemRole() == null || (!staff.getSystemRole().isDoctor()
                    && staff.getSystemRole() != org.example.doansummer2026.enums.SystemRole.NURSE)) continue;
            if (staff.getProfile() == null) continue;
            try {
                notificationService.create(new org.example.doansummer2026.dto.notification.NotificationCreateRequest(
                        staff.getProfile().getProfileId(),
                        org.example.doansummer2026.enums.NotificationType.GENERAL,
                        org.example.doansummer2026.enums.NotificationChannel.IN_APP,
                        "Bệnh nhân mới",
                        content,
                        "QueueTicket",
                        q.getTicketId()
                ));
            } catch (Exception ignored) {
                // Thong bao khong lam anh huong viec tao hang cho.
            }
        }
    }

    public QueueTicketResponse update(UUID id, QueueTicketUpdateRequest req) {
        findByIdForUpdate(id);
        throw new ConflictException(
                "Không được cập nhật trực tiếp trạng thái hàng chờ; hãy dùng thao tác gọi, bắt đầu, hoàn thành, vắng hoặc quay lại");
    }

    public QueueTicketResponse call(UUID id) {
        QueueTicket q = findByIdForUpdate(id);
        ensureCallableToday(q);
        ensureCurrentStaffCanOperate(q);
        // WAITING_FOR_TEST chua duoc goi lai; chi TEST_DONE moi quay ve bac si.
        if (q.getStatus() != QueueStatus.WAITING && q.getStatus() != QueueStatus.CALLED && q.getStatus() != QueueStatus.TEST_DONE) {
            throw new BadRequestException("Chỉ có thể gọi phiếu đang chờ; trạng thái hiện tại: " + q.getStatus());
        }
        departmentRepo.findByIdForUpdate(q.getDepartment().getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại"));
        QueuePriorityService.RankedTicket next = queuePriorityService.rank(
                        activeRoomTickets(q.getDepartment().getDepartmentId(), q.getWorkDate()))
                .stream().filter(QueuePriorityService.RankedTicket::canCall).findFirst().orElse(null);
        if (next == null || !q.getTicketId().equals(next.ticket().getTicketId())) {
            String nextPatient = next != null && next.ticket().getVisit() != null
                    && next.ticket().getVisit().getCustomer() != null
                    ? next.ticket().getVisit().getCustomer().getFullName() : "bệnh nhân đang đứng đầu";
            throw new ConflictException("Chưa đến lượt gọi phiếu này. Vui lòng gọi " + nextPatient + " trước");
        }
        ensurePatientNotBusy(q);
        q.setStatus(QueueStatus.CALLED);
        q.setCalledAt(LocalDateTime.now());
        QueueTicket saved = repo.save(q);
        updatePatientQueueDepartments(q);
        return toResponse(saved);
    }

    public QueueTicketResponse startExam(UUID id) {
        QueueTicket q = findByIdForUpdate(id);
        ensureCallableToday(q);
        ensureCurrentStaffCanOperate(q);
        if (q.getStatus() != QueueStatus.CALLED && q.getStatus() != QueueStatus.TEST_DONE) {
            throw new BadRequestException("Chỉ có thể bắt đầu với phiếu đã gọi hoặc đang chờ quay lại; trạng thái hiện tại: " + q.getStatus());
        }
        ensurePatientNotBusy(q);
        // Khoa phong nhu mot mutex: hai nhan vien khong the cung luc dua hai
        // benh nhan khac nhau vao IN_PROGRESS trong cung mot phong.
        departmentRepo.findByIdForUpdate(q.getDepartment().getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại"));
        long inprogressCount = repo.countInprogressByDepartment(q.getDepartment().getDepartmentId());
        if (inprogressCount >= 1) {
            throw new BadRequestException("Phòng đã có bệnh nhân đang khám, mỗi phòng chỉ được xử lý một bệnh nhân tại một thời điểm");
        }

        if (q.getDepartment().getDepartmentType() != null
                && q.getDepartment().getDepartmentType().isParaclinical()) {
            q.setStatus(QueueStatus.IN_PROGRESS);
            QueueTicket saved = repo.save(q);
            testRequestService.startRequestsForQueue(q.getTicketId());
            updatePatientQueueDepartments(q);
            return toResponse(saved, null, getWaitingCount(q), null);
        }

        StaffInfo treatingDoctor = staffDutyService.requireCurrentStaffOnDuty(q.getDepartment(), true);
        UUID doctorId = treatingDoctor.getStaffId();

        // Tu dong tao medical record neu chua co, hoac lay record cu
        UUID recordId = null;
        var medicalRecord = getMedicalRecordOrCreate(q, doctorId);
        if (medicalRecord != null) {
            recordId = medicalRecord.recordId();
        }

        q.setStatus(QueueStatus.IN_PROGRESS);
        QueueTicket saved = repo.save(q);
        updatePatientQueueDepartments(q);
        return toResponse(saved, recordId, getWaitingCount(q), medicalRecord);
    }

    public MedicalRecordResponse completeAndReturnRecord(UUID id) {
        return completeAndReturnRecord(id, null);
    }

    public MedicalRecordResponse completeAndReturnRecord(UUID id, MedicalRecordUpdateRequest req) {
        return completeInternal(id, req, false).record();
    }

    public ExaminationTransitionResponse completeAndTransition(UUID id, MedicalRecordUpdateRequest req) {
        CompletionResult result = completeInternal(id, req, true);
        return new ExaminationTransitionResponse(
                result.record(),
                result.nextTicket() == null ? null : toResponse(result.nextTicket()),
                sameRoomChain(result.nextTicket() == null ? id : result.nextTicket().getTicketId()),
                result.nextTicket() != null);
    }

    private CompletionResult completeInternal(UUID id, MedicalRecordUpdateRequest req,
                                              boolean continueInSameRoom) {
        QueueTicket q = findByIdForUpdate(id);
        if (q.getStatus() != QueueStatus.IN_PROGRESS) {
            throw new BadRequestException("Chỉ có thể đóng phiếu đang thực hiện; trạng thái hiện tại: " + q.getStatus());
        }

        // Complete medical record
        if (q.getVisit() == null) {
            throw new BadRequestException("Phiếu không có thông tin lượt khám");
        }
        visitRepo.findByIdForUpdate(q.getVisit().getVisitId())
                .orElseThrow(() -> new ResourceNotFoundException("Lượt khám không tồn tại"));
        departmentRepo.findByIdForUpdate(q.getDepartment().getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại"));
        var record = recordRepo.findByQueueTicket_TicketId(q.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có hồ sơ bệnh án cho lượt khám này"));

        UUID completingStaffId = getCurrentStaffId();
        UUID responsibleDoctorId = record.getDoctor() != null ? record.getDoctor().getStaffId() : null;
        if (completingStaffId == null || responsibleDoctorId == null
                || !responsibleDoctorId.equals(completingStaffId)) {
            throw new BadRequestException("Chỉ bác sĩ đã bắt đầu bệnh án mới được hoàn thành ca khám");
        }

        if (req != null && req.version() != null && !java.util.Objects.equals(req.version(), record.getVersion()))
            throw new ConflictException("Hồ sơ đã được nhân viên khác cập nhật. Vui lòng tải lại trước khi hoàn thành");

        // Cap nhat thong tin medical record neu co request (icd-10, prescription, vitals, ...)
        if (req != null) {
            updateMedicalRecordFields(record, req);
            record = recordRepo.save(record);
        }
        medicalRecordService.validateVitalSignsForCompletion(record);
        medicalRecordService.ensureAllergiesVerifiedForExistingPrescription(record);

        boolean hasTestRequests = req != null && req.testRequests() != null && !req.testRequests().isEmpty();
        boolean waitingForNewTestInvoicePayment = false;
        // Tao TestRequest neu co trong payload (gop voi API hoan thien de tranh goi 2 lan)
        if (hasTestRequests) {
            // responsibleDoctorId was validated together with the authenticated
            // doctor above, so re-checking the same null state here was dead code.
            UUID doctorId = responsibleDoctorId;
            
            java.util.List<UUID> requestedServiceIds = req.testRequests().stream()
                    .map(org.example.doansummer2026.dto.medicalrecord.TestRequestInExaminationRequest::serviceId)
                    .toList();
            if (new java.util.HashSet<>(requestedServiceIds).size() != requestedServiceIds.size()) {
                throw new ConflictException("Dịch vụ này đang bị chọn trùng trong chỉ định.");
            }
            java.util.Map<UUID, org.example.doansummer2026.dto.medicalrecord.TestRequestInExaminationRequest>
                    requestByServiceId = new java.util.LinkedHashMap<>();
            req.testRequests().forEach(item -> requestByServiceId.put(item.serviceId(), item));
            java.util.List<org.example.doansummer2026.model.MedicalService> normalizedServices =
                    serviceSelectionPolicyService.normalizeOrThrow(requestByServiceId.keySet());
            serviceSelectionPolicyService.validateAgainstExisting(
                    normalizedServices.stream().map(org.example.doansummer2026.model.MedicalService::getServiceId).toList(),
                    testRequestRepository.findDistinctActiveServiceIdsByVisit(
                            q.getVisit().getVisitId(),
                            org.example.doansummer2026.enums.TestRequestStatus.CANCELLED));

            // Thay vi tao truc tiep TestRequest -> Tao Invoice (hoa don) truoc
            java.util.List<org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest> invoiceItems = new java.util.ArrayList<>();
            for (org.example.doansummer2026.model.MedicalService svc : normalizedServices) {
                var testReq = requestByServiceId.get(svc.getServiceId());
                // Neu benh nhan da dat va thanh toan dich vu nay, gan yeu cau
                // hien co vao ho so kham thay vi tao trung va thu tien lan hai.
                if (testRequestService.attachPrepaidRequestToExamination(
                        q.getVisit().getVisitId(), record.getRecordId(), svc.getServiceId(),
                        doctorId, testReq.notes())) {
                    continue;
                }
                // TestRequest cua dich vu cu chi duoc tao sau khi hoa don PAID, do do
                // phai kiem tra truoc khi tao Invoice o day, khong chi o TestRequestService.create().
                testRequestService.ensureServiceNotAlreadyRequested(record.getRecordId(), svc.getServiceId());
                if (svc.getDepartmentType() == null || !svc.getDepartmentType().isParaclinical()) {
                    throw new BadRequestException(
                            "Bác sĩ chỉ được chỉ định dịch vụ cận lâm sàng; dịch vụ khám khác phải được lễ tân tạo lượt mới"
                    );
                }
                invoiceItems.add(new org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest(
                        svc.getServiceId(),
                        svc.getName(),
                        svc.getServiceCode(),
                        svc.getPrice() != null ? svc.getPrice() : java.math.BigDecimal.ZERO,
                        1,
                        java.math.BigDecimal.ZERO, // discountPercent
                        java.math.BigDecimal.ZERO, // discountAmount
                        svc.getPrice() != null ? svc.getPrice() : java.math.BigDecimal.ZERO, // finalPrice
                        testReq.notes()
                ));
            }
            
            if (!invoiceItems.isEmpty()) {
                waitingForNewTestInvoicePayment = true;
                invoiceService.create(new org.example.doansummer2026.dto.invoice.InvoiceCreateRequest(
                        q.getVisit().getCustomer() != null ? q.getVisit().getCustomer().getProfileId() : null,
                        q.getVisit().getVisitId(),
                        record.getRecordId(),
                        LocalDate.now(CLINIC_ZONE),
                        java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.ZERO,
                        "Hóa đơn xét nghiệm / CĐHA chỉ định từ phòng khám",
                        doctorId,
                        invoiceItems
                ));
            }
        }

        // Chi cac CLS duoc bac si chon trong benh an hien tai moi giu phong
        // kham o WAITING_FOR_TEST. CLS dat san khac van la buoc doc lap cua visit.
        boolean hasIncompleteLinkedTests =
                testRequestService.hasIncompleteRequestsForRecord(record.getRecordId());
        boolean shouldWaitForTests = waitingForNewTestInvoicePayment || hasIncompleteLinkedTests;
        if (shouldWaitForTests) {
            record.setStatus(MedicalRecordStatus.IN_PROGRESS);
            record.setCompletedAt(null);
            record.setDoctorConfirmedBy(null);
            record.setDoctorConfirmedAt(null);
            recordRepo.save(record);
        } else if (record.getStatus() != MedicalRecordStatus.COMPLETED) {
            boolean hasUnpaidInvoices = invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()).stream()
                    .anyMatch(inv -> inv.getStatus() == org.example.doansummer2026.enums.InvoiceStatus.PENDING);
            if (hasUnpaidInvoices) {
                throw new BadRequestException("Bệnh nhân chưa thanh toán hóa đơn cận lâm sàng/dịch vụ");
            }

            boolean hasDiagnosis = record.getDiagnosis() != null && !record.getDiagnosis().trim().isEmpty();
            boolean hasConclusion = record.getConclusion() != null && !record.getConclusion().trim().isEmpty();
            boolean hasIcd10 = record.getIcdSelections() != null && !record.getIcdSelections().isEmpty();
            if (!hasDiagnosis && !hasConclusion && !hasIcd10) {
                throw new BadRequestException(
                        "Vui lòng nhập chẩn đoán, kết luận hoặc chọn mã ICD-10 trước khi hoàn thành hồ sơ");
            }

            record.setStatus(MedicalRecordStatus.COMPLETED);
            record.setCompletedAt(LocalDateTime.now());
            StaffInfo confirmer = staffRepo.findById(completingStaffId).orElse(null);
            record.setDoctorConfirmedBy(confirmer);
            record.setDoctorConfirmedAt(LocalDateTime.now());
            recordRepo.save(record);
        }

        // Dat status queue ticket:
        // - Co test request -> WAITING_FOR_TEST (cho ket qua xet nghiem)
        // - Khong co -> DONE (hoan thien hoan toan)
        if (shouldWaitForTests) {
            q.setStatus(QueueStatus.WAITING_FOR_TEST);
            q.setCalledAt(null);
        } else {
            q.setStatus(QueueStatus.DONE);
            q.setCompletedAt(LocalDateTime.now());
        }
        repo.save(q);
        QueueTicket nextTicket = null;
        if (!shouldWaitForTests && continueInSameRoom) {
            nextTicket = continueNextSameRoomExamination(q, completingStaffId);
        }
        if (nextTicket == null && (!shouldWaitForTests || !waitingForNewTestInvoicePayment)) {
            patientJourneyService.activateNext(q.getVisit().getVisitId());
        }
        updatePatientQueueDepartments(q);
        if (nextTicket != null) updatePatientQueueDepartments(nextTicket);

        return new CompletionResult(MedicalRecordResponse.from(record, true), nextTicket);
    }

    private QueueTicket continueNextSameRoomExamination(QueueTicket completedTicket,
                                                         UUID doctorId) {
        QueueTicket next = sameRoomTickets(completedTicket).stream()
                .filter(ticket -> ticket.getStatus() == QueueStatus.BLOCKED
                        || ticket.getStatus() == QueueStatus.WAITING)
                .findFirst().orElse(null);
        if (next == null) return null;

        StaffInfo onDutyDoctor = staffDutyService.requireCurrentStaffOnDuty(next.getDepartment(), true);
        if (doctorId == null || !doctorId.equals(onDutyDoctor.getStaffId())) {
            throw new ConflictException("Dịch vụ tiếp theo trong phòng phải do bác sĩ đang phụ trách tiếp tục");
        }
        long otherInProgress = repo.countInprogressByDepartment(next.getDepartment().getDepartmentId());
        if (otherInProgress > 0) {
            throw new ConflictException("Phòng đã có bệnh nhân khác đang được xử lý");
        }

        MedicalRecord nextRecord = recordRepo.findByQueueTicket_TicketId(next.getTicketId()).orElse(null);
        if (nextRecord == null) {
            nextRecord = MedicalRecord.builder()
                    .visit(next.getVisit())
                    .queueTicket(next)
                    .doctor(onDutyDoctor)
                    .status(MedicalRecordStatus.IN_PROGRESS)
                    .build();
            nextRecord = recordRepo.save(nextRecord);
        }
        medicalRecordService.inheritFirstVisitVitalSigns(nextRecord);

        next.setStatus(QueueStatus.IN_PROGRESS);
        next.setCalledAt(completedTicket.getCalledAt() != null
                ? completedTicket.getCalledAt() : LocalDateTime.now());
        next.setCompletedAt(null);
        return repo.save(next);
    }

    @Transactional(readOnly = true)
    public SameRoomExaminationChainResponse sameRoomChain(UUID ticketId) {
        QueueTicket current = findById(ticketId);
        List<QueueTicket> tickets = sameRoomTickets(current);
        int currentIndex = java.util.stream.IntStream.range(0, tickets.size())
                .filter(index -> tickets.get(index).getTicketId().equals(ticketId))
                .findFirst().orElse(0);
        List<SameRoomExaminationChainResponse.ServiceStep> services = tickets.stream()
                .map(ticket -> new SameRoomExaminationChainResponse.ServiceStep(
                        ticket.getTicketId(),
                        recordRepo.findByQueueTicket_TicketId(ticket.getTicketId())
                                .map(MedicalRecord::getRecordId).orElse(null),
                        ticket.getService() != null ? ticket.getService().getServiceId() : null,
                        ticket.getService() != null ? ticket.getService().getServiceCode() : null,
                        ticket.getService() != null ? ticket.getService().getName() : "Dịch vụ khám",
                        ticket.getStatus()))
                .toList();
        int completed = (int) tickets.stream()
                .filter(ticket -> ticket.getStatus() == QueueStatus.DONE).count();
        return new SameRoomExaminationChainResponse(
                current.getVisit() != null ? current.getVisit().getVisitId() : null,
                current.getDepartment() != null ? current.getDepartment().getDepartmentId() : null,
                current.getDepartment() != null ? current.getDepartment().getName() : null,
                tickets.stream().map(QueueTicket::getQueueNumber).min(Integer::compareTo)
                        .orElse(current.getQueueNumber()),
                ticketId,
                tickets.isEmpty() ? 0 : currentIndex + 1,
                tickets.size(), completed, services);
    }

    private List<QueueTicket> sameRoomTickets(QueueTicket current) {
        if (current == null || current.getVisit() == null || current.getDepartment() == null) return List.of();
        return repo.findAllByVisit_VisitId(current.getVisit().getVisitId()).stream()
                .filter(ticket -> ticket.getDepartment() != null
                        && current.getDepartment().getDepartmentId().equals(ticket.getDepartment().getDepartmentId()))
                .filter(ticket -> java.util.Objects.equals(current.getWorkDate(), ticket.getWorkDate()))
                .filter(ticket -> ticket.getService() != null
                        && ticket.getService().getDepartmentType() != null
                        && ticket.getService().getDepartmentType().normalized()
                        == org.example.doansummer2026.enums.DepartmentType.EXAMINATION)
                .sorted(Comparator
                        .comparing((QueueTicket ticket) -> ticket.getCreatedAt(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(QueueTicket::getQueueNumber))
                .toList();
    }

    private record CompletionResult(MedicalRecordResponse record, QueueTicket nextTicket) {}

    // --- Doctor examination facade (1:1 with /api/doctor/examinations/{id}) ---

    /**
     * Load the examination (medical record + nested details) the doctor is editing.
     * Accepts either a queue-ticket id or a medical-record id.
     */
    @Transactional(readOnly = true)
    public MedicalRecordResponse loadExamination(UUID id) {
        MedicalRecord record = resolveMedicalRecord(id);
        return MedicalRecordResponse.from(record, true);
    }

    /**
     * Save a draft of the examination. Delegates to MedicalRecordService.saveDraft
     * (ownership + optimistic-lock checks apply). Accepts a queue-ticket id or record id.
     */
    @Transactional
    public MedicalRecordResponse saveExaminationDraft(UUID id, MedicalRecordUpdateRequest req) {
        MedicalRecord record = resolveMedicalRecord(id);
        return medicalRecordService.saveDraft(record.getRecordId(), req);
    }

    /**
     * Complete the examination. Resolves the id to the queue ticket and runs the canonical
     * complete flow already wired at /api/v1/queue-tickets/{id}/complete: ordered test requests
     * are billed via an invoice and the queue moves to WAITING_FOR_TEST; otherwise the record
     * is closed (COMPLETED) and the queue moves to DONE.
     */
    @Transactional
    public MedicalRecordResponse completeExamination(UUID id, MedicalRecordUpdateRequest req) {
        UUID ticketId = resolveTicketId(id);
        return completeAndReturnRecord(ticketId, req);
    }

    /** Resolve an id that may be a queue-ticket id or a medical-record id to a medical record. */
    private MedicalRecord resolveMedicalRecord(UUID id) {
        MedicalRecord record = recordRepo.findById(id).orElse(null);
        if (record != null) return record;
        QueueTicket ticket = repo.findById(id).orElse(null);
        if (ticket != null) {
            return recordRepo.findByQueueTicket_TicketId(ticket.getTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chưa có hồ sơ cho phiếu khám: " + id));
        }
        throw new ResourceNotFoundException("Không tìm thấy hồ sơ bệnh án: " + id);
    }

    /** Resolve an id that may be a queue-ticket id or a medical-record id to a queue ticket id. */
    private UUID resolveTicketId(UUID id) {
        if (repo.existsById(id)) return id;
        MedicalRecord record = recordRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ bệnh án: " + id));
        QueueTicket ticket = record.getQueueTicket(); // lazy; accessed within tx
        if (ticket == null) {
            throw new ResourceNotFoundException("Hồ sơ không có phiếu khám đang thực hiện: " + id);
        }
        return ticket.getTicketId();
    }

    private void updateMedicalRecordFields(MedicalRecord r, MedicalRecordUpdateRequest req) {
        medicalRecordService.validatePrescriptionAllergyStatus(r, req.prescriptionItems());
        if (req.chiefComplaint() != null) r.setChiefComplaint(req.chiefComplaint());
        if (req.clinicalFindings() != null) r.setClinicalFindings(req.clinicalFindings());
        if (req.diagnosis() != null) r.setDiagnosis(req.diagnosis());
        if (req.prescriptionNote() != null) r.setPrescriptionNote(req.prescriptionNote());
        if (req.conclusion() != null) r.setConclusion(req.conclusion());
        if (req.patientInstruction() != null) r.setPatientInstruction(req.patientInstruction());
        // Cap nhat thong tin tai kham (follow-up)
        if (req.followUp() != null) {
            String note = req.followUp().note();
            if ((note == null || note.trim().isEmpty()) && req.followUp().preferredDate() == null) {
                note = "Cần tái khám";
            }
            r.setFollowUpNote(note);
            r.setFollowUpDate(req.followUp().preferredDate());
        }

        // Cap nhat thuoc trong don
        if (req.prescriptionItems() != null) {
            r.getPrescriptionItems().clear();
            req.prescriptionItems().forEach(p -> {
                if (p.medicineName() != null && !p.medicineName().isBlank() && p.quantity() != null) {
                    var item = org.example.doansummer2026.model.PrescriptionItem.builder()
                            .medicalRecord(r)
                            .medicineName(p.medicineName())
                            .quantity(p.quantity())
                            .unit(p.unit())
                            .note(p.note())
                            .frequencyPerDay(p.frequencyPerDay())
                            .build();
                    r.getPrescriptionItems().add(item);
                }
            });
        }

        // Cap nhat benh chuan doan ICD-10
        if (req.icdSelections() != null) {
            r.getIcdSelections().clear();
            req.icdSelections().forEach(icd -> {
                // Uu tien su dung codeName tu request, fallback sang lookup DB
                String codeName = icd.codeName();
                if (codeName == null || codeName.isBlank()) {
                    Icd10Code icdCode = icd10Repo.findById(icd.code()).orElse(null);
                    codeName = icdCode != null ? icdCode.getName() : null;
                }
                Icd10Selection selection = Icd10Selection.builder()
                        .medicalRecord(r)
                        .code(icd.code())
                        .codeName(codeName)
                        .note(icd.note())
                        .build();
                r.getIcdSelections().add(selection);
            });
        }

        // Tao moi vital signs neu chua co va co du lieu
        if (r.getVitalSigns() == null && hasVitalSignsUpdate(req)) {
            var v = org.example.doansummer2026.model.VitalSigns.builder()
                    .medicalRecord(r)
                    .bloodPressure(req.bloodPressure())
                    .heartRate(req.heartRate())
                    .temperature(req.temperature())
                    .weight(req.weight())
                    .height(req.height())
                    .recordedAt(LocalDateTime.now(CLINIC_ZONE))
                    .recordedBy(r.getDoctor())
                    .build();
            r.setVitalSigns(v);
        } else if (r.getVitalSigns() != null && hasVitalSignsUpdate(req)) {
            var v = r.getVitalSigns();
            if (req.bloodPressure() != null) v.setBloodPressure(req.bloodPressure());
            if (req.heartRate() != null) v.setHeartRate(req.heartRate());
            if (req.temperature() != null) v.setTemperature(req.temperature());
            if (req.weight() != null) v.setWeight(req.weight());
            if (req.height() != null) v.setHeight(req.height());
        }
    }

    private boolean hasVitalSignsUpdate(MedicalRecordUpdateRequest req) {
        return req.bloodPressure() != null || req.heartRate() != null || req.temperature() != null ||
               req.weight() != null || req.height() != null;
    }

    public QueueTicketResponse complete(UUID id) {
        findByIdForUpdate(id);
        throw new ConflictException(
                "Không được hoàn thành trực tiếp hàng chờ; hãy dùng thao tác hoàn thành khám hoặc kết thúc dịch vụ cận lâm sàng");
    }

    /**
     * Kết thúc thời gian người bệnh hiện diện tại phòng CLS. Với dịch vụ lấy mẫu,
     * kết quả có thể tiếp tục được xử lý sau khi mẫu hợp lệ đã được thu nhận.
     */
    public QueueTicketResponse finishParaclinicalQueue(UUID id) {
        QueueTicket q = findByIdForUpdate(id);
        ensureCurrentStaffCanOperate(q);
        if (q.getDepartment().getDepartmentType() == null
                || !q.getDepartment().getDepartmentType().isParaclinical()) {
            throw new BadRequestException("Thao tác này chỉ áp dụng cho phòng cận lâm sàng");
        }
        if (q.getStatus() == QueueStatus.DONE) {
            return toResponse(q);
        }
        if (q.getStatus() != QueueStatus.IN_PROGRESS) {
            throw new BadRequestException("Chỉ có thể kết thúc khi bệnh nhân đang được thực hiện tại phòng");
        }
        List<org.example.doansummer2026.model.TestRequest> activeRequests =
                testRequestRepository.findAllByQueueTicket_TicketId(q.getTicketId()).stream()
                .filter(request -> request.getStatus()
                        != org.example.doansummer2026.enums.TestRequestStatus.CANCELLED)
                .toList();
        for (org.example.doansummer2026.model.TestRequest request : activeRequests) {
            boolean requiresSpecimen = request.getService() != null
                    && Boolean.TRUE.equals(request.getService().getRequiresSpecimen());
            if (!requiresSpecimen) {
                if (request.getStatus() != org.example.doansummer2026.enums.TestRequestStatus.COMPLETED
                        && request.getTestResult() == null) {
                    throw new ConflictException(
                            "Vui lòng lưu thông tin thực hiện hoặc kết quả nháp trước khi cho bệnh nhân rời phòng");
                }
                continue;
            }
            var result = request.getTestResult();
            boolean collected = result != null && result.getCollectedAt() != null
                    && result.getSampleId() != null && !result.getSampleId().isBlank()
                    && result.getSampleType() != null;
            if (!collected) {
                throw new ConflictException("Vui lòng lưu thông tin lấy mẫu trước khi cho bệnh nhân rời phòng");
            }
            if (result.getSampleStatus() != org.example.doansummer2026.enums.SpecimenStatus.ACCEPTED) {
                throw new ConflictException("Mẫu chưa đạt yêu cầu hoặc đang cần lấy lại");
            }
        }
        q.setStatus(QueueStatus.DONE);
        q.setCompletedAt(LocalDateTime.now());
        QueueTicket saved = repo.save(q);
        if (q.getVisit() != null) patientJourneyService.activateNext(q.getVisit().getVisitId());
        updatePatientQueueDepartments(q);
        return toResponse(saved);
    }

    public QueueTicketResponse skip(UUID id) {
        QueueTicket q = findByIdForUpdate(id);
        ensureCurrentStaffCanOperate(q);
        if (q.getStatus() != QueueStatus.CALLED) {
            throw new BadRequestException("Chỉ có thể đánh dấu vắng sau khi đã gọi bệnh nhân");
        }
        q.setStatus(QueueStatus.SKIPPED);
        testRequestService.blockRequestsForQueue(q.getTicketId());
        QueueTicket saved = repo.save(q);
        // "Vang" la tam dung benh nhan, khong phai da hoan thanh dich vu.
        // Khong duoc mo buoc ke tiep cho den khi benh nhan quay lai hoac nhan
        // vien xu ly huy/reschedule ca kham.
        updatePatientQueueDepartments(q);
        return toResponse(saved);
    }

    public QueueTicketResponse returnToQueue(UUID id) {
        QueueTicket q = findByIdForUpdate(id);
        ensureCurrentStaffCanOperate(q);
        return restoreSkippedTicket(q);
    }

    private QueueTicketResponse restoreSkippedTicket(QueueTicket q) {
        if (q.getStatus() != QueueStatus.SKIPPED) {
            throw new BadRequestException("Chỉ có thể đưa phiếu vắng quay lại hàng chờ");
        }
        if (q.getWorkDate() == null || !q.getWorkDate().equals(LocalDate.now(CLINIC_ZONE))) {
            throw new BadRequestException("Chỉ có thể đưa bệnh nhân quay lại hàng chờ trong ngày của phiếu");
        }
        boolean mustWaitForCurrentStep = q.getVisit() != null
                && patientJourneyService.hasActiveStep(q.getVisit().getVisitId());
        q.setStatus(mustWaitForCurrentStep ? QueueStatus.BLOCKED : QueueStatus.WAITING);
        // Giữ calledAt của lần gọi trước để read model nhận biết đây là khách
        // đã quay lại sau khi bị đánh vắng. Khi gọi lại, call() sẽ cập nhật mốc mới.
        q.setCompletedAt(null);
        testRequestService.restoreRequestsForQueue(q.getTicketId(), mustWaitForCurrentStep);
        QueueTicket saved = repo.save(q);
        updateDepartmentStatus(q.getDepartment().getDepartmentId());
        return toResponse(saved);
    }

    private void ensureCallableToday(QueueTicket queue) {
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        boolean calledTodayFromCompletedTests = queue.getStatus() == QueueStatus.CALLED
                && queue.getCalledAt() != null
                && today.equals(queue.getCalledAt().toLocalDate());
        boolean returningAfterCompletedTests = queue.getWorkDate() != null
                && queue.getWorkDate().isBefore(today)
                && (queue.getStatus() == QueueStatus.TEST_DONE || calledTodayFromCompletedTests);
        // A TEST_DONE examination ticket has already produced clinical work
        // (the ordered tests).  It may be called on a later day only so the
        // responsible doctor can review the result and close that record.
        // All untouched downstream WAITING/CALLED/BLOCKED tickets are changed
        // to SKIPPED by the end-of-day job and cannot enter this branch.
        if (!returningAfterCompletedTests
                && (queue.getWorkDate() == null || !today.equals(queue.getWorkDate()))) {
            throw new BadRequestException(
                    "Phiếu hàng chờ đã qua ngày; vui lòng tiếp nhận hoặc xếp lịch lại thay vì gọi phiếu cũ");
        }
        if (queue.getDepartment() != null
                && queue.getDepartment().getStatus() == DepartmentStatus.MAINTENANCE) {
            throw new BadRequestException("Phòng đang bảo trì, không thể gọi hoặc bắt đầu xử lý bệnh nhân");
        }
    }

    public void delete(UUID id) {
        findById(id);
        throw new ConflictException(
                "Không xóa phiếu hàng chờ để tránh mất lịch sử; hãy dùng thao tác đánh vắng khi bệnh nhân không có mặt");
    }

    public QueueTicket findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu xếp hàng không tồn tại: " + id));
    }

    private QueueTicket findByIdForUpdate(UUID id) {
        return repo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu xếp hàng không tồn tại: " + id));
    }

    private UUID getCurrentStaffId() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Map<?, ?> map) {
            Object staffId = map.get("staffId");
            if (staffId instanceof String sid && !sid.isBlank()) {
                try {
                    return UUID.fromString(sid);
                } catch (IllegalArgumentException ignored) {
                    // Fall through to username lookup for an old token.
                }
            }
            Object username = map.get("username");
            if (username instanceof String value && !value.isBlank()) {
                return staffRepo.findFirstByProfile_Account_Username(value)
                        .map(staff -> staff.getStaffId()).orElse(null);
            }
        }
        return null;
    }

    private void ensureCurrentStaffCanOperate(QueueTicket ticket) {
        if (ticket.getDepartment() == null) {
            throw new BadRequestException("Không xác định được nhân viên hoặc phòng thực hiện");
        }
        staffDutyService.requireCurrentStaffOnDuty(ticket.getDepartment(), false);
    }

    @Transactional(readOnly = true)
    public QueueTicketResponse getInprogressByDepartment(UUID departmentId) {
        QueueTicket ticket = repo.findTopByDepartment_DepartmentIdAndStatusOrderByCreatedAtAsc(departmentId, QueueStatus.IN_PROGRESS)
                .orElse(null);
        if (ticket == null) return null;
        var medicalRecord = getMedicalRecordByQueueTicket(ticket.getTicketId());
        UUID recordId = medicalRecord != null ? medicalRecord.recordId() : null;
        return toResponse(ticket, recordId, null, medicalRecord);
    }

    /** Lấy hàng chờ theo bộ xếp hạng chung: ưu tiên quay lại, giữ đầu FIFO, rồi nhóm lịch hẹn trước khách thường. */
    @Transactional(readOnly = true)
    public PageResponse<QueueTicketResponse> getWaitingByDepartment(UUID departmentId, LocalDate workDate, QueueStatus status, Pageable pageable) {
        LocalDate effectiveDate = workDate != null ? workDate : LocalDate.now(CLINIC_ZONE);
        List<QueuePriorityService.RankedTicket> ranked = queuePriorityService.rank(
                activeRoomTickets(departmentId, effectiveDate));
        List<QueuePriorityService.RankedTicket> visible = ranked.stream()
                .filter(item -> item.ticket().getStatus() != QueueStatus.IN_PROGRESS)
                .filter(item -> status == null
                        ? item.ticket().getStatus() != QueueStatus.BLOCKED
                        : item.ticket().getStatus() == status)
                .toList();
        int pageNumber = pageable.isPaged() ? pageable.getPageNumber() : 0;
        int pageSize = pageable.isPaged() ? pageable.getPageSize() : Math.max(1, visible.size());
        int from = Math.min(pageNumber * pageSize, visible.size());
        int to = Math.min(from + pageSize, visible.size());
        List<QueueTicketResponse> content = visible.subList(from, to).stream()
                .map(item -> applyRanking(toResponse(item.ticket(), getRecordId(item.ticket()),
                        null, null), item)).toList();
        int totalPages = visible.isEmpty() ? 0 : (int) Math.ceil((double) visible.size() / pageSize);
        return new PageResponse<>(content, pageNumber, pageSize, visible.size(), totalPages,
                pageNumber == 0, totalPages == 0 || pageNumber + 1 >= totalPages);
    }

    private List<QueueTicket> activeRoomTickets(UUID departmentId, LocalDate workDate) {
        return repo.findWaitingPrioritized(departmentId, workDate,
                List.of(QueueStatus.IN_PROGRESS, QueueStatus.CALLED, QueueStatus.TEST_DONE,
                        QueueStatus.WAITING, QueueStatus.WAITING_FOR_TEST), Pageable.unpaged()).getContent();
    }

    private Map<UUID, QueuePriorityService.RankedTicket> rankingByTicket(UUID departmentId,
                                                                         LocalDate workDate) {
        Map<UUID, QueuePriorityService.RankedTicket> result = new LinkedHashMap<>();
        queuePriorityService.rank(activeRoomTickets(departmentId, workDate))
                .forEach(item -> result.put(item.ticket().getTicketId(), item));
        return result;
    }

    private QueueTicketResponse applyRanking(QueueTicketResponse response,
                                              QueuePriorityService.RankedTicket ranked) {
        if (ranked == null) return response;
        QueuePriorityService.PriorityInfo priority = ranked.priority();
        return response.withQueuePriority(ranked.waitingPosition(), priority.category(), priority.label(),
                priority.appointmentScheduledAt(), ranked.canCall());
    }

    @Transactional(readOnly = true)
    public PageResponse<QueueTicketResponse> getAllInprogress(Pageable pageable) {
        Page<QueueTicket> page = repo.findAllByStatus(QueueStatus.IN_PROGRESS, pageable);
        return PageResponse.from(page, q -> toResponse(q, getRecordId(q), getWaitingCount(q), null));
    }

    private MedicalRecordResponse getMedicalRecord(UUID visitId) {
        if (visitId == null) return null;
        var record = recordRepo.findFirstByVisit_VisitIdOrderByCreatedAtDesc(visitId).orElse(null);
        if (record == null) return null;
        return MedicalRecordResponse.from(record, false);
    }

    /** Hồ sơ của màn khám phải thuộc đúng phiếu hàng chờ, không lấy hồ sơ mới nhất của cả lượt khám. */
    private MedicalRecordResponse getMedicalRecordByQueueTicket(UUID ticketId) {
        if (ticketId == null) return null;
        var record = recordRepo.findByQueueTicket_TicketId(ticketId).orElse(null);
        return record == null ? null : MedicalRecordResponse.from(record, true);
    }

    private MedicalRecordResponse getMedicalRecordOrCreate(QueueTicket q, UUID doctorId) {
        if (q.getVisit() == null) return null;
        var existingRecord = recordRepo.findByQueueTicket_TicketId(q.getTicketId()).orElse(null);
        MedicalRecord record;
        if (existingRecord == null) {
            StaffInfo doctor = staffRepo.findById(doctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bác sĩ không tồn tại: " + doctorId));
            // Moi dich vu kham co mot benh an rieng theo QueueTicket. Ho so tam
            // cua CLS dat san van doc lap; chi khi bac si chon dung dich vu thi
            // TestRequest moi duoc gan sang benh an nay de cho quay lai phong.
            record = MedicalRecord.builder()
                    .visit(q.getVisit())
                    .queueTicket(q)
                    .doctor(doctor)
                    .status(MedicalRecordStatus.IN_PROGRESS)
                    .build();
            record = recordRepo.save(record);
        } else {
            record = existingRecord;
        }
        record = medicalRecordService.inheritFirstVisitVitalSigns(record);
        // Fetch again with vital signs for response
        return MedicalRecordResponse.from(record, false);
    }

    private UUID getRecordId(QueueTicket q) {
        return recordRepo.findByQueueTicket_TicketId(q.getTicketId()).map(r -> r.getRecordId()).orElse(null);
    }

    private Integer getWaitingCount(QueueTicket q) {
        UUID deptId = q.getDepartment() != null ? q.getDepartment().getDepartmentId() : null;
        if (deptId == null) return null;
        return (int) repo.countWaitingByDepartment(deptId);
    }

    private QueueTicketResponse toResponse(QueueTicket ticket) {
        return toResponse(ticket, null, null, null);
    }

    private QueueTicketResponse toResponse(QueueTicket ticket, UUID recordId, Integer waitingCount,
                                           MedicalRecordResponse medicalRecord) {
        QueueTicket busyTicket = findBusyTicket(ticket);
        SameRoomExaminationChainResponse examinationChain = ticket.getService() != null
                && ticket.getService().getDepartmentType() != null
                && ticket.getService().getDepartmentType().normalized()
                == org.example.doansummer2026.enums.DepartmentType.EXAMINATION
                ? sameRoomChain(ticket.getTicketId()) : null;
        return QueueTicketResponse.from(ticket, recordId, waitingCount, medicalRecord,
                busyTicket != null, busyTicket,
                testRequestRepository != null
                        && ticket.getDepartment() != null && ticket.getDepartment().getDepartmentType() != null
                        && ticket.getDepartment().getDepartmentType().isParaclinical()
                        ? testRequestRepository.findAllByQueueTicket_TicketId(ticket.getTicketId()) : List.of(),
                examinationChain);
    }

    private QueueTicket findBusyTicket(QueueTicket ticket) {
        if (ticket == null || ticket.getTicketId() == null || ticket.getWorkDate() == null
                || ticket.getVisit() == null || ticket.getVisit().getCustomer() == null) {
            return null;
        }
        return repo.findPatientBusyTickets(
                        ticket.getVisit().getCustomer().getProfileId(),
                        ticket.getWorkDate(), ticket.getTicketId(),
                        List.of(QueueStatus.CALLED, QueueStatus.IN_PROGRESS))
                .stream().findFirst().orElse(null);
    }

    private void ensurePatientNotBusy(QueueTicket ticket) {
        if (ticket.getVisit() == null || ticket.getVisit().getCustomer() == null) return;
        profileRepo.findByIdForUpdate(ticket.getVisit().getCustomer().getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ bệnh nhân"));
        QueueTicket busyTicket = findBusyTicket(ticket);
        if (busyTicket == null) return;
        String roomName = busyTicket.getDepartment() != null
                ? busyTicket.getDepartment().getName() : "phòng khác";
        throw new ConflictException("Bệnh nhân đang được phục vụ tại " + roomName
                + ". Chưa thể gọi hoặc bắt đầu tại phòng này");
    }

    private void updatePatientQueueDepartments(QueueTicket ticket) {
        if (ticket == null || ticket.getDepartment() == null) return;
        updateDepartmentStatus(ticket.getDepartment().getDepartmentId());
        if (ticket.getVisit() == null || ticket.getVisit().getCustomer() == null
                || ticket.getWorkDate() == null) return;
        for (UUID departmentId : repo.findPatientQueueDepartmentIds(
                ticket.getVisit().getCustomer().getProfileId(), ticket.getWorkDate())) {
            if (!departmentId.equals(ticket.getDepartment().getDepartmentId())) {
                publishDepartmentQueueUpdate(departmentId);
            }
        }
    }

    /**
     * Dem so benh nhan cho ket qua xet nghiem - bay loi dau han cho bac si.
     */
    @Transactional(readOnly = true)
    public long countWaitingForTestByDepartment(UUID departmentId) {
        return repo.countWaitingForTestByDepartment(departmentId);
    }

    public long countTestDoneByDepartment(UUID departmentId) {
        return repo.countTestDoneByDepartment(departmentId);
    }

    /**
     * Danh dau queue ticket da hoan thanh xet nghiem (WAITING_FOR_TEST -> TEST_DONE).
     */
    public QueueTicketResponse markTestDone(UUID id) {
        QueueTicket q = findByIdForUpdate(id);
        ensureCurrentStaffCanOperate(q);
        if (q.getVisit() == null) {
            throw new BadRequestException("Phiếu không có thông tin lượt khám");
        }
        List<org.example.doansummer2026.dto.testrequest.TestRequestResponse> requests =
                testRequestService.listByVisit(q.getVisit().getVisitId());
        if (requests.isEmpty()) {
            throw new BadRequestException("Lượt khám chưa có yêu cầu cận lâm sàng");
        }
        boolean hasIncompleteResult = requests.stream().anyMatch(request ->
                request.status() != org.example.doansummer2026.enums.TestRequestStatus.COMPLETED
                        && request.status() != org.example.doansummer2026.enums.TestRequestStatus.CANCELLED);
        if (hasIncompleteResult) {
            throw new BadRequestException("Chưa thể đánh dấu đã có kết quả vì còn yêu cầu cận lâm sàng chưa hoàn thành");
        }
        if (q.getStatus() != QueueStatus.WAITING_FOR_TEST) {
            throw new BadRequestException("Chỉ có thể đánh dấu đã có kết quả từ trạng thái chờ kết quả; trạng thái hiện tại: " + q.getStatus());
        }
        q.setStatus(QueueStatus.TEST_DONE);
        repo.save(q);
        updateDepartmentStatus(q.getDepartment().getDepartmentId());
        return toResponse(q);
    }
    private void updateDepartmentStatus(UUID departmentId) {
        if (departmentId == null) return;
        Department dept = departmentRepo.findById(departmentId).orElse(null);
        if (dept == null) return;
        
        // Neu phong dang o trang thai MAINTENANCE thi khong doi sang AVAILABLE / IN_SESSION
        if (dept.getStatus() == DepartmentStatus.MAINTENANCE) {
            return;
        }

        long activeTickets = repo.countActiveTicketsByDepartment(departmentId);
        
        if (activeTickets > 0) {
            dept.setStatus(DepartmentStatus.IN_SESSION);
        } else {
            dept.setStatus(DepartmentStatus.AVAILABLE);
        }
        departmentRepo.save(dept);

        // Notify clients about queue changes in this department
        publishDepartmentQueueUpdate(departmentId);
    }

    private void publishDepartmentQueueUpdate(UUID departmentId) {
        if (departmentId == null) return;
        Runnable publish = () -> {
            try {
                messagingTemplate.convertAndSend(
                        "/topic/department-" + departmentId + "-queue", "QUEUE_UPDATED");
                messagingTemplate.convertAndSend("/topic/queue-display", "QUEUE_UPDATED");
            } catch (Exception e) {
                // Ignore messaging errors so it doesn't break the business transaction.
            }
        };
        if (org.springframework.transaction.support.TransactionSynchronizationManager
                .isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publish.run();
                        }
                    });
        } else {
            publish.run();
        }
    }
}
