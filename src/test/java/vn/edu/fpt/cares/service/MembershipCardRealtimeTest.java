package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.dto.membership.MembershipTopUpRequest;
import vn.edu.fpt.cares.enums.*;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipCardRealtimeTest {
    @Mock MembershipCardRepository cards;
    @Mock MembershipPolicyRepository policies;
    @Mock MembershipCardLedgerRepository ledger;
    @Mock InvoiceRepository invoices;
    @Mock InvoiceItemRepository invoiceItems;
    @Mock SimpMessagingTemplate messaging;
    @InjectMocks MembershipCardService service;
    private final UUID owner = UUID.randomUUID();
    private MembershipCard card;
    private final MembershipTopUpRequest request = new MembershipTopUpRequest(new BigDecimal("1000000"), PaymentMethod.CASH, "test-key");

    @BeforeEach
    void setup() {
        card = MembershipCard.builder().cardId(UUID.randomUUID()).cardCode("CS-TEST")
                .ownerProfile(Profile.builder().fullName("Test owner").account(Account.builder().accountId(owner).build()).build())
                .status(MembershipCardStatus.ACTIVE).balance(BigDecimal.ZERO).build();
        lenient().when(cards.findByCodeForUpdate("CS-TEST")).thenReturn(Optional.of(card));
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void cleanup() { TransactionSynchronizationManager.clear(); }

    private void allowTopUp() {
        when(policies.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(MembershipPolicy.builder()
                .minimumTopUp(new BigDecimal("1000000")).discountPercent(new BigDecimal("15")).validityMonths(12).build()));
        when(ledger.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void publishesOnlyAfterCommitToOwnerWithoutFinancialPayload() {
        allowTopUp();
        service.topUp("CS-TEST", request, null);
        verifyNoInteractions(messaging);
        var callbacks = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, callbacks.size());
        callbacks.forEach(TransactionSynchronization::afterCommit);
        verify(messaging).convertAndSendToUser(owner.toString(), "/queue/membership-card", "MEMBERSHIP_CARD_UPDATED");
    }

    @Test
    void rollbackDoesNotPublish() {
        allowTopUp();
        service.topUp("CS-TEST", request, null);
        TransactionSynchronizationManager.getSynchronizations().forEach(callback -> callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(messaging);
    }

    @Test
    void idempotentReplayDoesNotPublishAgain() {
        when(ledger.findByIdempotencyKey("test-key")).thenReturn(Optional.of(MembershipCardLedger.builder()
                .card(card).sourcePaymentMethod(PaymentMethod.CASH).amount(request.amount()).build()));
        service.topUp("CS-TEST", request, null);
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        verifyNoInteractions(messaging);
    }

    @Test
    void brokerFailureDoesNotFailCommittedTopUp() {
        allowTopUp();
        service.topUp("CS-TEST", request, null);
        doThrow(new IllegalStateException("offline")).when(messaging).convertAndSendToUser(anyString(), anyString(), any(Object.class));
        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit));
    }

    @Test
    void benefitIsCalculatedFromPatientPayableAfterInsuranceWithoutChangingItems() {
        Invoice invoice = Invoice.builder()
                .subtotal(new BigDecimal("100000"))
                .discount(new BigDecimal("20000"))
                .tax(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("80000"))
                .paidAmount(BigDecimal.ZERO)
                .build();
        when(invoices.save(invoice)).thenReturn(invoice);

        BigDecimal benefit = ReflectionTestUtils.invokeMethod(service, "applyBenefit", invoice, new BigDecimal("15"));

        assertEquals(new BigDecimal("12000.00"), benefit);
        assertEquals(new BigDecimal("32000.00"), invoice.getDiscount());
        assertEquals(new BigDecimal("68000.00"), invoice.getTotalAmount());
        verifyNoInteractions(invoiceItems);
    }
}
