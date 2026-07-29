package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.ReceptionistRecordPageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.medicalRecord.ReceptionistRecordResponse;
import org.example.doansummer2026.dto.medicalRecord.ReceptionistCustomerResponse;
import org.example.doansummer2026.dto.medicalRecord.ReceptionistAllCustomerResponse;
import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.service.MedicalRecordService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/receptionist/records")
@RequiredArgsConstructor
public class ReceptionistRecordController {

    private final MedicalRecordService medicalRecordService;

    /**
     * API cho le tan quan ly ho so benh an.
     * GET /api/receptionist/records
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<ReceptionistRecordPageResponse<ReceptionistRecordResponse>> listRecords(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) BloodType bloodType,
            Pageable pageable) {
        PageResponse<ReceptionistRecordResponse> pageResponse =
                medicalRecordService.searchForReceptionist(search, gender, age, bloodType, pageable);
        return RestResponses.ok(ReceptionistRecordPageResponse.from(pageResponse));
    }

    /**
     * API cho le tan lay danh sach customer (benh nhan) khong lap lai.
     * GET /api/receptionist/records/customers
     */
    @GetMapping("/customers")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<PageResponse<ReceptionistCustomerResponse>> listCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) BloodType bloodType,
            Pageable pageable) {
        PageResponse<ReceptionistCustomerResponse> pageResponse =
                medicalRecordService.searchUniqueCustomers(search, gender, age, bloodType, pageable);
        return RestResponses.ok(pageResponse);
    }

    /**
     * API tim customer hoac guest theo so dien thoai.
     * Tra ve danh sach gom ca customer co account va guest van g lai (khong lap).
     * GET /api/receptionist/records/search-by-phone?phone=...
     */
    @GetMapping("/search-by-phone")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<java.util.List<ReceptionistAllCustomerResponse>> searchByPhone(
            @RequestParam String phone) {
        var result = medicalRecordService.searchByPhone(phone);
        return RestResponses.ok(result);
    }
}