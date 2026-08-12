package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.transaction.TransactionCreateRequest;
import org.example.doansummer2026.dto.transaction.TransactionUpdateRequest;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.enums.TransactionStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.Transaction;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repo;

    @Mock
    private InvoiceRepository invoiceRepo;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private StaffInfoRepository staffRepo;

    @InjectMocks
    private TransactionService transactionService;


    // =========================================================
    // HELPERS
    // =========================================================

    private Invoice invoice(
            UUID id,
            String code,
            InvoiceStatus status
    ) {
        return Invoice.builder()
                .invoiceId(id)
                .invoiceCode(code)
                .status(status)
                .build();
    }

    private StaffInfo staff(UUID id) {
        return StaffInfo.builder()
                .staffId(id)
                .staffCode("STF-TEST")
                .build();
    }

    private Transaction transaction(
            UUID id,
            Invoice invoice,
            TransactionStatus status
    ) {
        return Transaction.builder()
                .transactionId(id)
                .invoice(invoice)
                .transactionCode("TXN-TEST")
                .amount(new BigDecimal("100000"))
                .status(status)
                .build();
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void search_ShouldReturnMappedPage() {

        UUID invoiceId = UUID.randomUUID();

        LocalDateTime from =
                LocalDateTime.now().minusDays(1);

        LocalDateTime to =
                LocalDateTime.now().plusDays(1);

        var pageable =
                PageRequest.of(0, 10);

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        Transaction t =
                transaction(
                        UUID.randomUUID(),
                        invoice,
                        TransactionStatus.PENDING
                );

        when(
                repo.search(
                        invoiceId,
                        TransactionStatus.PENDING,
                        from,
                        to,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of(t))
        );

        var result =
                transactionService.search(
                        invoiceId,
                        TransactionStatus.PENDING,
                        from,
                        to,
                        pageable
                );

        assertNotNull(result);

        verify(repo).search(
                invoiceId,
                TransactionStatus.PENDING,
                from,
                to,
                pageable
        );
    }


    // =========================================================
    // FIND / GET
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenFound() {

        UUID id = UUID.randomUUID();

        Transaction t =
                transaction(
                        id,
                        invoice(
                                UUID.randomUUID(),
                                "INV001",
                                InvoiceStatus.PENDING
                        ),
                        TransactionStatus.PENDING
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

        assertSame(
                t,
                transactionService.findById(id)
        );
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.findById(id)
        );
    }


    @Test
    void get_ShouldReturnResponse() {

        UUID id = UUID.randomUUID();

        Transaction t =
                transaction(
                        id,
                        invoice(
                                UUID.randomUUID(),
                                "INV001",
                                InvoiceStatus.PENDING
                        ),
                        TransactionStatus.PENDING
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

        assertNotNull(
                transactionService.get(id)
        );
    }


    // =========================================================
    // CREATE - INVOICE MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenInvoiceMissing() {

        UUID invoiceId =
                UUID.randomUUID();

        TransactionCreateRequest req =
                mock(TransactionCreateRequest.class);

        when(req.invoiceId())
                .thenReturn(invoiceId);

        when(invoiceRepo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.create(req)
        );

        verifyNoInteractions(staffRepo);
        verify(repo, never()).save(any());
    }


    // =========================================================
    // CREATE - INVOICE NOT PENDING
    // =========================================================

    @Test
    void create_ShouldReject_WhenInvoiceNotPending() {

        UUID invoiceId =
                UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PAID
                );

        TransactionCreateRequest req =
                mock(TransactionCreateRequest.class);

        when(req.invoiceId())
                .thenReturn(invoiceId);

        when(invoiceRepo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        assertThrows(
                ConflictException.class,
                () -> transactionService.create(req)
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // CREATE - RECEIVED BY MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenReceivedByMissing() {

        UUID invoiceId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        TransactionCreateRequest req =
                mock(TransactionCreateRequest.class);

        when(req.invoiceId())
                .thenReturn(invoiceId);

        when(req.receivedById())
                .thenReturn(staffId);

        when(invoiceRepo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.create(req)
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // CREATE - NO RECEIVED BY
    // =========================================================

    @Test
    void create_ShouldCreateWithoutReceivedBy() {

        UUID invoiceId =
                UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        TransactionCreateRequest req =
                mock(TransactionCreateRequest.class);

        when(req.invoiceId())
                .thenReturn(invoiceId);

        when(req.amount())
                .thenReturn(new BigDecimal("100000"));

        when(req.paymentMethod())
                .thenReturn(
                        org.example.doansummer2026.enums.PaymentMethod.CASH
                );

        when(req.gatewayReference())
                .thenReturn("GW001");

        when(req.note())
                .thenReturn("Thanh toan");

        when(invoiceRepo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(repo.existsByTransactionCode(anyString()))
                .thenReturn(false);

        when(repo.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t =
                            invocation.getArgument(0);

                    t.setTransactionId(UUID.randomUUID());

                    return t;
                });

        var result =
                transactionService.create(req);

        assertNotNull(result);

        verifyNoInteractions(staffRepo);

        verify(repo).save(argThat(t ->
                t.getInvoice() == invoice
                        && t.getStatus() == TransactionStatus.PENDING
                        && t.getReceivedBy() == null
                        && new BigDecimal("100000")
                        .compareTo(t.getAmount()) == 0
                        && t.getTransactionCode() != null
                        && t.getTransactionCode()
                        .startsWith("TXN-INV001-")
        ));
    }


    // =========================================================
    // CREATE - WITH RECEIVED BY
    // =========================================================

    @Test
    void create_ShouldCreateWithReceivedBy() {

        UUID invoiceId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV002",
                        InvoiceStatus.PENDING
                );

        StaffInfo staff =
                staff(staffId);

        TransactionCreateRequest req =
                mock(TransactionCreateRequest.class);

        when(req.invoiceId())
                .thenReturn(invoiceId);

        when(req.receivedById())
                .thenReturn(staffId);

        when(req.amount())
                .thenReturn(new BigDecimal("50000"));

        when(req.paymentMethod())
                .thenReturn(
                        org.example.doansummer2026.enums.PaymentMethod.CASH
                );

        when(invoiceRepo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(repo.existsByTransactionCode(anyString()))
                .thenReturn(false);

        when(repo.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t =
                            invocation.getArgument(0);

                    t.setTransactionId(UUID.randomUUID());

                    return t;
                });

        transactionService.create(req);

        verify(repo)
                .save(argThat(t ->
                        t.getReceivedBy() == staff
                                && t.getStatus()
                                == TransactionStatus.PENDING
                ));
    }


    // =========================================================
    // GENERATE CODE - FIRST CODE UNIQUE
    // =========================================================

    @Test
    void create_ShouldAcceptFirstGeneratedTransactionCode_WhenUnique() {

        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV100",
                        InvoiceStatus.PENDING
                );

        TransactionCreateRequest req =
                mock(TransactionCreateRequest.class);

        when(req.invoiceId())
                .thenReturn(invoiceId);

        when(invoiceRepo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(repo.existsByTransactionCode(anyString()))
                .thenReturn(false);

        when(repo.save(any(Transaction.class)))
                .thenAnswer(i -> {
                    Transaction t = i.getArgument(0);
                    t.setTransactionId(UUID.randomUUID());
                    return t;
                });

        transactionService.create(req);

        verify(repo, times(1))
                .existsByTransactionCode(anyString());
    }


    // =========================================================
    // GENERATE CODE - COLLISION THEN SUCCESS
    // =========================================================

    @Test
    void create_ShouldRetryTransactionCode_WhenFirstCodeExists() {

        UUID invoiceId =
                UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV200",
                        InvoiceStatus.PENDING
                );

        TransactionCreateRequest req =
                mock(TransactionCreateRequest.class);

        when(req.invoiceId())
                .thenReturn(invoiceId);

        when(invoiceRepo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(repo.existsByTransactionCode(anyString()))
                .thenReturn(
                        true,
                        false
                );

        when(repo.save(any(Transaction.class)))
                .thenAnswer(i -> {
                    Transaction t = i.getArgument(0);
                    t.setTransactionId(UUID.randomUUID());
                    return t;
                });

        transactionService.create(req);

        verify(repo, times(2))
                .existsByTransactionCode(anyString());
    }


    // =========================================================
    // GENERATE CODE - THREE COLLISIONS
    // =========================================================

    @Test
    void create_ShouldThrow_WhenTransactionCodeCollidesThreeTimes() {

        UUID invoiceId =
                UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV999",
                        InvoiceStatus.PENDING
                );

        TransactionCreateRequest req =
                mock(TransactionCreateRequest.class);

        when(req.invoiceId())
                .thenReturn(invoiceId);

        when(invoiceRepo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(repo.existsByTransactionCode(anyString()))
                .thenReturn(
                        true,
                        true,
                        true
                );

        assertThrows(
                ConflictException.class,
                () -> transactionService.create(req)
        );

        verify(repo, times(3))
                .existsByTransactionCode(anyString());

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // UPDATE - MISSING TRANSACTION
    // =========================================================

    @Test
    void update_ShouldThrow_WhenTransactionMissing() {

        UUID id =
                UUID.randomUUID();

        TransactionUpdateRequest req =
                mock(TransactionUpdateRequest.class);

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE - PENDING -> SUCCESS
    // =========================================================

    @Test
    void update_ShouldTransitionPendingToSuccessAndSetPaidAt() {

        UUID id = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        Transaction t =
                transaction(
                        id,
                        invoice,
                        TransactionStatus.PENDING
                );

        t.setPaidAt(null);

        TransactionUpdateRequest req =
                mock(TransactionUpdateRequest.class);

        when(req.status())
                .thenReturn(TransactionStatus.SUCCESS);

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        when(repo.save(t))
                .thenReturn(t);

        LocalDateTime before =
                LocalDateTime.now();

        var result =
                transactionService.update(
                        id,
                        req
                );

        LocalDateTime after =
                LocalDateTime.now();

        assertNotNull(result);

        assertEquals(
                TransactionStatus.SUCCESS,
                t.getStatus()
        );

        assertNotNull(
                t.getPaidAt()
        );

        assertFalse(
                t.getPaidAt().isBefore(before)
        );

        assertFalse(
                t.getPaidAt().isAfter(after)
        );

        verify(invoiceService)
                .recalculatePaidAmount(invoiceId);
    }


    // =========================================================
    // UPDATE - SUCCESS WITH EXISTING PAID AT
    // =========================================================

    @Test
    void update_ShouldKeepExistingPaidAt_WhenAlreadySet() {

        UUID id = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        Transaction t =
                transaction(
                        id,
                        invoice,
                        TransactionStatus.PENDING
                );

        LocalDateTime oldPaidAt =
                LocalDateTime.now()
                        .minusMinutes(5);

        t.setPaidAt(oldPaidAt);

        TransactionUpdateRequest req =
                mock(TransactionUpdateRequest.class);

        when(req.status())
                .thenReturn(TransactionStatus.SUCCESS);

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        when(repo.save(t))
                .thenReturn(t);

        transactionService.update(
                id,
                req
        );

        assertEquals(
                oldPaidAt,
                t.getPaidAt()
        );
    }


    // =========================================================
    // UPDATE - EXPLICIT PAID AT OVERRIDES
    // =========================================================

    @Test
    void update_ShouldUseExplicitPaidAt_WhenProvided() {

        UUID id = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        Transaction t =
                transaction(
                        id,
                        invoice,
                        TransactionStatus.PENDING
                );

        LocalDateTime explicit =
                LocalDateTime.of(
                        2026,
                        8,
                        10,
                        10,
                        30
                );

        TransactionUpdateRequest req =
                mock(TransactionUpdateRequest.class);

        when(req.status())
                .thenReturn(TransactionStatus.SUCCESS);

        when(req.paidAt())
                .thenReturn(explicit);

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        when(repo.save(t))
                .thenReturn(t);

        transactionService.update(
                id,
                req
        );

        assertEquals(
                explicit,
                t.getPaidAt()
        );
    }


    // =========================================================
    // UPDATE - PENDING -> FAILED
    // =========================================================

    @Test
    void update_ShouldAllowPendingToFailed() {

        UUID id = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        Transaction t =
                transaction(
                        id,
                        invoice,
                        TransactionStatus.PENDING
                );

        TransactionUpdateRequest req =
                mock(TransactionUpdateRequest.class);

        when(req.status())
                .thenReturn(TransactionStatus.FAILED);

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        when(repo.save(t))
                .thenReturn(t);

        transactionService.update(
                id,
                req
        );

        assertEquals(
                TransactionStatus.FAILED,
                t.getStatus()
        );

        assertNull(
                t.getPaidAt()
        );
    }


    // =========================================================
    // UPDATE - PENDING -> CANCELLED
    // =========================================================

    @Test
    void update_ShouldAllowPendingToCancelled() {

        UUID id = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        Transaction t =
                transaction(
                        id,
                        invoice,
                        TransactionStatus.PENDING
                );

        TransactionUpdateRequest req =
                mock(TransactionUpdateRequest.class);

        when(req.status())
                .thenReturn(TransactionStatus.CANCELLED);

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        when(repo.save(t))
                .thenReturn(t);

        transactionService.update(
                id,
                req
        );

        assertEquals(
                TransactionStatus.CANCELLED,
                t.getStatus()
        );
    }


    // =========================================================
    // UPDATE - INVALID PENDING TRANSITION
    // =========================================================

    @Test
    void update_ShouldRejectPendingToPending() {

        UUID id = UUID.randomUUID();

        Transaction t =
                transaction(
                        id,
                        invoice(
                                UUID.randomUUID(),
                                "INV",
                                InvoiceStatus.PENDING
                        ),
                        TransactionStatus.PENDING
                );

        TransactionUpdateRequest req =
                mock(TransactionUpdateRequest.class);

        when(req.status())
                .thenReturn(TransactionStatus.PENDING);

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        assertThrows(
                BadRequestException.class,
                () -> transactionService.update(id, req)
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // UPDATE - TERMINAL STATUS CANNOT TRANSITION
    // =========================================================

    @Test
    void update_ShouldRejectTransitionFromSuccess() {

        UUID id =
                UUID.randomUUID();

        Transaction t =
                transaction(
                        id,
                        invoice(
                                UUID.randomUUID(),
                                "INV",
                                InvoiceStatus.PENDING
                        ),
                        TransactionStatus.SUCCESS
                );

        TransactionUpdateRequest req =
                mock(TransactionUpdateRequest.class);

        when(req.status())
                .thenReturn(TransactionStatus.FAILED);

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        assertThrows(
                BadRequestException.class,
                () -> transactionService.update(id, req)
        );
    }


    @Test
    void update_ShouldRejectTransitionFromFailed() {

        UUID id =
                UUID.randomUUID();

        Transaction t =
                transaction(
                        id,
                        invoice(
                                UUID.randomUUID(),
                                "INV",
                                InvoiceStatus.PENDING
                        ),
                        TransactionStatus.FAILED
                );

        TransactionUpdateRequest req =
                mock(TransactionUpdateRequest.class);

        when(req.status())
                .thenReturn(TransactionStatus.SUCCESS);

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        assertThrows(
                BadRequestException.class,
                () -> transactionService.update(id, req)
        );
    }


    @Test
    void update_ShouldRejectTransitionFromCancelled() {

        UUID id =
                UUID.randomUUID();

        Transaction t =
                transaction(
                        id,
                        invoice(
                                UUID.randomUUID(),
                                "INV",
                                InvoiceStatus.PENDING
                        ),
                        TransactionStatus.CANCELLED
                );

        TransactionUpdateRequest req =
                mock(TransactionUpdateRequest.class);

        when(req.status())
                .thenReturn(TransactionStatus.SUCCESS);

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        assertThrows(
                BadRequestException.class,
                () -> transactionService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE OPTIONAL FIELDS
    // =========================================================

    @Test
    void update_ShouldUpdateGatewayAndNote() {

        UUID id = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        Transaction t =
                transaction(
                        id,
                        invoice,
                        TransactionStatus.PENDING
                );

        TransactionUpdateRequest req =
                mock(TransactionUpdateRequest.class);

        when(req.gatewayReference())
                .thenReturn("NEW-GW");

        when(req.note())
                .thenReturn("Updated note");

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        when(repo.save(t))
                .thenReturn(t);

        transactionService.update(
                id,
                req
        );

        assertEquals(
                "NEW-GW",
                t.getGatewayReference()
        );

        assertEquals(
                "Updated note",
                t.getNote()
        );
    }


    @Test
    void update_ShouldKeepFields_WhenRequestEmpty() {

        UUID id = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        Transaction t =
                transaction(
                        id,
                        invoice,
                        TransactionStatus.PENDING
                );

        t.setGatewayReference("OLD-GW");
        t.setNote("Old note");

        TransactionUpdateRequest req =
                new TransactionUpdateRequest(
                        null,
                        null,
                        null,
                        null
                );

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        when(repo.save(t))
                .thenReturn(t);

        transactionService.update(
                id,
                req
        );

        assertEquals(
                TransactionStatus.PENDING,
                t.getStatus()
        );

        assertEquals(
                "OLD-GW",
                t.getGatewayReference()
        );

        assertEquals(
                "Old note",
                t.getNote()
        );
    }


    // =========================================================
    // CONFIRM
    // =========================================================

    @Test
    void confirm_ShouldChangePendingToSuccess() {

        UUID id = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        Transaction t =
                transaction(
                        id,
                        invoice,
                        TransactionStatus.PENDING
                );

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        when(repo.save(t))
                .thenReturn(t);

        var result =
                transactionService.confirm(id);

        assertNotNull(result);

        assertEquals(
                TransactionStatus.SUCCESS,
                t.getStatus()
        );

        assertNotNull(
                t.getPaidAt()
        );

        verify(invoiceService)
                .recalculatePaidAmount(invoiceId);
    }


    // =========================================================
    // FAIL
    // =========================================================

    @Test
    void fail_ShouldChangePendingToFailed() {

        UUID id = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        Transaction t =
                transaction(
                        id,
                        invoice,
                        TransactionStatus.PENDING
                );

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(t));

        when(repo.save(t))
                .thenReturn(t);

        var result =
                transactionService.fail(id);

        assertNotNull(result);

        assertEquals(
                TransactionStatus.FAILED,
                t.getStatus()
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenTransactionMissing() {

        UUID id =
                UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.delete(id)
        );

        verify(repo, never())
                .deleteById(id);
    }


    @Test
    void delete_ShouldDeleteAndRecalculateInvoice() {

        UUID id = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                invoice(
                        invoiceId,
                        "INV001",
                        InvoiceStatus.PENDING
                );

        Transaction t =
                transaction(
                        id,
                        invoice,
                        TransactionStatus.PENDING
                );

        when(repo.existsById(id))
                .thenReturn(true);

        when(repo.findById(id))
                .thenReturn(Optional.of(t));

        transactionService.delete(id);

        verify(repo)
                .deleteById(id);

        verify(invoiceService)
                .recalculatePaidAmount(invoiceId);
    }
}