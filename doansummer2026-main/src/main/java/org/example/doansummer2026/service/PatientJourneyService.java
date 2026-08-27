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
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public void activateNext(UUID visitId) {
        // Khoa visit de hai phong hoan thanh dong thoi khong mo hai buoc BLOCKED.
        visitRepo.findByIdForUpdate(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám"));
        if (hasActiveStep(visitId)) return;
        List<QueueTicket> visitQueues = queueRepo.findAllByVisit_VisitId(visitId);
        // SKIPPED duoc dung cho trang thai benh nhan vang sau khi goi. Day la
        // trang thai tam dung, khong phai tin hieu bo qua dich vu de mo buoc sau.
        if (visitQueues.stream().anyMatch(queue -> queue.getStatus() == QueueStatus.SKIPPED)) return;

        // Khi mot phong kham dang cho ket qua CLS, benh nhan chi duoc di tiep qua
        // cac phong CLS con lai. Khong mo phong kham thu hai cho den khi bac si
        // cua phong nguon da nhan ket qua va hoan thanh ket luan.
        boolean examinationWaitingForTest = visitQueues.stream().anyMatch(q ->
                q.getStatus() == QueueStatus.WAITING_FOR_TEST
                        && q.getDepartment() != null
                        && q.getDepartment().getDepartmentType()
                        == org.example.doansummer2026.enums.DepartmentType.EXAMINATION);

        QueueTicket queue = visitQueues.stream()
                .filter(q -> q.getStatus() == QueueStatus.BLOCKED)
                .filter(q -> !examinationWaitingForTest
                        || (q.getDepartment() != null
                        && q.getDepartment().getDepartmentType() != null
                        && q.getDepartment().getDepartmentType().isParaclinical()))
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
            publishQueueActivated(queue);
        } else if (test != null) { test.setStatus(TestRequestStatus.PENDING); testRepo.save(test); }
        else if (examinationWaitingForTest) {
            // Da lam het cac phong CLS nhung ket qua chua san sang: giu nguyen
            // cac phong kham tiep theo o BLOCKED de cho benh nhan quay lai dung
            // phong da chi dinh CLS.
            return;
        }
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

    private void publishQueueActivated(QueueTicket queue) {
        if (queue == null || queue.getDepartment() == null) return;
        UUID departmentId = queue.getDepartment().getDepartmentId();
        try {
            messagingTemplate.convertAndSend(
                    "/topic/department-" + departmentId + "-queue", "QUEUE_UPDATED");
            if (queue.getDepartment().getDepartmentType() != null
                    && queue.getDepartment().getDepartmentType().isParaclinical()) {
                messagingTemplate.convertAndSend(
                        "/topic/department-" + departmentId + "-lab-queue", "LAB_UPDATED");
            }
        } catch (Exception ignored) {
            // Loi realtime khong duoc rollback workflow kham.
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

    /**
     * Tra cuu hanh trinh cho khach vang lai bang hai thong tin tren phieu kham.
     * Bat buoc khop ca ma luot va so dien thoai de tranh lo thong tin benh nhan.
     */
    @Transactional(readOnly=true)
    public PatientJourneyResponse lookupGuest(String visitCode, String phone) {
        String normalizedCode = visitCode == null ? "" : visitCode.trim().toUpperCase(Locale.ROOT);
        String normalizedPhone = phone == null ? "" : phone.replaceAll("\\s+", "");
        if (!normalizedCode.matches("VIS-[0-9A-F]{8}") || normalizedPhone.isBlank()) {
            throw new ResourceNotFoundException("Không tìm thấy lượt khám phù hợp");
        }

        return visitRepo.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc(normalizedPhone).stream()
                .filter(visit -> normalizedCode.equals(toVisitCode(visit.getVisitId())))
                .findFirst()
                .map(this::build)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám phù hợp"));
    }

    /** Phuc hoi luot cu bi ket o BLOCKED theo dung quy tac dieu phoi hien tai. */
    public PatientJourneyResponse advanceBlockedStep(UUID visitId) {
        visitRepo.findById(visitId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám"));
        List<QueueTicket> queues = queueRepo.findAllByVisit_VisitId(visitId);
        if (queues.stream().anyMatch(queue -> queue.getStatus() == QueueStatus.SKIPPED)) {
            throw new org.example.doansummer2026.exception.ConflictException(
                    "Bệnh nhân đang được đánh dấu vắng; hãy đưa bệnh nhân quay lại hàng chờ trước khi mở bước tiếp theo");
        }
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
        var absent = steps.stream().filter(s -> s.status().equals("SKIPPED")).findFirst().orElse(null);
        // WAITING_FOR_TEST cua phong kham chi la buoc tam treo. Neu benh nhan
        // dang cho/goi/thuc hien tai phong CLS thi phong vat ly do moi la vi tri
        // hien tai. Khi da roi het cac phong, hien buoc cho ket qua; TEST_DONE
        // moi dua benh nhan quay lai phong kham goc.
        var physicalCurrent = steps.stream()
                .filter(s -> List.of("IN_PROGRESS", "CALLED", "WAITING").contains(s.status()))
                .min(Comparator
                        .comparingInt((PatientJourneyResponse.Step s) -> switch (s.status()) {
                            case "IN_PROGRESS" -> 0;
                            case "CALLED" -> 1;
                            default -> 2;
                        })
                        .thenComparing(PatientJourneyResponse.Step::startedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        var readyToReturn = steps.stream().filter(s -> s.status().equals("TEST_DONE")).findFirst().orElse(null);
        var resultPending = steps.stream().filter(s -> s.status().equals("RESULT_PENDING")).findFirst().orElse(null);
        var suspendedExamination = steps.stream().filter(s -> s.status().equals("WAITING_FOR_TEST")).findFirst().orElse(null);
        var otherActive = steps.stream()
                .filter(s -> !List.of("PAYMENT_PENDING", "BLOCKED", "DONE", "COMPLETED", "SKIPPED", "CANCELLED",
                        "IN_PROGRESS", "CALLED", "WAITING", "TEST_DONE", "RESULT_PENDING", "WAITING_FOR_TEST")
                        .contains(s.status()))
                .findFirst().orElse(null);
        var current = payment != null ? payment
                : absent != null ? absent
                : physicalCurrent != null ? physicalCurrent
                : readyToReturn != null ? readyToReturn
                : resultPending != null ? resultPending
                : suspendedExamination != null ? suspendedExamination
                : otherActive;
        var next = steps.stream().filter(s -> s.status().equals("BLOCKED")).findFirst().orElse(null);
        boolean finished = current==null && next==null && !steps.isEmpty();
        String name = visit.getCustomer()!=null ? visit.getCustomer().getFullName() : visit.getAppointment()!=null ? visit.getAppointment().getGuestFullName() : "Khách vãng lai";
        String phone = visit.getCustomer()!=null ? visit.getCustomer().getPhone() : visit.getAppointment()!=null ? visit.getAppointment().getGuestPhone() : null;
        long waiting = current!=null && visit.getCheckInTime()!=null ? Math.max(0, Duration.between(visit.getCheckInTime(), LocalDateTime.now()).toMinutes()) : 0;
        String state = current!=null?current.status():finished?"COMPLETED":"UNASSIGNED";
        boolean guest = visit.getCustomer() == null || visit.getCustomer().getAccount() == null;
        return new PatientJourneyResponse(visit.getVisitId(), toVisitCode(visit.getVisitId()), name, phone,
                guest, current!=null?current.serviceName():finished?"Đã hoàn thành":"Chưa có lộ trình",
                current!=null && current.roomName()!=null
                        ? current.roomName()+" ("+(current.roomCode()==null?"-":current.roomCode())+")" : "-", state,
                next!=null?next.serviceName():"-", visit.getCheckInTime(), waiting, waiting>=60 || "UNASSIGNED".equals(state), steps);
    }

    private String toVisitCode(UUID visitId) {
        return "VIS-" + visitId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
