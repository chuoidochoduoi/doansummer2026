package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.specialization.SpecializationCreateRequest;
import org.example.doansummer2026.dto.specialization.SpecializationUpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Specialization;
import org.example.doansummer2026.repository.SpecializationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecializationServiceTest {

    @Mock SpecializationRepository repository;
    @InjectMocks SpecializationService service;

    @Test
    void listAndGetMapRepositoryEntities() {
        UUID id = UUID.randomUUID();
        Specialization value = specialization(id, "Nội khoa", null, null);
        var pageable = PageRequest.of(0, 5);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(value), pageable, 1));
        when(repository.findById(id)).thenReturn(Optional.of(value));

        var page = service.list(pageable);
        var response = service.get(id);

        assertAll(
                () -> assertEquals(1, page.totalElements()),
                () -> assertEquals("Nội khoa", page.content().get(0).name()),
                () -> assertTrue(page.content().get(0).active()),
                () -> assertEquals(id, response.specializationId()));
    }

    @Test
    void createNormalizesValuesAndDefaultsActiveToTrue() {
        when(repository.save(any())).thenAnswer(invocation -> {
            Specialization saved = invocation.getArgument(0);
            saved.setSpecializationId(UUID.randomUUID());
            return saved;
        });

        var response = service.create(new SpecializationCreateRequest("  Tim   mạch  ", "   ", null));

        assertAll(
                () -> assertEquals("Tim mạch", response.name()),
                () -> assertNull(response.description()),
                () -> assertTrue(response.active()));
        verify(repository).existsByNameIgnoreCase("Tim mạch");
    }

    @Test
    void createKeepsNormalizedDescriptionAndExplicitInactive() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new SpecializationCreateRequest("  Da liễu ", "  Điều trị   da ", false));

        assertEquals("Điều trị da", response.description());
        assertFalse(response.active());
    }

    @Test
    void createAcceptsExplicitActiveAndNullDescription() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new SpecializationCreateRequest("Nhi khoa", null, true));

        assertTrue(response.active());
        assertNull(response.description());
    }

    @Test
    void createRejectsDuplicateName() {
        when(repository.existsByNameIgnoreCase("Nội khoa")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.create(new SpecializationCreateRequest(" Nội  khoa ", null, true)));
        verify(repository, never()).save(any());
    }

    @Test
    void updateAllFieldsAndReactivate() {
        UUID id = UUID.randomUUID();
        Specialization value = specialization(id, "Tên cũ", "Mô tả cũ", false);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(repository.save(value)).thenReturn(value);

        var response = service.update(id,
                new SpecializationUpdateRequest("  Tai   mũi họng ", "  Khám   TMH ", true));

        assertAll(
                () -> assertEquals("Tai mũi họng", response.name()),
                () -> assertEquals("Khám TMH", response.description()),
                () -> assertTrue(response.active()));
        verify(repository).existsByNameIgnoreCaseAndSpecializationIdNot("Tai mũi họng", id);
    }

    @Test
    void updateRejectsDuplicateName() {
        UUID id = UUID.randomUUID();
        Specialization value = specialization(id, "Cũ", null, true);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(repository.existsByNameIgnoreCaseAndSpecializationIdNot("Trùng", id)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.update(id, new SpecializationUpdateRequest(" Trùng ", null, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void deactivateRejectsActiveReferences() {
        UUID id = UUID.randomUUID();
        Specialization value = specialization(id, "Nội", null, true);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(repository.countActiveReferences(id)).thenReturn(2L);

        assertThrows(ConflictException.class,
                () -> service.update(id, new SpecializationUpdateRequest(null, null, false)));
        assertTrue(value.getActive());
    }

    @Test
    void deactivateWithoutReferencesAndNoOpInactiveUpdateAreSafe() {
        UUID id = UUID.randomUUID();
        Specialization value = specialization(id, "Nội", null, true);
        when(repository.findById(id)).thenReturn(Optional.of(value));
        when(repository.save(value)).thenReturn(value);

        assertFalse(service.update(id, new SpecializationUpdateRequest(null, "", false)).active());
        assertFalse(service.update(id, new SpecializationUpdateRequest(null, null, false)).active());
        verify(repository, times(1)).countActiveReferences(id);
    }

    @Test
    void deleteRejectsUsedSpecializationAndDeletesUnusedOne() {
        UUID usedId = UUID.randomUUID();
        UUID unusedId = UUID.randomUUID();
        Specialization used = specialization(usedId, "Nội", null, true);
        Specialization unused = specialization(unusedId, "Ngoại", null, true);
        when(repository.findById(usedId)).thenReturn(Optional.of(used));
        when(repository.findById(unusedId)).thenReturn(Optional.of(unused));
        when(repository.countAllReferences(usedId)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.delete(usedId));
        service.delete(unusedId);

        verify(repository).delete(unused);
        verify(repository, never()).delete(used);
    }

    @Test
    void findMethodsHandleMissingAndInactiveValues() {
        UUID missingId = UUID.randomUUID();
        UUID inactiveId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());
        when(repository.findById(inactiveId))
                .thenReturn(Optional.of(specialization(inactiveId, "Đã đóng", null, false)));

        assertThrows(ResourceNotFoundException.class, () -> service.findById(missingId));
        assertThrows(ConflictException.class, () -> service.findActiveById(inactiveId));
    }

    @Test
    void findActiveReturnsActiveValue() {
        UUID id = UUID.randomUUID();
        Specialization value = specialization(id, "Nội", null, true);
        when(repository.findById(id)).thenReturn(Optional.of(value));

        assertSame(value, service.findActiveById(id));
    }

    private Specialization specialization(UUID id, String name, String description, Boolean active) {
        return Specialization.builder().specializationId(id).name(name)
                .description(description).active(active).build();
    }
}
