package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.report.DashboardReportResponse;
import org.example.doansummer2026.dto.report.ServiceReportResponse;
import org.example.doansummer2026.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Thong ke dashboard cho CLINIC_MANAGER.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    public ResponseEntity<DashboardReportResponse> getDashboard(
            @RequestParam(defaultValue = "month") String period) {
        return RestResponses.ok(reportService.getDashboardReport(period));
    }

    /**
     * Thong ke dich vu cho CLINIC_MANAGER.
     */
    @GetMapping("/services")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    public ResponseEntity<ServiceReportResponse> getServices(
            @RequestParam(defaultValue = "month") String period) {
        return RestResponses.ok(reportService.getServiceReport(period));
    }
}