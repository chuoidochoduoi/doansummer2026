package org.example.doansummer2026.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Read-only report. Payment dates and invoice issue dates are intentionally separate scopes. */
public record ClinicOverviewResponse(LocalDate fromDate, LocalDate toDate, Activity activity,
        Finance finance, List<AmountPoint> paymentChart, List<AmountPoint> paymentMethods,
        List<Room> rooms, List<ServiceLine> services) {
    public record Activity(long arrivals, long closedVisits, long partialVisits, long cancelledVisits,
                           long completedExaminations, long completedTests) {}
    public record Finance(BigDecimal collected, long successfulPayments, BigDecimal invoiceGross,
            BigDecimal insurance, BigDecimal caresBenefit, BigDecimal otherDiscount, BigDecimal tax,
            BigDecimal invoicePayable, BigDecimal invoicePaid, BigDecimal outstanding) {}
    public record AmountPoint(String label, BigDecimal amount) {}
    public record Room(String code, String name, long completedExaminations, long completedTests,
                       long waiting, long inProgress, long skipped, Double rating, long ratingCount) {}
    public record ServiceLine(String code, String name, String category, long quantity,
                              BigDecimal gross, BigDecimal insurance, BigDecimal patientAmount) {}
}
