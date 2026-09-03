package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.family.FamilyMemberRequest;
import org.example.doansummer2026.dto.family.FamilyMemberResponse;
import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.service.AuthService;
import org.example.doansummer2026.service.FamilyMemberService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/family-members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class FamilyMemberController {

    private final FamilyMemberService familyMemberService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<List<FamilyMemberResponse>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return RestResponses.ok(familyMemberService.list(currentAccountId(), includeInactive));
    }

    @PostMapping
    @Auditable(action = AuditAction.CREATE, entityName = "FamilyMember")
    public ResponseEntity<FamilyMemberResponse> create(@Valid @RequestBody FamilyMemberRequest request) {
        FamilyMemberResponse created = familyMemberService.create(currentAccountId(), request);
        return RestResponses.created("/api/v1/customer/family-members/{id}", created.id(), created);
    }

    @PutMapping("/{id}")
    @Auditable(action = AuditAction.UPDATE, entityName = "FamilyMember", idParamName = "id")
    public ResponseEntity<FamilyMemberResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody FamilyMemberRequest request) {
        return RestResponses.ok(familyMemberService.update(currentAccountId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "FamilyMember", idParamName = "id")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        familyMemberService.archive(currentAccountId(), id);
        return RestResponses.noContent();
    }

    @PostMapping("/{id}/restore")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "FamilyMember", idParamName = "id")
    public ResponseEntity<FamilyMemberResponse> restore(@PathVariable UUID id) {
        return RestResponses.ok(familyMemberService.restore(currentAccountId(), id));
    }

    private UUID currentAccountId() {
        return authService.currentAccount().getAccountId();
    }
}
