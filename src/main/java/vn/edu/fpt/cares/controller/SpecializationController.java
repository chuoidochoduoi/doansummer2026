package vn.edu.fpt.cares.controller;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.common.RestResponses;
import vn.edu.fpt.cares.dto.specialization.SpecializationResponse;
import vn.edu.fpt.cares.service.SpecializationService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/specializations")
@RequiredArgsConstructor
public class SpecializationController {

    private final SpecializationService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_STAFF', 'ROLE_DOCTOR')")
    public ResponseEntity<PageResponse<SpecializationResponse>> list(Pageable pageable) {
        return RestResponses.ok(service.list(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_STAFF', 'ROLE_DOCTOR')")
    public ResponseEntity<SpecializationResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

}

