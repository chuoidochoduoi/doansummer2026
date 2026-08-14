package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, UUID> {

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndSpecializationIdNot(String name, UUID specializationId);

    Optional<Specialization> findByName(String name);

    @Query(value = """
            SELECT (SELECT COUNT(*) FROM department d WHERE d.specialization_id = :id)
                 + (SELECT COUNT(*) FROM medical_service ms WHERE ms.required_specialization_id = :id)
                 + (SELECT COUNT(*) FROM staff_info si WHERE si.specialization_id = :id)
            """, nativeQuery = true)
    long countAllReferences(@Param("id") UUID id);

    @Query(value = """
            SELECT (SELECT COUNT(*) FROM department d
                    WHERE d.specialization_id = :id AND d.deleted = false AND d.status <> 'MAINTENANCE')
                 + (SELECT COUNT(*) FROM medical_service ms
                    WHERE ms.required_specialization_id = :id AND ms.deleted = false AND ms.status = 'ACTIVE')
                 + (SELECT COUNT(*) FROM staff_info si
                    JOIN profile p ON p.profile_id = si.profile_id
                    LEFT JOIN account a ON a.account_id = p.account_id
                    WHERE si.specialization_id = :id AND si.deleted = false AND p.deleted = false
                      AND (a.account_id IS NULL OR a.is_active = true))
            """, nativeQuery = true)
    long countActiveReferences(@Param("id") UUID id);
}



