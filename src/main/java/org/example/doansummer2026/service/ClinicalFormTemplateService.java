package org.example.doansummer2026.service;

import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.clinicalform.*;
import org.example.doansummer2026.enums.ClinicalFormContext;
import org.example.doansummer2026.enums.ClinicalTemplateStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClinicalFormTemplateService {
    private static final java.time.ZoneId CLINIC_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<ClinicalFormContext> RESULT_CONTEXTS = EnumSet.of(
            ClinicalFormContext.LAB_RESULT,
            ClinicalFormContext.IMAGING_RESULT,
            ClinicalFormContext.ECG_RESULT);
    private static final Set<String> SYSTEM_LAB_TEMPLATES = Set.of(
            "LAB_CBC", "LAB_GLUCOSE", "LAB_BIOCHEM", "LAB_LIVER",
            "LAB_KIDNEY", "LAB_URINALYSIS", "LAB_CRP", "LAB_RAPID_INFECTIOUS");
    private final ClinicalFormTemplateRepository templateRepo;
    private final ClinicalFormTemplateVersionRepository versionRepo;
    private final MedicalServiceFormTemplateRepository bindingRepo;
    private final MedicalServiceRepository serviceRepo;
    private final StaffInfoRepository staffRepo;
    private final AuthService authService;
    private final ClinicalFormEngine engine;

    @Transactional(readOnly = true)
    public List<ClinicalFormTemplateResponse> list() {
        return templateRepo.findAll().stream()
                .filter(t -> RESULT_CONTEXTS.contains(t.getContext()))
                .map(t -> response(t,
                versionRepo.findFirstByTemplate_TemplateIdOrderByVersionNoDesc(t.getTemplateId()).orElse(null))).toList();
    }

    @Transactional
    public ClinicalFormTemplateResponse create(ClinicalFormTemplateRequest req) {
        ensureResultContext(req.context());
        engine.validateSchema(req.schemaJson());
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if (templateRepo.existsByCodeIgnoreCase(code)) throw new ConflictException("Mã template đã tồn tại: " + code);
        ClinicalFormTemplate template = templateRepo.save(ClinicalFormTemplate.builder()
                .code(code).name(req.name().trim()).context(req.context()).description(req.description()).active(true).build());
        ClinicalFormTemplateVersion version = versionRepo.save(ClinicalFormTemplateVersion.builder()
                .template(template).versionNo(1).schemaJson(req.schemaJson()).status(ClinicalTemplateStatus.DRAFT)
                .changeReason(req.changeReason().trim()).effectiveFrom(req.effectiveFrom()).createdBy(currentStaff()).build());
        return response(template, version);
    }

    @Transactional
    public ClinicalFormTemplateResponse saveDraft(UUID templateId, ClinicalFormDraftRequest req) {
        engine.validateSchema(req.schemaJson());
        ClinicalFormTemplate template = findTemplate(templateId);
        ensureResultTemplate(template);
        ensureNotSystemLabTemplate(template);
        ClinicalFormTemplateVersion latest = versionRepo.findFirstByTemplate_TemplateIdOrderByVersionNoDesc(templateId).orElse(null);
        ClinicalFormTemplateVersion draft;
        if (latest != null && latest.getStatus() == ClinicalTemplateStatus.DRAFT) {
            draft = latest;
            draft.setSchemaJson(req.schemaJson());
            draft.setChangeReason(req.changeReason().trim());
            draft.setEffectiveFrom(req.effectiveFrom());
        } else {
            draft = ClinicalFormTemplateVersion.builder().template(template)
                    .versionNo(latest == null ? 1 : latest.getVersionNo() + 1).schemaJson(req.schemaJson())
                    .status(ClinicalTemplateStatus.DRAFT).changeReason(req.changeReason().trim())
                    .effectiveFrom(req.effectiveFrom()).createdBy(currentStaff()).build();
        }
        return response(template, versionRepo.save(draft));
    }

    @Transactional
    public ClinicalFormTemplateResponse publish(UUID templateId) {
        ClinicalFormTemplate template = findTemplate(templateId);
        ensureResultTemplate(template);
        ensureNotSystemLabTemplate(template);
        ClinicalFormTemplateVersion draft = versionRepo.findFirstByTemplate_TemplateIdAndStatusOrderByVersionNoDesc(
                templateId, ClinicalTemplateStatus.DRAFT).orElseThrow(() -> new ConflictException("Không có bản nháp để phát hành"));
        engine.validateSchema(draft.getSchemaJson());
        LocalDate effective = draft.getEffectiveFrom() == null ? LocalDate.now(CLINIC_ZONE) : draft.getEffectiveFrom();
        if (effective.isBefore(LocalDate.now(CLINIC_ZONE))) throw new BadRequestException("Ngày áp dụng không được ở quá khứ");
        // A future version may coexist with the currently effective version. Resolution uses
        // effectiveFrom <= service date, so publishing ahead of time must not create a gap.
        if (!effective.isAfter(LocalDate.now(CLINIC_ZONE))) {
            versionRepo.findFirstByTemplate_TemplateIdAndStatusOrderByVersionNoDesc(templateId, ClinicalTemplateStatus.PUBLISHED)
                    .ifPresent(v -> { v.setStatus(ClinicalTemplateStatus.RETIRED); versionRepo.save(v); });
        }
        draft.setEffectiveFrom(effective);
        draft.setStatus(ClinicalTemplateStatus.PUBLISHED);
        draft.setPublishedBy(currentStaff());
        draft.setPublishedAt(LocalDateTime.now());
        template.setActive(true);
        templateRepo.save(template);
        return response(template, versionRepo.save(draft));
    }

    @Transactional
    public ClinicalFormTemplateResponse retire(UUID templateId) {
        ClinicalFormTemplate template = findTemplate(templateId);
        ensureResultTemplate(template);
        ensureNotSystemLabTemplate(template);
        ClinicalFormTemplateVersion published = versionRepo.findFirstByTemplate_TemplateIdAndStatusOrderByVersionNoDesc(
                templateId, ClinicalTemplateStatus.PUBLISHED).orElseThrow(() -> new ConflictException("Template chưa được phát hành"));
        published.setStatus(ClinicalTemplateStatus.RETIRED);
        template.setActive(false);
        templateRepo.save(template);
        return response(template, versionRepo.save(published));
    }

    @Transactional
    public ClinicalFormTemplateResponse bindServices(UUID templateId, ClinicalFormBindingRequest req) {
        ClinicalFormTemplate template = findTemplate(templateId);
        ensureResultTemplate(template);
        ensureNotSystemLabTemplate(template);
        Set<UUID> uniqueIds = new LinkedHashSet<>(req.serviceIds());
        List<MedicalService> services = serviceRepo.findAllById(uniqueIds);
        if (services.size() != uniqueIds.size()) throw new ResourceNotFoundException("Có dịch vụ không tồn tại");
        for (MedicalService service : services) {
            if (service.getDepartmentType() == null || !service.getDepartmentType().isParaclinical())
                throw new BadRequestException("Chỉ dịch vụ cận lâm sàng mới được liên kết biểu mẫu kết quả: "
                        + service.getName());
        }
        bindingRepo.deleteByTemplate_TemplateId(templateId);
        bindingRepo.flush();
        for (MedicalService service : services) {
            MedicalServiceFormTemplate binding = bindingRepo.findByService_ServiceId(service.getServiceId()).orElse(null);
            if (binding == null) binding = MedicalServiceFormTemplate.builder().service(service).template(template).build();
            else binding.setTemplate(template);
            bindingRepo.save(binding);
        }
        return response(template, versionRepo.findFirstByTemplate_TemplateIdOrderByVersionNoDesc(templateId).orElse(null));
    }

    @Transactional(readOnly = true)
    public ClinicalFormTemplateVersion resolveVersion(UUID serviceId, UUID requestedVersionId) {
        MedicalService service = serviceRepo.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId));
        if (service.getDepartmentType() == null || !service.getDepartmentType().isParaclinical())
            throw new BadRequestException("Dịch vụ khám bệnh không sử dụng biểu mẫu động");
        if (requestedVersionId != null) {
            ClinicalFormTemplateVersion version = versionRepo.findById(requestedVersionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Phiên bản form không tồn tại"));
            MedicalServiceFormTemplate binding = bindingRepo.findByService_ServiceId(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ chưa được cấu hình biểu mẫu kết quả"));
            ensureResultTemplate(binding.getTemplate());
            if (!binding.getTemplate().getTemplateId().equals(version.getTemplate().getTemplateId()))
                throw new BadRequestException("Phiên bản form không thuộc dịch vụ này");
            return version;
        }
        MedicalServiceFormTemplate binding = bindingRepo.findByService_ServiceId(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ chưa được cấu hình biểu mẫu kết quả"));
        ensureResultTemplate(binding.getTemplate());
        return versionRepo.findFirstByTemplate_TemplateIdAndStatusAndEffectiveFromLessThanEqualOrderByVersionNoDesc(
                binding.getTemplate().getTemplateId(), ClinicalTemplateStatus.PUBLISHED, LocalDate.now(CLINIC_ZONE))
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ chưa có phiên bản form đang áp dụng"));
    }

    public ResolvedClinicalFormResponse resolvedResponse(ClinicalFormTemplateVersion version, JsonNode values) {
        return resolvedResponse(version, values, null);
    }

    public ResolvedClinicalFormResponse resolvedResponse(ClinicalFormTemplateVersion version, JsonNode values,
                                                         String serviceCode) {
        ClinicalFormTemplate t = version.getTemplate();
        return new ResolvedClinicalFormResponse(t.getTemplateId(), version.getVersionId(), version.getVersionNo(),
                t.getCode(), t.getName(), t.getContext(),
                LaboratoryAnalyteCatalog.schemaForService(serviceCode, version.getSchemaJson()), values);
    }

    public JsonNode schemaForService(String serviceCode, ClinicalFormTemplateVersion version) {
        return LaboratoryAnalyteCatalog.schemaForService(serviceCode, version.getSchemaJson());
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 10 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void retireSupersededEffectiveVersions() {
        Map<UUID, List<ClinicalFormTemplateVersion>> byTemplate = versionRepo
                .findByStatusAndEffectiveFromLessThanEqual(ClinicalTemplateStatus.PUBLISHED, LocalDate.now(CLINIC_ZONE))
                .stream().collect(java.util.stream.Collectors.groupingBy(v -> v.getTemplate().getTemplateId()));
        byTemplate.values().forEach(versions -> {
            int newest = versions.stream().mapToInt(ClinicalFormTemplateVersion::getVersionNo).max().orElse(0);
            versions.stream().filter(version -> version.getVersionNo() < newest).forEach(version -> {
                version.setStatus(ClinicalTemplateStatus.RETIRED);
                versionRepo.save(version);
            });
        });
    }

    private ClinicalFormTemplateResponse response(ClinicalFormTemplate t, ClinicalFormTemplateVersion v) {
        List<UUID> serviceIds = bindingRepo.findByTemplate_TemplateId(t.getTemplateId()).stream()
                .map(b -> b.getService().getServiceId()).toList();
        return ClinicalFormTemplateResponse.from(t, v, serviceIds);
    }

    private ClinicalFormTemplate findTemplate(UUID id) {
        return templateRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Template không tồn tại: " + id));
    }

    private void ensureNotSystemLabTemplate(ClinicalFormTemplate template) {
        if (template != null && SYSTEM_LAB_TEMPLATES.contains(template.getCode()))
            throw new ConflictException("Biểu mẫu xét nghiệm hệ thống chỉ được cập nhật bằng phiên bản ứng dụng đã kiểm soát");
    }

    private void ensureResultContext(ClinicalFormContext context) {
        if (!RESULT_CONTEXTS.contains(context))
            throw new BadRequestException("Biểu mẫu khám chuyên khoa đã ngừng sử dụng; chỉ được cấu hình biểu mẫu kết quả cận lâm sàng");
    }

    private void ensureResultTemplate(ClinicalFormTemplate template) {
        if (template == null || !RESULT_CONTEXTS.contains(template.getContext()))
            throw new BadRequestException("Biểu mẫu khám chuyên khoa đã ngừng sử dụng");
    }

    private StaffInfo currentStaff() {
        UUID id = authService.currentStaffId();
        if (id == null) throw new BadRequestException("Không xác định được Clinic Manager đang đăng nhập");
        return staffRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên hiện tại"));
    }
}
