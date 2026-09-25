package vn.edu.fpt.cares.controller;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.RestResponses;
import vn.edu.fpt.cares.dto.schedule.PublicDepartmentScheduleResponse;
import vn.edu.fpt.cares.service.PublicDepartmentScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class PublicDepartmentScheduleController {

    private final PublicDepartmentScheduleService scheduleService;

    @GetMapping("/api/public/department-schedules")
    public ResponseEntity<PublicDepartmentScheduleResponse> getDepartmentSchedules(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return RestResponses.ok(scheduleService.getWeek(week == null ? LocalDate.now() : week));
    }
}
