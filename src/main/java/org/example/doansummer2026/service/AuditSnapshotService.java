package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditSnapshotService {
    private final InvoiceRepository invoiceRepository;
    private final QueueTicketRepository queueTicketRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final TestRequestRepository testRequestRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public String snapshot(String entityName, String entityId) {
        if (entityId == null) return null;
        UUID id;
        try { id = UUID.fromString(entityId); }
        catch (IllegalArgumentException ex) { return null; }

        Map<String, Object> data = new LinkedHashMap<>();
        switch (entityName) {
            case "Invoice" -> invoiceRepository.findById(id).ifPresent(value -> {
                data.put("invoiceId", value.getInvoiceId());
                data.put("status", value.getStatus());
                data.put("totalAmount", value.getTotalAmount());
                data.put("paidAmount", value.getPaidAmount());
            });
            case "QueueTicket" -> queueTicketRepository.findById(id).ifPresent(value -> {
                data.put("ticketId", value.getTicketId());
                data.put("status", value.getStatus());
                data.put("queueNumber", value.getQueueNumber());
                data.put("calledAt", value.getCalledAt());
                data.put("completedAt", value.getCompletedAt());
            });
            case "MedicalRecord" -> medicalRecordRepository.findById(id).ifPresent(value -> {
                data.put("recordId", value.getRecordId());
                data.put("recordCode", value.getRecordCode());
                data.put("status", value.getStatus());
                data.put("diagnosis", value.getDiagnosis());
                data.put("conclusion", value.getConclusion());
            });
            case "TestRequest" -> testRequestRepository.findById(id).ifPresent(value -> {
                data.put("testRequestId", value.getTestRequestId());
                data.put("status", value.getStatus());
                data.put("performedAt", value.getPerformedAt());
                data.put("completedAt", value.getCompletedAt());
            });
            default -> { return null; }
        }
        if (data.isEmpty()) return null;
        try { return objectMapper.writeValueAsString(data); }
        catch (Exception ex) { return null; }
    }
}
