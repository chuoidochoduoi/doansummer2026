package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Icd10Selection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface Icd10SelectionRepository extends JpaRepository<Icd10Selection, UUID> {
}