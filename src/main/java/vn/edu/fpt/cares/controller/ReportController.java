package vn.edu.fpt.cares.controller;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.RestResponses;
import vn.edu.fpt.cares.dto.report.DashboardReportResponse;
import vn.edu.fpt.cares.dto.report.ServiceReportResponse;
import vn.edu.fpt.cares.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    public ResponseEntity<vn.edu.fpt.cares.dto.report.ClinicOverviewResponse> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return RestResponses.ok(reportService.getOverview(fromDate, toDate));
    }

    /**
     * Thong ke dashboard cho CLINIC_MANAGER.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    public ResponseEntity<DashboardReportResponse> getDashboard(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return RestResponses.ok(reportService.getDashboardReport(period, fromDate, toDate));
    }

    /**
     * Thong ke dich vu cho CLINIC_MANAGER.
     */
    @GetMapping("/services")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    public ResponseEntity<ServiceReportResponse> getServices(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return RestResponses.ok(reportService.getServiceReport(period, fromDate, toDate));
    }
}
