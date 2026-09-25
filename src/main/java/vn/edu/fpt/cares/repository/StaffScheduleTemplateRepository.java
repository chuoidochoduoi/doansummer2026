package vn.edu.fpt.cares.repository;

import vn.edu.fpt.cares.model.StaffInfo;
import vn.edu.fpt.cares.model.StaffScheduleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffScheduleTemplateRepository extends JpaRepository<StaffScheduleTemplate, UUID> {

    List<StaffScheduleTemplate> findByStaff(StaffInfo staff);

    Optional<StaffScheduleTemplate> findByStaffAndDayOfWeek(StaffInfo staff, DayOfWeek dayOfWeek);

    long countByShift_ShiftId(UUID shiftId);

    boolean existsByShift_ShiftIdAndIsActiveTrue(UUID shiftId);
}



