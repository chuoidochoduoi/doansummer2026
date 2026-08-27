package org.example.doansummer2026.repository;
import org.example.doansummer2026.model.StaffAttendance;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDate;
import java.util.*;
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance,UUID> {
 Optional<StaffAttendance> findBySchedule_ScheduleId(UUID scheduleId);
 @Query("select a from StaffAttendance a where a.staff.staffId=:staffId and a.schedule.workDate=:date order by a.schedule.shift") List<StaffAttendance> findToday(UUID staffId, LocalDate date);
 @Query("select a from StaffAttendance a where a.schedule.workDate=:date order by a.staff.staffCode") List<StaffAttendance> findAllForDate(LocalDate date);
}
