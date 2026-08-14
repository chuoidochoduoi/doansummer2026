package org.example.doansummer2026.dto.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import org.example.doansummer2026.model.ShiftConfig;
import org.example.doansummer2026.model.StaffSchedule;

/**
 * Response cho API lich truc Clinic Manager.
 */
public record ClinicManagerScheduleResponse(
        Map<String, List<StaffScheduleItemResponse>> schedule,
        List<ShiftResponse> shifts,
        List<StaffScheduleItemResponse> staff
) {
    /**
     * Tao key cho schedule: shiftId_dayKey (vi du: morning_mon, afternoon_tue)
     */
    public static String toKey(String shiftIdStr, DayOfWeek dayOfWeek) {
        String dayKey = dayOfWeek.name().substring(0, 3).toLowerCase();
        return shiftIdStr + "_" + dayKey;
    }

    /**
     * Tao response tu danh sach schedules trong 1 tuan.
     */
    public static ClinicManagerScheduleResponse from(List<StaffSchedule> schedules, LocalDate weekStart, List<ShiftConfig> allShifts) {
        Map<String, List<StaffScheduleItemResponse>> scheduleMap = new LinkedHashMap<>();
        Set<UUID> staffSet = new HashSet<>();

        // Khoi tao map rong cho 7 ngay/tuan voi cac ca
        for (ShiftConfig shift : allShifts) {
            for (DayOfWeek day : DayOfWeek.values()) {
                scheduleMap.put(toKey(shift.getShiftId().toString(), day), new ArrayList<>());
            }
        }

        // Dien du lieu
        for (StaffSchedule s : schedules) {
            DayOfWeek dayOfWeek = s.getWorkDate().getDayOfWeek();
            String key = toKey(s.getShift().getShiftId().toString(), dayOfWeek);
            StaffScheduleItemResponse item = StaffScheduleItemResponse.from(s);
            List<StaffScheduleItemResponse> cell = scheduleMap.computeIfAbsent(key, k -> new ArrayList<>());
            if (cell.stream().noneMatch(existing -> Objects.equals(existing.staffId(), item.staffId()))) {
                cell.add(item);
            }
            if (s.getStaff() != null) {
                staffSet.add(s.getStaff().getStaffId());
            }
        }

        // Tao danh sach shifts
        List<ShiftResponse> shiftResps = allShifts.stream()
                .map(ShiftResponse::from)
                .toList();

        // Lay danh sach staff (trong thuc te se query StaffService)
        Map<UUID, StaffScheduleItemResponse> staffMap = scheduleMap.values().stream()
                .flatMap(List::stream)
                .collect(LinkedHashMap::new, (map, item) -> {
                    if (!map.containsKey(item.staffId())) map.put(item.staffId(), item);
                }, LinkedHashMap::putAll);
        List<StaffScheduleItemResponse> staffResps = new ArrayList<>(staffMap.values());

        return new ClinicManagerScheduleResponse(scheduleMap, shiftResps, staffResps);
    }
}
