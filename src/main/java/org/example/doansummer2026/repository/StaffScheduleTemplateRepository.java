package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.StaffScheduleTemplate;
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
}