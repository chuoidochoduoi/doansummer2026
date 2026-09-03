package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.schedule.PublicDepartmentScheduleResponse;
import org.example.doansummer2026.enums.ClinicScheduleExceptionType;
import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.model.ClinicScheduleException;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.ShiftConfig;
import org.example.doansummer2026.model.StaffSchedule;
import org.example.doansummer2026.repository.ClinicScheduleExceptionRepository;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.repository.StaffScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicDepartmentScheduleService {

    private static final String PARACLINICAL_GROUP_ID = "PARACLINICAL";
    private static final String PARACLINICAL_GROUP_NAME = "Cận lâm sàng";

    private final StaffScheduleRepository scheduleRepository;
    private final DepartmentRepository departmentRepository;
    private final ShiftConfigRepository shiftRepository;
    private final ClinicScheduleExceptionRepository exceptionRepository;

    @Transactional(readOnly = true)
    public PublicDepartmentScheduleResponse getWeek(LocalDate requestedDate) {
        LocalDate weekStart = requestedDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<ShiftConfig> shifts = shiftRepository.findAllByIsActiveTrueOrderByStartTimeAsc();
        List<Department> departments = departmentRepository.findAll().stream()
                .filter(department -> department.getStatus() != DepartmentStatus.MAINTENANCE)
                .toList();
        List<StaffSchedule> schedules = scheduleRepository.findAllByWorkDateBetween(weekStart, weekEnd).stream()
                .filter(schedule -> schedule.getStatus() == ScheduleStatus.SCHEDULED)
                .filter(this::isMedicalSchedule)
                .toList();
        List<ClinicScheduleException> exceptions =
                exceptionRepository.findAllByWorkDateBetweenOrderByWorkDateAsc(weekStart, weekEnd);

        Set<ScheduleSlot> closedSlots = exceptions.stream()
                .filter(exception -> exception.getType() == ClinicScheduleExceptionType.CLOSED_DAY
                        || exception.getType() == ClinicScheduleExceptionType.SHIFT_OFF)
                .flatMap(exception -> closedSlots(exception, shifts).stream())
                .collect(Collectors.toSet());

        Map<GroupKey, List<Department>> groupedDepartments = new LinkedHashMap<>();
        departments.stream()
                .sorted(Comparator.comparing(Department::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(department -> {
                    GroupKey key = toGroupKey(department);
                    if (key != null) {
                        groupedDepartments.computeIfAbsent(key, ignored -> new ArrayList<>()).add(department);
                    }
                });

        List<PublicDepartmentScheduleResponse.DepartmentGroupItem> groups = groupedDepartments.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(GroupKey::sortOrder)
                        .thenComparing(GroupKey::name, String.CASE_INSENSITIVE_ORDER)))
                .map(entry -> toResponseGroup(entry.getKey(), entry.getValue(), shifts, schedules,
                        closedSlots, weekStart))
                .toList();

        List<PublicDepartmentScheduleResponse.ShiftItem> shiftItems = shifts.stream()
                .map(shift -> new PublicDepartmentScheduleResponse.ShiftItem(
                        shift.getShiftId(), shift.getName(), shift.getStartTime(), shift.getEndTime()))
                .toList();

        return new PublicDepartmentScheduleResponse(weekStart, weekEnd, shiftItems, groups);
    }

    private boolean isMedicalSchedule(StaffSchedule schedule) {
        return schedule.getStaff() != null
                && schedule.getStaff().getSystemRole() != null
                && (schedule.getStaff().getSystemRole().isDoctor()
                    || schedule.getStaff().getSystemRole() == SystemRole.NURSE)
                && schedule.getStaff().getDepartment() != null
                && schedule.getShift() != null;
    }

    private GroupKey toGroupKey(Department department) {
        if (department.getDepartmentType() != null && department.getDepartmentType().isParaclinical()) {
            return new GroupKey(PARACLINICAL_GROUP_ID, PARACLINICAL_GROUP_NAME, "PARACLINICAL", 1);
        }
        if (department.getSpecialization() == null
                || Boolean.FALSE.equals(department.getSpecialization().getActive())) {
            return null;
        }
        return new GroupKey(
                department.getSpecialization().getSpecializationId().toString(),
                department.getSpecialization().getName(),
                "CLINICAL",
                0);
    }

    private PublicDepartmentScheduleResponse.DepartmentGroupItem toResponseGroup(
            GroupKey key,
            List<Department> groupDepartments,
            List<ShiftConfig> shifts,
            List<StaffSchedule> schedules,
            Set<ScheduleSlot> closedSlots,
            LocalDate weekStart) {
        Set<UUID> departmentIds = groupDepartments.stream()
                .map(Department::getDepartmentId)
                .collect(Collectors.toSet());
        Set<ScheduleSlot> availableSlots = schedules.stream()
                .filter(schedule -> departmentIds.contains(
                        schedule.getStaff().getDepartment().getDepartmentId()))
                .filter(schedule -> "PARACLINICAL".equals(key.type())
                        || schedule.getStaff().getSystemRole().isDoctor())
                .map(schedule -> new ScheduleSlot(schedule.getWorkDate(), schedule.getShift().getShiftId()))
                .filter(slot -> !closedSlots.contains(slot))
                .collect(Collectors.toSet());

        List<PublicDepartmentScheduleResponse.AvailabilityItem> availability = new ArrayList<>();
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate date = weekStart.plusDays(dayOffset);
            for (ShiftConfig shift : shifts) {
                ScheduleSlot slot = new ScheduleSlot(date, shift.getShiftId());
                availability.add(new PublicDepartmentScheduleResponse.AvailabilityItem(
                        date, shift.getShiftId(), availableSlots.contains(slot)));
            }
        }
        return new PublicDepartmentScheduleResponse.DepartmentGroupItem(
                key.id(), key.name(), key.type(), List.copyOf(availability));
    }

    private Set<ScheduleSlot> closedSlots(ClinicScheduleException exception, List<ShiftConfig> shifts) {
        if (exception.getType() == ClinicScheduleExceptionType.CLOSED_DAY || exception.getShift() == null) {
            return shifts.stream()
                    .map(shift -> new ScheduleSlot(exception.getWorkDate(), shift.getShiftId()))
                    .collect(Collectors.toSet());
        }
        return Set.of(new ScheduleSlot(exception.getWorkDate(), exception.getShift().getShiftId()));
    }

    private record GroupKey(String id, String name, String type, int sortOrder) {}
    private record ScheduleSlot(LocalDate date, UUID shiftId) {}
}
