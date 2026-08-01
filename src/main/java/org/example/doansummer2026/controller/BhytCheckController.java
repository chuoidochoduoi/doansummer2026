package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.bhyt.BhytCheckResponse;
import org.example.doansummer2026.service.BhytCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bhyt")
@RequiredArgsConstructor
public class BhytCheckController {

    private final BhytCheckService bhytCheckService;

    @GetMapping("/check")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CUSTOMER','ADMIN')")
    public ResponseEntity<BhytCheckResponse> checkCard(@RequestParam String cardNumber) {
        BhytCheckResponse response = bhytCheckService.checkCard(cardNumber);
        return RestResponses.ok(response);
    }
}
