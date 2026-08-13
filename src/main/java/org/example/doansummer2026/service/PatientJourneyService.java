package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.journey.PatientJourneyResponse;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.example.doansummer2026.common.PageResponse;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class PatientJourneyService {
    private final CustomerVisitRepository visitRepo;
    private final QueueTicketRepository queueRepo;
    private final TestRequestRepository testRepo;
    private final InvoiceRepository invoiceRepo;

    public void activateNext(UUID visitId) {
        if (hasActiveStep(visitId)) return;
        QueueTicket queue = queueRepo.findAllByVisit_VisitId(visitId).stream().filter(q -> q.getStatus()==QueueStatus.BLOCKED)
                .min(Comparator.comparing(QueueTicket::getCreatedAt)).orElse(null);
        // TestRequest da gan QueueTicket se duoc kich hoat theo ticket cua no.
        // Chi giu nhanh du phong nay cho du lieu cu chua co QueueTicket.
        TestRequest test = testRepo.findAllByMedicalRecord_Visit_VisitId(visitId).stream()
                .filter(t -> t.getStatus() == TestRequestStatus.BLOCKED && t.getQueueTicket() == null)
                .min(Comparator.comparing(TestRequest::getCreatedAt)).orElse(null);
        if (queue != null) {
            queue.setStatus(QueueStatus.WAITING); queueRepo.save(queue);
            testRepo.findAllByQueueTicket_TicketId(queue.getTicketId()).stream()
                    .filter(item -> item.getStatus() == TestRequestStatus.BLOCKED)
                    .forEach(item -> { item.setStatus(TestRequestStatus.PENDING); testRepo.save(item); });
        } else if (test != null) { test.setStatus(TestRequestStatus.PENDING); testRepo.save(test); }
        else {
            CustomerVisit visit = visitRepo.findById(visitId).orElse(null);
            boolean hasAnyStep = !queueRepo.findAllByVisit_VisitId(visitId).isEmpty()
                    || !testRepo.findAllByMedicalRecord_Visit_VisitId(visitId).isEmpty();
            boolean awaitingResults = testRepo.findAllByMedicalRecord_Visit_VisitId(visitId).stream()
                    .anyMatch(tests -> tests.getStatus() == TestRequestStatus.PENDING
                            || tests.getStatus() == TestRequestStatus.IN_PROGRESS);
            if (visit != null && hasAnyStep && !hasActiveStep(visitId) && !awaitingResults) {
                visit.setStatus(org.example.doansummer2026.enums.VisitStatus.COMPLETED);
                visit.setCheckOutTime(LocalDateTime.now());
                visitRepo.save(visit);
            }
        }
    }

    @Transactional(readOnly=true)
    public boolean hasActiveStep(UUID visitId) {
        boolean queueActive = queueRepo.findAllByVisit_VisitId(visitId).stream().anyMatch(q ->
                q.getStatus()!=QueueStatus.BLOCKED && q.getStatus()!=QueueStatus.DONE
                        && q.getStatus()!=QueueStatus.SKIPPED && q.getStatus()!=QueueStatus.WAITING_FOR_TEST);
        // Ket qua can lam sang co the van dang xu ly sau khi benh nhan da roi
        // phong. Chi TestRequest cu khong gan QueueTicket moi chan workflow cu.
        boolean testActive = testRepo.findAllByMedicalRecord_Visit_VisitId(visitId).stream().anyMatch(t ->
                t.getQueueTicket() == null
                        && (t.getStatus() == TestRequestStatus.PENDING || t.getStatus() == TestRequestStatus.IN_PROGRESS));
        return queueActive || testActive;
    }

    @Transactional(readOnly=true)
    public PageResponse<PatientJourneyResponse> list(String search, String status, Pageable pageable) {
        String needle = search==null?"":search.trim().toLowerCase();
        List<PatientJourneyResponse> filtered = visitRepo.findAll().stream().map(this::build)
                .filter(j -> needle.isBlank() || ((j.patientName()==null?"":j.patientName())+" "+(j.phone()==null?"":j.phone())+" "+j.visitCode()).toLowerCase().contains(needle))
                .filter(j -> status==null || status.isBlank() || status.equals(j.currentStatus()))
                .sorted(Comparator.comparing(PatientJourneyResponse::checkInTime, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        int size = pageable.getPageSize();
        int from = Math.min(pageable.getPageNumber() * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) filtered.size() / size);
        return new PageResponse<>(filtered.subList(from, to), pageable.getPageNumber(), size,
                filtered.size(), totalPages, pageable.getPageNumber() == 0,
                pageable.getPageNumber() + 1 >= totalPages);
    }

    @Transactional(readOnly=true)
    public PatientJourneyResponse get(UUID id) { return build(visitRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám"))); }

    /** Phuc hoi luot cu bi ket o BLOCKED theo dung quy tac dieu phoi hien tai. */
    public PatientJourneyResponse advanceBlockedStep(UUID visitId) {
        visitRepo.findById(visitId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám"));
        List<QueueTicket> queues = queueRepo.findAllByVisit_VisitId(visitId);
        boolean hasPhysicalActiveQueue = queues.stream().anyMatch(queue ->
                queue.getStatus() == QueueStatus.WAITING || queue.getStatus() == QueueStatus.CALLED
                        || queue.getStatus() == QueueStatus.IN_PROGRESS);
        QueueTicket blockedParaclinical = queues.stream()
                .filter(queue -> queue.getStatus() == QueueStatus.BLOCKED)
                .filter(queue -> queue.getDepartment() != null && queue.getDepartment().getDepartmentType() != null
                        && queue.getDepartment().getDepartmentType().isParaclinical())
                .min(Comparator.comparing(QueueTicket::getCreatedAt)).orElse(null);

        // Du lieu cu co the da chuyen queue phong kham sang TEST_DONE qua som.
        // TEST_DONE khong phai la benh nhan dang o mot phong, nen khong duoc
        // chan queue CLS BLOCKED tiep theo trong thao tac phuc hoi.
        if (!hasPhysicalActiveQueue && blockedParaclinical != null) {
            blockedParaclinical.setStatus(QueueStatus.WAITING);
            queueRepo.save(blockedParaclinical);
            testRepo.findAllByQueueTicket_TicketId(blockedParaclinical.getTicketId()).stream()
                    .filter(test -> test.getStatus() == TestRequestStatus.BLOCKED)
                    .forEach(test -> {
                        test.setStatus(TestRequestStatus.PENDING);
                        testRepo.save(test);
                    });
        } else {
            activateNext(visitId);
        }
        return get(visitId);
    }

    @Transactional(readOnly=true)
    public List<PatientJourneyResponse> listForCustomer(UUID profileId) {
        return visitRepo.findAllByCustomer_ProfileIdOrderByCheckInTimeDesc(profileId).stream().map(this::build).toList();
    }

    private PatientJourneyResponse build(CustomerVisit visit) {
        List<PatientJourneyResponse.Step> steps = new ArrayList<>();
        // Hang cho chi duoc tao sau thanh toan. Dua hoa don PENDING vao hanh
        // trinh de benh nhan biet can den quay thu ngan truoc.
        invoiceRepo.findAllByVisit_VisitId(visit.getVisitId()).stream()
                .filter(invoice -> invoice.getStatus() == org.example.doansummer2026.enums.InvoiceStatus.PENDING)
                .forEach(invoice -> steps.add(new PatientJourneyResponse.Step(
                        "PAYMENT:" + invoice.getInvoiceId(), "PAYMENT", "Thanh toan dich vu",
                        "Quay thu ngan", null, null, "PAYMENT_PENDING",
                        invoice.getCreatedAt(), null)));
        var journeyTests = testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId());
        var testsByQueue = journeyTests.stream().filter(test -> test.getQueueTicket() != null)
                .collect(java.util.stream.Collectors.groupingBy(test -> test.getQueueTicket().getTicketId()));
        queueRepo.findAllByVisit_VisitId(visit.getVisitId()).forEach(queue -> {
            var queueTests = testsByQueue.get(queue.getTicketId());
            boolean paraclinical = queueTests != null && !queueTests.isEmpty();
            String groupedServiceName = paraclinical
                    ? queueTests.stream().map(test -> test.getService() != null ? test.getService().getName() : "Cận lâm sàng")
                        .distinct().collect(java.util.stream.Collectors.joining(", "))
                    : queue.getService() != null ? queue.getService().getName() : "Khám bệnh";
            steps.add(new PatientJourneyResponse.Step(
                    "QUEUE:" + queue.getTicketId(), paraclinical ? "PARACLINICAL" : "EXAMINATION", groupedServiceName,
                    queue.getDepartment().getName(), queue.getDepartment().getRoomCode(), queue.getQueueNumber(),
                    queue.getStatus().name(), queue.getCreatedAt(), queue.getCompletedAt()));
        });
        journeyTests.stream().filter(test -> test.getQueueTicket() == null).forEach(test -> steps.add(new PatientJourneyResponse.Step(
                "TEST:" + test.getTestRequestId(), "PARACLINICAL",
                test.getService() != null ? test.getService().getName() : "Cận lâm sàng",
                test.getPerformingDepartment().getName(), test.getPerformingDepartment().getRoomCode(), null,
                test.getStatus().name(), test.getCreatedAt(), test.getCompletedAt())));

        boolean waitingForResults = journeyTests.stream().anyMatch(test ->
                test.getStatus() == TestRequestStatus.PENDING || test.getStatus() == TestRequestStatus.IN_PROGRESS)
                && queueRepo.findAllByVisit_VisitId(visit.getVisitId()).stream().noneMatch(queue ->
                queue.getStatus() == QueueStatus.WAITING || queue.getStatus() == QueueStatus.CALLED
                        || queue.getStatus() == QueueStatus.IN_PROGRESS);
        if (waitingForResults) {
            steps.add(new PatientJourneyResponse.Step(
                    "RESULTS:" + visit.getVisitId(), "RESULT", "Dang cho ket qua can lam sang",
                    null, null, null, "RESULT_PENDING", LocalDateTime.now(), null));
        }

        if (false) { // Luồng cũ: giữ để tương thích mã nguồn, không cộng trùng QueueTicket và TestRequest.
        queueRepo.findAllByVisit_VisitId(visit.getVisitId()).forEach(q -> steps.add(new PatientJourneyResponse.Step(
                "QUEUE:"+q.getTicketId(), "EXAMINATION", q.getService()!=null?q.getService().getName():"Khám bệnh",
                q.getDepartment().getName(), q.getDepartment().getRoomCode(), q.getQueueNumber(), q.getStatus().name(), q.getCreatedAt(), q.getCompletedAt())));
        testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId()).forEach(t -> steps.add(new PatientJourneyResponse.Step(
                "TEST:"+t.getTestRequestId(), "PARACLINICAL", t.getService()!=null?t.getService().getName():"Cận lâm sàng",
                t.getPerformingDepartment().getName(), t.getPerformingDepartment().getRoomCode(), null, t.getStatus().name(), t.getCreatedAt(), t.getCompletedAt())));
        }
        steps.sort(Comparator.comparing(PatientJourneyResponse.Step::startedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        var payment = steps.stream().filter(s -> s.status().equals("PAYMENT_PENDING")).findFirst().orElse(null);
        var current = payment != null ? payment : steps.stream()
                .filter(s -> !List.of("BLOCKED","DONE","COMPLETED","SKIPPED","CANCELLED").contains(s.status()))
                .findFirst().orElse(null);
        var next = steps.stream().filter(s -> s.status().equals("BLOCKED")).findFirst().orElse(null);
        boolean finished = current==null && next==null && !steps.isEmpty();
        String name = visit.getCustomer()!=null ? visit.getCustomer().getFullName() : visit.getAppointment()!=null ? visit.getAppointment().getGuestFullName() : "Khách vãng lai";
        String phone = visit.getCustomer()!=null ? visit.getCustomer().getPhone() : visit.getAppointment()!=null ? visit.getAppointment().getGuestPhone() : null;
        long waiting = current!=null && visit.getCheckInTime()!=null ? Math.max(0, Duration.between(visit.getCheckInTime(), LocalDateTime.now()).toMinutes()) : 0;
        String state = current!=null?current.status():finished?"COMPLETED":"UNASSIGNED";
        return new PatientJourneyResponse(visit.getVisitId(), "VIS-"+visit.getVisitId().toString().substring(0,8).toUpperCase(), name, phone,
                visit.getCustomer()==null, current!=null?current.serviceName():finished?"Đã hoàn thành":"Chưa có lộ trình",
                current!=null?current.roomName()+" ("+(current.roomCode()==null?"-":current.roomCode())+")":"-", state,
                next!=null?next.serviceName():"-", visit.getCheckInTime(), waiting, waiting>=60 || "UNASSIGNED".equals(state), steps);
    }
}
