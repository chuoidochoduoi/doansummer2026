package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicOverviewReportTest {
    @Mock CustomerVisitRepository visitRepo;
    @Mock TransactionRepository transactionRepo;
    @Mock MembershipCardLedgerRepository ledgerRepo;
    @Mock InvoiceRepository invoiceRepo;
    @Mock MedicalServiceRepository serviceRepo;
    @Mock QueueTicketRepository queueTicketRepo;
    @Mock DepartmentRepository departmentRepo;
    @Mock InvoiceItemRepository invoiceItemRepo;
    @Mock MedicalRecordRepository medicalRecordRepo;
    @Mock TestRequestRepository testRequestRepo;
    @InjectMocks ReportService service;
    private final LocalDate day = LocalDate.of(2026,9,4);
    private BigDecimal b(long value) { return BigDecimal.valueOf(value); }
    private Transaction payment(Invoice invoice, long amount, TransactionStatus status) {
        return Transaction.builder().transactionId(UUID.randomUUID()).invoice(invoice).amount(b(amount))
                .status(status).paymentMethod(PaymentMethod.CASH).paidAt(day.atTime(12,0)).build();
    }

    @Test void paymentsUsePaidAtIncludePartialPaymentsAndExcludeCancelled() {
        var oldInvoice = Invoice.builder().invoiceId(UUID.randomUUID()).issueDate(day.minusMonths(1))
                .status(InvoiceStatus.PENDING).subtotal(b(1000)).totalAmount(b(1000)).paidAmount(b(200)).build();
        when(invoiceRepo.findAll()).thenReturn(List.of(oldInvoice));
        when(transactionRepo.findAll()).thenReturn(List.of(payment(oldInvoice,200,TransactionStatus.SUCCESS),
                payment(oldInvoice,800,TransactionStatus.CANCELLED),payment(oldInvoice,50,TransactionStatus.FAILED)));
        var result = service.getOverview(day,day.plusDays(2));
        assertEquals(0,b(200).compareTo(result.finance().collected()));
        assertEquals(1,result.finance().successfulPayments());
        assertEquals(0,result.finance().invoiceGross().signum());
        assertEquals(3,result.paymentChart().size());
        assertEquals(0,result.paymentChart().get(1).amount().signum());
    }

    @Test void caresAndInsuranceReconcileWithoutChangingItemAmountsOrMultiplyingFinalPriceTwice() {
        var inv = Invoice.builder().invoiceId(UUID.randomUUID()).issueDate(day).status(InvoiceStatus.PAID)
                .subtotal(b(200)).discount(b(64)).tax(b(0)).totalAmount(b(136)).paidAmount(b(136)).build();
        var item = InvoiceItem.builder().itemId(UUID.randomUUID()).invoice(inv).serviceSnapshot("Dịch vụ tại thời điểm bán")
                .serviceCodeSnapshot("LAB-X").unitPrice(b(100)).quantity(2).bhytFund(b(40)).finalPrice(b(160)).lineTotal(b(200)).build();
        var tx = payment(inv,136,TransactionStatus.SUCCESS); tx.setPaymentMethod(PaymentMethod.MEMBERSHIP_CARD);
        var ledger = MembershipCardLedger.builder().type(MembershipLedgerType.PAYMENT).invoice(inv)
                .paymentTransaction(tx).benefitDiscount(b(24)).build();
        when(invoiceRepo.findAll()).thenReturn(List.of(inv)); when(invoiceItemRepo.findAll()).thenReturn(List.of(item));
        when(transactionRepo.findAll()).thenReturn(List.of(tx)); when(ledgerRepo.findAll()).thenReturn(List.of(ledger));
        var result = service.getOverview(day,day);
        var f = result.finance();
        assertEquals(0,f.invoiceGross().subtract(f.insurance()).subtract(f.caresBenefit()).subtract(f.otherDiscount())
                .add(f.tax()).compareTo(f.invoicePayable()));
        assertEquals(0,b(24).compareTo(f.caresBenefit()));
        assertEquals(0,f.outstanding().signum());
        assertEquals(2,result.services().get(0).quantity());
        assertEquals(0,b(160).compareTo(result.services().get(0).patientAmount()));
        assertEquals("Dịch vụ tại thời điểm bán",result.services().get(0).name());
        verify(invoiceRepo,never()).save(any()); verify(invoiceItemRepo,never()).save(any());
    }

    @Test void reversedCardPaymentAndTopupAreNotCountedAsServicePaymentsOrBenefits() {
        var inv = Invoice.builder().invoiceId(UUID.randomUUID()).issueDate(day).status(InvoiceStatus.PENDING)
                .subtotal(b(200)).totalAmount(b(200)).paidAmount(b(0)).build();
        var cancelled = payment(inv,170,TransactionStatus.CANCELLED);
        when(invoiceRepo.findAll()).thenReturn(List.of(inv)); when(transactionRepo.findAll()).thenReturn(List.of(cancelled));
        when(ledgerRepo.findAll()).thenReturn(List.of(
                MembershipCardLedger.builder().type(MembershipLedgerType.PAYMENT).invoice(inv).paymentTransaction(cancelled).benefitDiscount(b(30)).build(),
                MembershipCardLedger.builder().type(MembershipLedgerType.TOP_UP).amount(b(1000000)).build()));
        var f = service.getOverview(day,day).finance();
        assertEquals(0,f.collected().signum()); assertEquals(0,f.caresBenefit().signum());
        assertEquals(0,b(200).compareTo(f.outstanding()));
    }

    @Test void visitsRecordsAndSignedTestsAreDifferentUnitsEvenWhenTestsShareAQueue() {
        var room = Department.builder().departmentId(UUID.randomUUID()).roomCode("INT-104").name("Nội 2").build();
        var exam = MedicalService.builder().serviceId(UUID.randomUUID()).departmentType(DepartmentType.EXAMINATION).build();
        var visit = CustomerVisit.builder().visitId(UUID.randomUUID()).checkInTime(day.atTime(8,0))
                .checkOutTime(day.atTime(14,0)).status(VisitStatus.COMPLETED).build();
        var q = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).department(room).service(exam)
                .workDate(day).status(QueueStatus.DONE).build();
        var skipped = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).department(room).service(exam)
                .workDate(day).status(QueueStatus.SKIPPED).build();
        var r1 = MedicalRecord.builder().recordId(UUID.randomUUID()).queueTicket(q).status(MedicalRecordStatus.COMPLETED)
                .completedAt(day.atTime(12,0)).ratingScore(5).ratedAt(day.atTime(15,0)).build();
        var r2 = MedicalRecord.builder().recordId(UUID.randomUUID()).queueTicket(q).status(MedicalRecordStatus.COMPLETED)
                .completedAt(day.atTime(13,0)).build();
        var t1 = TestRequest.builder().testRequestId(UUID.randomUUID()).queueTicket(q).performingDepartment(room)
                .status(TestRequestStatus.COMPLETED).completedAt(day.atTime(10,0)).build();
        var t2 = TestRequest.builder().testRequestId(UUID.randomUUID()).queueTicket(q).performingDepartment(room)
                .status(TestRequestStatus.COMPLETED).completedAt(day.atTime(11,0)).build();
        var unsigned = TestRequest.builder().testRequestId(UUID.randomUUID()).status(TestRequestStatus.COMPLETED)
                .completedAt(day.atTime(11,0)).performingDepartment(room).build();
        t1.setTestResult(signedResult(t1));
        t2.setTestResult(signedResult(t2));
        when(visitRepo.findAll()).thenReturn(List.of(visit)); when(queueTicketRepo.findAll()).thenReturn(List.of(q,skipped));
        when(medicalRecordRepo.findAll()).thenReturn(List.of(r1,r2)); when(departmentRepo.findAll()).thenReturn(List.of(room));
        when(testRequestRepo.findAll()).thenReturn(List.of(t1,t2,unsigned));
        var report = service.getOverview(day,day);
        assertEquals(1,report.activity().arrivals()); assertEquals(1,report.activity().closedVisits());
        assertEquals(1,report.activity().partialVisits()); assertEquals(2,report.activity().completedExaminations());
        assertEquals(2,report.activity().completedTests()); assertEquals(2,report.rooms().get(0).completedTests());
        assertEquals(1,report.rooms().get(0).ratingCount()); assertEquals(5.0,report.rooms().get(0).rating());
    }
    private TestResult signedResult(TestRequest request) {
        return TestResult.builder().testRequest(request)
                .verifiedBy(StaffInfo.builder().staffId(UUID.randomUUID()).build())
                .verifiedAt(day.atTime(11,0)).build();
    }

    @Test void recordUpdatesDoNotMoveCompletionIntoAnotherPeriodAndNoFeedbackIsNotZeroStars() {
        var room = Department.builder().departmentId(UUID.randomUUID()).roomCode("INT").name("Nội").build();
        var q = QueueTicket.builder().department(room).service(MedicalService.builder().departmentType(DepartmentType.EXAMINATION).build()).build();
        var record = MedicalRecord.builder().status(MedicalRecordStatus.COMPLETED).completedAt(day.minusDays(1).atTime(12,0)).queueTicket(q).build();
        record.setUpdatedAt(day.atTime(13,0));
        when(medicalRecordRepo.findAll()).thenReturn(List.of(record)); when(departmentRepo.findAll()).thenReturn(List.of(room));
        var report = service.getOverview(day,day);
        assertEquals(0,report.activity().completedExaminations()); assertNull(report.rooms().get(0).rating());
    }

    @Test void invalidRangeIsRejectedBeforeReadingData() {
        assertThrows(BadRequestException.class,() -> service.getOverview(day,null));
        assertThrows(BadRequestException.class,() -> service.getOverview(day,day.minusDays(1)));
        assertThrows(BadRequestException.class,() -> service.getOverview(day,day.plusDays(367)));
        verifyNoInteractions(invoiceRepo,transactionRepo,visitRepo);
    }

    @Test void defaultRangeAndNullOrExcludedRowsProduceAStableEmptyOverview() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        var cancelledInvoice = Invoice.builder().invoiceId(UUID.randomUUID()).issueDate(today)
                .status(InvoiceStatus.CANCELLED).build();
        var insurancePayment = Transaction.builder().status(TransactionStatus.SUCCESS)
                .paymentMethod(PaymentMethod.INSURANCE).paidAt(today.atTime(12, 0)).amount(b(99)).build();
        var noDatePayment = Transaction.builder().status(TransactionStatus.SUCCESS)
                .paymentMethod(PaymentMethod.CASH).amount(b(99)).build();
        var orphanItem = InvoiceItem.builder().invoice(null).build();
        var orphanQueue = QueueTicket.builder().status(QueueStatus.SKIPPED).visit(null).workDate(today).build();
        var cancelledVisit = CustomerVisit.builder().visitId(UUID.randomUUID()).status(VisitStatus.CANCELLED)
                .checkOutTime(today.minusDays(1).atTime(12, 0)).build();

        when(invoiceRepo.findAll()).thenReturn(List.of(cancelledInvoice));
        when(transactionRepo.findAll()).thenReturn(List.of(insurancePayment, noDatePayment));
        when(invoiceItemRepo.findAll()).thenReturn(List.of(orphanItem));
        when(queueTicketRepo.findAll()).thenReturn(List.of(orphanQueue));
        when(visitRepo.findAll()).thenReturn(List.of(cancelledVisit));

        var result = service.getOverview(null, null);

        assertEquals(today, result.fromDate());
        assertEquals(today, result.toDate());
        // Arrival is counted from check-in time even when the visit is later cancelled.
        assertEquals(1, result.activity().arrivals());
        assertEquals(0, result.finance().collected().signum());
        assertTrue(result.services().isEmpty());
    }

    @Test void overviewCoversMonthlyTrendRoomStatesFallbacksAndNullMoney() {
        LocalDate start = day.minusDays(70);
        var room = Department.builder().departmentId(UUID.randomUUID()).roomCode("LAB-1").name("Lab").build();
        var visit = CustomerVisit.builder().visitId(UUID.randomUUID()).status(VisitStatus.CANCELLED)
                .checkInTime(day.atTime(8, 0)).checkOutTime(day.atTime(9, 0)).build();
        var invoice = Invoice.builder().invoiceId(UUID.randomUUID()).issueDate(day).status(InvoiceStatus.PAID)
                .subtotal(null).discount(null).tax(null).totalAmount(b(100)).paidAmount(b(150)).build();
        var item = InvoiceItem.builder().invoice(invoice).service(null).serviceCodeSnapshot(null)
                .serviceSnapshot(null).quantity(null).unitPrice(null).bhytFund(null)
                .lineTotal(b(100)).finalPrice(null).build();
        var payment = payment(invoice, 100, TransactionStatus.SUCCESS);
        payment.setPaymentMethod(null);
        var waiting = QueueTicket.builder().department(room).visit(visit).workDate(day)
                .status(QueueStatus.WAITING).build();
        var called = QueueTicket.builder().department(room).visit(visit).workDate(day)
                .status(QueueStatus.CALLED).build();
        var testDone = QueueTicket.builder().department(room).visit(visit).workDate(day)
                .status(QueueStatus.TEST_DONE).build();
        var active = QueueTicket.builder().department(room).visit(visit).workDate(day)
                .status(QueueStatus.IN_PROGRESS).build();
        var waitingTest = QueueTicket.builder().department(room).visit(visit).workDate(day)
                .status(QueueStatus.WAITING_FOR_TEST).build();
        var skipped = QueueTicket.builder().department(room).visit(visit).workDate(day)
                .status(QueueStatus.SKIPPED).build();

        when(visitRepo.findAll()).thenReturn(List.of(visit));
        when(invoiceRepo.findAll()).thenReturn(List.of(invoice));
        when(invoiceItemRepo.findAll()).thenReturn(List.of(item));
        when(transactionRepo.findAll()).thenReturn(List.of(payment));
        when(queueTicketRepo.findAll()).thenReturn(List.of(waiting, called, testDone, active, waitingTest, skipped));
        when(departmentRepo.findAll()).thenReturn(List.of(room));

        var result = service.getOverview(start, day);

        assertTrue(result.paymentChart().size() > 1);
        assertEquals("OTHER", result.paymentMethods().get(0).label());
        assertEquals(3, result.rooms().get(0).waiting());
        assertEquals(2, result.rooms().get(0).inProgress());
        assertEquals(1, result.rooms().get(0).skipped());
        assertNull(result.services().get(0).code());
        assertEquals("OTHER", result.services().get(0).category());
        assertEquals(1, result.services().get(0).quantity());
        assertEquals(0, result.finance().outstanding().signum());
    }
}
