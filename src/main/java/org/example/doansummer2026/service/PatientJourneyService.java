package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.journey.PatientJourneyResponse;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class PatientJourneyService {
    private final CustomerVisitRepository visitRepo;
    private final QueueTicketRepository queueRepo;
    private final TestRequestRepository testRepo;

    public void activateNext(UUID visitId) {
        if (hasActiveStep(visitId)) return;
        QueueTicket queue = queueRepo.findAllByVisit_VisitId(visitId).stream().filter(q -> q.getStatus()==QueueStatus.BLOCKED)
                .min(Comparator.comparing(QueueTicket::getCreatedAt)).orElse(null);
        TestRequest test = testRepo.findAllByMedicalRecord_Visit_VisitId(visitId).stream().filter(t -> t.getStatus()==TestRequestStatus.BLOCKED)
                .min(Comparator.comparing(TestRequest::getCreatedAt)).orElse(null);
        if (queue != null && (test == null || !test.getCreatedAt().isBefore(queue.getCreatedAt()))) {
            queue.setStatus(QueueStatus.WAITING); queueRepo.save(queue);
            testRepo.findAllByQueueTicket_TicketId(queue.getTicketId()).stream()
                    .filter(item -> item.getStatus() == TestRequestStatus.BLOCKED)
                    .forEach(item -> { item.setStatus(TestRequestStatus.PENDING); testRepo.save(item); });
        } else if (test != null) { test.setStatus(TestRequestStatus.PENDING); testRepo.save(test); }
        else {
            CustomerVisit visit = visitRepo.findById(visitId).orElse(null);
            boolean hasAnyStep = !queueRepo.findAllByVisit_VisitId(visitId).isEmpty()
                    || !testRepo.findAllByMedicalRecord_Visit_VisitId(visitId).isEmpty();
            if (visit != null && hasAnyStep && !hasActiveStep(visitId)) {
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
        boolean testActive = testRepo.findAllByMedicalRecord_Visit_VisitId(visitId).stream().anyMatch(t ->
                t.getStatus()==TestRequestStatus.PENDING || t.getStatus()==TestRequestStatus.IN_PROGRESS);
        return queueActive || testActive;
    }

    @Transactional(readOnly=true)
    public List<PatientJourneyResponse> list(String search, String status) {
        String needle = search==null?"":search.trim().toLowerCase();
        return visitRepo.findAll().stream().map(this::build)
                .filter(j -> needle.isBlank() || ((j.patientName()==null?"":j.patientName())+" "+(j.phone()==null?"":j.phone())+" "+j.visitCode()).toLowerCase().contains(needle))
                .filter(j -> status==null || status.isBlank() || status.equals(j.currentStatus()))
                .sorted(Comparator.comparing(PatientJourneyResponse::checkInTime, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }

    @Transactional(readOnly=true)
    public PatientJourneyResponse get(UUID id) { return build(visitRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Khong tim thay luot kham"))); }

    @Transactional(readOnly=true)
    public List<PatientJourneyResponse> listForCustomer(UUID profileId) {
        return visitRepo.findAllByCustomer_ProfileIdOrderByCheckInTimeDesc(profileId).stream().map(this::build).toList();
    }

    private PatientJourneyResponse build(CustomerVisit visit) {
        List<PatientJourneyResponse.Step> steps = new ArrayList<>();
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

        if (false) { // Luồng cũ: giữ để tương thích mã nguồn, không cộng trùng QueueTicket và TestRequest.
        queueRepo.findAllByVisit_VisitId(visit.getVisitId()).forEach(q -> steps.add(new PatientJourneyResponse.Step(
                "QUEUE:"+q.getTicketId(), "EXAMINATION", q.getService()!=null?q.getService().getName():"Khám bệnh",
                q.getDepartment().getName(), q.getDepartment().getRoomCode(), q.getQueueNumber(), q.getStatus().name(), q.getCreatedAt(), q.getCompletedAt())));
        testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId()).forEach(t -> steps.add(new PatientJourneyResponse.Step(
                "TEST:"+t.getTestRequestId(), "PARACLINICAL", t.getService()!=null?t.getService().getName():"Cận lâm sàng",
                t.getPerformingDepartment().getName(), t.getPerformingDepartment().getRoomCode(), null, t.getStatus().name(), t.getCreatedAt(), t.getCompletedAt())));
        }
        steps.sort(Comparator.comparing(PatientJourneyResponse.Step::startedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        var current = steps.stream().filter(s -> !List.of("BLOCKED","DONE","COMPLETED","SKIPPED","CANCELLED").contains(s.status())).findFirst().orElse(null);
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
