package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.dto.medicalservice.MedicalServiceResponse;
import vn.edu.fpt.cares.dto.medicalservice.ServiceSelectionResolutionResponse;
import vn.edu.fpt.cares.enums.ServiceRelationType;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.MedicalService;
import vn.edu.fpt.cares.repository.MedicalServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Nguồn quy tắc duy nhất cho các dịch vụ bao gồm/trùng nhau.
 * Dùng serviceCode để quy tắc không phụ thuộc UUID của từng database.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalServiceSelectionPolicyService {
    private static final List<Rule> RULES = buildRules();

    private static List<Rule> buildRules() {
        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("LAB-003", "Sinh hóa máu cơ bản", "LAB-002", "Đường huyết",
                ServiceRelationType.INCLUDES,
                "Đường huyết đã được bao gồm trong Sinh hóa máu cơ bản"));
        LaboratoryAnalyteCatalog.PANELS.forEach(panel -> panel.analytes().forEach(analyte ->
                rules.add(new Rule(panel.serviceCode(), panel.name(), analyte.serviceCode(), analyte.name(),
                        ServiceRelationType.INCLUDES,
                        analyte.name() + " đã được bao gồm trong " + panel.name()))));
        return List.copyOf(rules);
    }

    private final MedicalServiceRepository serviceRepository;

    public static List<Rule> relationsFor(String serviceCode) {
        if (serviceCode == null) return List.of();
        return RULES.stream().filter(rule -> rule.sourceCode().equalsIgnoreCase(serviceCode)
                || (rule.type() != ServiceRelationType.INCLUDES
                && rule.targetCode().equalsIgnoreCase(serviceCode))).toList();
    }

    public Resolution resolve(Collection<UUID> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một dịch vụ");
        }
        LinkedHashSet<UUID> distinctIds = new LinkedHashSet<>();
        for (UUID id : requestedIds) {
            if (id == null) throw new BadRequestException("Danh sách dịch vụ không hợp lệ");
            distinctIds.add(id);
        }

        Map<UUID, MedicalService> byId = new LinkedHashMap<>();
        serviceRepository.findAllById(distinctIds).forEach(service -> byId.put(service.getServiceId(), service));
        if (byId.size() != distinctIds.size()) {
            UUID missing = distinctIds.stream().filter(id -> !byId.containsKey(id)).findFirst().orElse(null);
            throw new ResourceNotFoundException("Dịch vụ không tồn tại: " + missing);
        }

        Map<String, MedicalService> selectedByCode = new LinkedHashMap<>();
        distinctIds.forEach(id -> {
            MedicalService service = byId.get(id);
            selectedByCode.put(service.getServiceCode().toUpperCase(java.util.Locale.ROOT), service);
        });
        List<ServiceSelectionResolutionResponse.Adjustment> removed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();

        for (Rule rule : RULES) {
            MedicalService source = selectedByCode.get(rule.sourceCode());
            MedicalService target = selectedByCode.get(rule.targetCode());
            if (source == null || target == null) continue;
            if (rule.type() == ServiceRelationType.INCLUDES) {
                selectedByCode.remove(rule.targetCode());
                removed.add(new ServiceSelectionResolutionResponse.Adjustment(
                        target.getServiceId(), target.getServiceCode(), target.getName(),
                        source.getServiceId(), source.getServiceCode(), source.getName(), rule.message()));
            } else if (rule.type() == ServiceRelationType.MUTUALLY_EXCLUSIVE) {
                conflicts.add(rule.message());
            } else {
                warnings.add(rule.message());
            }
        }

        // Nếu người dùng chọn đủ toàn bộ chỉ số lẻ của một gói thì tự áp dụng
        // giá gói cố định. Việc thay thế diễn ra trước khi lập hóa đơn nên không
        // làm thay đổi dữ liệu lịch sử đã phát sinh.
        for (LaboratoryAnalyteCatalog.Panel panel : LaboratoryAnalyteCatalog.PANELS) {
            if (selectedByCode.containsKey(panel.serviceCode())) continue;
            boolean hasFullPanel = panel.analytes().stream()
                    .allMatch(analyte -> selectedByCode.containsKey(analyte.serviceCode()));
            if (!hasFullPanel) continue;
            MedicalService panelService = serviceRepository.findByServiceCode(panel.serviceCode()).orElse(null);
            if (panelService == null) continue;
            panel.analytes().forEach(analyte -> {
                MedicalService removedService = selectedByCode.remove(analyte.serviceCode());
                if (removedService != null) {
                    removed.add(new ServiceSelectionResolutionResponse.Adjustment(
                            removedService.getServiceId(), removedService.getServiceCode(), removedService.getName(),
                            panelService.getServiceId(), panelService.getServiceCode(), panelService.getName(),
                            "Đã chọn đủ chỉ số nên hệ thống áp dụng giá gói " + panel.name()));
                }
            });
            selectedByCode.put(panel.serviceCode(), panelService);
            warnings.add("Đã áp dụng giá gói " + panel.name() + " vì đã chọn đủ các chỉ số");
        }
        return new Resolution(List.copyOf(selectedByCode.values()), List.copyOf(removed),
                List.copyOf(warnings), List.copyOf(conflicts));
    }

    public List<MedicalService> normalizeOrThrow(Collection<UUID> requestedIds) {
        Resolution resolution = resolve(requestedIds);
        if (!resolution.conflicts().isEmpty()) {
            throw new BadRequestException(String.join("; ", resolution.conflicts()));
        }
        return resolution.services();
    }

    /**
     * Không tự loại dịch vụ đã tồn tại trong một lượt khám vì việc đó có thể
     * làm thay đổi hóa đơn hoặc kết quả đã phát sinh. Thay vào đó, chặn dịch vụ
     * mới có quan hệ bao gồm/xung đột với dữ liệu cũ và yêu cầu người dùng kiểm tra lại.
     */
    public void validateAgainstExisting(Collection<UUID> requestedIds, Collection<UUID> existingIds) {
        if (requestedIds == null || requestedIds.isEmpty() || existingIds == null || existingIds.isEmpty()) {
            return;
        }
        LinkedHashSet<UUID> allIds = new LinkedHashSet<>();
        allIds.addAll(requestedIds);
        allIds.addAll(existingIds);
        Map<UUID, MedicalService> byId = new LinkedHashMap<>();
        serviceRepository.findAllById(allIds).forEach(service -> byId.put(service.getServiceId(), service));

        Set<String> requestedCodes = requestedIds.stream()
                .map(byId::get).filter(java.util.Objects::nonNull)
                .map(service -> service.getServiceCode().toUpperCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> existingCodes = existingIds.stream()
                .map(byId::get).filter(java.util.Objects::nonNull)
                .map(service -> service.getServiceCode().toUpperCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());

        for (Rule rule : RULES) {
            boolean splitAcrossOldAndNew = (requestedCodes.contains(rule.sourceCode())
                    && existingCodes.contains(rule.targetCode()))
                    || (requestedCodes.contains(rule.targetCode())
                    && existingCodes.contains(rule.sourceCode()));
            if (splitAcrossOldAndNew && rule.type() != ServiceRelationType.SIMILAR) {
                String message = requestedCodes.contains(rule.sourceCode())
                        && existingCodes.contains(rule.targetCode())
                        ? "Không thể chọn " + rule.sourceName() + " vì " + rule.targetName()
                        + " đã có trong lượt khám"
                        : rule.message();
                throw new ConflictException(message
                        + ". Một dịch vụ liên quan đã có trong lượt khám nên hệ thống không tự sửa dữ liệu cũ");
            }
        }
    }

    public ServiceSelectionResolutionResponse resolveResponse(Collection<UUID> requestedIds) {
        Resolution resolution = resolve(requestedIds);
        return new ServiceSelectionResolutionResponse(
                resolution.services().stream().map(MedicalService::getServiceId).toList(),
                resolution.services().stream().map(MedicalServiceResponse::from).toList(),
                resolution.removed(), resolution.warnings(), resolution.conflicts());
    }

    public record Rule(String sourceCode, String sourceName, String targetCode, String targetName,
                       ServiceRelationType type, String message) {}

    public record Resolution(List<MedicalService> services,
                             List<ServiceSelectionResolutionResponse.Adjustment> removed,
                             List<String> warnings, List<String> conflicts) {}
}
