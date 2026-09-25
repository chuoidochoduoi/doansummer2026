package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.schedule.ScheduleResponse;
import vn.edu.fpt.cares.dto.schedule.ScheduleCreateRequest;
import vn.edu.fpt.cares.dto.schedule.ScheduleUpdateRequest;
import vn.edu.fpt.cares.model.StaffSchedule;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.edu.fpt.cares.dto.schedule.ScheduleAssignRequest;
import vn.edu.fpt.cares.dto.schedule.ScheduleResponse;

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



