package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.serviceCategory.ServiceCategoryCreateRequest;
import org.example.doansummer2026.dto.serviceCategory.ServiceCategoryUpdateRequest;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.ServiceCategory;
import org.example.doansummer2026.repository.ServiceCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ServiceCategoryServiceTest {

    @Mock
    private ServiceCategoryRepository repo;

    @InjectMocks
    private ServiceCategoryService service;


    // =========================================================
    // HELPERS
    // =========================================================

    private ServiceCategory category(
            UUID id,
            String name,
            String description
    ) {
        return ServiceCategory.builder()
                .categoryId(id)
                .name(name)
                .description(description)
                .build();
    }


    // =========================================================
    // LIST
    // =========================================================

    @Test
    void list_ShouldReturnMappedPage() {

        var pageable =
                PageRequest.of(0, 10);

        ServiceCategory category =
                category(
                        UUID.randomUUID(),
                        "Xet nghiem",
                        "Danh muc xet nghiem"
                );

        when(repo.findAll(pageable))
                .thenReturn(
                        new PageImpl<>(
                                List.of(category)
                        )
                );

        var result =
                service.list(pageable);

        assertNotNull(result);

        verify(repo)
                .findAll(pageable);
    }


    @Test
    void list_ShouldReturnEmptyPage() {

        var pageable =
                PageRequest.of(0, 10);

        when(repo.findAll(pageable))
                .thenReturn(
                        new PageImpl<>(List.of())
                );

        var result =
                service.list(pageable);

        assertNotNull(result);
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenFound() {

        UUID id = UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Kham benh",
                        "Mo ta"
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(category)
                );

        assertSame(
                category,
                service.findById(id)
        );
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.findById(id)
        );
    }


    // =========================================================
    // GET
    // =========================================================

    @Test
    void get_ShouldReturnResponse() {

        UUID id = UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Kham noi",
                        "Mo ta"
                );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(category)
                );

        var result =
                service.get(id);

        assertNotNull(result);
    }


    // =========================================================
    // CREATE - DUPLICATE NAME
    // =========================================================

    @Test
    void create_ShouldThrowConflict_WhenNameAlreadyExists() {

        ServiceCategoryCreateRequest req =
                mock(ServiceCategoryCreateRequest.class);

        when(req.name())
                .thenReturn("Xet nghiem");

        when(repo.existsByName("Xet nghiem"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> service.create(req)
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // CREATE - WITHOUT PARENT
    // =========================================================

    @Test
    void create_ShouldCreateWithoutParent() {

        ServiceCategoryCreateRequest req =
                mock(ServiceCategoryCreateRequest.class);

        when(req.name())
                .thenReturn("Xet nghiem");

        when(req.description())
                .thenReturn("Danh muc xet nghiem");

        when(repo.existsByName("Xet nghiem"))
                .thenReturn(false);

        when(repo.save(any(ServiceCategory.class)))
                .thenAnswer(invocation -> {
                    ServiceCategory c =
                            invocation.getArgument(0);

                    c.setCategoryId(
                            UUID.randomUUID()
                    );

                    return c;
                });

        var result =
                service.create(req);

        assertNotNull(result);

        ArgumentCaptor<ServiceCategory> captor =
                ArgumentCaptor.forClass(
                        ServiceCategory.class
                );

        verify(repo)
                .save(captor.capture());

        ServiceCategory saved =
                captor.getValue();

        assertEquals(
                "Xet nghiem",
                saved.getName()
        );

        assertEquals(
                "Danh muc xet nghiem",
                saved.getDescription()
        );

        assertNull(
                saved.getParentCategory()
        );
    }


    // =========================================================
    // CREATE - WITH PARENT
    // =========================================================

    @Test
    void create_ShouldCreateWithParent() {

        UUID parentId =
                UUID.randomUUID();

        ServiceCategory parent =
                category(
                        parentId,
                        "Can lam sang",
                        "Parent"
                );

        ServiceCategoryCreateRequest req =
                mock(ServiceCategoryCreateRequest.class);

        when(req.name())
                .thenReturn("Xet nghiem mau");

        when(req.description())
                .thenReturn("Child");

        when(req.parentId())
                .thenReturn(parentId);

        when(repo.existsByName("Xet nghiem mau"))
                .thenReturn(false);

        when(repo.findById(parentId))
                .thenReturn(
                        Optional.of(parent)
                );

        when(repo.save(any(ServiceCategory.class)))
                .thenAnswer(invocation -> {
                    ServiceCategory c =
                            invocation.getArgument(0);

                    c.setCategoryId(
                            UUID.randomUUID()
                    );

                    return c;
                });

        service.create(req);

        verify(repo)
                .save(argThat(c ->
                        c.getParentCategory() == parent
                                && "Xet nghiem mau"
                                .equals(c.getName())
                ));
    }


    // =========================================================
    // CREATE - PARENT MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenParentMissing() {

        UUID parentId =
                UUID.randomUUID();

        ServiceCategoryCreateRequest req =
                mock(ServiceCategoryCreateRequest.class);

        when(req.name())
                .thenReturn("Child");

        when(req.parentId())
                .thenReturn(parentId);

        when(repo.existsByName("Child"))
                .thenReturn(false);

        when(repo.findById(parentId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.create(req)
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // UPDATE - CATEGORY MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenCategoryMissing() {

        UUID id =
                UUID.randomUUID();

        ServiceCategoryUpdateRequest req =
                mock(ServiceCategoryUpdateRequest.class);

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.update(
                        id,
                        req
                )
        );
    }


    // =========================================================
    // UPDATE - NEW NAME DUPLICATE
    // =========================================================

    @Test
    void update_ShouldThrowConflict_WhenNewNameAlreadyExists() {

        UUID id =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Old",
                        "Mo ta"
                );

        ServiceCategoryUpdateRequest req =
                mock(ServiceCategoryUpdateRequest.class);

        when(req.name())
                .thenReturn("New");

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(category)
                );

        when(repo.existsByName("New"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> service.update(
                        id,
                        req
                )
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // UPDATE - SAME NAME
    // =========================================================

    @Test
    void update_ShouldNotCheckDuplicate_WhenNameUnchanged() {

        UUID id =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Same",
                        "Mo ta"
                );

        ServiceCategoryUpdateRequest req =
                mock(ServiceCategoryUpdateRequest.class);

        when(req.name())
                .thenReturn("Same");

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(category)
                );

        when(repo.save(category))
                .thenReturn(category);

        service.update(
                id,
                req
        );

        verify(repo, never())
                .existsByName(anyString());

        assertEquals(
                "Same",
                category.getName()
        );
    }


    // =========================================================
    // UPDATE - NAME SUCCESS
    // =========================================================

    @Test
    void update_ShouldChangeName_WhenUnique() {

        UUID id =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Old",
                        "Mo ta"
                );

        ServiceCategoryUpdateRequest req =
                mock(ServiceCategoryUpdateRequest.class);

        when(req.name())
                .thenReturn("New");

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(category)
                );

        when(repo.existsByName("New"))
                .thenReturn(false);

        when(repo.save(category))
                .thenReturn(category);

        service.update(
                id,
                req
        );

        assertEquals(
                "New",
                category.getName()
        );
    }


    // =========================================================
    // UPDATE DESCRIPTION
    // =========================================================

    @Test
    void update_ShouldChangeDescription() {

        UUID id =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Category",
                        "Old"
                );

        ServiceCategoryUpdateRequest req =
                mock(ServiceCategoryUpdateRequest.class);

        when(req.description())
                .thenReturn("New description");

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(category)
                );

        when(repo.save(category))
                .thenReturn(category);

        service.update(
                id,
                req
        );

        assertEquals(
                "New description",
                category.getDescription()
        );
    }


    // =========================================================
    // UPDATE PARENT = SELF
    // =========================================================

    @Test
    void update_ShouldReject_WhenParentIsSelf() {

        UUID id =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Category",
                        "Mo ta"
                );

        ServiceCategoryUpdateRequest req =
                mock(ServiceCategoryUpdateRequest.class);

        when(req.parentId())
                .thenReturn(id);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(category)
                );

        assertThrows(
                BadRequestException.class,
                () -> service.update(
                        id,
                        req
                )
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // UPDATE PARENT MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenNewParentMissing() {

        UUID id =
                UUID.randomUUID();

        UUID parentId =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Category",
                        "Mo ta"
                );

        ServiceCategoryUpdateRequest req =
                mock(ServiceCategoryUpdateRequest.class);

        when(req.parentId())
                .thenReturn(parentId);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(category)
                );

        when(repo.findById(parentId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.update(
                        id,
                        req
                )
        );
    }


    // =========================================================
    // UPDATE PARENT SUCCESS
    // =========================================================

    @Test
    void update_ShouldChangeParent() {

        UUID id =
                UUID.randomUUID();

        UUID parentId =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Category",
                        "Mo ta"
                );

        ServiceCategory parent =
                category(
                        parentId,
                        "Parent",
                        "Parent description"
                );

        ServiceCategoryUpdateRequest req =
                mock(ServiceCategoryUpdateRequest.class);

        when(req.parentId())
                .thenReturn(parentId);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(category)
                );

        when(repo.findById(parentId))
                .thenReturn(
                        Optional.of(parent)
                );

        when(repo.save(category))
                .thenReturn(category);

        service.update(
                id,
                req
        );

        assertSame(
                parent,
                category.getParentCategory()
        );
    }


    // =========================================================
    // UPDATE ALL BASIC FIELDS
    // =========================================================

    @Test
    void update_ShouldUpdateNameDescriptionAndParent() {

        UUID id =
                UUID.randomUUID();

        UUID parentId =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Old",
                        "Old description"
                );

        ServiceCategory parent =
                category(
                        parentId,
                        "Parent",
                        "Parent description"
                );

        ServiceCategoryUpdateRequest req =
                mock(ServiceCategoryUpdateRequest.class);

        when(req.name())
                .thenReturn("New");

        when(req.description())
                .thenReturn("New description");

        when(req.parentId())
                .thenReturn(parentId);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(category)
                );

        when(repo.existsByName("New"))
                .thenReturn(false);

        when(repo.findById(parentId))
                .thenReturn(
                        Optional.of(parent)
                );

        when(repo.save(category))
                .thenReturn(category);

        var result =
                service.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                "New",
                category.getName()
        );

        assertEquals(
                "New description",
                category.getDescription()
        );

        assertSame(
                parent,
                category.getParentCategory()
        );
    }


    // =========================================================
    // UPDATE EMPTY REQUEST
    // =========================================================

    @Test
    void update_ShouldSaveWithoutChanges_WhenRequestEmpty() {

        UUID id =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Category",
                        "Description"
                );

        ServiceCategoryUpdateRequest req =
                mock(ServiceCategoryUpdateRequest.class);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(category)
                );

        when(repo.save(category))
                .thenReturn(category);

        var result =
                service.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                "Category",
                category.getName()
        );

        assertEquals(
                "Description",
                category.getDescription()
        );

        verify(repo)
                .save(category);
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenMissing() {

        UUID id =
                UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.delete(id)
        );

        verify(repo, never())
                .deleteById(id);
    }


    @Test
    void delete_ShouldDelete_WhenExists() {

        UUID id =
                UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(true);

        service.delete(id);

        verify(repo)
                .deleteById(id);
    }
}