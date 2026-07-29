package org.example.doansummer2026.dto.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import org.example.doansummer2026.enums.Shift;
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
    public static String toKey(Shift shift, DayOfWeek dayOfWeek) {
        String shiftKey = shift.name().toLowerCase();
        String dayKey = dayOfWeek.name().substring(0, 3).toLowerCase();
        return shiftKey + "_" + dayKey;
    }

    /**
     * Tao response tu danh sach schedules trong 1 tuan.
     */
    public static ClinicManagerScheduleResponse from(List<StaffSchedule> schedules, LocalDate weekStart) {
        Map<String, List<StaffScheduleItemResponse>> scheduleMap = new LinkedHashMap<>();
        Set<Shift> shiftSet = new LinkedHashSet<>();
        Set<UUID> staffSet = new HashSet<>();

        // Khoi tao map rong cho 7 ngay/tuan voi 3 ca
        for (Shift shift : Shift.values()) {
            for (DayOfWeek day : DayOfWeek.values()) {
                scheduleMap.put(toKey(shift, day), new ArrayList<>());
            }
        }

        // Dien du lieu
        for (StaffSchedule s : schedules) {
            DayOfWeek dayOfWeek = s.getWorkDate().getDayOfWeek();
            String key = toKey(s.getShift(), dayOfWeek);
            scheduleMap.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(StaffScheduleItemResponse.from(s));
            shiftSet.add(s.getShift());
            if (s.getStaff() != null) {
                staffSet.add(s.getStaff().getStaffId());
            }
        }

        // Tao danh sach shifts
        List<ShiftResponse> shiftResps = Shift.values().length > 0
                ? Arrays.stream(Shift.values())
                        .map(s -> ShiftResponse.from(s, s.ordinal()))
                        .toList()
                : Collections.emptyList();

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