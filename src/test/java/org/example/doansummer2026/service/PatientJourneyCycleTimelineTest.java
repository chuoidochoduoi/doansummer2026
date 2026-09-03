package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.InvoiceItem;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.example.doansummer2026.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientJourneyCycleTimelineTest {

    @Mock CustomerVisitRepository visitRepo;
    @Mock QueueTicketRepository queueRepo;
    @Mock TestRequestRepository testRepo;
    @Mock InvoiceRepository invoiceRepo;
    @Mock MedicalRecordRepository recordRepo;
    @Mock NotificationRepository notificationRepo;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock QueuePriorityService queuePriorityService;
    @InjectMocks PatientJourneyService service;

    @Test
    void twoClinicalCyclesRemainOrderedAndOnlyLatestReturnIsCurrent() {
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 8, 0);
        CustomerVisit visit = CustomerVisit.builder()
                .visitId(UUID.randomUUID()).status(VisitStatus.IN_PROGRESS).checkInTime(base)
                .customer(Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyen Van A").build())
                .build();
        Department examinationRoom = Department.builder().departmentId(UUID.randomUUID())
                .name("Phong kham Noi").roomCode("INT-101")
                .departmentType(DepartmentType.EXAMINATION).build();
        Department labRoom = Department.builder().departmentId(UUID.randomUUID())
                .name("Phong xet nghiem").roomCode("LAB-201")
                .departmentType(DepartmentType.PARACLINICAL).build();
        MedicalService examinationService = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("EXAM-001").name("Kham Noi tong quat").build();
        MedicalService labService1 = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("LAB-001").name("Cong thuc mau").department(labRoom).build();
        MedicalService labService2 = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("LAB-003").name("Sinh hoa mau co ban").department(labRoom).build();

        QueueTicket examination = ticket(visit, examinationRoom, examinationService,
                QueueStatus.TEST_DONE, 1, base.plusMinutes(5));
        MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID())
                .visit(visit).queueTicket(examination).build();

        Invoice initial = invoice(visit, null, InvoiceStatus.PAID, base.minusMinutes(5));
        Invoice cycle1 = invoice(visit, record, InvoiceStatus.PAID, base.plusMinutes(20));
        Invoice cycle2 = invoice(visit, record, InvoiceStatus.PAID, base.plusMinutes(50));
        QueueTicket lab1 = ticket(visit, labRoom, labService1, QueueStatus.DONE, 2, base.plusMinutes(25));
        QueueTicket lab2 = ticket(visit, labRoom, labService2, QueueStatus.DONE, 3, base.plusMinutes(55));
        TestRequest test1 = test(record, labService1, labRoom, lab1, cycle1, base.plusMinutes(25));
        TestRequest test2 = test(record, labService2, labRoom, lab2, cycle2, base.plusMinutes(55));

        when(visitRepo.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(invoiceRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(cycle2, initial, cycle1));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId())).thenReturn(List.of(test2, test1));
        when(queueRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(lab2, examination, lab1));
        when(recordRepo.findByQueueTicket_TicketId(examination.getTicketId())).thenReturn(Optional.of(record));

        var result = service.get(visit.getVisitId());
        assertEquals(List.of(
                "INITIAL_PAYMENT", "INITIAL_EXAMINATION",
                "ORDER_PAYMENT", "PARACLINICAL", "RETURN_EXAMINATION",
                "ORDER_PAYMENT", "PARACLINICAL", "RETURN_EXAMINATION"),
                result.steps().stream().map(step -> step.phase()).toList());
        assertEquals("DONE", result.steps().get(4).status());
        assertEquals("TEST_DONE", result.steps().get(7).status());
        assertEquals(result.steps().get(7).id(), result.currentStepId());
        assertEquals(examination.getTicketId(), result.steps().get(7).queueTicketId());
    }

    @Test
    void sharedLabQueueAcrossSameRoomRecordsCreatesOneCycleAndPointsToLabStep() {
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 8, 0);
        CustomerVisit visit = CustomerVisit.builder()
                .visitId(UUID.randomUUID()).status(VisitStatus.IN_PROGRESS).checkInTime(base)
                .customer(Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyen Van B").build())
                .build();
        Department examinationRoom = Department.builder().departmentId(UUID.randomUUID())
                .name("Phong kham Noi 2").roomCode("INT-104")
                .departmentType(DepartmentType.EXAMINATION).build();
        Department labRoom = Department.builder().departmentId(UUID.randomUUID())
                .name("Phong xet nghiem sinh hoa").roomCode("LAB-202")
                .departmentType(DepartmentType.PARACLINICAL).build();
        MedicalService examinationService1 = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("EXAM-001").name("Kham Noi tong quat").build();
        MedicalService examinationService2 = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("EXAM-002").name("Kham Tim mach co ban").build();
        MedicalService labService1 = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("LAB-003").name("Sinh hoa mau co ban").department(labRoom).build();
        MedicalService labService2 = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("LAB-006").name("Chuc nang than").department(labRoom).build();

        QueueTicket examination1 = ticket(visit, examinationRoom, examinationService1,
                QueueStatus.DONE, 1, base.plusMinutes(5));
        QueueTicket examination2 = ticket(visit, examinationRoom, examinationService2,
                QueueStatus.DONE, 2, base.plusMinutes(35));
        MedicalRecord record1 = MedicalRecord.builder().recordId(UUID.randomUUID())
                .visit(visit).queueTicket(examination1).build();
        MedicalRecord record2 = MedicalRecord.builder().recordId(UUID.randomUUID())
                .visit(visit).queueTicket(examination2).build();
        Invoice initial = invoice(visit, null, InvoiceStatus.PAID, base.minusMinutes(5));
        Invoice order = invoice(visit, record1, InvoiceStatus.PAID, base.plusMinutes(45));
        QueueTicket sharedLabQueue = ticket(visit, labRoom, labService1,
                QueueStatus.BLOCKED, 3, base.plusMinutes(50));
        TestRequest linked = test(record1, labService1, labRoom, sharedLabQueue, order,
                TestRequestStatus.BLOCKED, base.plusMinutes(50));
        TestRequest sameCallLegacy = test(record2, labService2, labRoom, sharedLabQueue, null,
                TestRequestStatus.BLOCKED, base.plusMinutes(51));

        when(visitRepo.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(invoiceRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(order, initial));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId()))
                .thenReturn(List.of(linked, sameCallLegacy));
        when(queueRepo.findAllByVisit_VisitId(visit.getVisitId()))
                .thenReturn(List.of(examination2, sharedLabQueue, examination1));

        var result = service.get(visit.getVisitId());
        assertEquals(List.of(
                        "INITIAL_PAYMENT", "INITIAL_EXAMINATION", "ORDER_PAYMENT",
                        "PARACLINICAL", "RETURN_EXAMINATION"),
                result.steps().stream().map(step -> step.phase()).toList());
        assertEquals(2, result.steps().get(3).totalServices());
        assertEquals("BLOCKED", result.steps().get(3).status());
        assertEquals(result.steps().get(3).id(), result.currentStepId());
        assertEquals(1, result.steps().stream()
                .filter(step -> "RETURN_EXAMINATION".equals(step.phase())).count());
    }

    private QueueTicket ticket(CustomerVisit visit, Department room, MedicalService medicalService,
                               QueueStatus status, int number, LocalDateTime createdAt) {
        QueueTicket ticket = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit)
                .department(room).service(medicalService).status(status).queueNumber(number)
                .workDate(LocalDate.of(2026, 9, 1)).build();
        ticket.setCreatedAt(createdAt);
        ticket.setUpdatedAt(createdAt.plusMinutes(10));
        if (status == QueueStatus.DONE) ticket.setCompletedAt(createdAt.plusMinutes(10));
        return ticket;
    }

    private Invoice invoice(CustomerVisit visit, MedicalRecord record, InvoiceStatus status,
                            LocalDateTime createdAt) {
        Invoice invoice = Invoice.builder().invoiceId(UUID.randomUUID()).visit(visit)
                .medicalRecord(record).status(status).build();
        invoice.setCreatedAt(createdAt);
        invoice.setUpdatedAt(createdAt.plusMinutes(2));
        return invoice;
    }

    private TestRequest test(MedicalRecord record, MedicalService medicalService, Department room,
                             QueueTicket queue, Invoice invoice, LocalDateTime createdAt) {
        return test(record, medicalService, room, queue, invoice,
                TestRequestStatus.COMPLETED, createdAt);
    }

    private TestRequest test(MedicalRecord record, MedicalService medicalService, Department room,
                             QueueTicket queue, Invoice invoice, TestRequestStatus status,
                             LocalDateTime createdAt) {
        InvoiceItem item = invoice == null ? null : InvoiceItem.builder().itemId(UUID.randomUUID())
                .invoice(invoice).service(medicalService)
                .serviceCodeSnapshot(medicalService.getServiceCode())
                .serviceSnapshot(medicalService.getName()).build();
        TestRequest test = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record)
                .service(medicalService).performingDepartment(room).queueTicket(queue)
                .invoiceItem(item).status(status).build();
        test.setCreatedAt(createdAt);
        if (status == TestRequestStatus.COMPLETED) test.setCompletedAt(createdAt.plusMinutes(10));
        return test;
    }
}
