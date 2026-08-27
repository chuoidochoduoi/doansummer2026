package org.example.doansummer2026.dto.serviceCategory;

import org.example.doansummer2026.model.ServiceCategory;

import java.util.List;
import java.util.UUID;

public record ServiceCategoryResponse(
        UUID categoryId,
        String name,
        String description,
        UUID parentId,
        String parentName,
        List<ServiceCategoryResponse> subCategories
) {
    public static ServiceCategoryResponse from(ServiceCategory c, boolean includeChildren) {
        UUID parentId = c.getParentCategory() != null ? c.getParentCategory().getCategoryId() : null;
        String parentName = c.getParentCategory() != null ? c.getParentCategory().getName() : null;
        List<ServiceCategoryResponse> subs = includeChildren
                ? c.getSubCategories().stream()
                    .map(sc -> ServiceCategoryResponse.from(sc, false))
                    .toList()
                : List.of();
        return new ServiceCategoryResponse(c.getCategoryId(), c.getName(), c.getDescription(),
                parentId, parentName, subs);
    }
}




