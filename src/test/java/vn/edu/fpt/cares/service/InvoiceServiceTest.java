package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.dto.invoice.InvoiceCreateRequest;
import vn.edu.fpt.cares.dto.invoice.InvoiceItemCreateRequest;
import vn.edu.fpt.cares.dto.invoice.InvoiceUpdateRequest;
import vn.edu.fpt.cares.dto.invoice.InvoiceInsuranceRequest;
import vn.edu.fpt.cares.dto.insurance.BhxhCheckResponse;
import vn.edu.fpt.cares.enums.*;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.*;

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
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock private MembershipCardLedgerRepository membershipCardLedgerRepo;
    @Mock private SameDayParaclinicalResultService sameDayParaclinicalResultService;
    @Mock private PatientJourneyService patientJourneyService;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void validateServiceRegistrationsRejectsDuplicateLinesAndMissingVisit() {
        UUID serviceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        InvoiceItemCreateRequest item = invoiceItemRequest(serviceId);

        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "validateServiceRegistrations", null, null, null));
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "validateServiceRegistrations", List.of(), null, null));
        assertThrows(BadRequestException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        invoiceService, "validateServiceRegistrations", List.of(item, item), visitId, null));

        when(serviceRepo.findById(serviceId)).thenReturn(Optional.empty());
        when(visitRepo.findById(visitId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        invoiceService, "validateServiceRegistrations", List.of(item), visitId, null));
    }

    @Test
    void validateServiceRegistrationsRejectsExaminationAlreadyInVisitOrQueue() {
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        MedicalService examination = examinationService(serviceId, "Khám Nội",
                new BigDecimal("220000"), null);
        CustomerVisit visit = visit(visitId, null);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(examination));
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(itemRepo.findDistinctExaminationServiceIdsByVisit(visitId, null))
                .thenReturn(List.of(serviceId));

        assertThrows(BadRequestException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        invoiceService, "validateServiceRegistrations",
                        List.of(invoiceItemRequest(serviceId)), visitId, null));

        reset(itemRepo);
        when(itemRepo.findDistinctExaminationServiceIdsByVisit(visitId, null)).thenReturn(List.of());
        QueueTicket queued = QueueTicket.builder().service(examination).build();
        when(queueTicketRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(queued));
        assertThrows(BadRequestException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        invoiceService, "validateServiceRegistrations",
                        List.of(invoiceItemRequest(serviceId)), visitId, null));
    }

    @Test
    void validateServiceRegistrationsRejectsSameDayExaminationInAnotherVisit() {
        UUID visitId = UUID.randomUUID();
        UUID otherVisitId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Profile patient = customer(profileId);
        CustomerVisit visit = visit(visitId, patient);
        visit.setCheckInTime(java.time.LocalDateTime.of(2026, 9, 9, 8, 0));
        MedicalService examination = examinationService(serviceId, "Khám Nội",
                new BigDecimal("220000"), null);
        Invoice previousInvoice = Invoice.builder()
                .visit(visit(otherVisitId, patient)).build();
        InvoiceItem previousItem = invoiceItem(examination);
        previousItem.setInvoice(previousInvoice);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(examination));
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(profileRepo.findByIdForUpdate(profileId)).thenReturn(Optional.of(patient));
        when(itemRepo.findSameDayExaminationRegistrations(eq(profileId), any(), any(), isNull()))
                .thenReturn(List.of(previousItem));

        ConflictException error = assertThrows(ConflictException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        invoiceService, "validateServiceRegistrations",
                        List.of(invoiceItemRequest(serviceId)), visitId, null));
        assertTrue(error.getMessage().contains("đã được đăng ký hôm nay"));
    }

    @Test
    void buildItemUsesCatalogPriceAndRejectsInvalidOrDuplicateSameDayService() {
        Invoice invoice = Invoice.builder().build();
        InvoiceItemCreateRequest missingSelection = new InvoiceItemCreateRequest(
                null, "Dịch vụ", "DV", BigDecimal.ONE, 1,
                null, null, null, null);
        assertThrows(BadRequestException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        invoiceService, "buildItem", invoice, missingSelection));

        UUID serviceId = UUID.randomUUID();
        InvoiceItemCreateRequest request = invoiceItemRequest(serviceId);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        invoiceService, "buildItem", invoice, request));

        MedicalService inactive = paraclinicalService(serviceId, "Đường huyết", new BigDecimal("70000"));
        inactive.setStatus(ServiceStatus.INACTIVE);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(inactive));
        assertThrows(ConflictException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        invoiceService, "buildItem", invoice, request));

        MedicalService active = paraclinicalService(serviceId, "Đường huyết", new BigDecimal("70000"));
        CustomerVisit visit = visit(UUID.randomUUID(), customer(UUID.randomUUID()));
        invoice.setVisit(visit);
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(active));
        when(sameDayParaclinicalResultService.hasReusableResult(visit, serviceId)).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        invoiceService, "buildItem", invoice, request));

        when(sameDayParaclinicalResultService.hasReusableResult(visit, serviceId)).thenReturn(false);
        InvoiceItemCreateRequest negativeDiscount = new InvoiceItemCreateRequest(
                serviceId, "Dịch vụ", "DV", BigDecimal.ONE, 1,
                null, new BigDecimal("-1"), null, null);
        assertThrows(BadRequestException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        invoiceService, "buildItem", invoice, negativeDiscount));
        InvoiceItemCreateRequest excessiveDiscount = new InvoiceItemCreateRequest(
                serviceId, "Giá từ frontend không được tin", "SAI", BigDecimal.ONE, 1,
                null, new BigDecimal("70001"), null, "Ghi chú");
        assertThrows(BadRequestException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        invoiceService, "buildItem", invoice, excessiveDiscount));

        InvoiceItemCreateRequest valid = new InvoiceItemCreateRequest(
                serviceId, "Giá từ frontend không được tin", "SAI", BigDecimal.ONE, 2,
                null, new BigDecimal("10000"), null, "Ghi chú");
        InvoiceItem built = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "buildItem", invoice, valid);
        assertNotNull(built);
        assertEquals(new BigDecimal("70000"), built.getUnitPrice());
        assertEquals(new BigDecimal("140000"), built.getLineTotal());
        assertEquals(new BigDecimal("130000"), built.getFinalPrice());
        assertEquals("Đường huyết", built.getServiceSnapshot());
        assertEquals("XN01", built.getServiceCodeSnapshot());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void patientPaymentHistorySpecificationAppliesOwnershipDateAndSuccessfulMethodFilters() {
        UUID customerId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 9, 9);
        Specification<Invoice> filtered = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "searchForPatientSpec", customerId, from, to, PaymentMethod.CASH);
        jakarta.persistence.criteria.Root<Invoice> root = mock(
                jakarta.persistence.criteria.Root.class, RETURNS_DEEP_STUBS);
        jakarta.persistence.criteria.CriteriaQuery query = mock(
                jakarta.persistence.criteria.CriteriaQuery.class, RETURNS_DEEP_STUBS);
        jakarta.persistence.criteria.CriteriaBuilder cb = mock(
                jakarta.persistence.criteria.CriteriaBuilder.class, RETURNS_DEEP_STUBS);
        jakarta.persistence.criteria.Subquery<UUID> subquery = mock(
                jakarta.persistence.criteria.Subquery.class, RETURNS_DEEP_STUBS);
        when(query.subquery(UUID.class)).thenReturn(subquery);

        assertDoesNotThrow(() -> filtered.toPredicate(root, query, cb));
        verify(query).subquery(UUID.class);
        verify(cb).greaterThanOrEqualTo(any(), eq(from));
        verify(cb).lessThanOrEqualTo(any(), eq(to));
        verify(cb).exists(subquery);

        Specification<Invoice> paidOnly = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "searchForPatientSpec", null, null, null, null);
        reset(query, cb);
        assertDoesNotThrow(() -> paidOnly.toPredicate(root, query, cb));
        verify(query, never()).subquery(org.mockito.ArgumentMatchers.<Class<UUID>>any());
        verify(cb, never()).greaterThanOrEqualTo(any(), any(LocalDate.class));
        verify(cb, never()).lessThanOrEqualTo(any(), any(LocalDate.class));
    }


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

        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));

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

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void search_ShouldApplyEveryFilterAndPreserveCallerSort() {
        UUID customerId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 9, 10);
        Pageable pageable = PageRequest.of(1, 5, org.springframework.data.domain.Sort.by("issueDate"));
        org.mockito.ArgumentCaptor<Specification<Invoice>> specCaptor =
                org.mockito.ArgumentCaptor.forClass(Specification.class);
        when(repo.findAll(specCaptor.capture(), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        invoiceService.search(customerId, InvoiceStatus.PAID, "  Nguyễn An  ",
                "  Xét nghiệm  ", from, to, pageable);

        jakarta.persistence.criteria.Root<Invoice> root = mock(jakarta.persistence.criteria.Root.class,
                org.mockito.Answers.RETURNS_DEEP_STUBS);
        jakarta.persistence.criteria.CriteriaQuery query = mock(jakarta.persistence.criteria.CriteriaQuery.class,
                org.mockito.Answers.RETURNS_DEEP_STUBS);
        jakarta.persistence.criteria.CriteriaBuilder cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class,
                org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(cb.conjunction()).thenReturn(mock(jakarta.persistence.criteria.Predicate.class));
        lenient().when(cb.equal(any(jakarta.persistence.criteria.Expression.class), any())).thenReturn(mock(jakarta.persistence.criteria.Predicate.class));
        when(cb.like(any(jakarta.persistence.criteria.Expression.class), anyString())).thenReturn(mock(jakarta.persistence.criteria.Predicate.class));
        when(cb.or(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(mock(jakarta.persistence.criteria.Predicate.class));
        when(cb.greaterThanOrEqualTo(any(jakarta.persistence.criteria.Expression.class), eq(from)))
                .thenReturn(mock(jakarta.persistence.criteria.Predicate.class));
        when(cb.lessThanOrEqualTo(any(jakarta.persistence.criteria.Expression.class), eq(to)))
                .thenReturn(mock(jakarta.persistence.criteria.Predicate.class));

        assertNotNull(specCaptor.getValue().toPredicate(root, query, cb));
        verify(query).distinct(true);
        verify(repo).findAll(any(Specification.class), same(pageable));
    }

    @Test
    void applyInsuranceCalculatesRulesButNeverDiscountsIndividualAnalytes() {
        UUID invoiceId = UUID.randomUUID();
        UUID insuranceId = UUID.randomUUID();
        Profile patient = customer(UUID.randomUUID());
        patient.setFullName("Nguyễn Anh Đức");
        Insurance insurance = Insurance.builder().insuranceId(insuranceId).code("BHYT").name("BHYT").build();
        MedicalService examination = examinationService(UUID.randomUUID(), "Khám Nội", new BigDecimal("200000"),
                Department.builder().departmentId(UUID.randomUUID()).departmentType(DepartmentType.EXAMINATION).build());
        MedicalService analyte = paraclinicalService(UUID.randomUUID(), "Đường huyết", new BigDecimal("100000"));
        analyte.setServiceCode("AN-GLUCOSE");
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).invoiceCode("INV-01").customer(patient)
                .status(InvoiceStatus.PENDING).discount(BigDecimal.ZERO).tax(BigDecimal.ZERO)
                .items(new ArrayList<>()).build();
        invoice.getItems().add(InvoiceItem.builder().invoice(invoice).service(examination)
                .lineTotal(new BigDecimal("200000")).unitPrice(new BigDecimal("200000")).quantity(1)
                .discountAmount(BigDecimal.ZERO).finalPrice(new BigDecimal("200000")).build());
        invoice.getItems().add(InvoiceItem.builder().invoice(invoice).service(analyte)
                .lineTotal(new BigDecimal("100000")).unitPrice(new BigDecimal("100000")).quantity(1)
                .discountAmount(BigDecimal.ZERO).finalPrice(new BigDecimal("100000")).build());
        when(repo.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(transactionRepo.findByInvoice_InvoiceId(invoiceId)).thenReturn(List.of());
        when(insuranceRepository.findById(insuranceId)).thenReturn(Optional.of(insurance));
        when(bhxhIntegrationService.checkBhytCard("DN4010123456789")).thenReturn(
                new BhxhCheckResponse(true, "Hợp lệ", insuranceId, "BHYT", "nguyen anh duc", "01/01/2000"));
        when(insuranceRuleRepository.findByInsurance_InsuranceId(insuranceId)).thenReturn(List.of(
                InsuranceRule.builder().insurance(insurance).departmentType(DepartmentType.EXAMINATION)
                        .discountPercent(new BigDecimal("80")).build()));
        when(repo.save(invoice)).thenReturn(invoice);

        invoiceService.applyInsurance(invoiceId,
                new InvoiceInsuranceRequest(insuranceId, " DN4010123456789 "));

        assertEquals(new BigDecimal("160000.00"), invoice.getDiscount());
        assertEquals(new BigDecimal("140000.00"), invoice.getTotalAmount());
        assertEquals(new BigDecimal("0.00"), invoice.getItems().get(1).getBhytFund());
        assertEquals("DN4010123456789", patient.getInsuranceId());
        verify(profileRepo).save(patient);
    }

    @Test
    void applyInsuranceRejectsWrongStatePaymentMissingInsuranceAndInvalidCard() {
        UUID id = UUID.randomUUID();
        Invoice invoice = Invoice.builder().invoiceId(id).status(InvoiceStatus.PAID).build();
        when(repo.findByIdForUpdate(id)).thenReturn(Optional.of(invoice));
        assertThrows(ConflictException.class, () -> invoiceService.applyInsurance(id,
                new InvoiceInsuranceRequest(UUID.randomUUID(), "CARD")));

        invoice.setStatus(InvoiceStatus.PENDING);
        when(transactionRepo.findByInvoice_InvoiceId(id)).thenReturn(List.of(
                Transaction.builder().status(TransactionStatus.SUCCESS).build()));
        assertThrows(ConflictException.class, () -> invoiceService.applyInsurance(id,
                new InvoiceInsuranceRequest(UUID.randomUUID(), "CARD")));

        when(transactionRepo.findByInvoice_InvoiceId(id)).thenReturn(List.of());
        UUID insuranceId = UUID.randomUUID();
        when(insuranceRepository.findById(insuranceId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.applyInsurance(id,
                new InvoiceInsuranceRequest(insuranceId, "CARD")));

        Insurance insurance = Insurance.builder().insuranceId(insuranceId).build();
        when(insuranceRepository.findById(insuranceId)).thenReturn(Optional.of(insurance));
        when(bhxhIntegrationService.checkBhytCard("CARD")).thenReturn(
                new BhxhCheckResponse(false, "Thẻ hết hạn", insuranceId, null, null, null));
        assertThrows(BadRequestException.class, () -> invoiceService.applyInsurance(id,
                new InvoiceInsuranceRequest(insuranceId, "CARD")));
    }

    @Test
    void applyInsuranceRejectsMismatchedInsuranceAndIncompleteIdentity() {
        UUID id = UUID.randomUUID();
        UUID insuranceId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().invoiceId(id).status(InvoiceStatus.PENDING)
                .customer(customer(UUID.randomUUID())).items(new ArrayList<>()).build();
        Insurance insurance = Insurance.builder().insuranceId(insuranceId).build();
        when(repo.findByIdForUpdate(id)).thenReturn(Optional.of(invoice));
        when(transactionRepo.findByInvoice_InvoiceId(id)).thenReturn(List.of());
        when(insuranceRepository.findById(insuranceId)).thenReturn(Optional.of(insurance));
        when(bhxhIntegrationService.checkBhytCard("CARD")).thenReturn(
                new BhxhCheckResponse(true, "ok", UUID.randomUUID(), null, "Nguyen Van A", "2000-01-01"));
        assertThrows(BadRequestException.class, () -> invoiceService.applyInsurance(id,
                new InvoiceInsuranceRequest(insuranceId, "CARD")));

        when(bhxhIntegrationService.checkBhytCard("CARD")).thenReturn(
                new BhxhCheckResponse(true, "ok", insuranceId, null, null, null));
        assertThrows(BadRequestException.class, () -> invoiceService.applyInsurance(id,
                new InvoiceInsuranceRequest(insuranceId, "CARD")));
        invoice.getCustomer().setFullName(null);
        assertThrows(BadRequestException.class, () -> invoiceService.applyInsurance(id,
                new InvoiceInsuranceRequest(insuranceId, "CARD")));
    }

    @Test
    void applyInsuranceValidatesNameBirthDateAndDateFormats() {
        UUID id = UUID.randomUUID();
        UUID insuranceId = UUID.randomUUID();
        Profile patient = customer(UUID.randomUUID());
        Invoice invoice = Invoice.builder().invoiceId(id).status(InvoiceStatus.PENDING).customer(patient)
                .items(new ArrayList<>()).discount(BigDecimal.ZERO).tax(BigDecimal.ZERO).build();
        Insurance insurance = Insurance.builder().insuranceId(insuranceId).build();
        when(repo.findByIdForUpdate(id)).thenReturn(Optional.of(invoice));
        when(transactionRepo.findByInvoice_InvoiceId(id)).thenReturn(List.of());
        when(insuranceRepository.findById(insuranceId)).thenReturn(Optional.of(insurance));

        when(bhxhIntegrationService.checkBhytCard("CARD")).thenReturn(
                new BhxhCheckResponse(true, "ok", insuranceId, null, "Tên khác", "2000-01-01"));
        assertThrows(BadRequestException.class, () -> invoiceService.applyInsurance(id,
                new InvoiceInsuranceRequest(insuranceId, "CARD")));

        when(bhxhIntegrationService.checkBhytCard("CARD")).thenReturn(
                new BhxhCheckResponse(true, "ok", insuranceId, null, "Nguyen Van A", "2/1/2000"));
        assertThrows(BadRequestException.class, () -> invoiceService.applyInsurance(id,
                new InvoiceInsuranceRequest(insuranceId, "CARD")));

        when(bhxhIntegrationService.checkBhytCard("CARD")).thenReturn(
                new BhxhCheckResponse(true, "ok", insuranceId, null, "Nguyen Van A", "not-a-date"));
        assertThrows(BadRequestException.class, () -> invoiceService.applyInsurance(id,
                new InvoiceInsuranceRequest(insuranceId, "CARD")));

        when(bhxhIntegrationService.checkBhytCard("CARD")).thenReturn(
                new BhxhCheckResponse(true, "ok", insuranceId, null, "SKIP_VALIDATION", "ignored"));
        when(insuranceRuleRepository.findByInsurance_InsuranceId(insuranceId)).thenReturn(List.of());
        when(repo.save(invoice)).thenReturn(invoice);
        assertDoesNotThrow(() -> invoiceService.applyInsurance(id,
                new InvoiceInsuranceRequest(insuranceId, "CARD")));
    }


    // =========================================================

    private InvoiceItemCreateRequest invoiceItemRequest(UUID serviceId) {
        return new InvoiceItemCreateRequest(serviceId, "Dịch vụ", "DV",
                BigDecimal.ZERO, 1, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, null);
    }
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

    @Test
    void updateRejectsSuccessfulTransactionAndAcceptsNoOptionalChanges() {
        UUID id = UUID.randomUUID();
        Invoice invoice = Invoice.builder().invoiceId(id).invoiceCode("INV-U")
                .status(InvoiceStatus.PENDING).subtotal(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO).tax(BigDecimal.ZERO).totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>()).build();
        when(repo.findByIdForUpdate(id)).thenReturn(Optional.of(invoice));
        when(transactionRepo.findByInvoice_InvoiceId(id)).thenReturn(List.of(
                vn.edu.fpt.cares.model.Transaction.builder()
                        .status(TransactionStatus.SUCCESS).build()));

        InvoiceUpdateRequest empty = new InvoiceUpdateRequest(null, null, null, null, List.of());
        assertThrows(ConflictException.class, () -> invoiceService.update(id, empty));

        when(transactionRepo.findByInvoice_InvoiceId(id)).thenReturn(List.of(
                vn.edu.fpt.cares.model.Transaction.builder()
                        .status(TransactionStatus.FAILED).build()));
        when(repo.save(invoice)).thenReturn(invoice);
        assertNotNull(invoiceService.update(id, empty));
        verify(itemRepo, never()).deleteAll(anyCollection());
    }

    @Test
    void updateReplacesItemsAndUsesCurrentCatalogPrice() {
        UUID id = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        InvoiceItem oldItem = InvoiceItem.builder().itemId(UUID.randomUUID()).build();
        Invoice invoice = Invoice.builder().invoiceId(id).invoiceCode("INV-REPLACE")
                .status(InvoiceStatus.PENDING).subtotal(BigDecimal.ZERO).discount(BigDecimal.ZERO)
                .tax(BigDecimal.ZERO).totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>(List.of(oldItem))).build();
        MedicalService service = MedicalService.builder().serviceId(serviceId).serviceCode("LAB-X")
                .name("Xét nghiệm").status(ServiceStatus.ACTIVE).price(new BigDecimal("80000")).build();
        InvoiceItemCreateRequest itemRequest = invoiceItemRequest(serviceId);
        InvoiceUpdateRequest request = new InvoiceUpdateRequest(null, null, null, null, List.of(itemRequest));
        when(repo.findByIdForUpdate(id)).thenReturn(Optional.of(invoice));
        when(transactionRepo.findByInvoice_InvoiceId(id)).thenReturn(List.of());
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(service));
        when(repo.save(invoice)).thenReturn(invoice);

        List<InvoiceItem> originalItems = invoice.getItems();
        invoiceService.update(id, request);

        verify(itemRepo).deleteAll(same(originalItems));
        assertEquals(1, invoice.getItems().size());
        assertSame(service, invoice.getItems().get(0).getService());
        assertEquals(0, new BigDecimal("80000").compareTo(invoice.getItems().get(0).getUnitPrice()));
    }

    @Test
    void doctorMembershipRequiresDoctorRoleProfileAccountAndActiveStatus() {
        UUID departmentId = UUID.randomUUID();
        Department department = Department.builder().departmentId(departmentId).build();
        StaffInfo missingRole = StaffInfo.builder().systemRole(null).build();
        StaffInfo nurse = StaffInfo.builder().systemRole(SystemRole.NURSE).build();
        StaffInfo missingProfile = StaffInfo.builder().systemRole(SystemRole.DOCTOR).build();
        StaffInfo missingAccount = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().build()).build();
        StaffInfo inactive = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().account(Account.builder().isActive(false).build()).build()).build();
        StaffInfo active = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().account(Account.builder().isActive(true).build()).build()).build();

        when(staffRepo.findByDepartment_DepartmentId(departmentId))
                .thenReturn(List.of(missingRole, nurse, missingProfile, missingAccount, inactive));
        assertFalse(Boolean.TRUE.equals(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "hasDoctorMember", department)));
        when(staffRepo.findByDepartment_DepartmentId(departmentId))
                .thenReturn(List.of(missingRole, active));
        assertTrue(Boolean.TRUE.equals(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "hasDoctorMember", department)));
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
    void create_ShouldAllowMultipleExaminationServicesForDirectVisit() {
        UUID customerId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID firstServiceId = UUID.randomUUID();
        UUID secondServiceId = UUID.randomUUID();
        Profile customer = customer(customerId);
        CustomerVisit visit = visit(visitId, customer);
        visit.setCheckInTime(java.time.LocalDateTime.now());
        Department room = Department.builder().departmentId(UUID.randomUUID())
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        MedicalService first = examinationService(firstServiceId, "Khám Nội", new BigDecimal("200000"), room);
        MedicalService second = examinationService(secondServiceId, "Khám Da liễu", new BigDecimal("230000"), room);
        InvoiceItemCreateRequest firstItem = new InvoiceItemCreateRequest(firstServiceId, first.getName(),
                first.getServiceCode(), first.getPrice(), 1, BigDecimal.ZERO, BigDecimal.ZERO, first.getPrice(), null);
        InvoiceItemCreateRequest secondItem = new InvoiceItemCreateRequest(secondServiceId, second.getName(),
                second.getServiceCode(), second.getPrice(), 1, BigDecimal.ZERO, BigDecimal.ZERO, second.getPrice(), null);

        when(profileRepo.findById(customerId)).thenReturn(Optional.of(customer));
        when(profileRepo.findByIdForUpdate(customerId)).thenReturn(Optional.of(customer));
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(serviceRepo.findById(firstServiceId)).thenReturn(Optional.of(first));
        when(serviceRepo.findById(secondServiceId)).thenReturn(Optional.of(second));
        when(itemRepo.findSameDayExaminationRegistrations(eq(customerId), any(), any(), isNull()))
                .thenReturn(List.of());
        when(itemRepo.findDistinctExaminationServiceIdsByVisit(visitId, null)).thenReturn(List.of());
        when(queueTicketRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(repo.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice saved = invocation.getArgument(0);
            if (saved.getInvoiceId() == null) saved.setInvoiceId(UUID.randomUUID());
            return saved;
        });
        when(itemRepo.save(any(InvoiceItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = invoiceService.create(new InvoiceCreateRequest(customerId, visitId, null,
                null, BigDecimal.ZERO, BigDecimal.ZERO, null, null, List.of(firstItem, secondItem)));

        assertNotNull(response);
        verify(itemRepo, times(2)).save(any(InvoiceItem.class));
    }

    @Test
    void pay_ShouldOrderMultipleExaminationsByPriorityAndBlockFollowingStep() {
        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID lowServiceId = UUID.randomUUID();
        UUID highServiceId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID secondTicketId = UUID.randomUUID();
        Profile customer = customer(UUID.randomUUID());
        CustomerVisit visit = visit(visitId, customer);
        Department room = Department.builder().departmentId(roomId)
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        MedicalService low = examinationService(lowServiceId, "Khám ưu tiên thấp", BigDecimal.ONE, room);
        MedicalService high = examinationService(highServiceId, "Khám ưu tiên cao", BigDecimal.ONE, room);
        low.setWorkflowPriority(1);
        high.setWorkflowPriority(10);
        InvoiceItem lowItem = invoiceItem(low);
        InvoiceItem highItem = invoiceItem(high);
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).invoiceCode("INV-MULTI-EXAM")
                .status(InvoiceStatus.PENDING).totalAmount(new BigDecimal("2"))
                .paidAmount(BigDecimal.ZERO).customer(customer).visit(visit)
                .items(new ArrayList<>(List.of(lowItem, highItem))).build();
        Account activeAccount = Account.builder().accountId(UUID.randomUUID()).isActive(true).build();
        Profile doctorProfile = Profile.builder().profileId(UUID.randomUUID()).account(activeAccount).build();
        StaffInfo doctor = StaffInfo.builder().staffId(UUID.randomUUID()).profile(doctorProfile)
                .systemRole(SystemRole.DOCTOR).department(room).build();
        var firstResponse = mock(vn.edu.fpt.cares.dto.queueticket.QueueTicketResponse.class);
        var secondResponse = mock(vn.edu.fpt.cares.dto.queueticket.QueueTicketResponse.class);
        when(secondResponse.ticketId()).thenReturn(secondTicketId);
        QueueTicket secondTicket = QueueTicket.builder().ticketId(secondTicketId).status(QueueStatus.WAITING).build();

        when(repo.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(repo.save(invoice)).thenReturn(invoice);
        when(repo.getWithDetailsByInvoiceId(invoiceId)).thenReturn(Optional.of(invoice));
        when(transactionRepo.findByInvoice_InvoiceId(invoiceId)).thenReturn(List.of());
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(itemRepo.findAllWithServiceByInvoiceId(invoiceId)).thenReturn(List.of(lowItem, highItem));
        when(queueTicketRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(queueTicketRepo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(eq(visitId), any()))
                .thenReturn(Optional.empty());
        when(staffRepo.findByDepartment_DepartmentId(roomId)).thenReturn(List.of(doctor));
        when(queueTicketService.create(any())).thenReturn(firstResponse, secondResponse);
        when(queueTicketRepo.findById(secondTicketId)).thenReturn(Optional.of(secondTicket));

        invoiceService.pay(invoiceId, null);

        ArgumentCaptor<vn.edu.fpt.cares.dto.queueticket.QueueTicketCreateRequest> captor =
                ArgumentCaptor.forClass(vn.edu.fpt.cares.dto.queueticket.QueueTicketCreateRequest.class);
        verify(queueTicketService, times(2)).create(captor.capture());
        assertEquals(highServiceId, captor.getAllValues().get(0).serviceId());
        assertEquals(lowServiceId, captor.getAllValues().get(1).serviceId());
        assertEquals(QueueStatus.BLOCKED, secondTicket.getStatus());
    }

    @Test
    void paidInvoice_ShouldReuseFirstBalancedRoomForExaminationsOfSameSpecialization() {
        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID specializationId = UUID.randomUUID();
        UUID firstServiceId = UUID.randomUUID();
        UUID secondServiceId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID secondTicketId = UUID.randomUUID();
        Specialization specialization = Specialization.builder()
                .specializationId(specializationId).name("Nội khoa").build();
        Department room = Department.builder().departmentId(roomId)
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        MedicalService first = MedicalService.builder().serviceId(firstServiceId)
                .serviceCode("KB-NOI-01").name("Khám Nội tổng quát")
                .departmentType(DepartmentType.EXAMINATION).requiredSpecialization(specialization).build();
        MedicalService second = MedicalService.builder().serviceId(secondServiceId)
                .serviceCode("KB-NOI-02").name("Khám Tim mạch cơ bản")
                .departmentType(DepartmentType.EXAMINATION).requiredSpecialization(specialization).build();
        InvoiceItem firstItem = invoiceItem(first);
        InvoiceItem secondItem = invoiceItem(second);
        CustomerVisit visit = visit(visitId, customer(UUID.randomUUID()));
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).visit(visit).status(InvoiceStatus.PAID)
                .items(List.of(firstItem, secondItem)).build();
        var firstResponse = mock(vn.edu.fpt.cares.dto.queueticket.QueueTicketResponse.class);
        var secondResponse = mock(vn.edu.fpt.cares.dto.queueticket.QueueTicketResponse.class);
        QueueTicket secondTicket = QueueTicket.builder().ticketId(secondTicketId)
                .status(QueueStatus.WAITING).build();

        when(repo.getWithDetailsByInvoiceId(invoiceId)).thenReturn(Optional.of(invoice));
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(itemRepo.findAllWithServiceByInvoiceId(invoiceId)).thenReturn(List.of(firstItem, secondItem));
        when(queueTicketRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(queueTicketRepo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(eq(visitId), any()))
                .thenReturn(Optional.empty());
        when(departmentRepo.findEligibleExaminationRoomsBySpecialization(specializationId)).thenReturn(List.of(room));
        when(queueTicketService.create(any())).thenReturn(firstResponse, secondResponse);
        when(secondResponse.ticketId()).thenReturn(secondTicketId);
        when(queueTicketRepo.findById(secondTicketId)).thenReturn(Optional.of(secondTicket));

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "createQueueTicketsFromInvoiceItems", invoice);

        ArgumentCaptor<vn.edu.fpt.cares.dto.queueticket.QueueTicketCreateRequest> requests =
                ArgumentCaptor.forClass(vn.edu.fpt.cares.dto.queueticket.QueueTicketCreateRequest.class);
        verify(queueTicketService, times(2)).create(requests.capture());
        assertEquals(roomId, requests.getAllValues().get(0).departmentId());
        assertEquals(roomId, requests.getAllValues().get(1).departmentId());
        assertEquals(QueueStatus.BLOCKED, secondTicket.getStatus());
        verify(departmentRepo, times(1)).findEligibleExaminationRoomsBySpecialization(specializationId);
    }

    @Test
    void paidInvoice_ShouldKeepDifferentConfiguredRoomsEvenWhenSpecializationMatches() {
        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Specialization specialization = Specialization.builder().specializationId(UUID.randomUUID())
                .name("Nội khoa").build();
        Department firstRoom = Department.builder().departmentId(UUID.randomUUID())
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        Department secondRoom = Department.builder().departmentId(UUID.randomUUID())
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        MedicalService first = examinationService(UUID.randomUUID(), "Khám Nội 1", BigDecimal.ONE, firstRoom);
        first.setRequiredSpecialization(specialization);
        MedicalService second = examinationService(UUID.randomUUID(), "Khám Nội 2", BigDecimal.ONE, secondRoom);
        second.setRequiredSpecialization(specialization);
        CustomerVisit visit = visit(visitId, customer(UUID.randomUUID()));
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).visit(visit).status(InvoiceStatus.PAID)
                .items(List.of(invoiceItem(first), invoiceItem(second))).build();
        StaffInfo doctor = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().account(Account.builder().isActive(true).build()).build()).build();
        var firstResponse = mock(vn.edu.fpt.cares.dto.queueticket.QueueTicketResponse.class);
        var secondResponse = mock(vn.edu.fpt.cares.dto.queueticket.QueueTicketResponse.class);
        UUID secondTicketId = UUID.randomUUID();
        when(repo.getWithDetailsByInvoiceId(invoiceId)).thenReturn(Optional.of(invoice));
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(itemRepo.findAllWithServiceByInvoiceId(invoiceId)).thenReturn(invoice.getItems());
        when(queueTicketRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(testRequestRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(queueTicketRepo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(eq(visitId), any()))
                .thenReturn(Optional.empty());
        when(staffRepo.findByDepartment_DepartmentId(firstRoom.getDepartmentId())).thenReturn(List.of(doctor));
        when(staffRepo.findByDepartment_DepartmentId(secondRoom.getDepartmentId())).thenReturn(List.of(doctor));
        when(queueTicketService.create(any())).thenReturn(firstResponse, secondResponse);
        when(secondResponse.ticketId()).thenReturn(secondTicketId);
        when(queueTicketRepo.findById(secondTicketId)).thenReturn(Optional.of(
                QueueTicket.builder().ticketId(secondTicketId).status(QueueStatus.WAITING).build()));

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "createQueueTicketsFromInvoiceItems", invoice);

        ArgumentCaptor<vn.edu.fpt.cares.dto.queueticket.QueueTicketCreateRequest> requests =
                ArgumentCaptor.forClass(vn.edu.fpt.cares.dto.queueticket.QueueTicketCreateRequest.class);
        verify(queueTicketService, times(2)).create(requests.capture());
        assertEquals(firstRoom.getDepartmentId(), requests.getAllValues().get(0).departmentId());
        assertEquals(secondRoom.getDepartmentId(), requests.getAllValues().get(1).departmentId());
        verifyNoInteractions(departmentRepo);
    }

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
                        vn.edu.fpt.cares.dto.queueticket
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

        when(doctor.getSystemRole()).thenReturn(SystemRole.DOCTOR);
        when(doctor.getProfile()).thenReturn(Profile.builder()
                .account(Account.builder().isActive(true).build()).build());
        when(staffRepo.findByDepartment_DepartmentId(departmentId)).thenReturn(List.of(doctor));

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

    @Test
    void paidInvoiceRetry_ShouldKeepExistingExaminationQueueAndActivateJourneyOnce() {
        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID existingTicketId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId, customer(UUID.randomUUID()));
        Department room = Department.builder().departmentId(roomId)
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        MedicalService service = examinationService(serviceId, "Khám Nội", BigDecimal.ONE, room);
        InvoiceItem item = invoiceItem(service);
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).visit(visit)
                .status(InvoiceStatus.PAID).items(List.of(item)).build();
        StaffInfo doctor = StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(SystemRole.DOCTOR)
                .department(room).profile(Profile.builder()
                        .account(Account.builder().isActive(true).build()).build()).build();
        QueueTicket existing = QueueTicket.builder().ticketId(existingTicketId)
                .visit(visit).service(service).department(room).status(QueueStatus.WAITING).build();
        when(repo.getWithDetailsByInvoiceId(invoiceId)).thenReturn(Optional.of(invoice));
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(itemRepo.findAllWithServiceByInvoiceId(invoiceId)).thenReturn(List.of(item));
        when(queueTicketRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of(existing));
        when(queueTicketRepo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(
                visitId, serviceId)).thenReturn(Optional.of(existing));
        when(staffRepo.findByDepartment_DepartmentId(roomId)).thenReturn(List.of(doctor));

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "createQueueTicketsFromInvoiceItems", invoice);

        verify(queueTicketRepo, never()).save(any(QueueTicket.class));
        verify(patientJourneyService).activateNext(visitId);
    }

    @Test
    void paidInvoice_ShouldTreatQueueLessPendingTestAsActiveAndBlockNewExamination() {
        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID createdTicketId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId, customer(UUID.randomUUID()));
        Department room = Department.builder().departmentId(roomId)
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        MedicalService service = examinationService(serviceId, "Khám Ngoại", BigDecimal.ONE, room);
        InvoiceItem item = invoiceItem(service);
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).visit(visit)
                .status(InvoiceStatus.PAID).items(List.of(item)).build();
        StaffInfo doctor = StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(SystemRole.DOCTOR)
                .department(room).profile(Profile.builder()
                        .account(Account.builder().isActive(true).build()).build()).build();
        List<QueueTicket> inactiveQueues = List.of(
                QueueTicket.builder().status(QueueStatus.BLOCKED).build(),
                QueueTicket.builder().status(QueueStatus.DONE).build(),
                QueueTicket.builder().status(QueueStatus.SKIPPED).build(),
                QueueTicket.builder().status(QueueStatus.WAITING_FOR_TEST).build());
        TestRequest unfinishedWithoutQueue = TestRequest.builder()
                .testRequestId(UUID.randomUUID()).queueTicket(null)
                .status(TestRequestStatus.PENDING).build();
        QueueTicket created = QueueTicket.builder().ticketId(createdTicketId)
                .status(QueueStatus.WAITING).build();
        var response = mock(vn.edu.fpt.cares.dto.queueticket.QueueTicketResponse.class);
        when(response.ticketId()).thenReturn(createdTicketId);
        when(repo.getWithDetailsByInvoiceId(invoiceId)).thenReturn(Optional.of(invoice));
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(itemRepo.findAllWithServiceByInvoiceId(invoiceId)).thenReturn(List.of(item));
        when(queueTicketRepo.findAllByVisit_VisitId(visitId)).thenReturn(inactiveQueues);
        when(testRequestRepo.findAllByMedicalRecord_Visit_VisitId(visitId))
                .thenReturn(List.of(unfinishedWithoutQueue));
        when(queueTicketRepo.findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(
                visitId, serviceId)).thenReturn(Optional.empty());
        when(staffRepo.findByDepartment_DepartmentId(roomId)).thenReturn(List.of(doctor));
        when(queueTicketService.create(any())).thenReturn(response);
        when(queueTicketRepo.findById(createdTicketId)).thenReturn(Optional.of(created));

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "createQueueTicketsFromInvoiceItems", invoice);

        assertEquals(QueueStatus.BLOCKED, created.getStatus());
        verify(queueTicketRepo).save(created);
        verify(patientJourneyService).activateNext(visitId);
    }

    @Test
    void pay_ShouldRejectWhenSuccessfulPaymentsExceedInvoiceTotal() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).invoiceCode("INV-OVERPAID")
                .status(InvoiceStatus.PENDING).totalAmount(new BigDecimal("100000"))
                .paidAmount(BigDecimal.ZERO).build();
        vn.edu.fpt.cares.model.Transaction success =
                vn.edu.fpt.cares.model.Transaction.builder()
                        .status(TransactionStatus.SUCCESS).amount(new BigDecimal("100001")).build();
        when(repo.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(transactionRepo.findByInvoice_InvoiceId(invoiceId)).thenReturn(List.of(success));

        ConflictException error = assertThrows(ConflictException.class,
                () -> invoiceService.pay(invoiceId, null));

        assertTrue(error.getMessage().contains("lớn hơn tổng hóa đơn"));
        verify(repo, never()).save(any(Invoice.class));
        verifyNoInteractions(queueTicketService, testRequestService, patientJourneyService);
    }

    @Test
    void pay_ShouldNotCollectAgainWhenSuccessfulTransactionsAlreadyCoverTotal() {
        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        CustomerVisit visit = visit(visitId, customer(UUID.randomUUID()));
        MedicalService service = paraclinicalService(serviceId, "Đường huyết", new BigDecimal("100000"));
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).invoiceCode("INV-COVERED")
                .status(InvoiceStatus.PENDING).totalAmount(new BigDecimal("100000"))
                .paidAmount(BigDecimal.ZERO).visit(visit).items(new ArrayList<>()).build();
        InvoiceItem item = prepareParaclinicalWorkflow(invoice, visit, service);
        vn.edu.fpt.cares.model.Transaction success =
                vn.edu.fpt.cares.model.Transaction.builder()
                        .status(TransactionStatus.SUCCESS).amount(new BigDecimal("100000")).build();
        when(repo.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(transactionRepo.findByInvoice_InvoiceId(invoiceId)).thenReturn(List.of(success));
        when(repo.save(invoice)).thenReturn(invoice);

        invoiceService.pay(invoiceId, null);

        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(0, invoice.getPaidAmount().compareTo(new BigDecimal("100000")));
        verify(transactionRepo, never()).save(any(vn.edu.fpt.cares.model.Transaction.class));
        verify(testRequestService).createFromPaidInvoice(eq(visitId), isNull(), eq(serviceId),
                isNull(), eq("Đường huyết"), eq(item.getItemId()));
        verify(patientJourneyService).activateNext(visitId);
    }

    @Test
    void insuranceIdentityValidationCoversMissingMockNormalizedAndMismatchCases() {
        Invoice missingCustomer = Invoice.builder().build();
        assertThrows(BadRequestException.class, () ->
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(invoiceService,
                        "validateInsuranceIdentity", missingCustomer, "Nguyen Van A", "2000-01-01"));

        Profile incomplete = Profile.builder().fullName("Nguyễn Văn A").build();
        Invoice invoice = Invoice.builder().customer(incomplete).build();
        assertThrows(BadRequestException.class, () ->
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(invoiceService,
                        "validateInsuranceIdentity", invoice, "Nguyen Van A", "2000-01-01"));

        incomplete.setDateOfBirth(LocalDate.of(2000, 1, 2));
        assertThrows(BadRequestException.class, () ->
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(invoiceService,
                        "validateInsuranceIdentity", invoice, " ", "2000-01-02"));
        assertThrows(BadRequestException.class, () ->
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(invoiceService,
                        "validateInsuranceIdentity", invoice, "Nguyen Van A", null));

        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(invoiceService,
                "validateInsuranceIdentity", invoice, "SKIP_VALIDATION", "ignored"));
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(invoiceService,
                "validateInsuranceIdentity", invoice, "  nguyen   van a ", "2/1/2000"));

        assertThrows(BadRequestException.class, () ->
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(invoiceService,
                        "validateInsuranceIdentity", invoice, "Trần Văn B", "02/01/2000"));
        assertThrows(BadRequestException.class, () ->
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(invoiceService,
                        "validateInsuranceIdentity", invoice, "Nguyễn Văn A", "03/01/2000"));
        assertThrows(BadRequestException.class, () ->
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(invoiceService,
                        "validateInsuranceIdentity", invoice, "Nguyễn Văn A", "not-a-date"));
    }

    @Test
    void issueAndCancelCoverInvalidEmptySuccessfulTransactionAndSuccessBranches() {
        UUID id = UUID.randomUUID();
        Invoice paid = Invoice.builder().invoiceId(id).status(InvoiceStatus.PAID)
                .items(new ArrayList<>()).build();
        when(repo.findById(id)).thenReturn(Optional.of(paid));
        assertThrows(ConflictException.class, () -> invoiceService.issue(id));

        Invoice empty = Invoice.builder().invoiceId(id).status(InvoiceStatus.PENDING)
                .items(new ArrayList<>()).build();
        when(repo.findById(id)).thenReturn(Optional.of(empty));
        assertThrows(BadRequestException.class, () -> invoiceService.issue(id));

        Invoice pending = Invoice.builder().invoiceId(id).invoiceCode("INV-1")
                .status(InvoiceStatus.PENDING).items(new ArrayList<>()).build();
        pending.getItems().add(InvoiceItem.builder().quantity(1).unitPrice(BigDecimal.ONE)
                .lineTotal(BigDecimal.ONE).finalPrice(BigDecimal.ONE).build());
        when(repo.findById(id)).thenReturn(Optional.of(pending));
        when(repo.save(pending)).thenReturn(pending);
        assertDoesNotThrow(() -> invoiceService.issue(id));

        when(repo.findByIdForUpdate(id)).thenReturn(Optional.of(paid));
        assertThrows(ConflictException.class, () -> invoiceService.cancel(id));

        when(repo.findByIdForUpdate(id)).thenReturn(Optional.of(pending));
        vn.edu.fpt.cares.model.Transaction successful =
                vn.edu.fpt.cares.model.Transaction.builder().status(TransactionStatus.SUCCESS).build();
        when(transactionRepo.findByInvoice_InvoiceId(id)).thenReturn(List.of(successful));
        assertThrows(ConflictException.class, () -> invoiceService.cancel(id));

        when(transactionRepo.findByInvoice_InvoiceId(id)).thenReturn(List.of(
                vn.edu.fpt.cares.model.Transaction.builder().status(TransactionStatus.FAILED).build()));
        when(repo.save(pending)).thenReturn(pending);
        assertEquals(InvoiceStatus.CANCELLED, invoiceService.cancel(id).status());
    }

    @Test
    void receiptPrintRequiresPaidInvoiceAndSupportsMissingOrSuccessfulPayment() {
        UUID id = UUID.randomUUID();
        when(repo.getWithDetailsByInvoiceId(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getReceiptPrintData(id));

        Invoice invoice = Invoice.builder().invoiceId(id).invoiceCode("INV-PRINT")
                .status(InvoiceStatus.PENDING).items(new ArrayList<>())
                .subtotal(BigDecimal.TEN).tax(BigDecimal.ZERO).totalAmount(BigDecimal.TEN)
                .paidAmount(BigDecimal.ZERO).build();
        when(repo.getWithDetailsByInvoiceId(id)).thenReturn(Optional.of(invoice));
        assertThrows(ConflictException.class, () -> invoiceService.getReceiptPrintData(id));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAmount(BigDecimal.TEN);
        when(transactionRepo.findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                id, TransactionStatus.SUCCESS)).thenReturn(Optional.empty());
        var withoutPayment = invoiceService.getReceiptPrintData(id);
        assertEquals("INV-PRINT", withoutPayment.receiptNumber());
        assertNull(withoutPayment.paymentTransactionCode());

        UUID transactionId = UUID.randomUUID();
        var payment = vn.edu.fpt.cares.model.Transaction.builder()
                .transactionId(transactionId).transactionCode("TX-PRINT")
                .status(TransactionStatus.SUCCESS).paymentMethod(PaymentMethod.CASH).build();
        when(transactionRepo.findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                id, TransactionStatus.SUCCESS)).thenReturn(Optional.of(payment));
        when(membershipCardLedgerRepo.findByPaymentTransaction_TransactionId(transactionId))
                .thenReturn(Optional.empty());
        var withPayment = invoiceService.getReceiptPrintData(id);
        assertEquals("TX-PRINT", withPayment.receiptNumber());
        assertEquals("Tien mat", withPayment.paymentMethod());
    }

    @Test
    void examinationRoomSelectionUsesEligibleDirectRoomAndRejectsMissingSpecialization() {
        Department direct = Department.builder().departmentId(UUID.randomUUID())
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        MedicalService service = MedicalService.builder().name("Khám Nội").department(direct).build();
        StaffInfo doctor = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().account(Account.builder().isActive(true).build()).build()).build();
        when(staffRepo.findByDepartment_DepartmentId(direct.getDepartmentId())).thenReturn(List.of(doctor));
        assertSame(direct, org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "selectExaminationRoom", service));

        direct.setStatus(DepartmentStatus.MAINTENANCE);
        assertThrows(BadRequestException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "selectExaminationRoom", service));
        direct.setStatus(DepartmentStatus.AVAILABLE);
        service.setDepartment(null);
        assertThrows(BadRequestException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "selectExaminationRoom", service));
    }

    @Test
    void examinationRoomSelectionPrefersDoctorThenLowerActiveLoad() {
        Specialization specialization = Specialization.builder().specializationId(UUID.randomUUID())
                .name("Nội khoa").build();
        MedicalService service = MedicalService.builder().name("Khám Nội")
                .requiredSpecialization(specialization).build();
        Department noDoctor = Department.builder().departmentId(UUID.randomUUID())
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        Department busyDoctorRoom = Department.builder().departmentId(UUID.randomUUID())
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        Department freeDoctorRoom = Department.builder().departmentId(UUID.randomUUID())
                .departmentType(DepartmentType.EXAMINATION).status(DepartmentStatus.AVAILABLE).build();
        StaffInfo activeDoctor = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().account(Account.builder().isActive(true).build()).build()).build();
        StaffInfo inactiveDoctor = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().account(Account.builder().isActive(false).build()).build()).build();
        when(departmentRepo.findEligibleExaminationRoomsBySpecialization(specialization.getSpecializationId()))
                .thenReturn(List.of(noDoctor, busyDoctorRoom, freeDoctorRoom));
        when(staffRepo.findByDepartment_DepartmentId(noDoctor.getDepartmentId()))
                .thenReturn(List.of(StaffInfo.builder().systemRole(SystemRole.NURSE).build(), inactiveDoctor));
        when(staffRepo.findByDepartment_DepartmentId(busyDoctorRoom.getDepartmentId()))
                .thenReturn(List.of(activeDoctor));
        when(staffRepo.findByDepartment_DepartmentId(freeDoctorRoom.getDepartmentId()))
                .thenReturn(List.of(activeDoctor));
        when(queueTicketRepo.countActiveTicketsByDepartment(busyDoctorRoom.getDepartmentId())).thenReturn(5L);
        when(queueTicketRepo.countActiveTicketsByDepartment(freeDoctorRoom.getDepartmentId())).thenReturn(1L);

        assertSame(freeDoctorRoom, org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "selectExaminationRoom", service));

        when(departmentRepo.findEligibleExaminationRoomsBySpecialization(specialization.getSpecializationId()))
                .thenReturn(List.of());
        assertThrows(BadRequestException.class, () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "selectExaminationRoom", service));
    }

    @Test
    void doctorMembershipRequiresRoleProfileAccountAndActiveState() {
        Department room = Department.builder().departmentId(UUID.randomUUID()).build();
        StaffInfo noRole = StaffInfo.builder().build();
        StaffInfo noProfile = StaffInfo.builder().systemRole(SystemRole.DOCTOR).build();
        StaffInfo noAccount = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().build()).build();
        StaffInfo inactive = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().account(Account.builder().isActive(false).build()).build()).build();
        StaffInfo active = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().account(Account.builder().isActive(true).build()).build()).build();
        when(staffRepo.findByDepartment_DepartmentId(room.getDepartmentId()))
                .thenReturn(List.of(noRole, noProfile, noAccount, inactive));
        assertFalse(Boolean.TRUE.equals(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "hasDoctorMember", room)));
        when(staffRepo.findByDepartment_DepartmentId(room.getDepartmentId()))
                .thenReturn(List.of(noRole, active));
        assertTrue(Boolean.TRUE.equals(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "hasDoctorMember", room)));
    }

    @Test
    void normalizeItemRequests_ShouldHandleNullEmptyAndMissingPolicy() {
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "normalizeItemRequests", (Object) null));
        List<InvoiceItemCreateRequest> empty = List.of();
        assertSame(empty, org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "normalizeItemRequests", empty));

        InvoiceItemCreateRequest item = new InvoiceItemCreateRequest(
                UUID.randomUUID(), "Khám Nội", "EX-001", BigDecimal.TEN, 1,
                null, null, null, null);
        List<InvoiceItemCreateRequest> items = List.of(item);
        assertSame(items, org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "normalizeItemRequests", items));
    }

    @Test
    void getReceiptDetail_ShouldReturnPaidReceiptWithoutPaymentLedger() {
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Profile customer = Profile.builder().profileId(customerId).fullName("Nguyễn Văn A").build();
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).invoiceCode("INV-DETAIL")
                .customer(customer).status(InvoiceStatus.PAID).items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO).discount(BigDecimal.ZERO).tax(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO).paidAmount(BigDecimal.ZERO).build();
        when(repo.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(transactionRepo.findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                invoiceId, TransactionStatus.SUCCESS)).thenReturn(Optional.empty());

        var receipt = invoiceService.getReceiptDetail(invoiceId, customerId);

        assertNotNull(receipt);
        verifyNoInteractions(membershipCardLedgerRepo);
    }

    @Test
    void generateInvoiceCode_ShouldCoverImmediateSuccessAndRetryExhaustion() {
        when(repo.existsByInvoiceCode(anyString())).thenReturn(false);
        String generated = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "generateInvoiceCode");
        assertTrue(generated.startsWith("INV-"));
        verify(repo, times(1)).existsByInvoiceCode(generated);

        reset(repo);
        when(repo.existsByInvoiceCode(anyString())).thenReturn(true);
        assertThrows(ConflictException.class, () ->
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        invoiceService, "generateInvoiceCode"));
        verify(repo, times(3)).existsByInvoiceCode(anyString());
    }

    @Test
    void successfulTransactionHelper_ShouldCoverEmptyFailedAndSuccessfulLists() {
        UUID invoiceId = UUID.randomUUID();
        when(transactionRepo.findByInvoice_InvoiceId(invoiceId)).thenReturn(List.of());
        assertFalse(Boolean.TRUE.equals(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "hasSuccessfulTransaction", invoiceId)));
        when(transactionRepo.findByInvoice_InvoiceId(invoiceId)).thenReturn(List.of(
                vn.edu.fpt.cares.model.Transaction.builder()
                        .status(TransactionStatus.FAILED).build()));
        assertFalse(Boolean.TRUE.equals(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "hasSuccessfulTransaction", invoiceId)));
        when(transactionRepo.findByInvoice_InvoiceId(invoiceId)).thenReturn(List.of(
                vn.edu.fpt.cares.model.Transaction.builder()
                        .status(TransactionStatus.FAILED).build(),
                vn.edu.fpt.cares.model.Transaction.builder()
                        .status(TransactionStatus.SUCCESS).build()));
        assertTrue(Boolean.TRUE.equals(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "hasSuccessfulTransaction", invoiceId)));
    }

    @Test
    void notifyCashiers_ShouldCoverRegisteredGuestFallbackProfileAndFailureBranches() {
        Invoice noIdentity = Invoice.builder().invoiceId(UUID.randomUUID()).invoiceCode("INV-NONE").build();
        StaffInfo noProfile = StaffInfo.builder().systemRole(SystemRole.CASHIER).build();
        StaffInfo cashier = StaffInfo.builder().systemRole(SystemRole.CASHIER)
                .profile(Profile.builder().profileId(UUID.randomUUID()).build()).build();
        when(staffRepo.findAllBySystemRoleIn(anyList())).thenReturn(List.of(noProfile, cashier));
        doThrow(new RuntimeException("notification unavailable")).when(notificationService).create(any());
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "notifyCashiers", noIdentity));

        reset(notificationService);
        Invoice registered = Invoice.builder().invoiceId(UUID.randomUUID()).invoiceCode("INV-CUSTOMER")
                .customer(Profile.builder().profileId(UUID.randomUUID()).fullName(null).build()).build();
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                invoiceService, "notifyCashiers", registered));
        verify(notificationService).create(argThat(message -> message.content().contains("Khách")));

        reset(notificationService);
        Invoice guest = Invoice.builder().invoiceId(UUID.randomUUID()).invoiceCode("INV-GUEST")
                .visit(CustomerVisit.builder().appointment(Appointment.builder()
                        .guestFullName("Nguyễn Khách").build()).build()).build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(invoiceService, "notifyCashiers", guest);
        verify(notificationService).create(argThat(message -> message.content().contains("Nguyễn Khách")));
    }

    @Test
    void create_ShouldCoverEveryMissingReferencedEntityAndServiceWithoutVisit() {
        UUID customerId = UUID.randomUUID();
        InvoiceCreateRequest missingCustomer = new InvoiceCreateRequest(customerId, null, null,
                null, null, null, null, null, null);
        when(profileRepo.findById(customerId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.create(missingCustomer));

        UUID visitId = UUID.randomUUID();
        InvoiceCreateRequest missingVisit = new InvoiceCreateRequest(null, visitId, null,
                null, null, null, null, null, null);
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.create(missingVisit));

        UUID recordId = UUID.randomUUID();
        InvoiceCreateRequest missingRecord = new InvoiceCreateRequest(null, null, recordId,
                null, null, null, null, null, null);
        when(recordRepo.findById(recordId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.create(missingRecord));

        UUID staffId = UUID.randomUUID();
        InvoiceCreateRequest missingStaff = new InvoiceCreateRequest(null, null, null,
                null, null, null, null, staffId, null);
        when(staffRepo.findById(staffId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.create(missingStaff));

        InvoiceItemCreateRequest item = new InvoiceItemCreateRequest(UUID.randomUUID(), "Dịch vụ",
                "SVC", BigDecimal.TEN, 1, null, null, null, null);
        InvoiceCreateRequest serviceWithoutVisit = new InvoiceCreateRequest(null, null, null,
                null, null, null, null, null, List.of(item));
        assertThrows(BadRequestException.class, () -> invoiceService.create(serviceWithoutVisit));
    }

    @Test
    void insuranceDateAndConfiguredRoomHelpers_ShouldCoverAllFormatsAndShortCircuits() {
        assertEquals(LocalDate.of(2000, 1, 2), ReflectionTestUtils.invokeMethod(
                invoiceService, "parseInsuranceDate", "2000-01-02"));
        assertEquals(LocalDate.of(2000, 1, 2), ReflectionTestUtils.invokeMethod(
                invoiceService, "parseInsuranceDate", "02/01/2000"));
        assertEquals(LocalDate.of(2000, 1, 2), ReflectionTestUtils.invokeMethod(
                invoiceService, "parseInsuranceDate", "2/1/2000"));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                invoiceService, "parseInsuranceDate", "not-a-date"));

        MedicalService service = MedicalService.builder().build();
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(
                invoiceService, "hasConfiguredExaminationRoom", service)));
        Department room = Department.builder().departmentId(UUID.randomUUID())
                .departmentType(DepartmentType.LABORATORY).status(DepartmentStatus.AVAILABLE).build();
        service.setDepartment(room);
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(
                invoiceService, "hasConfiguredExaminationRoom", service)));
        room.setDepartmentType(DepartmentType.EXAMINATION);
        room.setStatus(DepartmentStatus.MAINTENANCE);
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(
                invoiceService, "hasConfiguredExaminationRoom", service)));
        room.setStatus(DepartmentStatus.AVAILABLE);
        when(staffRepo.findByDepartment_DepartmentId(room.getDepartmentId())).thenReturn(List.of());
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(
                invoiceService, "hasConfiguredExaminationRoom", service)));
        StaffInfo doctor = StaffInfo.builder().systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().account(Account.builder().isActive(true).build()).build()).build();
        when(staffRepo.findByDepartment_DepartmentId(room.getDepartmentId())).thenReturn(List.of(doctor));
        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(
                invoiceService, "hasConfiguredExaminationRoom", service)));
    }

    @Test
    void search_ShouldTreatBlankSearchAndCategoryAsAbsentAndMapNonEmptyPage() {
        Pageable pageable = PageRequest.of(0, 3);
        Invoice invoice = Invoice.builder().invoiceId(UUID.randomUUID()).invoiceCode("INV-BLANK")
                .status(InvoiceStatus.PENDING).items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO).discount(BigDecimal.ZERO).tax(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO).paidAmount(BigDecimal.ZERO).build();
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(invoice), pageable, 1));
        when(transactionRepo.findByInvoice_InvoiceId(invoice.getInvoiceId())).thenReturn(List.of(
                Transaction.builder().transactionId(UUID.randomUUID()).status(TransactionStatus.PENDING).build()));

        var response = invoiceService.search(null, null, "  ", "\t", null, null, pageable);

        assertEquals(1, response.content().size());
        verify(transactionRepo).findByInvoice_InvoiceId(invoice.getInvoiceId());
    }

    @Test
    void search_ShouldTreatNullSearchAndCategoryAsAbsent() {
        Pageable pageable = PageRequest.of(0, 2);
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var response = invoiceService.search(null, null, null, null, null, null, pageable);

        assertTrue(response.content().isEmpty());
    }

    @Test
    void dispatchPaidInvoice_ShouldCoverMissingVisitEmptyItemsAndUnresolvableSnapshotGuards() {
        UUID invoiceId = UUID.randomUUID();
        Invoice detached = Invoice.builder().invoiceId(invoiceId).status(InvoiceStatus.PAID).build();
        when(repo.getWithDetailsByInvoiceId(invoiceId)).thenReturn(Optional.of(detached));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                invoiceService, "createQueueTicketsFromInvoiceItems", detached));

        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).build();
        Invoice empty = Invoice.builder().invoiceId(invoiceId).visit(visit)
                .status(InvoiceStatus.PAID).items(List.of()).build();
        when(repo.getWithDetailsByInvoiceId(invoiceId)).thenReturn(Optional.of(empty));
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(queueTicketRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(testRequestRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(itemRepo.findAllWithServiceByInvoiceId(invoiceId)).thenReturn(List.of());
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                invoiceService, "createQueueTicketsFromInvoiceItems", empty));

        InvoiceItem unknown = InvoiceItem.builder().itemId(UUID.randomUUID())
                .service(null).serviceSnapshot("Dịch vụ cũ").serviceCodeSnapshot(null).build();
        Invoice legacy = Invoice.builder().invoiceId(invoiceId).visit(visit)
                .status(InvoiceStatus.PAID).items(List.of(unknown)).build();
        when(repo.getWithDetailsByInvoiceId(invoiceId)).thenReturn(Optional.of(legacy));
        when(itemRepo.findAllWithServiceByInvoiceId(invoiceId)).thenReturn(List.of(unknown));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                invoiceService, "createQueueTicketsFromInvoiceItems", legacy));

        unknown.setServiceCodeSnapshot("REMOVED-SVC");
        when(serviceRepo.findByServiceCode("REMOVED-SVC")).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                invoiceService, "createQueueTicketsFromInvoiceItems", legacy));
    }

    @Test
    void missingLockedInvoiceOperations_ShouldExposeEveryNotFoundContract() {
        UUID id = UUID.randomUUID();
        when(repo.findByIdForUpdate(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.update(id, mock(InvoiceUpdateRequest.class)));
        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.applyInsurance(id, mock(InvoiceInsuranceRequest.class)));
        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.recalculatePaidAmount(id));
    }

    @Test
    void pay_ShouldRejectUnknownCashierAfterPersistingThePaidInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID cashierId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).invoiceCode("INV-X")
                .status(InvoiceStatus.PENDING).totalAmount(BigDecimal.TEN)
                .paidAmount(BigDecimal.ZERO).items(new ArrayList<>()).build();
        when(repo.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(transactionRepo.findByInvoice_InvoiceId(invoiceId)).thenReturn(List.of());
        when(repo.save(invoice)).thenReturn(invoice);
        when(staffRepo.findById(cashierId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.pay(invoiceId, cashierId));
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
    }

    @Test
    void paymentHistory_ShouldMapCustomerAndLatestTransactionForSortedAndDefaultPages() {
        UUID customerId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Profile customer = Profile.builder().profileId(customerId).fullName("Nguyễn An").build();
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).invoiceCode("INV-HISTORY")
                .customer(customer).status(InvoiceStatus.PAID).issueDate(LocalDate.now())
                .subtotal(BigDecimal.TEN).discount(BigDecimal.ZERO).tax(BigDecimal.ZERO)
                .totalAmount(BigDecimal.TEN).paidAmount(BigDecimal.TEN)
                .items(new ArrayList<>()).build();
        Transaction transaction = Transaction.builder().transactionId(UUID.randomUUID())
                .invoice(invoice).amount(BigDecimal.TEN).status(TransactionStatus.SUCCESS)
                .paymentMethod(PaymentMethod.CASH).build();
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(invoice)));
        when(transactionRepo.findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                invoiceId, TransactionStatus.SUCCESS)).thenReturn(Optional.of(transaction));

        assertEquals(1, invoiceService.getPaymentHistoryForPatient(customerId, null, null,
                null, PageRequest.of(0, 10)).content().size());

        when(transactionRepo.findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                invoiceId, TransactionStatus.SUCCESS)).thenReturn(Optional.empty());
        assertEquals(1, invoiceService.getPaymentHistoryForPatient(customerId, null, null,
                null, PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by("issueDate"))).content().size());
    }

    @Test
    void create_ShouldRejectCustomerAndMedicalRecordThatBelongToAnotherVisit() {
        UUID customerId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Profile requested = Profile.builder().profileId(customerId).build();
        Profile owner = Profile.builder().profileId(UUID.randomUUID()).build();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).customer(owner).build();
        when(profileRepo.findById(customerId)).thenReturn(Optional.of(requested));
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        InvoiceCreateRequest mismatch = new InvoiceCreateRequest(customerId, visitId, null,
                null, null, null, null, null, null);
        assertThrows(ConflictException.class, () -> invoiceService.create(mismatch));

        UUID recordId = UUID.randomUUID();
        CustomerVisit anotherVisit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        MedicalRecord record = MedicalRecord.builder().recordId(recordId).visit(anotherVisit).build();
        when(recordRepo.findById(recordId)).thenReturn(Optional.of(record));
        InvoiceCreateRequest wrongRecord = new InvoiceCreateRequest(null, visitId, recordId,
                null, null, null, null, null, null);
        assertThrows(ConflictException.class, () -> invoiceService.create(wrongRecord));
    }

    @Test
    void dispatch_ShouldRejectMissingLockedVisitAndServiceWithoutDepartmentType() {
        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).build();
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).visit(visit).status(InvoiceStatus.PAID).build();
        when(repo.getWithDetailsByInvoiceId(invoiceId)).thenReturn(Optional.of(invoice));
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> ReflectionTestUtils.invokeMethod(
                invoiceService, "createQueueTicketsFromInvoiceItems", invoice));

        MedicalService unclassified = MedicalService.builder().serviceId(UUID.randomUUID())
                .name("Dịch vụ chưa phân loại").build();
        InvoiceItem item = InvoiceItem.builder().itemId(UUID.randomUUID()).service(unclassified).build();
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(queueTicketRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());
        when(testRequestRepo.findAllByMedicalRecord_Visit_VisitId(visitId)).thenReturn(List.of());
        when(itemRepo.findAllWithServiceByInvoiceId(invoiceId)).thenReturn(List.of(item));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(
                invoiceService, "createQueueTicketsFromInvoiceItems", invoice));
    }

    @Test
    void normalizeItems_ShouldRetainOnlyServicesSelectedByPolicy() {
        UUID keepId = UUID.randomUUID();
        UUID removeId = UUID.randomUUID();
        InvoiceItemCreateRequest keep = new InvoiceItemCreateRequest(
                keepId, "Giữ", "KEEP", BigDecimal.ONE, 1, null, null, null, null);
        InvoiceItemCreateRequest remove = new InvoiceItemCreateRequest(
                removeId, "Bỏ", "REMOVE", BigDecimal.ONE, 1, null, null, null, null);
        MedicalServiceSelectionPolicyService selectionPolicy = mock(MedicalServiceSelectionPolicyService.class);
        ReflectionTestUtils.setField(invoiceService, "serviceSelectionPolicyService", selectionPolicy);
        when(selectionPolicy.normalizeOrThrow(List.of(keepId, removeId)))
                .thenReturn(List.of(MedicalService.builder().serviceId(keepId).build()));

        @SuppressWarnings("unchecked")
        List<InvoiceItemCreateRequest> normalized = ReflectionTestUtils.invokeMethod(
                invoiceService, "normalizeItemRequests", List.of(keep, remove));
        assertEquals(List.of(keep), normalized);
    }

    @Test
    void applyInsurance_ShouldCoverMissingServiceTypeNullRuleRateAndNullLineTotal() {
        UUID invoiceId = UUID.randomUUID();
        UUID insuranceId = UUID.randomUUID();
        Profile customer = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn An")
                .dateOfBirth(LocalDate.of(2000, 1, 1)).build();
        InvoiceItem item = InvoiceItem.builder().itemId(UUID.randomUUID()).service(null)
                .lineTotal(null).build();
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).status(InvoiceStatus.PENDING)
                .customer(customer).items(new ArrayList<>(List.of(item)))
                .subtotal(BigDecimal.ZERO).discount(BigDecimal.ZERO).tax(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO).paidAmount(BigDecimal.ZERO).build();
        Insurance insurance = Insurance.builder().insuranceId(insuranceId).build();
        InsuranceRule nullRate = InsuranceRule.builder().insurance(insurance)
                .departmentType(DepartmentType.EXAMINATION).discountPercent(null).build();
        when(repo.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(transactionRepo.findByInvoice_InvoiceId(invoiceId)).thenReturn(List.of());
        when(insuranceRepository.findById(insuranceId)).thenReturn(Optional.of(insurance));
        when(bhxhIntegrationService.checkBhytCard("CARD"))
                .thenReturn(new BhxhCheckResponse(true, "OK", null, null,
                        "SKIP_VALIDATION", "ignored"));
        when(insuranceRuleRepository.findByInsurance_InsuranceId(insuranceId))
                .thenReturn(List.of(nullRate));
        // Service is mandatory in persisted invoice items.  This deliberately
        // malformed legacy row still exercises the defensive insurance math;
        // response mapping then rejects the impossible row with an NPE.
        assertThrows(NullPointerException.class, () -> invoiceService.applyInsurance(invoiceId,
                new InvoiceInsuranceRequest(insuranceId, " CARD ")));
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getBhytFund()));
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getFinalPrice()));
    }

    @Test
    void create_ShouldCoverVisitWithoutCustomerAndRecordWithoutVisit() {
        UUID visitId = UUID.randomUUID();
        CustomerVisit guestVisit = CustomerVisit.builder().visitId(visitId).build();
        when(visitRepo.findByIdForUpdate(visitId)).thenReturn(Optional.of(guestVisit));
        InvoiceCreateRequest guest = new InvoiceCreateRequest(null, visitId, null,
                null, null, null, null, null, null);
        assertThrows(ConflictException.class, () -> invoiceService.create(guest));

        UUID recordId = UUID.randomUUID();
        MedicalRecord detached = MedicalRecord.builder().recordId(recordId).build();
        when(recordRepo.findById(recordId)).thenReturn(Optional.of(detached));
        InvoiceCreateRequest detachedRecord = new InvoiceCreateRequest(null, null, recordId,
                null, null, null, null, null, null);
        assertThrows(ConflictException.class, () -> invoiceService.create(detachedRecord));
    }

    @Test
    void selectExaminationRoomForVisit_ShouldDelegateMissingSpecializationAndReuseCachedRoom() {
        MedicalService missingSpecialization = MedicalService.builder().name("Khám chưa cấu hình").build();
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(invoiceService,
                "selectExaminationRoomForVisit", missingSpecialization, new java.util.HashMap<UUID, Department>()));

        UUID specializationId = UUID.randomUUID();
        Specialization specialization = Specialization.builder().specializationId(specializationId)
                .name("Nội khoa").build();
        Department cached = Department.builder().departmentId(UUID.randomUUID()).build();
        MedicalService service = MedicalService.builder().name("Khám Nội")
                .requiredSpecialization(specialization).build();
        java.util.Map<UUID, Department> rooms = new java.util.HashMap<>();
        rooms.put(specializationId, cached);

        Department selected = ReflectionTestUtils.invokeMethod(invoiceService,
                "selectExaminationRoomForVisit", service, rooms);
        assertSame(cached, selected);
        verifyNoInteractions(departmentRepo);
    }

    @Test
    void getReceiptDetail_ShouldCoverSuccessfulPaymentWithoutMembershipLedger() {
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Profile customer = Profile.builder().profileId(customerId).fullName("Nguyễn An").build();
        Invoice invoice = Invoice.builder().invoiceId(invoiceId).invoiceCode("INV-LEDGER")
                .customer(customer).status(InvoiceStatus.PAID).items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO).discount(BigDecimal.ZERO).tax(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO).paidAmount(BigDecimal.ZERO).build();
        Transaction payment = Transaction.builder().transactionId(UUID.randomUUID())
                .amount(BigDecimal.ZERO).status(TransactionStatus.SUCCESS).build();
        when(repo.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(transactionRepo.findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                invoiceId, TransactionStatus.SUCCESS)).thenReturn(Optional.of(payment));
        when(membershipCardLedgerRepo.findByPaymentTransaction_TransactionId(payment.getTransactionId()))
                .thenReturn(Optional.empty());

        assertNotNull(invoiceService.getReceiptDetail(invoiceId, customerId));
        verify(membershipCardLedgerRepo).findByPaymentTransaction_TransactionId(payment.getTransactionId());
    }

    @Test
    void validateServiceRegistrations_ShouldInvokeSelectionPolicyForExistingVisit() {
        MedicalServiceSelectionPolicyService selectionPolicy = mock(MedicalServiceSelectionPolicyService.class);
        ReflectionTestUtils.setField(invoiceService, "serviceSelectionPolicyService", selectionPolicy);
        UUID visitId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId).build();
        MedicalService service = MedicalService.builder().serviceId(serviceId)
                .departmentType(DepartmentType.LABORATORY).build();
        when(visitRepo.findById(visitId)).thenReturn(Optional.of(visit));
        when(serviceRepo.findById(serviceId)).thenReturn(Optional.of(service));
        when(itemRepo.findDistinctActiveServiceIdsByVisit(visitId, null)).thenReturn(List.of());
        when(itemRepo.findDistinctExaminationServiceIdsByVisit(visitId, null)).thenReturn(List.of());
        when(queueTicketRepo.findAllByVisit_VisitId(visitId)).thenReturn(List.of());

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(invoiceService,
                "validateServiceRegistrations", List.of(invoiceItemRequest(serviceId)), visitId, null));
        verify(selectionPolicy).validateAgainstExisting(List.of(serviceId), List.of());
    }
}
