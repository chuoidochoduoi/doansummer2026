package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.schedule.ScheduleCreateRequest;
import org.example.doansummer2026.dto.schedule.ScheduleGenerateRequest;
import org.example.doansummer2026.dto.schedule.ScheduleResponse;
import org.example.doansummer2026.dto.schedule.ScheduleUpdateRequest;
import org.example.doansummer2026.enums.Shift;
import org.example.doansummer2026.service.StaffScheduleService;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class StaffScheduleController {

    private final StaffScheduleService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ScheduleResponse>> search(
            @RequestParam(required = false) UUID staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Shift shift,
            Pageable pageable) {
        return RestResponses.ok(service.search(staffId, from, to, shift, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody ScheduleCreateRequest req) {
        ScheduleResponse created = service.create(req);
        return RestResponses.created("/api/v1/schedules/{id}", created.scheduleId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponse> update(@PathVariable UUID id,
                                                   @RequestBody ScheduleUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    /** POST tac vu batch - sinh nhieu lich, khong co Location don le -> 200 OK. */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ScheduleResponse>> generate(@RequestBody ScheduleGenerateRequest req) {
        return RestResponses.ok(service.generateFromTemplates(
                req.weekStart(), req.staffIds(), req.overrideExisting()));
    }
}