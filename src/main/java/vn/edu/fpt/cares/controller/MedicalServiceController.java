package vn.edu.fpt.cares.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.common.RestResponses;
import vn.edu.fpt.cares.dto.medicalservice.MedicalServiceCreateRequest;
import vn.edu.fpt.cares.dto.medicalservice.MedicalServiceResponse;
import vn.edu.fpt.cares.dto.medicalservice.MedicalServiceUpdateRequest;
import vn.edu.fpt.cares.dto.medicalservice.ServiceSelectionResolveRequest;
import vn.edu.fpt.cares.dto.medicalservice.ServiceSelectionResolutionResponse;
import vn.edu.fpt.cares.enums.ServiceStatus;
import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.service.MedicalServiceService;
import vn.edu.fpt.cares.aop.Auditable;
import vn.edu.fpt.cares.enums.AuditAction;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medical-services")
@RequiredArgsConstructor
public class MedicalServiceController {

    private final MedicalServiceService service;
    private final vn.edu.fpt.cares.service.MedicalServiceSelectionPolicyService selectionPolicyService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_STAFF', 'ROLE_DOCTOR', 'ROLE_RECEPTIONIST')")
    public ResponseEntity<PageResponse<MedicalServiceResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) DepartmentType departmentType,
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(required = false) UUID specializationId,
            @RequestParam(defaultValue = "false") boolean primaryOnly,
            Pageable pageable) {
        return RestResponses.ok(service.search(
                keyword, departmentType, status, specializationId, primaryOnly, pageable));
    }

    /**
     * API cho khach hang/benh nhan xem dich vu dang hoat dong.
     * Chi tra ve cac dich vu co status = ACTIVE.
     */
    @GetMapping("/available")
    public ResponseEntity<PageResponse<MedicalServiceResponse>> listAvailable(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) DepartmentType departmentType,
            Pageable pageable) {
        return RestResponses.ok(service.listAvailable(keyword, departmentType, pageable));
    }

    /** Chuẩn hóa danh sách trước khi đặt lịch; endpoint chỉ đọc và không làm lộ dữ liệu bệnh nhân. */
    @PostMapping("/resolve-selection")
    public ResponseEntity<ServiceSelectionResolutionResponse> resolveSelection(
            @Valid @RequestBody ServiceSelectionResolveRequest request) {
        return RestResponses.ok(selectionPolicyService.resolveResponse(request.serviceIds()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<MedicalServiceResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_STAFF', 'ROLE_DOCTOR')")
    public ResponseEntity<Map<String, Long>> getStats(
            @RequestParam(defaultValue = "false") boolean primaryOnly) {
        return RestResponses.ok(service.getStats(primaryOnly));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "ServiceItem")
    public ResponseEntity<MedicalServiceResponse> create(@Valid @RequestBody MedicalServiceCreateRequest req) {
        MedicalServiceResponse created = service.create(req);
        return RestResponses.created("/api/v1/medical-services/{id}", created.serviceId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Auditable(action = AuditAction.UPDATE, entityName = "ServiceItem", idParamName = "id")
    public ResponseEntity<MedicalServiceResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody MedicalServiceUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Auditable(action = AuditAction.DELETE, entityName = "ServiceItem", idParamName = "id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    /** Ngung hoat dong dich vu - chi dich vu ACTIVE moi duoc ngung. */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "ServiceItem", idParamName = "id")
    public ResponseEntity<MedicalServiceResponse> deactivate(@PathVariable UUID id) {
        return RestResponses.ok(service.deactivate(id));
    }

    /** Phat hanh dich vu - chi dich vu DRAFT moi duoc phat hanh. */
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "ServiceItem", idParamName = "id")
    public ResponseEntity<MedicalServiceResponse> publish(@PathVariable UUID id) {
        return RestResponses.ok(service.publish(id));
    }
}
