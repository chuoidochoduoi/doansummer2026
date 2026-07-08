package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.schedule.ScheduleCreateRequest;
import org.example.doansummer2026.dto.schedule.ScheduleResponse;
import org.example.doansummer2026.dto.schedule.ScheduleUpdateRequest;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.enums.Shift;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.StaffSchedule;
import org.example.doansummer2026.model.StaffScheduleTemplate;
import org.example.doansummer2026.repository.StaffScheduleRepository;
import org.example.doansummer2026.repository.StaffScheduleTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.StaffScheduleServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lich lam viec cu the theo ngay cua nhan vien.
 * - CRUD co ban theo staffId/date range/shift.
 * - generate(weekStart, staffIds) sinh StaffSchedule tu StaffScheduleTemplate.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StaffScheduleService implements StaffScheduleServiceInterface {

    private final StaffScheduleRepository scheduleRepo;
    private final StaffScheduleTemplateRepository templateRepo;
    private final StaffService staffService;

    public ScheduleResponse create(ScheduleCreateRequest req) {
        StaffInfo staff = staffService.findById(req.staffId());
        StaffSchedule schedule = StaffSchedule.builder()
                .staff(staff)
                .workDate(req.workDate())
                .shift(req.shift())
                .status(req.status() != null ? req.status() : ScheduleStatus.SCHEDULED)
                .isCustom(req.isCustom() != null && req.isCustom())
                .note(req.note())
                .template(req.templateId() != null
                        ? templateRepo.findById(req.templateId()).orElse(null) : null)
                .build();
        return ScheduleResponse.from(scheduleRepo.save(schedule));
    }

    public ScheduleResponse update(UUID id, ScheduleUpdateRequest req) {
        StaffSchedule s = findById(id);
        if (req.shift() != null) s.setShift(req.shift());
        if (req.status() != null) s.setStatus(req.status());
        if (req.isCustom() != null) s.setIsCustom(req.isCustom());
        if (req.note() != null) s.setNote(req.note());
        return ScheduleResponse.from(scheduleRepo.save(s));
    }

    public void delete(UUID id) {
        if (!scheduleRepo.existsById(id)) {
            throw new ResourceNotFoundException("Lich khong ton tai: " + id);
        }
        scheduleRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ScheduleResponse get(UUID id) {
        return ScheduleResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ScheduleResponse> search(UUID staffId, LocalDate from, LocalDate to,
                                                 Shift shift, Pageable pageable) {
        Page<StaffSchedule> page = scheduleRepo.search(staffId, from, to, shift, pageable);
        return PageResponse.from(page, ScheduleResponse::from);
    }

    /**
     * Sinh lich cho 1 tuan (Mon..Sun) tu cac StaffScheduleTemplate dang active.
     * - weekStart: ngay thu 2 (Mon) cua tuan.
     * - staffIds: null/rong = tat ca nhan vien co template active.
     * - overrideExisting: neu true, ghi de StaffSchedule cung (staff, workDate, shift) da ton tai.
     * Tra ve danh sach ScheduleResponse da tao/gan.
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> generateFromTemplates(LocalDate weekStart,
                                                        List<UUID> staffIds,
                                                        Boolean overrideExisting) {
        boolean override = overrideExisting != null && overrideExisting;
        List<StaffInfo> targets;
        if (staffIds == null || staffIds.isEmpty()) {
            // Lay tat ca staff co it nhat 1 template
            targets = templateRepo.findAll().stream()
                    .map(StaffScheduleTemplate::getStaff)
                    .distinct()
                    .toList();
        } else {
            targets = staffIds.stream().map(staffService::findById).toList();
        }

        List<StaffSchedule> toCreate = new ArrayList<>();
        for (StaffInfo staff : targets) {
            List<StaffScheduleTemplate> templates = templateRepo.findByStaff(staff);
            for (StaffScheduleTemplate t : templates) {
                if (Boolean.FALSE.equals(t.getIsActive())) continue;
                LocalDate workDate = dateForDayOfWeek(weekStart, t.getDayOfWeek());
                StaffSchedule schedule = StaffSchedule.builder()
                        .staff(staff)
                        .workDate(workDate)
                        .shift(t.getShift())
                        .status(ScheduleStatus.SCHEDULED)
                        .isCustom(false)
                        .template(t)
                        .build();
                if (override) {
                    // Tim va xoa ban ghi cu neu co
                    scheduleRepo.findByStaffAndWorkDateBetween(staff, workDate, workDate)
                            .stream()
                            .filter(x -> x.getShift() == t.getShift())
                            .findFirst()
                            .ifPresent(scheduleRepo::delete);
                }
                toCreate.add(schedule);
            }
        }
        return scheduleRepo.saveAll(toCreate).stream().map(ScheduleResponse::from).toList();
    }

    public StaffSchedule findById(UUID id) {
        return scheduleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lich khong ton tai: " + id));
    }

    private LocalDate dateForDayOfWeek(LocalDate weekStart, DayOfWeek dow) {
        int diff = dow.getValue() - DayOfWeek.MONDAY.getValue();
        return weekStart.plusDays(diff);
    }
}