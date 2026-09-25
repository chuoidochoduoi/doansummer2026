package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.enums.InvoiceStatus;
import vn.edu.fpt.cares.enums.QueueStatus;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditSnapshotServiceTest {
    @Mock InvoiceRepository invoiceRepository;
    @Mock QueueTicketRepository queueTicketRepository;
    @Mock MedicalRecordRepository medicalRecordRepository;
    @Mock TestRequestRepository testRequestRepository;
    @Mock ClinicInformationRepository clinicInformationRepository;
    private AuditSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new AuditSnapshotService(invoiceRepository, queueTicketRepository, medicalRecordRepository,
                testRequestRepository, clinicInformationRepository, new ObjectMapper());
    }

    @Test
    void rejectsMissingInvalidUnknownAndAbsentEntities() {
        assertNull(service.snapshot("Invoice", null));
        assertNull(service.snapshot("Invoice", "not-a-uuid"));
        assertNull(service.snapshot("Unknown", UUID.randomUUID().toString()));
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());
        assertNull(service.snapshot("Invoice", id.toString()));
    }

    @Test
    void snapshotsInvoice() {
        UUID id = UUID.randomUUID();
        Invoice value = Invoice.builder().invoiceId(id).invoiceCode("INV-01").status(InvoiceStatus.PAID)
                .totalAmount(new BigDecimal("120000")).paidAmount(new BigDecimal("120000")).build();
        when(invoiceRepository.findById(id)).thenReturn(Optional.of(value));
        String json = service.snapshot("Invoice", id.toString());
        assertAll(() -> assertTrue(json.contains(id.toString())), () -> assertTrue(json.contains("PAID")),
                () -> assertTrue(json.contains("120000")));
    }

    @Test
    void snapshotsQueueTicket() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 9, 5, 9, 0);
        QueueTicket value = QueueTicket.builder().ticketId(id).status(QueueStatus.DONE).queueNumber(7)
                .calledAt(now).completedAt(now.plusMinutes(10)).build();
        when(queueTicketRepository.findById(id)).thenReturn(Optional.of(value));
        String json = service.snapshot("QueueTicket", id.toString());
        assertTrue(json.contains("queueNumber"));
        assertTrue(json.contains("DONE"));
    }

    @Test
    void snapshotsMedicalRecord() {
        UUID id = UUID.randomUUID();
        MedicalRecord value = mock(MedicalRecord.class);
        when(value.getRecordId()).thenReturn(id);
        when(value.getRecordCode()).thenReturn("MR-001");
        when(value.getDiagnosis()).thenReturn("Viêm họng");
        when(value.getConclusion()).thenReturn("Theo dõi");
        when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(value));
        String json = service.snapshot("MedicalRecord", id.toString());
        assertTrue(json.contains("MR-001"));
        assertTrue(json.contains("Viêm họng"));
    }

    @Test
    void snapshotsTestRequest() {
        UUID id = UUID.randomUUID();
        TestRequest value = mock(TestRequest.class);
        when(value.getTestRequestId()).thenReturn(id);
        when(testRequestRepository.findById(id)).thenReturn(Optional.of(value));
        String json = service.snapshot("TestRequest", id.toString());
        assertTrue(json.contains(id.toString()));
        assertTrue(json.contains("testRequestId"));
    }

    @Test
    void clinicInformationUsesSingletonIdAndSerializesFields() {
        UUID id = ClinicInformationService.SINGLETON_ID;
        ClinicInformation value = mock(ClinicInformation.class);
        when(value.getClinicInformationId()).thenReturn(id);
        when(value.getClinicName()).thenReturn("CareS");
        when(value.getLegalName()).thenReturn("Phòng khám CareS");
        when(clinicInformationRepository.findById(id)).thenReturn(Optional.of(value));
        String json = service.snapshot("ClinicInformation", null);
        assertTrue(json.contains("CareS"));
        verify(clinicInformationRepository).findById(id);
    }

    @Test
    void serializationFailureReturnsNull() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        service = new AuditSnapshotService(invoiceRepository, queueTicketRepository, medicalRecordRepository,
                testRequestRepository, clinicInformationRepository, mapper);
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.of(Invoice.builder().invoiceId(id).build()));
        when(mapper.writeValueAsString(any())).thenThrow(new IllegalStateException("json unavailable"));
        assertNull(service.snapshot("Invoice", id.toString()));
    }
}
