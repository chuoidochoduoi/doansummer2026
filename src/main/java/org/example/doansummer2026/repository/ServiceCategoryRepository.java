package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {

    boolean existsByName(String name);

    Optional<ServiceCategory> findByName(String name);

    boolean existsByParentCategory_CategoryId(UUID categoryId);
}



