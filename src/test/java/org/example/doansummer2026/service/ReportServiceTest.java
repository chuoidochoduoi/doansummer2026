package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.InvoiceItem;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.InvoiceItemRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Mock
    private MedicalRecordRepository medicalRecordRepo;

    @Mock
    private TestRequestRepository testRequestRepo;

    @InjectMocks
    private ReportService reportService;


    // =========================================================
    // HELPERS
    // =========================================================

    private Invoice invoice(
            InvoiceStatus status,
            LocalDate issueDate,
            long total
    ) {
        Invoice invoice = mock(Invoice.class);

        lenient()
                .when(invoice.getStatus())
                .thenReturn(status);

        lenient()
                .when(invoice.getIssueDate())
                .thenReturn(issueDate);

        lenient()
                .when(invoice.getTotalAmount())
                .thenReturn(BigDecimal.valueOf(total));

        return invoice;
    }


    private Department department(
            UUID id,
            String name,
            String roomCode
    ) {
        Department department = mock(Department.class);

        lenient()
                .when(department.getDepartmentId())
                .thenReturn(id);

        lenient()
                .when(department.getName())
                .thenReturn(name);

        lenient()
                .when(department.getRoomCode())
                .thenReturn(roomCode);

        return department;
    }


    private QueueTicket queue(
            QueueStatus status,
            LocalDate date,
            Department department
    ) {
        QueueTicket queue = mock(QueueTicket.class);

        lenient()
                .when(queue.getStatus())
                .thenReturn(status);

        lenient()
                .when(queue.getUpdatedAt())
                .thenReturn(
                        date.atTime(10, 0)
                );

        lenient()
                .when(queue.getWorkDate())
                .thenReturn(date);

        lenient()
                .when(queue.getDepartment())
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
        MedicalService service =
                mock(MedicalService.class);

        lenient()
                .when(service.getServiceId())
                .thenReturn(id);

        lenient()
                .when(service.getName())
                .thenReturn(name);

        lenient()
                .when(service.getDescription())
                .thenReturn(description);

        lenient()
                .when(service.getPrice())
                .thenReturn(price);

        lenient()
                .when(service.getDepartmentType())
                .thenReturn(departmentType);

        return service;
    }


    private void mockEmptyDashboardDependencies() {

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(departmentRepo.findAll())
                .thenReturn(List.of());

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        when(medicalRecordRepo.findAll())
                .thenReturn(List.of());

        when(testRequestRepo.findAll())
                .thenReturn(List.of());
    }


    private void mockEmptyServiceReportDependencies() {

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(List.of());

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());
    }


    // =========================================================
    // DASHBOARD - CUSTOM RANGE
    // =========================================================

    @Test
    void getDashboardReport_ShouldUseCustomDateRange() {

        LocalDate from =
                LocalDate.of(
                        2026,
                        1,
                        1
                );

        LocalDate to =
                LocalDate.of(
                        2026,
                        1,
                        31
                );

        mockEmptyDashboardDependencies();

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

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        when(medicalRecordRepo.findAll())
                .thenReturn(List.of());

        when(testRequestRepo.findAll())
                .thenReturn(List.of());

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

        int quarter =
                (now.getMonthValue() - 1) / 3 + 1;

        LocalDate quarterStart =
                LocalDate.of(
                        now.getYear(),
                        (quarter - 1) * 3 + 1,
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

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        when(medicalRecordRepo.findAll())
                .thenReturn(List.of());

        when(testRequestRepo.findAll())
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

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        when(medicalRecordRepo.findAll())
                .thenReturn(List.of());

        when(testRequestRepo.findAll())
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
                .thenReturn(
                        List.of(invoice)
                );

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(departmentRepo.findAll())
                .thenReturn(List.of());

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        when(medicalRecordRepo.findAll())
                .thenReturn(List.of());

        when(testRequestRepo.findAll())
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
    // DASHBOARD - RANGE FILTER
    // =========================================================

    @Test
    void getDashboardReport_ShouldIgnoreDataOutsideRequestedRange() {

        LocalDate from =
                LocalDate.of(
                        2026,
                        1,
                        1
                );

        LocalDate to =
                LocalDate.of(
                        2026,
                        1,
                        31
                );

        Department department =
                department(
                        UUID.randomUUID(),
                        "Ngoai",
                        "P102"
                );

        Invoice inside =
                invoice(
                        InvoiceStatus.PAID,
                        LocalDate.of(
                                2026,
                                1,
                                15
                        ),
                        100000
                );

        Invoice before =
                invoice(
                        InvoiceStatus.PAID,
                        LocalDate.of(
                                2025,
                                12,
                                31
                        ),
                        900000
                );

        Invoice after =
                invoice(
                        InvoiceStatus.PAID,
                        LocalDate.of(
                                2026,
                                2,
                                1
                        ),
                        900000
                );

        QueueTicket queueInside =
                queue(
                        QueueStatus.DONE,
                        LocalDate.of(
                                2026,
                                1,
                                15
                        ),
                        department
                );

        QueueTicket queueOutside =
                queue(
                        QueueStatus.DONE,
                        LocalDate.of(
                                2026,
                                2,
                                1
                        ),
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

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        when(medicalRecordRepo.findAll())
                .thenReturn(List.of());

        when(testRequestRepo.findAll())
                .thenReturn(List.of());

        var result =
                reportService.getDashboardReport(
                        "month",
                        from,
                        to
                );

        assertNotNull(result);
    }


    // =========================================================
    // DASHBOARD - DEPARTMENT WITHOUT SESSION
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

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        when(medicalRecordRepo.findAll())
                .thenReturn(List.of());

        when(testRequestRepo.findAll())
                .thenReturn(List.of());

        var result =
                reportService.getDashboardReport(
                        "day",
                        null,
                        null
                );

        assertNotNull(result);
    }


    // =========================================================
    // DASHBOARD - DEPARTMENT SESSION
    // =========================================================

    @Test
    void getDashboardReport_ShouldCalculateDepartmentSessions() {

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
                        List.of(
                                q1,
                                q2
                        )
                );

        when(departmentRepo.findAll())
                .thenReturn(
                        List.of(department)
                );

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        when(medicalRecordRepo.findAll())
                .thenReturn(List.of());

        when(testRequestRepo.findAll())
                .thenReturn(List.of());

        var result =
                reportService.getDashboardReport(
                        "day",
                        null,
                        null
                );

        assertNotNull(result);
    }


    // =========================================================
    // DASHBOARD - CSAT
    // =========================================================

    @Test
    void getDashboardReport_ShouldCalculateDepartmentCsat() {

        LocalDate today =
                LocalDate.now();

        UUID departmentId =
                UUID.randomUUID();

        Department department =
                department(
                        departmentId,
                        "Noi",
                        "N01"
                );

        QueueTicket queue =
                queue(
                        QueueStatus.DONE,
                        today,
                        department
                );

        MedicalRecord first =
                mock(MedicalRecord.class);

        when(first.getRatingScore())
                .thenReturn(4);

        when(first.getQueueTicket())
                .thenReturn(queue);

        when(first.getCompletedAt())
                .thenReturn(
                        today.atTime(
                                10,
                                0
                        )
                );

        MedicalRecord second =
                mock(MedicalRecord.class);

        when(second.getRatingScore())
                .thenReturn(5);

        when(second.getQueueTicket())
                .thenReturn(queue);

        when(second.getCompletedAt())
                .thenReturn(
                        today.atTime(
                                11,
                                0
                        )
                );

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(
                        List.of(queue)
                );

        when(departmentRepo.findAll())
                .thenReturn(
                        List.of(department)
                );

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        when(medicalRecordRepo.findAll())
                .thenReturn(
                        List.of(
                                first,
                                second
                        )
                );

        when(testRequestRepo.findAll())
                .thenReturn(List.of());

        var result =
                reportService.getDashboardReport(
                        "day",
                        null,
                        null
                );

        assertNotNull(result);
    }


    // =========================================================
    // DASHBOARD - IGNORE UNRATED RECORD
    // =========================================================

    @Test
    void getDashboardReport_ShouldIgnoreMedicalRecordWithoutRating() {

        LocalDate today =
                LocalDate.now();

        Department department =
                department(
                        UUID.randomUUID(),
                        "Noi",
                        "N01"
                );

        MedicalRecord record =
                mock(MedicalRecord.class);

        when(record.getRatingScore())
                .thenReturn(null);

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(departmentRepo.findAll())
                .thenReturn(
                        List.of(department)
                );

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        when(medicalRecordRepo.findAll())
                .thenReturn(
                        List.of(record)
                );

        when(testRequestRepo.findAll())
                .thenReturn(List.of());

        assertNotNull(
                reportService.getDashboardReport(
                        "day",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // DASHBOARD - REVENUE CHART
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

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        when(medicalRecordRepo.findAll())
                .thenReturn(List.of());

        when(testRequestRepo.findAll())
                .thenReturn(List.of());

        var result =
                reportService.getDashboardReport(
                        "month",
                        today.minusDays(5),
                        today
                );

        assertNotNull(result);
    }


    // =========================================================
    // DASHBOARD - SESSION CHART
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

        when(invoiceItemRepo.findAll())
                .thenReturn(List.of());

        when(medicalRecordRepo.findAll())
                .thenReturn(List.of());

        when(testRequestRepo.findAll())
                .thenReturn(List.of());

        assertNotNull(
                reportService.getDashboardReport(
                        "day",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // SERVICE REPORT - EMPTY
    // =========================================================

    @Test
    void getServiceReport_ShouldHandleEmptyData() {

        LocalDate from =
                LocalDate.of(
                        2026,
                        1,
                        1
                );

        LocalDate to =
                LocalDate.of(
                        2026,
                        1,
                        31
                );

        mockEmptyServiceReportDependencies();

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

        mockEmptyServiceReportDependencies();

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

        mockEmptyServiceReportDependencies();

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

        mockEmptyServiceReportDependencies();

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

        mockEmptyServiceReportDependencies();

        assertNotNull(
                reportService.getServiceReport(
                        "unknown",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // SERVICE REPORT - TOTAL REVENUE / SESSION / BHYT
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

        when(bhyt.getInvoice())
                .thenReturn(paid);

        when(bhyt.getBhytFund())
                .thenReturn(
                        new BigDecimal(
                                "200000"
                        )
                );

        InvoiceItem noBhyt =
                mock(InvoiceItem.class);

        when(noBhyt.getInvoice())
                .thenReturn(paid);

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
    // SERVICE REPORT - SERVICE STAT / FINAL PRICE
    // =========================================================

    @Test
    void getServiceReport_ShouldUseFinalPriceForServiceRevenue() {

        LocalDate today =
                LocalDate.now();

        UUID serviceId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Xet nghiem mau",
                        "Cong thuc mau",
                        new BigDecimal(
                                "200000"
                        ),
                        DepartmentType.EXAMINATION
                );

        Invoice paid =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        150000
                );

        InvoiceItem item =
                mock(InvoiceItem.class);

        when(item.getService())
                .thenReturn(service);

        when(item.getInvoice())
                .thenReturn(paid);

        when(item.getFinalPrice())
                .thenReturn(
                        new BigDecimal(
                                "150000"
                        )
                );

        when(item.getBhytFund())
                .thenReturn(
                        new BigDecimal(
                                "50000"
                        )
                );

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(paid)
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
    // SERVICE REPORT - LINE TOTAL FALLBACK
    // =========================================================

    @Test
    void getServiceReport_ShouldUseLineTotal_WhenFinalPriceNull() {

        LocalDate today =
                LocalDate.now();

        UUID serviceId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Dich vu",
                        "Mo ta",
                        BigDecimal.valueOf(
                                100000
                        ),
                        DepartmentType.EXAMINATION
                );

        Invoice paid =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        100000
                );

        InvoiceItem item =
                mock(InvoiceItem.class);

        when(item.getService())
                .thenReturn(service);

        when(item.getInvoice())
                .thenReturn(paid);

        when(item.getFinalPrice())
                .thenReturn(null);

        when(item.getLineTotal())
                .thenReturn(
                        BigDecimal.valueOf(
                                100000
                        )
                );

        when(item.getBhytFund())
                .thenReturn(null);

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(paid)
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
    // SERVICE REPORT - ZERO AMOUNT FALLBACK
    // =========================================================

    @Test
    void getServiceReport_ShouldUseZero_WhenFinalPriceAndLineTotalNull() {

        LocalDate today =
                LocalDate.now();

        UUID serviceId =
                UUID.randomUUID();

        MedicalService service =
                medicalService(
                        serviceId,
                        "Dich vu",
                        "Mo ta",
                        BigDecimal.valueOf(
                                100000
                        ),
                        DepartmentType.EXAMINATION
                );

        Invoice paid =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        0
                );

        InvoiceItem item =
                mock(InvoiceItem.class);

        when(item.getService())
                .thenReturn(service);

        when(item.getInvoice())
                .thenReturn(paid);

        when(item.getFinalPrice())
                .thenReturn(null);

        when(item.getLineTotal())
                .thenReturn(null);

        when(item.getBhytFund())
                .thenReturn(null);

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(paid)
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

        assertNotNull(
                reportService.getServiceReport(
                        "day",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // SERVICE REPORT - FALLBACK CATEGORY / DESCRIPTION
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
                        new BigDecimal(
                                "100000"
                        ),
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
    // SERVICE REPORT - WRONG SERVICE
    // =========================================================

    @Test
    void getServiceReport_ShouldIgnoreInvoiceItemBelongingToAnotherService() {

        MedicalService wanted =
                medicalService(
                        UUID.randomUUID(),
                        "Service A",
                        "A",
                        BigDecimal.valueOf(
                                100000
                        ),
                        DepartmentType.EXAMINATION
                );

        MedicalService other =
                medicalService(
                        UUID.randomUUID(),
                        "Service B",
                        "B",
                        BigDecimal.valueOf(
                                100000
                        ),
                        DepartmentType.EXAMINATION
                );

        InvoiceItem item =
                mock(InvoiceItem.class);

        when(item.getService())
                .thenReturn(other);

        when(invoiceRepo.findAll())
                .thenReturn(List.of());

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(serviceRepo.findAll())
                .thenReturn(
                        List.of(wanted)
                );

        when(invoiceItemRepo.findAll())
                .thenReturn(
                        List.of(item)
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
    // SERVICE REPORT - UNPAID ITEM
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
                        BigDecimal.valueOf(
                                100000
                        ),
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
                .thenReturn(
                        List.of(service)
                );

        when(invoiceItemRepo.findAll())
                .thenReturn(
                        List.of(item)
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
    // SERVICE REPORT - OUTSIDE RANGE
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
                        BigDecimal.valueOf(
                                100000
                        ),
                        DepartmentType.EXAMINATION
                );

        Invoice invoice =
                invoice(
                        InvoiceStatus.PAID,
                        LocalDate.of(
                                2025,
                                12,
                                31
                        ),
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
                .thenReturn(
                        List.of(service)
                );

        when(invoiceItemRepo.findAll())
                .thenReturn(
                        List.of(item)
                );

        assertNotNull(
                reportService.getServiceReport(
                        "month",
                        LocalDate.of(
                                2026,
                                1,
                                1
                        ),
                        LocalDate.of(
                                2026,
                                1,
                                31
                        )
                )
        );
    }


    // =========================================================
    // SERVICE REPORT - CATEGORY BREAKDOWN
    // =========================================================

    @Test
    void getServiceReport_ShouldCreateCategoryBreakdown() {

        LocalDate today =
                LocalDate.now();

        MedicalService serviceA =
                medicalService(
                        UUID.randomUUID(),
                        "Service A",
                        "Description A",
                        BigDecimal.valueOf(
                                300000
                        ),
                        DepartmentType.EXAMINATION
                );

        MedicalService serviceB =
                medicalService(
                        UUID.randomUUID(),
                        "Service B",
                        "Description B",
                        BigDecimal.valueOf(
                                100000
                        ),
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

        when(itemA.getFinalPrice())
                .thenReturn(
                        BigDecimal.valueOf(
                                300000
                        )
                );

        when(itemA.getBhytFund())
                .thenReturn(BigDecimal.ZERO);

        InvoiceItem itemB =
                mock(InvoiceItem.class);

        when(itemB.getService())
                .thenReturn(serviceB);

        when(itemB.getInvoice())
                .thenReturn(paidB);

        when(itemB.getFinalPrice())
                .thenReturn(
                        BigDecimal.valueOf(
                                100000
                        )
                );

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
    // DEPARTMENT REVENUE - QUEUE MATCH
    // =========================================================

    @Test
    void getDashboardReport_ShouldCalculateDepartmentRevenueFromQueue() {

        LocalDate today =
                LocalDate.now();

        UUID departmentId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        Department department =
                department(
                        departmentId,
                        "Kham noi",
                        "P01"
                );

        MedicalService service =
                medicalService(
                        serviceId,
                        "Kham noi",
                        "Mo ta",
                        BigDecimal.valueOf(
                                200000
                        ),
                        DepartmentType.EXAMINATION
                );

        CustomerVisit visit =
                mock(CustomerVisit.class);

        when(visit.getVisitId())
                .thenReturn(visitId);

        QueueTicket queue =
                queue(
                        QueueStatus.DONE,
                        today,
                        department
                );

        when(queue.getVisit())
                .thenReturn(visit);

        when(queue.getService())
                .thenReturn(service);

        Invoice invoice =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        200000
                );

        when(invoice.getVisit())
                .thenReturn(visit);

        InvoiceItem item =
                mock(InvoiceItem.class);

        when(item.getInvoice())
                .thenReturn(invoice);

        when(item.getService())
                .thenReturn(service);

        when(item.getFinalPrice())
                .thenReturn(
                        BigDecimal.valueOf(
                                200000
                        )
                );

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(invoice)
                );

        when(queueTicketRepo.findAll())
                .thenReturn(
                        List.of(queue)
                );

        when(departmentRepo.findAll())
                .thenReturn(
                        List.of(department)
                );

        when(invoiceItemRepo.findAll())
                .thenReturn(
                        List.of(item)
                );

        when(medicalRecordRepo.findAll())
                .thenReturn(List.of());

        when(testRequestRepo.findAll())
                .thenReturn(List.of());

        assertNotNull(
                reportService.getDashboardReport(
                        "day",
                        null,
                        null
                )
        );
    }


    // =========================================================
    // DEPARTMENT REVENUE - TEST REQUEST MATCH
    // =========================================================

    @Test
    void getDashboardReport_ShouldCalculateDepartmentRevenueFromTestRequest() {

        LocalDate today =
                LocalDate.now();

        UUID departmentId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Department department =
                department(
                        departmentId,
                        "Xet nghiem",
                        "LAB01"
                );

        MedicalService service =
                medicalService(
                        serviceId,
                        "Xet nghiem mau",
                        "Mo ta",
                        BigDecimal.valueOf(
                                150000
                        ),
                        DepartmentType.PARACLINICAL
                );

        CustomerVisit visit =
                mock(CustomerVisit.class);

        Invoice invoice =
                invoice(
                        InvoiceStatus.PAID,
                        today,
                        150000
                );

        when(invoice.getVisit())
                .thenReturn(visit);

        UUID itemId =
                UUID.randomUUID();

        InvoiceItem item =
                mock(InvoiceItem.class);

        when(item.getItemId())
                .thenReturn(itemId);

        when(item.getInvoice())
                .thenReturn(invoice);

        when(item.getService())
                .thenReturn(service);

        when(item.getFinalPrice())
                .thenReturn(
                        BigDecimal.valueOf(
                                150000
                        )
                );

        TestRequest request =
                mock(TestRequest.class);

        when(request.getInvoiceItem())
                .thenReturn(item);

        when(request.getPerformingDepartment())
                .thenReturn(department);

        when(invoiceRepo.findAll())
                .thenReturn(
                        List.of(invoice)
                );

        when(queueTicketRepo.findAll())
                .thenReturn(List.of());

        when(departmentRepo.findAll())
                .thenReturn(
                        List.of(department)
                );

        when(invoiceItemRepo.findAll())
                .thenReturn(
                        List.of(item)
                );

        when(medicalRecordRepo.findAll())
                .thenReturn(List.of());

        when(testRequestRepo.findAll())
                .thenReturn(
                        List.of(request)
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