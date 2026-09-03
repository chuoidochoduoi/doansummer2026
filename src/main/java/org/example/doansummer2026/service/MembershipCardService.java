package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.example.doansummer2026.dto.invoice.InvoiceResponse;
import org.example.doansummer2026.dto.membership.*;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.*;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipCardService {
    private static final BigDecimal DEFAULT_MINIMUM = new BigDecimal("1000000");
    private static final BigDecimal DEFAULT_DISCOUNT = new BigDecimal("15");
    private static final int DEFAULT_MONTHS = 12;

    private final MembershipPolicyRepository policyRepository;
    private final MembershipCardRepository cardRepository;
    private final MembershipCardLedgerRepository ledgerRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository itemRepository;
    private final TransactionRepository transactionRepository;
    private final StaffInfoRepository staffRepository;
    private final QueueTicketRepository queueTicketRepository;
    private final TestRequestRepository testRequestRepository;
    private final FamilyAccessService familyAccessService;
    private final InvoiceService invoiceService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MembershipCardResponse register(UUID accountId, MembershipCardRequest request) {
        if (!Boolean.TRUE.equals(request.acceptedTerms())) {
            throw new BadRequestException("Bạn cần xác nhận điều khoản sử dụng thẻ trả trước");
        }
        Profile owner = familyAccessService.ownerProfile(accountId);
        if (cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId()).isPresent()) {
            throw new ConflictException("Tài khoản đã đăng ký thẻ trả trước CareS");
        }
        MembershipPolicy policy = policy();
        MembershipCard card = MembershipCard.builder()
                .cardCode("CS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase())
                .ownerProfile(owner).status(MembershipCardStatus.PENDING).balance(BigDecimal.ZERO)
                .pinHash(passwordEncoder.encode(request.pin()))
                .benefitPercent(policy.getDiscountPercent()).build();
        MembershipCard saved = cardRepository.save(card);
        publishCardUpdateAfterCommit(saved);
        return MembershipCardResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public MembershipCardResponse myCard(UUID accountId) {
        Profile owner = familyAccessService.ownerProfile(accountId);
        return cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId())
                .map(MembershipCardResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa đăng ký thẻ trả trước CareS"));
    }

    @Transactional
    public MembershipTopUpResponse topUp(String cardCode, MembershipTopUpRequest request, UUID staffId) {
        MembershipCard card = cardRepository.findByCodeForUpdate(cardCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ trả trước"));
        MembershipCardLedger duplicated = ledgerRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (duplicated != null) {
            if (!duplicated.getCard().getCardId().equals(card.getCardId())) {
                throw new ConflictException("Mã chống gửi lặp đã được sử dụng cho giao dịch khác");
            }
            return MembershipTopUpResponse.from(card, duplicated);
        }
        if (card.getStatus() == MembershipCardStatus.CLOSED || card.getStatus() == MembershipCardStatus.SUSPENDED) {
            throw new ConflictException("Thẻ hiện không thể nạp tiền");
        }
        if (request.paymentMethod() == PaymentMethod.MEMBERSHIP_CARD || request.paymentMethod() == PaymentMethod.INSURANCE) {
            throw new BadRequestException("Phương thức nạp tiền không hợp lệ");
        }
        MembershipPolicy policy = policy();
        if (card.getStatus() == MembershipCardStatus.PENDING
                && request.amount().compareTo(policy.getMinimumTopUp()) < 0) {
            throw new BadRequestException("Lần nạp đầu tối thiểu " + policy.getMinimumTopUp().toPlainString() + " đồng");
        }
        BigDecimal before = card.getBalance();
        card.setBalance(before.add(request.amount()));
        if (card.getStatus() == MembershipCardStatus.PENDING) {
            card.setStatus(MembershipCardStatus.ACTIVE);
            card.setActivatedAt(LocalDateTime.now());
            card.setBenefitPercent(policy.getDiscountPercent());
        }
        if (request.amount().compareTo(policy.getMinimumTopUp()) >= 0) {
            LocalDateTime base = card.getBenefitExpiresAt() != null && card.getBenefitExpiresAt().isAfter(LocalDateTime.now())
                    ? card.getBenefitExpiresAt() : LocalDateTime.now();
            card.setBenefitExpiresAt(base.plusMonths(policy.getValidityMonths()));
            card.setBenefitPercent(policy.getDiscountPercent());
        }
        cardRepository.save(card);
        StaffInfo staff = staffId == null ? null : staffRepository.findById(staffId).orElse(null);
        MembershipCardLedger ledger = ledgerRepository.save(MembershipCardLedger.builder().card(card).type(MembershipLedgerType.TOP_UP)
                .amount(request.amount()).balanceBefore(before).balanceAfter(card.getBalance())
                .performedBy(staff).sourcePaymentMethod(request.paymentMethod())
                .idempotencyKey(request.idempotencyKey()).referenceCode(reference("NT")).build());
        publishCardUpdateAfterCommit(card);
        return MembershipTopUpResponse.from(card, ledger);
    }

    @Transactional
    public InvoiceResponse pay(UUID accountId, MembershipPaymentRequest request) {
        Profile owner = familyAccessService.ownerProfile(accountId);
        MembershipCard existing = cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa đăng ký thẻ trả trước CareS"));
        MembershipCard card = cardRepository.findByIdForUpdate(existing.getCardId()).orElseThrow();
        MembershipCardLedger duplicated = ledgerRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (duplicated != null) {
            if (duplicated.getInvoice() == null || !duplicated.getInvoice().getInvoiceId().equals(request.invoiceId())) {
                throw new ConflictException("Mã chống gửi lặp đã được sử dụng cho giao dịch khác");
            }
            return invoiceService.get(request.invoiceId());
        }
        validatePin(card, request.pin());
        if (card.getStatus() != MembershipCardStatus.ACTIVE) throw new ConflictException("Thẻ chưa hoạt động");

        Profile patient = familyAccessService.resolveActiveProfile(accountId, request.patientProfileId());
        Invoice invoice = invoiceRepository.findByIdForUpdate(request.invoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn không tồn tại"));
        if (!invoice.getCustomer().getProfileId().equals(patient.getProfileId())) {
            throw new BadRequestException("Hóa đơn không thuộc người được khám đã chọn");
        }
        if (invoice.getStatus() != InvoiceStatus.PENDING) throw new ConflictException("Chỉ thanh toán hóa đơn đang chờ");

        boolean benefit = Boolean.TRUE.equals(request.useBenefit())
                && card.getBenefitExpiresAt() != null && card.getBenefitExpiresAt().isAfter(LocalDateTime.now());
        if (benefit && transactionRepository.findByInvoice_InvoiceId(invoice.getInvoiceId()).stream()
                .anyMatch(t -> t.getStatus() == TransactionStatus.SUCCESS)) {
            throw new ConflictException("Ưu đãi thẻ chỉ áp dụng khi thẻ thanh toán toàn bộ hóa đơn");
        }
        BigDecimal benefitDiscount = benefit ? applyBenefit(invoice, card.getBenefitPercent()) : BigDecimal.ZERO;
        BigDecimal remaining = invoice.getTotalAmount().subtract(invoice.getPaidAmount()).max(BigDecimal.ZERO);
        BigDecimal amount = benefit ? remaining : (request.amount() == null ? remaining : request.amount().min(remaining));
        if (amount.signum() <= 0) throw new ConflictException("Hóa đơn không còn số tiền cần thanh toán");
        if (card.getBalance().compareTo(amount) < 0) throw new BadRequestException("Số dư thẻ không đủ");

        BigDecimal before = card.getBalance();
        card.setBalance(before.subtract(amount));
        cardRepository.save(card);
        Transaction tx = transactionRepository.save(Transaction.builder().invoice(invoice)
                .transactionCode(reference("THE")).amount(amount).paymentMethod(PaymentMethod.MEMBERSHIP_CARD)
                .status(TransactionStatus.SUCCESS).paidAt(LocalDateTime.now())
                .note(benefit ? "Thanh toán thẻ CareS có áp dụng ưu đãi" : "Thanh toán bằng số dư thẻ CareS").build());
        ledgerRepository.save(MembershipCardLedger.builder().card(card).type(MembershipLedgerType.PAYMENT)
                .amount(amount).balanceBefore(before).balanceAfter(card.getBalance()).invoice(invoice)
                .patientProfile(patient).paymentTransaction(tx).sourcePaymentMethod(PaymentMethod.MEMBERSHIP_CARD)
                .benefitDiscount(benefitDiscount)
                .idempotencyKey(request.idempotencyKey()).referenceCode(reference("TT")).build());
        invoiceService.recalculatePaidAmount(invoice.getInvoiceId());
        publishCardUpdateAfterCommit(card);
        return invoiceService.get(invoice.getInvoiceId());
    }

    @Transactional
    public InvoiceResponse payAtCounter(MembershipCounterPaymentRequest request) {
        MembershipCard card = cardRepository.findByCardCode(request.cardCode().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ trả trước"));
        if (card.getOwnerProfile().getAccount() == null) {
            throw new BadRequestException("Thẻ không có tài khoản chủ sở hữu hợp lệ");
        }
        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn không tồn tại"));
        return pay(card.getOwnerProfile().getAccount().getAccountId(), new MembershipPaymentRequest(
                request.invoiceId(), invoice.getCustomer().getProfileId(), request.pin(), request.useBenefit(),
                request.amount(), request.idempotencyKey()));
    }

    @Transactional
    public MembershipLedgerResponse reverse(UUID ledgerId, MembershipReversalRequest request, UUID staffId) {
        MembershipCardLedger original = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thẻ"));
        if (original.getType() != MembershipLedgerType.PAYMENT || original.getPaymentTransaction() == null) {
            throw new BadRequestException("Chỉ có thể hoàn tác giao dịch thanh toán bằng thẻ");
        }
        if (ledgerRepository.existsByReversedLedger_LedgerId(ledgerId)) {
            throw new ConflictException("Giao dịch đã được hoàn tác trước đó");
        }
        MembershipCardLedger duplicated = ledgerRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (duplicated != null) return MembershipLedgerResponse.from(duplicated);
        Invoice invoice = invoiceRepository.findByIdForUpdate(original.getInvoice().getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn không tồn tại"));
        if (invoice.getVisit() != null) {
            var serviceIds = itemRepository.findAllWithServiceByInvoiceId(invoice.getInvoiceId()).stream()
                    .filter(item -> item.getService() != null).map(item -> item.getService().getServiceId()).collect(java.util.stream.Collectors.toSet());
            var queues = queueTicketRepository.findAllByVisit_VisitId(invoice.getVisit().getVisitId()).stream()
                    .filter(q -> q.getService() != null && serviceIds.contains(q.getService().getServiceId())).toList();
            boolean started = queues.stream().anyMatch(q -> q.getStatus() == QueueStatus.IN_PROGRESS
                    || q.getStatus() == QueueStatus.DONE || q.getStatus() == QueueStatus.TEST_DONE
                    || q.getStatus() == QueueStatus.WAITING_FOR_TEST);
            if (started) throw new ConflictException("Không thể hoàn tác vì dịch vụ đã bắt đầu thực hiện");
            queues.stream().filter(q -> q.getStatus() == QueueStatus.WAITING || q.getStatus() == QueueStatus.BLOCKED
                    || q.getStatus() == QueueStatus.CALLED).forEach(q -> { q.setStatus(QueueStatus.SKIPPED); queueTicketRepository.save(q); });
        }
        var tests = testRequestRepository.findByInvoiceId(invoice.getInvoiceId());
        if (tests.stream().anyMatch(t -> t.getStatus() == TestRequestStatus.IN_PROGRESS
                || t.getStatus() == TestRequestStatus.COMPLETED)) {
            throw new ConflictException("Không thể hoàn tác vì dịch vụ cận lâm sàng đã bắt đầu thực hiện");
        }
        tests.forEach(t -> { t.setStatus(TestRequestStatus.CANCELLED); testRequestRepository.save(t); });
        MembershipCard card = cardRepository.findByIdForUpdate(original.getCard().getCardId()).orElseThrow();
        BigDecimal before = card.getBalance();
        card.setBalance(before.add(original.getAmount()));
        cardRepository.save(card);
        Transaction tx = transactionRepository.findByIdForUpdate(original.getPaymentTransaction().getTransactionId()).orElseThrow();
        tx.setStatus(TransactionStatus.CANCELLED);
        tx.setNote("Hoàn tác thanh toán thẻ: " + request.reason().trim());
        transactionRepository.save(tx);
        restoreBenefit(invoice, original.getBenefitDiscount());
        invoiceService.recalculatePaidAmount(invoice.getInvoiceId());
        MembershipCardLedger reversal = ledgerRepository.save(MembershipCardLedger.builder().card(card)
                .type(MembershipLedgerType.REVERSAL).amount(original.getAmount()).balanceBefore(before)
                .balanceAfter(card.getBalance()).invoice(invoice).patientProfile(original.getPatientProfile())
                .performedBy(staffId == null ? null : staffRepository.findById(staffId).orElse(null)).sourcePaymentMethod(PaymentMethod.MEMBERSHIP_CARD)
                .idempotencyKey(request.idempotencyKey()).referenceCode(reference("HT"))
                .reversedLedger(original).reason(request.reason().trim()).build());
        publishCardUpdateAfterCommit(card);
        return MembershipLedgerResponse.from(reversal);
    }

    private void publishCardUpdateAfterCommit(MembershipCard card) {
        var ownerAccount = card.getOwnerProfile().getAccount();
        if (ownerAccount == null) return;
        String recipient = ownerAccount.getAccountId().toString();
        // Capture identity while the transaction is open; do not access lazy entities after commit.
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    messagingTemplate.convertAndSendToUser(recipient, "/queue/membership-card", "MEMBERSHIP_CARD_UPDATED");
                } catch (RuntimeException ex) {
                    // A notification failure must not turn a committed payment into an API failure.
                    log.warn("Không thể gửi tín hiệu đồng bộ thẻ; khách hàng có thể tải lại dữ liệu");
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public Page<MembershipLedgerResponse> history(UUID accountId, Pageable pageable) {
        Profile owner = familyAccessService.ownerProfile(accountId);
        MembershipCard card = cardRepository.findByOwnerProfile_ProfileId(owner.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa đăng ký thẻ trả trước CareS"));
        return ledgerRepository.findByCard_CardIdOrderByCreatedAtDesc(card.getCardId(), pageable)
                .map(MembershipLedgerResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<MembershipLedgerResponse> allHistory(Pageable pageable) {
        return ledgerRepository.findAllByOrderByCreatedAtDesc(pageable).map(MembershipLedgerResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<MembershipLedgerResponse> topUpHistory(Pageable pageable) {
        return ledgerRepository.findByTypeOrderByCreatedAtDescLedgerIdDesc(MembershipLedgerType.TOP_UP, pageable)
                .map(MembershipLedgerResponse::from);
    }

    @Transactional
    public MembershipPolicy updatePolicy(MembershipPolicyRequest request) {
        MembershipPolicy p = policy();
        p.setMinimumTopUp(request.minimumTopUp());
        p.setDiscountPercent(request.discountPercent());
        p.setValidityMonths(request.validityMonths());
        return policyRepository.save(p);
    }

    @Transactional
    public MembershipPolicy getPolicy() { return policy(); }

    private MembershipPolicy policy() {
        return policyRepository.findFirstByActiveTrueOrderByCreatedAtDesc().orElseGet(() ->
                policyRepository.save(MembershipPolicy.builder().minimumTopUp(DEFAULT_MINIMUM)
                        .discountPercent(DEFAULT_DISCOUNT).validityMonths(DEFAULT_MONTHS).active(true).build()));
    }

    private BigDecimal applyBenefit(Invoice invoice, BigDecimal percent) {
        BigDecimal patientPayable = invoice.getTotalAmount().subtract(invoice.getPaidAmount()).max(BigDecimal.ZERO);
        BigDecimal added = patientPayable.multiply(percent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        invoice.setDiscount(invoice.getDiscount().add(added));
        invoice.setTotalAmount(invoice.getSubtotal().subtract(invoice.getDiscount()).add(invoice.getTax()));
        invoiceRepository.save(invoice);
        return added;
    }

    private void restoreBenefit(Invoice invoice, BigDecimal discount) {
        if (discount == null || discount.signum() <= 0) return;
        invoice.setDiscount(invoice.getDiscount().subtract(discount).max(BigDecimal.ZERO));
        invoice.setTotalAmount(invoice.getSubtotal().subtract(invoice.getDiscount()).add(invoice.getTax()));
        invoiceRepository.save(invoice);
    }

    private void validatePin(MembershipCard card, String pin) {
        String key = "membership:pin-fail:" + card.getCardId();
        String raw = redisTemplate.opsForValue().get(key);
        int failures = raw == null ? 0 : Integer.parseInt(raw);
        if (failures >= 5) throw new BadRequestException("Thẻ tạm khóa xác thực PIN trong 15 phút");
        if (!passwordEncoder.matches(pin, card.getPinHash())) {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) redisTemplate.expire(key, 15, TimeUnit.MINUTES);
            throw new BadRequestException("Mã PIN không đúng");
        }
        redisTemplate.delete(key);
    }

    private String reference(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
