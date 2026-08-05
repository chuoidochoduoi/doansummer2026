package org.example.doansummer2026.controller;

import org.example.doansummer2026.dto.insurance.BhxhCheckResponse;
import org.example.doansummer2026.service.BhxhIntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bhxh")
public class BhxhController {

    private final BhxhIntegrationService bhxhIntegrationService;

    public BhxhController(BhxhIntegrationService bhxhIntegrationService) {
        this.bhxhIntegrationService = bhxhIntegrationService;
    }

    @GetMapping("/check")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'CLINIC_MANAGER', 'ADMIN', 'ROLE_CUSTOMER')")
    public ResponseEntity<BhxhCheckResponse> checkCard(@RequestParam String cardNumber) {
        return ResponseEntity.ok(bhxhIntegrationService.checkBhytCard(cardNumber));
    }
}
