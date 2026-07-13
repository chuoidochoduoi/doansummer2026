package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.scheduleTemplate.ScheduleTemplateRequest;
import org.example.doansummer2026.dto.scheduleTemplate.ScheduleTemplateResponse;
import org.example.doansummer2026.service.StaffScheduleTemplateService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedule-templates")
@RequiredArgsConstructor
public class StaffScheduleTemplateController {

    private final StaffScheduleTemplateService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleTemplateResponse> create(@Valid @RequestBody ScheduleTemplateRequest req) {
        ScheduleTemplateResponse created = service.create(req);
        return RestResponses.created("/api/v1/schedule-templates/{id}", created.templateId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleTemplateResponse> update(@PathVariable UUID id,
                                                            @RequestBody ScheduleTemplateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ScheduleTemplateResponse>> listByStaff(@RequestParam UUID staffId) {
        return RestResponses.ok(service.listByStaff(staffId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleTemplateResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }
}