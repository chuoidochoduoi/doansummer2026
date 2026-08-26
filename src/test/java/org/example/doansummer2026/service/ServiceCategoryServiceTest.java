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

        when(
                repo.findAll(pageable)
        ).thenReturn(
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

        when(
                repo.findAll(pageable)
        ).thenReturn(
                new PageImpl<>(
                        List.of()
                )
        );

        var result =
                service.list(pageable);

        assertNotNull(result);

        verify(repo)
                .findAll(pageable);
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenFound() {

        UUID id =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Kham benh",
                        "Mo ta"
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        assertSame(
                category,
                service.findById(id)
        );

        verify(repo)
                .findById(id);
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id =
                UUID.randomUUID();

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.empty()
        );

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> service.findById(id)
                );

        assertTrue(
                exception.getMessage()
                        .contains(id.toString())
        );
    }


    // =========================================================
    // GET
    // =========================================================

    @Test
    void get_ShouldReturnResponse() {

        UUID id =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Kham noi",
                        "Mo ta"
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        var result =
                service.get(id);

        assertNotNull(result);

        verify(repo)
                .findById(id);
    }


    // =========================================================
    // CREATE - INVALID NAME
    // =========================================================

    @Test
    void create_ShouldRejectNullName() {

        ServiceCategoryCreateRequest req =
                mock(ServiceCategoryCreateRequest.class);

        when(req.name())
                .thenReturn(null);

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> service.create(req)
                );

        assertEquals(
                "Tên danh mục không được để trống",
                exception.getMessage()
        );

        verifyNoInteractions(repo);
    }


    @Test
    void create_ShouldRejectBlankName() {

        ServiceCategoryCreateRequest req =
                mock(ServiceCategoryCreateRequest.class);

        when(req.name())
                .thenReturn("   ");

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> service.create(req)
                );

        assertEquals(
                "Tên danh mục không được để trống",
                exception.getMessage()
        );

        verifyNoInteractions(repo);
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

        when(
                repo.existsByNameIgnoreCase(
                        "Xet nghiem"
                )
        ).thenReturn(true);

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> service.create(req)
                );

        assertEquals(
                "Tên danh mục đã tồn tại: Xet nghiem",
                exception.getMessage()
        );

        verify(repo, never())
                .save(any(ServiceCategory.class));
    }


    // =========================================================
    // CREATE - NORMALIZE NAME
    // =========================================================

    @Test
    void create_ShouldNormalizeName() {

        ServiceCategoryCreateRequest req =
                mock(ServiceCategoryCreateRequest.class);

        when(req.name())
                .thenReturn(
                        "   Xet    nghiem    mau   "
                );

        when(
                repo.save(
                        any(ServiceCategory.class)
                )
        ).thenAnswer(
                invocation -> {

                    ServiceCategory category =
                            invocation.getArgument(0);

                    category.setCategoryId(
                            UUID.randomUUID()
                    );

                    return category;
                }
        );

        service.create(req);

        verify(repo)
                .existsByNameIgnoreCase(
                        "Xet nghiem mau"
                );

        verify(repo)
                .save(
                        argThat(category ->
                                "Xet nghiem mau"
                                        .equals(
                                                category.getName()
                                        )
                        )
                );
    }


    // =========================================================
    // CREATE - WITHOUT PARENT
    // =========================================================

    @Test
    void create_ShouldCreateWithoutParent() {

        ServiceCategoryCreateRequest req =
                mock(ServiceCategoryCreateRequest.class);

        when(req.name())
                .thenReturn(
                        "Xet nghiem"
                );

        when(req.description())
                .thenReturn(
                        "Danh muc xet nghiem"
                );

        when(
                repo.save(
                        any(ServiceCategory.class)
                )
        ).thenAnswer(
                invocation -> {

                    ServiceCategory category =
                            invocation.getArgument(0);

                    category.setCategoryId(
                            UUID.randomUUID()
                    );

                    return category;
                }
        );

        var result =
                service.create(req);

        assertNotNull(result);

        ArgumentCaptor<ServiceCategory> captor =
                ArgumentCaptor.forClass(
                        ServiceCategory.class
                );

        verify(repo)
                .save(
                        captor.capture()
                );

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

        verify(repo)
                .existsByNameIgnoreCase(
                        "Xet nghiem"
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
                .thenReturn(
                        "Xet nghiem mau"
                );

        when(req.description())
                .thenReturn(
                        "Child"
                );

        when(req.parentId())
                .thenReturn(
                        parentId
                );

        when(
                repo.findById(parentId)
        ).thenReturn(
                Optional.of(parent)
        );

        when(
                repo.save(
                        any(ServiceCategory.class)
                )
        ).thenAnswer(
                invocation -> {

                    ServiceCategory category =
                            invocation.getArgument(0);

                    category.setCategoryId(
                            UUID.randomUUID()
                    );

                    return category;
                }
        );

        var result =
                service.create(req);

        assertNotNull(result);

        verify(repo)
                .existsByNameIgnoreCase(
                        "Xet nghiem mau"
                );

        verify(repo)
                .findById(parentId);

        verify(repo)
                .save(
                        argThat(category ->
                                category.getParentCategory()
                                        == parent
                                        &&
                                        "Xet nghiem mau"
                                                .equals(
                                                        category.getName()
                                                )
                                        &&
                                        "Child"
                                                .equals(
                                                        category.getDescription()
                                                )
                        )
                );
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
                .thenReturn(
                        "Child"
                );

        when(req.parentId())
                .thenReturn(
                        parentId
                );

        when(
                repo.findById(parentId)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.create(req)
        );

        verify(repo)
                .existsByNameIgnoreCase(
                        "Child"
                );

        verify(repo)
                .findById(parentId);

        verify(repo, never())
                .save(any(ServiceCategory.class));
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

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        service.update(
                                id,
                                req
                        )
        );

        verify(repo, never())
                .save(any(ServiceCategory.class));
    }


    // =========================================================
    // UPDATE - DUPLICATE NAME
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
                .thenReturn(
                        "New"
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                repo.existsByNameIgnoreCaseAndCategoryIdNot(
                        "New",
                        id
                )
        ).thenReturn(true);

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                service.update(
                                        id,
                                        req
                                )
                );

        assertEquals(
                "Tên danh mục đã tồn tại: New",
                exception.getMessage()
        );

        verify(repo, never())
                .save(any(ServiceCategory.class));
    }


    // =========================================================
    // UPDATE - SAME NAME
    // =========================================================

    @Test
    void update_ShouldAllowSameName_WhenNoOtherCategoryUsesIt() {

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
                .thenReturn(
                        "Same"
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                repo.save(category)
        ).thenReturn(category);

        var result =
                service.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                "Same",
                category.getName()
        );

        verify(repo)
                .existsByNameIgnoreCaseAndCategoryIdNot(
                        "Same",
                        id
                );

        verify(repo)
                .save(category);
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
                .thenReturn(
                        "New"
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                repo.save(category)
        ).thenReturn(category);

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

        verify(repo)
                .existsByNameIgnoreCaseAndCategoryIdNot(
                        "New",
                        id
                );

        verify(repo)
                .save(category);
    }


    // =========================================================
    // UPDATE - NORMALIZE NAME
    // =========================================================

    @Test
    void update_ShouldNormalizeName() {

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
                .thenReturn(
                        "   New    Category   "
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                repo.save(category)
        ).thenReturn(category);

        service.update(
                id,
                req
        );

        assertEquals(
                "New Category",
                category.getName()
        );

        verify(repo)
                .existsByNameIgnoreCaseAndCategoryIdNot(
                        "New Category",
                        id
                );
    }


    // =========================================================
    // UPDATE - BLANK NAME
    // =========================================================

    @Test
    void update_ShouldRejectBlankName() {

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
                .thenReturn(
                        "   "
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                service.update(
                                        id,
                                        req
                                )
                );

        assertEquals(
                "Tên danh mục không được để trống",
                exception.getMessage()
        );

        verify(repo, never())
                .save(any(ServiceCategory.class));
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
                .thenReturn(
                        "New description"
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                repo.save(category)
        ).thenReturn(category);

        var result =
                service.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                "New description",
                category.getDescription()
        );

        verify(repo)
                .save(category);
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
                .thenReturn(
                        id
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                service.update(
                                        id,
                                        req
                                )
                );

        assertEquals(
                "Không thể đặt danh mục cha là chính nó",
                exception.getMessage()
        );

        verify(repo, never())
                .save(any(ServiceCategory.class));
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
                .thenReturn(
                        parentId
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                repo.findById(parentId)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        service.update(
                                id,
                                req
                        )
        );

        verify(repo, never())
                .save(any(ServiceCategory.class));
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
                .thenReturn(
                        parentId
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                repo.findById(parentId)
        ).thenReturn(
                Optional.of(parent)
        );

        when(
                repo.save(category)
        ).thenReturn(category);

        var result =
                service.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertSame(
                parent,
                category.getParentCategory()
        );

        verify(repo)
                .save(category);
    }


    // =========================================================
    // UPDATE - REJECT CYCLE
    // =========================================================

    @Test
    void update_ShouldReject_WhenParentCreatesCycle() {

        UUID rootId =
                UUID.randomUUID();

        UUID childId =
                UUID.randomUUID();

        UUID grandChildId =
                UUID.randomUUID();

        ServiceCategory root =
                category(
                        rootId,
                        "Root",
                        "Root"
                );

        ServiceCategory child =
                category(
                        childId,
                        "Child",
                        "Child"
                );

        ServiceCategory grandChild =
                category(
                        grandChildId,
                        "Grand Child",
                        "Grand Child"
                );

        child.setParentCategory(root);
        grandChild.setParentCategory(child);

        ServiceCategoryUpdateRequest req =
                mock(ServiceCategoryUpdateRequest.class);

        when(req.parentId())
                .thenReturn(
                        grandChildId
                );

        when(
                repo.findById(rootId)
        ).thenReturn(
                Optional.of(root)
        );

        when(
                repo.findById(grandChildId)
        ).thenReturn(
                Optional.of(grandChild)
        );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                service.update(
                                        rootId,
                                        req
                                )
                );

        assertEquals(
                "Không thể chọn danh mục con làm danh mục cha vì sẽ tạo vòng lặp",
                exception.getMessage()
        );

        verify(repo, never())
                .save(any(ServiceCategory.class));
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
                .thenReturn(
                        "New"
                );

        when(req.description())
                .thenReturn(
                        "New description"
                );

        when(req.parentId())
                .thenReturn(
                        parentId
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                repo.findById(parentId)
        ).thenReturn(
                Optional.of(parent)
        );

        when(
                repo.save(category)
        ).thenReturn(category);

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

        verify(repo)
                .existsByNameIgnoreCaseAndCategoryIdNot(
                        "New",
                        id
                );

        verify(repo)
                .save(category);
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

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                repo.save(category)
        ).thenReturn(category);

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

        assertNull(
                category.getParentCategory()
        );

        verify(repo)
                .save(category);
    }


    // =========================================================
    // DELETE - NOT FOUND
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenMissing() {

        UUID id =
                UUID.randomUUID();

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        service.delete(id)
        );

        verify(repo, never())
                .delete(any(ServiceCategory.class));

        verify(repo, never())
                .existsByParentCategory_CategoryId(id);
    }


    // =========================================================
    // DELETE - HAS CHILDREN
    // =========================================================

    @Test
    void delete_ShouldReject_WhenCategoryHasChildren() {

        UUID id =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Parent",
                        "Parent category"
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                repo.existsByParentCategory_CategoryId(
                        id
                )
        ).thenReturn(true);

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                service.delete(id)
                );

        assertEquals(
                "Không thể xóa danh mục đang có danh mục con. Vui lòng xử lý danh mục con trước",
                exception.getMessage()
        );

        verify(repo, never())
                .delete(any(ServiceCategory.class));
    }


    // =========================================================
    // DELETE - SUCCESS
    // =========================================================

    @Test
    void delete_ShouldDelete_WhenExistsAndHasNoChildren() {

        UUID id =
                UUID.randomUUID();

        ServiceCategory category =
                category(
                        id,
                        "Category",
                        "Description"
                );

        when(
                repo.findById(id)
        ).thenReturn(
                Optional.of(category)
        );

        service.delete(id);

        verify(repo)
                .existsByParentCategory_CategoryId(
                        id
                );

        verify(repo)
                .delete(category);

        verify(repo, never())
                .deleteById(id);
    }
}