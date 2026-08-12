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
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT d FROM Department d WHERE d.departmentId = :id")
    Optional<Department> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

    boolean existsByRoomCode(String roomCode);

    boolean existsByName(String name);

    Optional<Department> findByName(String name);

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.headDoctor hd LEFT JOIN FETCH hd.profile WHERE d.departmentId = :id")
    Optional<Department> findWithHeadDoctorById(@Param("id") UUID id);

    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.headDoctor hd LEFT JOIN FETCH hd.profile")
    Page<Department> findAllWithHeadDoctor(Pageable pageable);

    Page<Department> findAllByDepartmentType(DepartmentType departmentType, Pageable pageable);

    Page<Department> findAllByDepartmentTypeIn(List<DepartmentType> departmentTypes, Pageable pageable);

    /**
     * Phong IN_SESSION van nhan benh nhan vao hang cho; trang thai nay chi cho
     * biet phong dang phuc vu mot ca. Chi phong BAO TRI moi bi loai dieu phoi.
     */
    @Query("SELECT DISTINCT d FROM Department d JOIN d.capabilities c WHERE c.capabilityId = :capabilityId AND d.status <> org.example.doansummer2026.enums.DepartmentStatus.MAINTENANCE")
    List<Department> findEligibleByCapability(@Param("capabilityId") UUID capabilityId);

    /**
     * Phòng khám được điều phối theo chuyên khoa của dịch vụ, không theo một
     * phòng cố định trên MedicalService. Dữ liệu cũ có thể chưa đồng bộ
     * head_doctor_id nên không được loại phòng chỉ vì trường này đang trống.
     */
    @Query("SELECT d FROM Department d " +
           "WHERE d.departmentType = org.example.doansummer2026.enums.DepartmentType.EXAMINATION " +
           "AND d.specialization.specializationId = :specializationId " +
           "AND d.status <> org.example.doansummer2026.enums.DepartmentStatus.MAINTENANCE")
    List<Department> findEligibleExaminationRoomsBySpecialization(
            @Param("specializationId") UUID specializationId);

    /** Kiem tra bac si da duoc gianh cho phong nao khong. */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Department d WHERE d.headDoctor.staffId = :staffId")
    boolean existsByHeadDoctor_StaffId(@Param("staffId") UUID staffId);

    /** Kiem tra bac si da duoc gianh cho phong khac (dung khi cap nhat). */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Department d WHERE d.headDoctor.staffId = :headDoctorId AND d.departmentId != :departmentId")
    boolean existsByHeadDoctor_StaffIdAndDepartmentIdNot(@Param("headDoctorId") UUID headDoctorId,
                                                          @Param("departmentId") UUID departmentId);

    Optional<Department> findByHeadDoctor_StaffId(UUID staffId);

    /** Tim danh sach phong theo y ta. */
    List<Department> findByNurses_StaffId(UUID staffId);

    /** Tim phong theo y ta. */
    Optional<Department> findFirstByNurses_StaffId(UUID staffId);
}
