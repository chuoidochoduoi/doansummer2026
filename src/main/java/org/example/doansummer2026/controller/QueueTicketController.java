package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.queueTicket.QueueTicketCreateRequest;
import org.example.doansummer2026.dto.queueTicket.QueueTicketResponse;
import org.example.doansummer2026.dto.queueTicket.QueueTicketUpdateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;
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
import org.example.doansummer2026.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/v1/queue-tickets")
@RequiredArgsConstructor
public class QueueTicketController {

    private final QueueTicketService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> list(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam(required = false) QueueStatus status,
            Pageable pageable) {
        return RestResponses.ok(service.search(departmentId, workDate, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','RECEPTIONIST','ADMIN')")
    public ResponseEntity<QueueTicketResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<QueueTicketResponse> create(@Valid @RequestBody QueueTicketCreateRequest req) {
        QueueTicketResponse created = service.create(req);
        return RestResponses.created("/api/v1/queue-tickets/{id}", created.ticketId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','RECEPTIONIST','ADMIN')")
    public ResponseEntity<QueueTicketResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody QueueTicketUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/{id}/call")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','RECEPTIONIST','ADMIN')")
    public ResponseEntity<QueueTicketResponse> call(@PathVariable UUID id) {
        return RestResponses.ok(service.call(id));
    }

    @PostMapping("/{id}/start-exam")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','RECEPTIONIST','ADMIN')")
    public ResponseEntity<QueueTicketResponse> startExam(@PathVariable UUID id) {
        return RestResponses.ok(service.startExam(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','RECEPTIONIST','ADMIN')")
    public ResponseEntity<MedicalRecordResponse> complete(@PathVariable UUID id) {
        return RestResponses.ok(service.completeAndReturnRecord(id));
    }

    @PostMapping("/{id}/skip")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','RECEPTIONIST','ADMIN')")
    public ResponseEntity<QueueTicketResponse> skip(@PathVariable UUID id) {
        return RestResponses.ok(service.skip(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    /**
     * API dong bo trang kham benh - lay thong tin benh nhan tu queue ticket dang IN_PROGRESS.
     * - Chi co 1 benh nhan dang kham moi phong (da kiem tra).
     * - Tra ve 404 neu phong trong.
     */
    @GetMapping("/in-progress/{departmentId}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public ResponseEntity<QueueTicketResponse> getInprogress(@PathVariable UUID departmentId) {
        QueueTicketResponse result = service.getInprogressByDepartment(departmentId);
        if (result == null) {
            throw new ResourceNotFoundException("Phong chua co benh nhan dang kham");
        }
        return RestResponses.ok(result);
    }

    /**
     * API cho phong kham da khoa - lay danh sach benh nhan dang kham.
     * - Moi phong chi co 1 benh nhan dang kham.
     * - Frontend cho bac si chon phong/de lua chon benh nhan.
     */
    @GetMapping("/in-progress")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> getAllInprogress(Pageable pageable) {
        return RestResponses.ok(service.getAllInprogress(pageable));
    }

    /**
     * API lay hang cho cua phong - hien thi danh sach benh nhan dang cho (WAITING/CALLED).
     * Dung cho bac si xem so luong va don tiep benh nhan.
     * Co the filter theo ngay va status (neu status null lay ca WAITING/CALLED).
     */
    @GetMapping("/waiting/{departmentId}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> getWaiting(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) LocalDate workDate,
            @RequestParam(required = false) QueueStatus status,
            Pageable pageable) {
        return RestResponses.ok(service.getWaitingByDepartment(departmentId, workDate, status, pageable));
    }
}
