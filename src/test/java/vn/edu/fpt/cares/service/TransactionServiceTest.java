package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.dto.transaction.TransactionCreateRequest;
import vn.edu.fpt.cares.dto.transaction.TransactionUpdateRequest;
import vn.edu.fpt.cares.enums.InvoiceStatus;
import vn.edu.fpt.cares.enums.PaymentMethod;
import vn.edu.fpt.cares.enums.TransactionStatus;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.Invoice;
import vn.edu.fpt.cares.model.StaffInfo;
import vn.edu.fpt.cares.model.Transaction;
import vn.edu.fpt.cares.repository.InvoiceRepository;
import vn.edu.fpt.cares.repository.StaffInfoRepository;
import vn.edu.fpt.cares.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository repository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock InvoiceService invoiceService;
    @Mock StaffInfoRepository staffRepository;
    @InjectMocks TransactionService service;

    @Test
    void searchAndGetMapTransactions() {
        UUID invoiceId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime to = from.plusDays(1);
        var pageable = PageRequest.of(0, 10);
        Transaction transaction = transaction(transactionId, invoice(invoiceId, InvoiceStatus.PENDING),
                TransactionStatus.PENDING, "100000");
        when(repository.search(invoiceId, TransactionStatus.PENDING, from, to, pageable))
                .thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));
        when(repository.findById(transactionId)).thenReturn(Optional.of(transaction));

        var page = service.search(invoiceId, TransactionStatus.PENDING, from, to, pageable);
        var item = service.get(transactionId);

        assertAll(
                () -> assertEquals(1, page.totalElements()),
                () -> assertEquals(transactionId, page.content().get(0).transactionId()),
                () -> assertEquals(invoiceId, item.invoiceId()));
    }

    @Test
    void createRejectsMissingOrNonPendingInvoice() {
        UUID missingId = UUID.randomUUID();
        UUID paidId = UUID.randomUUID();
        when(invoiceRepository.findByIdForUpdate(missingId)).thenReturn(Optional.empty());
        when(invoiceRepository.findByIdForUpdate(paidId))
                .thenReturn(Optional.of(invoice(paidId, InvoiceStatus.PAID)));

        assertThrows(ResourceNotFoundException.class, () -> service.create(request(missingId, "10000", null)));
        assertThrows(ConflictException.class, () -> service.create(request(paidId, "10000", null)));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsNonPositiveAndAmountAboveRemainingBalance() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = invoice(invoiceId, InvoiceStatus.PENDING);
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(repository.findByInvoice_InvoiceId(invoiceId)).thenReturn(List.of(
                transaction(UUID.randomUUID(), invoice, TransactionStatus.SUCCESS, "30000"),
                transaction(UUID.randomUUID(), invoice, TransactionStatus.FAILED, "90000")));

        assertThrows(BadRequestException.class, () -> service.create(request(invoiceId, "0", null)));
        assertThrows(BadRequestException.class, () -> service.create(request(invoiceId, "70001", null)));
        verify(repository, never()).save(any());
    }

    @Test
    void createPersistsTransactionWithoutReceiverAndRetriesCodeCollision() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = invoice(invoiceId, InvoiceStatus.PENDING);
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(repository.existsByTransactionCode(anyString())).thenReturn(true, false);
        when(repository.save(any())).thenAnswer(invocation -> {
            Transaction value = invocation.getArgument(0);
            value.setTransactionId(UUID.randomUUID());
            return value;
        });

        var response = service.create(request(invoiceId, "40000", null));

        assertAll(
                () -> assertEquals(new BigDecimal("40000"), response.amount()),
                () -> assertEquals(TransactionStatus.PENDING, response.status()),
                () -> assertNull(response.receivedById()),
                () -> assertTrue(response.transactionCode().startsWith("TXN-INV-001-")));
        verify(repository, times(2)).existsByTransactionCode(anyString());
        verifyNoInteractions(staffRepository);
    }

    @Test
    void createResolvesReceiverAndCopiesOptionalFields() {
        UUID invoiceId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Invoice invoice = invoice(invoiceId, InvoiceStatus.PENDING);
        StaffInfo receiver = StaffInfo.builder().staffId(staffId).staffCode("STF-001").build();
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(staffRepository.findById(staffId)).thenReturn(Optional.of(receiver));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new TransactionCreateRequest(invoiceId, new BigDecimal("50000"),
                PaymentMethod.BANK_TRANSFER, "GW-001", "Đã đối soát", staffId));

        assertAll(
                () -> assertEquals(staffId, response.receivedById()),
                () -> assertEquals("STF-001", response.receivedByName()),
                () -> assertEquals("GW-001", response.gatewayReference()),
                () -> assertEquals("Đã đối soát", response.note()));
    }

    @Test
    void createRejectsUnknownReceiverAndThreeCodeCollisions() {
        UUID invoiceId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Invoice invoice = invoice(invoiceId, InvoiceStatus.PENDING);
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(staffRepository.findById(staffId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(request(invoiceId, "10000", staffId)));

        reset(staffRepository);
        when(repository.existsByTransactionCode(anyString())).thenReturn(true, true, true);
        assertThrows(ConflictException.class,
                () -> service.create(request(invoiceId, "10000", null)));
        verify(repository, times(3)).existsByTransactionCode(anyString());
    }

    @Test
    void updateRejectsMissingTransactionAndMissingInvoice() {
        UUID missing = UUID.randomUUID();
        UUID orphan = UUID.randomUUID();
        Invoice invoice = invoice(UUID.randomUUID(), InvoiceStatus.PENDING);
        when(repository.findByIdForUpdate(missing)).thenReturn(Optional.empty());
        when(repository.findByIdForUpdate(orphan)).thenReturn(Optional.of(
                transaction(orphan, invoice, TransactionStatus.PENDING, "10000")));
        when(invoiceRepository.findByIdForUpdate(invoice.getInvoiceId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(missing, emptyUpdate()));
        assertThrows(ResourceNotFoundException.class, () -> service.update(orphan, emptyUpdate()));
    }

    @Test
    void updateSuccessSetsSystemPaidAtAndExcludesCurrentTransaction() {
        UUID invoiceId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Invoice invoice = invoice(invoiceId, InvoiceStatus.PENDING);
        Transaction current = transaction(transactionId, invoice, TransactionStatus.PENDING, "70000");
        Transaction other = transaction(UUID.randomUUID(), invoice, TransactionStatus.SUCCESS, "30000");
        when(repository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(current));
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(repository.findByInvoice_InvoiceId(invoiceId)).thenReturn(List.of(current, other));
        when(repository.save(current)).thenReturn(current);

        var before = LocalDateTime.now();
        var response = service.update(transactionId,
                new TransactionUpdateRequest(TransactionStatus.SUCCESS, LocalDateTime.of(2000, 1, 1, 0, 0),
                        "GW-NEW", "Thành công"));

        assertAll(
                () -> assertEquals(TransactionStatus.SUCCESS, response.status()),
                () -> assertFalse(response.paidAt().isBefore(before)),
                () -> assertEquals("GW-NEW", response.gatewayReference()),
                () -> assertEquals("Thành công", response.note()));
        verify(invoiceService).recalculatePaidAmount(invoiceId);
    }

    @Test
    void updateSuccessKeepsExistingPaidAt() {
        UUID invoiceId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Invoice invoice = invoice(invoiceId, InvoiceStatus.PENDING);
        Transaction current = transaction(transactionId, invoice, TransactionStatus.PENDING, "10000");
        LocalDateTime existing = LocalDateTime.of(2026, 9, 1, 8, 0);
        current.setPaidAt(existing);
        when(repository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(current));
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(repository.save(current)).thenReturn(current);

        assertEquals(existing, service.confirm(transactionId).paidAt());
    }

    @Test
    void updateSuccessRejectsAmountAboveRemainingBalance() {
        UUID invoiceId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Invoice invoice = invoice(invoiceId, InvoiceStatus.PENDING);
        Transaction current = transaction(transactionId, invoice, TransactionStatus.PENDING, "80000");
        Transaction paid = transaction(UUID.randomUUID(), invoice, TransactionStatus.SUCCESS, "30000");
        when(repository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(current));
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(repository.findByInvoice_InvoiceId(invoiceId)).thenReturn(List.of(current, paid));

        assertThrows(ConflictException.class, () -> service.confirm(transactionId));
        assertEquals(TransactionStatus.PENDING, current.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void pendingCanFailOrCancelAndEmptyUpdateKeepsFields() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = invoice(invoiceId, InvoiceStatus.PENDING);
        Transaction failed = transaction(UUID.randomUUID(), invoice, TransactionStatus.PENDING, "10000");
        Transaction cancelled = transaction(UUID.randomUUID(), invoice, TransactionStatus.PENDING, "10000");
        Transaction unchanged = transaction(UUID.randomUUID(), invoice, TransactionStatus.PENDING, "10000");
        unchanged.setGatewayReference("OLD");
        unchanged.setNote("Ghi chú cũ");
        for (Transaction value : List.of(failed, cancelled, unchanged)) {
            when(repository.findByIdForUpdate(value.getTransactionId())).thenReturn(Optional.of(value));
            when(repository.save(value)).thenReturn(value);
        }
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));

        assertEquals(TransactionStatus.FAILED, service.fail(failed.getTransactionId()).status());
        assertEquals(TransactionStatus.CANCELLED, service.update(cancelled.getTransactionId(),
                new TransactionUpdateRequest(TransactionStatus.CANCELLED, null, null, null)).status());
        var response = service.update(unchanged.getTransactionId(), emptyUpdate());
        assertAll(
                () -> assertEquals("OLD", response.gatewayReference()),
                () -> assertEquals("Ghi chú cũ", response.note()));
    }

    @Test
    void invalidTransitionsFromEveryStateAreRejected() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = invoice(invoiceId, InvoiceStatus.PENDING);
        List<Transaction> values = List.of(
                transaction(UUID.randomUUID(), invoice, TransactionStatus.PENDING, "1"),
                transaction(UUID.randomUUID(), invoice, TransactionStatus.SUCCESS, "1"),
                transaction(UUID.randomUUID(), invoice, TransactionStatus.FAILED, "1"),
                transaction(UUID.randomUUID(), invoice, TransactionStatus.CANCELLED, "1"));
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        for (Transaction value : values) {
            when(repository.findByIdForUpdate(value.getTransactionId())).thenReturn(Optional.of(value));
        }

        assertThrows(BadRequestException.class, () -> service.update(values.get(0).getTransactionId(),
                new TransactionUpdateRequest(TransactionStatus.PENDING, null, null, null)));
        assertThrows(BadRequestException.class, () -> service.update(values.get(1).getTransactionId(),
                new TransactionUpdateRequest(TransactionStatus.FAILED, null, null, null)));
        assertThrows(BadRequestException.class, () -> service.update(values.get(2).getTransactionId(),
                new TransactionUpdateRequest(TransactionStatus.SUCCESS, null, null, null)));
        assertThrows(BadRequestException.class, () -> service.update(values.get(3).getTransactionId(),
                new TransactionUpdateRequest(TransactionStatus.SUCCESS, null, null, null)));
    }

    @Test
    void deleteAlwaysPreservesPaymentHistoryAndMissingFindFails() {
        UUID existing = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(repository.findById(existing)).thenReturn(Optional.of(
                transaction(existing, invoice(UUID.randomUUID(), InvoiceStatus.PENDING),
                        TransactionStatus.PENDING, "10000")));
        when(repository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(ConflictException.class, () -> service.delete(existing));
        assertThrows(ResourceNotFoundException.class, () -> service.findById(missing));
        verify(repository, never()).delete(any());
    }

    private TransactionCreateRequest request(UUID invoiceId, String amount, UUID receiverId) {
        return new TransactionCreateRequest(invoiceId, new BigDecimal(amount), PaymentMethod.CASH,
                null, null, receiverId);
    }

    private TransactionUpdateRequest emptyUpdate() {
        return new TransactionUpdateRequest(null, null, null, null);
    }

    private Invoice invoice(UUID id, InvoiceStatus status) {
        return Invoice.builder().invoiceId(id).invoiceCode("INV-001")
                .totalAmount(new BigDecimal("100000")).status(status).build();
    }

    private Transaction transaction(UUID id, Invoice invoice, TransactionStatus status, String amount) {
        return Transaction.builder().transactionId(id).invoice(invoice).transactionCode("TXN-" + id)
                .amount(new BigDecimal(amount)).paymentMethod(PaymentMethod.CASH).status(status).build();
    }
}
