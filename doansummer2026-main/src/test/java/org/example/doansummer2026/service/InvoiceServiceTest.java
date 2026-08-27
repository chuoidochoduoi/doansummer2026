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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private DepartmentRepository departmentRepo;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private InsuranceRuleRepository insuranceRuleRepository;

    @Mock
    private BhxhIntegrationService bhxhIntegrationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private InvoiceService invoiceService;


    // =========================================================
    // HELPERS
    // =========================================================

    private Profile customer(UUID profileId) {
        return Profile.builder()
                .profileId(profileId)
                .fullName("Nguyen Van A")
                .phone("0901234567")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .build();
    }


    private CustomerVisit visit(
            UUID visitId,
            Profile customer
    ) {
        return CustomerVisit.builder()
                .visitId(visitId)
                .customer(customer)
                .status(VisitStatus.CHECKED_IN)
                .build();
    }


    private MedicalService paraclinicalService(
            UUID serviceId,
            String name,
            BigDecimal price
    ) {
        return MedicalService.builder()
                .serviceId(serviceId)
                .name(name)
                .serviceCode("XN01")
                .price(price)
                .status(ServiceStatus.ACTIVE)
                .departmentType(DepartmentType.LABORATORY)
                .build();
    }


    private MedicalService examinationService(
            UUID serviceId,
            String name,
            BigDecimal price,
            Department department
    ) {
        return MedicalService.builder()
                .serviceId(serviceId)
                .name(name)
                .serviceCode("KB01")
                .price(price)
                .status(ServiceStatus.ACTIVE)
                .departmentType(DepartmentType.EXAMINATION)
                .department(department)
                .build();
    }


    private InvoiceItem invoiceItem(
            MedicalService service
    ) {
        return InvoiceItem.builder()
                .itemId(UUID.randomUUID())
                .service(service)
                .serviceSnapshot(service.getName())
                .serviceCodeSnapshot(service.getServiceCode())
                .unitPrice(service.getPrice())
                .quantity(1)
                .lineTotal(service.getPrice())
                .discountPercent(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalPrice(service.getPrice())
                .build();
    }


    /**
     * Chuẩn bị workflow tối thiểu cho một hóa đơn chứa dịch vụ CLS.
     *
     * InvoiceService.pay() sau khi PAID luôn gọi
     * createQueueTicketsFromInvoiceItems().
     */
    private InvoiceItem prepareParaclinicalWorkflow(
            Invoice invoice,
            CustomerVisit visit,
            MedicalService service
    ) {

        InvoiceItem item =
                invoiceItem(service);

        invoice.setVisit(visit);
        invoice.setItems(
                new ArrayList<>(
                        List.of(item)
                )
        );

        UUID invoiceId =
                invoice.getInvoiceId();

        UUID visitId =
                visit.getVisitId();

        when(
                repo.getWithDetailsByInvoiceId(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                itemRepo.findAllWithServiceByInvoiceId(
                        invoiceId
                )
        ).thenReturn(
                List.of(item)
        );

        when(
                queueTicketRepo
                        .findAllByVisit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                testRequestRepo
                        .findAllByMedicalRecord_Visit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

        return item;
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturnInvoice_WhenExists() {

        UUID id =
                UUID.randomUUID();

        Invoice invoice =
                mock(Invoice.class);

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(invoice)
                );

        assertSame(
                invoice,
                invoiceService.findById(id)
        );
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id =
                UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        invoiceService.findById(id)
        );
    }


    // =========================================================
    // GET
    // =========================================================

    @Test
    void get_ShouldThrow_WhenInvoiceMissing() {

        UUID id =
                UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        invoiceService.get(id)
        );
    }


    @Test
    void get_ShouldReturnInvoiceWithTransactionIds() {

        UUID id =
                UUID.randomUUID();

        UUID transactionId =
                UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PENDING)
                        .items(new ArrayList<>())
                        .build();

        Transaction transaction =
                mock(Transaction.class);

        when(
                transaction.getTransactionId()
        ).thenReturn(
                transactionId
        );

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(invoice)
                );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(id)
        ).thenReturn(
                List.of(transaction)
        );

        var result =
                invoiceService.get(id);

        assertNotNull(result);

        verify(transactionRepo)
                .findByInvoice_InvoiceId(id);
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void search_ShouldUseDefaultCreatedAtSort_WhenPageableHasNoSort() {

        Pageable pageable =
                PageRequest.of(0, 10);

        when(
                repo.findAll(
                        any(Specification.class),
                        any(Pageable.class)
                )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        var result =
                invoiceService.search(
                        null,
                        null,
                        "  ABC  ",
                        "  XET NGHIEM ",
                        null,
                        null,
                        pageable
                );

        assertNotNull(result);

        verify(repo)
                .findAll(
                        any(Specification.class),
                        argThat((Pageable p) ->
                                p.getSort().isSorted()
                                        &&
                                        p.getSort()
                                                .getOrderFor("createdAt")
                                                != null
                        )
                );
    }


    // =========================================================
    // CREATE - VISIT NOT FOUND
    // =========================================================

    @Test
    void create_ShouldThrow_WhenVisitDoesNotExist() {

        UUID visitId =
                UUID.randomUUID();

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

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        invoiceService.create(req)
        );

        verify(
                repo,
                never()
        ).save(any());
    }


    // =========================================================
    // CREATE - MEDICAL RECORD NOT FOUND
    // =========================================================

    @Test
    void create_ShouldThrow_WhenMedicalRecordDoesNotExist() {

        UUID recordId =
                UUID.randomUUID();

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

        when(
                recordRepo.findById(recordId)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        invoiceService.create(req)
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

        when(
                repo.existsByInvoiceCode(
                        anyString()
                )
        ).thenReturn(false);

        when(
                repo.save(
                        any(Invoice.class)
                )
        ).thenAnswer(
                invocation -> {

                    Invoice invoice =
                            invocation.getArgument(0);

                    if (invoice.getInvoiceId() == null) {
                        invoice.setInvoiceId(
                                UUID.randomUUID()
                        );
                    }

                    return invoice;
                }
        );

        when(
                staffRepo
                        .findAllBySystemRoleIn(
                                anyList()
                        )
        ).thenReturn(
                List.of()
        );

        var result =
                invoiceService.create(req);

        assertNotNull(result);

        verify(
                repo,
                times(2)
        ).save(any(Invoice.class));
    }


    // =========================================================
    // CREATE - RELATED ENTITIES
    // =========================================================

    @Test
    void create_ShouldAttachRelatedEntities() {

        UUID customerId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID recordId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        Profile customer =
                customer(customerId);

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        MedicalRecord record =
                mock(MedicalRecord.class);

        StaffInfo staff =
                mock(StaffInfo.class);

        when(
                record.getVisit()
        ).thenReturn(visit);

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

        when(
                profileRepo.findById(
                        customerId
                )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                recordRepo.findById(
                        recordId
                )
        ).thenReturn(
                Optional.of(record)
        );

        when(
                staffRepo.findById(
                        staffId
                )
        ).thenReturn(
                Optional.of(staff)
        );

        when(
                repo.existsByInvoiceCode(
                        anyString()
                )
        ).thenReturn(false);

        when(
                repo.save(
                        any(Invoice.class)
                )
        ).thenAnswer(
                invocation -> {

                    Invoice invoice =
                            invocation.getArgument(0);

                    if (invoice.getInvoiceId() == null) {
                        invoice.setInvoiceId(
                                UUID.randomUUID()
                        );
                    }

                    return invoice;
                }
        );

        when(
                staffRepo
                        .findAllBySystemRoleIn(
                                anyList()
                        )
        ).thenReturn(
                List.of()
        );

        var result =
                invoiceService.create(req);

        assertNotNull(result);

        verify(
                repo,
                times(2)
        ).save(
                argThat(invoice ->
                        invoice.getCustomer() == customer
                                &&
                                invoice.getVisit() == visit
                                &&
                                invoice.getMedicalRecord() == record
                                &&
                                invoice.getIssuedBy() == staff
                )
        );
    }


    // =========================================================
    // CREATE - ITEM
    // =========================================================

    @Test
    void create_ShouldBuildItemAndCalculateTotal() {

        UUID profileId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Profile customer =
                customer(profileId);

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        MedicalService service =
                paraclinicalService(
                        serviceId,
                        "Xet nghiem mau",
                        new BigDecimal("100000")
                );

        InvoiceItemCreateRequest itemRequest =
                new InvoiceItemCreateRequest(
                        serviceId,
                        "Ignored snapshot",
                        "IGNORED",
                        new BigDecimal("1"),
                        2,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("1"),
                        null
                );

        InvoiceCreateRequest req =
                new InvoiceCreateRequest(
                        profileId,
                        visitId,
                        null,
                        LocalDate.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        List.of(itemRequest)
                );

        when(
                profileRepo.findById(
                        profileId
                )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                serviceRepo.findById(
                        serviceId
                )
        ).thenReturn(
                Optional.of(service)
        );

        when(
                repo.existsByInvoiceCode(
                        anyString()
                )
        ).thenReturn(false);

        when(
                repo.save(
                        any(Invoice.class)
                )
        ).thenAnswer(
                invocation -> {

                    Invoice invoice =
                            invocation.getArgument(0);

                    if (invoice.getInvoiceId() == null) {
                        invoice.setInvoiceId(
                                UUID.randomUUID()
                        );
                    }

                    return invoice;
                }
        );

        when(
                itemRepo.save(
                        any(InvoiceItem.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        when(
                staffRepo
                        .findAllBySystemRoleIn(
                                anyList()
                        )
        ).thenReturn(
                List.of()
        );

        var result =
                invoiceService.create(req);

        assertNotNull(result);

        var itemCaptor = ArgumentCaptor.forClass(InvoiceItem.class);
        verify(itemRepo).save(itemCaptor.capture());
        var savedItem = itemCaptor.getValue();
        assertSame(service, savedItem.getService());
        assertEquals(0, new BigDecimal("100000").compareTo(savedItem.getUnitPrice()));
        assertEquals(0, new BigDecimal("200000").compareTo(savedItem.getLineTotal()));

        verify(
                repo,
                atLeastOnce()
        ).save(
                argThat(invoice ->
                        new BigDecimal("200000")
                                .compareTo(
                                        invoice.getSubtotal()
                                )
                                == 0
                                &&
                                new BigDecimal("200000")
                                        .compareTo(
                                                invoice.getTotalAmount()
                                        )
                                == 0
                )
        );

        // Giá trên InvoiceItem là snapshot tại thời điểm tạo hóa đơn.
        // Việc Admin đổi giá MedicalService sau đó không được cập nhật ngược hóa đơn cũ.
        service.setPrice(new BigDecimal("150000"));
        assertEquals(0, new BigDecimal("100000").compareTo(savedItem.getUnitPrice()));
        assertEquals(0, new BigDecimal("200000").compareTo(savedItem.getLineTotal()));
    }


    // =========================================================
    // CREATE - NEGATIVE TOTAL
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

        when(
                repo.existsByInvoiceCode(
                        anyString()
                )
        ).thenReturn(false);

        when(
                repo.save(
                        any(Invoice.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        invoiceService.create(req)
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void update_ShouldReject_WhenInvoiceNotPending() {

        UUID id =
                UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PAID)
                        .items(new ArrayList<>())
                        .build();

        InvoiceUpdateRequest req =
                mock(InvoiceUpdateRequest.class);

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(invoice)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        invoiceService.update(
                                id,
                                req
                        )
        );
    }


    @Test
    void update_ShouldUpdateSimpleFields_WhenPending() {

        UUID id =
                UUID.randomUUID();

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

        LocalDate dueDate =
                LocalDate.now().plusDays(5);

        InvoiceUpdateRequest req =
                mock(InvoiceUpdateRequest.class);

        when(req.dueDate())
                .thenReturn(dueDate);

        when(req.discount())
                .thenReturn(BigDecimal.ZERO);

        when(req.tax())
                .thenReturn(
                        new BigDecimal("5")
                );

        when(req.note())
                .thenReturn("updated");

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(id)
        ).thenReturn(
                List.of()
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        var result =
                invoiceService.update(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                dueDate,
                invoice.getDueDate()
        );

        assertEquals(
                "updated",
                invoice.getNote()
        );

        assertEquals(
                0,
                new BigDecimal("5")
                        .compareTo(
                                invoice.getTotalAmount()
                        )
        );
    }


    // =========================================================
    // ISSUE
    // =========================================================

    @Test
    void issue_ShouldReject_WhenNotPending() {

        UUID id =
                UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PAID)
                        .items(new ArrayList<>())
                        .build();

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(invoice)
                );

        assertThrows(
                ConflictException.class,
                () ->
                        invoiceService.issue(id)
        );
    }


    @Test
    void issue_ShouldReject_WhenNoItems() {

        UUID id =
                UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PENDING)
                        .items(new ArrayList<>())
                        .build();

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(invoice)
                );

        assertThrows(
                BadRequestException.class,
                () ->
                        invoiceService.issue(id)
        );
    }


    @Test
    void issue_ShouldSave_WhenPendingAndHasItems() {

        UUID id =
                UUID.randomUUID();

        InvoiceItem item =
                mock(InvoiceItem.class);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PENDING)
                        .items(
                                new ArrayList<>(
                                        List.of(item)
                                )
                        )
                        .build();

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(invoice)
                );

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

        UUID id =
                UUID.randomUUID();

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        invoiceService.cancel(id)
        );
    }


    @Test
    void cancel_ShouldRejectPaidInvoice() {

        UUID id =
                UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PAID)
                        .build();

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(invoice)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        invoiceService.cancel(id)
        );
    }


    @Test
    void cancel_ShouldReject_WhenSuccessfulTransactionExists() {

        UUID id =
                UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PENDING)
                        .build();

        Transaction transaction =
                mock(Transaction.class);

        when(
                transaction.getStatus()
        ).thenReturn(
                TransactionStatus.SUCCESS
        );

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(id)
        ).thenReturn(
                List.of(transaction)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        invoiceService.cancel(id)
        );
    }


    @Test
    void cancel_ShouldSetCancelled_WhenValid() {

        UUID id =
                UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PENDING)
                        .build();

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(id)
        ).thenReturn(
                List.of()
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        invoiceService.cancel(id);

        assertEquals(
                InvoiceStatus.CANCELLED,
                invoice.getStatus()
        );
    }


    // =========================================================
    // PAY - MISSING
    // =========================================================

    @Test
    void pay_ShouldThrow_WhenInvoiceMissing() {

        UUID id =
                UUID.randomUUID();

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        invoiceService.pay(
                                id,
                                null
                        )
        );
    }


    // =========================================================
    // PAY - CANCELLED
    // =========================================================

    @Test
    void pay_ShouldRejectCancelledInvoice() {

        UUID id =
                UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.CANCELLED)
                        .build();

        when(
                repo.findByIdForUpdate(id)
        ).thenReturn(
                Optional.of(invoice)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        invoiceService.pay(
                                id,
                                null
                        )
        );
    }


    // =========================================================
    // PAY - ALREADY PAID / IDEMPOTENT
    // =========================================================

    @Test
    void pay_ShouldReturnExistingInvoice_WhenAlreadyPaid() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        MedicalService service =
                paraclinicalService(
                        serviceId,
                        "Xet nghiem",
                        BigDecimal.ONE
                );

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .invoiceCode("INV-PAID")
                        .status(InvoiceStatus.PAID)
                        .totalAmount(BigDecimal.ONE)
                        .paidAmount(BigDecimal.ONE)
                        .customer(customer)
                        .visit(visit)
                        .items(new ArrayList<>())
                        .build();

        InvoiceItem item =
                prepareParaclinicalWorkflow(
                        invoice,
                        visit,
                        service
                );

        when(
                repo.findByIdForUpdate(invoiceId)
        ).thenReturn(
                Optional.of(invoice)
        );

        var result =
                invoiceService.pay(
                        invoiceId,
                        null
                );

        assertNotNull(result);

        assertEquals(
                InvoiceStatus.PAID,
                invoice.getStatus()
        );

        verify(
                transactionRepo,
                never()
        ).save(any());

        verify(testRequestService)
                .createFromPaidInvoice(
                        eq(visitId),
                        isNull(),
                        eq(serviceId),
                        isNull(),
                        eq("Xet nghiem"),
                        eq(item.getItemId())
                );
    }


    // =========================================================
    // PAY - SUCCESS
    // =========================================================

    @Test
    void pay_ShouldSetPaidAndCreateCashTransaction() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID cashierId =
                UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        MedicalService service =
                paraclinicalService(
                        serviceId,
                        "Xet nghiem",
                        new BigDecimal("200000")
                );

        StaffInfo cashier =
                mock(StaffInfo.class);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .invoiceCode("INV-001")
                        .status(InvoiceStatus.PENDING)
                        .totalAmount(
                                new BigDecimal("200000")
                        )
                        .paidAmount(BigDecimal.ZERO)
                        .customer(customer)
                        .visit(visit)
                        .items(new ArrayList<>())
                        .build();

        prepareParaclinicalWorkflow(
                invoice,
                visit,
                service
        );

        when(
                repo.findByIdForUpdate(invoiceId)
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                staffRepo.findById(
                        cashierId
                )
        ).thenReturn(
                Optional.of(cashier)
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        var result =
                invoiceService.pay(
                        invoiceId,
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
                        .compareTo(
                                invoice.getPaidAmount()
                        )
        );

        verify(transactionRepo)
                .save(
                        argThat(transaction ->
                                transaction.getInvoice()
                                        == invoice
                                        &&
                                        transaction.getStatus()
                                                == TransactionStatus.SUCCESS
                                        &&
                                        transaction.getPaymentMethod()
                                                == PaymentMethod.CASH
                                        &&
                                        transaction.getReceivedBy()
                                                == cashier
                        )
                );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenMissing() {

        UUID id =
                UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        invoiceService.delete(id)
        );
    }


    @Test
    void delete_ShouldReject_WhenInvoiceExists() {

        UUID id =
                UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(id)
                        .status(InvoiceStatus.PENDING)
                        .build();

        when(repo.findById(id))
                .thenReturn(
                        Optional.of(invoice)
                );

        assertThrows(
                ConflictException.class,
                () ->
                        invoiceService.delete(id)
        );

        verify(
                repo,
                never()
        ).deleteById(any());
    }


    // =========================================================
    // RECALCULATE PAID AMOUNT
    // =========================================================

    @Test
    void recalculatePaidAmount_ShouldSumOnlySuccessfulTransactions() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        MedicalService service =
                paraclinicalService(
                        serviceId,
                        "Xet nghiem",
                        new BigDecimal("100")
                );

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .invoiceCode("INV-REC")
                        .status(InvoiceStatus.PENDING)
                        .totalAmount(
                                new BigDecimal("100")
                        )
                        .paidAmount(BigDecimal.ZERO)
                        .customer(customer)
                        .visit(visit)
                        .items(new ArrayList<>())
                        .build();

        prepareParaclinicalWorkflow(
                invoice,
                visit,
                service
        );

        Transaction success =
                mock(Transaction.class);

        when(
                success.getStatus()
        ).thenReturn(
                TransactionStatus.SUCCESS
        );

        when(
                success.getAmount()
        ).thenReturn(
                new BigDecimal("100")
        );

        Transaction failed =
                mock(Transaction.class);

        when(
                failed.getStatus()
        ).thenReturn(
                TransactionStatus.FAILED
        );

        when(
                repo.findByIdForUpdate(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of(
                        success,
                        failed
                )
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        invoiceService
                .recalculatePaidAmount(
                        invoiceId
                );

        assertEquals(
                0,
                new BigDecimal("100")
                        .compareTo(
                                invoice.getPaidAmount()
                        )
        );

        assertEquals(
                InvoiceStatus.PAID,
                invoice.getStatus()
        );
    }


    @Test
    void recalculatePaidAmount_ShouldNotChangeCancelledStatus() {

        UUID invoiceId =
                UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .status(
                                InvoiceStatus.CANCELLED
                        )
                        .totalAmount(
                                new BigDecimal("100")
                        )
                        .build();

        when(
                repo.findByIdForUpdate(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of()
        );

        invoiceService
                .recalculatePaidAmount(
                        invoiceId
                );

        assertEquals(
                InvoiceStatus.CANCELLED,
                invoice.getStatus()
        );

        verify(
                repo,
                never()
        ).save(invoice);
    }


    @Test
    void recalculatePaidAmount_ShouldMovePaidBackToPending_WhenAmountInsufficient() {

        UUID invoiceId =
                UUID.randomUUID();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .status(InvoiceStatus.PAID)
                        .totalAmount(
                                new BigDecimal("100")
                        )
                        .build();

        Transaction success =
                mock(Transaction.class);

        when(
                success.getStatus()
        ).thenReturn(
                TransactionStatus.SUCCESS
        );

        when(
                success.getAmount()
        ).thenReturn(
                new BigDecimal("50")
        );

        when(
                repo.findByIdForUpdate(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of(success)
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        invoiceService
                .recalculatePaidAmount(
                        invoiceId
                );

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

        UUID invoiceId =
                UUID.randomUUID();

        when(
                repo.findById(invoiceId)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        invoiceService
                                .getReceiptDetail(
                                        invoiceId,
                                        UUID.randomUUID()
                                )
        );
    }


    @Test
    void getReceiptDetail_ShouldReject_WhenInvoiceBelongsToDifferentCustomer() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID customerId =
                UUID.randomUUID();

        UUID otherId =
                UUID.randomUUID();

        Profile customer =
                customer(otherId);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .customer(customer)
                        .status(InvoiceStatus.PAID)
                        .build();

        when(
                repo.findById(invoiceId)
        ).thenReturn(
                Optional.of(invoice)
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        invoiceService
                                .getReceiptDetail(
                                        invoiceId,
                                        customerId
                                )
        );
    }


    @Test
    void getReceiptDetail_ShouldReject_WhenInvoiceNotPaid() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID customerId =
                UUID.randomUUID();

        Profile customer =
                customer(customerId);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .customer(customer)
                        .status(InvoiceStatus.PENDING)
                        .build();

        when(
                repo.findById(invoiceId)
        ).thenReturn(
                Optional.of(invoice)
        );

        assertThrows(
                ConflictException.class,
                () ->
                        invoiceService
                                .getReceiptDetail(
                                        invoiceId,
                                        customerId
                                )
        );
    }


    @Test
    void getReceiptDetail_ShouldReturnReceipt_WhenPaidAndOwnedByCustomer() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID customerId =
                UUID.randomUUID();

        Profile customer =
                customer(customerId);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .customer(customer)
                        .status(InvoiceStatus.PAID)
                        .items(new ArrayList<>())
                        .build();

        when(
                repo.findById(invoiceId)
        ).thenReturn(
                Optional.of(invoice)
        );

        var result =
                invoiceService
                        .getReceiptDetail(
                                invoiceId,
                                customerId
                        );

        assertNotNull(result);
    }


    // =========================================================
    // PAYMENT HISTORY
    // =========================================================

    @Test
    void getPaymentHistoryForPatient_ShouldReturnEmptyPage() {

        UUID customerId =
                UUID.randomUUID();

        Pageable pageable =
                PageRequest.of(0, 10);

        when(
                repo.findAll(
                        any(Specification.class),
                        any(Pageable.class)
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
    // WORKFLOW - EXAMINATION BLOCKED
    // =========================================================

    @Test
    void pay_ShouldBlockNewExaminationQueue_WhenWorkflowAlreadyActive() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID departmentId =
                UUID.randomUUID();

        UUID createdTicketId =
                UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        StaffInfo doctor =
                mock(StaffInfo.class);

        Department department =
                Department.builder()
                        .departmentId(departmentId)
                        .departmentType(
                                DepartmentType.EXAMINATION
                        )
                        .status(
                                DepartmentStatus.AVAILABLE
                        )
                        .headDoctor(doctor)
                        .build();

        MedicalService service =
                examinationService(
                        serviceId,
                        "Kham noi",
                        BigDecimal.ONE,
                        department
                );

        InvoiceItem item =
                invoiceItem(service);

        QueueTicket activeTicket =
                QueueTicket.builder()
                        .ticketId(
                                UUID.randomUUID()
                        )
                        .status(
                                QueueStatus.WAITING
                        )
                        .build();

        QueueTicket newlyCreated =
                QueueTicket.builder()
                        .ticketId(
                                createdTicketId
                        )
                        .status(
                                QueueStatus.WAITING
                        )
                        .build();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .invoiceCode("INV-EXAM-2")
                        .status(InvoiceStatus.PENDING)
                        .totalAmount(BigDecimal.ONE)
                        .paidAmount(BigDecimal.ZERO)
                        .customer(customer)
                        .visit(visit)
                        .items(
                                new ArrayList<>(
                                        List.of(item)
                                )
                        )
                        .build();

        when(
                repo.findByIdForUpdate(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of()
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                repo.getWithDetailsByInvoiceId(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                itemRepo
                        .findAllWithServiceByInvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of(item)
        );

        when(
                queueTicketRepo
                        .findAllByVisit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of(activeTicket)
        );

        when(
                queueTicketRepo
                        .findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(
                                visitId,
                                serviceId
                        )
        ).thenReturn(
                Optional.empty()
        );

        var response =
                mock(
                        org.example.doansummer2026.dto.queueTicket
                                .QueueTicketResponse.class
                );

        when(
                response.ticketId()
        ).thenReturn(
                createdTicketId
        );

        when(
                queueTicketService.create(any())
        ).thenReturn(response);

        when(
                queueTicketRepo.findById(
                        createdTicketId
                )
        ).thenReturn(
                Optional.of(newlyCreated)
        );

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
    // WORKFLOW - UNRESOLVED SERVICE
    // =========================================================

    @Test
    void pay_ShouldReject_WhenInvoiceItemServiceCannotBeResolved() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        InvoiceItem item =
                InvoiceItem.builder()
                        .itemId(
                                UUID.randomUUID()
                        )
                        .service(null)
                        .serviceSnapshot(
                                "Old Service"
                        )
                        .serviceCodeSnapshot(null)
                        .build();

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .invoiceCode("INV-NULL")
                        .status(InvoiceStatus.PENDING)
                        .totalAmount(BigDecimal.ONE)
                        .paidAmount(BigDecimal.ZERO)
                        .customer(customer)
                        .visit(visit)
                        .items(
                                new ArrayList<>(
                                        List.of(item)
                                )
                        )
                        .build();

        when(
                repo.findByIdForUpdate(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of()
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                repo.getWithDetailsByInvoiceId(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueTicketRepo
                        .findAllByVisit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                testRequestRepo
                        .findAllByMedicalRecord_Visit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                itemRepo
                        .findAllWithServiceByInvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of(item)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        invoiceService.pay(
                                invoiceId,
                                null
                        )
        );

        verifyNoInteractions(
                queueTicketService
        );

        verifyNoInteractions(
                testRequestService
        );
    }


    // =========================================================
    // WORKFLOW - EXAM NO ROOM / SPECIALIZATION
    // =========================================================

    @Test
    void pay_ShouldRejectExaminationService_WhenNoRoomOrSpecializationConfigured() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        MedicalService service =
                MedicalService.builder()
                        .serviceId(serviceId)
                        .name("Kham noi")
                        .serviceCode("KB01")
                        .price(BigDecimal.ONE)
                        .status(ServiceStatus.ACTIVE)
                        .departmentType(
                                DepartmentType.EXAMINATION
                        )
                        .department(null)
                        .requiredSpecialization(null)
                        .build();

        InvoiceItem item =
                invoiceItem(service);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .invoiceCode("INV-NODEPT")
                        .status(InvoiceStatus.PENDING)
                        .totalAmount(BigDecimal.ONE)
                        .paidAmount(BigDecimal.ZERO)
                        .customer(customer)
                        .visit(visit)
                        .items(
                                new ArrayList<>(
                                        List.of(item)
                                )
                        )
                        .build();

        when(
                repo.findByIdForUpdate(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of()
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                repo.getWithDetailsByInvoiceId(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                queueTicketRepo
                        .findAllByVisit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                testRequestRepo
                        .findAllByMedicalRecord_Visit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                itemRepo
                        .findAllWithServiceByInvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of(item)
        );

        assertThrows(
                BadRequestException.class,
                () ->
                        invoiceService.pay(
                                invoiceId,
                                null
                        )
        );

        verifyNoInteractions(
                queueTicketService
        );
    }


    // =========================================================
    // WORKFLOW - PARACLINICAL
    // =========================================================

    @Test
    void pay_ShouldCreateTestRequest_WhenServiceIsParaclinical() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID recordId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID staffId =
                UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .build();

        StaffInfo issuedBy =
                mock(StaffInfo.class);

        when(
                issuedBy.getStaffId()
        ).thenReturn(
                staffId
        );

        MedicalService service =
                paraclinicalService(
                        serviceId,
                        "Xet nghiem mau",
                        BigDecimal.ONE
                );

        InvoiceItem item =
                invoiceItem(service);

        item.setNote("XN mau");

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .invoiceCode("INV-LAB")
                        .status(InvoiceStatus.PENDING)
                        .totalAmount(BigDecimal.ONE)
                        .paidAmount(BigDecimal.ZERO)
                        .customer(customer)
                        .visit(visit)
                        .medicalRecord(record)
                        .issuedBy(issuedBy)
                        .items(
                                new ArrayList<>(
                                        List.of(item)
                                )
                        )
                        .build();

        when(
                repo.findByIdForUpdate(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                staffRepo.findById(
                        staffId
                )
        ).thenReturn(
                Optional.of(issuedBy)
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                repo.getWithDetailsByInvoiceId(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                itemRepo
                        .findAllWithServiceByInvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of(item)
        );

        when(
                queueTicketRepo
                        .findAllByVisit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                testRequestRepo
                        .findAllByMedicalRecord_Visit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

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
    // WORKFLOW - NO MEDICAL RECORD FALLBACK
    // =========================================================

    @Test
    void pay_ShouldKeepMedicalRecordNull_WhenInvoiceRecordIsNull() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        MedicalService service =
                paraclinicalService(
                        serviceId,
                        "Xet nghiem",
                        BigDecimal.ONE
                );

        InvoiceItem item =
                invoiceItem(service);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .invoiceCode("INV-NO-RECORD")
                        .status(InvoiceStatus.PENDING)
                        .totalAmount(BigDecimal.ONE)
                        .paidAmount(BigDecimal.ZERO)
                        .customer(customer)
                        .visit(visit)
                        .medicalRecord(null)
                        .items(
                                new ArrayList<>(
                                        List.of(item)
                                )
                        )
                        .build();

        when(
                repo.findByIdForUpdate(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of()
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                repo.getWithDetailsByInvoiceId(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                itemRepo
                        .findAllWithServiceByInvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of(item)
        );

        when(
                queueTicketRepo
                        .findAllByVisit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                testRequestRepo
                        .findAllByMedicalRecord_Visit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

        invoiceService.pay(
                invoiceId,
                null
        );

        verify(testRequestService)
                .createFromPaidInvoice(
                        eq(visitId),
                        isNull(),
                        eq(serviceId),
                        isNull(),
                        eq("Xet nghiem"),
                        eq(item.getItemId())
                );

        verify(
                recordRepo,
                never()
        ).findFirstByVisit_VisitIdOrderByCreatedAtDesc(
                any()
        );
    }


    // =========================================================
    // WORKFLOW - CASHIER FALLBACK
    // =========================================================

    @Test
    void pay_ShouldUseCashierAsRequester_WhenInvoiceIssuedByIsNull() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID cashierId =
                UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        StaffInfo cashier =
                mock(StaffInfo.class);

        when(
                cashier.getStaffId()
        ).thenReturn(
                cashierId
        );

        Transaction payment =
                mock(Transaction.class);

        when(
                payment.getReceivedBy()
        ).thenReturn(
                cashier
        );

        MedicalService service =
                paraclinicalService(
                        serviceId,
                        "Xet nghiem",
                        BigDecimal.ONE
                );

        InvoiceItem item =
                invoiceItem(service);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .invoiceCode("INV-CASHIER")
                        .status(InvoiceStatus.PENDING)
                        .totalAmount(BigDecimal.ONE)
                        .paidAmount(BigDecimal.ZERO)
                        .customer(customer)
                        .visit(visit)
                        .issuedBy(null)
                        .items(
                                new ArrayList<>(
                                        List.of(item)
                                )
                        )
                        .build();

        when(
                repo.findByIdForUpdate(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                staffRepo.findById(
                        cashierId
                )
        ).thenReturn(
                Optional.of(cashier)
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                repo.getWithDetailsByInvoiceId(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                                invoiceId,
                                TransactionStatus.SUCCESS
                        )
        ).thenReturn(
                Optional.of(payment)
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                itemRepo
                        .findAllWithServiceByInvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of(item)
        );

        when(
                queueTicketRepo
                        .findAllByVisit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

        when(
                testRequestRepo
                        .findAllByMedicalRecord_Visit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of()
        );

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
                        eq("Xet nghiem"),
                        eq(item.getItemId())
                );
    }


    // =========================================================
    // WORKFLOW - PARACLINICAL DELEGATION
    // =========================================================

    @Test
    void pay_ShouldDelegateParaclinicalBlockingDecisionToTestRequestService() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID itemId =
                UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        MedicalService service =
                paraclinicalService(
                        serviceId,
                        "Xet nghiem mau",
                        BigDecimal.ONE
                );

        InvoiceItem item =
                invoiceItem(service);

        item.setItemId(itemId);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .invoiceCode(
                                "INV-LAB-DELEGATE"
                        )
                        .status(
                                InvoiceStatus.PENDING
                        )
                        .totalAmount(BigDecimal.ONE)
                        .paidAmount(BigDecimal.ZERO)
                        .customer(customer)
                        .visit(visit)
                        .items(
                                new ArrayList<>(
                                        List.of(item)
                                )
                        )
                        .build();

        QueueTicket activeWorkflow =
                QueueTicket.builder()
                        .ticketId(
                                UUID.randomUUID()
                        )
                        .status(
                                QueueStatus.WAITING
                        )
                        .build();

        when(
                repo.findByIdForUpdate(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of()
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                repo.getWithDetailsByInvoiceId(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                itemRepo
                        .findAllWithServiceByInvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of(item)
        );

        when(
                queueTicketRepo
                        .findAllByVisit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of(activeWorkflow)
        );

        invoiceService.pay(
                invoiceId,
                null
        );

        assertEquals(
                InvoiceStatus.PAID,
                invoice.getStatus()
        );

        verify(testRequestService)
                .createFromPaidInvoice(
                        eq(visitId),
                        isNull(),
                        eq(serviceId),
                        isNull(),
                        eq("Xet nghiem mau"),
                        eq(itemId)
                );

        verify(
                testRequestRepo,
                never()
        ).save(any(TestRequest.class));

        verify(
                queueTicketRepo,
                never()
        ).save(any(QueueTicket.class));
    }


    // =========================================================
    // WORKFLOW - KEEP EXISTING QUEUE
    // =========================================================

    @Test
    void pay_ShouldNotChangeParaclinicalQueueStateInsideInvoiceService() {

        UUID invoiceId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        UUID serviceId =
                UUID.randomUUID();

        UUID itemId =
                UUID.randomUUID();

        UUID recordId =
                UUID.randomUUID();

        Profile customer =
                customer(UUID.randomUUID());

        CustomerVisit visit =
                visit(
                        visitId,
                        customer
                );

        MedicalRecord record =
                MedicalRecord.builder()
                        .recordId(recordId)
                        .build();

        MedicalService service =
                paraclinicalService(
                        serviceId,
                        "Xet nghiem",
                        BigDecimal.ONE
                );

        InvoiceItem item =
                invoiceItem(service);

        item.setItemId(itemId);

        Invoice invoice =
                Invoice.builder()
                        .invoiceId(invoiceId)
                        .invoiceCode(
                                "INV-LAB-STATE"
                        )
                        .status(
                                InvoiceStatus.PENDING
                        )
                        .totalAmount(BigDecimal.ONE)
                        .paidAmount(BigDecimal.ZERO)
                        .customer(customer)
                        .visit(visit)
                        .medicalRecord(record)
                        .items(
                                new ArrayList<>(
                                        List.of(item)
                                )
                        )
                        .build();

        QueueTicket existingWorkflow =
                QueueTicket.builder()
                        .ticketId(
                                UUID.randomUUID()
                        )
                        .status(
                                QueueStatus.WAITING
                        )
                        .build();

        when(
                repo.findByIdForUpdate(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                transactionRepo
                        .findByInvoice_InvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of()
        );

        when(repo.save(invoice))
                .thenReturn(invoice);

        when(
                repo.getWithDetailsByInvoiceId(
                        invoiceId
                )
        ).thenReturn(
                Optional.of(invoice)
        );

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                itemRepo
                        .findAllWithServiceByInvoiceId(
                                invoiceId
                        )
        ).thenReturn(
                List.of(item)
        );

        when(
                queueTicketRepo
                        .findAllByVisit_VisitId(
                                visitId
                        )
        ).thenReturn(
                List.of(existingWorkflow)
        );

        invoiceService.pay(
                invoiceId,
                null
        );

        assertEquals(
                InvoiceStatus.PAID,
                invoice.getStatus()
        );

        assertEquals(
                QueueStatus.WAITING,
                existingWorkflow.getStatus()
        );

        verify(testRequestService)
                .createFromPaidInvoice(
                        eq(visitId),
                        eq(recordId),
                        eq(serviceId),
                        isNull(),
                        eq("Xet nghiem"),
                        eq(itemId)
                );

        verify(
                testRequestRepo,
                never()
        ).save(any(TestRequest.class));

        verify(
                queueTicketRepo,
                never()
        ).save(any(QueueTicket.class));
    }


    // =========================================================
    // NOTIFICATION - CUSTOMER
    // =========================================================

    @Test
    void create_ShouldNotifyCashierWithCustomerName() {

        UUID customerId =
                UUID.randomUUID();

        UUID cashierProfileId =
                UUID.randomUUID();

        Profile customer =
                customer(customerId);

        customer.setFullName(
                "Nguyen Van Customer"
        );

        Profile cashierProfile =
                Profile.builder()
                        .profileId(
                                cashierProfileId
                        )
                        .build();

        StaffInfo cashier =
                StaffInfo.builder()
                        .staffId(
                                UUID.randomUUID()
                        )
                        .profile(
                                cashierProfile
                        )
                        .systemRole(
                                SystemRole.CASHIER
                        )
                        .build();

        when(
                profileRepo.findById(
                        customerId
                )
        ).thenReturn(
                Optional.of(customer)
        );

        when(
                repo.existsByInvoiceCode(
                        anyString()
                )
        ).thenReturn(false);

        when(
                repo.save(
                        any(Invoice.class)
                )
        ).thenAnswer(
                invocation -> {

                    Invoice invoice =
                            invocation.getArgument(0);

                    invoice.setInvoiceId(
                            UUID.randomUUID()
                    );

                    return invoice;
                }
        );

        when(
                staffRepo
                        .findAllBySystemRoleIn(
                                anyList()
                        )
        ).thenReturn(
                List.of(cashier)
        );

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
                .create(
                        argThat(notification ->
                                cashierProfileId.equals(
                                        notification.recipientId()
                                )
                                        &&
                                        notification.content()
                                                .contains(
                                                        "Nguyen Van Customer"
                                                )
                                        &&
                                        "Invoice".equals(
                                                notification.relatedEntity()
                                        )
                        )
                );
    }


    // =========================================================
    // NOTIFICATION - VISIT CUSTOMER NAME
    // =========================================================

    @Test
    void create_ShouldUseVisitCustomerNameForCashierNotification() {

        UUID profileId =
                UUID.randomUUID();

        UUID visitId =
                UUID.randomUUID();

        Profile customer =
                customer(profileId);

        customer.setFullName(
                "Guest ABC"
        );

        Appointment appointment =
                Appointment.builder()
                        .appointmentId(
                                UUID.randomUUID()
                        )
                        .isGuest(true)
                        .guestFullName(
                                "Guest ABC"
                        )
                        .build();

        CustomerVisit visit =
                CustomerVisit.builder()
                        .visitId(visitId)
                        .customer(customer)
                        .appointment(appointment)
                        .status(
                                VisitStatus.CHECKED_IN
                        )
                        .build();

        Profile cashierProfile =
                Profile.builder()
                        .profileId(
                                UUID.randomUUID()
                        )
                        .build();

        StaffInfo cashier =
                StaffInfo.builder()
                        .staffId(
                                UUID.randomUUID()
                        )
                        .profile(
                                cashierProfile
                        )
                        .systemRole(
                                SystemRole.CASHIER
                        )
                        .build();

        when(
                visitRepo.findByIdForUpdate(
                        visitId
                )
        ).thenReturn(
                Optional.of(visit)
        );

        when(
                repo.existsByInvoiceCode(
                        anyString()
                )
        ).thenReturn(false);

        when(
                repo.save(
                        any(Invoice.class)
                )
        ).thenAnswer(
                invocation -> {

                    Invoice invoice =
                            invocation.getArgument(0);

                    invoice.setInvoiceId(
                            UUID.randomUUID()
                    );

                    return invoice;
                }
        );

        when(
                staffRepo
                        .findAllBySystemRoleIn(
                                anyList()
                        )
        ).thenReturn(
                List.of(cashier)
        );

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
                .create(
                        argThat(notification ->
                                notification.content()
                                        .contains(
                                                "Guest ABC"
                                        )
                        )
                );
    }


    // =========================================================
    // NOTIFICATION - CASHIER PROFILE NULL
    // =========================================================

    @Test
    void create_ShouldNotNotifyCashier_WhenCashierProfileIsNull() {

        StaffInfo cashier =
                mock(StaffInfo.class);

        when(
                repo.existsByInvoiceCode(
                        anyString()
                )
        ).thenReturn(false);

        when(
                repo.save(
                        any(Invoice.class)
                )
        ).thenAnswer(
                invocation -> {

                    Invoice invoice =
                            invocation.getArgument(0);

                    invoice.setInvoiceId(
                            UUID.randomUUID()
                    );

                    return invoice;
                }
        );

        when(
                staffRepo
                        .findAllBySystemRoleIn(
                                anyList()
                        )
        ).thenReturn(
                List.of(cashier)
        );

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

        verify(
                notificationService,
                never()
        ).create(any());
    }


    // =========================================================
    // NOTIFICATION - EXCEPTION IGNORED
    // =========================================================

    @Test
    void create_ShouldIgnoreNotificationException() {

        Profile cashierProfile =
                Profile.builder()
                        .profileId(
                                UUID.randomUUID()
                        )
                        .build();

        StaffInfo cashier =
                StaffInfo.builder()
                        .staffId(
                                UUID.randomUUID()
                        )
                        .profile(
                                cashierProfile
                        )
                        .systemRole(
                                SystemRole.CASHIER
                        )
                        .build();

        when(
                repo.existsByInvoiceCode(
                        anyString()
                )
        ).thenReturn(false);

        when(
                repo.save(
                        any(Invoice.class)
                )
        ).thenAnswer(
                invocation -> {

                    Invoice invoice =
                            invocation.getArgument(0);

                    invoice.setInvoiceId(
                            UUID.randomUUID()
                    );

                    return invoice;
                }
        );

        when(
                staffRepo
                        .findAllBySystemRoleIn(
                                anyList()
                        )
        ).thenReturn(
                List.of(cashier)
        );

        doThrow(
                new RuntimeException(
                        "notification failed"
                )
        ).when(notificationService)
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
                () ->
                        invoiceService.create(req)
        );
    }
}
