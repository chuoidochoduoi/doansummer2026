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
import org.example.doansummer2026.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/v1/queue-tickets")
@RequiredArgsConstructor
public class QueueTicketController {

    private final QueueTicketService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> list(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam(required = false) QueueStatus status,
            Pageable pageable) {
        return RestResponses.ok(service.search(departmentId, workDate, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<QueueTicketResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN')")
    public ResponseEntity<QueueTicketResponse> create(@Valid @RequestBody QueueTicketCreateRequest req) {
        QueueTicketResponse created = service.create(req);
        return RestResponses.created("/api/v1/queue-tickets/{id}", created.ticketId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN')")
    public ResponseEntity<QueueTicketResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody QueueTicketUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/{id}/call")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<QueueTicketResponse> call(@PathVariable UUID id) {
        return RestResponses.ok(service.call(id));
    }

    @PostMapping("/{id}/start-exam")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<QueueTicketResponse> startExam(@PathVariable UUID id) {
        return RestResponses.ok(service.startExam(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<MedicalRecordResponse> complete(@PathVariable UUID id,
                                                         @RequestBody(required = false) MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.completeAndReturnRecord(id, req));
    }

    @PostMapping("/{id}/skip")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<QueueTicketResponse> skip(@PathVariable UUID id) {
        return RestResponses.ok(service.skip(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> getAllInprogress(Pageable pageable) {
        return RestResponses.ok(service.getAllInprogress(pageable));
    }

    /**
     * API lay hang cho cua phong - hien thi danh sach benh nhan dang cho (WAITING/CALLED/TEST_DONE/WAITING_FOR_TEST).
     * Dung cho bac si xem so luong va don tiep benh nhan.
     * Benh nhan TEST_DONE co the duoc goi vao kham truc tiep (chi can click "call").
     * Co the filter theo ngay va status (neu status null lay ca 4 status tren).
     */
    @GetMapping("/waiting/{departmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
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
    @GetMapping("/waiting-for-test/{departmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> getWaitingForTest(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) LocalDate workDate,
            Pageable pageable) {
        return RestResponses.ok(service.getWaitingByDepartment(departmentId, workDate, QueueStatus.WAITING_FOR_TEST, pageable));
    }

    /**
     * API lay danh sach benh nhan da hoan thanh xet nghiem (TEST_DONE).
     */
    @GetMapping("/test-done/{departmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> getTestDone(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) LocalDate workDate,
            Pageable pageable) {
        return RestResponses.ok(service.getWaitingByDepartment(departmentId, workDate, QueueStatus.TEST_DONE, pageable));
    }

    /**
     * API danh dau queue ticket da hoan thanh xet nghiem (WAITING_FOR_TEST -> TEST_DONE).
     */
    @PostMapping("/{id}/mark-test-done")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<QueueTicketResponse> markTestDone(@PathVariable UUID id) {
        return RestResponses.ok(service.markTestDone(id));
    }
}






