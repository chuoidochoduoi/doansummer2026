package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.repository.InvoiceRepository;
import vn.edu.fpt.cares.repository.MedicalRecordRepository;
import vn.edu.fpt.cares.repository.QueueTicketRepository;
import vn.edu.fpt.cares.repository.TestRequestRepository;
import vn.edu.fpt.cares.repository.ClinicInformationRepository;
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
    private final ClinicInformationRepository clinicInformationRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public String snapshot(String entityName, String entityId) {
        if (entityId == null && "ClinicInformation".equals(entityName)) {
            entityId = ClinicInformationService.SINGLETON_ID.toString();
        }
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
                data.put("completedAt", value.getCompletedAt());
            });
            case "ClinicInformation" -> clinicInformationRepository.findById(id).ifPresent(value -> {
                data.put("clinicInformationId", value.getClinicInformationId());
                data.put("clinicName", value.getClinicName());
                data.put("legalName", value.getLegalName());
                data.put("taxCode", value.getTaxCode());
                data.put("operatingLicense", value.getOperatingLicense());
                data.put("supportEmail", value.getSupportEmail());
                data.put("phone", value.getPhone());
                data.put("address", value.getAddress());
                data.put("latitude", value.getLatitude());
                data.put("longitude", value.getLongitude());
            });
            default -> { return null; }
        }
        if (data.isEmpty()) return null;
        try { return objectMapper.writeValueAsString(data); }
        catch (Exception ex) { return null; }
    }
}
