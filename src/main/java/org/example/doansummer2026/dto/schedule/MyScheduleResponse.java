package org.example.doansummer2026.dto.schedule;

import org.example.doansummer2026.model.ShiftConfig;
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
    public static String toKey(String shiftIdStr, DayOfWeek dayOfWeek) {
        String dayKey = dayOfWeek.name().substring(0, 3).toLowerCase();
        return shiftIdStr + "_" + dayKey;
    }

    /**
     * Tao response tu danh sach schedules cua 1 nhan su trong 1 tuan.
     */
    public static MyScheduleResponse from(List<StaffSchedule> schedules, UUID staffId, List<ShiftConfig> allShifts) {
        Map<String, List<StaffScheduleItemResponse>> scheduleMap = new LinkedHashMap<>();

        // Khoi tao map rong cho 7 ngay/tuan voi cac ca
        for (ShiftConfig shift : allShifts) {
            for (DayOfWeek day : DayOfWeek.values()) {
                scheduleMap.put(toKey(shift.getShiftId().toString(), day), new ArrayList<>());
            }
        }

        // Dien du lieu - chi hien thi staffId cua minh
        for (StaffSchedule s : schedules) {
            DayOfWeek dayOfWeek = s.getWorkDate().getDayOfWeek();
            String key = toKey(s.getShift().getShiftId().toString(), dayOfWeek);
            if (staffId != null && s.getStaff() != null && s.getStaff().getStaffId().equals(staffId)) {
                scheduleMap.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(StaffScheduleItemResponse.from(s));
            }
        }

        // Tao danh sach shifts
        List<ShiftResponse> shiftResps = allShifts.stream()
                .map(ShiftResponse::from)
                .toList();

        return new MyScheduleResponse(shiftResps, scheduleMap, staffId);
    }
}