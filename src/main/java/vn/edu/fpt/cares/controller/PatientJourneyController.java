package vn.edu.fpt.cares.controller;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.RestResponses;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.journey.PatientJourneyResponse;
import vn.edu.fpt.cares.service.PatientJourneyService;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import vn.edu.fpt.cares.aop.Auditable;
import vn.edu.fpt.cares.enums.AuditAction;

@RestController @RequiredArgsConstructor
public class PatientJourneyController {
    private final PatientJourneyService service;
    private final vn.edu.fpt.cares.service.AuthService authService;
    private final vn.edu.fpt.cares.service.FamilyAccessService familyAccessService;
    @GetMapping("/api/v1/patient-journeys")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER','ROLE_ADMIN','ROLE_DOCTOR','ROLE_NURSE')")
    public ResponseEntity<PageResponse<PatientJourneyResponse>> list(@RequestParam(required=false) String search,
                                                                       @RequestParam(required=false) String status,
                                                                       @RequestParam(required=false) String scope,
                                                                       Pageable pageable) {
        return RestResponses.ok(service.list(search, status, scope, pageable));
    }
    @GetMapping("/api/v1/patient-journeys/{visitId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER','ROLE_ADMIN','ROLE_DOCTOR','ROLE_NURSE')")
    public ResponseEntity<PatientJourneyResponse> get(@PathVariable UUID visitId) { return RestResponses.ok(service.get(visitId)); }

    @PostMapping("/api/v1/patient-journeys/{visitId}/advance")
    @PreAuthorize("hasAnyAuthority('ROLE_CLINIC_MANAGER','ROLE_ADMIN')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "CustomerVisit", idParamName = "visitId", description = "Khôi phục bước hàng chờ bị kẹt")
    public ResponseEntity<PatientJourneyResponse> advance(@PathVariable UUID visitId) {
        return RestResponses.ok(service.advanceBlockedStep(visitId));
    }
    @GetMapping("/api/patient/my-journeys")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<List<PatientJourneyResponse>> mine(
            @RequestParam(required = false) UUID patientProfileId,
            @RequestParam(defaultValue = "false") boolean includeFamily) {
        UUID accountId = authService.currentAccount().getAccountId();
        if (patientProfileId != null) {
            UUID profileId = familyAccessService.resolveReadableProfile(accountId, patientProfileId).getProfileId();
            return RestResponses.ok(service.listForCustomer(profileId));
        }
        if (includeFamily) {
            List<PatientJourneyResponse> all = familyAccessService.readableProfiles(accountId, true).stream()
                    .flatMap(profile -> service.listForCustomer(profile.getProfileId()).stream())
                    .toList();
            return RestResponses.ok(all);
        }
        return RestResponses.ok(service.listForCustomer(familyAccessService.ownerProfile(accountId).getProfileId()));
    }

    @GetMapping("/api/patient/my-journeys/{visitId}/queue")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<vn.edu.fpt.cares.dto.journey.PatientQueueResponse> myQueue(@PathVariable UUID visitId) {
        UUID accountId = authService.currentAccount().getAccountId();
        var readableIds = familyAccessService.readableProfiles(accountId, true).stream()
                .map(vn.edu.fpt.cares.model.Profile::getProfileId).toList();
        return RestResponses.ok(service.queueForCustomer(visitId, readableIds));
    }

    @GetMapping("/api/public/patient-journeys/lookup")
    public ResponseEntity<PatientJourneyResponse> lookupGuest(@RequestParam String visitCode,
                                                               @RequestParam String phone) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(service.lookupGuest(visitCode, phone));
    }

    @GetMapping("/api/public/patient-journeys/lookup/queue")
    public ResponseEntity<vn.edu.fpt.cares.dto.journey.PatientQueueResponse> lookupGuestQueue(
            @RequestParam String visitCode, @RequestParam String phone) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(service.lookupGuestQueue(visitCode, phone));
    }

}
