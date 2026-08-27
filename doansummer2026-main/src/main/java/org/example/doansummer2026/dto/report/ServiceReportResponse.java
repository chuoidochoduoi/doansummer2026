package org.example.doansummer2026.dto.report;

import java.util.List;

/**
 * Response cho API thong ke dich vu (Tab 2 - Thong ke dich vu).
 */
public record ServiceReportResponse(
        long totalRevenue,
        int totalSessions,
        double avgPerSession,
        long bhytTotal,
        double bhytRate,
        List<BreakdownItem> breakdown,
        List<ServiceStat> table
) {
    public record BreakdownItem(String label, double pct, long amount) {}

    public record ServiceStat(
            String category,
            String name,
            String note,
            int totalOrders,
            long unitPrice,
            long totalRevenue,
            int bhytQty,
            long bhytFund
    ) {}
}