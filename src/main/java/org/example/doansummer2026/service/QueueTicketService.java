package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.queueTicket.QueueTicketCreateRequest;
import org.example.doansummer2026.dto.queueTicket.QueueTicketResponse;
import org.example.doansummer2026.dto.queueTicket.QueueTicketUpdateRequest;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.QueueTicketServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class QueueTicketService implements QueueTicketServiceInterface {

    private final QueueTicketRepository repo;
    private final CustomerVisitRepository visitRepo;
    private final DepartmentRepository departmentRepo;
    private final MedicalServiceRepository serviceRepo;

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
}
