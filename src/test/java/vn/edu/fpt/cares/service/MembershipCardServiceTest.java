package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.dto.invoice.InvoiceResponse;
import vn.edu.fpt.cares.dto.membership.*;
import vn.edu.fpt.cares.enums.*;
import vn.edu.fpt.cares.exception.*;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipCardServiceTest {
    @Mock MembershipPolicyRepository policyRepository;
    @Mock MembershipCardRepository cardRepository;
    @Mock MembershipCardLedgerRepository ledgerRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock InvoiceItemRepository itemRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock StaffInfoRepository staffRepository;
    @Mock QueueTicketRepository queueTicketRepository;
    @Mock TestRequestRepository testRequestRepository;
    @Mock FamilyAccessService familyAccessService;
    @Mock InvoiceService invoiceService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks MembershipCardService service;

    private UUID accountId;
    private Profile owner;
    private MembershipPolicy policy;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        owner = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Anh Đức")
                .account(Account.builder().accountId(accountId).build()).build();
        policy = MembershipPolicy.builder().policyId(UUID.randomUUID())
                .minimumTopUp(new BigDecimal("1000000")).discountPercent(new BigDecimal("15"))
                .validityMonths(12).active(true).build();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void registerValidatesTermsDuplicateAndCreatesPendingCard() {
        assertThrows(BadRequestException.class,
                () -> service.register(accountId, new MembershipCardRequest("123456", false)));
        verifyNoInteractions(familyAccessService);

        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        when(cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId()))
                .thenReturn(Optional.of(card(MembershipCardStatus.ACTIVE)));
        assertThrows(ConflictException.class,
                () -> service.register(accountId, new MembershipCardRequest("123456", true)));

        reset(cardRepository);
        when(cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId())).thenReturn(Optional.empty());
        when(policyRepository.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(policy));
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(cardRepository.save(any())).thenAnswer(invocation -> {
            MembershipCard saved = invocation.getArgument(0);
            saved.setCardId(UUID.randomUUID());
            return saved;
        });

        MembershipCardResponse response = service.register(accountId,
                new MembershipCardRequest("123456", true));
        assertEquals("PENDING", response.status());
        assertEquals(BigDecimal.ZERO, response.balance());
        assertEquals(new BigDecimal("15"), response.benefitPercent());
        assertTrue(response.cardCode().startsWith("CS-"));
        ArgumentCaptor<MembershipCard> saved = ArgumentCaptor.forClass(MembershipCard.class);
        verify(cardRepository).save(saved.capture());
        assertEquals("hash", saved.getValue().getPinHash());
        assertSame(owner, saved.getValue().getOwnerProfile());
    }

    @Test
    void myCardReturnsCardOrReportsMissingRegistration() {
        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        MembershipCard active = card(MembershipCardStatus.ACTIVE);
        when(cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId()))
                .thenReturn(Optional.of(active), Optional.empty());
        assertEquals(active.getCardCode(), service.myCard(accountId).cardCode());
        assertThrows(ResourceNotFoundException.class, () -> service.myCard(accountId));
    }

    @Test
    void resetPinRequiresMatchingConfirmationAndCurrentAccountPassword() {
        assertThrows(BadRequestException.class, () -> service.resetPin(accountId,
                new MembershipPinResetRequest("password", "123456", "654321")));
        verifyNoInteractions(familyAccessService);

        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        owner.getAccount().setPasswordHash("stored-password");
        when(passwordEncoder.matches("wrong-password", "stored-password")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> service.resetPin(accountId,
                new MembershipPinResetRequest("wrong-password", "123456", "123456")));
        verify(cardRepository, never()).save(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void resetPinReplacesHashAndClearsFailedPinCounter() {
        MembershipCard card = card(MembershipCardStatus.ACTIVE);
        owner.getAccount().setPasswordHash("stored-password");
        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        when(passwordEncoder.matches("current-password", "stored-password")).thenReturn(true);
        when(cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId())).thenReturn(Optional.of(card));
        when(cardRepository.findByIdForUpdate(card.getCardId())).thenReturn(Optional.of(card));
        when(passwordEncoder.encode("654321")).thenReturn("new-pin-hash");
        when(cardRepository.save(card)).thenReturn(card);

        MembershipCardResponse response = service.resetPin(accountId,
                new MembershipPinResetRequest("current-password", "654321", "654321"));

        assertEquals(card.getCardCode(), response.cardCode());
        assertEquals("new-pin-hash", card.getPinHash());
        verify(redisTemplate).delete("membership:pin-fail:" + card.getCardId());
    }

    @Test
    void topUpIsIdempotentOnlyForSameCard() {
        MembershipCard card = card(MembershipCardStatus.ACTIVE);
        MembershipCardLedger duplicate = ledger(card, MembershipLedgerType.TOP_UP, new BigDecimal("300000"));
        when(cardRepository.findByCodeForUpdate(card.getCardCode())).thenReturn(Optional.of(card));
        when(ledgerRepository.findByIdempotencyKey("key")).thenReturn(Optional.of(duplicate));
        assertEquals(new BigDecimal("300000"), service.topUp(card.getCardCode(),
                new MembershipTopUpRequest(new BigDecimal("300000"), PaymentMethod.CASH, "key"), null).amount());
        verify(cardRepository, never()).save(any());

        duplicate.setCard(card(MembershipCardStatus.ACTIVE));
        assertThrows(ConflictException.class, () -> service.topUp(card.getCardCode(),
                new MembershipTopUpRequest(BigDecimal.ONE, PaymentMethod.CASH, "key"), null));
    }

    @Test
    void topUpValidatesCardStateMethodAndFirstMinimum() {
        var request = new MembershipTopUpRequest(new BigDecimal("500000"), PaymentMethod.CASH, "key");
        when(cardRepository.findByCodeForUpdate("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.topUp("missing", request, null));

        MembershipCard card = card(MembershipCardStatus.CLOSED);
        when(cardRepository.findByCodeForUpdate("card")).thenReturn(Optional.of(card));
        assertThrows(ConflictException.class, () -> service.topUp("card", request, null));
        card.setStatus(MembershipCardStatus.SUSPENDED);
        assertThrows(ConflictException.class, () -> service.topUp("card", request, null));
        card.setStatus(MembershipCardStatus.ACTIVE);
        assertThrows(BadRequestException.class, () -> service.topUp("card",
                new MembershipTopUpRequest(BigDecimal.TEN, PaymentMethod.MEMBERSHIP_CARD, "key"), null));
        assertThrows(BadRequestException.class, () -> service.topUp("card",
                new MembershipTopUpRequest(BigDecimal.TEN, PaymentMethod.INSURANCE, "key"), null));
        card.setStatus(MembershipCardStatus.PENDING);
        when(policyRepository.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(policy));
        assertThrows(BadRequestException.class, () -> service.topUp("card", request, null));
    }

    @Test
    void firstTopUpActivatesAwardsBenefitAndRecordsCashier() {
        MembershipCard card = card(MembershipCardStatus.PENDING);
        card.setBalance(BigDecimal.ZERO);
        StaffInfo cashier = StaffInfo.builder().staffId(UUID.randomUUID())
                .profile(Profile.builder().fullName("Lê Quốc Bảo").build()).build();
        when(cardRepository.findByCodeForUpdate(card.getCardCode())).thenReturn(Optional.of(card));
        when(policyRepository.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(policy));
        when(staffRepository.findById(cashier.getStaffId())).thenReturn(Optional.of(cashier));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MembershipTopUpResponse response = service.topUp(card.getCardCode(),
                new MembershipTopUpRequest(new BigDecimal("1000000"), PaymentMethod.CASH, "first"),
                cashier.getStaffId());
        assertEquals(MembershipCardStatus.ACTIVE, card.getStatus());
        assertNotNull(card.getActivatedAt());
        assertNotNull(card.getBenefitStartsAt());
        assertEquals(card.getActivatedAt().toLocalDate().plusDays(1), card.getBenefitStartsAt().toLocalDate());
        assertEquals(LocalDateTime.of(card.getBenefitStartsAt().toLocalDate(), java.time.LocalTime.MIDNIGHT),
                card.getBenefitStartsAt());
        assertNotNull(card.getBenefitExpiresAt());
        assertEquals(new BigDecimal("1000000"), card.getBalance());
        assertEquals("Lê Quốc Bảo", response.cashierName());
        ArgumentCaptor<MembershipCardLedger> ledger = ArgumentCaptor.forClass(MembershipCardLedger.class);
        verify(ledgerRepository).save(ledger.capture());
        assertEquals(BigDecimal.ZERO, ledger.getValue().getBalanceBefore());
    }

    @Test
    void qualifyingTopUpExtendsFutureBenefitWhileSmallTopUpDoesNot() {
        MembershipCard card = card(MembershipCardStatus.ACTIVE);
        LocalDateTime expiry = LocalDateTime.now().plusMonths(2);
        card.setBenefitStartsAt(LocalDateTime.now().minusDays(1));
        card.setBenefitExpiresAt(expiry);
        when(cardRepository.findByCodeForUpdate(card.getCardCode())).thenReturn(Optional.of(card));
        when(policyRepository.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(policy));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.topUp(card.getCardCode(), new MembershipTopUpRequest(
                new BigDecimal("1000000"), PaymentMethod.BANK_TRANSFER, "large"), null);
        assertEquals(expiry.plusMonths(12), card.getBenefitExpiresAt());
        LocalDateTime extended = card.getBenefitExpiresAt();
        service.topUp(card.getCardCode(), new MembershipTopUpRequest(
                new BigDecimal("50000"), PaymentMethod.CASH, "small"), null);
        assertEquals(extended, card.getBenefitExpiresAt());
        assertEquals(new BigDecimal("1150000"), card.getBalance());
    }

    @Test
    void payRejectsMissingRegistrationAndConflictingIdempotency() {
        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        when(cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId())).thenReturn(Optional.empty());
        MembershipPaymentRequest request = paymentRequest(UUID.randomUUID(), owner.getProfileId(), false,
                BigDecimal.TEN, "pay");
        assertThrows(ResourceNotFoundException.class, () -> service.pay(accountId, request));

        MembershipCard card = card(MembershipCardStatus.ACTIVE);
        when(cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId())).thenReturn(Optional.of(card));
        when(cardRepository.findByIdForUpdate(card.getCardId())).thenReturn(Optional.of(card));
        MembershipCardLedger duplicate = ledger(card, MembershipLedgerType.PAYMENT, BigDecimal.TEN);
        duplicate.setInvoice(Invoice.builder().invoiceId(UUID.randomUUID()).build());
        when(ledgerRepository.findByIdempotencyKey("pay")).thenReturn(Optional.of(duplicate));
        assertThrows(ConflictException.class, () -> service.pay(accountId, request));
        duplicate.setInvoice(null);
        assertThrows(ConflictException.class, () -> service.pay(accountId, request));
    }

    @Test
    void payIdempotentReplayReturnsExistingResponseWithoutCharging() {
        MembershipCard card = card(MembershipCardStatus.ACTIVE);
        UUID invoiceId = UUID.randomUUID();
        MembershipCardLedger duplicate = ledger(card, MembershipLedgerType.PAYMENT, BigDecimal.TEN);
        duplicate.setInvoice(Invoice.builder().invoiceId(invoiceId).build());
        InvoiceResponse expected = mock(InvoiceResponse.class);
        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        when(cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId())).thenReturn(Optional.of(card));
        when(cardRepository.findByIdForUpdate(card.getCardId())).thenReturn(Optional.of(card));
        when(ledgerRepository.findByIdempotencyKey("pay")).thenReturn(Optional.of(duplicate));
        when(invoiceService.get(invoiceId)).thenReturn(expected);
        assertSame(expected, service.pay(accountId,
                paymentRequest(invoiceId, owner.getProfileId(), false, null, "pay")));
        verifyNoInteractions(passwordEncoder);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void pinLockAndWrongPinAreRejectedAndFirstFailureGetsExpiry() {
        PayFixture fixture = payFixture(false);
        when(valueOperations.get(anyString())).thenReturn("5");
        assertThrows(BadRequestException.class, () -> service.pay(accountId, fixture.request));
        verify(passwordEncoder, never()).matches(anyString(), anyString());

        reset(valueOperations, passwordEncoder);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(passwordEncoder.matches("123456", fixture.card.getPinHash())).thenReturn(false);
        assertThrows(BadRequestException.class, () -> service.pay(accountId, fixture.request));
        verify(redisTemplate).expire(anyString(), eq(15L), eq(TimeUnit.MINUTES));
    }

    @Test
    void laterWrongPinDoesNotResetExpiry() {
        PayFixture fixture = payFixture(false);
        when(valueOperations.get(anyString())).thenReturn("2");
        when(valueOperations.increment(anyString())).thenReturn(3L);
        when(passwordEncoder.matches("123456", fixture.card.getPinHash())).thenReturn(false);
        assertThrows(BadRequestException.class, () -> service.pay(accountId, fixture.request));
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void payValidatesCardPatientStatusRemainingAndBalance() {
        PayFixture fixture = payFixture(false);
        fixture.card.setStatus(MembershipCardStatus.SUSPENDED);
        assertThrows(ConflictException.class, () -> service.pay(accountId, fixture.request));
        fixture.card.setStatus(MembershipCardStatus.ACTIVE);
        fixture.invoice.setCustomer(Profile.builder().profileId(UUID.randomUUID()).build());
        assertThrows(BadRequestException.class, () -> service.pay(accountId, fixture.request));
        fixture.invoice.setCustomer(fixture.patient);
        fixture.invoice.setStatus(InvoiceStatus.PAID);
        assertThrows(ConflictException.class, () -> service.pay(accountId, fixture.request));
        fixture.invoice.setStatus(InvoiceStatus.PENDING);
        fixture.invoice.setPaidAmount(fixture.invoice.getTotalAmount());
        assertThrows(ConflictException.class, () -> service.pay(accountId, fixture.request));
        fixture.invoice.setPaidAmount(BigDecimal.ZERO);
        fixture.card.setBalance(BigDecimal.ONE);
        assertThrows(BadRequestException.class, () -> service.pay(accountId, fixture.request));
    }

    @Test
    void payWithoutBenefitUsesPartialAmountAndPersistsAuditTrail() {
        PayFixture fixture = payFixture(false);
        fixture.request = paymentRequest(fixture.invoice.getInvoiceId(), fixture.patient.getProfileId(), false,
                new BigDecimal("30000"), "partial");
        InvoiceResponse expected = mock(InvoiceResponse.class);
        when(invoiceService.get(fixture.invoice.getInvoiceId())).thenReturn(expected);
        assertSame(expected, service.pay(accountId, fixture.request));
        assertEquals(new BigDecimal("970000"), fixture.card.getBalance());
        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(tx.capture());
        assertEquals(new BigDecimal("30000"), tx.getValue().getAmount());
        ArgumentCaptor<MembershipCardLedger> ledger = ArgumentCaptor.forClass(MembershipCardLedger.class);
        verify(ledgerRepository).save(ledger.capture());
        assertEquals(BigDecimal.ZERO, ledger.getValue().getBenefitDiscount());
        verify(invoiceService).recalculatePaidAmount(fixture.invoice.getInvoiceId());
        verify(redisTemplate).delete("membership:pin-fail:" + fixture.card.getCardId());
    }

    @Test
    void payWithBenefitDiscountsPatientPayableAndChargesWholeInvoice() {
        PayFixture fixture = payFixture(true);
        fixture.invoice.setSubtotal(new BigDecimal("100000"));
        fixture.invoice.setDiscount(new BigDecimal("20000"));
        fixture.invoice.setTotalAmount(new BigDecimal("80000"));
        fixture.request = paymentRequest(fixture.invoice.getInvoiceId(), fixture.patient.getProfileId(), true,
                new BigDecimal("1000"), "benefit");
        when(transactionRepository.findByInvoice_InvoiceId(fixture.invoice.getInvoiceId())).thenReturn(List.of());
        when(invoiceService.get(fixture.invoice.getInvoiceId())).thenReturn(mock(InvoiceResponse.class));
        service.pay(accountId, fixture.request);
        assertEquals(new BigDecimal("32000.00"), fixture.invoice.getDiscount());
        assertEquals(new BigDecimal("68000.00"), fixture.invoice.getTotalAmount());
        assertEquals(new BigDecimal("932000.00"), fixture.card.getBalance());
        ArgumentCaptor<MembershipCardLedger> ledger = ArgumentCaptor.forClass(MembershipCardLedger.class);
        verify(ledgerRepository).save(ledger.capture());
        assertEquals(new BigDecimal("12000.00"), ledger.getValue().getBenefitDiscount());
        assertEquals(new BigDecimal("68000.00"), ledger.getValue().getAmount());
    }

    @Test
    void benefitCannotBeAddedAfterSuccessfulInvoicePayment() {
        PayFixture fixture = payFixture(true);
        when(transactionRepository.findByInvoice_InvoiceId(fixture.invoice.getInvoiceId())).thenReturn(List.of(
                Transaction.builder().status(TransactionStatus.FAILED).build(),
                Transaction.builder().status(TransactionStatus.SUCCESS).build()));
        assertThrows(ConflictException.class, () -> service.pay(accountId, fixture.request));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void expiredBenefitFallsBackToNormalPaymentAndCapsAmount() {
        PayFixture fixture = payFixture(false);
        fixture.card.setBenefitExpiresAt(LocalDateTime.now().minusDays(1));
        fixture.request = paymentRequest(fixture.invoice.getInvoiceId(), fixture.patient.getProfileId(), true,
                new BigDecimal("999999"), "expired");
        when(invoiceService.get(fixture.invoice.getInvoiceId())).thenReturn(mock(InvoiceResponse.class));
        service.pay(accountId, fixture.request);
        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(tx.capture());
        assertEquals(fixture.invoice.getTotalAmount(), tx.getValue().getAmount());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void benefitWaitsUntilNextDayAndDoesNotApplyToOlderInvoice() {
        PayFixture waiting = payFixture(true);
        waiting.card.setBenefitStartsAt(LocalDateTime.now().plusDays(1).toLocalDate().atStartOfDay());
        waiting.request = paymentRequest(waiting.invoice.getInvoiceId(), waiting.patient.getProfileId(), true,
                null, "waiting-benefit");
        when(invoiceService.get(waiting.invoice.getInvoiceId())).thenReturn(mock(InvoiceResponse.class));
        service.pay(accountId, waiting.request);
        assertEquals(BigDecimal.ZERO, waiting.invoice.getDiscount());

        reset(transactionRepository, ledgerRepository, invoiceService);
        PayFixture oldInvoice = payFixture(true);
        oldInvoice.card.setBenefitStartsAt(LocalDateTime.now().minusDays(1).toLocalDate().atStartOfDay());
        oldInvoice.invoice.setIssueDate(oldInvoice.card.getBenefitStartsAt().toLocalDate().minusDays(1));
        oldInvoice.request = paymentRequest(oldInvoice.invoice.getInvoiceId(), oldInvoice.patient.getProfileId(), true,
                null, "old-invoice");
        doAnswer(invocation -> invocation.getArgument(0)).when(transactionRepository).save(any());
        doAnswer(invocation -> invocation.getArgument(0)).when(ledgerRepository).save(any());
        when(invoiceService.get(oldInvoice.invoice.getInvoiceId())).thenReturn(mock(InvoiceResponse.class));
        service.pay(accountId, oldInvoice.request);
        assertEquals(BigDecimal.ZERO, oldInvoice.invoice.getDiscount());
    }

    @Test
    void counterPaymentValidatesThenDelegates() {
        var request = new MembershipCounterPaymentRequest(" cs-demo ", UUID.randomUUID(), "123456",
                false, BigDecimal.TEN, "counter");
        when(cardRepository.findByCardCode("CS-DEMO")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.payAtCounter(request));
        MembershipCard card = card(MembershipCardStatus.ACTIVE);
        card.getOwnerProfile().setAccount(null);
        when(cardRepository.findByCardCode("CS-DEMO")).thenReturn(Optional.of(card));
        assertThrows(BadRequestException.class, () -> service.payAtCounter(request));
        card.getOwnerProfile().setAccount(Account.builder().accountId(accountId).build());
        when(invoiceRepository.findById(request.invoiceId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.payAtCounter(request));

        Profile patient = Profile.builder().profileId(UUID.randomUUID()).build();
        Invoice invoice = invoice(request.invoiceId(), patient);
        when(invoiceRepository.findById(request.invoiceId())).thenReturn(Optional.of(invoice));
        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        when(cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId())).thenReturn(Optional.of(card));
        when(cardRepository.findByIdForUpdate(card.getCardId())).thenReturn(Optional.of(card));
        when(passwordEncoder.matches("123456", card.getPinHash())).thenReturn(true);
        when(familyAccessService.resolveActiveProfile(accountId, patient.getProfileId())).thenReturn(patient);
        when(invoiceRepository.findByIdForUpdate(invoice.getInvoiceId())).thenReturn(Optional.of(invoice));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceService.get(invoice.getInvoiceId())).thenReturn(mock(InvoiceResponse.class));
        assertNotNull(service.payAtCounter(request));
        verify(transactionRepository).save(any());
    }

    @Test
    void reverseValidatesOriginalReplayAndInvoice() {
        UUID ledgerId = UUID.randomUUID();
        MembershipCardLedger topUp = ledger(card(MembershipCardStatus.ACTIVE), MembershipLedgerType.TOP_UP,
                BigDecimal.TEN);
        when(ledgerRepository.findById(ledgerId)).thenReturn(Optional.empty(), Optional.of(topUp));
        assertThrows(ResourceNotFoundException.class,
                () -> service.reverse(ledgerId, new MembershipReversalRequest("Sai", "key"), null));
        assertThrows(BadRequestException.class,
                () -> service.reverse(ledgerId, new MembershipReversalRequest("Sai", "key"), null));

        ReverseFixture fixture = reverseFixture();
        when(ledgerRepository.existsByReversedLedger_LedgerId(fixture.original.getLedgerId())).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> service.reverse(fixture.original.getLedgerId(), fixture.request, null));
        when(ledgerRepository.existsByReversedLedger_LedgerId(fixture.original.getLedgerId())).thenReturn(false);
        MembershipCardLedger replay = ledger(fixture.card, MembershipLedgerType.REVERSAL, fixture.original.getAmount());
        when(ledgerRepository.findByIdempotencyKey(fixture.request.idempotencyKey())).thenReturn(Optional.of(replay));
        assertEquals("REVERSAL", service.reverse(fixture.original.getLedgerId(), fixture.request, null).type());
        when(ledgerRepository.findByIdempotencyKey(fixture.request.idempotencyKey())).thenReturn(Optional.empty());
        when(invoiceRepository.findByIdForUpdate(fixture.invoice.getInvoiceId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.reverse(fixture.original.getLedgerId(), fixture.request, null));
    }

    @ParameterizedTest
    @EnumSource(value = QueueStatus.class, names = {"IN_PROGRESS", "DONE", "TEST_DONE", "WAITING_FOR_TEST"})
    void reverseRejectsStartedQueueStates(QueueStatus status) {
        ReverseFixture fixture = reverseFixture();
        MedicalService serviceItem = MedicalService.builder().serviceId(UUID.randomUUID()).build();
        when(itemRepository.findAllWithServiceByInvoiceId(fixture.invoice.getInvoiceId()))
                .thenReturn(List.of(InvoiceItem.builder().service(serviceItem).build()));
        when(queueTicketRepository.findAllByVisit_VisitId(fixture.invoice.getVisit().getVisitId()))
                .thenReturn(List.of(QueueTicket.builder().service(serviceItem).status(status).build()));
        assertThrows(ConflictException.class,
                () -> service.reverse(fixture.original.getLedgerId(), fixture.request, null));
    }

    @ParameterizedTest
    @EnumSource(value = TestRequestStatus.class, names = {"IN_PROGRESS", "COMPLETED"})
    void reverseRejectsStartedTests(TestRequestStatus status) {
        ReverseFixture fixture = reverseFixture();
        when(itemRepository.findAllWithServiceByInvoiceId(fixture.invoice.getInvoiceId())).thenReturn(List.of());
        when(queueTicketRepository.findAllByVisit_VisitId(fixture.invoice.getVisit().getVisitId())).thenReturn(List.of());
        when(testRequestRepository.findByInvoiceId(fixture.invoice.getInvoiceId()))
                .thenReturn(List.of(TestRequest.builder().status(status).build()));
        assertThrows(ConflictException.class,
                () -> service.reverse(fixture.original.getLedgerId(), fixture.request, null));
    }

    @Test
    void reverseCancelsUntouchedWorkRefundsAndRestoresBenefit() {
        ReverseFixture fixture = reverseFixture();
        MedicalService serviceItem = MedicalService.builder().serviceId(UUID.randomUUID()).build();
        QueueTicket waiting = QueueTicket.builder().service(serviceItem).status(QueueStatus.WAITING).build();
        QueueTicket blocked = QueueTicket.builder().service(serviceItem).status(QueueStatus.BLOCKED).build();
        QueueTicket skipped = QueueTicket.builder().service(serviceItem).status(QueueStatus.SKIPPED).build();
        TestRequest pending = TestRequest.builder().status(TestRequestStatus.PENDING).build();
        StaffInfo cashier = StaffInfo.builder().staffId(UUID.randomUUID()).build();
        when(itemRepository.findAllWithServiceByInvoiceId(fixture.invoice.getInvoiceId()))
                .thenReturn(List.of(InvoiceItem.builder().service(serviceItem).build()));
        when(queueTicketRepository.findAllByVisit_VisitId(fixture.invoice.getVisit().getVisitId()))
                .thenReturn(List.of(waiting, blocked, skipped));
        when(testRequestRepository.findByInvoiceId(fixture.invoice.getInvoiceId())).thenReturn(List.of(pending));
        when(cardRepository.findByIdForUpdate(fixture.card.getCardId())).thenReturn(Optional.of(fixture.card));
        when(transactionRepository.findByIdForUpdate(fixture.transaction.getTransactionId()))
                .thenReturn(Optional.of(fixture.transaction));
        when(staffRepository.findById(cashier.getStaffId())).thenReturn(Optional.of(cashier));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> {
            MembershipCardLedger saved = invocation.getArgument(0);
            saved.setLedgerId(UUID.randomUUID());
            return saved;
        });

        MembershipLedgerResponse result = service.reverse(fixture.original.getLedgerId(), fixture.request,
                cashier.getStaffId());
        assertEquals("REVERSAL", result.type());
        assertEquals(new BigDecimal("150000"), fixture.card.getBalance());
        assertEquals(QueueStatus.SKIPPED, waiting.getStatus());
        assertEquals(QueueStatus.SKIPPED, blocked.getStatus());
        assertEquals(QueueStatus.SKIPPED, skipped.getStatus());
        assertEquals(TestRequestStatus.CANCELLED, pending.getStatus());
        assertEquals(TransactionStatus.CANCELLED, fixture.transaction.getStatus());
        assertTrue(fixture.transaction.getNote().contains("Khách yêu cầu"));
        assertEquals(BigDecimal.ZERO, fixture.invoice.getDiscount());
        assertEquals(new BigDecimal("100000"), fixture.invoice.getTotalAmount());
        verify(invoiceService).recalculatePaidAmount(fixture.invoice.getInvoiceId());
    }

    @Test
    void reverseWithoutVisitOrBenefitDoesNotRewriteInvoice() {
        ReverseFixture fixture = reverseFixture();
        fixture.invoice.setVisit(null);
        fixture.original.setBenefitDiscount(null);
        when(testRequestRepository.findByInvoiceId(fixture.invoice.getInvoiceId())).thenReturn(List.of());
        when(cardRepository.findByIdForUpdate(fixture.card.getCardId())).thenReturn(Optional.of(fixture.card));
        when(transactionRepository.findByIdForUpdate(fixture.transaction.getTransactionId()))
                .thenReturn(Optional.of(fixture.transaction));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.reverse(fixture.original.getLedgerId(), fixture.request, null);
        verifyNoInteractions(itemRepository, queueTicketRepository);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void historiesMapLedgersAndMissingCardIsReported() {
        MembershipCard card = card(MembershipCardStatus.ACTIVE);
        MembershipCardLedger ledger = ledger(card, MembershipLedgerType.TOP_UP, BigDecimal.TEN);
        Pageable pageable = PageRequest.of(0, 10);
        when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        when(cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId()))
                .thenReturn(Optional.of(card), Optional.empty());
        when(ledgerRepository.findByCard_CardIdOrderByCreatedAtDesc(card.getCardId(), pageable))
                .thenReturn(new PageImpl<>(List.of(ledger)));
        when(ledgerRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(new PageImpl<>(List.of(ledger)));
        when(ledgerRepository.findByTypeOrderByCreatedAtDescLedgerIdDesc(MembershipLedgerType.TOP_UP, pageable))
                .thenReturn(new PageImpl<>(List.of(ledger)));
        assertEquals(1, service.history(accountId, pageable).getTotalElements());
        assertEquals(1, service.allHistory(pageable).getTotalElements());
        assertEquals(1, service.topUpHistory(pageable).getTotalElements());
        assertThrows(ResourceNotFoundException.class, () -> service.history(accountId, pageable));
    }

    @Test
    void policyUsesConfiguredCreatesDefaultAndCanBeUpdated() {
        when(policyRepository.findFirstByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(Optional.of(policy), Optional.empty(), Optional.of(policy));
        when(policyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertSame(policy, service.getPolicy());
        MembershipPolicy fallback = service.getPolicy();
        assertEquals(new BigDecimal("1000000"), fallback.getMinimumTopUp());
        assertEquals(new BigDecimal("15"), fallback.getDiscountPercent());
        assertEquals(12, fallback.getValidityMonths());
        assertTrue(fallback.getActive());
        MembershipPolicy updated = service.updatePolicy(
                new MembershipPolicyRequest(new BigDecimal("2000000"), new BigDecimal("10"), 6));
        assertEquals(new BigDecimal("2000000"), updated.getMinimumTopUp());
        assertEquals(new BigDecimal("10"), updated.getDiscountPercent());
        assertEquals(6, updated.getValidityMonths());
    }

    private PayFixture payFixture(boolean activeBenefit) {
        MembershipCard card = card(MembershipCardStatus.ACTIVE);
        card.setBalance(new BigDecimal("1000000"));
        card.setBenefitStartsAt(activeBenefit ? LocalDateTime.now().minusDays(1) : null);
        card.setBenefitExpiresAt(activeBenefit ? LocalDateTime.now().plusDays(30) : null);
        Profile patient = Profile.builder().profileId(UUID.randomUUID()).fullName("Trần Minh Anh").build();
        Invoice invoice = invoice(UUID.randomUUID(), patient);
        MembershipPaymentRequest request = paymentRequest(invoice.getInvoiceId(), patient.getProfileId(),
                activeBenefit, null, "pay");
        lenient().when(familyAccessService.ownerProfile(accountId)).thenReturn(owner);
        lenient().when(cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId())).thenReturn(Optional.of(card));
        lenient().when(cardRepository.findByIdForUpdate(card.getCardId())).thenReturn(Optional.of(card));
        lenient().when(passwordEncoder.matches("123456", card.getPinHash())).thenReturn(true);
        lenient().when(familyAccessService.resolveActiveProfile(accountId, patient.getProfileId())).thenReturn(patient);
        lenient().when(invoiceRepository.findByIdForUpdate(invoice.getInvoiceId())).thenReturn(Optional.of(invoice));
        lenient().when(transactionRepository.save(any())).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setTransactionId(UUID.randomUUID());
            return saved;
        });
        lenient().when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return new PayFixture(card, patient, invoice, request);
    }

    private ReverseFixture reverseFixture() {
        MembershipCard card = card(MembershipCardStatus.ACTIVE);
        card.setBalance(new BigDecimal("50000"));
        Profile patient = Profile.builder().profileId(UUID.randomUUID()).fullName("Trần Minh Anh").build();
        Invoice invoice = invoice(UUID.randomUUID(), patient);
        invoice.setVisit(CustomerVisit.builder().visitId(UUID.randomUUID()).customer(patient).build());
        invoice.setDiscount(new BigDecimal("15000"));
        invoice.setTotalAmount(new BigDecimal("85000"));
        Transaction transaction = Transaction.builder().transactionId(UUID.randomUUID()).invoice(invoice)
                .amount(new BigDecimal("100000")).paymentMethod(PaymentMethod.MEMBERSHIP_CARD)
                .status(TransactionStatus.SUCCESS).build();
        MembershipCardLedger original = ledger(card, MembershipLedgerType.PAYMENT, new BigDecimal("100000"));
        original.setInvoice(invoice);
        original.setPatientProfile(patient);
        original.setPaymentTransaction(transaction);
        original.setBenefitDiscount(new BigDecimal("15000"));
        MembershipReversalRequest request = new MembershipReversalRequest(" Khách yêu cầu ", "reverse");
        lenient().when(ledgerRepository.findById(original.getLedgerId())).thenReturn(Optional.of(original));
        lenient().when(ledgerRepository.existsByReversedLedger_LedgerId(original.getLedgerId())).thenReturn(false);
        lenient().when(invoiceRepository.findByIdForUpdate(invoice.getInvoiceId())).thenReturn(Optional.of(invoice));
        return new ReverseFixture(card, invoice, transaction, original, request);
    }

    private MembershipCard card(MembershipCardStatus status) {
        return MembershipCard.builder().cardId(UUID.randomUUID()).cardCode("CS-DEMO")
                .ownerProfile(owner).status(status).balance(new BigDecimal("100000"))
                .pinHash("hash").benefitPercent(new BigDecimal("15")).build();
    }

    private MembershipCardLedger ledger(MembershipCard card, MembershipLedgerType type, BigDecimal amount) {
        return MembershipCardLedger.builder().ledgerId(UUID.randomUUID()).card(card).type(type)
                .amount(amount).balanceBefore(BigDecimal.ZERO).balanceAfter(amount)
                .sourcePaymentMethod(PaymentMethod.CASH).referenceCode("REF").build();
    }

    private Invoice invoice(UUID id, Profile patient) {
        return Invoice.builder().invoiceId(id).invoiceCode("INV-DEMO").customer(patient)
                .subtotal(new BigDecimal("100000")).discount(BigDecimal.ZERO).tax(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100000")).paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.PENDING).build();
    }

    private MembershipPaymentRequest paymentRequest(UUID invoiceId, UUID patientId, boolean benefit,
                                                       BigDecimal amount, String key) {
        return new MembershipPaymentRequest(invoiceId, patientId, "123456", benefit, amount, key);
    }

    private static final class PayFixture {
        final MembershipCard card;
        final Profile patient;
        final Invoice invoice;
        MembershipPaymentRequest request;
        PayFixture(MembershipCard card, Profile patient, Invoice invoice, MembershipPaymentRequest request) {
            this.card = card;
            this.patient = patient;
            this.invoice = invoice;
            this.request = request;
        }
    }

    private record ReverseFixture(MembershipCard card, Invoice invoice, Transaction transaction,
                                  MembershipCardLedger original, MembershipReversalRequest request) {}
}
