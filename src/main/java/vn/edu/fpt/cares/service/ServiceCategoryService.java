package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.servicecategory.ServiceCategoryCreateRequest;
import vn.edu.fpt.cares.dto.servicecategory.ServiceCategoryResponse;
import vn.edu.fpt.cares.dto.servicecategory.ServiceCategoryUpdateRequest;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.ServiceCategory;
import vn.edu.fpt.cares.repository.ServiceCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.cares.service.interfaces.ServiceCategoryServiceInterface;
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
        String normalizedName = normalizeName(req.name());
        if (repo.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Tên danh mục đã tồn tại: " + normalizedName);
        }
        ServiceCategory parent = null;
        if (req.parentId() != null) {
            parent = findById(req.parentId());
        }
        ServiceCategory c = ServiceCategory.builder()
                .name(normalizedName)
                .description(req.description())
                .parentCategory(parent)
                .build();
        return ServiceCategoryResponse.from(repo.save(c), false);
    }

    public ServiceCategoryResponse update(UUID id, ServiceCategoryUpdateRequest req) {
        ServiceCategory c = findById(id);
        if (req.name() != null) {
            String normalizedName = normalizeName(req.name());
            if (repo.existsByNameIgnoreCaseAndCategoryIdNot(normalizedName, id)) {
                throw new ConflictException("Tên danh mục đã tồn tại: " + normalizedName);
            }
            c.setName(normalizedName);
        }
        if (req.description() != null) c.setDescription(req.description());
        if (req.parentId() != null) {
            if (req.parentId().equals(id)) {
                throw new vn.edu.fpt.cares.exception.BadRequestException(
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
                throw new vn.edu.fpt.cares.exception.BadRequestException(
                        "Không thể chọn danh mục con làm danh mục cha vì sẽ tạo vòng lặp"
                );
            }
            current = current.getParentCategory();
        }
    }

    private String normalizeName(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new vn.edu.fpt.cares.exception.BadRequestException(
                    "Tên danh mục không được để trống");
        }
        return normalized;
    }
}




