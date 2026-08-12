package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.invoice.InvoiceCreateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceUpdateRequest;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository repo;

    @Mock
    private InvoiceItemRepository itemRepo;

    @Mock
    private TransactionRepository transactionRepo;

    @Mock
    private ProfileRepository profileRepo;

    @Mock
    private CustomerVisitRepository visitRepo;

    @Mock
    private MedicalRecordRepository recordRepo;

    @Mock
    private StaffInfoRepository staffRepo;

    @Mock
    private MedicalServiceRepository serviceRepo;

    @Mock
    private AccountRepository accountRepo;

    @Mock
    private QueueTicketService queueTicketService;

    @Mock
    private TestRequestService testRequestService;

    @Mock
    private QueueTicketRepository queueTicketRepo;

    @Mock
    private TestRequestRepository testRequestRepo;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private InvoiceService invoiceService;


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturnInvoice_WhenExists() {

        UUID id = UUID.randomUUID();

        Invoice invoice = mock(Invoice.class);

        when(repo.findById(id))
                .thenReturn(Optional.of(invoice));

        assertSame(
                invoice,
                invoiceService.findById(id)
        );
    }

    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> invoiceService.findById(id)
        );
    }


    // =========================================================
    // GET
    // =========================================================

    @Test
    void get_ShouldThrow_WhenInvoiceMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> invoiceService.get(id)
        );
    }

    @Test
    void get_ShouldReturnInvoiceWithTransactionIds() {

        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PENDING)
                        .items(new ArrayList<>())
                        .build();

        Transaction tx =
                mock(Transaction.class);

        when(tx.getTransactionId())
                .thenReturn(transactionId);

        when(repo.findById(id))
                .thenReturn(Optional.of(invoice));

        when(transactionRepo.findByInvoice_InvoiceId(id))
                .thenReturn(List.of(tx));

        var result = invoiceService.get(id);

        assertNotNull(result);

        verify(transactionRepo)
                .findByInvoice_InvoiceId(id);
    }


    // =========================================================
    // SEARCH - BASIC
    // Specification body để Integration Test cover sâu hơn.
    // =========================================================

    @Test
    void search_ShouldUseDefaultCreatedAtSort_WhenPageableHasNoSort() {

        var pageable =
                PageRequest.of(0, 10);

        when(
                repo.findAll(
                        any(Specification.class),
                        any(Pageable.class)
                )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        var result = invoiceService.search(
                null,
                null,
                "  ABC  ",
                "  XET NGHIEM ",
                null,
                null,
                pageable
        );

        assertNotNull(result);

        verify(repo).findAll(
                any(Specification.class),
                argThat((Pageable p) ->
                        p.getSort().isSorted()
                                && p.getSort().getOrderFor("createdAt") != null
                )
        );
    }


    // =========================================================
    // CREATE - VISIT NOT FOUND
    // =========================================================

    @Test
    void create_ShouldThrow_WhenVisitDoesNotExist() {

        UUID visitId = UUID.randomUUID();

        InvoiceCreateRequest req =
                new InvoiceCreateRequest(
                        null,
                        visitId,
                        null,
                        LocalDate.now(),
                        null,
                        null,
                        "test",
                        null,
                        null
                );

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> invoiceService.create(req)
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // CREATE - MEDICAL RECORD NOT FOUND
    // =========================================================

    @Test
    void create_ShouldThrow_WhenMedicalRecordDoesNotExist() {

        UUID recordId = UUID.randomUUID();

        InvoiceCreateRequest req =
                new InvoiceCreateRequest(
                        null,
                        null,
                        recordId,
                        LocalDate.now(),
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> invoiceService.create(req)
        );
    }


    // =========================================================
    // CREATE - GUEST / NO ITEMS
    // =========================================================

    @Test
    void create_ShouldCreateGuestInvoiceWithoutItems() {

        InvoiceCreateRequest req =
                new InvoiceCreateRequest(
                        null,
                        null,
                        null,
                        LocalDate.now().plusDays(1),
                        null,
                        null,
                        "Guest invoice",
                        null,
                        null
                );

        when(repo.existsByInvoiceCode(anyString()))
                .thenReturn(false);

        when(repo.save(any(Invoice.class)))
                .thenAnswer(i -> {
                    Invoice invoice = i.getArgument(0);

                    if (invoice.getInvoiceId() == null) {
                        invoice.setInvoiceId(UUID.randomUUID());
                    }

                    return invoice;
                });

        when(staffRepo.findAllBySystemRoleIn(anyList()))
                .thenReturn(List.of());

        var result = invoiceService.create(req);

        assertNotNull(result);

        // create() save 2 lần
        verify(repo, times(2))
                .save(any(Invoice.class));
    }


    // =========================================================
    // CREATE - CUSTOMER + VISIT + RECORD + STAFF
    // =========================================================

    @Test
    void create_ShouldAttachRelatedEntities() {

        UUID customerId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        Profile customer = mock(Profile.class);
        CustomerVisit visit = mock(CustomerVisit.class);
        MedicalRecord record = mock(MedicalRecord.class);
        StaffInfo staff = mock(StaffInfo.class);

        InvoiceCreateRequest req =
                new InvoiceCreateRequest(
                        customerId,
                        visitId,
                        recordId,
                        LocalDate.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "Invoice",
                        staffId,
                        null
                );

        when(profileRepo.findById(customerId))
                .thenReturn(Optional.of(customer));

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(recordRepo.findById(recordId))
                .thenReturn(Optional.of(record));

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(repo.existsByInvoiceCode(anyString()))
                .thenReturn(false);

        when(repo.save(any(Invoice.class)))
                .thenAnswer(i -> {
                    Invoice invoice = i.getArgument(0);

                    if (invoice.getInvoiceId() == null) {
                        invoice.setInvoiceId(UUID.randomUUID());
                    }

                    return invoice;
                });

        when(staffRepo.findAllBySystemRoleIn(anyList()))
                .thenReturn(List.of());

        var result = invoiceService.create(req);

        assertNotNull(result);

        verify(repo, times(2))
                .save(argThat(invoice ->
                        invoice.getCustomer() == customer
                                && invoice.getVisit() == visit
                                && invoice.getMedicalRecord() == record
                                && invoice.getIssuedBy() == staff
                ));
    }


    // =========================================================
    // CREATE - ITEM + RECALCULATE TOTAL
    // =========================================================

    @Test
    void create_ShouldBuildItemAndCalculateTotal() {

        UUID serviceId = UUID.randomUUID();

        MedicalService service =
                mock(MedicalService.class);

        InvoiceItemCreateRequest itemReq =
                mock(InvoiceItemCreateRequest.class);

        when(itemReq.serviceId())
                .thenReturn(serviceId);

        when(itemReq.unitPrice())
                .thenReturn(new BigDecimal("100000"));

        when(itemReq.quantity())
                .thenReturn(2);

        when(itemReq.serviceSnapshot())
                .thenReturn("Xet nghiem mau");

        when(itemReq.serviceCodeSnapshot())
                .thenReturn("XN01");

        when(serviceRepo.findById(serviceId))
                .thenReturn(Optional.of(service));

        InvoiceCreateRequest req =
                new InvoiceCreateRequest(
                        null,
                        null,
                        null,
                        LocalDate.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        List.of(itemReq)
                );

        when(repo.existsByInvoiceCode(anyString()))
                .thenReturn(false);

        when(repo.save(any(Invoice.class)))
                .thenAnswer(i -> {
                    Invoice invoice = i.getArgument(0);
                    invoice.setInvoiceId(UUID.randomUUID());
                    return invoice;
                });

        when(staffRepo.findAllBySystemRoleIn(anyList()))
                .thenReturn(List.of());

        invoiceService.create(req);

        verify(repo, atLeastOnce()).save(argThat(invoice ->
                new BigDecimal("200000")
                        .compareTo(invoice.getSubtotal()) == 0
                        &&
                        new BigDecimal("200000")
                                .compareTo(invoice.getTotalAmount()) == 0
        ));
    }


    // =========================================================
    // CREATE - TOTAL NEGATIVE
    // =========================================================

    @Test
    void create_ShouldThrow_WhenDiscountMakesTotalNegative() {

        InvoiceCreateRequest req =
                new InvoiceCreateRequest(
                        null,
                        null,
                        null,
                        LocalDate.now(),
                        new BigDecimal("1000"),
                        BigDecimal.ZERO,
                        null,
                        null,
                        null
                );

        when(repo.existsByInvoiceCode(anyString()))
                .thenReturn(false);

        when(repo.save(any(Invoice.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertThrows(
                BadRequestException.class,
                () -> invoiceService.create(req)
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void update_ShouldReject_WhenInvoiceNotPending() {

        UUID id = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PAID)
                        .items(new ArrayList<>())
                        .build();

        InvoiceUpdateRequest req =
                mock(InvoiceUpdateRequest.class);

        when(repo.findById(id))
                .thenReturn(Optional.of(invoice));

        assertThrows(
                ConflictException.class,
                () -> invoiceService.update(id, req)
        );
    }


    @Test
    void update_ShouldUpdateSimpleFields_WhenPending() {

        UUID id = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PENDING)
                        .subtotal(BigDecimal.ZERO)
                        .discount(BigDecimal.ZERO)
                        .tax(BigDecimal.ZERO)
                        .totalAmount(BigDecimal.ZERO)
                        .items(new ArrayList<>())
                        .build();

        InvoiceUpdateRequest req =
                mock(InvoiceUpdateRequest.class);

        LocalDate dueDate =
                LocalDate.now().plusDays(5);

        when(req.dueDate())
                .thenReturn(dueDate);

        when(req.discount())
                .thenReturn(BigDecimal.ZERO);

        when(req.tax())
                .thenReturn(new BigDecimal("5"));

        when(req.note())
                .thenReturn("updated");

        when(repo.findById(id))
                .thenReturn(Optional.of(invoice));

        when(repo.save(invoice))
                .thenReturn(invoice);

        var result =
                invoiceService.update(id, req);

        assertNotNull(result);

        assertEquals(dueDate, invoice.getDueDate());
        assertEquals("updated", invoice.getNote());

        assertEquals(
                0,
                new BigDecimal("5")
                        .compareTo(invoice.getTotalAmount())
        );
    }


    // =========================================================
    // ISSUE
    // =========================================================

    @Test
    void issue_ShouldReject_WhenNotPending() {

        UUID id = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PAID)
                        .items(new ArrayList<>())
                        .build();

        when(repo.findById(id))
                .thenReturn(Optional.of(invoice));

        assertThrows(
                ConflictException.class,
                () -> invoiceService.issue(id)
        );
    }

    @Test
    void issue_ShouldReject_WhenNoItems() {

        UUID id = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PENDING)
                        .items(new ArrayList<>())
                        .build();

        when(repo.findById(id))
                .thenReturn(Optional.of(invoice));

        assertThrows(
                BadRequestException.class,
                () -> invoiceService.issue(id)
        );
    }

    @Test
    void issue_ShouldSave_WhenPendingAndHasItems() {

        UUID id = UUID.randomUUID();

        InvoiceItem item =
                mock(InvoiceItem.class);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PENDING)
                        .items(new ArrayList<>(List.of(item)))
                        .build();

        when(repo.findById(id))
                .thenReturn(Optional.of(invoice));

        when(repo.save(invoice))
                .thenReturn(invoice);

        assertNotNull(
                invoiceService.issue(id)
        );

        verify(repo)
                .save(invoice);
    }


    // =========================================================
    // CANCEL
    // =========================================================

    @Test
    void cancel_ShouldThrow_WhenInvoiceMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> invoiceService.cancel(id)
        );
    }

    @Test
    void cancel_ShouldRejectPaidInvoice() {

        UUID id = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PAID)
                        .build();

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(invoice));

        assertThrows(
                ConflictException.class,
                () -> invoiceService.cancel(id)
        );
    }

    @Test
    void cancel_ShouldReject_WhenSuccessfulTransactionExists() {

        UUID id = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PENDING)
                        .build();

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getStatus())
                .thenReturn(TransactionStatus.SUCCESS);

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(invoice));

        when(transactionRepo.findByInvoice_InvoiceId(id))
                .thenReturn(List.of(transaction));

        assertThrows(
                ConflictException.class,
                () -> invoiceService.cancel(id)
        );
    }

    @Test
    void cancel_ShouldSetCancelled_WhenValid() {

        UUID id = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PENDING)
                        .build();

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(invoice));

        when(transactionRepo.findByInvoice_InvoiceId(id))
                .thenReturn(List.of());

        when(repo.save(invoice))
                .thenReturn(invoice);

        invoiceService.cancel(id);

        assertEquals(
                InvoiceStatus.CANCELLED,
                invoice.getStatus()
        );
    }


    // =========================================================
    // PAY
    // =========================================================

    @Test
    void pay_ShouldThrow_WhenInvoiceMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> invoiceService.pay(
                        id,
                        null
                )
        );
    }


    @Test
    void pay_ShouldRejectCancelledInvoice() {

        UUID id = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.CANCELLED)
                        .build();

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(invoice));

        assertThrows(
                ConflictException.class,
                () -> invoiceService.pay(
                        id,
                        null
                )
        );
    }


    @Test
    void pay_ShouldRejectAlreadyPaidInvoice() {

        UUID id = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PAID)
                        .build();

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(invoice));

        when(
                transactionRepo
                        .findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                                id,
                                TransactionStatus.SUCCESS
                        )
        ).thenReturn(Optional.empty());

        ConflictException ex =
                assertThrows(
                        ConflictException.class,
                        () -> invoiceService.pay(
                                id,
                                null
                        )
                );

        assertTrue(
                ex.getMessage()
                        .contains("nhan vien khac")
        );
    }


    @Test
    void pay_ShouldSetPaidAndCreateCashTransaction() {

        UUID id = UUID.randomUUID();
        UUID cashierId = UUID.randomUUID();

        StaffInfo cashier =
                mock(StaffInfo.class);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .invoiceCode("INV-001")
                        .status(InvoiceStatus.PENDING)
                        .totalAmount(new BigDecimal("200000"))
                        .paidAmount(BigDecimal.ZERO)
                        .items(new ArrayList<>())
                        .build();

        when(repo.findByIdForUpdate(id))
                .thenReturn(Optional.of(invoice));

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(staffRepo.findById(cashierId))
                .thenReturn(Optional.of(cashier));

        when(
                transactionRepo
                        .findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                                id,
                                TransactionStatus.SUCCESS
                        )
        ).thenReturn(Optional.empty());

        var result =
                invoiceService.pay(
                        id,
                        cashierId
                );

        assertNotNull(result);

        assertEquals(
                InvoiceStatus.PAID,
                invoice.getStatus()
        );

        assertEquals(
                0,
                invoice.getTotalAmount()
                        .compareTo(invoice.getPaidAmount())
        );

        verify(transactionRepo)
                .save(argThat(tx ->
                        tx.getInvoice() == invoice
                                && tx.getStatus() == TransactionStatus.SUCCESS
                                && tx.getPaymentMethod() == PaymentMethod.CASH
                                && tx.getReceivedBy() == cashier
                ));
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> invoiceService.delete(id)
        );
    }

    @Test
    void delete_ShouldDelete_WhenExists() {

        UUID id = UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(true);

        invoiceService.delete(id);

        verify(repo)
                .deleteById(id);
    }


    // =========================================================
    // RECALCULATE PAID AMOUNT
    // =========================================================

    @Test
    void recalculatePaidAmount_ShouldSumOnlySuccessfulTransactions() {

        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .status(InvoiceStatus.PENDING)
                        .totalAmount(new BigDecimal("100"))
                        .paidAmount(BigDecimal.ZERO)
                        .build();

        Transaction success =
                mock(Transaction.class);

        when(success.getStatus())
                .thenReturn(TransactionStatus.SUCCESS);

        when(success.getAmount())
                .thenReturn(new BigDecimal("100"));

        Transaction failed =
                mock(Transaction.class);

        when(failed.getStatus())
                .thenReturn(TransactionStatus.FAILED);

        when(repo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(transactionRepo.findByInvoice_InvoiceId(invoiceId))
                .thenReturn(List.of(success, failed));

        when(repo.save(invoice))
                .thenReturn(invoice);

        invoiceService.recalculatePaidAmount(invoiceId);

        assertEquals(
                0,
                new BigDecimal("100")
                        .compareTo(invoice.getPaidAmount())
        );

        assertEquals(
                InvoiceStatus.PAID,
                invoice.getStatus()
        );
    }


    @Test
    void recalculatePaidAmount_ShouldNotChangeCancelledStatus() {

        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .status(InvoiceStatus.CANCELLED)
                        .totalAmount(new BigDecimal("100"))
                        .build();

        when(repo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(transactionRepo.findByInvoice_InvoiceId(invoiceId))
                .thenReturn(List.of());

        invoiceService.recalculatePaidAmount(invoiceId);

        assertEquals(
                InvoiceStatus.CANCELLED,
                invoice.getStatus()
        );

        verify(repo, never())
                .save(invoice);
    }


    @Test
    void recalculatePaidAmount_ShouldMovePaidBackToPending_WhenAmountInsufficient() {

        UUID invoiceId = UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .status(InvoiceStatus.PAID)
                        .totalAmount(new BigDecimal("100"))
                        .build();

        Transaction success =
                mock(Transaction.class);

        when(success.getStatus())
                .thenReturn(TransactionStatus.SUCCESS);

        when(success.getAmount())
                .thenReturn(new BigDecimal("50"));

        when(repo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(transactionRepo.findByInvoice_InvoiceId(invoiceId))
                .thenReturn(List.of(success));

        when(repo.save(invoice))
                .thenReturn(invoice);

        invoiceService.recalculatePaidAmount(invoiceId);

        assertEquals(
                InvoiceStatus.PENDING,
                invoice.getStatus()
        );
    }


    // =========================================================
    // RECEIPT DETAIL
    // =========================================================

    @Test
    void getReceiptDetail_ShouldThrow_WhenInvoiceMissing() {

        UUID invoiceId = UUID.randomUUID();

        when(repo.findById(invoiceId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> invoiceService.getReceiptDetail(
                        invoiceId,
                        UUID.randomUUID()
                )
        );
    }


    @Test
    void getReceiptDetail_ShouldReject_WhenInvoiceBelongsToDifferentCustomer() {

        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        Profile customer =
                mock(Profile.class);

        when(customer.getProfileId())
                .thenReturn(otherId);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .customer(customer)
                        .status(InvoiceStatus.PAID)
                        .build();

        when(repo.findById(invoiceId))
                .thenReturn(Optional.of(invoice));

        assertThrows(
                ResourceNotFoundException.class,
                () -> invoiceService.getReceiptDetail(
                        invoiceId,
                        customerId
                )
        );
    }


    @Test
    void getReceiptDetail_ShouldReject_WhenInvoiceNotPaid() {

        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Profile customer =
                mock(Profile.class);

        when(customer.getProfileId())
                .thenReturn(customerId);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .customer(customer)
                        .status(InvoiceStatus.PENDING)
                        .build();

        when(repo.findById(invoiceId))
                .thenReturn(Optional.of(invoice));

        assertThrows(
                ConflictException.class,
                () -> invoiceService.getReceiptDetail(
                        invoiceId,
                        customerId
                )
        );
    }


    @Test
    void getReceiptDetail_ShouldReturnReceipt_WhenPaidAndOwnedByCustomer() {

        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Profile customer =
                mock(Profile.class);

        when(customer.getProfileId())
                .thenReturn(customerId);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .customer(customer)
                        .status(InvoiceStatus.PAID)
                        .items(new ArrayList<>())
                        .build();

        when(repo.findById(invoiceId))
                .thenReturn(Optional.of(invoice));

        var result =
                invoiceService.getReceiptDetail(
                        invoiceId,
                        customerId
                );

        assertNotNull(result);
    }


    // =========================================================
    // PAYMENT HISTORY - BASIC PATH
    // Specification sâu để Integration Test cover.
    // =========================================================

    @Test
    void getPaymentHistoryForPatient_ShouldReturnEmptyPage() {

        UUID customerId = UUID.randomUUID();

        var pageable =
                PageRequest.of(0, 10);

        when(
                repo.findAll(
                        any(Specification.class),
                        any(org.springframework.data.domain.Pageable.class)
                )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        var result =
                invoiceService.getPaymentHistoryForPatient(
                        customerId,
                        null,
                        null,
                        null,
                        pageable
                );

        assertNotNull(result);
    }

    // =========================================================
// WORKFLOW - EXAMINATION IS BLOCKED WHEN WORKFLOW ACTIVE
// =========================================================

    @Test
    void pay_ShouldBlockNewExaminationQueue_WhenWorkflowAlreadyActive() {

        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID createdTicketId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId())
                .thenReturn(visitId);

        Department department = mock(Department.class);
        when(department.getDepartmentId())
                .thenReturn(departmentId);

        MedicalService service = mock(MedicalService.class);

        when(service.getServiceId())
                .thenReturn(serviceId);

        when(service.getDepartmentType())
                .thenReturn(DepartmentType.EXAMINATION);

        when(service.getDepartment())
                .thenReturn(department);

        QueueTicket activeTicket = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.WAITING)
                .build();

        QueueTicket newlyCreated = QueueTicket.builder()
                .ticketId(createdTicketId)
                .status(QueueStatus.WAITING)
                .build();

        InvoiceItem item = InvoiceItem.builder()
                .itemId(UUID.randomUUID())
                .service(service)
                .build();

        Invoice invoice = Invoice.builder()
                .invoiceId(invoiceId)
                .invoiceCode("INV-EXAM-2")
                .status(InvoiceStatus.PENDING)
                .totalAmount(BigDecimal.ONE)
                .visit(visit)
                .items(new ArrayList<>(List.of(item)))
                .build();

        when(repo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                transactionRepo
                        .findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                                invoiceId,
                                TransactionStatus.SUCCESS
                        )
        ).thenReturn(Optional.empty());

        when(repo.getWithDetailsByInvoiceId(invoiceId))
                .thenReturn(Optional.of(invoice));

        /*
         * Có queue đang active -> workflowActivated = true.
         */
        when(queueTicketRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(activeTicket));

        var response =
                mock(org.example.doansummer2026.dto.queueTicket.QueueTicketResponse.class);

        when(response.ticketId())
                .thenReturn(createdTicketId);

        when(queueTicketService.create(any()))
                .thenReturn(response);

        when(queueTicketRepo.findById(createdTicketId))
                .thenReturn(Optional.of(newlyCreated));

        invoiceService.pay(
                invoiceId,
                null
        );

        assertEquals(
                QueueStatus.BLOCKED,
                newlyCreated.getStatus()
        );

        verify(queueTicketRepo)
                .save(newlyCreated);
    }


// =========================================================
// WORKFLOW - SKIP NULL SERVICE
// =========================================================

    @Test
    void pay_ShouldIgnoreInvoiceItem_WhenServiceIsNull() {

        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId())
                .thenReturn(visitId);

        InvoiceItem item = InvoiceItem.builder()
                .itemId(UUID.randomUUID())
                .service(null)
                .build();

        Invoice invoice = Invoice.builder()
                .invoiceId(invoiceId)
                .invoiceCode("INV-NULL")
                .status(InvoiceStatus.PENDING)
                .totalAmount(BigDecimal.ONE)
                .visit(visit)
                .items(new ArrayList<>(List.of(item)))
                .build();

        when(repo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                transactionRepo
                        .findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                                invoiceId,
                                TransactionStatus.SUCCESS
                        )
        ).thenReturn(Optional.empty());

        when(repo.getWithDetailsByInvoiceId(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(queueTicketRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        when(testRequestRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        invoiceService.pay(invoiceId, null);

        verifyNoInteractions(queueTicketService);
        verifyNoInteractions(testRequestService);
    }


// =========================================================
// WORKFLOW - EXAM SERVICE WITHOUT DEPARTMENT
// =========================================================

    @Test
    void pay_ShouldSkipExaminationService_WhenDepartmentIsNull() {

        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId())
                .thenReturn(visitId);

        MedicalService service = mock(MedicalService.class);

        when(service.getDepartmentType())
                .thenReturn(DepartmentType.EXAMINATION);

        InvoiceItem item = InvoiceItem.builder()
                .itemId(UUID.randomUUID())
                .service(service)
                .build();

        Invoice invoice = Invoice.builder()
                .invoiceId(invoiceId)
                .invoiceCode("INV-NODEPT")
                .status(InvoiceStatus.PENDING)
                .totalAmount(BigDecimal.ONE)
                .visit(visit)
                .items(new ArrayList<>(List.of(item)))
                .build();

        when(repo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                transactionRepo
                        .findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                                invoiceId,
                                TransactionStatus.SUCCESS
                        )
        ).thenReturn(Optional.empty());

        when(repo.getWithDetailsByInvoiceId(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(queueTicketRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        when(testRequestRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        invoiceService.pay(invoiceId, null);

        verifyNoInteractions(queueTicketService);
    }

    // =========================================================
// WORKFLOW - PARACLINICAL FIRST ITEM
// =========================================================

    @Test
    void pay_ShouldCreateTestRequest_WhenServiceIsParaclinical() {

        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId())
                .thenReturn(visitId);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .build();

        StaffInfo issuedBy = mock(StaffInfo.class);
        when(issuedBy.getStaffId())
                .thenReturn(staffId);

        MedicalService service = mock(MedicalService.class);

        when(service.getServiceId())
                .thenReturn(serviceId);

        when(service.getDepartmentType())
                .thenReturn(DepartmentType.LABORATORY);

        when(service.getName())
                .thenReturn("Xet nghiem mau");

        InvoiceItem item = InvoiceItem.builder()
                .itemId(UUID.randomUUID())
                .service(service)
                .note("XN mau")
                .build();

        Invoice invoice = Invoice.builder()
                .invoiceId(invoiceId)
                .invoiceCode("INV-LAB")
                .status(InvoiceStatus.PENDING)
                .totalAmount(BigDecimal.ONE)
                .visit(visit)
                .medicalRecord(record)
                .issuedBy(issuedBy)
                .items(new ArrayList<>(List.of(item)))
                .build();

        when(repo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                transactionRepo
                        .findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                                invoiceId,
                                TransactionStatus.SUCCESS
                        )
        ).thenReturn(Optional.empty());

        when(repo.getWithDetailsByInvoiceId(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(queueTicketRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        when(testRequestRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        invoiceService.pay(
                invoiceId,
                staffId
        );

        verify(testRequestService)
                .createFromPaidInvoice(
                        eq(visitId),
                        eq(recordId),
                        eq(serviceId),
                        eq(staffId),
                        eq("XN mau"),
                        eq(item.getItemId())
                );
    }

    // =========================================================
// WORKFLOW - RECORD ID FALLBACK FROM VISIT
// =========================================================

    @Test
    void pay_ShouldResolveMedicalRecordFromVisit_WhenInvoiceRecordIsNull() {

        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId())
                .thenReturn(visitId);

        MedicalRecord record = MedicalRecord.builder()
                .recordId(recordId)
                .build();

        MedicalService service = mock(MedicalService.class);

        when(service.getServiceId())
                .thenReturn(serviceId);

        when(service.getDepartmentType())
                .thenReturn(DepartmentType.LABORATORY);

        InvoiceItem item = InvoiceItem.builder()
                .itemId(UUID.randomUUID())
                .service(service)
                .build();

        Invoice invoice = Invoice.builder()
                .invoiceId(invoiceId)
                .invoiceCode("INV-FALLBACK")
                .status(InvoiceStatus.PENDING)
                .totalAmount(BigDecimal.ONE)
                .visit(visit)
                .items(new ArrayList<>(List.of(item)))
                .build();

        when(repo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                transactionRepo
                        .findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                                invoiceId,
                                TransactionStatus.SUCCESS
                        )
        ).thenReturn(Optional.empty());

        when(repo.getWithDetailsByInvoiceId(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(
                recordRepo.findFirstByVisit_VisitIdOrderByCreatedAtDesc(
                        visitId
                )
        ).thenReturn(Optional.of(record));

        when(queueTicketRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        when(testRequestRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        invoiceService.pay(invoiceId, null);

        verify(testRequestService)
                .createFromPaidInvoice(
                        eq(visitId),
                        eq(recordId),
                        eq(serviceId),
                        isNull(),
                        isNull(),
                        eq(item.getItemId())
                );
    }

    // =========================================================
// WORKFLOW - REQUESTED BY FALLBACK FROM PAYMENT TRANSACTION
// =========================================================

    @Test
    void pay_ShouldUseCashierAsRequester_WhenInvoiceIssuedByIsNull() {

        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID cashierId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId())
                .thenReturn(visitId);

        StaffInfo cashier = mock(StaffInfo.class);
        when(cashier.getStaffId())
                .thenReturn(cashierId);

        Transaction payment = mock(Transaction.class);
        when(payment.getReceivedBy())
                .thenReturn(cashier);

        MedicalService service = mock(MedicalService.class);

        when(service.getServiceId())
                .thenReturn(serviceId);

        when(service.getDepartmentType())
                .thenReturn(DepartmentType.LABORATORY);

        InvoiceItem item = InvoiceItem.builder()
                .itemId(UUID.randomUUID())
                .service(service)
                .build();

        Invoice invoice = Invoice.builder()
                .invoiceId(invoiceId)
                .invoiceCode("INV-CASHIER")
                .status(InvoiceStatus.PENDING)
                .totalAmount(BigDecimal.ONE)
                .visit(visit)
                .items(new ArrayList<>(List.of(item)))
                .build();

        when(repo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(repo.save(invoice))
                .thenReturn(invoice);

        /*
         * Lần đầu trong pay(): chưa có success -> tạo transaction.
         * Sau khi load workflow: trả payment để lấy receivedBy.
         */
        when(
                transactionRepo
                        .findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                                invoiceId,
                                TransactionStatus.SUCCESS
                        )
        ).thenReturn(
                Optional.empty(),
                Optional.of(payment)
        );

        when(repo.getWithDetailsByInvoiceId(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(queueTicketRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of());

        when(testRequestRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of());

        invoiceService.pay(
                invoiceId,
                cashierId
        );

        verify(testRequestService)
                .createFromPaidInvoice(
                        eq(visitId),
                        isNull(),
                        eq(serviceId),
                        eq(cashierId),
                        isNull(),
                        eq(item.getItemId())
                );
    }
    // =========================================================
// WORKFLOW - BLOCK PARACLINICAL TEST WHEN WORKFLOW ACTIVE
// =========================================================

    @Test
    void pay_ShouldBlockParaclinicalTest_WhenWorkflowAlreadyActive() {

        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID testRequestId = UUID.randomUUID();
        UUID queueId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId())
                .thenReturn(visitId);

        QueueTicket existingActive = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.WAITING)
                .build();

        QueueTicket labQueue = QueueTicket.builder()
                .ticketId(queueId)
                .status(QueueStatus.WAITING)
                .build();

        MedicalService service = mock(MedicalService.class);

        when(service.getServiceId())
                .thenReturn(serviceId);

        when(service.getDepartmentType())
                .thenReturn(DepartmentType.LABORATORY);

        InvoiceItem item = InvoiceItem.builder()
                .itemId(UUID.randomUUID())
                .service(service)
                .build();

        Invoice invoice = Invoice.builder()
                .invoiceId(invoiceId)
                .invoiceCode("INV-BLOCK-LAB")
                .status(InvoiceStatus.PENDING)
                .totalAmount(BigDecimal.ONE)
                .visit(visit)
                .items(new ArrayList<>(List.of(item)))
                .build();

        var testResponse =
                mock(org.example.doansummer2026.dto.testRequest.TestRequestResponse.class);

        when(testResponse.testRequestId())
                .thenReturn(testRequestId);

        TestRequest blocked = TestRequest.builder()
                .testRequestId(testRequestId)
                .status(TestRequestStatus.PENDING)
                .queueTicket(labQueue)
                .build();

        when(repo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                transactionRepo
                        .findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                                invoiceId,
                                TransactionStatus.SUCCESS
                        )
        ).thenReturn(Optional.empty());

        when(repo.getWithDetailsByInvoiceId(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(queueTicketRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(existingActive));

        when(testRequestService.createFromPaidInvoice(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(testResponse);

        when(testRequestRepo.findById(testRequestId))
                .thenReturn(Optional.of(blocked));

        when(testRequestRepo.findAllByQueueTicket_TicketId(queueId))
                .thenReturn(List.of(blocked));

        invoiceService.pay(
                invoiceId,
                null
        );

        assertEquals(
                TestRequestStatus.BLOCKED,
                blocked.getStatus()
        );

        assertEquals(
                QueueStatus.BLOCKED,
                labQueue.getStatus()
        );

        verify(testRequestRepo)
                .save(blocked);

        verify(queueTicketRepo)
                .save(labQueue);
    }
    // =========================================================
// WORKFLOW - KEEP TEST ACTIVE WHEN SHARING ACTIVE QUEUE
// =========================================================

    @Test
    void pay_ShouldNotBlockTest_WhenQueueHasAnotherActiveTest() {

        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID testRequestId = UUID.randomUUID();
        UUID queueId = UUID.randomUUID();

        CustomerVisit visit = mock(CustomerVisit.class);
        when(visit.getVisitId())
                .thenReturn(visitId);

        QueueTicket existingWorkflow = QueueTicket.builder()
                .ticketId(UUID.randomUUID())
                .status(QueueStatus.WAITING)
                .build();

        QueueTicket sharedQueue = QueueTicket.builder()
                .ticketId(queueId)
                .status(QueueStatus.WAITING)
                .build();

        MedicalService service = mock(MedicalService.class);

        when(service.getServiceId())
                .thenReturn(serviceId);

        when(service.getDepartmentType())
                .thenReturn(DepartmentType.LABORATORY);

        InvoiceItem item = InvoiceItem.builder()
                .itemId(UUID.randomUUID())
                .service(service)
                .build();

        Invoice invoice = Invoice.builder()
                .invoiceId(invoiceId)
                .invoiceCode("INV-SHARED")
                .status(InvoiceStatus.PENDING)
                .totalAmount(BigDecimal.ONE)
                .visit(visit)
                .items(new ArrayList<>(List.of(item)))
                .build();

        var response =
                mock(org.example.doansummer2026.dto.testRequest.TestRequestResponse.class);

        when(response.testRequestId())
                .thenReturn(testRequestId);

        TestRequest blocked = TestRequest.builder()
                .testRequestId(testRequestId)
                .status(TestRequestStatus.PENDING)
                .queueTicket(sharedQueue)
                .build();

        TestRequest otherActive = TestRequest.builder()
                .testRequestId(UUID.randomUUID())
                .status(TestRequestStatus.IN_PROGRESS)
                .queueTicket(sharedQueue)
                .build();

        when(repo.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                transactionRepo
                        .findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                                invoiceId,
                                TransactionStatus.SUCCESS
                        )
        ).thenReturn(Optional.empty());

        when(repo.getWithDetailsByInvoiceId(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(queueTicketRepo.findAllByVisit_VisitId(visitId))
                .thenReturn(List.of(existingWorkflow));

        when(testRequestService.createFromPaidInvoice(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(response);

        when(testRequestRepo.findById(testRequestId))
                .thenReturn(Optional.of(blocked));

        when(testRequestRepo.findAllByQueueTicket_TicketId(queueId))
                .thenReturn(List.of(
                        blocked,
                        otherActive
                ));

        invoiceService.pay(
                invoiceId,
                null
        );

        assertEquals(
                TestRequestStatus.PENDING,
                blocked.getStatus()
        );

        assertEquals(
                QueueStatus.WAITING,
                sharedQueue.getStatus()
        );

        verify(testRequestRepo, never())
                .save(blocked);

        verify(queueTicketRepo, never())
                .save(sharedQueue);
    }
    @Test
    void create_ShouldNotifyCashierWithCustomerName() {

        UUID customerId = UUID.randomUUID();
        UUID cashierProfileId = UUID.randomUUID();

        Profile customer = mock(Profile.class);
        when(customer.getFullName())
                .thenReturn("Nguyen Van Customer");

        Profile cashierProfile = mock(Profile.class);
        when(cashierProfile.getProfileId())
                .thenReturn(cashierProfileId);

        StaffInfo cashier = mock(StaffInfo.class);
        when(cashier.getProfile())
                .thenReturn(cashierProfile);

        when(profileRepo.findById(customerId))
                .thenReturn(Optional.of(customer));

        when(repo.existsByInvoiceCode(anyString()))
                .thenReturn(false);

        when(repo.save(any(Invoice.class)))
                .thenAnswer(i -> {
                    Invoice inv = i.getArgument(0);
                    inv.setInvoiceId(UUID.randomUUID());
                    return inv;
                });

        when(staffRepo.findAllBySystemRoleIn(anyList()))
                .thenReturn(List.of(cashier));

        InvoiceCreateRequest req =
                new InvoiceCreateRequest(
                        customerId,
                        null,
                        null,
                        LocalDate.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null
                );

        invoiceService.create(req);

        verify(notificationService)
                .create(argThat(n ->
                        cashierProfileId.equals(n.recipientId())
                                && n.content()
                                .contains("Nguyen Van Customer")
                                && "Invoice".equals(n.relatedEntity())
                ));
    }


    @Test
    void create_ShouldUseGuestNameForCashierNotification() {

        UUID visitId = UUID.randomUUID();

        Appointment appointment = mock(Appointment.class);

        when(appointment.getGuestFullName())
                .thenReturn("Guest ABC");

        CustomerVisit visit = mock(CustomerVisit.class);

        when(visit.getAppointment())
                .thenReturn(appointment);

        Profile cashierProfile = mock(Profile.class);
        when(cashierProfile.getProfileId())
                .thenReturn(UUID.randomUUID());

        StaffInfo cashier = mock(StaffInfo.class);
        when(cashier.getProfile())
                .thenReturn(cashierProfile);

        when(visitRepo.findById(visitId))
                .thenReturn(Optional.of(visit));

        when(repo.existsByInvoiceCode(anyString()))
                .thenReturn(false);

        when(repo.save(any(Invoice.class)))
                .thenAnswer(i -> {
                    Invoice inv = i.getArgument(0);
                    inv.setInvoiceId(UUID.randomUUID());
                    return inv;
                });

        when(staffRepo.findAllBySystemRoleIn(anyList()))
                .thenReturn(List.of(cashier));

        InvoiceCreateRequest req =
                new InvoiceCreateRequest(
                        null,
                        visitId,
                        null,
                        LocalDate.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null
                );

        invoiceService.create(req);

        verify(notificationService)
                .create(argThat(n ->
                        n.content().contains("Guest ABC")
                ));
    }


    @Test
    void create_ShouldNotNotifyCashier_WhenCashierProfileIsNull() {

        StaffInfo cashier =
                mock(StaffInfo.class);

        when(repo.existsByInvoiceCode(anyString()))
                .thenReturn(false);

        when(repo.save(any(Invoice.class)))
                .thenAnswer(i -> {
                    Invoice inv = i.getArgument(0);
                    inv.setInvoiceId(UUID.randomUUID());
                    return inv;
                });

        when(staffRepo.findAllBySystemRoleIn(anyList()))
                .thenReturn(List.of(cashier));

        InvoiceCreateRequest req =
                new InvoiceCreateRequest(
                        null,
                        null,
                        null,
                        LocalDate.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null
                );

        invoiceService.create(req);

        verify(notificationService, never())
                .create(any());
    }


    @Test
    void create_ShouldIgnoreNotificationException() {

        Profile cashierProfile = mock(Profile.class);
        when(cashierProfile.getProfileId())
                .thenReturn(UUID.randomUUID());

        StaffInfo cashier = mock(StaffInfo.class);
        when(cashier.getProfile())
                .thenReturn(cashierProfile);

        when(repo.existsByInvoiceCode(anyString()))
                .thenReturn(false);

        when(repo.save(any(Invoice.class)))
                .thenAnswer(i -> {
                    Invoice inv = i.getArgument(0);
                    inv.setInvoiceId(UUID.randomUUID());
                    return inv;
                });

        when(staffRepo.findAllBySystemRoleIn(anyList()))
                .thenReturn(List.of(cashier));

        doThrow(new RuntimeException("notification failed"))
                .when(notificationService)
                .create(any());

        InvoiceCreateRequest req =
                new InvoiceCreateRequest(
                        null,
                        null,
                        null,
                        LocalDate.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null
                );

        assertDoesNotThrow(
                () -> invoiceService.create(req)
        );
    }
}