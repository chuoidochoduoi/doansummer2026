package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.queueTicket.QueueTicketCreateRequest;
import org.example.doansummer2026.dto.queueTicket.QueueTicketResponse;
import org.example.doansummer2026.dto.queueTicket.QueueTicketUpdateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.QueueTicketServiceInterface;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public PageResponse<QueueTicketResponse> search(UUID departmentId, LocalDate workDate,
                                                     QueueStatus status, Pageable pageable) {
        Page<QueueTicket> page = repo.search(departmentId, workDate, status, pageable);
        return PageResponse.from(page, QueueTicketResponse::from);
    }

    @Transactional(readOnly = true)
    public QueueTicketResponse get(UUID id) {
        return QueueTicketResponse.from(findById(id));
    }

    public QueueTicketResponse create(QueueTicketCreateRequest req) {
        CustomerVisit visit = visitRepo.findById(req.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Luot kham khong ton tai: " + req.visitId()));
        Department dept = departmentRepo.findById(req.departmentId())
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
        return QueueTicketResponse.from(repo.save(q));
    }

    public QueueTicketResponse update(UUID id, QueueTicketUpdateRequest req) {
        QueueTicket q = findById(id);
        if (req.status() != null) {
            if (req.status() == QueueStatus.IN_PROGRESS) {
                if (q.getStatus() != QueueStatus.CALLED) {
                    throw new BadRequestException("Chi chuyen sang IN_PROGRESS tu CALLED, hien tai: " + q.getStatus());
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
        return QueueTicketResponse.from(repo.save(q));
    }

    public QueueTicketResponse call(UUID id) {
        QueueTicket q = findById(id);
        if (q.getStatus() != QueueStatus.WAITING) {
            throw new BadRequestException("Chi goi duoc phieu dang cho (WAITING), hien tai: " + q.getStatus());
        }
        q.setStatus(QueueStatus.CALLED);
        q.setCalledAt(LocalDateTime.now());
        return QueueTicketResponse.from(repo.save(q));
    }

    public QueueTicketResponse startExam(UUID id) {
        QueueTicket q = findById(id);
        if (q.getStatus() != QueueStatus.CALLED) {
            throw new BadRequestException("Chi bat dau kham duoc phieu dang CALL (CALLED), hien tai: " + q.getStatus());
        }
        long inprogressCount = repo.countInprogressByDepartment(q.getDepartment().getDepartmentId());
        if (inprogressCount >= 1) {
            throw new BadRequestException("Phong da co benh nhan dang kham, chi duoc 1 benh nhan/phong");
        }

        // Lay staffId tu SecurityContext (JWT token)
        UUID doctorId = getCurrentStaffId();
        if (doctorId == null) {
            throw new BadRequestException("Tai khoan khong phai bac si");
        }

        // Tu dong tao medical record neu chua co, hoac lay record cu
        UUID recordId = null;
        var medicalRecord = getMedicalRecordOrCreate(q, doctorId);
        if (medicalRecord != null) {
            recordId = medicalRecord.recordId();
        }

        q.setStatus(QueueStatus.IN_PROGRESS);
        return QueueTicketResponse.from(repo.save(q), recordId, getWaitingCount(q), medicalRecord);
    }

    public MedicalRecordResponse completeAndReturnRecord(UUID id) {
        QueueTicket q = findById(id);
        if (q.getStatus() != QueueStatus.IN_PROGRESS) {
            throw new BadRequestException("Chi dong phieu dang kham (IN_PROGRESS), hien tai: " + q.getStatus());
        }
        q.setStatus(QueueStatus.DONE);
        q.setCompletedAt(LocalDateTime.now());
        repo.save(q);

        // Complete medical record
        if (q.getVisit() == null) {
            throw new BadRequestException("Phieu khong co thong tin visit");
        }
        var record = recordRepo.findByVisit_VisitId(q.getVisit().getVisitId())
                .orElseThrow(() -> new ResourceNotFoundException("Chua co ho so benh an cho visit nay"));
        if (record.getStatus() != MedicalRecordStatus.COMPLETED) {
            record.setStatus(MedicalRecordStatus.COMPLETED);
            record.setCompletedAt(LocalDateTime.now());
            recordRepo.save(record);
        }
        var fetched = recordRepo.findByVisit_VisitIdWithVitalSigns(q.getVisit().getVisitId()).orElse(record);
        return MedicalRecordResponse.from(fetched, true);
    }

    public QueueTicketResponse complete(UUID id) {
        QueueTicket q = findById(id);
        q.setStatus(QueueStatus.DONE);
        q.setCompletedAt(LocalDateTime.now());
        return QueueTicketResponse.from(repo.save(q));
    }

    public QueueTicketResponse skip(UUID id) {
        QueueTicket q = findById(id);
        q.setStatus(QueueStatus.SKIPPED);
        return QueueTicketResponse.from(repo.save(q));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Phieu xep hang khong ton tai: " + id);
        }
        repo.deleteById(id);
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
        var medicalRecord = getMedicalRecord(ticket.getVisit() != null ? ticket.getVisit().getVisitId() : null);
        UUID recordId = medicalRecord != null ? medicalRecord.recordId() : null;
        return QueueTicketResponse.from(ticket, recordId, null, medicalRecord);
    }

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
            List<QueueStatus> waitingStatuses = List.of(QueueStatus.WAITING, QueueStatus.CALLED);
            if (workDate != null) {
                page = repo.findByDepartment_DepartmentIdAndWorkDateAndStatusIn(departmentId, workDate, waitingStatuses, pageable);
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
        var record = recordRepo.findByVisit_VisitIdWithVitalSigns(visitId).orElse(null);
        if (record == null) return null;
        return MedicalRecordResponse.from(record, false);
    }

    private MedicalRecordResponse getMedicalRecordOrCreate(QueueTicket q, UUID doctorId) {
        if (q.getVisit() == null) return null;
        var visitId = q.getVisit().getVisitId();
        var existingRecord = recordRepo.findByVisit_VisitId(visitId).orElse(null);
        MedicalRecord record;
        if (existingRecord == null) {
            StaffInfo doctor = staffRepo.findById(doctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bac si khong ton tai: " + doctorId));
            record = MedicalRecord.builder()
                    .visit(q.getVisit())
                    .doctor(doctor)
                    .status(MedicalRecordStatus.IN_PROGRESS)
                    .build();
            record = recordRepo.save(record);
        } else {
            record = existingRecord;
        }
        // Fetch again with vital signs for response
        var fetchedRecord = recordRepo.findByVisit_VisitIdWithVitalSigns(visitId).orElse(record);
        return MedicalRecordResponse.from(fetchedRecord, false);
    }

    private UUID getRecordId(QueueTicket q) {
        UUID visitId = q.getVisit() != null ? q.getVisit().getVisitId() : null;
        return visitId != null ? recordRepo.findByVisit_VisitId(visitId).map(r -> r.getRecordId()).orElse(null) : null;
    }

    private Integer getWaitingCount(QueueTicket q) {
        UUID deptId = q.getDepartment() != null ? q.getDepartment().getDepartmentId() : null;
        if (deptId == null) return null;
        return (int) repo.countWaitingByDepartment(deptId);
    }
}