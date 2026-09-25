package vn.edu.fpt.cares.dto;

import vn.edu.fpt.cares.dto.customervisit.CustomerVisitResponse;
import vn.edu.fpt.cares.dto.department.DepartmentResponse;
import vn.edu.fpt.cares.dto.invoice.PaymentHistoryResponse;
import vn.edu.fpt.cares.dto.invoice.ReceiptDetailResponse;
import vn.edu.fpt.cares.dto.invoice.ReceiptItemResponse;
import vn.edu.fpt.cares.dto.invoice.ReceiptPrintResponse;
import vn.edu.fpt.cares.dto.medicalrecord.FollowUpResponse;
import vn.edu.fpt.cares.enums.*;
import vn.edu.fpt.cares.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResponseMappingCoverageTest {

    @ParameterizedTest
    @EnumSource(PaymentMethod.class)
    void paymentMethodLabelsAndReceiptPrintingCoverEveryMethod(PaymentMethod method) {
        Profile patient = profile(Gender.MALE);
        StaffInfo cashier = staff(SystemRole.CASHIER, "Thu ngân A");
        Invoice invoice = invoice(patient, InvoiceStatus.PAID, BigDecimal.valueOf(100_000));
        Transaction payment = Transaction.builder().transactionId(UUID.randomUUID()).invoice(invoice)
                .transactionCode("TX-" + method.name()).amount(invoice.getTotalAmount())
                .paymentMethod(method).status(TransactionStatus.SUCCESS)
                .paidAt(LocalDateTime.of(2026, 9, 6, 10, 30)).receivedBy(cashier).build();

        ReceiptPrintResponse result = ReceiptPrintResponse.from(invoice, payment);

        assertNotNull(method.getDisplayName());
        assertNotNull(result.paymentMethod());
        assertEquals("Thu ngân A", result.cashierName());
        assertEquals(payment.getTransactionCode(), result.receiptNumber());
    }

    @Test
    void paymentHistoryCoversDatesMethodsAndStatuses() {
        Invoice pending = invoice(profile(Gender.FEMALE), InvoiceStatus.PENDING, BigDecimal.TEN);
        pending.setIssueDate(LocalDate.of(2026, 9, 5));
        assertEquals("pending", PaymentHistoryResponse.from(pending, null).status());
        assertEquals("2026-09-05", PaymentHistoryResponse.from(pending, null).settlementDate());
        assertNull(PaymentHistoryResponse.from(pending, null).paymentMethod());

        pending.setStatus(InvoiceStatus.CANCELLED);
        Transaction paid = Transaction.builder().paymentMethod(PaymentMethod.CASH)
                .paidAt(LocalDateTime.of(2026, 9, 6, 8, 7)).build();
        var cancelled = PaymentHistoryResponse.from(pending, paid);
        assertEquals("cancelled", cancelled.status());
        assertEquals("08:07", cancelled.settlementTime());
        assertEquals("Cash", cancelled.paymentMethod());

        pending.setStatus(InvoiceStatus.PAID);
        paid.setPaymentMethod(null);
        assertEquals("paid", PaymentHistoryResponse.from(pending, paid).status());
    }

    @Test
    void receiptMappingsCoverInsuranceMembershipAndPatientVariants() {
        for (Gender gender : Gender.values()) {
            Profile patient = profile(gender);
            Invoice invoice = invoice(patient, InvoiceStatus.PAID, BigDecimal.valueOf(85_000));
            InvoiceItem item = invoice.getItems().get(0);
            item.setBhytFund(BigDecimal.valueOf(15_000));
            MembershipCard card = MembershipCard.builder().cardCode(" cs-1234-abcd ").build();
            MembershipCardLedger ledger = MembershipCardLedger.builder().card(card)
                    .amount(BigDecimal.valueOf(85_000)).benefitDiscount(BigDecimal.valueOf(15_000)).build();
            ReceiptPrintResponse print = ReceiptPrintResponse.from(invoice, null, ledger);
            ReceiptDetailResponse detail = ReceiptDetailResponse.from(invoice, null, ledger);
            assertEquals("CS-••••-ABCD", print.membershipCardCodeMasked());
            assertEquals(new BigDecimal("15.00"), print.membershipBenefitPercent());
            assertEquals(BigDecimal.valueOf(15_000), detail.bhytCoverage());
            assertFalse(detail.items().isEmpty());
        }

        Invoice noPatient = invoice(null, InvoiceStatus.PENDING, BigDecimal.ZERO);
        noPatient.setIssueDate(null);
        noPatient.setSubtotal(BigDecimal.ZERO);
        noPatient.setPaidAmount(null);
        noPatient.setTotalAmount(null);
        noPatient.setTax(null);
        noPatient.setItems(List.of());
        ReceiptPrintResponse blank = ReceiptPrintResponse.from(noPatient, null,
                MembershipCardLedger.builder().card(MembershipCard.builder().cardCode(" ").build())
                        .amount(BigDecimal.ZERO).benefitDiscount(null).build());
        assertNull(blank.patientName());
        assertNull(blank.paymentMethod());
        assertNull(blank.membershipCardCodeMasked());

        noPatient.setTotalAmount(BigDecimal.ZERO);
        noPatient.setPaidAmount(BigDecimal.ZERO);
        noPatient.setTax(BigDecimal.ZERO);
        assertEquals("không đồng", ReceiptDetailResponse.from(noPatient).inWords());
    }

    @Test
    void receiptItemHandlesServiceAndInsuranceEdgeCases() {
        MedicalService service = MedicalService.builder().name("Công thức máu").build();
        InvoiceItem covered = InvoiceItem.builder().service(service).serviceSnapshot("CBC")
                .quantity(1).unitPrice(BigDecimal.valueOf(100_000)).lineTotal(BigDecimal.valueOf(100_000))
                .bhytFund(BigDecimal.valueOf(80_000)).build();
        assertEquals(80, ReceiptItemResponse.from(covered).bhytRate());
        assertEquals("Công thức máu", ReceiptItemResponse.from(covered).category());

        for (InvoiceItem item : List.of(
                InvoiceItem.builder().serviceSnapshot("Khám").quantity(1).unitPrice(BigDecimal.ZERO)
                        .lineTotal(null).bhytFund(BigDecimal.ONE).build(),
                InvoiceItem.builder().serviceSnapshot("Khám").quantity(1).unitPrice(BigDecimal.ZERO)
                        .lineTotal(BigDecimal.ZERO).bhytFund(BigDecimal.ONE).build(),
                InvoiceItem.builder().serviceSnapshot("Khám").quantity(1).unitPrice(BigDecimal.ZERO)
                        .lineTotal(BigDecimal.ONE).bhytFund(null).build())) {
            assertEquals(0, ReceiptItemResponse.from(item).bhytRate());
            assertEquals("Khám bệnh", ReceiptItemResponse.from(item).category());
        }
    }

    @Test
    void followUpMapsRegisteredGuestAndMissingDoctor() {
        StaffInfo doctor = staff(SystemRole.DOCTOR, "Bác sĩ A");
        MedicalRecord registered = record(CustomerVisit.builder().customer(profile(Gender.MALE)).build(), doctor);
        assertEquals("Bác sĩ A", FollowUpResponse.from(registered).doctorName());

        Appointment guestAppointment = Appointment.builder().isGuest(true).guestFullName("Khách A")
                .guestPhone("0900000000").build();
        MedicalRecord guest = record(CustomerVisit.builder().appointment(guestAppointment).build(), null);
        assertEquals("Khách A", FollowUpResponse.from(guest).customerName());
        assertEquals("Unknown", FollowUpResponse.from(guest).doctorName());

        guest.getVisit().setAppointment(null);
        assertEquals("Khách vãng lai", FollowUpResponse.from(guest).customerName());
    }

    @Test
    void departmentMappingCoversCoverageStates() {
        StaffInfo doctor = staff(SystemRole.DOCTOR, "Bác sĩ A");
        StaffInfo nurse = staff(SystemRole.NURSE, "Điều dưỡng A");
        Department department = Department.builder().departmentId(UUID.randomUUID()).name("Phòng Nội")
                .departmentType(DepartmentType.EXAMINATION).nurses(List.of()).capabilities(java.util.Set.of()).build();
        assertEquals("UNASSIGNED", DepartmentResponse.from(department, List.of(), List.of()).coverageStatus());
        assertEquals("MISSING_DOCTOR", DepartmentResponse.from(department, List.of(nurse), List.of(nurse)).coverageStatus());
        department.setHeadDoctor(doctor);
        var covered = DepartmentResponse.from(department, List.of(doctor, nurse), List.of(doctor, nurse));
        assertEquals("COVERED", covered.coverageStatus());
        assertEquals(1, covered.doctors().size());
        assertEquals(1, covered.nursesOnDuty().size());
    }

    @Test
    void customerVisitMappingCoversRegisteredGuestAndInvoiceSummaries() {
        Profile patient = profile(Gender.FEMALE);
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(patient)
                .status(VisitStatus.COMPLETED).build();
        assertEquals(patient.getFullName(), CustomerVisitResponse.from(visit).customerName());

        Appointment guest = Appointment.builder().appointmentId(UUID.randomUUID()).isGuest(true)
                .guestFullName("Khách B").build();
        visit.setAppointment(guest);
        assertEquals("Khách B", CustomerVisitResponse.from(visit, UUID.randomUUID()).customerName());

        Invoice empty = invoice(patient, InvoiceStatus.PENDING, BigDecimal.ZERO);
        empty.setItems(List.of());
        assertNull(CustomerVisitResponse.from(visit, empty).serviceSummary());
        Invoice full = invoice(patient, InvoiceStatus.PAID, BigDecimal.TEN);
        full.getItems().add(InvoiceItem.builder().service(null).serviceSnapshot("Dịch vụ cũ")
                .quantity(1).unitPrice(BigDecimal.ONE).lineTotal(BigDecimal.ONE).build());
        var response = CustomerVisitResponse.from(visit, full);
        assertTrue(response.serviceSummary().contains("Khám Nội"));
        assertTrue(response.serviceSummary().contains("Dịch vụ cũ"));
        assertEquals("PAID", response.invoiceStatus());
    }

    private Profile profile(Gender gender) {
        return Profile.builder().profileId(UUID.randomUUID()).patientCode("BN-001").fullName("Nguyễn An")
                .phone("0900000000").address("TP.HCM").dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(gender).insuranceId("BHYT-1").build();
    }

    private StaffInfo staff(SystemRole role, String name) {
        return StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(role)
                .profile(Profile.builder().profileId(UUID.randomUUID()).fullName(name).build()).build();
    }

    private Invoice invoice(Profile patient, InvoiceStatus status, BigDecimal total) {
        MedicalService service = MedicalService.builder().serviceId(UUID.randomUUID()).name("Khám Nội").build();
        Invoice invoice = Invoice.builder().invoiceId(UUID.randomUUID()).invoiceCode("INV-001").customer(patient)
                .issueDate(LocalDate.of(2026, 9, 6)).subtotal(BigDecimal.valueOf(100_000))
                .tax(BigDecimal.ZERO).totalAmount(total).paidAmount(total).status(status).build();
        invoice.setItems(new java.util.ArrayList<>(List.of(InvoiceItem.builder().invoice(invoice).service(service)
                .serviceSnapshot("Khám Nội").quantity(1).unitPrice(BigDecimal.valueOf(100_000))
                .lineTotal(BigDecimal.valueOf(100_000)).bhytFund(BigDecimal.ZERO).build())));
        return invoice;
    }

    private MedicalRecord record(CustomerVisit visit, StaffInfo doctor) {
        return MedicalRecord.builder().recordId(UUID.randomUUID()).recordCode("MR-001")
                .visit(visit).doctor(doctor).followUpNote("Tái khám").followUpDate(LocalDate.now().plusDays(7)).build();
    }
}
