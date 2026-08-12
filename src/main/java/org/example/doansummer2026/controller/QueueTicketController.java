package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.queueTicket.QueueTicketCreateRequest;
import org.example.doansummer2026.dto.queueTicket.QueueTicketResponse;
import org.example.doansummer2026.dto.queueTicket.QueueTicketUpdateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.service.QueueTicketService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.enums.AuditAction;

@RestController
@RequiredArgsConstructor
public class QueueTicketController {

    private final QueueTicketService service;

    // --- MAIN ENDPOINTS ---

    @GetMapping("/api/v1/queue-tickets")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> list(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam(required = false) QueueStatus status,
            Pageable pageable) {
        return RestResponses.ok(service.search(departmentId, workDate, status, pageable));
    }

    @GetMapping("/api/v1/queue-tickets/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_ADMIN')")
    public ResponseEntity<QueueTicketResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping("/api/v1/queue-tickets")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN')")
    public ResponseEntity<QueueTicketResponse> create(@Valid @RequestBody QueueTicketCreateRequest req) {
        QueueTicketResponse created = service.create(req);
        return RestResponses.created("/api/v1/queue-tickets/{id}", created.ticketId(), created);
    }

    @PutMapping("/api/v1/queue-tickets/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN')")
    public ResponseEntity<QueueTicketResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody QueueTicketUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/api/v1/queue-tickets/{id}/call")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    @Auditable(action = AuditAction.PATIENT_CALLED, entityName = "QueueTicket", idParamName = "id", description = "Gọi bệnh nhân vào phòng")
    public ResponseEntity<QueueTicketResponse> call(@PathVariable UUID id) {
        return RestResponses.ok(service.call(id));
    }

    @PostMapping("/api/v1/queue-tickets/{id}/start-exam")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    @Auditable(action = AuditAction.EXAM_STARTED, entityName = "QueueTicket", idParamName = "id", description = "Bắt đầu xử lý bệnh nhân")
    public ResponseEntity<QueueTicketResponse> startExam(@PathVariable UUID id) {
        return RestResponses.ok(service.startExam(id));
    }

    @PostMapping("/api/v1/queue-tickets/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_ADMIN')")
    @Auditable(action = AuditAction.RECORD_COMPLETED, entityName = "QueueTicket", idParamName = "id", description = "Hoàn thành ca khám")
    public ResponseEntity<MedicalRecordResponse> complete(@PathVariable UUID id,
                                                         @RequestBody(required = false) MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.completeAndReturnRecord(id, req));
    }

    @PostMapping("/api/v1/queue-tickets/{id}/finish-service")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "QueueTicket", idParamName = "id", description = "Hoan thanh thao tac tai phong can lam sang")
    public ResponseEntity<QueueTicketResponse> finishParaclinicalService(@PathVariable UUID id) {
        return RestResponses.ok(service.finishParaclinicalQueue(id));
    }

    @PostMapping("/api/v1/queue-tickets/{id}/skip")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_ADMIN')")
    @Auditable(action = AuditAction.QUEUE_SKIPPED, entityName = "QueueTicket", idParamName = "id", description = "Bỏ lượt bệnh nhân")
    public ResponseEntity<QueueTicketResponse> skip(@PathVariable UUID id) {
        return RestResponses.ok(service.skip(id));
    }

    @PostMapping("/api/v1/queue-tickets/{id}/return")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_ADMIN')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "QueueTicket", idParamName = "id", description = "Dua benh nhan vang quay lai hang cho")
    public ResponseEntity<QueueTicketResponse> returnToQueue(@PathVariable UUID id) {
        return RestResponses.ok(service.returnToQueue(id));
    }

    @DeleteMapping("/api/v1/queue-tickets/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    /**
     * API dong bo trang kham benh - lay thong tin benh nhan tu queue ticket dang IN_PROGRESS.
     * - Chi co 1 benh nhan dang kham moi phong (da kiem tra).
     * - Phòng trống là trạng thái bình thường, trả về 204 thay vì 404.
     */
    @GetMapping("/api/v1/queue-tickets/in-progress/{departmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public ResponseEntity<QueueTicketResponse> getInprogress(@PathVariable UUID departmentId) {
        QueueTicketResponse result = service.getInprogressByDepartment(departmentId);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return RestResponses.ok(result);
    }

    /**
     * API cho phong kham da khoa - lay danh sach benh nhan dang kham.
     * - Moi phong chi co 1 benh nhan dang kham.
     * - Frontend cho bac si chon phong/de lua chon benh nhan.
     */
    @GetMapping("/api/v1/queue-tickets/in-progress")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> getAllInprogress(Pageable pageable) {
        return RestResponses.ok(service.getAllInprogress(pageable));
    }

    /**
     * API lay hang cho cua phong - hien thi danh sach benh nhan dang cho (WAITING/CALLED/TEST_DONE/WAITING_FOR_TEST).
     * Dung cho bac si xem so luong va don tiep benh nhan.
     * Benh nhan TEST_DONE co the duoc goi vao kham truc tiep (chi can click "call").
     * Co the filter theo ngay va status (neu status null lay ca 4 status tren).
     */
    @GetMapping("/api/v1/queue-tickets/waiting/{departmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> getWaiting(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) LocalDate workDate,
            @RequestParam(required = false) QueueStatus status,
            Pageable pageable) {
        return RestResponses.ok(service.getWaitingByDepartment(departmentId, workDate, status, pageable));
    }

    /**
     * API lay danh sach benh nhan cho xet nghiem.
     */
    @GetMapping("/api/v1/queue-tickets/waiting-for-test/{departmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> getWaitingForTest(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) LocalDate workDate,
            Pageable pageable) {
        return RestResponses.ok(service.getWaitingByDepartment(departmentId, workDate, QueueStatus.WAITING_FOR_TEST, pageable));
    }

    /**
     * API lay danh sach benh nhan da hoan thanh xet nghiem (TEST_DONE).
     */
    @GetMapping("/api/v1/queue-tickets/test-done/{departmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> getTestDone(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) LocalDate workDate,
            Pageable pageable) {
        return RestResponses.ok(service.getWaitingByDepartment(departmentId, workDate, QueueStatus.TEST_DONE, pageable));
    }

    /**
     * API danh dau queue ticket da hoan thanh xet nghiem (WAITING_FOR_TEST -> TEST_DONE).
     */
    @PostMapping("/api/v1/queue-tickets/{id}/mark-test-done")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public ResponseEntity<QueueTicketResponse> markTestDone(@PathVariable UUID id) {
        return RestResponses.ok(service.markTestDone(id));
    }

    // --- LEGACY QUEUE ENDPOINTS ---

    /**
     * API endpoint cho frontend hook useQueueList.
     */
    @GetMapping("/api/queue")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> getQueue(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            Pageable pageable) {
        // Map frontend status 'all' to null (get all)
        QueueStatus queueStatus = null;
        if (status != null && !"all".equals(status)) {
            try {
                queueStatus = QueueStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {}
        }
        // For now, ignore search/sort - can be enhanced later
        return RestResponses.ok(service.search(departmentId, null, queueStatus, pageable));
    }

    @PutMapping("/api/v1/queue/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_ADMIN')")
    public ResponseEntity<QueueTicketResponse> updateQueue(
            @PathVariable UUID id,
            @RequestBody QueueTicketUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }
}




