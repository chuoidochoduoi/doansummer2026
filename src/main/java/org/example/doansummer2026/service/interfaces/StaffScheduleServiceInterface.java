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

/** Service interface for StaffSchedule management. */
public interface StaffScheduleServiceInterface {
    PageResponse<ScheduleResponse> search(UUID staffId, LocalDate from, LocalDate to,
                                           org.example.doansummer2026.enums.Shift shift, Pageable pageable);
    ScheduleResponse get(UUID id);
    ScheduleResponse create(ScheduleCreateRequest req);
    ScheduleResponse update(UUID id, ScheduleUpdateRequest req);
    void delete(UUID id);
    List<ScheduleResponse> generateFromTemplates(LocalDate weekStart, List<UUID> staffIds, Boolean override);
    StaffSchedule findById(UUID id);
}