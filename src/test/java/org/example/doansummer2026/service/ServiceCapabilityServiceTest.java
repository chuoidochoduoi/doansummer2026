package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.capability.ServiceCapabilityRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.ServiceCapability;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ServiceCapabilityRepository;
import org.example.doansummer2026.repository.StaffCapabilityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceCapabilityServiceTest {

    @Mock ServiceCapabilityRepository repository;
    @Mock MedicalServiceRepository medicalServiceRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock StaffCapabilityRepository staffCapabilityRepository;
    @InjectMocks ServiceCapabilityService service;

    @Test
    void listMapsValuesInRepositoryOrder() {
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(
                capability(UUID.randomUUID(), "LAB", "Xét nghiệm", true),
                capability(UUID.randomUUID(), "IMG", "Chẩn đoán hình ảnh", false)));

        var result = service.list();

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals("LAB", result.get(0).code()),
                () -> assertFalse(result.get(1).active()));
    }

    @Test
    void createNormalizesCodeAndDefaultsActive() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(new ServiceCapabilityRequest(" lab ", " Xét nghiệm ", "Mô tả", null));

        assertAll(
                () -> assertEquals("LAB", result.code()),
                () -> assertEquals("Xét nghiệm", result.name()),
                () -> assertEquals("Mô tả", result.description()),
                () -> assertTrue(result.active()));
    }

    @Test
    void createAcceptsExplicitActiveValue() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(new ServiceCapabilityRequest("img", "Chẩn đoán hình ảnh", null, true));

        assertTrue(result.active());
    }

    @Test
    void createRejectsDuplicateCodeBeforeCheckingName() {
        when(repository.existsByCodeIgnoreCase("dup")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.create(new ServiceCapabilityRequest("dup", "Tên", null, true)));
        verify(repository, never()).existsByNameIgnoreCase(any());
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateName() {
        when(repository.existsByNameIgnoreCase("Trùng")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.create(new ServiceCapabilityRequest("NEW", "Trùng", null, false)));
        verify(repository, never()).save(any());
    }

    @Test
    void updateNormalizesValuesAndChangesActive() {
        UUID id = UUID.randomUUID();
        ServiceCapability value = capability(id, "OLD", "Cũ", false);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(repository.save(value)).thenReturn(value);

        var result = service.update(id,
                new ServiceCapabilityRequest(" new ", " Tên   mới ", "Chi tiết", true));

        assertAll(
                () -> assertEquals("NEW", result.code()),
                () -> assertEquals("Tên mới", result.name()),
                () -> assertEquals("Chi tiết", result.description()),
                () -> assertTrue(result.active()));
    }

    @Test
    void updateRejectsDuplicateCodeAndDuplicateName() {
        UUID id = UUID.randomUUID();
        ServiceCapability value = capability(id, "OLD", "Cũ", true);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(repository.existsByCodeIgnoreCaseAndCapabilityIdNot("DUP", id)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(id,
                new ServiceCapabilityRequest("dup", "Tên", null, null)));

        reset(repository);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(repository.existsByNameIgnoreCaseAndCapabilityIdNot("Tên trùng", id)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.update(id,
                new ServiceCapabilityRequest("OK", " Tên  trùng ", null, null)));
    }

    @Test
    void deactivateRejectsAnyActiveReference() {
        UUID id = UUID.randomUUID();
        ServiceCapability value = capability(id, "LAB", "Lab", true);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(medicalServiceRepository.countActiveReferencesToCapability(id)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.update(id,
                new ServiceCapabilityRequest("LAB", "Lab", null, false)));
        assertTrue(value.getActive());
    }

    @Test
    void deactivateChecksDepartmentAndStaffReferencesToo() {
        UUID id = UUID.randomUUID();
        ServiceCapability value = capability(id, "LAB", "Lab", true);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(departmentRepository.countReferencesToCapability(id)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.update(id,
                new ServiceCapabilityRequest("LAB", "Lab", null, false)));

        reset(departmentRepository, staffCapabilityRepository);
        when(staffCapabilityRepository.countActiveReferencesToCapability(id)).thenReturn(1L);
        assertThrows(ConflictException.class, () -> service.update(id,
                new ServiceCapabilityRequest("LAB", "Lab", null, false)));
    }

    @Test
    void deactivateWithoutReferencesAndNullActiveArePersisted() {
        UUID id = UUID.randomUUID();
        ServiceCapability value = capability(id, "LAB", "Lab", true);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(repository.save(value)).thenReturn(value);

        assertFalse(service.update(id,
                new ServiceCapabilityRequest("LAB", "Lab", null, false)).active());
        assertFalse(service.update(id,
                new ServiceCapabilityRequest("LAB", "Lab", null, null)).active());
        assertFalse(service.update(id,
                new ServiceCapabilityRequest("LAB", "Lab", null, false)).active());
        verify(repository, times(3)).save(value);
    }

    @Test
    void deleteRejectsReferencesAndDeletesUnusedValue() {
        UUID usedId = UUID.randomUUID();
        UUID unusedId = UUID.randomUUID();
        ServiceCapability used = capability(usedId, "A", "A", true);
        ServiceCapability unused = capability(unusedId, "B", "B", true);
        when(repository.findById(usedId)).thenReturn(Optional.of(used));
        when(repository.findById(unusedId)).thenReturn(Optional.of(unused));
        when(medicalServiceRepository.countActiveReferencesToCapability(usedId)).thenReturn(2L);

        assertThrows(ConflictException.class, () -> service.delete(usedId));
        service.delete(unusedId);

        verify(repository).delete(unused);
        verify(repository, never()).delete(used);
    }

    @Test
    void deleteRejectsDepartmentAndStaffReferencesIndependently() {
        UUID departmentUsedId = UUID.randomUUID();
        UUID staffUsedId = UUID.randomUUID();
        ServiceCapability departmentUsed = capability(departmentUsedId, "D", "Department", true);
        ServiceCapability staffUsed = capability(staffUsedId, "S", "Staff", true);
        when(repository.findById(departmentUsedId)).thenReturn(Optional.of(departmentUsed));
        when(repository.findById(staffUsedId)).thenReturn(Optional.of(staffUsed));
        when(departmentRepository.countReferencesToCapability(any(UUID.class)))
                .thenAnswer(invocation -> departmentUsedId.equals(invocation.getArgument(0)) ? 1L : 0L);
        when(staffCapabilityRepository.countActiveReferencesToCapability(any(UUID.class)))
                .thenAnswer(invocation -> staffUsedId.equals(invocation.getArgument(0)) ? 1L : 0L);

        assertThrows(ConflictException.class, () -> service.delete(departmentUsedId));
        assertThrows(ConflictException.class, () -> service.delete(staffUsedId));
        verify(repository, never()).delete(any());
    }

    @Test
    void findAndFindActiveHandleMissingInactiveAndActiveValues() {
        UUID missing = UUID.randomUUID();
        UUID inactive = UUID.randomUUID();
        UUID active = UUID.randomUUID();
        ServiceCapability activeValue = capability(active, "A", "A", true);
        when(repository.findById(missing)).thenReturn(Optional.empty());
        when(repository.findById(inactive)).thenReturn(Optional.of(capability(inactive, "I", "I", false)));
        when(repository.findById(active)).thenReturn(Optional.of(activeValue));

        assertThrows(ResourceNotFoundException.class, () -> service.find(missing));
        assertThrows(ConflictException.class, () -> service.findActive(inactive));
        assertSame(activeValue, service.findActive(active));
    }

    private ServiceCapability capability(UUID id, String code, String name, Boolean active) {
        return ServiceCapability.builder().capabilityId(id).code(code).name(name).active(active).build();
    }
}
