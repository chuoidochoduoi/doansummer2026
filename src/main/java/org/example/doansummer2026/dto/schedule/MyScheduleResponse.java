package org.example.doansummer2026.dto.schedule;

import org.example.doansummer2026.enums.Shift;
import org.example.doansummer2026.model.StaffSchedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * Response cho API lich ca nhan cua nhan su.
 */
public record MyScheduleResponse(
        List<ShiftResponse> shifts,
        Map<String, List<StaffScheduleItemResponse>> schedule,
        UUID myStaffId
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
     * Tao response tu danh sach schedules cua 1 nhan su trong 1 tuan.
     */
    public static MyScheduleResponse from(List<StaffSchedule> schedules, UUID staffId) {
        Map<String, List<StaffScheduleItemResponse>> scheduleMap = new LinkedHashMap<>();

        // Khoi tao map rong cho 7 ngay/tuan voi 3 ca
        for (Shift shift : Shift.values()) {
            for (DayOfWeek day : DayOfWeek.values()) {
                scheduleMap.put(toKey(shift, day), new ArrayList<>());
            }
        }

        // Dien du lieu - chi hien thi staffId cua minh
        for (StaffSchedule s : schedules) {
            DayOfWeek dayOfWeek = s.getWorkDate().getDayOfWeek();
            String key = toKey(s.getShift(), dayOfWeek);
            if (staffId != null && s.getStaff() != null && s.getStaff().getStaffId().equals(staffId)) {
                scheduleMap.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(StaffScheduleItemResponse.from(s));
            }
        }

        // Tao danh sach shifts
        List<ShiftResponse> shiftResps = Arrays.stream(Shift.values())
                .map(s -> ShiftResponse.from(s, s.ordinal()))
                .toList();

        return new MyScheduleResponse(shiftResps, scheduleMap, staffId);
    }
}