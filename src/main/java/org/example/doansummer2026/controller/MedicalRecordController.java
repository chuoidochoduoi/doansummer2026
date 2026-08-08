package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.ReceptionistRecordPageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.medicalHistory.MedicalHistoryResponse;
import org.example.doansummer2026.dto.medicalHistory.VisitDetailResponse;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.dto.medicalRecord.ReceptionistAllCustomerResponse;
import org.example.doansummer2026.dto.medicalRecord.ReceptionistCustomerResponse;
import org.example.doansummer2026.dto.medicalRecord.ReceptionistRecordResponse;
import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.service.AuthService;
import org.example.doansummer2026.service.MedicalRecordService;
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
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService service;
    private final AuthService authService;
    private final org.example.doansummer2026.service.AppointmentService appointmentService;

    // --- MAIN ENDPOINTS ---

    @GetMapping("/api/v1/medical-records")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<PageResponse<MedicalRecordResponse>> list(
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) MedicalRecordStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        return RestResponses.ok(service.search(doctorId, status, from, to, pageable));
    }

    @GetMapping("/api/v1/medical-records/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<MedicalRecordResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping("/api/v1/medical-records")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "MedicalRecord")
    public ResponseEntity<MedicalRecordResponse> create(@Valid @RequestBody MedicalRecordCreateRequest req) {
        MedicalRecordResponse created = service.create(req);
        return RestResponses.created("/api/v1/medical-records/{id}", created.recordId(), created);
    }

    @PutMapping("/api/v1/medical-records/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ADMIN')")
    @Auditable(action = AuditAction.UPDATE, entityName = "MedicalRecord", idParamName = "id")
    public ResponseEntity<MedicalRecordResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/api/v1/medical-records/{id}/draft")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ADMIN')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "MedicalRecord", idParamName = "id")
    public ResponseEntity<MedicalRecordResponse> saveDraft(@PathVariable UUID id,
                                                           @Valid @RequestBody MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.saveDraft(id, req));
    }

    @PostMapping("/api/v1/medical-records/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "MedicalRecord", idParamName = "id")
    public ResponseEntity<MedicalRecordResponse> complete(@PathVariable UUID id,
                                                          @Valid @RequestBody(required = false) MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.complete(id, req));
    }

    @DeleteMapping("/api/v1/medical-records/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    @Auditable(action = AuditAction.DELETE, entityName = "MedicalRecord", idParamName = "id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    @PostMapping("/api/v1/medical-records/{id}/rate")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<MedicalRecordResponse> rate(@PathVariable UUID id,
                                                     @RequestParam int ratingScore) {
        return RestResponses.ok(service.rate(id, ratingScore));
    }

    // --- PATIENT ENDPOINTS ---

    @GetMapping("/api/patient/medical-history")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ADMIN')")
    public ResponseEntity<ReceptionistRecordPageResponse<MedicalHistoryResponse>> getMedicalHistory(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        UUID profileId = authService.currentProfileId();
        if (profileId == null) {
            return RestResponses.ok(new ReceptionistRecordPageResponse<>(java.util.Collections.emptyList(), 0L, 0));
        }
        var pageResponse = service.getMedicalHistoryForPatient(profileId, search, pageable);
        return RestResponses.ok(ReceptionistRecordPageResponse.from(pageResponse));
    }

    @GetMapping("/api/patient/medical-history/{recordId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ADMIN')")
    public ResponseEntity<VisitDetailResponse> getVisitDetail(@PathVariable UUID recordId) {
        UUID profileId = authService.currentProfileId();
        if (profileId == null) {
            throw new org.example.doansummer2026.exception.ResourceNotFoundException("Khong tim thay profile");
        }
        VisitDetailResponse response = service.getVisitDetailByRecordId(recordId, profileId);
        return RestResponses.ok(response);
    }

    @PostMapping("/api/patient/medical-history/{recordId}/rate")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ADMIN')")
    public ResponseEntity<MedicalRecordResponse> rateVisit(@PathVariable UUID recordId,
                                                          @RequestParam int ratingScore) {
        return RestResponses.ok(service.rate(recordId, ratingScore));
    }

    @PostMapping("/api/patient/medical-history/{recordId}/feedback")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<org.example.doansummer2026.dto.medicalRecord.FeedbackResponse> submitFeedback(
            @PathVariable UUID recordId,
            @Valid @RequestBody org.example.doansummer2026.dto.medicalRecord.FeedbackRequest req) {
        return RestResponses.ok(service.submitFeedback(recordId, authService.currentProfileId(), req));
    }

    @GetMapping("/api/v1/feedbacks")
    @PreAuthorize("hasAnyAuthority('ROLE_CLINIC_MANAGER','ROLE_DOCTOR')")
    public ResponseEntity<org.example.doansummer2026.common.PageResponse<org.example.doansummer2026.dto.medicalRecord.FeedbackResponse>> feedbacks(
            Pageable pageable) {
        UUID doctorId = authService.getCurrentSystemRole() == org.example.doansummer2026.enums.SystemRole.CLINIC_MANAGER
                ? null : authService.currentStaffId();
        return RestResponses.ok(service.listFeedbacks(doctorId, pageable));
    }

    @PutMapping("/api/v1/feedbacks/{id}/respond")
    @PreAuthorize("hasAuthority('ROLE_CLINIC_MANAGER')")
    public ResponseEntity<org.example.doansummer2026.dto.medicalRecord.FeedbackResponse> respond(
            @PathVariable UUID id, @RequestBody java.util.Map<String,String> body) {
        return RestResponses.ok(service.respondFeedback(id, authService.currentStaffId(), body.get("response"), body.get("internalNote"), body.get("status")));
    }

    @PutMapping("/api/v1/feedbacks/{id}/explain")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<org.example.doansummer2026.dto.medicalRecord.FeedbackResponse> explain(
            @PathVariable UUID id, @RequestBody java.util.Map<String,String> body) {
        return RestResponses.ok(service.explainFeedback(id, authService.currentStaffId(), body.get("explanation")));
    }

    // --- RECEPTIONIST ENDPOINTS ---

    @GetMapping("/api/receptionist/records")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<ReceptionistRecordPageResponse<ReceptionistRecordResponse>> listRecordsForReceptionist(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) BloodType bloodType,
            Pageable pageable) {
        PageResponse<ReceptionistRecordResponse> pageResponse =
                service.searchForReceptionist(search, gender, age, bloodType, pageable);
        return RestResponses.ok(ReceptionistRecordPageResponse.from(pageResponse));
    }

    @GetMapping("/api/receptionist/records/customers")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<PageResponse<ReceptionistCustomerResponse>> listCustomersForReceptionist(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) BloodType bloodType,
            Pageable pageable) {
        PageResponse<ReceptionistCustomerResponse> pageResponse =
                service.searchUniqueCustomers(search, gender, age, bloodType, pageable);
        return RestResponses.ok(pageResponse);
    }

    @GetMapping("/api/receptionist/records/search-by-phone")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<java.util.List<ReceptionistAllCustomerResponse>> searchByPhoneForReceptionist(
            @RequestParam String phone) {
        var result = service.searchByPhone(phone);
        return RestResponses.ok(result);
    }

    @GetMapping("/api/receptionist/follow-ups")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<PageResponse<org.example.doansummer2026.dto.medicalRecord.FollowUpResponse>> getPendingFollowUps(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return RestResponses.ok(service.getPendingFollowUps(search, pageable));
    }

    @PostMapping("/api/receptionist/follow-ups/{recordId}/schedule")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<org.example.doansummer2026.dto.medicalRecord.FollowUpResponse> scheduleFollowUp(
            @PathVariable UUID recordId,
            @Valid @RequestBody org.example.doansummer2026.dto.appointment.AppointmentCreateRequest req) {
        return RestResponses.ok(service.scheduleFollowUp(recordId, req));
    }
}


