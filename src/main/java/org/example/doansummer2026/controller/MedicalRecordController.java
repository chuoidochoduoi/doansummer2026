package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.ReceptionistRecordPageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.medicalhistory.MedicalHistoryResponse;
import org.example.doansummer2026.dto.medicalhistory.VisitDetailResponse;
import org.example.doansummer2026.dto.medicalhistory.VisitHistorySummaryResponse;
import org.example.doansummer2026.dto.medicalrecord.MedicalRecordCreateRequest;
import org.example.doansummer2026.dto.medicalrecord.MedicalRecordResponse;
import org.example.doansummer2026.dto.medicalrecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.dto.medicalrecord.ReceptionistAllCustomerResponse;
import org.example.doansummer2026.dto.medicalrecord.ReceptionistCustomerResponse;
import org.example.doansummer2026.dto.medicalrecord.ReceptionistRecordResponse;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService service;
    private final AuthService authService;
    private final org.example.doansummer2026.service.AppointmentService appointmentService;
    private final org.example.doansummer2026.service.ProfileService profileService;
    private final org.example.doansummer2026.service.FamilyAccessService familyAccessService;

    // --- MAIN ENDPOINTS ---

    @GetMapping("/api/v1/medical-records")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public ResponseEntity<PageResponse<MedicalRecordResponse>> list(
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) MedicalRecordStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        return RestResponses.ok(service.search(doctorId, status, from, to, pageable));
    }

    @GetMapping("/api/v1/medical-records/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER','ROLE_ADMIN')")
    public ResponseEntity<MedicalRecordResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @GetMapping("/api/v1/medical-records/{id}/clinical-form")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER','ROLE_ADMIN')")
    public ResponseEntity<org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse> clinicalForm(
            @PathVariable UUID id) {
        return RestResponses.ok(service.getClinicalForm(id));
    }

    @GetMapping("/api/v1/medical-records/{id}/patient-allergies")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER','ROLE_ADMIN')")
    public ResponseEntity<org.example.doansummer2026.dto.medicalrecord.PatientAllergyResponse> patientAllergies(
            @PathVariable UUID id) {
        return RestResponses.ok(service.getPatientAllergies(id));
    }

    @GetMapping("/api/v1/medical-records/{id}/visit-detail")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER','ROLE_ADMIN')")
    public ResponseEntity<VisitDetailResponse> staffVisitDetail(@PathVariable UUID id) {
        return RestResponses.ok(service.getVisitDetailForStaff(id));
    }

    @PutMapping("/api/v1/medical-records/{id}/patient-allergies")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE')")
    @Auditable(action = AuditAction.UPDATE, entityName = "PatientAllergy", idParamName = "id",
            description = "Xác minh và cập nhật dị ứng trong quá trình khám")
    public ResponseEntity<org.example.doansummer2026.dto.medicalrecord.PatientAllergyResponse> updatePatientAllergies(
            @PathVariable UUID id,
            @Valid @RequestBody org.example.doansummer2026.dto.medicalrecord.PatientAllergyRequest request) {
        return RestResponses.ok(service.updatePatientAllergies(id, request));
    }

    @GetMapping("/api/v1/medical-records/{id}/previous-history")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public ResponseEntity<java.util.List<MedicalHistoryResponse>> previousHistory(@PathVariable UUID id) {
        return RestResponses.ok(service.getPreviousHistoryForDoctor(id));
    }

    @PostMapping("/api/v1/medical-records")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "MedicalRecord")
    public ResponseEntity<MedicalRecordResponse> create(@Valid @RequestBody MedicalRecordCreateRequest req) {
        if (authService.getCurrentSystemRole() != org.example.doansummer2026.enums.SystemRole.ADMIN
                && !java.util.Objects.equals(authService.currentStaffId(), req.doctorId())) {
            throw new org.example.doansummer2026.exception.BadRequestException(
                    "Bác sĩ tạo hồ sơ phải là bác sĩ đang đăng nhập");
        }
        MedicalRecordResponse created = service.create(req);
        return RestResponses.created("/api/v1/medical-records/{id}", created.recordId(), created);
    }

    @PutMapping("/api/v1/medical-records/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_ADMIN')")
    @Auditable(action = AuditAction.UPDATE, entityName = "MedicalRecord", idParamName = "id")
    public ResponseEntity<MedicalRecordResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/api/v1/medical-records/{id}/draft")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    @Auditable(action = AuditAction.DRAFT_SAVED, entityName = "MedicalRecord", idParamName = "id", description = "Lưu nháp hồ sơ khám")
    public ResponseEntity<MedicalRecordResponse> saveDraft(@PathVariable UUID id,
                                                           @Valid @RequestBody MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.saveDraft(id, req));
    }

    @PostMapping("/api/v1/medical-records/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_ADMIN')")
    @Auditable(action = AuditAction.RECORD_COMPLETED, entityName = "MedicalRecord", idParamName = "id", description = "Hoàn thành hồ sơ khám")
    public ResponseEntity<MedicalRecordResponse> complete(@PathVariable UUID id,
                                                          @Valid @RequestBody(required = false) MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.complete(id, req));
    }

    /** Bac si dat lich tai kham truc tiep cho benh nhan dang kham. */
    @PostMapping("/api/v1/medical-records/{id}/follow-up-appointment")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "Appointment", idParamName = "id", description = "Tạo lịch tái khám")
    public ResponseEntity<org.example.doansummer2026.dto.medicalrecord.FollowUpResponse> createFollowUpAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody org.example.doansummer2026.dto.appointment.AppointmentCreateRequest req) {
        return RestResponses.ok(service.scheduleFollowUp(id, req));
    }

    @DeleteMapping("/api/v1/medical-records/{id}")
    @PreAuthorize("hasAuthority('ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.DELETE, entityName = "MedicalRecord", idParamName = "id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    @PostMapping("/api/v1/medical-records/{id}/rate")
    @Auditable(action = AuditAction.UPDATE, entityName = "MedicalRecord", idParamName = "id", description = "Đánh giá lượt khám (API tương thích)")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MedicalRecordResponse> rate(@PathVariable UUID id,
                                                     @RequestParam int ratingScore) {
        return RestResponses.ok(service.rate(id, ratingScore));
    }

    // --- PATIENT ENDPOINTS ---

    @GetMapping("/api/patient/medical-history")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<ReceptionistRecordPageResponse<MedicalHistoryResponse>> getMedicalHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID patientProfileId,
            Pageable pageable) {
        UUID profileId = patientProfileId(patientProfileId, false);
        if (profileId == null) {
            return RestResponses.ok(new ReceptionistRecordPageResponse<>(java.util.Collections.emptyList(), 0L, 0));
        }
        var pageResponse = service.getMedicalHistoryForPatient(profileId, search, pageable);
        return RestResponses.ok(ReceptionistRecordPageResponse.from(pageResponse));
    }

    @GetMapping("/api/patient/medical-history/{recordId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<VisitDetailResponse> getVisitDetail(@PathVariable UUID recordId,
                                                               @RequestParam(required = false) UUID patientProfileId) {
        UUID profileId = patientProfileId(patientProfileId, false);
        if (profileId == null) {
            throw new org.example.doansummer2026.exception.ResourceNotFoundException("Không tìm thấy hồ sơ cá nhân");
        }
        VisitDetailResponse response = service.getVisitDetailByRecordId(recordId, profileId);
        return RestResponses.ok(response);
    }

    @GetMapping("/api/patient/medical-history/visits")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<ReceptionistRecordPageResponse<VisitHistorySummaryResponse>> getVisitHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID patientProfileId, Pageable pageable) {
        UUID profileId = patientProfileId(patientProfileId, false);
        if (profileId == null) {
            return RestResponses.ok(new ReceptionistRecordPageResponse<>(java.util.List.of(), 0L, 0));
        }
        return RestResponses.ok(ReceptionistRecordPageResponse.from(
                service.getVisitHistoryForPatient(profileId, search, pageable)));
    }

    @GetMapping("/api/patient/medical-history/visits/{visitId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<VisitDetailResponse> getPatientVisitDetail(@PathVariable UUID visitId,
                                                                      @RequestParam(required = false) UUID patientProfileId) {
        UUID profileId = patientProfileId(patientProfileId, false);
        if (profileId == null) {
            throw new org.example.doansummer2026.exception.ResourceNotFoundException(
                    "Không tìm thấy hồ sơ cá nhân");
        }
        return RestResponses.ok(service.getPatientVisitDetail(visitId, profileId));
    }

    @GetMapping("/api/patient/medical-history/{recordId}/clinical-form")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse> getPatientClinicalForm(
            @PathVariable UUID recordId,
            @RequestParam(required = false) UUID patientProfileId) {
        return RestResponses.ok(service.getClinicalFormForPatient(recordId, patientProfileId(patientProfileId, false)));
    }

    @PostMapping("/api/patient/medical-history/{recordId}/rate")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER')")
    @Auditable(action = AuditAction.UPDATE, entityName = "MedicalRecord", idParamName = "recordId", description = "Đánh giá lượt khám")
    public ResponseEntity<MedicalRecordResponse> rateVisit(@PathVariable UUID recordId,
                                                          @RequestParam(required = false) UUID patientProfileId,
                                                          @RequestParam int ratingScore) {
        UUID profileId = patientProfileId(patientProfileId, true);
        if (profileId == null) {
            throw new org.example.doansummer2026.exception.ResourceNotFoundException(
                    "Không tìm thấy hồ sơ cá nhân");
        }
        // Không cho khách hàng đánh giá hồ sơ của người khác chỉ bằng cách đổi recordId.
        service.getVisitDetailByRecordId(recordId, profileId);
        return RestResponses.ok(service.rate(recordId, ratingScore));
    }

    @PostMapping("/api/patient/medical-history/{recordId}/feedback")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @Auditable(action = AuditAction.UPDATE, entityName = "MedicalRecord", idParamName = "recordId", description = "Gửi phản hồi lượt khám")
    public ResponseEntity<org.example.doansummer2026.dto.medicalrecord.FeedbackResponse> submitFeedback(
            @PathVariable UUID recordId,
            @RequestParam(required = false) UUID patientProfileId,
            @Valid @RequestBody org.example.doansummer2026.dto.medicalrecord.FeedbackRequest req) {
        return RestResponses.ok(service.submitFeedback(recordId, patientProfileId(patientProfileId, true), req));
    }

    private UUID patientProfileId(UUID requestedProfileId, boolean activeRequired) {
        UUID accountId = authService.currentAccount().getAccountId();
        return (activeRequired
                ? familyAccessService.resolveActiveProfile(accountId, requestedProfileId)
                : familyAccessService.resolveReadableProfile(accountId, requestedProfileId)).getProfileId();
    }

    @GetMapping("/api/v1/feedbacks")
    @PreAuthorize("hasAnyAuthority('ROLE_CLINIC_MANAGER','ROLE_RECEPTIONIST')")
    public ResponseEntity<org.example.doansummer2026.common.PageResponse<org.example.doansummer2026.dto.medicalrecord.FeedbackResponse>> feedbacks(
            Pageable pageable) {
        return RestResponses.ok(service.listFeedbacks(null, pageable));
    }

    @GetMapping("/api/v1/feedbacks/stats/unanswered-count")
    @PreAuthorize("hasAnyAuthority('ROLE_CLINIC_MANAGER','ROLE_RECEPTIONIST')")
    public ResponseEntity<Map<String, Long>> countUnansweredFeedbacks() {
        return RestResponses.ok(Map.of("count", service.countUnansweredFeedbacks()));
    }

    @PutMapping("/api/v1/feedbacks/{id}/respond")
    @PreAuthorize("hasAnyAuthority('ROLE_CLINIC_MANAGER','ROLE_RECEPTIONIST')")
    @Auditable(action = AuditAction.UPDATE, entityName = "MedicalRecord", idParamName = "id", description = "Phản hồi đánh giá của bệnh nhân")
    public ResponseEntity<org.example.doansummer2026.dto.medicalrecord.FeedbackResponse> respond(
            @PathVariable UUID id, @RequestBody java.util.Map<String,String> body) {
        return RestResponses.ok(service.respondFeedback(id, authService.currentStaffId(), body.get("response")));
    }

    // --- RECEPTIONIST ENDPOINTS ---

    @GetMapping("/api/receptionist/records")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
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

    /** Danh sách bệnh nhân chỉ đọc dành cho quản lý phòng khám. */
    @GetMapping("/api/v1/clinic-manager/patients")
    @PreAuthorize("hasAuthority('ROLE_CLINIC_MANAGER')")
    public ResponseEntity<PageResponse<ReceptionistCustomerResponse>> listPatientsForClinicManager(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) BloodType bloodType,
            Pageable pageable) {
        return RestResponses.ok(service.searchUniqueCustomers(search, gender, age, bloodType, pageable));
    }

    @GetMapping("/api/receptionist/records/customers/{customerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<ReceptionistCustomerResponse> getCustomerForReceptionist(
            @PathVariable UUID customerId) {
        return RestResponses.ok(service.getCustomerForReceptionist(customerId));
    }

    @PutMapping("/api/receptionist/records/customers/{customerId}")
    @Auditable(action = org.example.doansummer2026.enums.AuditAction.UPDATE, entityName = "Profile", idParamName = "customerId", description = "Lễ tân cập nhật hồ sơ bệnh nhân")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<org.example.doansummer2026.dto.profile.ProfileResponse> updateCustomerForReceptionist(
            @PathVariable UUID customerId,
            @Valid @RequestBody org.example.doansummer2026.dto.profile.ProfileUpdateRequest req) {
        return RestResponses.ok(profileService.update(customerId, req));
    }

    @GetMapping("/api/receptionist/records/customers/{customerId}/visits")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<PageResponse<MedicalHistoryResponse>> getCustomerVisitsForReceptionist(
            @PathVariable UUID customerId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return RestResponses.ok(service.getMedicalHistoryForPatient(customerId, search, pageable));
    }

    @GetMapping("/api/receptionist/records/customers/{customerId}/visits/{visitId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<VisitDetailResponse> getCustomerVisitForReceptionist(
            @PathVariable UUID customerId,
            @PathVariable UUID visitId) {
        return RestResponses.ok(service.getVisitDetail(visitId, customerId));
    }

    @GetMapping("/api/receptionist/records/search-by-phone")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<java.util.List<ReceptionistAllCustomerResponse>> searchByPhoneForReceptionist(
            @RequestParam String phone) {
        var result = service.searchByPhone(phone);
        return RestResponses.ok(result);
    }

}



