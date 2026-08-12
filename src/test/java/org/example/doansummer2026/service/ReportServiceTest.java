package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.InvoiceItem;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.InvoiceItemRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private InvoiceRepository invoiceRepo;

    @Mock
    private MedicalServiceRepository serviceRepo;

    @Mock
    private QueueTicketRepository queueTicketRepo;

    @Mock
    private DepartmentRepository departmentRepo;

    @Mock
    private InvoiceItemRepository invoiceItemRepo;

    @InjectMocks
    private ReportService reportService;


    // =========================================================
// HELPERS
// Dùng lenient vì mỗi test chỉ sử dụng một phần getter
// =========================================================

    private Invoice invoice(
            InvoiceStatus status,
            LocalDate issueDate,
            long total
    ) {
        Invoice invoice = mock(Invoice.class);

        lenient().when(invoice.getStatus())
                .thenReturn(status);

        lenient().when(invoice.getIssueDate())
                .thenReturn(issueDate);

        lenient().when(invoice.getTotalAmount())
                .thenReturn(BigDecimal.valueOf(total));

        return invoice;
    }


    private Department department(
            UUID id,
            String name,
            String roomCode
    ) {
        Department department = mock(Department.class);

        lenient().when(department.getDepartmentId())
                .thenReturn(id);

        lenient().when(department.getName())
                .thenReturn(name);

        lenient().when(department.getRoomCode())
                .thenReturn(roomCode);

        return department;
    }


    private QueueTicket queue(
            QueueStatus status,
            LocalDate updatedDate,
            Department department
    ) {
        QueueTicket queue = mock(QueueTicket.class);

        lenient().when(queue.getStatus())
                .thenReturn(status);

        lenient().when(queue.getUpdatedAt())
                .thenReturn(updatedDate.atTime(10, 0));

        lenient().when(queue.getDepartment())
                .thenReturn(department);

        return queue;
    }


    private MedicalService medicalService(
            UUID id,
            String name,
            String description,
            BigDecimal price,
            DepartmentType departmentType
    ) {
        MedicalService service = mock(MedicalService.class);

        lenient().when(service.getServiceId())
                .thenReturn(id);

        lenient().when(service.getName())
                .thenReturn(name);

        lenient().when(service.getDescription())
                .thenReturn(description);

        lenient().when(service.getPrice())
                .thenReturn(price);

        lenient().when(service.getDepartmentType())
                .thenReturn(departmentType);

        return service;
    }


    // =========================================================
    // DASHBOARD - CUSTOM RANGE
    // =========================================================

    @Test
    void getDashboardReport_ShouldUseCustomDateRange() {

        LocalDate from =
                LocalDate.of(2026, 1, 1);

        LocalDate to =
                LocalDate.of(2026, 1, 31);

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(departmentRepo.findAll())
                .thenReturn(List.of());

        var result =
                reportService.getDashboardReport(
                        "month",
                        from,
                        to
                );

        assertNotNull(result);

        verify(invoiceRepo)
                .findAll();

        verify(departmentRepo)
                .findAll();
    }


    // =========================================================
    // DASHBOARD - DAY
    // =========================================================

    @Test
    void getDashboardReport_ShouldHandleDayPeriod() {

        LocalDate today =
                LocalDate.now();

        Department department =
                department(
                        UUID.randomUUID(),
                        "Noi",
                        "P101"
                );

        QueueTicket doneToday =
                queue(
                        QueueStatus.DONE,
                        today,
                        department
                );

        QueueTicket waiting =
                queue(
                        QueueStatus.WAITING,
                        today,
                        department
                );

        Invoice paidToday =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        500000
                );

        Invoice pendingToday =
                invoice(
                        InvoiceStatus.PENDING,
                        today,
                        999999
                );

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(
                                paidToday,
                                pendingToday
                        )
                );

        when(queueTicketRepo.findAll())
                .thenReturn(
                        List.of(
                                doneToday,
                                waiting
                        )
                );

        when(departmentRepo.findAll())
                .thenReturn(
                        List.of(department)
                );

        var result =
                reportService.getDashboardReport(
                        "day",
                        null,
                        null
                );

        assertNotNull(result);
    }


    // =========================================================
    // DASHBOARD - QUARTER
    // =========================================================

    @Test
    void getDashboardReport_ShouldHandleQuarterPeriod() {

        LocalDate now =
                LocalDate.now();

        int q =
                (now.getMonthValue() - 1) / 3 + 1;

        LocalDate quarterStart =
                LocalDate.of(
                        now.getYear(),
                        (q - 1) * 3 + 1,
                        1
                );

        Invoice firstMonth =
                invoice(
                        InvoiceStatus.PAID,
                        quarterStart,
                        100000
                );

        Invoice secondMonth =
                invoice(
                        InvoiceStatus.PAID,
                        quarterStart.plusMonths(1),
                        200000
                );

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(
                                firstMonth,
                                secondMonth
                        )
                );

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(departmentRepo.findAll())
                .thenReturn(List.of());

        var result =
                reportService.getDashboardReport(
                        "quarter",
                        null,
                        null
                );

        assertNotNull(result);
    }


    // =========================================================
    // DASHBOARD - YEAR
    // =========================================================

    @Test
    void getDashboardReport_ShouldHandleYearPeriod() {

        LocalDate now =
                LocalDate.now();

        Invoice january =
                invoice(
                        InvoiceStatus.PAID,
                        LocalDate.of(
                                now.getYear(),
                                1,
                                10
                        ),
                        100000
                );

        Invoice june =
                invoice(
                        InvoiceStatus.PAID,
                        LocalDate.of(
                                now.getYear(),
                                6,
                                10
                        ),
                        200000
                );

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(
                                january,
                                june
                        )
                );

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(departmentRepo.findAll())
                .thenReturn(List.of());

        var result =
                reportService.getDashboardReport(
                        "year",
                        null,
                        null
                );

        assertNotNull(result);
    }


    // =========================================================
    // DASHBOARD - DEFAULT MONTH
    // =========================================================

    @Test
    void getDashboardReport_ShouldUseMonthAsDefaultPeriod() {

        LocalDate today =
                LocalDate.now();

        Invoice invoice =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        100000
                );

        when(invoiceRepo.findAll())
                .thenReturn(List.of(invoice));

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(departmentRepo.findAll())
                .thenReturn(List.of());

        var result =
                reportService.getDashboardReport(
                        "anything",
                        null,
                        null
                );

        assertNotNull(result);
    }


    // =========================================================
    // DASHBOARD - FILTER OUTSIDE DATE
    // =========================================================

    @Test
    void getDashboardReport_ShouldIgnoreDataOutsideRequestedRange() {

        LocalDate from =
                LocalDate.of(2026, 1, 1);

        LocalDate to =
                LocalDate.of(2026, 1, 31);

        Department department =
                department(
                        UUID.randomUUID(),
                        "Ngoai",
                        "P102"
                );

        Invoice inside =
                invoice(
                        InvoiceStatus.PAID,
                        LocalDate.of(2026, 1, 15),
                        100000
                );

        Invoice before =
                invoice(
                        InvoiceStatus.PAID,
                        LocalDate.of(2025, 12, 31),
                        900000
                );

        Invoice after =
                invoice(
                        InvoiceStatus.PAID,
                        LocalDate.of(2026, 2, 1),
                        900000
                );

        QueueTicket queueInside =
                queue(
                        QueueStatus.DONE,
                        LocalDate.of(2026, 1, 15),
                        department
                );

        QueueTicket queueOutside =
                queue(
                        QueueStatus.DONE,
                        LocalDate.of(2026, 2, 1),
                        department
                );

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(
                                inside,
                                before,
                                after
                        )
                );

        when(queueTicketRepo.findAll())
                .thenReturn(
                        List.of(
                                queueInside,
                                queueOutside
                        )
                );

        when(departmentRepo.findAll())
                .thenReturn(
                        List.of(department)
                );

        assertNotNull(
                reportService.getDashboardReport(
                        "month",
                        from,
                        to
                )
        );
    }


    // =========================================================
    // DASHBOARD - DEPARTMENT WITHOUT SESSION
    // sessions == 0
    // =========================================================

    @Test
    void getDashboardReport_ShouldHandleDepartmentWithoutSessions() {

        Department department =
                department(
                        UUID.randomUUID(),
                        "Da lieu",
                        "P103"
                );

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(departmentRepo.findAll())
                .thenReturn(
                        List.of(department)
                );

        var result =
                reportService.getDashboardReport(
                        "day",
                        null,
                        null
                );

        assertNotNull(result);
    }


    // =========================================================
    // DASHBOARD - DEPARTMENT WITH SESSION
    // sessions > 0 -> occupancy + csat branches
    // =========================================================

    @Test
    void getDashboardReport_ShouldCalculateDepartmentStats_WhenSessionsExist() {

        LocalDate today =
                LocalDate.now();

        UUID departmentId =
                UUID.randomUUID();

        Department department =
                department(
                        departmentId,
                        "Noi tong quat",
                        "N01"
                );

        QueueTicket q1 =
                queue(
                        QueueStatus.DONE,
                        today,
                        department
                );

        QueueTicket q2 =
                queue(
                        QueueStatus.DONE,
                        today,
                        department
                );

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(
                        List.of(q1, q2)
                );

        when(departmentRepo.findAll())
                .thenReturn(
                        List.of(department)
                );

        var result =
                reportService.getDashboardReport(
                        "day",
                        null,
                        null
                );

        assertNotNull(result);
    }


    // =========================================================
    // DASHBOARD - OCCUPANCY CAPPED AT 100
    // Math.min(100,...)
    // =========================================================

    @Test
    void getDashboardReport_ShouldCapDepartmentOccupancyAt100() {

        LocalDate today =
                LocalDate.now();

        UUID departmentId =
                UUID.randomUUID();

        Department department =
                department(
                        departmentId,
                        "Sieu dong",
                        "FULL"
                );

        List<QueueTicket> queues =
                new ArrayList<>();

        /*
         * 13 sessions:
         * 40 + 13*5 = 105
         * -> Math.min(...) = 100
         */
        for (int i = 0; i < 13; i++) {
            queues.add(
                    queue(
                            QueueStatus.DONE,
                            today,
                            department
                    )
            );
        }

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(queues);

        when(departmentRepo.findAll())
                .thenReturn(
                        List.of(department)
                );

        assertNotNull(
                reportService.getDashboardReport(
                        "day",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // SERVICE REPORT - CUSTOM RANGE + EMPTY DATA
    // totalRevenue == 0
    // totalSessions == 0
    // calcTotalRevenue == 0
    // bhytTotal == 0
    // =========================================================

    @Test
    void getServiceReport_ShouldHandleEmptyData() {

        LocalDate from =
                LocalDate.of(2026, 1, 1);

        LocalDate to =
                LocalDate.of(2026, 1, 31);

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(List.of());

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        var result =
                reportService.getServiceReport(
                        "month",
                        from,
                        to
                );

        assertNotNull(result);
    }


    // =========================================================
    // SERVICE REPORT - DAY
    // =========================================================

    @Test
    void getServiceReport_ShouldHandleDayPeriod() {

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(List.of());

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        assertNotNull(
                reportService.getServiceReport(
                        "day",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // SERVICE REPORT - QUARTER
    // =========================================================

    @Test
    void getServiceReport_ShouldHandleQuarterPeriod() {

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(List.of());

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        assertNotNull(
                reportService.getServiceReport(
                        "quarter",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // SERVICE REPORT - YEAR
    // =========================================================

    @Test
    void getServiceReport_ShouldHandleYearPeriod() {

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(List.of());

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        assertNotNull(
                reportService.getServiceReport(
                        "year",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // SERVICE REPORT - DEFAULT MONTH
    // =========================================================

    @Test
    void getServiceReport_ShouldUseDefaultMonth() {

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(List.of());

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        assertNotNull(
                reportService.getServiceReport(
                        "unknown",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // SERVICE REPORT - REVENUE / SESSION / BHYT
    // =========================================================

    @Test
    void getServiceReport_ShouldCalculateRevenueSessionsAndBhyt() {

        LocalDate today =
                LocalDate.now();

        Department department =
                department(
                        UUID.randomUUID(),
                        "Xet nghiem",
                        "LAB"
                );

        Invoice paid =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        1000000
                );

        Invoice pending =
                invoice(
                        InvoiceStatus.PENDING,
                        today,
                        5000000
                );

        QueueTicket done =
                queue(
                        QueueStatus.DONE,
                        today,
                        department
                );

        QueueTicket waiting =
                queue(
                        QueueStatus.WAITING,
                        today,
                        department
                );

        InvoiceItem bhyt =
                mock(InvoiceItem.class);

        when(bhyt.getBhytFund())
                .thenReturn(
                        new BigDecimal("200000")
                );

        InvoiceItem noBhyt =
                mock(InvoiceItem.class);

        when(noBhyt.getBhytFund())
                .thenReturn(null);

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(
                                paid,
                                pending
                        )
                );

        when(queueTicketRepo.findAll())
                .thenReturn(
                        List.of(
                                done,
                                waiting
                        )
                );

        when(serviceRepo.findAll())
                .thenReturn(List.of());

        when(invoiceItemRepo.findAll())
                .thenReturn(
                        List.of(
                                bhyt,
                                noBhyt
                        )
                );

        var result =
                reportService.getServiceReport(
                        "day",
                        null,
                        null
                );

        assertNotNull(result);
    }


    // =========================================================
    // SERVICE STATS - PAID ITEMS
    // =========================================================

    @Test
    void getServiceReport_ShouldBuildServiceStatsFromPaidInvoiceItems() {

        LocalDate today =
                LocalDate.now();

        UUID serviceId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Xet nghiem mau",
                        "Cong thuc mau",
                        new BigDecimal("200000"),
                        DepartmentType.EXAMINATION
                );

        Invoice paidInvoice =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        200000
                );

        InvoiceItem item =
                mock(InvoiceItem.class);

        when(item.getService())
                .thenReturn(service);

        when(item.getInvoice())
                .thenReturn(paidInvoice);

        when(item.getBhytFund())
                .thenReturn(
                        new BigDecimal("100000")
                );

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(paidInvoice)
                );

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(
                        List.of(service)
                );

        when(invoiceItemRepo.findAll())
                .thenReturn(
                        List.of(item)
                );

        var result =
                reportService.getServiceReport(
                        "day",
                        null,
                        null
                );

        assertNotNull(result);
    }


    // =========================================================
    // SERVICE STATS - NULL DEPARTMENT TYPE / NULL DESCRIPTION
    // =========================================================

    @Test
    void getServiceReport_ShouldUseFallbackCategoryAndDescription() {

        UUID serviceId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Dich vu dac biet",
                        null,
                        new BigDecimal("100000"),
                        null
                );

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(
                        List.of(service)
                );

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        var result =
                reportService.getServiceReport(
                        "day",
                        null,
                        null
                );

        assertNotNull(result);
    }


    // =========================================================
    // SERVICE STATS - FILTER WRONG SERVICE
    // =========================================================

    @Test
    void getServiceReport_ShouldIgnoreInvoiceItemBelongingToAnotherService() {

        LocalDate today =
                LocalDate.now();

        MedicalService wanted =
                medicalService(
                        UUID.randomUUID(),
                        "Service A",
                        "A",
                        BigDecimal.valueOf(100000),
                        DepartmentType.EXAMINATION
                );

        MedicalService other =
                medicalService(
                        UUID.randomUUID(),
                        "Service B",
                        "B",
                        BigDecimal.valueOf(100000),
                        DepartmentType.EXAMINATION
                );

        Invoice invoice =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        100000
                );

        InvoiceItem item =
                mock(InvoiceItem.class);

        when(item.getService())
                .thenReturn(other);

        when(invoiceRepo.findAll())
                .thenReturn(List.of(invoice));

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(List.of(wanted));

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of(item));

        assertNotNull(
                reportService.getServiceReport(
                        "day",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // SERVICE STATS - IGNORE UNPAID ITEM
    // =========================================================

    @Test
    void getServiceReport_ShouldIgnoreInvoiceItem_WhenInvoiceNotPaid() {

        LocalDate today =
                LocalDate.now();

        UUID serviceId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Service A",
                        "Description",
                        BigDecimal.valueOf(100000),
                        DepartmentType.EXAMINATION
                );

        Invoice pending =
                invoice(
                        InvoiceStatus.PENDING,
                        today,
                        100000
                );

        InvoiceItem item =
                mock(InvoiceItem.class);

        when(item.getService())
                .thenReturn(service);

        when(item.getInvoice())
                .thenReturn(pending);

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(List.of(service));

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of(item));

        assertNotNull(
                reportService.getServiceReport(
                        "day",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // SERVICE STATS - IGNORE ITEM BEFORE RANGE
    // =========================================================

    @Test
    void getServiceReport_ShouldIgnoreInvoiceItemBeforeDateRange() {

        UUID serviceId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Service",
                        "Description",
                        BigDecimal.valueOf(100000),
                        DepartmentType.EXAMINATION
                );

        Invoice invoice =
                invoice(
                        InvoiceStatus.PAID,
                        LocalDate.of(2025, 12, 31),
                        100000
                );

        InvoiceItem item =
                mock(InvoiceItem.class);

        when(item.getService())
                .thenReturn(service);

        when(item.getInvoice())
                .thenReturn(invoice);

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(List.of(service));

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of(item));

        assertNotNull(
                reportService.getServiceReport(
                        "month",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)
                )
        );
    }


    // =========================================================
    // SERVICE BREAKDOWN - MULTIPLE CATEGORIES
    // Covers grouping + map + sorted comparator
    // =========================================================

    @Test
    void getServiceReport_ShouldCreateAndSortCategoryBreakdown() {

        LocalDate today =
                LocalDate.now();

        MedicalService serviceA =
                medicalService(
                        UUID.randomUUID(),
                        "Service A",
                        "Description A",
                        BigDecimal.valueOf(300000),
                        DepartmentType.EXAMINATION
                );

        MedicalService serviceB =
                medicalService(
                        UUID.randomUUID(),
                        "Service B",
                        "Description B",
                        BigDecimal.valueOf(100000),
                        null
                );

        Invoice paidA =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        300000
                );

        Invoice paidB =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        100000
                );

        InvoiceItem itemA =
                mock(InvoiceItem.class);

        when(itemA.getService())
                .thenReturn(serviceA);

        when(itemA.getInvoice())
                .thenReturn(paidA);

        when(itemA.getBhytFund())
                .thenReturn(BigDecimal.ZERO);

        InvoiceItem itemB =
                mock(InvoiceItem.class);

        when(itemB.getService())
                .thenReturn(serviceB);

        when(itemB.getInvoice())
                .thenReturn(paidB);

        when(itemB.getBhytFund())
                .thenReturn(null);

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(
                                paidA,
                                paidB
                        )
                );

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(
                        List.of(
                                serviceA,
                                serviceB
                        )
                );

        when(invoiceItemRepo.findAll())
                .thenReturn(
                        List.of(
                                itemA,
                                itemB
                        )
                );

        assertNotNull(
                reportService.getServiceReport(
                        "day",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // REVENUE CHART - MULTIPLE DAYS
    // Covers grouping + sorting by label
    // =========================================================

    @Test
    void getDashboardReport_ShouldGroupRevenueByDayAndSortLabels() {

        LocalDate today =
                LocalDate.now();

        Invoice later =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        200000
                );

        Invoice earlier =
                invoice(
                        InvoiceStatus.PAID,
                        today.minusDays(1),
                        100000
                );

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(
                                later,
                                earlier
                        )
                );

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(departmentRepo.findAll())
                .thenReturn(List.of());

        assertNotNull(
                reportService.getDashboardReport(
                        "month",
                        today.minusDays(5),
                        today
                )
        );
    }


    // =========================================================
    // SESSION CHART - MULTIPLE DEPARTMENTS
    // Covers grouping by department name
    // =========================================================

    @Test
    void getDashboardReport_ShouldGroupSessionsByDepartment() {

        LocalDate today =
                LocalDate.now();

        Department deptA =
                department(
                        UUID.randomUUID(),
                        "Noi",
                        "N01"
                );

        Department deptB =
                department(
                        UUID.randomUUID(),
                        "Ngoai",
                        "N02"
                );

        QueueTicket a1 =
                queue(
                        QueueStatus.DONE,
                        today,
                        deptA
                );

        QueueTicket a2 =
                queue(
                        QueueStatus.DONE,
                        today,
                        deptA
                );

        QueueTicket b1 =
                queue(
                        QueueStatus.DONE,
                        today,
                        deptB
                );

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(
                        List.of(
                                a1,
                                a2,
                                b1
                        )
                );

        when(departmentRepo.findAll())
                .thenReturn(
                        List.of(
                                deptA,
                                deptB
                        )
                );

        assertNotNull(
                reportService.getDashboardReport(
                        "day",
                        null,
                        null
                )
        );
    }
}