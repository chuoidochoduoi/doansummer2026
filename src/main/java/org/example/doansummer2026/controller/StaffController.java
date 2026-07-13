package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.staff.StaffCreateRequest;
import org.example.doansummer2026.dto.staff.StaffResponse;
import org.example.doansummer2026.dto.staff.StaffUpdateRequest;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.service.StaffService;
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
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    /** ADMIN xem danh sach. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<StaffResponse>> search(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID specializationId,
            @RequestParam(required = false) SystemRole systemRole,
            Pageable pageable) {
        return RestResponses.ok(staffService.search(departmentId, specializationId, systemRole, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(staffService.get(id));
    }

    /** ADMIN tao nhan vien moi (kem account + profile). */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffResponse> create(@RequestBody StaffCreateRequest req) {
        StaffResponse created = staffService.create(req);
        return RestResponses.created("/api/v1/staff/{id}", created.staffId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffResponse> update(@PathVariable UUID id,
                                                @RequestBody StaffUpdateRequest req) {
        return RestResponses.ok(staffService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        staffService.delete(id);
        return RestResponses.noContent();
    }
}