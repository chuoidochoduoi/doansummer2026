package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.notification.NotificationCreateRequest;
import org.example.doansummer2026.dto.notification.NotificationResponse;
import org.example.doansummer2026.dto.notification.NotificationUpdateRequest;
import org.example.doansummer2026.dto.notification.UnreadCountResponse;
import org.example.doansummer2026.enums.NotificationStatus;
import org.example.doansummer2026.service.NotificationService;
import org.springframework.data.domain.Pageable;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<NotificationResponse>> list(
            @RequestParam(required = false) UUID recipientId,
            @RequestParam(required = false) NotificationStatus status,
            Pageable pageable) {
        return RestResponses.ok(service.search(recipientId, status, pageable));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UnreadCountResponse> unreadCount(@RequestParam UUID recipientId) {
        return RestResponses.ok(service.unreadCount(recipientId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationCreateRequest req) {
        NotificationResponse created = service.create(req);
        return RestResponses.created("/api/v1/notifications/{id}", created.notificationId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody NotificationUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> send(@PathVariable UUID id) {
        return RestResponses.ok(service.send(id));
    }

    @PostMapping("/{id}/mark-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable UUID id) {
        return RestResponses.ok(service.markRead(id));
    }

    @PostMapping("/{id}/mark-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> markFailed(@PathVariable UUID id,
                                                             @RequestParam(required = false) String reason) {
        return RestResponses.ok(service.markFailed(id, reason));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }
}
