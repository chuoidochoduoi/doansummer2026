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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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


    @ParameterizedTest
    @CsvSource({"WAITING_FOR_TEST,BLOCKED,IN_PROGRESS", "WAITING_FOR_TEST,TEST_DONE,COMPLETED", "TEST_DONE,TEST_DONE,COMPLETED", "CALLED,CALLED,COMPLETED", "IN_PROGRESS,IN_PROGRESS,COMPLETED", "DONE,DONE,COMPLETED"})
    void specialistInitialPhaseRemainsCompletedDuringReturn(String examinationStatus, String returnStatus, String testStatus) {
        var base = LocalDateTime.of(2026, 9, 1, 8, 0);
        var visit = CustomerVisit.builder().visitId(UUID.randomUUID()).status(VisitStatus.IN_PROGRESS)
                .checkInTime(base).customer(Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Anh Đức").build()).build();
        var room = Department.builder().departmentId(UUID.randomUUID()).departmentType(DepartmentType.EXAMINATION).build();
        var labRoom = Department.builder().departmentId(UUID.randomUUID()).departmentType(DepartmentType.PARACLINICAL).build();
        var examinationService = MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("EX-INT-001").name("Khám Nội tổng quát").build();
        var nextService = MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("EX-INT-002").name("Khám Tim mạch").build();
        var labService = MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("LAB-001").name("Công thức máu").department(labRoom).build();
        var examination = ticket(visit, room, examinationService, QueueStatus.valueOf(examinationStatus), 1, base);
        var next = ticket(visit, room, nextService, QueueStatus.BLOCKED, 2, base.plusMinutes(1));
        var record = MedicalRecord.builder().recordId(UUID.randomUUID()).visit(visit).queueTicket(examination).build();
        var initial = invoice(visit, null, InvoiceStatus.PAID, base);
        var order = invoice(visit, record, InvoiceStatus.PAID, base.plusMinutes(20));
        boolean testsCompleted = "COMPLETED".equals(testStatus);
        var lab = ticket(visit, labRoom, labService, testsCompleted ? QueueStatus.DONE : QueueStatus.IN_PROGRESS, 3, base.plusMinutes(25));
        var result = test(record, labService, labRoom, lab, order, TestRequestStatus.valueOf(testStatus), base.plusMinutes(25));
        when(visitRepo.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(invoiceRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(initial, order));
        when(queueRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(next, lab, examination));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId())).thenReturn(List.of(result));

        var journey = service.get(visit.getVisitId());

        assertEquals(List.of("INITIAL_PAYMENT", "INITIAL_EXAMINATION", "ORDER_PAYMENT", "PARACLINICAL", "RETURN_EXAMINATION", "INITIAL_EXAMINATION"),
                journey.steps().stream().map(step -> step.phase()).toList());
        assertEquals("DONE", journey.steps().get(1).status());
        assertEquals("DONE", journey.steps().get(1).services().get(0).status());
        assertEquals(returnStatus, journey.steps().get(4).status());
        assertEquals(examination.getTicketId(), journey.steps().get(4).queueTicketId());
        assertEquals("BLOCKED", journey.steps().get(5).status());
        assertEquals(next.getTicketId(), journey.steps().get(5).queueTicketId());
        if (!testsCompleted) assertEquals(journey.steps().get(3).id(), journey.currentStepId());
        else if (!"DONE".equals(returnStatus)) assertEquals(journey.steps().get(4).id(), journey.currentStepId());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void independentSpecialistExaminationsDoNotCompleteTogether(boolean sameRoom) {
        var base = LocalDateTime.of(2026, 9, 1, 8, 0);
        var visit = CustomerVisit.builder().visitId(UUID.randomUUID()).status(VisitStatus.IN_PROGRESS)
                .checkInTime(base).customer(Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Anh Đức").build()).build();
        var room = Department.builder().departmentId(UUID.randomUUID()).departmentType(DepartmentType.EXAMINATION).build();
        var nextRoom = sameRoom ? room : Department.builder().departmentId(UUID.randomUUID()).departmentType(DepartmentType.EXAMINATION).build();
        var firstService = MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("EX-INT-001").name("Khám Nội tổng quát").build();
        var secondService = MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("EX-INT-002").name("Khám Tim mạch").build();
        var first = ticket(visit, room, firstService, QueueStatus.IN_PROGRESS, 1, base);
        var next = ticket(visit, nextRoom, secondService, QueueStatus.BLOCKED, 2, base.plusMinutes(1));
        when(visitRepo.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(queueRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(next, first));
        when(invoiceRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(invoice(visit, null, InvoiceStatus.PAID, base)));

        var journey = service.get(visit.getVisitId());

        assertEquals(List.of("INITIAL_PAYMENT", "INITIAL_EXAMINATION", "INITIAL_EXAMINATION"), journey.steps().stream().map(step -> step.phase()).toList());
        assertEquals("IN_PROGRESS", journey.steps().get(1).status());
        assertEquals("BLOCKED", journey.steps().get(2).status());
        assertEquals(0, journey.steps().get(2).completedServices());
        assertEquals(journey.steps().get(1).id(), journey.currentStepId());
    }

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

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void sharedLabQueueAcrossSameRoomRecordsCreatesOneCycleAndPointsToLabStep(boolean sharedCall, boolean sameExaminationRoom) {
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
        Department secondRoom = sameExaminationRoom ? examinationRoom : Department.builder()
                .departmentId(UUID.randomUUID()).name("Phong kham Tim mach").roomCode("INT-105")
                .departmentType(DepartmentType.EXAMINATION).build();
        QueueTicket examination2 = ticket(visit, secondRoom, examinationService2,
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
        QueueTicket legacyQueue = sharedCall ? sharedLabQueue : ticket(visit, labRoom, labService2,
                QueueStatus.BLOCKED, 4, base.plusMinutes(60));
        TestRequest sameCallLegacy = test(record2, labService2, labRoom, legacyQueue, null,
                TestRequestStatus.BLOCKED, base.plusMinutes(51));

        when(visitRepo.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(invoiceRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(order, initial));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId()))
                .thenReturn(List.of(linked, sameCallLegacy));
        when(queueRepo.findAllByVisit_VisitId(visit.getVisitId()))
                .thenReturn(sharedCall ? List.of(examination2, sharedLabQueue, examination1)
                        : List.of(examination2, legacyQueue, sharedLabQueue, examination1));

        var result = service.get(visit.getVisitId());
        assertEquals(sharedCall ? List.of(
                        "INITIAL_PAYMENT", "INITIAL_EXAMINATION", "ORDER_PAYMENT",
                        "PARACLINICAL", "RETURN_EXAMINATION", "INITIAL_EXAMINATION") : List.of(
                        "INITIAL_PAYMENT", "INITIAL_EXAMINATION", "ORDER_PAYMENT", "PARACLINICAL",
                        "RETURN_EXAMINATION", "INITIAL_EXAMINATION", "PARACLINICAL", "RETURN_EXAMINATION"),
                result.steps().stream().map(step -> step.phase()).toList());
        assertEquals(sharedCall ? 2 : 1, result.steps().get(3).totalServices());
        assertEquals("BLOCKED", result.steps().get(3).status());
        assertEquals(result.steps().get(3).id(), result.currentStepId());
        assertEquals(sharedCall ? 1 : 2, result.steps().stream()
                .filter(step -> "RETURN_EXAMINATION".equals(step.phase())).count());
        assertEquals(examination1.getTicketId(), result.steps().get(4).queueTicketId());
        assertEquals("BLOCKED", result.steps().get(4).status());
        if (!sharedCall) {
            assertEquals(legacyQueue.getTicketId(), result.steps().get(6).queueTicketId());
            assertEquals(examination2.getTicketId(), result.steps().get(7).queueTicketId());
        }
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
