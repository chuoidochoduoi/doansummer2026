package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.schedule.ScheduleResponse;
import org.example.doansummer2026.dto.schedule.ScheduleCreateRequest;
import org.example.doansummer2026.dto.schedule.ScheduleUpdateRequest;
import org.example.doansummer2026.model.StaffSchedule;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.example.doansummer2026.dto.schedule.ScheduleAssignRequest;
import org.example.doansummer2026.dto.schedule.ScheduleResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Service interface for StaffSchedule management. */
public interface StaffScheduleServiceInterface {
    PageResponse<ScheduleResponse> search(UUID staffId, LocalDate from, LocalDate to,
                                           UUID shiftId, Pageable pageable);
    ScheduleResponse get(UUID id);
    ScheduleResponse create(ScheduleCreateRequest req);
    ScheduleResponse update(UUID id, ScheduleUpdateRequest req);
    void delete(UUID id);
    List<ScheduleResponse> generateFromTemplates(LocalDate weekStart, List<UUID> staffIds, Boolean override);
    StaffSchedule findById(UUID id);

    /** Tim kiem schedule trong 1 tuan. */
    List<StaffSchedule> findByWeek(LocalDate from, LocalDate to);

    /** Gan nhan su va Assign staff to a specific shift/day in the schedule. */
    void assignStaff(ScheduleAssignRequest req);

    /** Sao chep lich tu tuan cu sang tuan moi. */
    List<StaffSchedule> copyWeek(LocalDate fromWeek, LocalDate toWeek);
}



