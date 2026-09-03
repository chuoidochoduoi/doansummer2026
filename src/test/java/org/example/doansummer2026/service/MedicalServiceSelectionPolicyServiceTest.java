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

    private MedicalService service(String code, String name) {
        return MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode(code).name(name).build();
    }
}
