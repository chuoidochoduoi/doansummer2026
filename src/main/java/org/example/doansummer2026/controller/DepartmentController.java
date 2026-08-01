package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.department.DepartmentCreateRequest;
import org.example.doansummer2026.dto.department.DepartmentResponse;
import org.example.doansummer2026.dto.department.DepartmentUpdateRequest;
import org.example.doansummer2026.dto.staff.StaffOptionResponse;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.service.DepartmentService;
import org.example.doansummer2026.service.StaffService;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.enums.AuditAction;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService service;
    private final StaffService staffService;

    /** API cho ADMIN - xem danh sach phong voi tat ca truong */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<DepartmentResponse>> listForAdmin(
            @RequestParam(required = false) DepartmentType[] departmentTypes,
            Pageable pageable) {
        if (departmentTypes == null || departmentTypes.length == 0) {
            return RestResponses.ok(service.listAll(pageable));
        }
        return RestResponses.ok(service.listMultiple(pageable, List.of(departmentTypes)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<PageResponse<DepartmentResponse>> list(
            @RequestParam(required = false) DepartmentType[] departmentTypes,
            Pageable pageable) {
        if (departmentTypes == null || departmentTypes.length == 0) {
            return RestResponses.ok(service.listAll(pageable));
        }
        return RestResponses.ok(service.listMultiple(pageable, List.of(departmentTypes)));
    }

    /** Lấy các khoa khám bệnh, xét nghiệm, chẩn đoán hình ảnh cho bác sĩ. */
    @GetMapping("/clinical")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<PageResponse<DepartmentResponse>> listClinical(
            Pageable pageable) {
        return RestResponses.ok(service.listMultiple(pageable, List.of(
                DepartmentType.EXAMINATION,
                DepartmentType.LABORATORY,
                DepartmentType.IMAGING
        )));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<DepartmentResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "Department")
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentCreateRequest req) {
        DepartmentResponse created = service.create(req);
        return RestResponses.created("/api/v1/departments/{id}", created.departmentId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = AuditAction.UPDATE, entityName = "Department", idParamName = "id")
    public ResponseEntity<DepartmentResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody DepartmentUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = AuditAction.DELETE, entityName = "Department", idParamName = "id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "Department", idParamName = "id")
    public ResponseEntity<DepartmentResponse> updateStatus(@PathVariable UUID id, @RequestBody java.util.Map<String, String> payload) {
        String statusStr = payload.get("status");
        if (statusStr != null) {
            org.example.doansummer2026.enums.DepartmentStatus status = org.example.doansummer2026.enums.DepartmentStatus.valueOf(statusStr);
            return RestResponses.ok(service.updateStatus(id, status));
        }
        return RestResponses.ok(service.get(id));
    }

    /**
     * Lay danh sach bac si (GENERAL_DOCTOR, SPECIALIST_DOCTOR) de chon lam head doctor.
     * Dung cho form tao/sua department.
     */
    @GetMapping("/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StaffOptionResponse>> listDoctors() {
        var doctors = staffService.findAllDoctors();
        return RestResponses.ok(doctors);
    }

    @GetMapping("/nurses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StaffOptionResponse>> listNurses() {
        var nurses = staffService.findAllNurses();
        return RestResponses.ok(nurses);
    }

    /** Lay phong kham duoc chi dinh cho bac si/y ta hien tai */
    @GetMapping("/my-department")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','GENERAL_DOCTOR','SPECIALIST_DOCTOR')")
    public ResponseEntity<DepartmentResponse> getMyDepartment() {
        return RestResponses.ok(service.getMyDepartment());
    }


}



