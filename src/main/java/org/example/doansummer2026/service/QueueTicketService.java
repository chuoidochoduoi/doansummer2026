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

    private final QueueTicketRepository repo;
    private final CustomerVisitRepository visitRepo;
    private final DepartmentRepository departmentRepo;
    private final MedicalServiceRepository serviceRepo;
    private final MedicalRecordRepository recordRepo;
    private final StaffInfoRepository staffRepo;
    private final Icd10CodeRepository icd10Repo;
    private final TestRequestService testRequestService;
    private final PatientJourneyService patientJourneyService;

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
        CustomerVisit visit = visitRepo.findById(req.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Luot kham khong ton tai: " + req.visitId()));
        var existing = repo.findByVisit_VisitIdAndService_ServiceId(req.visitId(), req.serviceId());
        if (existing.isPresent()) return QueueTicketResponse.from(existing.get());
        Department dept = departmentRepo.findByIdForUpdate(req.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Khoa khong ton tai: " + req.departmentId()));
        org.example.doansummer2026.model.MedicalService service = serviceRepo.findById(req.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + req.serviceId()));
        LocalDate workDate = req.workDate() != null ? req.workDate() : LocalDate.now();
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
        return QueueTicketResponse.from(saved);
    }

    public QueueTicketResponse update(UUID id, QueueTicketUpdateRequest req) {
        QueueTicket q = findById(id);
        if (req.status() != null) {
            if (req.status() == QueueStatus.IN_PROGRESS) {
                if (q.getStatus() != QueueStatus.CALLED && q.getStatus() != QueueStatus.WAITING_FOR_TEST && q.getStatus() != QueueStatus.TEST_DONE) {
                    throw new BadRequestException("Chi chuyen sang IN_PROGRESS tu CALLED, WAITING_FOR_TEST hoac TEST_DONE, hien tai: " + q.getStatus());
                }
                long inprogressCount = repo.countInprogressByDepartment(q.getDepartment().getDepartmentId());
                if (inprogressCount >= 1) {
                    throw new BadRequestException("Phong da co benh nhan dang kham, chi duoc 1 benh nhan/phong");
                }
            }
            q.setStatus(req.status());
            if (req.status() == QueueStatus.CALLED && q.getCalledAt() == null) {
                q.setCalledAt(LocalDateTime.now());
            }
            if (req.status() == QueueStatus.DONE && q.getCompletedAt() == null) {
                q.setCompletedAt(LocalDateTime.now());
            }
        }
        QueueTicket saved = repo.save(q);
        updateDepartmentStatus(q.getDepartment().getDepartmentId());
        return QueueTicketResponse.from(saved);
    }

    public QueueTicketResponse call(UUID id) {
        QueueTicket q = findById(id);
        // Cho phep call tu WAITING, WAITING_FOR_TEST hoac TEST_DONE
        if (q.getStatus() != QueueStatus.WAITING && q.getStatus() != QueueStatus.CALLED && q.getStatus() != QueueStatus.WAITING_FOR_TEST && q.getStatus() != QueueStatus.TEST_DONE) {
            throw new BadRequestException("Chi goi duoc phieu dang cho (WAITING, WAITING_FOR_TEST hoac TEST_DONE), hien tai: " + q.getStatus());
        }
        q.setStatus(QueueStatus.CALLED);
        q.setCalledAt(LocalDateTime.now());
        QueueTicket saved = repo.save(q);
        updateDepartmentStatus(q.getDepartment().getDepartmentId());
        return QueueTicketResponse.from(saved);
    }

    public QueueTicketResponse startExam(UUID id) {
        QueueTicket q = findById(id);
        if (q.getStatus() != QueueStatus.CALLED && q.getStatus() != QueueStatus.WAITING_FOR_TEST && q.getStatus() != QueueStatus.TEST_DONE) {
            throw new BadRequestException("Chi bat dau kham duoc phieu dang CALL, RETURN (WAITING_FOR_TEST) hoac TEST_DONE, hien tai: " + q.getStatus());
        }
        long inprogressCount = repo.countInprogressByDepartment(q.getDepartment().getDepartmentId());
        if (inprogressCount >= 1) {
            throw new BadRequestException("Phong da co benh nhan dang kham, chi duoc 1 benh nhan/phong");
        }

        // Lay staffId tu SecurityContext (JWT token)
        UUID currentStaffId = getCurrentStaffId();
        if (currentStaffId == null) {
            throw new BadRequestException("Tai khoan khong phai bac si");
        }
        boolean nurse = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_NURSE"));
        UUID doctorId = currentStaffId;
        if (nurse) {
            if (q.getDepartment().getHeadDoctor() == null)
                throw new BadRequestException("Phong chua co bac si phu trach; y ta khong the bat dau ca kham");
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
        QueueTicket q = findById(id);
        if (q.getStatus() != QueueStatus.IN_PROGRESS) {
            throw new BadRequestException("Chi dong phieu dang kham (IN_PROGRESS), hien tai: " + q.getStatus());
        }

        // Complete medical record
        if (q.getVisit() == null) {
            throw new BadRequestException("Phieu khong co thong tin visit");
        }
        var record = recordRepo.findByQueueTicket_TicketId(q.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Chua co ho so benh an cho visit nay"));

        UUID completingStaffId = getCurrentStaffId();
        boolean admin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        if (!admin && (completingStaffId == null || record.getDoctor() == null
                || !record.getDoctor().getStaffId().equals(completingStaffId)))
            throw new BadRequestException("Chi bac si phu trach moi duoc hoan thanh ca kham");

        if (req != null && req.version() != null && !java.util.Objects.equals(req.version(), record.getVersion()))
            throw new ConflictException("Ho so da duoc nhan vien khac cap nhat. Vui long tai lai truoc khi hoan thanh");

        // Cap nhat thong tin medical record neu co request (icd-10, prescription, vitals, ...)
        if (req != null) {
            updateMedicalRecordFields(record, req);
            record = recordRepo.save(record);
        }

        boolean hasTestRequests = req != null && req.testRequests() != null && !req.testRequests().isEmpty();
        if (!hasTestRequests && record.getStatus() != MedicalRecordStatus.COMPLETED) {
            record.setStatus(MedicalRecordStatus.COMPLETED);
            record.setCompletedAt(LocalDateTime.now());
            StaffInfo confirmer = completingStaffId != null ? staffRepo.findById(completingStaffId).orElse(null) : null;
            record.setDoctorConfirmedBy(confirmer);
            record.setDoctorConfirmedAt(LocalDateTime.now());
            recordRepo.save(record);
        }

        // Tao TestRequest neu co trong payload (gop voi API hoan thien de tranh goi 2 lan)
        if (hasTestRequests) {
            UUID doctorId = record.getDoctor() != null ? record.getDoctor().getStaffId() : null;
            if (doctorId == null) {
                throw new BadRequestException("Khong the tao test request: khong xac dinh duoc bac si");
            }
            
            // Thay vi tao truc tiep TestRequest -> Tao Invoice (hoa don) truoc
            java.util.List<org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest> invoiceItems = new java.util.ArrayList<>();
            for (org.example.doansummer2026.dto.medicalRecord.TestRequestInExaminationRequest testReq : req.testRequests()) {
                org.example.doansummer2026.model.MedicalService svc = serviceRepo.findById(testReq.serviceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + testReq.serviceId()));
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
                invoiceService.create(new org.example.doansummer2026.dto.invoice.InvoiceCreateRequest(
                        q.getVisit().getCustomer() != null ? q.getVisit().getCustomer().getProfileId() : null,
                        q.getVisit().getVisitId(),
                        record.getRecordId(),
                        LocalDate.now(),
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
        if (hasTestRequests) {
            q.setStatus(QueueStatus.WAITING_FOR_TEST);
            q.setCalledAt(null);
        } else {
            q.setStatus(QueueStatus.DONE);
            q.setCompletedAt(LocalDateTime.now());
        }
        repo.save(q);
        if (q.getStatus() == QueueStatus.DONE) patientJourneyService.activateNext(q.getVisit().getVisitId());
        updateDepartmentStatus(q.getDepartment().getDepartmentId());

        return MedicalRecordResponse.from(record, true);
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
        QueueTicket q = findById(id);
        q.setStatus(QueueStatus.DONE);
        q.setCompletedAt(LocalDateTime.now());
        QueueTicket saved = repo.save(q);
        if (q.getVisit() != null) patientJourneyService.activateNext(q.getVisit().getVisitId());
        updateDepartmentStatus(q.getDepartment().getDepartmentId());
        return QueueTicketResponse.from(saved);
    }

    public QueueTicketResponse skip(UUID id) {
        QueueTicket q = findById(id);
        q.setStatus(QueueStatus.SKIPPED);
        QueueTicket saved = repo.save(q);
        updateDepartmentStatus(q.getDepartment().getDepartmentId());
        return QueueTicketResponse.from(saved);
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Phieu xep hang khong ton tai: " + id);
        }
        QueueTicket q = findById(id);
        UUID deptId = q.getDepartment() != null ? q.getDepartment().getDepartmentId() : null;
        repo.deleteById(id);
        if (deptId != null) {
            updateDepartmentStatus(deptId);
        }
    }

    public QueueTicket findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phieu xep hang khong ton tai: " + id));
    }

    private UUID getCurrentStaffId() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Map<?, ?> map) {
            String sid = (String) map.get("staffId");
            return sid != null ? UUID.fromString(sid) : null;
        }
        return null;
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
                    .orElseThrow(() -> new ResourceNotFoundException("Bac si khong ton tai: " + doctorId));
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
        QueueTicket q = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Queue ticket khong ton tai: " + id));
        if (q.getStatus() != QueueStatus.WAITING_FOR_TEST) {
            throw new BadRequestException("Chi co the danh dau TEST_DONE tu trang thai WAITING_FOR_TEST, hien tai: " + q.getStatus());
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
    }
}
