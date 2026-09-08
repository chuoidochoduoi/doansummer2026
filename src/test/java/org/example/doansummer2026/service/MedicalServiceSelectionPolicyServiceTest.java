package org.example.doansummer2026.service;

import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MedicalServiceSelectionPolicyServiceTest {

    @Test
    void sinhHoaBaoGomDuongHuyetBatKeThuTuLuaChon() {
        MedicalServiceRepository repository = mock(MedicalServiceRepository.class);
        MedicalService glucose = service("LAB-002", "Đường huyết");
        MedicalService biochemistry = service("LAB-003", "Sinh hóa máu cơ bản");
        when(repository.findAllById(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(glucose, biochemistry));
        MedicalServiceSelectionPolicyService policy = new MedicalServiceSelectionPolicyService(repository);

        var resolution = policy.resolve(List.of(glucose.getServiceId(), biochemistry.getServiceId()));

        assertEquals(List.of(biochemistry.getServiceId()),
                resolution.services().stream().map(MedicalService::getServiceId).toList());
        assertEquals(1, resolution.removed().size());
        assertEquals(glucose.getServiceId(), resolution.removed().get(0).serviceId());
        assertTrue(resolution.conflicts().isEmpty());
    }

    @Test
    void dichVuKhongLienQuanDuocGiuNguyen() {
        MedicalServiceRepository repository = mock(MedicalServiceRepository.class);
        MedicalService cbc = service("LAB-001", "Công thức máu");
        MedicalService biochemistry = service("LAB-003", "Sinh hóa máu cơ bản");
        when(repository.findAllById(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(cbc, biochemistry));
        MedicalServiceSelectionPolicyService policy = new MedicalServiceSelectionPolicyService(repository);

        var resolution = policy.resolve(List.of(cbc.getServiceId(), biochemistry.getServiceId()));

        assertEquals(2, resolution.services().size());
        assertTrue(resolution.removed().isEmpty());
    }

    @Test
    void khongTuSuaDichVuLienQuanDaTonTaiTrongLuot() {
        MedicalServiceRepository repository = mock(MedicalServiceRepository.class);
        MedicalService glucose = service("LAB-002", "Đường huyết");
        MedicalService biochemistry = service("LAB-003", "Sinh hóa máu cơ bản");
        when(repository.findAllById(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(glucose, biochemistry));
        MedicalServiceSelectionPolicyService policy = new MedicalServiceSelectionPolicyService(repository);

        assertThrows(org.example.doansummer2026.exception.ConflictException.class,
                () -> policy.validateAgainstExisting(
                        List.of(biochemistry.getServiceId()), List.of(glucose.getServiceId())));
    }

    @Test
    void chonDuChiSoLeTuDongApDungGiaGoi() {
        MedicalServiceRepository repository = mock(MedicalServiceRepository.class);
        var panelDefinition = LaboratoryAnalyteCatalog.panel("LAB-003").orElseThrow();
        MedicalService panel = service(panelDefinition.serviceCode(), panelDefinition.name());
        List<MedicalService> analytes = panelDefinition.analytes().stream()
                .map(item -> service(item.serviceCode(), item.name())).toList();
        when(repository.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(analytes);
        when(repository.findByServiceCode("LAB-003")).thenReturn(Optional.of(panel));
        MedicalServiceSelectionPolicyService policy = new MedicalServiceSelectionPolicyService(repository);

        var resolution = policy.resolve(analytes.stream().map(MedicalService::getServiceId).toList());

        assertEquals(List.of("LAB-003"), resolution.services().stream()
                .map(MedicalService::getServiceCode).toList());
        assertEquals(analytes.size(), resolution.removed().size());
        assertTrue(resolution.warnings().stream().anyMatch(message -> message.contains("giá gói")));
    }

    @Test
    void chonThieuMotChiSoVanTinhTheoGiaLe() {
        MedicalServiceRepository repository = mock(MedicalServiceRepository.class);
        var panelDefinition = LaboratoryAnalyteCatalog.panel("LAB-003").orElseThrow();
        List<MedicalService> analytes = panelDefinition.analytes().stream().limit(2)
                .map(item -> service(item.serviceCode(), item.name())).toList();
        when(repository.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(analytes);
        MedicalServiceSelectionPolicyService policy = new MedicalServiceSelectionPolicyService(repository);

        var resolution = policy.resolve(analytes.stream().map(MedicalService::getServiceId).toList());

        assertEquals(2, resolution.services().size());
        assertTrue(resolution.removed().isEmpty());
    }

    @Test
    void rejectsEmptyNullElementAndMissingServiceSelections() {
        MedicalServiceRepository repository = mock(MedicalServiceRepository.class);
        MedicalServiceSelectionPolicyService policy = new MedicalServiceSelectionPolicyService(repository);

        assertThrows(org.example.doansummer2026.exception.BadRequestException.class,
                () -> policy.resolve(null));
        assertThrows(org.example.doansummer2026.exception.BadRequestException.class,
                () -> policy.resolve(List.of()));
        assertThrows(org.example.doansummer2026.exception.BadRequestException.class,
                () -> policy.resolve(java.util.Arrays.asList(UUID.randomUUID(), null)));

        UUID missing = UUID.randomUUID();
        when(repository.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        var exception = assertThrows(org.example.doansummer2026.exception.ResourceNotFoundException.class,
                () -> policy.resolve(List.of(missing, missing)));
        assertTrue(exception.getMessage().contains(missing.toString()));
    }

    @Test
    void relationLookupNormalizeAndResponseCoverPublicContracts() {
        MedicalServiceRepository repository = mock(MedicalServiceRepository.class);
        MedicalService glucose = service("lab-002", "Đường huyết");
        when(repository.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(glucose));
        MedicalServiceSelectionPolicyService policy = new MedicalServiceSelectionPolicyService(repository);

        assertTrue(MedicalServiceSelectionPolicyService.relationsFor(null).isEmpty());
        assertTrue(MedicalServiceSelectionPolicyService.relationsFor("lab-003").stream()
                .anyMatch(rule -> rule.targetCode().equals("LAB-002")));
        assertEquals(List.of(glucose), policy.normalizeOrThrow(List.of(glucose.getServiceId())));
        var response = policy.resolveResponse(List.of(glucose.getServiceId()));
        assertEquals(List.of(glucose.getServiceId()), response.selectedServiceIds());
        assertEquals(1, response.selectedServices().size());
    }

    @Test
    void fullAnalytesRemainRetailWhenConfiguredPanelServiceIsUnavailable() {
        MedicalServiceRepository repository = mock(MedicalServiceRepository.class);
        var definition = LaboratoryAnalyteCatalog.panel("LAB-004").orElseThrow();
        List<MedicalService> analytes = definition.analytes().stream()
                .map(item -> service(item.serviceCode(), item.name())).toList();
        when(repository.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(analytes);
        when(repository.findByServiceCode(definition.serviceCode())).thenReturn(Optional.empty());
        MedicalServiceSelectionPolicyService policy = new MedicalServiceSelectionPolicyService(repository);

        var resolution = policy.resolve(analytes.stream().map(MedicalService::getServiceId).toList());

        assertEquals(analytes.size(), resolution.services().size());
        assertTrue(resolution.removed().isEmpty());
    }

    @Test
    void existingValidationHandlesEmptyUnknownAndBothDirections() {
        MedicalServiceRepository repository = mock(MedicalServiceRepository.class);
        MedicalService glucose = service("LAB-002", "Đường huyết");
        MedicalService panel = service("LAB-003", "Sinh hóa máu cơ bản");
        when(repository.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(glucose, panel));
        MedicalServiceSelectionPolicyService policy = new MedicalServiceSelectionPolicyService(repository);

        assertDoesNotThrow(() -> policy.validateAgainstExisting(null, List.of(glucose.getServiceId())));
        assertDoesNotThrow(() -> policy.validateAgainstExisting(List.of(), List.of(glucose.getServiceId())));
        assertDoesNotThrow(() -> policy.validateAgainstExisting(List.of(glucose.getServiceId()), null));
        assertDoesNotThrow(() -> policy.validateAgainstExisting(List.of(UUID.randomUUID()), List.of(UUID.randomUUID())));

        var inverse = assertThrows(org.example.doansummer2026.exception.ConflictException.class,
                () -> policy.validateAgainstExisting(List.of(glucose.getServiceId()), List.of(panel.getServiceId())));
        assertTrue(inverse.getMessage().contains("Đường huyết"));
    }

    private MedicalService service(String code, String name) {
        return MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode(code).name(name).build();
    }
}
