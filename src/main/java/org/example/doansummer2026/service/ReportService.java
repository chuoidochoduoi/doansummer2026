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
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.TransactionRepository;
import org.example.doansummer2026.repository.MembershipCardLedgerRepository;
import org.example.doansummer2026.dto.report.ClinicOverviewResponse;
import java.time.temporal.ChronoUnit;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.enums.TransactionStatus;
import org.example.doansummer2026.enums.PaymentMethod;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.enums.VisitStatus;
import java.math.BigDecimal;
import org.example.doansummer2026.enums.MembershipLedgerType;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final CustomerVisitRepository visitRepo;
    private final TransactionRepository transactionRepo;
    private final MembershipCardLedgerRepository ledgerRepo;

    /** Canonical report for the manager UI; old response contracts remain available. */
    public ClinicOverviewResponse getOverview(LocalDate from, LocalDate to) {
        if (from == null && to == null) from = to = LocalDate.now(CLINIC_ZONE);
        if (from == null || to == null || to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366) {
            throw new BadRequestException("Chọn khoảng ngày hợp lệ, tối đa 367 ngày.");
        }
        final LocalDate start = from, end = to;
        var visits = visitRepo.findAll();
        var queues = queueTicketRepo.findAll();
        var records = medicalRecordRepo.findAll();
        var tests = testRequestRepo.findAll();
        var invoices = invoiceRepo.findAll().stream().filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
                .filter(i -> inPeriod(i.getIssueDate(), start, end)).toList();
        var invoiceIds = invoices.stream().map(Invoice::getInvoiceId).collect(Collectors.toSet());
        var items = invoiceItemRepo.findAll().stream().filter(i -> i.getInvoice() != null
                && invoiceIds.contains(i.getInvoice().getInvoiceId())).toList();
        // A cancelled/failed transaction is never counted, even when its invoice still exists.
        var payments = transactionRepo.findAll().stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> t.getPaymentMethod() != PaymentMethod.INSURANCE)
                .filter(t -> inPeriod(t.getPaidAt(), start, end)).toList();
        var signedTests = tests.stream()
                .filter(t -> t.getTestResult() != null && t.getTestResult().getVerifiedAt() != null
                        && t.getTestResult().getVerifiedBy() != null)
                .map(t -> t.getTestRequestId()).collect(Collectors.toSet());
        var completedRecords = records.stream()
                .filter(r -> r.getStatus() == MedicalRecordStatus.COMPLETED)
                .filter(r -> r.getQueueTicket() != null && r.getQueueTicket().getService() != null
                        && r.getQueueTicket().getService().getDepartmentType() == DepartmentType.EXAMINATION)
                .filter(r -> inPeriod(r.getCompletedAt(), start, end)).toList();
        var completedTests = tests.stream()
                .filter(t -> t.getStatus() == TestRequestStatus.COMPLETED)
                .filter(t -> signedTests.contains(t.getTestRequestId()) && inPeriod(t.getCompletedAt(), start, end)).toList();
        var skippedVisits = queues.stream().filter(q -> q.getStatus() == QueueStatus.SKIPPED && q.getVisit() != null)
                .map(q -> q.getVisit().getVisitId()).collect(Collectors.toSet());
        var closed = visits.stream().filter(v -> inPeriod(v.getCheckOutTime(), start, end))
                .filter(v -> v.getStatus() == VisitStatus.COMPLETED).toList();
        var activity = new ClinicOverviewResponse.Activity(
                visits.stream().filter(v -> inPeriod(v.getCheckInTime(), start, end)).count(), closed.size(),
                closed.stream().filter(v -> skippedVisits.contains(v.getVisitId())).count(),
                visits.stream().filter(v -> v.getStatus() == VisitStatus.CANCELLED
                        && inPeriod(v.getCheckOutTime(), start, end)).count(), completedRecords.size(), completedTests.size());

        var gross = invoices.stream().map(i -> money(i.getSubtotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
        var discount = invoices.stream().map(i -> money(i.getDiscount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        var insurance = items.stream().map(i -> money(i.getBhytFund())).reduce(BigDecimal.ZERO, BigDecimal::add);
        var care = ledgerRepo.findAll().stream().filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .filter(l -> l.getType() == MembershipLedgerType.PAYMENT)
                .filter(l -> l.getInvoice() != null && invoiceIds.contains(l.getInvoice().getInvoiceId()))
                .filter(l -> l.getPaymentTransaction() != null && l.getPaymentTransaction().getStatus()
                        == TransactionStatus.SUCCESS)
                .map(l -> money(l.getBenefitDiscount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        var finance = new ClinicOverviewResponse.Finance(
                payments.stream().map(t -> money(t.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add), payments.size(),
                gross, insurance, care, discount.subtract(insurance).subtract(care),
                invoices.stream().map(i -> money(i.getTax())).reduce(BigDecimal.ZERO, BigDecimal::add),
                invoices.stream().map(i -> money(i.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add),
                invoices.stream().map(i -> money(i.getPaidAmount())).reduce(BigDecimal.ZERO, BigDecimal::add),
                invoices.stream().map(i -> money(i.getTotalAmount()).subtract(money(i.getPaidAmount())).max(BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        Map<String, BigDecimal> trend = new TreeMap<>(), methods = new TreeMap<>();
        boolean monthly = ChronoUnit.DAYS.between(start, end) > 62;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) trend.put(monthly ? YearMonth.from(d).toString() : d.toString(), BigDecimal.ZERO);
        payments.forEach(t -> {
            String key = monthly ? YearMonth.from(t.getPaidAt()).toString() : t.getPaidAt().toLocalDate().toString();
            trend.merge(key, money(t.getAmount()), BigDecimal::add);
            methods.merge(t.getPaymentMethod() == null ? "OTHER" : t.getPaymentMethod().name(), money(t.getAmount()), BigDecimal::add);
        });
        var rooms = departmentRepo.findAll().stream().sorted(Comparator.comparing(d -> d.getRoomCode())).map(d -> {
            var roomQueues = queues.stream().filter(q -> q.getDepartment() != null && d.getDepartmentId().equals(q.getDepartment().getDepartmentId()))
                    .filter(q -> inPeriod(q.getWorkDate(), start, end)).toList();
            var ratings = records.stream().filter(r -> r.getRatingScore() != null && inPeriod(r.getRatedAt(), start, end))
                    .filter(r -> r.getQueueTicket() != null && r.getQueueTicket().getDepartment() != null
                            && d.getDepartmentId().equals(r.getQueueTicket().getDepartment().getDepartmentId())).toList();
            return new ClinicOverviewResponse.Room(d.getRoomCode(), d.getName(),
                    completedRecords.stream().filter(r -> r.getQueueTicket().getDepartment() != null && d.getDepartmentId().equals(r.getQueueTicket().getDepartment().getDepartmentId())).count(),
                    completedTests.stream().filter(t -> t.getPerformingDepartment() != null && d.getDepartmentId().equals(t.getPerformingDepartment().getDepartmentId())).count(),
                    roomQueues.stream().filter(q -> Set.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.TEST_DONE).contains(q.getStatus())).count(),
                    roomQueues.stream().filter(q -> Set.of(QueueStatus.IN_PROGRESS, QueueStatus.WAITING_FOR_TEST).contains(q.getStatus())).count(),
                    roomQueues.stream().filter(q -> q.getStatus() == QueueStatus.SKIPPED).count(),
                    ratings.isEmpty() ? null : Math.round(ratings.stream().mapToInt(r -> r.getRatingScore()).average().orElse(0)*10)/10.0, ratings.size());
        }).toList();
        // Use invoice snapshots, not today's list price; item finalPrice already covers the entire quantity.
        var groups = items.stream().collect(Collectors.groupingBy(i -> Objects.toString(i.getServiceCodeSnapshot(), "UNKNOWN") + "|" + Objects.toString(i.getServiceSnapshot(), "Dịch vụ"), TreeMap::new, Collectors.toList()));
        var services = groups.values().stream().map(group -> {
            var first = group.get(0);
            return new ClinicOverviewResponse.ServiceLine(first.getServiceCodeSnapshot(), first.getServiceSnapshot(),
                    first.getService() == null || first.getService().getDepartmentType() == null ? "OTHER" : first.getService().getDepartmentType().name(),
                    group.stream().mapToLong(i -> i.getQuantity() == null ? 1 : i.getQuantity()).sum(),
                    group.stream().map(i -> money(i.getUnitPrice()).multiply(BigDecimal.valueOf(i.getQuantity() == null ? 1 : i.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add),
                    group.stream().map(i -> money(i.getBhytFund())).reduce(BigDecimal.ZERO, BigDecimal::add),
                    group.stream().map(i -> money(i.getFinalPrice() == null ? i.getLineTotal() : i.getFinalPrice())).reduce(BigDecimal.ZERO, BigDecimal::add));
        }).toList();
        return new ClinicOverviewResponse(start, end, activity, finance,
                trend.entrySet().stream().map(e -> new ClinicOverviewResponse.AmountPoint(e.getKey(),e.getValue())).toList(),
                methods.entrySet().stream().map(e -> new ClinicOverviewResponse.AmountPoint(e.getKey(),e.getValue())).toList(), rooms, services);
    }

    private static BigDecimal money(BigDecimal amount) { return amount == null ? BigDecimal.ZERO : amount; }
    private static boolean inPeriod(LocalDateTime value, LocalDate from, LocalDate to) { return value != null && inPeriod(value.toLocalDate(), from, to); }
    private static boolean inPeriod(LocalDate value, LocalDate from, LocalDate to) { return value != null && !value.isBefore(from) && !value.isAfter(to); }

    private static final java.time.ZoneId CLINIC_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    private final InvoiceRepository invoiceRepo;
    private final MedicalServiceRepository serviceRepo;
    private final QueueTicketRepository queueTicketRepo;
    private final DepartmentRepository departmentRepo;
    private final InvoiceItemRepository invoiceItemRepo;
    private final MedicalRecordRepository medicalRecordRepo;
    private final TestRequestRepository testRequestRepo;

    public DashboardReportResponse getDashboardReport(String period, LocalDate customFrom, LocalDate customTo) {
        LocalDate now = LocalDate.now(CLINIC_ZONE);
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
        LocalDate now = LocalDate.now(CLINIC_ZONE);
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
                .filter(ii -> ii.getInvoice() != null && ii.getInvoice().getStatus() == InvoiceStatus.PAID)
                .filter(ii -> !ii.getInvoice().getIssueDate().isBefore(from)
                        && !ii.getInvoice().getIssueDate().isAfter(to))
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
        List<QueueTicket> queues = queueTicketRepo.findAll().stream()
                .filter(q -> !q.getWorkDate().isBefore(from) && !q.getWorkDate().isAfter(to))
                .toList();
        List<org.example.doansummer2026.model.InvoiceItem> paidItems = invoiceItemRepo.findAll().stream()
                .filter(item -> item.getInvoice() != null && item.getInvoice().getStatus() == InvoiceStatus.PAID)
                .filter(item -> !item.getInvoice().getIssueDate().isBefore(from)
                        && !item.getInvoice().getIssueDate().isAfter(to))
                .toList();
        List<org.example.doansummer2026.model.MedicalRecord> ratedRecords = medicalRecordRepo.findAll().stream()
                .filter(record -> record.getRatingScore() != null)
                .filter(record -> record.getQueueTicket() != null)
                .filter(record -> record.getCompletedAt() != null)
                .filter(record -> !record.getCompletedAt().toLocalDate().isBefore(from)
                        && !record.getCompletedAt().toLocalDate().isAfter(to))
                .toList();
        List<org.example.doansummer2026.model.TestRequest> testRequests = testRequestRepo.findAll();

        return departmentRepo.findAll().stream()
                .map(d -> {
                    int sessions = (int) queues.stream()
                            .filter(q -> q.getDepartment().getDepartmentId().equals(d.getDepartmentId()))
                            .filter(q -> q.getStatus() == QueueStatus.DONE)
                            .count();
                    long revenue = paidItems.stream()
                            .filter(item -> item.getService() != null && item.getInvoice().getVisit() != null)
                            .filter(item -> testRequests.stream().anyMatch(request ->
                                    request.getInvoiceItem() != null
                                            && request.getInvoiceItem().getItemId().equals(item.getItemId())
                                            && request.getPerformingDepartment().getDepartmentId().equals(d.getDepartmentId()))
                                    || queues.stream().anyMatch(queue ->
                                    queue.getVisit().getVisitId().equals(item.getInvoice().getVisit().getVisitId())
                                            && queue.getDepartment().getDepartmentId().equals(d.getDepartmentId())
                                            && queue.getService() != null
                                            && queue.getService().getServiceId().equals(item.getService().getServiceId())))
                            .mapToLong(this::actualItemAmount)
                            .sum();

                    // Chưa có cấu hình công suất tối đa theo phòng nên không tự tạo phần trăm giả.
                    int occupancy = 0;
                    double csat = ratedRecords.stream()
                            .filter(record -> record.getQueueTicket().getDepartment().getDepartmentId()
                                    .equals(d.getDepartmentId()))
                            .mapToInt(org.example.doansummer2026.model.MedicalRecord::getRatingScore)
                            .average()
                            .orElse(0.0);
                    csat = Math.round(csat * 10.0) / 10.0;

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
                            items.stream().mapToLong(this::actualItemAmount).sum(),
                            bhytQty,
                            bhytFund
                    );
                })
                .toList();
    }

    private long actualItemAmount(org.example.doansummer2026.model.InvoiceItem item) {
        if (item.getFinalPrice() != null) return item.getFinalPrice().longValue();
        return item.getLineTotal() != null ? item.getLineTotal().longValue() : 0L;
    }
}
