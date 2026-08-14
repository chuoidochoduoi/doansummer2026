package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.dto.queueTicket.QueueTicketCreateRequest;
import org.example.doansummer2026.dto.queueTicket.QueueTicketResponse;
import org.example.doansummer2026.dto.queueTicket.QueueTicketUpdateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.dto.medicalRecord.TestRequestInExaminationRequest;
import org.example.doansummer2026.dto.icd.ICD10SelectionCreateRequest;
import org.example.doansummer2026.dto.testRequest.TestRequestCreateRequest;
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
    private final Icd10CodeRepository icd10Repo;
    private final TestRequestService testRequestService;
    private final PatientJourneyService patientJourneyService;
    private final MedicalRecordService medicalRecordService;
    private final org.example.doansummer2026.repository.InvoiceRepository invoiceRepo;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @Autowired
    @Lazy
    private InvoiceServiceInterface invoiceService;

    @Transactional(readOnly = true)
    public PageResponse<QueueTicketResponse> search(UUID departmentId, LocalDate workDate,
                                                     QueueStatus status, Pageable pageable) {
        Page<QueueTicket> page = repo.search(departmentId, workDate, status, pageable);
        return PageResponse.from(page, q -> QueueTicketResponse.from(q, getRecordId(q), null));
    }

    @Transactional(readOnly = true)
    public QueueTicketResponse get(UUID id) {
        return QueueTicketResponse.from(findById(id));
    }

    public QueueTicketResponse create(QueueTicketCreateRequest req) {
        // Khoa Visit de moi invoice/callback cua cung luot dung chung mot workflow.
        CustomerVisit visit = visitRepo.findByIdForUpdate(req.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Lượt khám không tồn tại: " + req.visitId()));
        var existing = repo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(
                req.visitId(), req.serviceId());
        if (existing.isPresent()) return QueueTicketResponse.from(existing.get());
        Department dept = departmentRepo.findByIdForUpdate(req.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại: " + req.departmentId()));
        // Kiem tra lai sau khi khoa phong. Neu hai thanh toan den cung luc,
        // request thu hai phai tai su dung phieu request thu nhat vua tao.
        existing = repo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(
                req.visitId(), req.serviceId());
        if (existing.isPresent()) return QueueTicketResponse.from(existing.get());
        org.example.doansummer2026.model.MedicalService service = serviceRepo.findById(req.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + req.serviceId()));
        LocalDate workDate = req.workDate() != null ? req.workDate() : LocalDate.now(CLINIC_ZONE);
        Integer max = repo.findMaxQueueNumberForDay(req.departmentId(), workDate).orElse(0);
        QueueTicket q = QueueTicket.builder()
                .visit(visit)
                .department(dept)
                .service(service)
                .workDate(workDate)
                .queueNumber(max + 1)
                .status(QueueStatus.WAITING)
                .build();
        QueueTicket saved = repo.save(q);
        updateDepartmentStatus(dept.getDepartmentId());
        if (dept.getDepartmentType() == org.example.doansummer2026.enums.DepartmentType.EXAMINATION) {
            notifyDoctors(saved);
        }
        return QueueTicketResponse.from(saved);
    }
    
    private void notifyDoctors(QueueTicket q) {
        if (q.getDepartment() == null || q.getDepartment().getHeadDoctor() == null
                || q.getDepartment().getHeadDoctor().getProfile() == null) {
            return;
        }
        String patientName = q.getVisit() != null && q.getVisit().getAppointment() != null ? q.getVisit().getAppointment().getGuestFullName() : "Khach";
        if (q.getVisit() != null && q.getVisit().getCustomer() != null) {
            patientName = q.getVisit().getCustomer().getFullName();
        }
        String roomName = q.getDepartment() != null ? q.getDepartment().getName() : "";
        String content = String.format("Co benh nhan moi (Ten: %s) xep hang cho kham tai phong %s", patientName, roomName);
        
        StaffInfo doctor = q.getDepartment().getHeadDoctor();
        try {
            notificationService.create(new org.example.doansummer2026.dto.notification.NotificationCreateRequest(
                    doctor.getProfile().getProfileId(),
                    org.example.doansummer2026.enums.NotificationType.GENERAL,
                    org.example.doansummer2026.enums.NotificationChannel.IN_APP,
                    "Benh nhan moi",
                    content,
                    "QueueTicket",
                    q.getTicketId()
            ));
        } catch (Exception e) {
            // Thong bao khong lam anh huong viec tao hang cho.
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
        // Cho phep call tu WAITING, WAITING_FOR_TEST hoac TEST_DONE
        if (q.getStatus() != QueueStatus.WAITING && q.getStatus() != QueueStatus.CALLED && q.getStatus() != QueueStatus.WAITING_FOR_TEST && q.getStatus() != QueueStatus.TEST_DONE) {
            throw new BadRequestException("Chỉ có thể gọi phiếu đang chờ; trạng thái hiện tại: " + q.getStatus());
        }
        q.setStatus(QueueStatus.CALLED);
        q.setCalledAt(LocalDateTime.now());
        QueueTicket saved = repo.save(q);
        updateDepartmentStatus(q.getDepartment().getDepartmentId());
        return QueueTicketResponse.from(saved);
    }

    public QueueTicketResponse startExam(UUID id) {
        QueueTicket q = findByIdForUpdate(id);
        ensureCallableToday(q);
        ensureCurrentStaffCanOperate(q);
        if (q.getStatus() != QueueStatus.CALLED && q.getStatus() != QueueStatus.WAITING_FOR_TEST && q.getStatus() != QueueStatus.TEST_DONE) {
            throw new BadRequestException("Chỉ có thể bắt đầu với phiếu đã gọi hoặc đang chờ quay lại; trạng thái hiện tại: " + q.getStatus());
        }
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
            updateDepartmentStatus(q.getDepartment().getDepartmentId());
            return QueueTicketResponse.from(saved, null, getWaitingCount(q), null);
        }

        // Lay staffId tu SecurityContext (JWT token)
        UUID currentStaffId = getCurrentStaffId();
        if (currentStaffId == null) {
            throw new BadRequestException("Tài khoản hiện tại không phải bác sĩ");
        }
        boolean nurse = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_NURSE"));
        UUID doctorId = currentStaffId;
        if (nurse) {
            if (q.getDepartment().getHeadDoctor() == null)
                throw new BadRequestException("Phòng chưa có bác sĩ phụ trách; y tá không thể bắt đầu ca khám");
            doctorId = q.getDepartment().getHeadDoctor().getStaffId();
        }

        // Tu dong tao medical record neu chua co, hoac lay record cu
        UUID recordId = null;
        var medicalRecord = getMedicalRecordOrCreate(q, doctorId);
        if (medicalRecord != null) {
            recordId = medicalRecord.recordId();
        }

        q.setStatus(QueueStatus.IN_PROGRESS);
        QueueTicket saved = repo.save(q);
        updateDepartmentStatus(q.getDepartment().getDepartmentId());
        return QueueTicketResponse.from(saved, recordId, getWaitingCount(q), medicalRecord);
    }

    public MedicalRecordResponse completeAndReturnRecord(UUID id) {
        return completeAndReturnRecord(id, null);
    }

    public MedicalRecordResponse completeAndReturnRecord(UUID id, MedicalRecordUpdateRequest req) {
        QueueTicket q = findByIdForUpdate(id);
        if (q.getStatus() != QueueStatus.IN_PROGRESS) {
            throw new BadRequestException("Chỉ có thể đóng phiếu đang thực hiện; trạng thái hiện tại: " + q.getStatus());
        }

        // Complete medical record
        if (q.getVisit() == null) {
            throw new BadRequestException("Phiếu không có thông tin lượt khám");
        }
        var record = recordRepo.findByQueueTicket_TicketId(q.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có hồ sơ bệnh án cho lượt khám này"));

        UUID completingStaffId = getCurrentStaffId();
        boolean admin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        // Quyen ket thuc ca kham thuoc ve bac si phu trach phong da duoc cau hinh.
        // record.doctor chi la bac si da tao/ghi benh an, co the khac khi y ta ho tro
        // hoac bac si khac bat dau ca, nen khong dung lam dieu kien uu tien.
        UUID responsibleDoctorId = q.getDepartment() != null && q.getDepartment().getHeadDoctor() != null
                ? q.getDepartment().getHeadDoctor().getStaffId()
                : (record.getDoctor() != null ? record.getDoctor().getStaffId() : null);
        if (!admin && (completingStaffId == null || responsibleDoctorId == null
                || !responsibleDoctorId.equals(completingStaffId)))
            throw new BadRequestException("Chỉ bác sĩ phụ trách mới được hoàn thành ca khám");

        if (req != null && req.version() != null && !java.util.Objects.equals(req.version(), record.getVersion()))
            throw new ConflictException("Hồ sơ đã được nhân viên khác cập nhật. Vui lòng tải lại trước khi hoàn thành");

        // Cap nhat thong tin medical record neu co request (icd-10, prescription, vitals, ...)
        if (req != null) {
            updateMedicalRecordFields(record, req);
            record = recordRepo.save(record);
        }

        boolean hasTestRequests = req != null && req.testRequests() != null && !req.testRequests().isEmpty();
        boolean hasIncompletePrepaidTests = testRequestService.hasIncompleteRequestsForRecord(record.getRecordId());
        boolean shouldWaitForTests = hasTestRequests || hasIncompletePrepaidTests;
        if (!shouldWaitForTests && record.getStatus() != MedicalRecordStatus.COMPLETED) {
            
            // Validate unpaid invoices
            boolean hasUnpaidInvoices = invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()).stream()
                .anyMatch(inv -> inv.getStatus() == org.example.doansummer2026.enums.InvoiceStatus.PENDING);
            if (hasUnpaidInvoices) {
                throw new BadRequestException("Bệnh nhân chưa thanh toán hóa đơn cận lâm sàng/dịch vụ");
            }

            // Validate diagnosis/conclusion/icd10
            boolean hasDiagnosis = record.getDiagnosis() != null && !record.getDiagnosis().trim().isEmpty();
            boolean hasConclusion = record.getConclusion() != null && !record.getConclusion().trim().isEmpty();
            boolean hasIcd10 = record.getIcdSelections() != null && !record.getIcdSelections().isEmpty();
            
            if (!hasDiagnosis && !hasConclusion && !hasIcd10) {
                throw new BadRequestException("Vui lòng nhập chẩn đoán, kết luận hoặc chọn mã ICD-10 trước khi hoàn thành hồ sơ");
            }

            record.setStatus(MedicalRecordStatus.COMPLETED);
            record.setCompletedAt(LocalDateTime.now());
            StaffInfo confirmer = completingStaffId != null ? staffRepo.findById(completingStaffId).orElse(null) : null;
            record.setDoctorConfirmedBy(confirmer);
            record.setDoctorConfirmedAt(LocalDateTime.now());
            recordRepo.save(record);
        }

        boolean waitingForNewTestInvoicePayment = false;
        // Tao TestRequest neu co trong payload (gop voi API hoan thien de tranh goi 2 lan)
        if (hasTestRequests) {
            UUID doctorId = record.getDoctor() != null ? record.getDoctor().getStaffId() : null;
            if (doctorId == null) {
                throw new BadRequestException("Không thể tạo yêu cầu cận lâm sàng: không xác định được bác sĩ");
            }
            
            // Thay vi tao truc tiep TestRequest -> Tao Invoice (hoa don) truoc
            java.util.List<org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest> invoiceItems = new java.util.ArrayList<>();
            java.util.Set<UUID> selectedServiceIds = new java.util.HashSet<>();
            for (org.example.doansummer2026.dto.medicalRecord.TestRequestInExaminationRequest testReq : req.testRequests()) {
                if (!selectedServiceIds.add(testReq.serviceId())) {
                    throw new ConflictException("Dịch vụ này đang bị chọn trùng trong chỉ định.");
                }
                // Neu benh nhan da dat va thanh toan dich vu nay, gan yeu cau
                // hien co vao ho so kham thay vi tao trung va thu tien lan hai.
                if (testRequestService.attachPrepaidRequestToExamination(
                        q.getVisit().getVisitId(), record.getRecordId(), testReq.serviceId(),
                        doctorId, testReq.notes())) {
                    continue;
                }
                // TestRequest cua dich vu cu chi duoc tao sau khi hoa don PAID, do do
                // phai kiem tra truoc khi tao Invoice o day, khong chi o TestRequestService.create().
                testRequestService.ensureServiceNotAlreadyRequested(record.getRecordId(), testReq.serviceId());
                org.example.doansummer2026.model.MedicalService svc = serviceRepo.findById(testReq.serviceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + testReq.serviceId()));
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
        if (q.getStatus() == QueueStatus.DONE
                || (shouldWaitForTests && !waitingForNewTestInvoicePayment)) {
            patientJourneyService.activateNext(q.getVisit().getVisitId());
        }
        updateDepartmentStatus(q.getDepartment().getDepartmentId());

        return MedicalRecordResponse.from(record, true);
    }

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

    /** Ket thuc thao tac tai phong can lam sang; ket qua van co the dang duoc xu ly/ky. */
    public QueueTicketResponse finishParaclinicalQueue(UUID id) {
        QueueTicket q = findByIdForUpdate(id);
        ensureCurrentStaffCanOperate(q);
        if (q.getDepartment().getDepartmentType() == null
                || !q.getDepartment().getDepartmentType().isParaclinical()) {
            throw new BadRequestException("Thao tác này chỉ áp dụng cho phòng cận lâm sàng");
        }
        if (q.getStatus() != QueueStatus.IN_PROGRESS) {
            throw new BadRequestException("Chỉ có thể kết thúc khi bệnh nhân đang được thực hiện tại phòng");
        }
        q.setStatus(QueueStatus.DONE);
        q.setCompletedAt(LocalDateTime.now());
        QueueTicket saved = repo.save(q);
        if (q.getVisit() != null) patientJourneyService.activateNext(q.getVisit().getVisitId());
        updateDepartmentStatus(q.getDepartment().getDepartmentId());
        return QueueTicketResponse.from(saved);
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
        updateDepartmentStatus(q.getDepartment().getDepartmentId());
        return QueueTicketResponse.from(saved);
    }

    public QueueTicketResponse returnToQueue(UUID id) {
        QueueTicket q = findByIdForUpdate(id);
        ensureCurrentStaffCanOperate(q);
        if (q.getStatus() != QueueStatus.SKIPPED) {
            throw new BadRequestException("Chỉ có thể đưa phiếu vắng quay lại hàng chờ");
        }
        if (q.getWorkDate() == null || !q.getWorkDate().equals(LocalDate.now(CLINIC_ZONE))) {
            throw new BadRequestException("Chỉ có thể đưa bệnh nhân quay lại hàng chờ trong ngày của phiếu");
        }
        boolean mustWaitForCurrentStep = q.getVisit() != null
                && patientJourneyService.hasActiveStep(q.getVisit().getVisitId());
        q.setStatus(mustWaitForCurrentStep ? QueueStatus.BLOCKED : QueueStatus.WAITING);
        q.setCalledAt(null);
        q.setCompletedAt(null);
        testRequestService.restoreRequestsForQueue(q.getTicketId(), mustWaitForCurrentStep);
        QueueTicket saved = repo.save(q);
        updateDepartmentStatus(q.getDepartment().getDepartmentId());
        return QueueTicketResponse.from(saved);
    }

    private void ensureCallableToday(QueueTicket queue) {
        if (queue.getWorkDate() == null || !LocalDate.now(CLINIC_ZONE).equals(queue.getWorkDate())) {
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
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (admin) return;

        UUID staffId = getCurrentStaffId();
        if (staffId == null || ticket.getDepartment() == null) {
            throw new BadRequestException("Không xác định được nhân viên hoặc phòng thực hiện");
        }
        Department department = ticket.getDepartment();
        boolean headDoctor = department.getHeadDoctor() != null
                && staffId.equals(department.getHeadDoctor().getStaffId());
        boolean assignedNurse = department.getNurses() != null && department.getNurses().stream()
                .anyMatch(nurse -> staffId.equals(nurse.getStaffId()));
        if (!headDoctor && !assignedNurse) {
            throw new BadRequestException("Bạn không được phân công thực hiện tại phòng này");
        }
    }

    @Transactional(readOnly = true)
    public QueueTicketResponse getInprogressByDepartment(UUID departmentId) {
        QueueTicket ticket = repo.findTopByDepartment_DepartmentIdAndStatusOrderByCreatedAtAsc(departmentId, QueueStatus.IN_PROGRESS)
                .orElse(null);
        if (ticket == null) return null;
        var medicalRecord = getMedicalRecordByQueueTicket(ticket.getTicketId());
        UUID recordId = medicalRecord != null ? medicalRecord.recordId() : null;
        return QueueTicketResponse.from(ticket, recordId, null, medicalRecord);
    }

    /**
     * Lay danh sach cho (WAITING/CALLED/WAITING_FOR_TEST/TEST_DONE) theo khoa, uu tien TEST_DONE/WAITING_FOR_TEST.
     */
    @Transactional(readOnly = true)
    public PageResponse<QueueTicketResponse> getWaitingByDepartment(UUID departmentId, LocalDate workDate, QueueStatus status, Pageable pageable) {
        Page<QueueTicket> page;
        if (status != null) {
            if (workDate != null) {
                page = repo.findByDepartment_DepartmentIdAndWorkDateAndStatus(departmentId, workDate, status, pageable);
            } else {
                page = repo.findByDepartment_DepartmentIdAndStatus(departmentId, status, pageable);
            }
        } else {
            // Uu tien: TEST_DONE, WAITING_FOR_TEST, WAITING, CALLED
            List<QueueStatus> waitingStatuses = List.of(QueueStatus.TEST_DONE, QueueStatus.WAITING_FOR_TEST, QueueStatus.WAITING, QueueStatus.CALLED);
            if (workDate != null) {
                page = repo.findWaitingPrioritized(departmentId, workDate, waitingStatuses, pageable);
            } else {
                page = repo.findByDepartment_DepartmentIdAndStatusIn(departmentId, waitingStatuses, pageable);
            }
        }
        return PageResponse.from(page, q -> QueueTicketResponse.from(q, getRecordId(q), null));
    }

    @Transactional(readOnly = true)
    public PageResponse<QueueTicketResponse> getAllInprogress(Pageable pageable) {
        Page<QueueTicket> page = repo.findAllByStatus(QueueStatus.IN_PROGRESS, pageable);
        return PageResponse.from(page, q -> QueueTicketResponse.from(q, getRecordId(q), getWaitingCount(q)));
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
        var visitId = q.getVisit().getVisitId();
        var existingRecord = recordRepo.findByQueueTicket_TicketId(q.getTicketId()).orElse(null);
        MedicalRecord record;
        if (existingRecord == null) {
            StaffInfo doctor = staffRepo.findById(doctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bác sĩ không tồn tại: " + doctorId));
            // Neu benh nhan da mua CLS kem dich vu kham, TestRequest da nam trong
            // record tam cua visit. Tai su dung record nay lam record kham chinh
            // de ket qua CLS tu dong quay lai dung phong kham, khong tao hai benh an.
            record = recordRepo.findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(visitId)
                    .orElse(null);
            if (record == null) {
                record = MedicalRecord.builder()
                        .visit(q.getVisit())
                        .queueTicket(q)
                        .doctor(doctor)
                        .status(MedicalRecordStatus.IN_PROGRESS)
                        .build();
            } else {
                record.setQueueTicket(q);
                record.setDoctor(doctor);
                record.setStatus(MedicalRecordStatus.IN_PROGRESS);
                if ("Dich vu can lam sang".equals(record.getChiefComplaint())) {
                    record.setChiefComplaint(null);
                }
            }
            record = recordRepo.save(record);
        } else {
            record = existingRecord;
        }
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
        List<org.example.doansummer2026.dto.testRequest.TestRequestResponse> requests =
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
        return QueueTicketResponse.from(q);
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
        try {
            messagingTemplate.convertAndSend("/topic/department-" + departmentId + "-queue", "QUEUE_UPDATED");
        } catch (Exception e) {
            // Ignore messaging errors so it doesn't break the transaction
        }
    }
}
