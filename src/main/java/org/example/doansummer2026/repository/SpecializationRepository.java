package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, UUID> {

    boolean existsByName(String name);

    Optional<Specialization> findByName(String name);
}