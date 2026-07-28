package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.queueTicket.QueueTicketResponse;
import org.example.doansummer2026.dto.queueTicket.QueueTicketUpdateRequest;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.service.QueueTicketService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * API endpoint cho frontend hook useQueueList.
 * GET /api/queue?status=&search=&sort=&page=&pageSize=
 * PUT /api/v1/queue/{id} - cap nhat trang thai phieu xep hang (bac si, y ta)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QueueController {

    private final QueueTicketService service;

    @GetMapping("/queue")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','RECEPTIONIST','ADMIN')")
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

    @PutMapping("/v1/queue/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','RECEPTIONIST','ADMIN')")
    public ResponseEntity<QueueTicketResponse> updateQueue(
            @PathVariable UUID id,
            @RequestBody QueueTicketUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }
}




