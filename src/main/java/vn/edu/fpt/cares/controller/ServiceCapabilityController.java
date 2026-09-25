package vn.edu.fpt.cares.controller;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.dto.capability.*;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.service.ServiceCapabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/service-capabilities") @RequiredArgsConstructor
public class ServiceCapabilityController {
    private static final String FIXED_CATALOG_MESSAGE =
            "Danh mục kỹ thuật là dữ liệu hệ thống cố định và chỉ được phép xem";
    private final ServiceCapabilityService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER','ROLE_STAFF')")
    public List<ServiceCapabilityResponse> list() { return service.list(); }

    @PostMapping @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ServiceCapabilityResponse create(@RequestBody ServiceCapabilityRequest request) {
        throw new ConflictException(FIXED_CATALOG_MESSAGE);
    }

    @PutMapping("/{id}") @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ServiceCapabilityResponse update(@PathVariable UUID id, @RequestBody ServiceCapabilityRequest request) {
        throw new ConflictException(FIXED_CATALOG_MESSAGE);
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        throw new ConflictException(FIXED_CATALOG_MESSAGE);
    }
}
