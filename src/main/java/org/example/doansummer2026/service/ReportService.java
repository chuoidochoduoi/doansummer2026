package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.report.DashboardReportResponse;
import org.example.doansummer2026.dto.report.ServiceReportResponse;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.InvoiceItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final InvoiceRepository invoiceRepo;
    private final MedicalServiceRepository serviceRepo;
    private final QueueTicketRepository queueTicketRepo;
    private final DepartmentRepository departmentRepo;
    private final InvoiceItemRepository invoiceItemRepo;

    public DashboardReportResponse getDashboardReport(String period, LocalDate customFrom, LocalDate customTo) {
        LocalDate now = LocalDate.now();
        LocalDate from, to;

        if (customFrom != null && customTo != null) {
            from = customFrom;
            to = customTo;
        } else {
            switch (period.toLowerCase()) {
                case "day" -> { from = now; to = now; }
                case "quarter" -> {
                    int q = (now.getMonthValue() - 1) / 3 + 1;
                    from = LocalDate.of(now.getYear(), (q - 1) * 3 + 1, 1);
                    to = from.plusMonths(2).withDayOfMonth(from.plusMonths(2).lengthOfMonth());
                }
                case "year" -> { from = now.withDayOfYear(1); to = now.withDayOfYear(now.lengthOfYear()); }
                default -> { YearMonth ym = YearMonth.from(now); from = ym.atDay(1); to = ym.atEndOfMonth(); }
            }
        }

        // 1. Doanh thu theo tháng
        List<DashboardReportResponse.ChartItem> revenueChart = getRevenueChart(from, to, period);

        // 2. Phân bố ca khám theo khoa
        List<DashboardReportResponse.ChartItem> sessionChart = getSessionChart(from, to);

        // 3. Tổng số ca hoàn thành
        int totalSessions = (int) queueTicketRepo.findAll().stream()
                .filter(q -> q.getStatus() == QueueStatus.DONE)
                .filter(q -> !q.getUpdatedAt().toLocalDate().isBefore(from) && !q.getUpdatedAt().toLocalDate().isAfter(to))
                .count();

        // 4. Bảng thống kê theo khoa
        List<DashboardReportResponse.DepartmentStat> table = getDepartmentStats(from, to);

        return new DashboardReportResponse(revenueChart, sessionChart, totalSessions, table);
    }

    public ServiceReportResponse getServiceReport(String period, LocalDate customFrom, LocalDate customTo) {
        LocalDate now = LocalDate.now();
        LocalDate from, to;

        if (customFrom != null && customTo != null) {
            from = customFrom;
            to = customTo;
        } else {
            switch (period.toLowerCase()) {
                case "day" -> { from = now; to = now; }
                case "quarter" -> {
                    int q = (now.getMonthValue() - 1) / 3 + 1;
                    from = LocalDate.of(now.getYear(), (q - 1) * 3 + 1, 1);
                    to = from.plusMonths(2).withDayOfMonth(from.plusMonths(2).lengthOfMonth());
                }
                case "year" -> { from = now.withDayOfYear(1); to = now.withDayOfYear(now.lengthOfYear()); }
                default -> { YearMonth ym = YearMonth.from(now); from = ym.atDay(1); to = ym.atEndOfMonth(); }
            }
        }

        // 1. Tổng doanh thu (PAID invoices)
        long totalRevenue = invoiceRepo.findAll().stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .filter(i -> !i.getIssueDate().isBefore(from) && !i.getIssueDate().isAfter(to))
                .mapToLong(i -> i.getTotalAmount().longValue())
                .sum();

        // 2. Tổng ca
        int totalSessions = (int) queueTicketRepo.findAll().stream()
                .filter(q -> q.getStatus() == QueueStatus.DONE)
                .filter(q -> !q.getUpdatedAt().toLocalDate().isBefore(from) && !q.getUpdatedAt().toLocalDate().isAfter(to))
                .count();

        // 3. Bảng chi tiết dịch vụ với BHYT thực tế
        List<ServiceReportResponse.ServiceStat> table = getServiceStats(from, to);

        // 4. Breakdown theo category
        long calcTotalRevenue = table.stream().mapToLong(ServiceReportResponse.ServiceStat::totalRevenue).sum();
        List<ServiceReportResponse.BreakdownItem> breakdown = table.stream()
                .collect(Collectors.groupingBy(
                        ServiceReportResponse.ServiceStat::category,
                        Collectors.summingLong(ServiceReportResponse.ServiceStat::totalRevenue)
                ))
                .entrySet().stream()
                .map(e -> {
                    double pct = calcTotalRevenue > 0 ? (e.getValue() * 100.0 / calcTotalRevenue) : 0.0;
                    return new ServiceReportResponse.BreakdownItem(e.getKey(), Math.round(pct * 10.0) / 10.0, e.getValue());
                })
                .sorted((a, b) -> Double.compare(b.pct(), a.pct()))
                .collect(Collectors.toList());

        // 5. Tính tổng BHYT từ tất cả InvoiceItem
        long bhytTotal = invoiceItemRepo.findAll().stream()
                .mapToLong(ii -> ii.getBhytFund() != null ? ii.getBhytFund().longValue() : 0L)
                .sum();

        double bhytRate = totalRevenue > 0 ? (double) bhytTotal / totalRevenue * 100 : 0.0;

        return new ServiceReportResponse(
                totalRevenue,
                totalSessions,
                totalSessions > 0 ? (double) totalRevenue / totalSessions : 0.0,
                bhytTotal,
                bhytRate,
                breakdown,
                table
        );
    }

    private List<DashboardReportResponse.ChartItem> getRevenueChart(LocalDate from, LocalDate to, String period) {
        List<DashboardReportResponse.ChartItem> list = invoiceRepo.findAll().stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .filter(i -> !i.getIssueDate().isBefore(from) && !i.getIssueDate().isAfter(to))
                .collect(Collectors.groupingBy(
                        i -> {
                            if (period.equalsIgnoreCase("year") || period.equalsIgnoreCase("quarter")) {
                                return YearMonth.from(i.getIssueDate()).toString();
                            }
                            return i.getIssueDate().toString();
                        },
                        Collectors.summingLong(i -> i.getTotalAmount().longValue())
                ))
                .entrySet().stream()
                .map(e -> new DashboardReportResponse.ChartItem(e.getKey(), e.getValue().doubleValue()))
                .sorted(Comparator.comparing(DashboardReportResponse.ChartItem::label))
                .collect(Collectors.toList());
                
        return list;
    }

    private List<DashboardReportResponse.ChartItem> getSessionChart(LocalDate from, LocalDate to) {
        return queueTicketRepo.findAll().stream()
                .filter(q -> q.getStatus() == QueueStatus.DONE)
                .filter(q -> !q.getUpdatedAt().toLocalDate().isBefore(from) && !q.getUpdatedAt().toLocalDate().isAfter(to))
                .collect(Collectors.groupingBy(
                        q -> q.getDepartment().getName(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> new DashboardReportResponse.ChartItem(e.getKey(), (double) e.getValue()))
                .toList();
    }

    private List<DashboardReportResponse.DepartmentStat> getDepartmentStats(LocalDate from, LocalDate to) {
        return departmentRepo.findAll().stream()
                .map(d -> {
                    // Đếm số ca DONE trong khoảng thời gian
                    int sessions = (int) queueTicketRepo.findAll().stream()
                            .filter(q -> q.getDepartment().getDepartmentId().equals(d.getDepartmentId()))
                            .filter(q -> q.getStatus() == QueueStatus.DONE)
                            .filter(q -> !q.getUpdatedAt().toLocalDate().isBefore(from) && !q.getUpdatedAt().toLocalDate().isAfter(to))
                            .count();
                    long revenue = sessions * 300000L; // Tạm tính: trung bình 300k/ca
                    
                    // Mock data cho occupancy va csat
                    int occupancy = sessions > 0 ? Math.min(100, 40 + (sessions * 5)) : 0;
                    double csat = sessions > 0 ? Math.round((4.0 + Math.random()) * 10.0) / 10.0 : 0.0;
                    if (csat > 5.0) csat = 5.0;

                    return new DashboardReportResponse.DepartmentStat(
                            d.getRoomCode(),
                            d.getName(),
                            revenue,
                            sessions,
                            occupancy,
                            csat
                    );
                })
                .toList();
    }



    private List<ServiceReportResponse.ServiceStat> getServiceStats(LocalDate from, LocalDate to) {
        return serviceRepo.findAll().stream()
                .map(s -> {
                    List<org.example.doansummer2026.model.InvoiceItem> items = invoiceItemRepo.findAll().stream()
                            .filter(ii -> ii.getService().getServiceId().equals(s.getServiceId()))
                            .filter(ii -> ii.getInvoice().getStatus() == InvoiceStatus.PAID)
                            .filter(ii -> !ii.getInvoice().getIssueDate().isBefore(from) && !ii.getInvoice().getIssueDate().isAfter(to))
                            .toList();

                    int totalOrders = items.size();
                    int bhytQty = (int) items.stream().filter(ii -> ii.getBhytFund() != null && ii.getBhytFund().longValue() > 0).count();
                    long bhytFund = items.stream().mapToLong(ii -> ii.getBhytFund() != null ? ii.getBhytFund().longValue() : 0L).sum();

                    return new ServiceReportResponse.ServiceStat(
                            s.getDepartmentType() != null ? s.getDepartmentType().name() : "Khác",
                            s.getName(),
                            s.getDescription() != null ? s.getDescription() : "",
                            totalOrders,
                            s.getPrice().longValue(),
                            s.getPrice().longValue() * totalOrders,
                            bhytQty,
                            bhytFund
                    );
                })
                .toList();
    }
}