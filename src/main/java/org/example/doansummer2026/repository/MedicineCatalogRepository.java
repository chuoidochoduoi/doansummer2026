package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.MedicineCatalog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface MedicineCatalogRepository extends JpaRepository<MedicineCatalog, UUID> {
    @Query("""
        select m from MedicineCatalog m
        where m.active = true and (
            :keyword is null or :keyword = '' or
            lower(m.name) like lower(concat('%', :keyword, '%')) or
            lower(coalesce(m.activeIngredient, '')) like lower(concat('%', :keyword, '%')) or
            lower(m.medicineCode) like lower(concat('%', :keyword, '%'))
        ) order by m.name
        """)
    List<MedicineCatalog> searchActive(@Param("keyword") String keyword, Pageable pageable);
}
