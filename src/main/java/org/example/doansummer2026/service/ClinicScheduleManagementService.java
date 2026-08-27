package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.shift.*;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.*;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ClinicScheduleManagementService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate BASELINE_DATE = LocalDate.of(1970, 1, 1);
    private static final List<AppointmentStatus> ACTIVE_APPOINTMENTS =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.RESCHEDULED);

    private final ShiftConfigRepository shiftRepository;
    private final ShiftVersionRepository versionRepository;
    private final ClinicScheduleExceptionRepository exceptionRepository;
    private final StaffScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final AccountRepository accountRepository;
    private final ShiftScheduleResolver resolver;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureInitialVersions() {
        for (ShiftConfig shift : shiftRepository.findAll()) {
            if (!ShiftConfigService.isFixedShift(shift)) continue;
            if (!versionRepository.existsByShift_ShiftId(shift.getShiftId())) {
                versionRepository.save(ShiftVersion.builder()
                        .shift(shift).startTime(LocalTime.parse(shift.getStartTime()))
                        .endTime(LocalTime.parse(shift.getEndTime())).effectiveFrom(BASELINE_DATE)
                        .changeReason("Initial version migrated from shift configuration").build());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ShiftVersionResponse> history(UUID shiftId) {
        requireShift(shiftId);
        return versionRepository.findAllByShift_ShiftIdOrderByEffectiveFromDesc(shiftId)
                .stream().map(ShiftVersionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ScheduleImpactResponse previewVersionImpact(UUID shiftId, LocalDate effectiveFrom) {
        ShiftConfig shift = requireShift(shiftId);
        return impactFrom(shift, effectiveFrom);
    }

    public ShiftVersionResponse createVersion(UUID shiftId, ShiftVersionCreateRequest request) {
        ShiftConfig shift = requireShift(shiftId);
        validateFuture(request.effectiveFrom());
        validateTimes(request.startTime(), request.endTime());
        String reason = normalizeReason(request.changeReason());

        ScheduleImpactResponse impact = impactFrom(shift, request.effectiveFrom());
        rejectImpact(impact);

        List<ShiftVersion> existing = versionRepository.findAllByShiftForUpdate(shiftId);
        ShiftVersion latest = existing.stream().max(Comparator.comparing(ShiftVersion::getEffectiveFrom)).orElse(null);
        if (latest != null && !request.effectiveFrom().isAfter(latest.getEffectiveFrom())) {
            throw new ConflictException("Ngày áp dụng phải sau phiên bản ca mới nhất");
        }
        validateVersionOverlapWithOtherShifts(shiftId, request.startTime(), request.endTime(), request.effectiveFrom());
        if (latest != null) latest.setEffectiveTo(request.effectiveFrom().minusDays(1));

        ShiftVersion saved = versionRepository.save(ShiftVersion.builder()
                .shift(shift).startTime(request.startTime()).endTime(request.endTime())
                .effectiveFrom(request.effectiveFrom()).changeReason(reason).createdBy(currentAccountId()).build());
        return ShiftVersionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ClinicScheduleExceptionResponse> exceptions(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) throw new BadRequestException("Khoảng ngày không hợp lệ");
        return exceptionRepository.findAllByWorkDateBetweenOrderByWorkDateAsc(from, to)
                .stream().map(ClinicScheduleExceptionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ScheduleImpactResponse previewExceptionImpact(ClinicScheduleExceptionRequest request) {
        ShiftConfig shift = request.shiftId() == null ? null : requireShift(request.shiftId());
        return impactOn(request.workDate(), shift);
    }

    public ClinicScheduleExceptionResponse createException(ClinicScheduleExceptionRequest request) {
        validateFuture(request.workDate());
        validateExceptionShape(request);
        ShiftConfig shift = request.shiftId() == null ? null : requireShift(request.shiftId());
        List<ClinicScheduleException> locked = exceptionRepository.findAllByWorkDateForUpdate(request.workDate());
        ScheduleImpactResponse impact = impactOn(request.workDate(), shift);
        rejectImpact(impact);

        if (request.type() != ClinicScheduleExceptionType.CLOSED_DAY && locked.stream()
                .anyMatch(e -> e.getType() == ClinicScheduleExceptionType.CLOSED_DAY)) {
            throw new ConflictException("Phòng khám đang đóng toàn bộ trong ngày này. Hãy mở ngày trước");
        }
        if (request.type() == ClinicScheduleExceptionType.SPECIAL_HOURS) {
            validateSpecialHoursOverlap(request.workDate(), shift, request.specialStartTime(), request.specialEndTime());
        }

        if (request.type() == ClinicScheduleExceptionType.CLOSED_DAY) {
            locked.forEach(exceptionRepository::delete);
        } else {
            locked.stream().filter(e -> e.getShift() != null
                    && e.getShift().getShiftId().equals(shift.getShiftId())).forEach(exceptionRepository::delete);
        }
        ClinicScheduleException saved = exceptionRepository.save(ClinicScheduleException.builder()
                .workDate(request.workDate()).shift(shift).type(request.type())
                .specialStartTime(request.specialStartTime()).specialEndTime(request.specialEndTime())
                .reason(normalizeReason(request.reason())).createdBy(currentAccountId()).build());
        return ClinicScheduleExceptionResponse.from(saved);
    }

    public void reopen(UUID exceptionId) {
        ClinicScheduleException value = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Ngoại lệ lịch không tồn tại"));
        if (!value.getWorkDate().isAfter(LocalDate.now(CLINIC_ZONE))) {
            throw new ConflictException("Không thể thay đổi ngoại lệ đã xảy ra");
        }
        exceptionRepository.delete(value);
    }

    private ScheduleImpactResponse impactFrom(ShiftConfig shift, LocalDate from) {
        List<StaffSchedule> schedules = scheduleRepository.findAllByWorkDateBetween(from, LocalDate.of(9999, 12, 31))
                .stream().filter(s -> s.getStatus() == ScheduleStatus.SCHEDULED && s.getShift() != null
                        && s.getShift().getShiftId().equals(shift.getShiftId())).toList();
        List<Appointment> appointments = appointmentRepository.findActiveBetween(from.atStartOfDay(),
                        LocalDateTime.of(9999, 12, 31, 23, 59), ACTIVE_APPOINTMENTS).stream()
                .filter(a -> appointmentUsesShift(a, shift)).toList();
        return impact(appointments, schedules);
    }

    private ScheduleImpactResponse impactOn(LocalDate date, ShiftConfig shift) {
        List<StaffSchedule> schedules = scheduleRepository.findAllByWorkDateAndStatus(date, ScheduleStatus.SCHEDULED)
                .stream().filter(s -> shift == null || (s.getShift() != null
                        && s.getShift().getShiftId().equals(shift.getShiftId()))).toList();
        List<Appointment> appointments = appointmentRepository.findActiveBetween(date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay(), ACTIVE_APPOINTMENTS).stream()
                .filter(a -> shift == null || appointmentUsesShift(a, shift)).toList();
        return impact(appointments, schedules);
    }

    private ScheduleImpactResponse impact(List<Appointment> appointments, List<StaffSchedule> schedules) {
        Set<UUID> services = new LinkedHashSet<>();
        Set<LocalDate> dates = new TreeSet<>();
        appointments.forEach(a -> { dates.add(a.getScheduledAt().toLocalDate());
            a.getServices().forEach(s -> services.add(s.getServiceId())); });
        schedules.forEach(s -> dates.add(s.getWorkDate()));
        return new ScheduleImpactResponse(!appointments.isEmpty() || !schedules.isEmpty(), appointments.size(),
                schedules.size(), services, new ArrayList<>(dates),
                appointments.stream().map(Appointment::getAppointmentId).toList(),
                schedules.stream().map(StaffSchedule::getScheduleId).toList());
    }

    private boolean appointmentUsesShift(Appointment appointment, ShiftConfig shift) {
        return appointment.getShiftVersion() != null
                && appointment.getShiftVersion().getShift().getShiftId().equals(shift.getShiftId())
                || appointment.getShiftVersion() == null && Objects.equals(appointment.getShiftName(), shift.getName());
    }

    private void rejectImpact(ScheduleImpactResponse impact) {
        if (impact.blocked()) throw new ConflictException("Không thể thay đổi lịch: còn "
                + impact.appointmentCount() + " lịch hẹn và " + impact.staffScheduleCount()
                + " lịch nhân viên bị ảnh hưởng. Vui lòng xử lý trước");
    }

    private void validateExceptionShape(ClinicScheduleExceptionRequest request) {
        if (request.type() == ClinicScheduleExceptionType.CLOSED_DAY && request.shiftId() != null)
            throw new BadRequestException("Nghỉ toàn ngày không được chọn ca");
        if (request.type() != ClinicScheduleExceptionType.CLOSED_DAY && request.shiftId() == null)
            throw new BadRequestException("Vui lòng chọn ca");
        if (request.type() == ClinicScheduleExceptionType.SPECIAL_HOURS)
            validateTimes(request.specialStartTime(), request.specialEndTime());
        else if (request.specialStartTime() != null || request.specialEndTime() != null)
            throw new BadRequestException("Chỉ giờ đặc biệt mới được nhập thời gian");
    }

    private void validateSpecialHoursOverlap(LocalDate date, ShiftConfig target, LocalTime start, LocalTime end) {
        for (ShiftConfig other : shiftRepository.findAllByIsActiveTrueOrderByStartTimeAsc()) {
            if (!ShiftConfigService.isFixedShift(other)) continue;
            if (other.getShiftId().equals(target.getShiftId())) continue;
            ShiftScheduleResolver.ResolvedShift resolved = resolver.resolve(other, date);
            if (resolved.available() && overlaps(start, end, resolved.startTime(), resolved.endTime()))
                throw new ConflictException("Giờ đặc biệt trùng với " + other.getName());
        }
    }

    private void validateVersionOverlapWithOtherShifts(UUID shiftId, LocalTime start, LocalTime end, LocalDate from) {
        for (ShiftVersion other : versionRepository.findAll()) {
            if (!ShiftConfigService.isFixedShift(other.getShift())) continue;
            if (other.getShift().getShiftId().equals(shiftId)) continue;
            boolean rangesOverlap = other.getEffectiveTo() == null || !other.getEffectiveTo().isBefore(from);
            if (rangesOverlap && overlaps(start, end, other.getStartTime(), other.getEndTime()))
                throw new ConflictException("Khung giờ mới trùng với ca " + other.getShift().getName());
        }
    }

    private boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }

    private void validateTimes(LocalTime start, LocalTime end) {
        if (start == null || end == null || !start.isBefore(end))
            throw new BadRequestException("Thời gian bắt đầu phải trước thời gian kết thúc");
    }

    private void validateFuture(LocalDate date) {
        if (date == null || !date.isAfter(LocalDate.now(CLINIC_ZONE)))
            throw new BadRequestException("Ngày áp dụng phải từ ngày mai trở đi");
    }

    private String normalizeReason(String value) {
        String result = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (result.isBlank()) throw new BadRequestException("Vui lòng nhập lý do thay đổi");
        return result;
    }

    private ShiftConfig requireShift(UUID id) {
        ShiftConfig shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ca khám không tồn tại"));
        if (!ShiftConfigService.isFixedShift(shift)) {
            throw new BadRequestException("Hệ thống chỉ hỗ trợ Ca Sáng, Ca Chiều và Ca Tối");
        }
        return shift;
    }

    private UUID currentAccountId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return accountRepository.findFirstByUsername(authentication.getName()).map(Account::getAccountId).orElse(null);
    }
}
