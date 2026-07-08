package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.queueTicket.QueueTicketCreateRequest;
import org.example.doansummer2026.dto.queueTicket.QueueTicketResponse;
import org.example.doansummer2026.dto.queueTicket.QueueTicketUpdateRequest;
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

@RestController
@RequestMapping("/api/v1/queue-tickets")
@RequiredArgsConstructor
public class QueueTicketController {

    private final QueueTicketService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<PageResponse<QueueTicketResponse>> list(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam(required = false) QueueStatus status,
            Pageable pageable) {
        return RestResponses.ok(service.search(departmentId, workDate, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<QueueTicketResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<QueueTicketResponse> create(@Valid @RequestBody QueueTicketCreateRequest req) {
        QueueTicketResponse created = service.create(req);
        return RestResponses.created("/api/v1/queue-tickets/{id}", created.ticketId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<QueueTicketResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody QueueTicketUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/{id}/call")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<QueueTicketResponse> call(@PathVariable UUID id) {
        return RestResponses.ok(service.call(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<QueueTicketResponse> complete(@PathVariable UUID id) {
        return RestResponses.ok(service.complete(id));
    }

    @PostMapping("/{id}/skip")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<QueueTicketResponse> skip(@PathVariable UUID id) {
        return RestResponses.ok(service.skip(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }
}
