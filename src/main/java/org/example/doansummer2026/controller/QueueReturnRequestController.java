package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.journey.QueueReturnRequestResponse;
import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.service.QueueReturnRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queue-return-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER','ROLE_ADMIN')")
public class QueueReturnRequestController {

    private final QueueReturnRequestService service;

    @GetMapping
    public ResponseEntity<List<QueueReturnRequestResponse>> pending() {
        return RestResponses.ok(service.pendingRequests());
    }

    @PostMapping("/{queueTicketId}/confirm")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "QueueTicket",
            idParamName = "queueTicketId", description = "Xác nhận bệnh nhân vắng đã quay lại quầy")
    public ResponseEntity<QueueReturnRequestResponse> confirm(@PathVariable UUID queueTicketId) {
        return RestResponses.ok(service.confirm(queueTicketId));
    }
}
