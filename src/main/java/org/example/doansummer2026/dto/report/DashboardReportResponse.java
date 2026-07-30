package org.example.doansummer2026.dto.report;

import java.util.List;

/**
 * Response cho API thong ke dashboard (Tab 1 - Thong ke khoa/phong).
 */
public record DashboardReportResponse(
        List<ChartItem> revenueChart,
        List<ChartItem> sessionChart,
        int totalSessions,
        List<DepartmentStat> table
) {
    public record ChartItem(String label, double value) {}

    public record DepartmentStat(
            String code,
            String dept,
            long revenue,
            int sessions,
            int occupancy,
            double csat
    ) {}
}