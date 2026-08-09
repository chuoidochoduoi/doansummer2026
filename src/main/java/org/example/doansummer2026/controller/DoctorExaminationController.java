package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.service.QueueTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Doctor-facing examination facade.
 *
 * <p>Exposes the examination as its own resource at {@code /api/doctor/examinations/{id}} so a
 * doctor can load the active medical record, save a draft, and complete the exam in one call —
 * optionally ordering lab tests which then keep the queue ticket in {@code WAITING_FOR_TEST}.
 *
 * <p>{@code id} accepts either the queue-ticket id (returned by {@code start-exam}) or the
 * medical-record id; both are resolved inside {@link QueueTicketService}.
 */
@RestController
@RequestMapping("/api/doctor/examinations")
@RequiredArgsConstructor
public class DoctorExaminationController {

    private final QueueTicketService service;

    /** Load the examination (medical record + nested details) the doctor is editing. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public ResponseEntity<MedicalRecordResponse> load(@PathVariable UUID id) {
        return RestResponses.ok(service.loadExamination(id));
    }

    /**
     * Save a draft of the examination. The frontend injects an extra {@code status:'draft'}
     * field which is intentionally ignored here (the service enforces the draft status).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public ResponseEntity<MedicalRecordResponse> draft(@PathVariable UUID id,
                                                       @Valid @RequestBody MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.saveExaminationDraft(id, req));
    }

    /**
     * Complete the examination.
     * <ul>
     *   <li>If {@code req.testRequests()} is present, the backend bills an invoice for the
     *       ordered services and moves the queue ticket to {@code WAITING_FOR_TEST}.</li>
     *   <li>Otherwise the medical record is closed ({@code COMPLETED}) and the queue ticket
     *       moves to {@code DONE}.</li>
     * </ul>
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_ADMIN')")
    public ResponseEntity<MedicalRecordResponse> complete(@PathVariable UUID id,
                                                          @Valid @RequestBody(required = false) MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.completeExamination(id, req));
    }
}
