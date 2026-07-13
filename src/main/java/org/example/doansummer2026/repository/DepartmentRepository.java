package org.example.doansummer2026.repository;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.model.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    boolean existsByName(String name);

    Optional<Department> findByName(String name);

    Page<Department> findAllByDepartmentType(DepartmentType departmentType, Pageable pageable);
}