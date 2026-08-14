package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.serviceCategory.ServiceCategoryCreateRequest;
import org.example.doansummer2026.dto.serviceCategory.ServiceCategoryResponse;
import org.example.doansummer2026.dto.serviceCategory.ServiceCategoryUpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.ServiceCategory;
import org.example.doansummer2026.repository.ServiceCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.ServiceCategoryServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ServiceCategoryService implements ServiceCategoryServiceInterface {

    private final ServiceCategoryRepository repo;

    @Transactional(readOnly = true)
    public PageResponse<ServiceCategoryResponse> list(Pageable pageable) {
        Page<ServiceCategory> page = repo.findAll(pageable);
        return PageResponse.from(page, c -> ServiceCategoryResponse.from(c, false));
    }

    @Transactional(readOnly = true)
    public ServiceCategoryResponse get(UUID id) {
        return ServiceCategoryResponse.from(findById(id), true);
    }

    public ServiceCategoryResponse create(ServiceCategoryCreateRequest req) {
        if (repo.existsByName(req.name())) {
            throw new ConflictException("Tên danh mục đã tồn tại: " + req.name());
        }
        ServiceCategory parent = null;
        if (req.parentId() != null) {
            parent = findById(req.parentId());
        }
        ServiceCategory c = ServiceCategory.builder()
                .name(req.name())
                .description(req.description())
                .parentCategory(parent)
                .build();
        return ServiceCategoryResponse.from(repo.save(c), false);
    }

    public ServiceCategoryResponse update(UUID id, ServiceCategoryUpdateRequest req) {
        ServiceCategory c = findById(id);
        if (req.name() != null && !req.name().equals(c.getName())) {
            if (repo.existsByName(req.name())) {
                throw new ConflictException("Tên danh mục đã tồn tại: " + req.name());
            }
            c.setName(req.name());
        }
        if (req.description() != null) c.setDescription(req.description());
        if (req.parentId() != null) {
            if (req.parentId().equals(id)) {
                throw new org.example.doansummer2026.exception.BadRequestException(
                        "Không thể đặt danh mục cha là chính nó");
            }
            ServiceCategory parent = findById(req.parentId());
            validateNoCycle(c, parent);
            c.setParentCategory(parent);
        }
        return ServiceCategoryResponse.from(repo.save(c), false);
    }

    public void delete(UUID id) {
        ServiceCategory category = findById(id);
        if (repo.existsByParentCategory_CategoryId(id)) {
            throw new ConflictException("Không thể xóa danh mục đang có danh mục con. Vui lòng xử lý danh mục con trước");
        }
        repo.delete(category);
    }

    public ServiceCategory findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại: " + id));
    }

    private void validateNoCycle(ServiceCategory category, ServiceCategory proposedParent) {
        ServiceCategory current = proposedParent;
        while (current != null) {
            if (current.getCategoryId().equals(category.getCategoryId())) {
                throw new org.example.doansummer2026.exception.BadRequestException(
                        "Không thể chọn danh mục con làm danh mục cha vì sẽ tạo vòng lặp"
                );
            }
            current = current.getParentCategory();
        }
    }
}




