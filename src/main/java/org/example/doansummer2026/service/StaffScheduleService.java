package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.schedule.ScheduleAssignRequest;
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
import java.util.*;

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

    /**
     * Tim kiem schedule trong 1 tuan.
     */
    @Transactional(readOnly = true)
    public List<StaffSchedule> findByWeek(LocalDate from, LocalDate to) {
        return scheduleRepo.findAllByWorkDateBetween(from, to);
    }

    /**
     * Tim kiem schedule cua 1 nhan su trong 1 tuan.
     */
    @Transactional(readOnly = true)
    public List<StaffSchedule> findByStaffAndWeek(UUID staffId, LocalDate from, LocalDate to) {
        StaffInfo staff = staffService.findById(staffId);
        return scheduleRepo.findByStaffAndWorkDateBetween(staff, from, to);
    }

    /**
     * Gan nhan su vao ca truc theo ngay.
     */
    public void assignStaff(ScheduleAssignRequest req) {
        LocalDate date = parseDateFromDayOfWeek(req.week(), req.dayKey());
        Shift shift = Shift.valueOf(req.shiftId().toUpperCase());

        StaffInfo staff = staffService.findById(req.staffId());

        if ("remove".equalsIgnoreCase(req.action())) {
            // Xoa schedule
            scheduleRepo.deleteByStaffAndWorkDateAndShift(staff, date, shift);
        } else {
            // Them schedule - kiem tra neu da ton tai thi bo qua
            if (scheduleRepo.findByStaffAndWorkDateAndShift(staff, date, shift).isEmpty()) {
                StaffSchedule schedule = StaffSchedule.builder()
                        .staff(staff)
                        .workDate(date)
                        .shift(shift)
                        .status(org.example.doansummer2026.enums.ScheduleStatus.SCHEDULED)
                        .isCustom(true)
                        .build();
                scheduleRepo.save(schedule);
            }
        }
    }

    /**
     * Sao chep lich tu tuan cu sang tuan moi.
     */
    public List<StaffSchedule> copyWeek(LocalDate fromWeek, LocalDate toWeek) {
        List<StaffSchedule> oldSchedules = scheduleRepo.findAllByWorkDateBetween(fromWeek, fromWeek.plusDays(6));
        List<StaffSchedule> toCreate = new ArrayList<>();

        for (StaffSchedule old : oldSchedules) {
            LocalDate newDate = toWeek.plusDays(old.getWorkDate().getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
            Optional<StaffSchedule> existing = scheduleRepo.findByStaffAndWorkDateAndShift(
                    old.getStaff(), newDate, old.getShift());

            if (existing.isEmpty()) {
                StaffSchedule newSchedule = StaffSchedule.builder()
                        .staff(old.getStaff())
                        .workDate(newDate)
                        .shift(old.getShift())
                        .status(old.getStatus())
                        .isCustom(true)
                        .note(old.getNote())
                        .build();
                toCreate.add(newSchedule);
            }
        }
        return scheduleRepo.saveAll(toCreate).stream().map(s -> {
            return s;
        }).toList();
    }

    private LocalDate parseDateFromDayOfWeek(LocalDate weekStart, String dayKey) {
        Map<String, DayOfWeek> dayMap = Map.of(
                "mon", DayOfWeek.MONDAY,
                "tue", DayOfWeek.TUESDAY,
                "wed", DayOfWeek.WEDNESDAY,
                "thu", DayOfWeek.THURSDAY,
                "fri", DayOfWeek.FRIDAY,
                "sat", DayOfWeek.SATURDAY,
                "sun", DayOfWeek.SUNDAY
        );
        DayOfWeek dow = dayMap.get(dayKey.toLowerCase());
        if (dow == null) {
            throw new IllegalArgumentException("Invalid dayKey: " + dayKey);
        }
        return weekStart.with(dow);
    }
}



