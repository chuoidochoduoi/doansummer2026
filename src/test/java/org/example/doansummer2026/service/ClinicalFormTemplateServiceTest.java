package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.clinicalform.*;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.*;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicalFormTemplateServiceTest {
    @Mock ClinicalFormTemplateRepository templateRepo;
    @Mock ClinicalFormTemplateVersionRepository versionRepo;
    @Mock MedicalServiceFormTemplateRepository bindingRepo;
    @Mock MedicalServiceRepository serviceRepo;
    @Mock StaffInfoRepository staffRepo;
    @Mock AuthService authService;
    @Mock ClinicalFormEngine engine;
    @InjectMocks ClinicalFormTemplateService service;
    final JsonNode schema = new JsonMapper().readTree("{\"fields\":[]}");
    final LocalDate today = LocalDate.of(2026, 9, 4);
    final ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

    ClinicalFormTemplate template() {
        return ClinicalFormTemplate.builder().templateId(UUID.randomUUID()).code("CUSTOM_LAB")
                .name("Kết quả xét nghiệm").context(ClinicalFormContext.LAB_RESULT).active(true).build();
    }
    void lookup(ClinicalFormTemplate t) { when(templateRepo.findById(t.getTemplateId())).thenReturn(Optional.of(t)); }
    ClinicalFormTemplateVersion version(ClinicalFormTemplate t, int number, ClinicalTemplateStatus status) {
        return ClinicalFormTemplateVersion.builder().versionId(UUID.randomUUID()).template(t).versionNo(number)
                .status(status).schemaJson(schema).effectiveFrom(today).build();
    }
    StaffInfo actor() {
        StaffInfo staff = StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(SystemRole.CLINIC_MANAGER).build();
        when(authService.currentStaffId()).thenReturn(staff.getStaffId());
        when(staffRepo.findById(staff.getStaffId())).thenReturn(Optional.of(staff));
        return staff;
    }
    void saveVersions() { when(versionRepo.save(any())).thenAnswer(call -> call.getArgument(0)); }
    MockedStatic<LocalDate> fixedDate() {
        MockedStatic<LocalDate> dates = mockStatic(LocalDate.class, CALLS_REAL_METHODS);
        dates.when(() -> LocalDate.now(zone)).thenReturn(today);
        return dates;
    }

    @ParameterizedTest @EnumSource(value=ClinicalFormContext.class, names={"LAB_RESULT","IMAGING_RESULT","ECG_RESULT"})
    void createMakesFirstDraftWithAuthorAndNormalizedCode(ClinicalFormContext context) {
        StaffInfo staff = actor(); saveVersions();
        when(templateRepo.save(any())).thenAnswer(call -> call.getArgument(0));
        var response = service.create(new ClinicalFormTemplateRequest(" custom_lab ", " Mẫu mới ", context,
                "Mô tả", schema, " Tạo mẫu ", today));
        assertEquals("CUSTOM_LAB", response.code()); assertEquals("Mẫu mới", response.name());
        assertEquals(1, response.versionNo()); assertEquals(ClinicalTemplateStatus.DRAFT, response.status());
        assertEquals("Tạo mẫu", response.changeReason()); assertEquals(schema, response.schemaJson());
        ArgumentCaptor<ClinicalFormTemplateVersion> capture = ArgumentCaptor.forClass(ClinicalFormTemplateVersion.class);
        verify(versionRepo).save(capture.capture()); assertSame(staff, capture.getValue().getCreatedBy());
        verify(engine).validateSchema(schema);
    }

    @Test void createRejectsDuplicateCodeBeforeWriting() {
        when(templateRepo.existsByCodeIgnoreCase("CUSTOM_LAB")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(new ClinicalFormTemplateRequest("custom_lab", "Mẫu",
                ClinicalFormContext.LAB_RESULT, null, schema, "Tạo", null)));
        verify(templateRepo, never()).save(any()); verifyNoInteractions(versionRepo);
    }

    @Test void examinationFormsCannotBeCreatedOrListed() {
        assertThrows(BadRequestException.class, () -> service.create(new ClinicalFormTemplateRequest("EXAM", "Khám",
                ClinicalFormContext.EXAMINATION, null, schema, "Tạo", null)));
        verifyNoInteractions(engine);
        ClinicalFormTemplate old = template(); old.setContext(ClinicalFormContext.EXAMINATION);
        ClinicalFormTemplate current = template();
        when(templateRepo.findAll()).thenReturn(List.of(old, current));
        var results = service.list(); assertEquals(1, results.size());
        assertEquals(current.getTemplateId(), results.get(0).templateId()); assertNull(results.get(0).versionId());
    }

    @ParameterizedTest @ValueSource(strings={"draft", "published", "none"})
    void saveDraftReusesOnlyDraftAndNeverOverwritesPublishedVersion(String state) {
        ClinicalFormTemplate t = template(); lookup(t); saveVersions();
        ClinicalFormTemplateVersion previous = state.equals("none") ? null : version(t, 3,
                state.equals("draft") ? ClinicalTemplateStatus.DRAFT : ClinicalTemplateStatus.PUBLISHED);
        when(versionRepo.findFirstByTemplate_TemplateIdOrderByVersionNoDesc(t.getTemplateId())).thenReturn(Optional.ofNullable(previous));
        if (!state.equals("draft")) actor();
        var response = service.saveDraft(t.getTemplateId(), new ClinicalFormDraftRequest(schema, " Bổ sung ", today));
        assertEquals(state.equals("none") ? 1 : state.equals("draft") ? 3 : 4, response.versionNo());
        assertEquals(ClinicalTemplateStatus.DRAFT, response.status()); assertEquals("Bổ sung", response.changeReason());
        if (state.equals("published")) assertEquals(ClinicalTemplateStatus.PUBLISHED, previous.getStatus());
        if (state.equals("draft")) verifyNoInteractions(authService);
    }

    @ParameterizedTest @ValueSource(strings={"LAB_CBC","LAB_GLUCOSE","LAB_BIOCHEM","LAB_LIVER","LAB_KIDNEY","LAB_URINALYSIS","LAB_CRP","LAB_RAPID_INFECTIOUS"})
    void systemTemplatesCannotBeChangedByManager(String code) {
        ClinicalFormTemplate t = template(); t.setCode(code); lookup(t);
        assertThrows(ConflictException.class, () -> service.saveDraft(t.getTemplateId(), new ClinicalFormDraftRequest(schema, "Sửa", null)));
        assertThrows(ConflictException.class, () -> service.publish(t.getTemplateId()));
        assertThrows(ConflictException.class, () -> service.retire(t.getTemplateId()));
        assertThrows(ConflictException.class, () -> service.bindServices(t.getTemplateId(), new ClinicalFormBindingRequest(List.of())));
        verifyNoInteractions(versionRepo, bindingRepo, serviceRepo);
    }

    @ParameterizedTest @ValueSource(strings={"missing", "examination"})
    void invalidTemplatesCannotBeMutated(String state) {
        ClinicalFormTemplate t = template();
        if (state.equals("examination")) { t.setContext(ClinicalFormContext.EXAMINATION); lookup(t); }
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.retire(t.getTemplateId()));
        assertTrue(state.equals("missing") ? error instanceof ResourceNotFoundException : error instanceof BadRequestException);
        verifyNoInteractions(versionRepo);
    }

    @ParameterizedTest @ValueSource(strings={"null", "today", "future"})
    void publishRetiresPreviousOnlyWhenNewVersionIsEffective(String effective) {
        ClinicalFormTemplate t = template(); lookup(t); t.setActive(false); saveVersions();
        ClinicalFormTemplateVersion draft = version(t, 2, ClinicalTemplateStatus.DRAFT);
        draft.setEffectiveFrom(effective.equals("null") ? null : effective.equals("future") ? today.plusDays(1) : today);
        ClinicalFormTemplateVersion previous = version(t, 1, ClinicalTemplateStatus.PUBLISHED);
        when(versionRepo.findFirstByTemplate_TemplateIdAndStatusOrderByVersionNoDesc(t.getTemplateId(), ClinicalTemplateStatus.DRAFT)).thenReturn(Optional.of(draft));
        if (!effective.equals("future")) when(versionRepo.findFirstByTemplate_TemplateIdAndStatusOrderByVersionNoDesc(t.getTemplateId(), ClinicalTemplateStatus.PUBLISHED)).thenReturn(Optional.of(previous));
        StaffInfo staff = actor();
        try (var ignored = fixedDate()) { service.publish(t.getTemplateId()); }
        assertEquals(ClinicalTemplateStatus.PUBLISHED, draft.getStatus()); assertTrue(t.getActive());
        assertSame(staff, draft.getPublishedBy()); assertNotNull(draft.getPublishedAt());
        assertEquals(effective.equals("future") ? today.plusDays(1) : today, draft.getEffectiveFrom());
        assertEquals(effective.equals("future") ? ClinicalTemplateStatus.PUBLISHED : ClinicalTemplateStatus.RETIRED, previous.getStatus());
        if (effective.equals("future")) verify(versionRepo, never()).save(previous);
    }

    @Test void pastEffectiveDateCannotBePublished() {
        ClinicalFormTemplate t = template(); lookup(t);
        ClinicalFormTemplateVersion draft = version(t, 2, ClinicalTemplateStatus.DRAFT); draft.setEffectiveFrom(today.minusDays(1));
        when(versionRepo.findFirstByTemplate_TemplateIdAndStatusOrderByVersionNoDesc(t.getTemplateId(), ClinicalTemplateStatus.DRAFT)).thenReturn(Optional.of(draft));
        try (var ignored = fixedDate()) { assertThrows(BadRequestException.class, () -> service.publish(t.getTemplateId())); }
        assertEquals(ClinicalTemplateStatus.DRAFT, draft.getStatus()); verify(versionRepo, never()).save(any());
    }

    @Test void publishAndRetireRequireCorrespondingVersion() {
        ClinicalFormTemplate t = template(); lookup(t);
        assertThrows(ConflictException.class, () -> service.publish(t.getTemplateId()));
        assertThrows(ConflictException.class, () -> service.retire(t.getTemplateId()));
        verify(versionRepo, never()).save(any());
    }

    @Test void retireDeactivatesTemplateAndRetainsVersionData() {
        ClinicalFormTemplate t = template(); lookup(t); saveVersions();
        var published = version(t, 2, ClinicalTemplateStatus.PUBLISHED);
        when(versionRepo.findFirstByTemplate_TemplateIdAndStatusOrderByVersionNoDesc(t.getTemplateId(), ClinicalTemplateStatus.PUBLISHED)).thenReturn(Optional.of(published));
        var result = service.retire(t.getTemplateId()); assertFalse(t.getActive());
        assertEquals(ClinicalTemplateStatus.RETIRED, result.status()); assertEquals(schema, result.schemaJson());
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void creatingVersionRequiresExistingActor(boolean authenticated) {
        ClinicalFormTemplate t = template(); lookup(t);
        if (authenticated) when(authService.currentStaffId()).thenReturn(UUID.randomUUID());
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.saveDraft(t.getTemplateId(), new ClinicalFormDraftRequest(schema, "Tạo", null)));
        assertTrue(authenticated ? error instanceof ResourceNotFoundException : error instanceof BadRequestException);
        verify(versionRepo, never()).save(any());
    }

    MedicalService lab() {
        return MedicalService.builder().serviceId(UUID.randomUUID()).name("Xét nghiệm")
                .departmentType(DepartmentType.PARACLINICAL).build();
    }

    @Test void bindingDeduplicatesIdsAndReassignsExistingBindings() {
        ClinicalFormTemplate t = template(); lookup(t);
        MedicalService first = lab(), second = lab();
        var existing = MedicalServiceFormTemplate.builder().service(first).template(template()).build();
        Set<UUID> ids = new LinkedHashSet<>(List.of(first.getServiceId(), second.getServiceId()));
        when(serviceRepo.findAllById(ids)).thenReturn(List.of(first, second));
        when(bindingRepo.findByService_ServiceId(first.getServiceId())).thenReturn(Optional.of(existing));
        when(bindingRepo.findByTemplate_TemplateId(t.getTemplateId())).thenReturn(List.of(existing));
        var result = service.bindServices(t.getTemplateId(), new ClinicalFormBindingRequest(
                List.of(first.getServiceId(), second.getServiceId(), first.getServiceId())));
        assertSame(t, existing.getTemplate()); assertEquals(List.of(first.getServiceId()), result.serviceIds());
        ArgumentCaptor<MedicalServiceFormTemplate> capture = ArgumentCaptor.forClass(MedicalServiceFormTemplate.class);
        verify(bindingRepo, times(2)).save(capture.capture());
        assertSame(second, capture.getAllValues().get(1).getService());
        assertSame(t, capture.getAllValues().get(1).getTemplate());
        var order = inOrder(bindingRepo); order.verify(bindingRepo).deleteByTemplate_TemplateId(t.getTemplateId()); order.verify(bindingRepo).flush();
    }

    @ParameterizedTest @ValueSource(strings={"missing", "examination", "untyped"})
    void invalidServiceBindingDoesNotRemoveExistingBindings(String condition) {
        ClinicalFormTemplate t = template(); lookup(t); MedicalService lab = lab();
        if (!condition.equals("missing")) {
            lab.setDepartmentType(condition.equals("untyped") ? null : DepartmentType.EXAMINATION);
            when(serviceRepo.findAllById(Set.of(lab.getServiceId()))).thenReturn(List.of(lab));
        }
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.bindServices(t.getTemplateId(), new ClinicalFormBindingRequest(List.of(lab.getServiceId()))));
        assertTrue(condition.equals("missing") ? error instanceof ResourceNotFoundException : error instanceof BadRequestException);
        verifyNoInteractions(bindingRepo);
    }

    @ParameterizedTest @ValueSource(strings={"missingService", "examination", "untyped", "missingVersion", "missingBinding", "foreignVersion", "legacyBinding", "nullTemplate", "noPublished"})
    void resolveRejectsUnavailableOrUnrelatedForms(String condition) {
        MedicalService lab = lab(); ClinicalFormTemplate t = template();
        var selected = version(t, 1, ClinicalTemplateStatus.PUBLISHED);
        UUID versionId = condition.equals("noPublished") ? null : selected.getVersionId();
        if (!condition.equals("missingService")) {
            if (condition.equals("examination")) lab.setDepartmentType(DepartmentType.EXAMINATION);
            if (condition.equals("untyped")) lab.setDepartmentType(null);
            when(serviceRepo.findById(lab.getServiceId())).thenReturn(Optional.of(lab));
        }
        if (!Set.of("missingService","examination","untyped","missingVersion").contains(condition)) {
            if (versionId != null) when(versionRepo.findById(versionId)).thenReturn(Optional.of(selected));
            if (!condition.equals("missingBinding")) {
                ClinicalFormTemplate bound = condition.equals("foreignVersion") ? template() : t;
                if (condition.equals("legacyBinding")) bound.setContext(ClinicalFormContext.EXAMINATION);
                if (condition.equals("nullTemplate")) bound = null;
                when(bindingRepo.findByService_ServiceId(lab.getServiceId())).thenReturn(Optional.of(
                        MedicalServiceFormTemplate.builder().service(lab).template(bound).build()));
            }
        }
        try (var ignored = fixedDate()) {
            RuntimeException error = assertThrows(RuntimeException.class, () -> service.resolveVersion(lab.getServiceId(), versionId));
            assertTrue(error instanceof ResourceNotFoundException || error instanceof BadRequestException);
        }
        verify(versionRepo, never()).save(any());
    }

    @Test void resolveUsesExplicitHistoricalVersionOrCurrentlyEffectiveVersion() {
        MedicalService lab = lab(); ClinicalFormTemplate t = template();
        var historical = version(t, 1, ClinicalTemplateStatus.RETIRED);
        var current = version(t, 2, ClinicalTemplateStatus.PUBLISHED);
        when(serviceRepo.findById(lab.getServiceId())).thenReturn(Optional.of(lab));
        when(bindingRepo.findByService_ServiceId(lab.getServiceId())).thenReturn(Optional.of(
                MedicalServiceFormTemplate.builder().service(lab).template(t).build()));
        when(versionRepo.findById(historical.getVersionId())).thenReturn(Optional.of(historical));
        assertSame(historical, service.resolveVersion(lab.getServiceId(), historical.getVersionId()));
        when(versionRepo.findFirstByTemplate_TemplateIdAndStatusAndEffectiveFromLessThanEqualOrderByVersionNoDesc(t.getTemplateId(), ClinicalTemplateStatus.PUBLISHED, today))
                .thenReturn(Optional.of(current));
        try (var ignored = fixedDate()) { assertSame(current, service.resolveVersion(lab.getServiceId(), null)); }
        assertEquals(schema, service.schemaForService(null, current));
        var response = service.resolvedResponse(current, schema);
        assertEquals(current.getVersionId(), response.templateVersionId());
    }

    @Test void invalidSchemaStopsDraftBeforePersisting() {
        doThrow(new BadRequestException("Schema không hợp lệ")).when(engine).validateSchema(schema);
        assertThrows(BadRequestException.class, () -> service.saveDraft(UUID.randomUUID(), new ClinicalFormDraftRequest(schema, "Sửa", null)));
        verifyNoInteractions(templateRepo, versionRepo, bindingRepo);
    }

    @Test void cleanupRetiresOnlyOlderEffectiveVersionsWithinEachTemplate() {
        ClinicalFormTemplate a = template(), b = template();
        var old = version(a, 1, ClinicalTemplateStatus.PUBLISHED);
        var current = version(a, 3, ClinicalTemplateStatus.PUBLISHED);
        var independent = version(b, 1, ClinicalTemplateStatus.PUBLISHED);
        when(versionRepo.findByStatusAndEffectiveFromLessThanEqual(ClinicalTemplateStatus.PUBLISHED, today)).thenReturn(List.of(old, current, independent));
        try (var ignored = fixedDate()) { service.retireSupersededEffectiveVersions(); }
        verify(versionRepo).save(old); verify(versionRepo, never()).save(current); verify(versionRepo, never()).save(independent);
        assertEquals(ClinicalTemplateStatus.RETIRED, old.getStatus()); assertEquals(ClinicalTemplateStatus.PUBLISHED, independent.getStatus());
    }
}
