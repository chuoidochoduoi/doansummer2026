package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.schedule.ScheduleAssignRequest;
import org.example.doansummer2026.dto.schedule.ScheduleCreateRequest;
import org.example.doansummer2026.dto.schedule.ScheduleResponse;
import org.example.doansummer2026.dto.schedule.ScheduleUpdateRequest;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.model.ShiftConfig;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.StaffSchedule;
import org.example.doansummer2026.model.StaffScheduleTemplate;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.repository.StaffScheduleRepository;
import org.example.doansummer2026.repository.StaffScheduleTemplateRepository;
import org.example.doansummer2026.repository.StaffAttendanceRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.dto.notification.NotificationCreateRequest;
import org.example.doansummer2026.enums.NotificationType;
import org.example.doansummer2026.enums.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.StaffScheduleServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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

    private static final java.time.ZoneId CLINIC_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    private final StaffScheduleRepository scheduleRepo;
    private final StaffScheduleTemplateRepository templateRepo;
    private final ShiftConfigRepository shiftConfigRepo;
    private final StaffService staffService;
    private final NotificationService notificationService;
    private final StaffAttendanceRepository attendanceRepository;
    private final StaffInfoRepository staffInfoRepository;

    public ScheduleResponse create(ScheduleCreateRequest req) {
        if (req.workDate().isBefore(LocalDate.now(CLINIC_ZONE))) {
            throw new ConflictException("Không thể tạo lịch trực cho ngày đã qua");
        }
        StaffInfo staff = staffInfoRepository.findByIdForScheduleUpdate(req.staffId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân sự không tồn tại: " + req.staffId()));
        ShiftConfig shift = shiftConfigRepo.findByIdForScheduleUpdate(req.shiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Ca làm việc không tồn tại: " + req.shiftId()));
        if (Boolean.FALSE.equals(shift.getIsActive())) {
            throw new ConflictException("Ca làm việc đã ngừng hoạt động và không thể dùng cho lịch trực mới");
        }
        if (!findExactSchedules(staff, req.workDate(), shift).isEmpty()) {
            throw new ConflictException("Nhân sự đã được phân công vào ca này");
        }
        validateNoOverlappingShift(staff, req.workDate(), shift, null);
        StaffSchedule schedule = StaffSchedule.builder()
                .staff(staff)
                .workDate(req.workDate())
                .shift(shift)
                .status(ScheduleStatus.SCHEDULED)
                .isCustom(req.isCustom() != null && req.isCustom())
                .note(req.note())
                .template(req.templateId() != null
                        ? templateRepo.findById(req.templateId()).orElse(null) : null)
                .build();
        StaffSchedule saved = scheduleRepo.save(schedule);
        
        if (staff.getProfile() != null) {
            try {
                notificationService.create(new NotificationCreateRequest(
                        staff.getProfile().getProfileId(),
                        NotificationType.GENERAL,
                        NotificationChannel.IN_APP,
                        "Phân công lịch trực mới",
                        String.format("Bạn vừa được phân công lịch trực vào ngày %s, ca %s.", saved.getWorkDate(), shift.getName()),
                        "StaffSchedule",
                        saved.getScheduleId()
                ));
            } catch (Exception e) {}
        }
        
        return ScheduleResponse.from(saved);
    }

    public ScheduleResponse update(UUID id, ScheduleUpdateRequest req) {
        StaffSchedule s = scheduleRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch làm việc không tồn tại: " + id));
        if (req.shiftId() != null) {
            LocalDate today = LocalDate.now(CLINIC_ZONE);
            if (s.getWorkDate().isBefore(today)
                    || (s.getWorkDate().equals(today)
                    && s.getShift() != null
                    && !java.time.LocalTime.now(CLINIC_ZONE).isBefore(java.time.LocalTime.parse(s.getShift().getStartTime())))) {
                throw new ConflictException("Không thể đổi ca trực đã bắt đầu hoặc đã qua");
            }
            ShiftConfig shift = shiftConfigRepo.findByIdForScheduleUpdate(req.shiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ca làm việc không tồn tại: " + req.shiftId()));
            if (Boolean.FALSE.equals(shift.getIsActive())) {
                throw new ConflictException("Ca làm việc đã ngừng hoạt động và không thể gán cho lịch trực");
            }
            validateNoOverlappingShift(s.getStaff(), s.getWorkDate(), shift, s.getScheduleId());
            s.setShift(shift);
        }
        if (req.status() != null) s.setStatus(req.status());
        if (req.isCustom() != null) s.setIsCustom(req.isCustom());
        if (req.note() != null) s.setNote(req.note());
        
        StaffSchedule updated = scheduleRepo.save(s);
        
        if (s.getStaff() != null && s.getStaff().getProfile() != null) {
            try {
                notificationService.create(new NotificationCreateRequest(
                        s.getStaff().getProfile().getProfileId(),
                        NotificationType.GENERAL,
                        NotificationChannel.IN_APP,
                        "Thay đổi lịch trực",
                        String.format("Lịch trực ngày %s, ca %s của bạn đã được cập nhật.", updated.getWorkDate(), updated.getShift().getName()),
                        "StaffSchedule",
                        updated.getScheduleId()
                ));
            } catch (Exception e) {}
        }
        
        return ScheduleResponse.from(updated);
    }

    public void delete(UUID id) {
        StaffSchedule s = scheduleRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lịch làm việc không tồn tại: " + id));
        if (s.getStatus() != ScheduleStatus.SCHEDULED
                || s.getWorkDate() == null
                || !s.getWorkDate().isAfter(LocalDate.now(CLINIC_ZONE))) {
            throw new ConflictException("Chỉ có thể xóa lịch trực chưa diễn ra trong tương lai");
        }
        if (attendanceRepository.findBySchedule_ScheduleId(id).isPresent()) {
            throw new ConflictException("Không thể xóa lịch trực đã phát sinh dữ liệu điểm danh");
        }
        
        if (s.getStaff() != null && s.getStaff().getProfile() != null) {
            try {
                notificationService.create(new NotificationCreateRequest(
                        s.getStaff().getProfile().getProfileId(),
                        NotificationType.GENERAL,
                        NotificationChannel.IN_APP,
                        "Hủy lịch trực",
                        String.format("Lịch trực ngày %s, ca %s của bạn đã bị hủy.", s.getWorkDate(), s.getShift().getName()),
                        "StaffSchedule",
                        s.getScheduleId()
                ));
            } catch (Exception e) {}
        }
        
        scheduleRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ScheduleResponse get(UUID id) {
        return ScheduleResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getForStaff(UUID id, UUID staffId) {
        StaffSchedule schedule = findById(id);
        if (schedule.getStaff() == null || !schedule.getStaff().getStaffId().equals(staffId)) {
            throw new ConflictException("Bạn không có quyền xem lịch trực của nhân viên khác");
        }
        return ScheduleResponse.from(schedule);
    }

    @Transactional(readOnly = true)
    public PageResponse<ScheduleResponse> search(UUID staffId, LocalDate from, LocalDate to,
                                                 UUID shiftId, Pageable pageable) {
        ShiftConfig shift = null;
        if (shiftId != null) {
            shift = shiftConfigRepo.findById(shiftId).orElse(null);
        }
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
    @Transactional
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

        LocalDate normalizedWeekStart = weekStart.with(DayOfWeek.MONDAY);
        scheduleRepo.findAllByWorkDateBetweenForUpdate(
                normalizedWeekStart, normalizedWeekStart.plusDays(6));

        List<StaffSchedule> toCreate = new ArrayList<>();
        for (StaffInfo staff : targets) {
            List<StaffScheduleTemplate> templates = templateRepo.findByStaff(staff);
            for (StaffScheduleTemplate t : templates) {
                if (Boolean.FALSE.equals(t.getIsActive())) continue;
                LocalDate workDate = dateForDayOfWeek(normalizedWeekStart, t.getDayOfWeek());
                List<StaffSchedule> existing = findExactSchedules(staff, workDate, t.getShift());
                if (!existing.isEmpty()) {
                    if (!override) continue;
                    // Du lieu cu co the da bi lap do thao tac sao chep truoc day.
                    // Override phai thay the toan bo, khong chi xoa mot ban ghi.
                    scheduleRepo.deleteAll(existing);
                    scheduleRepo.flush();
                }
                validateNoOverlappingShift(staff, workDate, t.getShift(), null);
                StaffSchedule schedule = StaffSchedule.builder()
                        .staff(staff)
                        .workDate(workDate)
                        .shift(t.getShift())
                        .status(ScheduleStatus.SCHEDULED)
                        .isCustom(false)
                        .template(t)
                        .build();
                toCreate.add(schedule);
            }
        }
        return scheduleRepo.saveAll(toCreate).stream().map(ScheduleResponse::from).toList();
    }

    public StaffSchedule findById(UUID id) {
        return scheduleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch làm việc không tồn tại: " + id));
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
        ShiftConfig shift = shiftConfigRepo.findByIdForScheduleUpdate(req.shiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Ca làm việc không tồn tại: " + req.shiftId()));
        StaffInfo staff = staffInfoRepository.findByIdForScheduleUpdate(req.staffId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân sự không tồn tại: " + req.staffId()));

        if (date.isBefore(LocalDate.now(CLINIC_ZONE))) {
            throw new ConflictException("Không thể thay đổi phân công của ngày đã qua");
        }
        List<StaffSchedule> schedulesOfDay = scheduleRepo
                .findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(), date);
        List<StaffSchedule> exactSchedules = schedulesOfDay.stream()
                .filter(schedule -> schedule.getShift() != null
                        && schedule.getShift().getShiftId().equals(shift.getShiftId()))
                .toList();

        if ("remove".equalsIgnoreCase(req.action())) {
            boolean hasAttendance = exactSchedules.stream().anyMatch(schedule -> attendanceRepository
                    .findBySchedule_ScheduleId(schedule.getScheduleId()).isPresent());
            if (hasAttendance) {
                throw new ConflictException("Không thể gỡ ca đã phát sinh dữ liệu điểm danh");
            }
            // Xoa tat ca ban trung cu neu du lieu cu da tung bi lap.
            scheduleRepo.deleteByStaffAndWorkDateAndShift(staff, date, shift);
            return;
        }
        if (!"add".equalsIgnoreCase(req.action())) {
            throw new ConflictException("Thao tác phân công không hợp lệ");
        }
        if (!Boolean.TRUE.equals(shift.getIsActive())) {
            throw new ConflictException("Ca làm việc đã ngừng hoạt động và không thể phân công");
        }
        if (staff.getProfile() == null || staff.getProfile().getAccount() == null
                || !Boolean.TRUE.equals(staff.getProfile().getAccount().getIsActive())) {
            throw new ConflictException("Nhân sự đã ngừng hoạt động và không thể phân công");
        }

        if (!exactSchedules.isEmpty()) return;

        validateNoOverlappingShift(staff, date, shift, null);

        scheduleRepo.save(StaffSchedule.builder()
                .staff(staff)
                .workDate(date)
                .shift(shift)
                .status(org.example.doansummer2026.enums.ScheduleStatus.SCHEDULED)
                .isCustom(true)
                .build());
    }

    /**
     * Sao chep lich tu tuan cu sang tuan moi.
     */
    public List<StaffSchedule> copyWeek(LocalDate fromWeek, LocalDate toWeek) {
        LocalDate sourceStart = fromWeek.with(DayOfWeek.MONDAY);
        LocalDate targetStart = toWeek.with(DayOfWeek.MONDAY);
        LocalDate targetEnd = targetStart.plusDays(6);
        if (sourceStart.equals(targetStart)) {
            throw new ConflictException("Tuần nguồn và tuần đích không được trùng nhau");
        }
        if (!targetStart.isAfter(LocalDate.now(CLINIC_ZONE))) {
            throw new ConflictException("Chỉ được sao chép lịch vào một tuần chưa bắt đầu");
        }

        // Khoa tuan nguon de hai quan ly khong the dong thoi tao hai bo lich
        // giong nhau cho cung mot tuan dich.
        List<StaffSchedule> sourceSchedules = scheduleRepo
                .findAllByWorkDateBetweenForUpdate(sourceStart, sourceStart.plusDays(6));
        if (sourceSchedules.isEmpty()) {
            throw new ConflictException("Tuần nguồn chưa có lịch trực để sao chép");
        }

        List<StaffSchedule> targetSchedules = scheduleRepo.findAllByWorkDateBetween(targetStart, targetEnd);
        boolean hasAttendance = targetSchedules.stream()
                .anyMatch(schedule -> attendanceRepository
                        .findBySchedule_ScheduleId(schedule.getScheduleId()).isPresent());
        if (hasAttendance) {
            throw new ConflictException(
                    "Không thể ghi đè tuần đã phát sinh dữ liệu điểm danh");
        }

        // Sao chep la thao tac thay the: xoa lich tuan dich truoc khi tao lai.
        // Nhờ vậy gọi API nhiều lần liên tiếp vẫn cho cùng một kết quả.
        scheduleRepo.deleteAll(targetSchedules);
        scheduleRepo.flush();

        Map<String, StaffSchedule> uniqueSourceSchedules = new LinkedHashMap<>();
        for (StaffSchedule source : sourceSchedules) {
            if (source.getStaff() == null || source.getShift() == null) continue;
            if (!Boolean.TRUE.equals(source.getShift().getIsActive())
                    || source.getStaff().getProfile() == null
                    || source.getStaff().getProfile().getAccount() == null
                    || !Boolean.TRUE.equals(source.getStaff().getProfile().getAccount().getIsActive())) {
                continue;
            }
            String key = source.getStaff().getStaffId()
                    + "|" + source.getWorkDate().getDayOfWeek()
                    + "|" + source.getShift().getShiftId();
            uniqueSourceSchedules.putIfAbsent(key, source);
        }

        List<StaffSchedule> replacements = uniqueSourceSchedules.values().stream()
                .map(source -> StaffSchedule.builder()
                        .staff(source.getStaff())
                        .workDate(targetStart.plusDays(
                                source.getWorkDate().getDayOfWeek().getValue()
                                        - DayOfWeek.MONDAY.getValue()))
                        .shift(source.getShift())
                        .status(source.getStatus())
                        .isCustom(true)
                        .note(source.getNote())
                        .build())
                .toList();
        return scheduleRepo.saveAll(replacements);
    }

    private List<StaffSchedule> findExactSchedules(StaffInfo staff, LocalDate workDate, ShiftConfig shift) {
        return scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(), workDate).stream()
                .filter(schedule -> schedule.getShift() != null
                        && schedule.getShift().getShiftId().equals(shift.getShiftId()))
                .toList();
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
            throw new IllegalArgumentException("Ngày trong tuần không hợp lệ: " + dayKey);
        }
        return weekStart.with(dow);
    }

    private void validateNoOverlappingShift(StaffInfo staff, LocalDate date,
                                            ShiftConfig shift, UUID excludedScheduleId) {
        LocalTime newStart = LocalTime.parse(shift.getStartTime());
        LocalTime newEnd = LocalTime.parse(shift.getEndTime());
        boolean overlaps = scheduleRepo.findAllByStaff_StaffIdAndWorkDate(staff.getStaffId(), date)
                .stream()
                .filter(schedule -> schedule.getShift() != null)
                .filter(schedule -> excludedScheduleId == null
                        || !schedule.getScheduleId().equals(excludedScheduleId))
                .anyMatch(schedule -> {
                    LocalTime existingStart = LocalTime.parse(schedule.getShift().getStartTime());
                    LocalTime existingEnd = LocalTime.parse(schedule.getShift().getEndTime());
                    return newStart.isBefore(existingEnd) && existingStart.isBefore(newEnd);
                });
        if (overlaps) {
            throw new ConflictException("Nhân sự đã có ca làm việc trùng thời gian trong ngày này");
        }
    }
}
