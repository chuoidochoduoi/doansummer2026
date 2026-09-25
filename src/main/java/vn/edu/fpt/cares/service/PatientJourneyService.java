package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.dto.journey.PatientJourneyResponse;
import vn.edu.fpt.cares.dto.journey.PatientQueueResponse;
import vn.edu.fpt.cares.enums.QueueStatus;
import vn.edu.fpt.cares.enums.TestRequestStatus;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import vn.edu.fpt.cares.common.PageResponse;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class PatientJourneyService {
    private final CustomerVisitRepository visitRepo;
    private final QueueTicketRepository queueRepo;
    private final TestRequestRepository testRepo;
    private final InvoiceRepository invoiceRepo;
    private final MedicalRecordRepository recordRepo;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final QueuePriorityService queuePriorityService;

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
        QueueTicket suspendedExamination = visitQueues.stream().filter(q ->
                        q.getStatus() == QueueStatus.WAITING_FOR_TEST
                                && q.getDepartment() != null
                                && q.getDepartment().getDepartmentType()
                                == vn.edu.fpt.cares.enums.DepartmentType.EXAMINATION)
                .findFirst().orElse(null);
        boolean examinationWaitingForTest = suspendedExamination != null;

        // Neu bac si vua chi dinh CLS, uu tien cac phong co yeu cau gan voi
        // chinh benh an dang tam treo. CLS dat san doc lap van duoc lam sau do,
        // nhung khong duoc chen truoc yeu cau dang quyet dinh viec quay lai bac si.
        java.util.Set<UUID> linkedQueueIds = suspendedExamination == null
                ? java.util.Set.of()
                : testRepo.findAllByMedicalRecord_Visit_VisitId(visitId).stream()
                .filter(test -> test.getQueueTicket() != null
                        && test.getMedicalRecord() != null
                        && test.getMedicalRecord().getQueueTicket() != null
                        && suspendedExamination.getTicketId().equals(
                        test.getMedicalRecord().getQueueTicket().getTicketId()))
                .map(test -> test.getQueueTicket().getTicketId())
                .collect(java.util.stream.Collectors.toSet());
        Comparator<QueueTicket> nextStepOrder = Comparator
                .comparingInt((QueueTicket candidate) -> linkedQueueIds.contains(candidate.getTicketId()) ? 0 : 1)
                .thenComparing(workflowOrder());

        QueueTicket queue = visitQueues.stream()
                .filter(q -> q.getStatus() == QueueStatus.BLOCKED)
                .filter(q -> !examinationWaitingForTest
                        || (q.getDepartment() != null
                        && q.getDepartment().getDepartmentType() != null
                        && q.getDepartment().getDepartmentType().isParaclinical()))
                .min(nextStepOrder).orElse(null);
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
                visit.setStatus(vn.edu.fpt.cares.enums.VisitStatus.COMPLETED);
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
            messagingTemplate.convertAndSend("/topic/queue-display", "QUEUE_UPDATED");
            if (queue.getDepartment().getDepartmentType() != null
                    && queue.getDepartment().getDepartmentType().isParaclinical()) {
                messagingTemplate.convertAndSend(
                        "/topic/department-" + departmentId + "-lab-queue", "LAB_UPDATED");
            }
        } catch (Exception ignored) {
            // Loi realtime khong duoc rollback workflow kham.
        }
    }

    /**
     * Thu tu on dinh cua mot luot: kham benh truoc CLS, sau do uu tien
     * workflowPriority cao hon va cuoi cung la ma dich vu/thoi diem tao.
     */
    private Comparator<QueueTicket> workflowOrder() {
        return Comparator
                .comparingInt((QueueTicket queue) -> queue.getDepartment() != null
                        && queue.getDepartment().getDepartmentType()
                        == vn.edu.fpt.cares.enums.DepartmentType.EXAMINATION ? 0 : 1)
                .thenComparing((QueueTicket queue) -> queue.getService() != null
                                && queue.getService().getWorkflowPriority() != null
                                ? queue.getService().getWorkflowPriority() : 0,
                        Comparator.reverseOrder())
                .thenComparing(queue -> queue.getService() != null
                                ? queue.getService().getServiceCode() : null,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(QueueTicket::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()));
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
        return list(search, status, null, pageable);
    }

    @Transactional(readOnly=true)
    public PageResponse<PatientJourneyResponse> list(String search, String status, String scope, Pageable pageable) {
        String needle = search==null?"":search.trim().toLowerCase();
        String normalizedScope = normalizeScope(scope);
        List<PatientJourneyResponse> filtered = visitRepo.findAll().stream().map(this::build)
                .filter(journey -> matchesScope(journey, normalizedScope))
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

    private String normalizeScope(String scope) {
        String normalized = scope == null || scope.isBlank()
                ? "ALL" : scope.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("TODAY", "OVERDUE", "ALL").contains(normalized)) {
            throw new vn.edu.fpt.cares.exception.BadRequestException(
                    "Phạm vi hành trình chỉ hỗ trợ TODAY, OVERDUE hoặc ALL");
        }
        return normalized;
    }

    /**
     * Trong 5 phut chot ca sau nua dem, luot cua ngay hom truoc van thuoc
     * ngay van hanh hien tai. Sau 00:05, cac luot chua ket thuc moi duoc
     * dua sang danh sach ton dong.
     */
    private boolean matchesScope(PatientJourneyResponse journey, String scope) {
        if ("ALL".equals(scope)) return true;
        if (journey.checkInTime() == null) return "OVERDUE".equals(scope)
                && !isTerminalJourney(journey.currentStatus());

        ZoneId clinicZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(clinicZone);
        LocalTime now = LocalTime.now(clinicZone);
        LocalDate activeFrom = now.isBefore(LocalTime.of(0, 5)) ? today.minusDays(1) : today;
        LocalDate checkInDate = journey.checkInTime().toLocalDate();
        if ("TODAY".equals(scope)) {
            return !checkInDate.isBefore(activeFrom) && !checkInDate.isAfter(today);
        }
        return checkInDate.isBefore(activeFrom) && !isTerminalJourney(journey.currentStatus());
    }

    private boolean isTerminalJourney(String status) {
        return Set.of("DONE", "COMPLETED", "SKIPPED", "CANCELLED").contains(status);
    }

    @Transactional(readOnly=true)
    public PatientJourneyResponse get(UUID id) { return build(visitRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám"))); }

    /**
     * Tra cuu hanh trinh cho khach vang lai bang hai thong tin tren phieu kham.
     * Bat buoc khop ca ma luot va so dien thoai de tranh lo thong tin benh nhan.
     */
    @Transactional(readOnly=true)
    public PatientJourneyResponse lookupGuest(String visitCode, String phone) {
        return build(resolveGuestVisit(visitCode, phone));
    }

    @Transactional(readOnly = true)
    public PatientQueueResponse lookupGuestQueue(String visitCode, String phone) {
        return buildPatientQueue(resolveGuestVisit(visitCode, phone));
    }

    private CustomerVisit resolveGuestVisit(String visitCode, String phone) {
        String normalizedCode = visitCode == null ? "" : visitCode.trim().toUpperCase(Locale.ROOT);
        String normalizedPhone = phone == null ? "" : phone.replaceAll("\\s+", "");
        if (!normalizedCode.matches("VIS-[0-9A-F]{8}") || normalizedPhone.isBlank()) {
            throw new ResourceNotFoundException("Không tìm thấy lượt khám phù hợp");
        }

        return visitRepo.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc(normalizedPhone).stream()
                .filter(visit -> normalizedCode.equals(toVisitCode(visit.getVisitId())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám phù hợp"));
    }

    /** Phuc hoi luot cu bi ket o BLOCKED theo dung quy tac dieu phoi hien tai. */
    public PatientJourneyResponse advanceBlockedStep(UUID visitId) {
        visitRepo.findById(visitId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám"));
        List<QueueTicket> queues = queueRepo.findAllByVisit_VisitId(visitId);
        if (queues.stream().anyMatch(queue -> queue.getStatus() == QueueStatus.SKIPPED)) {
            throw new vn.edu.fpt.cares.exception.ConflictException(
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

    @Transactional(readOnly = true)
    public PatientQueueResponse queueForCustomer(UUID visitId, Collection<UUID> readableProfileIds) {
        CustomerVisit visit = visitRepo.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám"));
        // Check before reading any room or journey data; archived family profiles remain readable.
        if (visit.getCustomer() == null || !readableProfileIds.contains(visit.getCustomer().getProfileId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xem lượt khám này");
        }
        return buildPatientQueue(visit);
    }

    private PatientQueueResponse buildPatientQueue(CustomerVisit visit) {
        UUID visitId = visit.getVisitId();
        QueueTicket skipped = queueRepo.findAllByVisit_VisitId(visitId).stream()
                .filter(ticket -> ticket.getStatus() == QueueStatus.SKIPPED)
                .min(Comparator.comparing(QueueTicket::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        if (skipped != null) return skippedQueue(visitId, skipped);
        if (visit.getStatus() == vn.edu.fpt.cares.enums.VisitStatus.CANCELLED
                || visit.getStatus() == vn.edu.fpt.cares.enums.VisitStatus.COMPLETED) {
            return emptyQueue(visitId, visit.getStatus().name());
        }
        PatientJourneyResponse journey = build(visit, false);
        if (!List.of("WAITING", "CALLED", "IN_PROGRESS", "TEST_DONE").contains(journey.currentStatus())) {
            return emptyQueue(visitId, journey.currentStatus());
        }
        PatientJourneyResponse.Step step = physicalCurrent(journey.steps());
        if (step == null) {
            step = journey.steps().stream().filter(s -> "TEST_DONE".equals(s.status())).findFirst().orElse(null);
        }
        // Legacy tests without a queue ticket do not have a calculable position.
        if (step == null || step.queueTicketId() == null) {
            return emptyQueue(visitId, journey.currentStatus());
        }
        UUID ticketId = step.queueTicketId();
        QueueTicket own = queueRepo.findAllByVisit_VisitId(visitId).stream()
                .filter(q -> ticketId.equals(q.getTicketId())).findFirst().orElse(null);
        if (own == null || own.getDepartment() == null || own.getWorkDate() == null) {
            return emptyQueue(visitId, journey.currentStatus());
        }
        // Dung cung bo xep hang voi bac si/man TV de vi tri cua benh nhan khong bi lech.
        var tickets = queueRepo.findWaitingPrioritized(own.getDepartment().getDepartmentId(), own.getWorkDate(),
                        List.of(QueueStatus.WAITING, QueueStatus.TEST_DONE, QueueStatus.CALLED,
                                QueueStatus.IN_PROGRESS, QueueStatus.WAITING_FOR_TEST),
                        Pageable.unpaged()).getContent();
        List<QueuePriorityService.RankedTicket> ranked = queuePriorityService.rank(tickets);
        List<PatientQueueResponse.Entry> serving = new ArrayList<>();
        List<PatientQueueResponse.Entry> waiting = new ArrayList<>();
        Integer position = null;
        QueuePriorityService.RankedTicket ownRank = null;
        for (QueuePriorityService.RankedTicket item : ranked) {
            QueueTicket ticket = item.ticket();
            if (ticket.getVisit().getStatus() == vn.edu.fpt.cares.enums.VisitStatus.CANCELLED
                    || ticket.getVisit().getStatus() == vn.edu.fpt.cares.enums.VisitStatus.COMPLETED) continue;
            boolean self = ticketId.equals(ticket.getTicketId());
            if (self) ownRank = item;
            if (ticket.getStatus() == QueueStatus.IN_PROGRESS || ticket.getStatus() == QueueStatus.CALLED) {
                serving.add(new PatientQueueResponse.Entry(null, self, ticket.getStatus().name()));
            } else if (item.waitingPosition() != null) {
                waiting.add(new PatientQueueResponse.Entry(item.waitingPosition(), self,
                        self ? ticket.getStatus().name() : QueueStatus.WAITING.name()));
                if (self) position = item.waitingPosition();
            }
        }
        QueuePriorityService.PriorityInfo priority = ownRank != null ? ownRank.priority()
                : new QueuePriorityService.PriorityInfo(QueuePriorityService.REGULAR,
                "Khách trực tiếp", null, false);
        return new PatientQueueResponse(visitId, own.getDepartment().getName(), own.getDepartment().getRoomCode(),
                own.getWorkDate(), journey.currentStatus(), position, position == null ? null : position - 1,
                own.getQueueNumber(), priority.category(), priority.label(), priority.appointmentScheduledAt(),
                false, QueuePriorityService.RETURNED_AFTER_ABSENCE.equals(priority.category()) ? "CONFIRMED" : "NONE", null,
                serving, waiting);
    }

    private PatientQueueResponse emptyQueue(UUID visitId, String status) {
        return new PatientQueueResponse(visitId, null, null, null, status, null, null,
                null, null, null, null, false, "NONE", null, List.of(), List.of());
    }

    private PatientQueueResponse skippedQueue(UUID visitId, QueueTicket ticket) {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        boolean expired = ticket.getWorkDate() == null || ticket.getWorkDate().isBefore(today);
        return new PatientQueueResponse(visitId,
                ticket.getDepartment() != null ? ticket.getDepartment().getName() : null,
                ticket.getDepartment() != null ? ticket.getDepartment().getRoomCode() : null,
                ticket.getWorkDate(), QueueStatus.SKIPPED.name(), null, null,
                ticket.getQueueNumber(), null, null, null,
                false, expired ? "EXPIRED" : "NONE", null, List.of(), List.of());
    }

    private PatientJourneyResponse.Step physicalCurrent(List<PatientJourneyResponse.Step> steps) {
        return steps.stream()
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
    }

    private PatientJourneyResponse build(CustomerVisit visit) {
        return build(visit, true);
    }

    private PatientJourneyResponse build(CustomerVisit visit, boolean includeQueuePriority) {
        List<PatientJourneyResponse.Step> steps = new ArrayList<>();
        List<Invoice> invoices = invoiceRepo.findAllByVisit_VisitId(visit.getVisitId()).stream()
                .sorted(Comparator.comparing(Invoice::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        invoices.stream().filter(invoice -> invoice.getMedicalRecord() == null)
                .forEach(invoice -> steps.add(paymentStep(invoice, "Thanh toán dịch vụ ban đầu",
                        "INITIAL_PAYMENT", null)));

        var journeyTests = testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId());
        var testsByQueue = journeyTests.stream().filter(test -> test.getQueueTicket() != null)
                .collect(java.util.stream.Collectors.groupingBy(test -> test.getQueueTicket().getTicketId()));
        var visitQueues = queueRepo.findAllByVisit_VisitId(visit.getVisitId());
        var examinationQueues = visitQueues.stream()
                .filter(this::isExaminationQueue)
                .sorted(workflowOrder())
                .toList();
        var paraclinicalQueues = visitQueues.stream()
                .filter(queue -> !isExaminationQueue(queue))
                .sorted(workflowOrder())
                .toList();
        java.util.Set<UUID> addedQueueIds = new java.util.HashSet<>();
        // A shared Lab call can include tests from another examination. Keep this
        // visit-wide so the fallback path does not render those tests a second time.
        Set<UUID> includedTestIds = new HashSet<>();

        // Xay dung dung thu tu nghiep vu, khong chi sap theo createdAt. CLS da
        // gan MedicalRecord nam ngay sau phong kham nguon, sau do la buoc quay
        // lai phong do; cac CLS doc lap nam sau tat ca dich vu kham.
        java.util.Map<String, java.util.List<QueueTicket>> examinationGroups = examinationQueues.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        queue -> (queue.getDepartment() == null ? "NO_ROOM" : queue.getDepartment().getDepartmentId())
                                + "|" + queue.getWorkDate(),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));

        for (java.util.List<QueueTicket> examinationGroup : examinationGroups.values()) {
            int cycleNumber = 0;
            // Services may share a physical room, but they remain separate examinations and
            // separate medical records.  A later service is only reached after the previous
            // record has really been completed (including any return after ordered tests).
            for (QueueTicket examination : examinationGroup) {
                steps.add(examinationGroupStep(List.of(examination), examination,
                        hasClinicalCycle(examination, invoices, journeyTests)));
                addedQueueIds.add(examination.getTicketId());

                List<CycleInvoice> examinationCycles = invoices.stream()
                        .filter(invoice -> belongsToExamination(invoice, examination))
                        .map(invoice -> new CycleInvoice(invoice, examination))
                        .sorted(Comparator.comparing(cycle -> cycle.invoice().getCreatedAt(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();
                Set<UUID> examinationInvoiceIds = examinationCycles.stream()
                        .map(cycle -> cycle.invoice().getInvoiceId())
                        .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());

                for (int cycleIndex = 0; cycleIndex < examinationCycles.size(); cycleIndex++) {
                    CycleInvoice cycle = examinationCycles.get(cycleIndex);
                    Invoice invoice = cycle.invoice();
                    cycleNumber++;
                steps.add(paymentStep(invoice,
                        "Thanh toán chỉ định cận lâm sàng lần " + cycleNumber,
                        "ORDER_PAYMENT", cycleNumber));
                if (invoice.getStatus() == vn.edu.fpt.cares.enums.InvoiceStatus.CANCELLED) {
                    continue;
                }
                List<TestRequest> directlyLinkedTests = journeyTests.stream()
                        .filter(test -> belongsToExamination(test, examination))
                        .filter(test -> invoiceIdOf(test)
                                .map(invoice.getInvoiceId()::equals).orElse(false))
                        .toList();
                Set<UUID> sharedQueueIds = directlyLinkedTests.stream()
                        .map(TestRequest::getQueueTicket).filter(Objects::nonNull)
                        .map(QueueTicket::getTicketId).collect(java.util.stream.Collectors.toSet());
                // Mot luot goi CLS co the gom yeu cau cua nhieu benh an cung
                // phong. Khi da hien thi luot goi nay trong mot chu ky, gom tat
                // ca dich vu cua chinh QueueTicket de khong sinh them mot vong
                // "quay lai bac si" gia o phia sau.
                List<TestRequest> cycleTests = journeyTests.stream()
                        .filter(test -> directlyLinkedTests.contains(test)
                                || test.getQueueTicket() != null
                                && sharedQueueIds.contains(test.getQueueTicket().getTicketId()))
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
                // CLS da mua tu dau khong nen bi day xuong sau cac dich vu kham
                // khac khi bac si vua chi dinh CLS. Gan chung chung vao dot chi
                // dinh dau tien, nhung khong sua lien ket benh an/hoa don lich su.
                carriedPrebookedTests(visit, invoice, invoices, journeyTests)
                        .forEach(test -> {
                            if (!cycleTests.contains(test)) cycleTests.add(test);
                        });
                cycleTests.forEach(test -> includedTestIds.add(test.getTestRequestId()));
                addCycleParaclinicalSteps(steps, invoice, examination, cycleNumber,
                        cycleTests, addedQueueIds);
                boolean hasLaterActiveCycle = examinationCycles.subList(cycleIndex + 1, examinationCycles.size()).stream()
                        .anyMatch(next -> next.invoice().getStatus()
                                != vn.edu.fpt.cares.enums.InvoiceStatus.CANCELLED);
                boolean hasFallbackTests = journeyTests.stream()
                        .filter(test -> belongsToExamination(test, examination))
                        .anyMatch(test -> !includedTestIds.contains(test.getTestRequestId())
                                && invoiceIdOf(test)
                                .map(id -> !examinationInvoiceIds.contains(id)).orElse(true));
                steps.add(returnStep(examination, invoice, cycleNumber, cycleTests,
                        !hasLaterActiveCycle && !hasFallbackTests));
                }

                List<TestRequest> legacyTests = journeyTests.stream()
                        .filter(test -> belongsToExamination(test, examination))
                        .filter(test -> !includedTestIds.contains(test.getTestRequestId()))
                        .sorted(Comparator.comparing(TestRequest::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
                // Du lieu da tao truoc ban sua co the moi chi gan mot dich vu
                // tra truoc vao benh an. Neu cac dich vu con lai cung thuoc hoa
                // don ban dau da thanh toan, hien chung trong cung dot CLS dau
                // ma khong thay doi cac lien ket lich su trong database.
                carriedPrebookedTestsForLegacyCycle(legacyTests, invoices, journeyTests)
                        .forEach(test -> {
                            if (!legacyTests.contains(test)) legacyTests.add(test);
                        });
                if (!legacyTests.isEmpty()) {
                    legacyTests.forEach(test -> includedTestIds.add(test.getTestRequestId()));
                    cycleNumber++;
                    addLegacyParaclinicalSteps(steps, examination, cycleNumber,
                            legacyTests, addedQueueIds);
                    steps.add(returnStep(examination, null, cycleNumber, legacyTests, true));
                }
            }
        }
        for (QueueTicket labQueue : paraclinicalQueues) {
            if (addedQueueIds.add(labQueue.getTicketId())) {
                steps.add(queueStep(labQueue, testsByQueue.get(labQueue.getTicketId()),
                        "PARACLINICAL", labQueue.getStatus().name()));
            }
        }
        Map<String, List<TestRequest>> standaloneTests = journeyTests.stream()
                .filter(test -> test.getQueueTicket() == null)
                .filter(test -> test.getMedicalRecord() == null
                        || test.getMedicalRecord().getQueueTicket() == null)
                .collect(java.util.stream.Collectors.groupingBy(this::standaloneTestGroupKey,
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        standaloneTests.forEach((key, group) -> steps.add(groupedQueueLessStep(
                "TEST-GROUP:" + key, group, null, null,
                invoiceIdOf(group.get(0)).orElse(null))));

        boolean waitingForResults = journeyTests.stream().anyMatch(test ->
                test.getStatus() == TestRequestStatus.PENDING || test.getStatus() == TestRequestStatus.IN_PROGRESS)
                && queueRepo.findAllByVisit_VisitId(visit.getVisitId()).stream().noneMatch(queue ->
                queue.getStatus() == QueueStatus.WAITING || queue.getStatus() == QueueStatus.CALLED
                        || queue.getStatus() == QueueStatus.IN_PROGRESS);
        if (waitingForResults) {
            steps.add(new PatientJourneyResponse.Step(
                    "RESULTS:" + visit.getVisitId(), "RESULT", "Đang chờ kết quả cận lâm sàng",
                    null, null, null, "RESULT_PENDING", LocalDateTime.now(), null, List.of(), 0, 0,
                    "RESULT", null, null, null));
        }

        var payment = steps.stream().filter(s -> s.status().equals("PAYMENT_PENDING")).findFirst().orElse(null);
        // WAITING_FOR_TEST cua phong kham chi la buoc tam treo. Neu benh nhan
        // dang cho/goi/thuc hien tai phong CLS thi phong vat ly do moi la vi tri
        // hien tai. Khi da roi het cac phong, hien buoc cho ket qua; TEST_DONE
        // moi dua benh nhan quay lai phong kham goc.
        var physicalCurrent = physicalCurrent(steps);
        var readyToReturn = steps.stream().filter(s -> s.status().equals("TEST_DONE")).findFirst().orElse(null);
        var resultPending = steps.stream().filter(s -> s.status().equals("RESULT_PENDING")).findFirst().orElse(null);
        var otherActive = steps.stream()
                .filter(s -> !List.of("PAYMENT_PENDING", "BLOCKED", "DONE", "COMPLETED", "CANCELLED",
                        "IN_PROGRESS", "CALLED", "WAITING", "TEST_DONE", "RESULT_PENDING", "WAITING_FOR_TEST")
                        .contains(s.status()))
                .findFirst().orElse(null);
        var firstUnresolved = steps.stream()
                .filter(s -> !List.of("DONE", "COMPLETED", "CANCELLED").contains(s.status()))
                .filter(s -> !"WAITING_FOR_TEST".equals(s.status()))
                .findFirst().orElse(null);
        boolean visitClosed = visit.getStatus() == vn.edu.fpt.cares.enums.VisitStatus.CANCELLED
                || visit.getStatus() == vn.edu.fpt.cares.enums.VisitStatus.COMPLETED;
        boolean hasSkippedService = visitQueues.stream()
                .anyMatch(queue -> queue.getStatus() == QueueStatus.SKIPPED);
        var current = visitClosed ? null : payment != null ? payment
                : physicalCurrent != null ? physicalCurrent
                : readyToReturn != null ? readyToReturn
                : resultPending != null ? resultPending
                : firstUnresolved != null ? firstUnresolved
                : otherActive;
        int currentIndex = current == null ? -1 : steps.indexOf(current);
        var next = visitClosed ? null : java.util.stream.IntStream.range(Math.max(0, currentIndex + 1), steps.size())
                .mapToObj(steps::get).filter(step -> step.status().equals("BLOCKED"))
                .findFirst().orElseGet(() -> current == null ? steps.stream()
                        .filter(step -> step.status().equals("BLOCKED")).findFirst().orElse(null) : null);
        boolean finished = current==null && next==null && !steps.isEmpty();
        String name = visit.getCustomer()!=null ? visit.getCustomer().getFullName() : visit.getAppointment()!=null ? visit.getAppointment().getGuestFullName() : "Khách vãng lai";
        String phone = visit.getCustomer()!=null ? visit.getCustomer().getPhone() : visit.getAppointment()!=null ? visit.getAppointment().getGuestPhone() : null;
        long waiting = current!=null && visit.getCheckInTime()!=null
                ? Math.max(0, Duration.between(visit.getCheckInTime(),
                        LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))).toMinutes()) : 0;
        String state = visitClosed ? visit.getStatus().name()
                : current!=null?current.status():finished?"COMPLETED":"UNASSIGNED";
        boolean guest = visit.getCustomer() == null || visit.getCustomer().getAccount() == null;
        QueueTicket currentTicket = currentQueue(current, visitQueues).orElse(null);
        MedicalRecord responsibleRecord = Optional.ofNullable(currentTicket)
                .flatMap(queue -> recordRepo.findByQueueTicket_TicketId(queue.getTicketId()))
                .orElse(null);
        StaffInfo responsibleDoctor = responsibleRecord != null ? responsibleRecord.getDoctor() : null;
        UUID responsibleDoctorId = responsibleDoctor != null ? responsibleDoctor.getStaffId() : null;
        String responsibleDoctorName = responsibleDoctor != null && responsibleDoctor.getProfile() != null
                ? responsibleDoctor.getProfile().getFullName() : null;
        QueuePriorityService.RankedTicket currentRank = includeQueuePriority
                ? rankCurrentTicket(currentTicket) : null;
        String closedLabel = hasSkippedService ? "Lượt khám trong ngày đã kết thúc"
                : visit.getStatus() == vn.edu.fpt.cares.enums.VisitStatus.COMPLETED
                ? "Đã hoàn thành" : "Lượt khám đã hủy";
        return new PatientJourneyResponse(visit.getVisitId(), toVisitCode(visit.getVisitId()), name, phone,
                guest, current!=null?current.serviceName():visitClosed?closedLabel:finished?"Đã hoàn thành":"Chưa có lộ trình",
                current!=null && current.roomName()!=null
                        ? current.roomName()+" ("+(current.roomCode()==null?"-":current.roomCode())+")" : "-", state,
                next!=null?next.serviceName():"-", responsibleDoctorId, responsibleDoctorName,
                visit.getCheckInTime(), waiting, waiting>=60 || "UNASSIGNED".equals(state), steps,
                current != null ? current.id() : null,
                currentTicket != null ? currentTicket.getQueueNumber() : null,
                currentRank != null ? currentRank.waitingPosition() : null,
                currentRank != null ? currentRank.priority().category() : null,
                currentRank != null ? currentRank.priority().label() : null,
                currentRank != null ? currentRank.priority().appointmentScheduledAt() : null);
    }

    private QueuePriorityService.RankedTicket rankCurrentTicket(QueueTicket currentTicket) {
        if (currentTicket == null || currentTicket.getDepartment() == null
                || currentTicket.getWorkDate() == null) return null;
        var page = queueRepo.findWaitingPrioritized(
                currentTicket.getDepartment().getDepartmentId(), currentTicket.getWorkDate(),
                List.of(QueueStatus.WAITING, QueueStatus.TEST_DONE, QueueStatus.CALLED,
                        QueueStatus.IN_PROGRESS, QueueStatus.WAITING_FOR_TEST), Pageable.unpaged());
        if (page == null || queuePriorityService == null) return null;
        List<QueuePriorityService.RankedTicket> ranked = queuePriorityService.rank(page.getContent());
        if (ranked == null) return null;
        return ranked.stream().filter(item -> currentTicket.getTicketId().equals(item.ticket().getTicketId()))
                .findFirst().orElse(null);
    }

    private PatientJourneyResponse.Step paymentStep(Invoice invoice, String name,
                                                     String phase, Integer cycleNumber) {
        String status = switch (invoice.getStatus()) {
            case PAID -> QueueStatus.DONE.name();
            case CANCELLED -> "CANCELLED";
            default -> "PAYMENT_PENDING";
        };
        return new PatientJourneyResponse.Step(
                "PAYMENT:" + invoice.getInvoiceId(), "PAYMENT", name,
                "Quầy thu ngân", null, null, status, invoice.getCreatedAt(),
                invoice.getStatus() == vn.edu.fpt.cares.enums.InvoiceStatus.PENDING
                        ? null : invoice.getUpdatedAt(),
                List.of(), 0, status.equals(QueueStatus.DONE.name()) ? 1 : 0,
                phase, cycleNumber, null, invoice.getInvoiceId());
    }

    private record CycleInvoice(Invoice invoice, QueueTicket examination) {}

    private boolean belongsToExamination(Invoice invoice, QueueTicket examination) {
        return invoice.getMedicalRecord() != null
                && invoice.getMedicalRecord().getQueueTicket() != null
                && examination.getTicketId().equals(
                invoice.getMedicalRecord().getQueueTicket().getTicketId());
    }

    private boolean belongsToExamination(TestRequest test, QueueTicket examination) {
        return test.getMedicalRecord() != null
                && test.getMedicalRecord().getQueueTicket() != null
                && examination.getTicketId().equals(
                test.getMedicalRecord().getQueueTicket().getTicketId());
    }

    /** CLS da thanh toan tu hoa don ban dau, chua ket thuc truoc luc co chi dinh.
     * Chi dot chi dinh CLS dau tien cua ca luot duoc nhan phan CLS dat truoc nay. */
    private List<TestRequest> carriedPrebookedTests(CustomerVisit visit, Invoice clinicalInvoice,
                                                    List<Invoice> invoices, List<TestRequest> tests) {
        if (!isFirstClinicalInvoice(clinicalInvoice, invoices)) return List.of();
        LocalDateTime orderedAt = clinicalInvoice.getCreatedAt();
        Map<UUID, Invoice> invoiceById = invoices.stream().filter(invoice -> invoice.getInvoiceId() != null)
                .collect(java.util.stream.Collectors.toMap(Invoice::getInvoiceId, invoice -> invoice));
        return tests.stream().filter(test -> {
            if (test.getStatus() == TestRequestStatus.CANCELLED
                    || test.getMedicalRecord() == null || test.getMedicalRecord().getQueueTicket() != null) return false;
            Invoice sourceInvoice = invoiceIdOf(test).map(invoiceById::get).orElse(null);
            if (sourceInvoice == null || sourceInvoice.getMedicalRecord() != null
                    || sourceInvoice.getStatus() != vn.edu.fpt.cares.enums.InvoiceStatus.PAID) return false;
            if (orderedAt != null && test.getCreatedAt() != null && test.getCreatedAt().isAfter(orderedAt)) return false;
            return test.getCompletedAt() == null || orderedAt == null || !test.getCompletedAt().isBefore(orderedAt);
        }).toList();
    }

    private boolean isFirstClinicalInvoice(Invoice candidate, List<Invoice> invoices) {
        return invoices.stream().filter(invoice -> invoice.getStatus()
                        != vn.edu.fpt.cares.enums.InvoiceStatus.CANCELLED)
                .filter(invoice -> invoice.getMedicalRecord() != null
                        && invoice.getMedicalRecord().getQueueTicket() != null)
                .min(Comparator.comparing(Invoice::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(first -> Objects.equals(first.getInvoiceId(), candidate.getInvoiceId())).orElse(false);
    }

    private List<TestRequest> carriedPrebookedTestsForLegacyCycle(List<TestRequest> linkedTests,
                                                                  List<Invoice> invoices,
                                                                  List<TestRequest> allTests) {
        if (linkedTests.isEmpty()) return List.of();
        Map<UUID, Invoice> invoiceById = invoices.stream()
                .filter(invoice -> invoice.getInvoiceId() != null)
                .collect(java.util.stream.Collectors.toMap(Invoice::getInvoiceId, invoice -> invoice));
        Set<UUID> paidInitialInvoiceIds = linkedTests.stream().map(this::invoiceIdOf)
                .flatMap(Optional::stream)
                .filter(id -> {
                    Invoice invoice = invoiceById.get(id);
                    return invoice != null && invoice.getMedicalRecord() == null
                            && invoice.getStatus() == vn.edu.fpt.cares.enums.InvoiceStatus.PAID;
                }).collect(java.util.stream.Collectors.toSet());
        if (paidInitialInvoiceIds.isEmpty()) return List.of();
        return allTests.stream()
                .filter(test -> test.getStatus() != TestRequestStatus.CANCELLED
                        && test.getStatus() != TestRequestStatus.COMPLETED)
                .filter(test -> test.getMedicalRecord() != null
                        && test.getMedicalRecord().getQueueTicket() == null)
                .filter(test -> invoiceIdOf(test).map(paidInitialInvoiceIds::contains).orElse(false))
                .toList();
    }

    /** True while a doctor must still wait for either ordered CLS or paid CLS
     * carried from the initial invoice into that first clinical cycle. */
    public boolean hasOutstandingTestsForExamination(UUID examinationTicketId) {
        MedicalRecord record = recordRepo.findByQueueTicket_TicketId(examinationTicketId).orElse(null);
        if (record == null || record.getVisit() == null) return false;
        List<Invoice> invoices = invoiceRepo.findAllByVisit_VisitId(record.getVisit().getVisitId());
        Invoice cycleInvoice = invoices.stream().filter(invoice -> belongsToExamination(invoice,
                        record.getQueueTicket()))
                .filter(invoice -> invoice.getStatus() != vn.edu.fpt.cares.enums.InvoiceStatus.CANCELLED)
                .min(Comparator.comparing(Invoice::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))).orElse(null);
        List<TestRequest> tests = testRepo.findAllByMedicalRecord_Visit_VisitId(record.getVisit().getVisitId());
        List<TestRequest> batch = new ArrayList<>(tests.stream()
                .filter(test -> belongsToExamination(test, record.getQueueTicket())).toList());
        if (cycleInvoice != null) carriedPrebookedTests(record.getVisit(), cycleInvoice, invoices, tests)
                .forEach(test -> { if (!batch.contains(test)) batch.add(test); });
        return batch.stream().anyMatch(test -> test.getStatus() != TestRequestStatus.COMPLETED
                && test.getStatus() != TestRequestStatus.CANCELLED);
    }

    /** Called after every completed CLS result so a carried initial CLS can
     * release the return-to-doctor ticket once the whole batch is finished. */
    public void refreshWaitingExaminationsAfterTestCompletion(UUID visitId) {
        queueRepo.findAllByVisit_VisitId(visitId).stream()
                .filter(this::isExaminationQueue)
                .filter(queue -> queue.getStatus() == QueueStatus.WAITING_FOR_TEST)
                .filter(queue -> !hasOutstandingTestsForExamination(queue.getTicketId()))
                .forEach(queue -> {
                    queue.setStatus(QueueStatus.TEST_DONE);
                    queueRepo.save(queue);
                    publishQueueActivated(queue);
                });
    }

    private boolean hasClinicalCycle(QueueTicket examination, List<Invoice> invoices,
                                     List<TestRequest> tests) {
        return invoices.stream().anyMatch(invoice -> belongsToExamination(invoice, examination))
                || tests.stream().anyMatch(test -> belongsToExamination(test, examination));
    }

    private Optional<UUID> invoiceIdOf(TestRequest test) {
        if (test == null || test.getInvoiceItem() == null
                || test.getInvoiceItem().getInvoice() == null) return Optional.empty();
        return Optional.ofNullable(test.getInvoiceItem().getInvoice().getInvoiceId());
    }

    private void addCycleParaclinicalSteps(List<PatientJourneyResponse.Step> steps,
                                           Invoice invoice, QueueTicket examination,
                                           int cycleNumber, List<TestRequest> tests,
                                           Set<UUID> addedQueueIds) {
        Map<UUID, List<TestRequest>> byQueue = tests.stream()
                .filter(test -> test.getQueueTicket() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        test -> test.getQueueTicket().getTicketId(),
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        for (List<TestRequest> group : byQueue.values()) {
            QueueTicket queue = group.get(0).getQueueTicket();
            if (addedQueueIds.add(queue.getTicketId())) {
                steps.add(cycleQueueStep(queue, group, invoice, cycleNumber));
            }
        }

        Map<String, List<TestRequest>> withoutQueue = tests.stream()
                .filter(test -> test.getQueueTicket() == null)
                .collect(java.util.stream.Collectors.groupingBy(this::performingDepartmentKey,
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        withoutQueue.forEach((key, group) -> steps.add(groupedQueueLessStep(
                "CYCLE:" + invoice.getInvoiceId() + ":LAB:" + key,
                group, cycleNumber, "PARACLINICAL", invoice.getInvoiceId())));

        if (tests.isEmpty()) {
            Map<String, List<InvoiceItem>> plannedByRoom = Optional.ofNullable(invoice.getItems())
                    .orElseGet(List::of).stream()
                    .collect(java.util.stream.Collectors.groupingBy(item -> {
                        Department department = item.getService() != null ? item.getService().getDepartment() : null;
                        return department != null ? department.getDepartmentId().toString() : "UNASSIGNED";
                    }, LinkedHashMap::new, java.util.stream.Collectors.toList()));
            int index = 0;
            for (Map.Entry<String, List<InvoiceItem>> entry : plannedByRoom.entrySet()) {
                index++;
                List<InvoiceItem> items = entry.getValue();
                Department department = items.stream().map(InvoiceItem::getService)
                        .filter(Objects::nonNull).map(MedicalService::getDepartment)
                        .filter(Objects::nonNull).findFirst().orElse(null);
                List<PatientJourneyResponse.ServiceProgress> progress = groupedPlannedServiceProgress(items);
                steps.add(new PatientJourneyResponse.Step(
                        "CYCLE:" + invoice.getInvoiceId() + ":LAB:" + entry.getKey(),
                        "PARACLINICAL", progress.size() > 1
                        ? progress.size() + " dịch vụ cận lâm sàng" : progress.isEmpty()
                        ? "Cận lâm sàng" : progress.get(0).serviceName(),
                        department != null ? department.getName() : "Phân phòng sau thanh toán",
                        department != null ? department.getRoomCode() : null, null,
                        QueueStatus.BLOCKED.name(), invoice.getCreatedAt(), null,
                        progress, progress.size(), 0, "PARACLINICAL", cycleNumber,
                        null, invoice.getInvoiceId()));
            }
        }
    }

    private void addLegacyParaclinicalSteps(List<PatientJourneyResponse.Step> steps,
                                            QueueTicket examination, int cycleNumber,
                                            List<TestRequest> tests, Set<UUID> addedQueueIds) {
        Map<UUID, List<TestRequest>> byQueue = tests.stream()
                .filter(test -> test.getQueueTicket() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        test -> test.getQueueTicket().getTicketId(),
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        for (List<TestRequest> group : byQueue.values()) {
            QueueTicket queue = group.get(0).getQueueTicket();
            if (addedQueueIds.add(queue.getTicketId())) {
                steps.add(cycleQueueStep(queue, group, null, cycleNumber));
            }
        }
        Map<String, List<TestRequest>> withoutQueue = tests.stream()
                .filter(test -> test.getQueueTicket() == null)
                .collect(java.util.stream.Collectors.groupingBy(this::performingDepartmentKey,
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        withoutQueue.forEach((key, group) -> steps.add(groupedQueueLessStep(
                "LEGACY:" + examination.getTicketId() + ":LAB:" + key,
                group, cycleNumber, "PARACLINICAL", null)));
    }

    private PatientJourneyResponse.Step groupedQueueLessStep(
            String id, List<TestRequest> tests, Integer cycleNumber,
            String phase, UUID invoiceId) {
        List<TestRequest> ordered = tests.stream()
                .sorted(Comparator.comparing(TestRequest::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        TestRequest first = ordered.get(0);
        Department department = first.getPerformingDepartment();
        List<PatientJourneyResponse.ServiceProgress> progress = groupedServiceProgress(ordered);
        int completed = (int) progress.stream()
                .filter(item -> TestRequestStatus.COMPLETED.name().equals(item.status())).count();
        String name = journeyServiceName(progress);
        String status = aggregateTestStatus(ordered);
        LocalDateTime completedAt = completed == progress.size()
                ? ordered.stream().map(TestRequest::getCompletedAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null) : null;
        return new PatientJourneyResponse.Step(
                id, "PARACLINICAL", name,
                department != null ? department.getName() : null,
                department != null ? department.getRoomCode() : null,
                null, status, first.getCreatedAt(), completedAt,
                progress, progress.size(), completed,
                phase, cycleNumber, null, invoiceId);
    }

    private String standaloneTestGroupKey(TestRequest test) {
        return invoiceIdOf(test).map(UUID::toString).orElse("NO_INVOICE")
                + ":" + performingDepartmentKey(test);
    }

    private String performingDepartmentKey(TestRequest test) {
        return test.getPerformingDepartment() != null
                && test.getPerformingDepartment().getDepartmentId() != null
                ? test.getPerformingDepartment().getDepartmentId().toString() : "UNASSIGNED";
    }

    private PatientJourneyResponse.Step cycleQueueStep(QueueTicket queue, List<TestRequest> tests,
                                                       Invoice invoice, int cycleNumber) {
        List<TestRequest> orderedTests = tests.stream()
                .sorted(Comparator.comparing(TestRequest::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<PatientJourneyResponse.ServiceProgress> progress = groupedServiceProgress(orderedTests);
        int completed = (int) progress.stream()
                .filter(item -> "COMPLETED".equals(item.status()) || "DONE".equals(item.status())).count();
        String name = journeyServiceName(progress);
        String cycleKey = invoice != null ? invoice.getInvoiceId().toString()
                : "LEGACY-" + queue.getTicketId();
        String roomKey = queue.getDepartment() != null
                ? queue.getDepartment().getDepartmentId().toString() : queue.getTicketId().toString();
        return new PatientJourneyResponse.Step(
                "CYCLE:" + cycleKey + ":LAB:" + roomKey,
                "PARACLINICAL", name,
                queue.getDepartment() != null ? queue.getDepartment().getName() : null,
                queue.getDepartment() != null ? queue.getDepartment().getRoomCode() : null,
                queue.getQueueNumber(), queue.getStatus().name(), queue.getCreatedAt(), queue.getCompletedAt(),
                progress, progress.size(), completed, "PARACLINICAL", cycleNumber,
                queue.getTicketId(), invoice != null ? invoice.getInvoiceId() : null);
    }

    /** Hanh trinh trinh bay mot goi xet nghiem nhu mot dich vu. Chi so le da
     * chon trong goi van duoc tinh tien va thuc hien rieng trong Lab, nhung
     * khong lam timeline bien thanh 20-30 "dich vu" kho doc. */
    private List<PatientJourneyResponse.ServiceProgress> groupedServiceProgress(List<TestRequest> tests) {
        Map<String, List<TestRequest>> groups = new LinkedHashMap<>();
        for (TestRequest test : tests) {
            String code = test.getService() != null ? test.getService().getServiceCode() : null;
            LaboratoryAnalyteCatalog.Panel panel = LaboratoryAnalyteCatalog.panel(code)
                    .or(() -> LaboratoryAnalyteCatalog.parentPanel(code)).orElse(null);
            String key = panel != null ? "PANEL:" + panel.serviceCode()
                    : "SERVICE:" + (test.getService() != null && test.getService().getServiceId() != null
                    ? test.getService().getServiceId() : test.getTestRequestId());
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(test);
        }
        return groups.values().stream().map(group -> {
            TestRequest first = group.get(0);
            String code = first.getService() != null ? first.getService().getServiceCode() : null;
            LaboratoryAnalyteCatalog.Panel panel = LaboratoryAnalyteCatalog.panel(code)
                    .or(() -> LaboratoryAnalyteCatalog.parentPanel(code)).orElse(null);
            if (panel == null) return serviceProgress(first);

            boolean selectedAsWholePanel = group.stream().anyMatch(test ->
                    test.getService() != null
                            && panel.serviceCode().equalsIgnoreCase(test.getService().getServiceCode()));
            long selectedAnalytes = group.stream().map(TestRequest::getService)
                    .filter(Objects::nonNull).map(MedicalService::getServiceCode)
                    .filter(codeValue -> LaboratoryAnalyteCatalog.parentPanel(codeValue)
                            .map(parent -> parent.serviceCode().equals(panel.serviceCode())).orElse(false))
                    .distinct().count();
            String name = selectedAsWholePanel || selectedAnalytes == panel.analytes().size()
                    ? panel.name() : panel.name() + " (" + selectedAnalytes + " chỉ số)";
            String status = aggregateTestStatus(group);
            return new PatientJourneyResponse.ServiceProgress(
                    first.getService() != null ? first.getService().getServiceId() : null,
                    panel.serviceCode(), name, status);
        }).toList();
    }

    /** Invoice items exist before TestRequest/QueueTicket creation. Apply the
     * same panel grouping here so the planned journey never exposes a panel's
     * billable analytes as many unrelated services. */
    private List<PatientJourneyResponse.ServiceProgress> groupedPlannedServiceProgress(
            List<InvoiceItem> items) {
        Map<String, List<InvoiceItem>> groups = new LinkedHashMap<>();
        for (InvoiceItem item : items) {
            String code = item.getService() != null
                    ? item.getService().getServiceCode() : item.getServiceCodeSnapshot();
            LaboratoryAnalyteCatalog.Panel panel = LaboratoryAnalyteCatalog.panel(code)
                    .or(() -> LaboratoryAnalyteCatalog.parentPanel(code)).orElse(null);
            String key = panel != null ? "PANEL:" + panel.serviceCode()
                    : "SERVICE:" + (item.getService() != null && item.getService().getServiceId() != null
                    ? item.getService().getServiceId() : code);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }
        return groups.values().stream().map(group -> {
            InvoiceItem first = group.get(0);
            String code = first.getService() != null
                    ? first.getService().getServiceCode() : first.getServiceCodeSnapshot();
            LaboratoryAnalyteCatalog.Panel panel = LaboratoryAnalyteCatalog.panel(code)
                    .or(() -> LaboratoryAnalyteCatalog.parentPanel(code)).orElse(null);
            if (panel == null) {
                return new PatientJourneyResponse.ServiceProgress(
                        first.getService() != null ? first.getService().getServiceId() : null,
                        first.getServiceCodeSnapshot(), first.getServiceSnapshot(), "BLOCKED");
            }
            boolean selectedAsWholePanel = group.stream().anyMatch(item -> {
                String itemCode = item.getService() != null
                        ? item.getService().getServiceCode() : item.getServiceCodeSnapshot();
                return panel.serviceCode().equalsIgnoreCase(itemCode);
            });
            long selectedAnalytes = group.stream().map(item -> item.getService() != null
                            ? item.getService().getServiceCode() : item.getServiceCodeSnapshot())
                    .filter(itemCode -> LaboratoryAnalyteCatalog.parentPanel(itemCode)
                            .map(parent -> parent.serviceCode().equals(panel.serviceCode())).orElse(false))
                    .distinct().count();
            String name = selectedAsWholePanel || selectedAnalytes == panel.analytes().size()
                    ? panel.name() : panel.name() + " (" + selectedAnalytes + " chỉ số)";
            return new PatientJourneyResponse.ServiceProgress(
                    first.getService() != null ? first.getService().getServiceId() : null,
                    panel.serviceCode(), name, "BLOCKED");
        }).toList();
    }

    private String journeyServiceName(List<PatientJourneyResponse.ServiceProgress> progress) {
        return progress.size() > 1
                ? progress.size() + " dịch vụ: " + progress.stream()
                .map(PatientJourneyResponse.ServiceProgress::serviceName).distinct()
                .collect(java.util.stream.Collectors.joining(", "))
                : progress.isEmpty() ? "Cận lâm sàng" : progress.get(0).serviceName();
    }

    private String aggregateTestStatus(List<TestRequest> tests) {
        if (tests.stream().allMatch(test -> test.getStatus() == TestRequestStatus.COMPLETED)) {
            return TestRequestStatus.COMPLETED.name();
        }
        if (tests.stream().anyMatch(test -> test.getStatus() == TestRequestStatus.IN_PROGRESS)) {
            return TestRequestStatus.IN_PROGRESS.name();
        }
        if (tests.stream().allMatch(test -> test.getStatus() == TestRequestStatus.BLOCKED)) {
            return TestRequestStatus.BLOCKED.name();
        }
        if (tests.stream().anyMatch(test -> test.getStatus() == TestRequestStatus.PENDING)) {
            return TestRequestStatus.PENDING.name();
        }
        return tests.get(0).getStatus().name();
    }

    private PatientJourneyResponse.ServiceProgress serviceProgress(TestRequest test) {
        return new PatientJourneyResponse.ServiceProgress(
                test.getService() != null ? test.getService().getServiceId() : null,
                test.getService() != null ? test.getService().getServiceCode() : null,
                test.getService() != null ? test.getService().getName() : "Cận lâm sàng",
                test.getStatus().name());
    }

    private PatientJourneyResponse.Step returnStep(QueueTicket examination, Invoice invoice,
                                                   int cycleNumber, List<TestRequest> tests,
                                                   boolean latestCycle) {
        String status;
        if (invoice != null && invoice.getStatus() == vn.edu.fpt.cares.enums.InvoiceStatus.PENDING)
            status = QueueStatus.BLOCKED.name();
        else if (invoice != null && invoice.getStatus() == vn.edu.fpt.cares.enums.InvoiceStatus.CANCELLED)
            status = "CANCELLED";
        else if (tests.isEmpty() || tests.stream().anyMatch(test ->
                test.getStatus() == TestRequestStatus.PENDING
                        || test.getStatus() == TestRequestStatus.IN_PROGRESS
                        || test.getStatus() == TestRequestStatus.BLOCKED))
            status = QueueStatus.BLOCKED.name();
        // A later invoice must never make this return step appear completed
        // before every test in its own cycle has actually been completed.
        // The timeline is clinical order, not merely invoice creation order.
        else if (!latestCycle) status = QueueStatus.DONE.name();
        else if (java.util.Set.of(QueueStatus.TEST_DONE, QueueStatus.CALLED,
                QueueStatus.IN_PROGRESS, QueueStatus.DONE, QueueStatus.SKIPPED)
                .contains(examination.getStatus()))
            status = examination.getStatus().name();
        else status = QueueStatus.TEST_DONE.name();

        String serviceName = examination.getService() != null
                ? examination.getService().getName() : "bác sĩ";
        UUID invoiceId = invoice != null ? invoice.getInvoiceId() : null;
        String id = "RETURN:" + examination.getTicketId() + ":CYCLE:"
                + (invoiceId != null ? invoiceId : cycleNumber);
        return new PatientJourneyResponse.Step(
                id, "RETURN_EXAMINATION", "Quay lại bác sĩ lần " + cycleNumber + " · " + serviceName,
                examination.getDepartment() != null ? examination.getDepartment().getName() : null,
                examination.getDepartment() != null ? examination.getDepartment().getRoomCode() : null,
                examination.getQueueNumber(), status, examination.getUpdatedAt(),
                QueueStatus.DONE.name().equals(status) ? examination.getUpdatedAt() : null,
                List.of(), 0, QueueStatus.DONE.name().equals(status) ? 1 : 0,
                "RETURN_EXAMINATION", cycleNumber, examination.getTicketId(), invoiceId);
    }

    private Optional<QueueTicket> currentQueue(
            PatientJourneyResponse.Step current, List<QueueTicket> visitQueues) {
        if (current == null) {
            return Optional.empty();
        }
        if (current.queueTicketId() != null) {
            return visitQueues.stream()
                    .filter(queue -> current.queueTicketId().equals(queue.getTicketId())).findFirst();
        }
        if (current.id() == null
                || !(current.id().startsWith("QUEUE:") || current.id().startsWith("RETURN:"))) {
            return Optional.empty();
        }
        try {
            String rawId = current.id().substring(current.id().indexOf(':') + 1);
            UUID ticketId = UUID.fromString(rawId.contains(":")
                    ? rawId.substring(0, rawId.indexOf(':')) : rawId);
            return visitQueues.stream().filter(queue -> ticketId.equals(queue.getTicketId())).findFirst();
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private String toVisitCode(UUID visitId) {
        return "VIS-" + visitId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private boolean isExaminationQueue(QueueTicket queue) {
        return queue.getDepartment() != null
                && queue.getDepartment().getDepartmentType()
                == vn.edu.fpt.cares.enums.DepartmentType.EXAMINATION;
    }

    private int journeyStatusPriority(QueueStatus status) {
        return switch (status) {
            case IN_PROGRESS -> 0;
            case CALLED -> 1;
            case WAITING, TEST_DONE, WAITING_FOR_TEST -> 2;
            case BLOCKED -> 3;
            case DONE -> 4;
            default -> 5;
        };
    }

    private PatientJourneyResponse.Step examinationGroupStep(
            java.util.List<QueueTicket> group, QueueTicket representative,
            boolean initialPhaseFinished) {
        // WAITING_FOR_TEST/TEST_DONE mean the doctor has already finished the
        // initial examination and handed the patient to the dependent flow.
        boolean handedToParaclinical = initialPhaseFinished || representative.getStatus() == QueueStatus.WAITING_FOR_TEST
                || representative.getStatus() == QueueStatus.TEST_DONE;
        java.util.List<PatientJourneyResponse.ServiceProgress> progress = group.stream()
                .map(queue -> new PatientJourneyResponse.ServiceProgress(
                        queue.getService() != null ? queue.getService().getServiceId() : null,
                        queue.getService() != null ? queue.getService().getServiceCode() : null,
                        queue.getService() != null ? queue.getService().getName() : "Khám bệnh",
                        handedToParaclinical ? QueueStatus.DONE.name() : queue.getStatus().name()))
                .toList();
        int completed = handedToParaclinical ? group.size()
                : (int) group.stream().filter(queue -> queue.getStatus() == QueueStatus.DONE).count();
        String names = progress.stream().map(PatientJourneyResponse.ServiceProgress::serviceName)
                .collect(java.util.stream.Collectors.joining(", "));
        String displayName = group.size() > 1 ? group.size() + " dịch vụ: " + names : names;
        LocalDateTime completedAt = completed == group.size()
                ? group.stream().map(QueueTicket::getCompletedAt).filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo).orElse(representative.getUpdatedAt())
                : null;
        return new PatientJourneyResponse.Step(
                "QUEUE:" + representative.getTicketId(), "EXAMINATION", displayName,
                representative.getDepartment() != null ? representative.getDepartment().getName() : null,
                representative.getDepartment() != null ? representative.getDepartment().getRoomCode() : null,
                group.stream().map(QueueTicket::getQueueNumber).min(Integer::compareTo)
                        .orElse(representative.getQueueNumber()),
                handedToParaclinical ? QueueStatus.DONE.name() : representative.getStatus().name(),
                group.stream().map(QueueTicket::getCreatedAt).filter(java.util.Objects::nonNull)
                        .min(LocalDateTime::compareTo).orElse(null),
                completedAt,
                progress, progress.size(), completed, "INITIAL_EXAMINATION", null,
                representative.getTicketId(), null);
    }

    private PatientJourneyResponse.Step queueStep(
            QueueTicket queue, java.util.List<TestRequest> queueTests,
            String type, String status) {
        java.util.List<PatientJourneyResponse.ServiceProgress> progress = queueTests != null && !queueTests.isEmpty()
                ? groupedServiceProgress(queueTests.stream()
                .sorted(java.util.Comparator.comparing(TestRequest::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList())
                : queue.getService() == null ? List.of() : List.of(new PatientJourneyResponse.ServiceProgress(
                    queue.getService().getServiceId(), queue.getService().getServiceCode(),
                    queue.getService().getName(), status));
        String serviceName = progress.isEmpty()
                ? queue.getService() != null ? queue.getService().getName() : "Khám bệnh"
                : journeyServiceName(progress);
        int completedServices = (int) progress.stream()
                .filter(item -> "COMPLETED".equals(item.status()) || "DONE".equals(item.status())).count();
        return new PatientJourneyResponse.Step(
                "QUEUE:" + queue.getTicketId(), type, serviceName,
                queue.getDepartment() != null ? queue.getDepartment().getName() : null,
                queue.getDepartment() != null ? queue.getDepartment().getRoomCode() : null,
                queue.getQueueNumber(), status, queue.getCreatedAt(), queue.getCompletedAt(),
                progress, progress.size(), completedServices, type, null,
                queue.getTicketId(), null);
    }
}
