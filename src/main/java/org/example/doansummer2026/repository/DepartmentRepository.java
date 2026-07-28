package org.example.doansummer2026.repository;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.model.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    boolean existsByRoomCode(String roomCode);

    boolean existsByName(String name);

    Optional<Department> findByName(String name);

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.headDoctor hd LEFT JOIN FETCH hd.profile WHERE d.departmentId = :id")
    Optional<Department> findWithHeadDoctorById(@Param("id") UUID id);

    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.headDoctor hd LEFT JOIN FETCH hd.profile")
    Page<Department> findAllWithHeadDoctor(Pageable pageable);

    Page<Department> findAllByDepartmentType(DepartmentType departmentType, Pageable pageable);

    Page<Department> findAllByDepartmentTypeIn(List<DepartmentType> departmentTypes, Pageable pageable);

    /** Kiem tra bac si da duoc gianh cho phong nao khong. */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Department d WHERE d.headDoctor.staffId = :staffId")
    boolean existsByHeadDoctor_StaffId(@Param("staffId") UUID staffId);

    /** Kiem tra bac si da duoc gianh cho phong khac (dung khi cap nhat). */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Department d WHERE d.headDoctor.staffId = :headDoctorId AND d.departmentId != :departmentId")
    boolean existsByHeadDoctor_StaffIdAndDepartmentIdNot(@Param("headDoctorId") UUID headDoctorId,
                                                          @Param("departmentId") UUID departmentId);
}



