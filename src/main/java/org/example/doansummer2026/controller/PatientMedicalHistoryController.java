package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.ReceptionistRecordPageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.medicalHistory.MedicalHistoryResponse;
import org.example.doansummer2026.dto.medicalHistory.VisitDetailResponse;
import org.example.doansummer2026.service.MedicalRecordService;
import org.example.doansummer2026.service.AuthService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/patient/medical-history")
@RequiredArgsConstructor
public class PatientMedicalHistoryController {

    private final MedicalRecordService medicalRecordService;
    private final AuthService authService;

    /**
     * API danh sach lich su kham benh cua benh nhan.
     * GET /api/patient/medical-history
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ADMIN')")
    public ResponseEntity<ReceptionistRecordPageResponse<MedicalHistoryResponse>> getMedicalHistory(
            @RequestParam(required = false) String search,
            Pageable pageable) {

        UUID profileId = authService.currentProfileId();
        if (profileId == null) {
            return RestResponses.ok(new ReceptionistRecordPageResponse<>(java.util.Collections.emptyList(), 0L, 0));
        }

        var pageResponse = medicalRecordService.getMedicalHistoryForPatient(profileId, search, pageable);

        return RestResponses.ok(ReceptionistRecordPageResponse.from(pageResponse));
    }

    /**
     * API chi tiet luot kham.
     * GET /api/patient/medical-history/{recordId}
     */
    @GetMapping("/{recordId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ADMIN')")
    public ResponseEntity<VisitDetailResponse> getVisitDetail(
            @PathVariable UUID recordId) {

        UUID profileId = authService.currentProfileId();
        if (profileId == null) {
            throw new org.example.doansummer2026.exception.ResourceNotFoundException("Khong tim thay profile");
        }

        VisitDetailResponse response = medicalRecordService.getVisitDetailByRecordId(recordId, profileId);
        return RestResponses.ok(response);
    }
}