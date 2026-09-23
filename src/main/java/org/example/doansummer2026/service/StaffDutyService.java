package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.enums.StaffCapabilityStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.StaffSchedule;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.StaffScheduleRepository;
import org.example.doansummer2026.repository.StaffCapabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** Xac dinh nhan su thuc su duoc phan cong, khong su dung headDoctor. */
@Service
@RequiredArgsConstructor
public class StaffDutyService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final StaffScheduleRepository scheduleRepository;
    private final StaffInfoRepository staffRepository;
    private final StaffCapabilityRepository staffCapabilityRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public StaffInfo requireCurrentStaffOnDuty(Department department, boolean doctorRequired) {
        UUID staffId = authService.currentStaffId();
        if (staffId == null) throw new BadRequestException("Tài khoản hiện tại không phải nhân viên");
        StaffInfo staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân sự không tồn tại"));
        requireMembership(staff, department);
        requireEligibility(staff, department);
        if (doctorRequired && (staff.getSystemRole() == null || !staff.getSystemRole().isDoctor())) {
            throw new BadRequestException("Chỉ bác sĩ được phân ca tại phòng mới được thực hiện thao tác này");
        }
        if (!isOnDuty(staff, LocalDateTime.now(CLINIC_ZONE))) {
            throw new BadRequestException("Bạn không có lịch trực tại phòng trong ca hiện tại");
        }
        return staff;
    }

    /**
     * Enforces the clinic roster for operational counter roles that are not
     * attached to a treatment room. Administrators and clinic managers are
     * deliberately left as audited emergency overrides.
     */
    @Transactional(readOnly = true)
    public void requireCurrentStaffOnDuty(SystemRole roleToEnforce) {
        UUID staffId = authService.currentStaffId();
        if (staffId == null) return;
        StaffInfo staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân sự không tồn tại"));
        if (staff.getSystemRole() == null
                || staff.getSystemRole().normalized() != roleToEnforce.normalized()) {
            return;
        }
        if (!isOnDuty(staff, LocalDateTime.now(CLINIC_ZONE))) {
            throw new BadRequestException("Bạn hiện không trong ca trực nên không thể thực hiện thao tác này");
        }
    }

    @Transactional(readOnly = true)
    public boolean isOnDuty(StaffInfo staff, LocalDateTime at) {
        if (staff == null || at == null) return false;
        List<StaffSchedule> schedules = schedulesAround(staff.getStaffId(), at.toLocalDate());
        System.out.println("isOnDuty checking for staff " + staff.getStaffId() + " at " + at + ". Schedules around: " + schedules.size());
        return schedules.stream()
                .anyMatch(schedule -> contains(schedule, at));
    }

    @Transactional(readOnly = true)
    public List<StaffInfo> findOnDutyStaff(Department department, LocalDateTime at) {
        if (department == null) return List.of();
        List<StaffSchedule> schedules = new java.util.ArrayList<>();
        schedules.addAll(scheduleRepository.findAllByWorkDateAndStatus(
                at.toLocalDate(), ScheduleStatus.SCHEDULED));
        schedules.addAll(scheduleRepository.findAllByWorkDateAndStatus(
                at.toLocalDate().minusDays(1), ScheduleStatus.SCHEDULED));
        return schedules
                .stream()
                .filter(schedule -> schedule.getStaff() != null
                        && schedule.getStaff().getDepartment() != null
                        && department.getDepartmentId().equals(
                        schedule.getStaff().getDepartment().getDepartmentId()))
                .filter(schedule -> contains(schedule, at))
                .map(StaffSchedule::getStaff)
                .filter(staff -> staff.getProfile() != null
                        && staff.getProfile().getAccount() != null
                        && Boolean.TRUE.equals(staff.getProfile().getAccount().getIsActive()))
                .filter(staff -> isEligible(staff, department))
                .distinct()
                .toList();
    }

    private List<StaffSchedule> schedulesAround(UUID staffId, LocalDate date) {
        List<StaffSchedule> schedules = new java.util.ArrayList<>();
        schedules.addAll(scheduleRepository.findAllByStaff_StaffIdAndWorkDate(staffId, date));
        schedules.addAll(scheduleRepository.findAllByStaff_StaffIdAndWorkDate(staffId, date.minusDays(1)));
        return schedules;
    }

    public void requireMembership(StaffInfo staff, Department department) {
        if (department == null || staff == null || staff.getDepartment() == null
                || !department.getDepartmentId().equals(staff.getDepartment().getDepartmentId())) {
            throw new BadRequestException("Nhân sự không thuộc phòng thực hiện");
        }
    }

    public void requireEligibility(StaffInfo staff, Department department) {
        if (staff == null || department == null || staff.getSystemRole() == null) {
            throw new BadRequestException("Không xác định được năng lực nhân sự tại phòng");
        }
        DepartmentType type = department.getDepartmentType() == null
                ? DepartmentType.EXAMINATION : department.getDepartmentType().normalized();
        if (type == DepartmentType.EXAMINATION && staff.getSystemRole().isDoctor()
                && department.getSpecialization() != null) {
            if (staff.getSpecialization() == null || !department.getSpecialization().getSpecializationId()
                    .equals(staff.getSpecialization().getSpecializationId())) {
                throw new BadRequestException("Bác sĩ không có chuyên khoa phù hợp với phòng khám");
            }
            return;
        }
        if (!type.isParaclinical()) return;
        if (department.getCapabilities() == null || department.getCapabilities().isEmpty()) {
            throw new BadRequestException("Phòng cận lâm sàng chưa được cấu hình danh mục kỹ thuật");
        }
        boolean matches = department.getCapabilities().stream().anyMatch(capability ->
                staffCapabilityRepository.existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(
                        staff.getStaffId(), capability.getCapabilityId(), StaffCapabilityStatus.ACTIVE));
        if (!matches) {
            throw new BadRequestException("Nhân sự chưa có năng lực đang hiệu lực phù hợp với phòng cận lâm sàng");
        }
    }

    private boolean isEligible(StaffInfo staff, Department department) {
        try {
            requireEligibility(staff, department);
            return true;
        } catch (BadRequestException ignored) {
            return false;
        }
    }

    private boolean contains(StaffSchedule schedule, LocalDateTime at) {
        if (schedule.getStatus() != ScheduleStatus.SCHEDULED || schedule.getWorkDate() == null) {
            System.out.println("contains() false: status=" + schedule.getStatus() + " date=" + schedule.getWorkDate() + " for schedule " + schedule.getScheduleId());
            return false;
        }
        LocalTime start = schedule.getActualStartTime();
        LocalTime end = schedule.getActualEndTime();
        if (start == null && schedule.getShift() != null) start = LocalTime.parse(schedule.getShift().getStartTime());
        if (end == null && schedule.getShift() != null) end = LocalTime.parse(schedule.getShift().getEndTime());
        if (start == null || end == null) {
            System.out.println("contains() false: start=" + start + " end=" + end + " for schedule " + schedule.getScheduleId());
            return false;
        }
        LocalDate date = schedule.getWorkDate();
        LocalDateTime from = LocalDateTime.of(date, start);
        boolean finalBoundary = end.equals(LocalTime.of(23, 59, 59));
        LocalDateTime to = finalBoundary
                ? date.plusDays(1).atStartOfDay()
                : LocalDateTime.of(end.isAfter(start) ? date : date.plusDays(1), end);
        boolean contains = !at.isBefore(from) && at.isBefore(to);
        if (!contains) {
            System.out.println("contains() false: at=" + at + " not between from=" + from + " and to=" + to + " for schedule " + schedule.getScheduleId());
        }
        return contains;
    }
}
