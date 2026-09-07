package org.example.doansummer2026.controller;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.invoice.*;
import org.example.doansummer2026.dto.payment.PayOSPaymentResponse;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {
    @Mock InvoiceService service;
    @Mock AuthService authService;
    @Mock PayOSService payOSService;
    @Mock FamilyAccessService familyAccessService;
    @InjectMocks InvoiceController controller;

    private UUID accountId;
    private UUID profileId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        profileId = UUID.randomUUID();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void staffEndpointsDelegateAllParametersAndReturnExpectedStatuses() {
        UUID invoiceId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var pageable = PageRequest.of(2, 10);
        var page = new PageResponse<InvoiceResponse>(List.of(), 2, 10, 0, 0, false, true);
        InvoiceResponse invoice = mock(InvoiceResponse.class);
        when(invoice.invoiceId()).thenReturn(invoiceId);
        when(authService.currentStaffId()).thenReturn(staffId);
        when(service.search(customerId, InvoiceStatus.PENDING, "INV", "INITIAL",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), pageable)).thenReturn(page);
        when(service.get(invoiceId)).thenReturn(invoice);
        when(service.create(any())).thenReturn(invoice);
        when(service.update(eq(invoiceId), any())).thenReturn(invoice);
        when(service.applyInsurance(eq(invoiceId), any())).thenReturn(invoice);
        when(service.issue(invoiceId)).thenReturn(invoice);
        when(service.cancel(invoiceId)).thenReturn(invoice);
        when(service.pay(invoiceId, staffId)).thenReturn(invoice);
        when(service.getReceiptPrintData(invoiceId)).thenReturn(mock(ReceiptPrintResponse.class));
        when(payOSService.createPaymentLink(invoiceId)).thenReturn(mock(PayOSPaymentResponse.class));

        assertSame(page, controller.list(customerId, InvoiceStatus.PENDING, "INV", "INITIAL",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), pageable).getBody());
        assertSame(invoice, controller.get(invoiceId).getBody());
        InvoiceCreateRequest create = new InvoiceCreateRequest(customerId, UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now(), BigDecimal.ZERO, BigDecimal.ZERO, "note", UUID.randomUUID(), List.of());
        assertEquals(201, controller.create(create).getStatusCode().value());
        ArgumentCaptor<InvoiceCreateRequest> issued = ArgumentCaptor.forClass(InvoiceCreateRequest.class);
        verify(service).create(issued.capture());
        assertEquals(staffId, issued.getValue().issuedById());
        assertEquals(create.items(), issued.getValue().items());

        InvoiceUpdateRequest update = new InvoiceUpdateRequest(LocalDate.now(), BigDecimal.ZERO,
                BigDecimal.ZERO, "updated", List.of());
        assertSame(invoice, controller.update(invoiceId, update).getBody());
        InvoiceInsuranceRequest insurance = new InvoiceInsuranceRequest(UUID.randomUUID(), "DN4012345678901");
        assertSame(invoice, controller.applyInsurance(invoiceId, insurance).getBody());
        assertSame(invoice, controller.issue(invoiceId).getBody());
        assertSame(invoice, controller.cancel(invoiceId).getBody());
        assertSame(invoice, controller.pay(invoiceId).getBody());
        assertNotNull(controller.payosMock(invoiceId).getBody());
        assertNotNull(controller.getPrintData(invoiceId).getBody());
        assertEquals(204, controller.delete(invoiceId).getStatusCode().value());
        verify(service).delete(invoiceId);
    }

    @Test
    void patientPaymentHistoryParsesEverySupportedMethodAndOptionalDates() {
        var pageable = PageRequest.of(0, 10);
        Profile profile = Profile.builder().profileId(profileId).build();
        when(authService.currentAccount()).thenReturn(Account.builder().accountId(accountId).build());
        when(familyAccessService.resolveReadableProfile(eq(accountId), any())).thenReturn(profile);
        when(service.getPaymentHistoryForPatient(eq(profileId), any(), any(), any(), eq(pageable)))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, true, true));

        String[] aliases = {"appbanking", "banking", "bank_transfer", "cash", "card",
                "membership_card", "membership", "unknown", " "};
        PaymentMethod[] expected = {PaymentMethod.BANK_TRANSFER, PaymentMethod.BANK_TRANSFER,
                PaymentMethod.BANK_TRANSFER, PaymentMethod.CASH, PaymentMethod.CARD,
                PaymentMethod.MEMBERSHIP_CARD, PaymentMethod.MEMBERSHIP_CARD, null, null};
        for (int i = 0; i < aliases.length; i++) {
            assertNotNull(controller.getPaymentHistory(profileId, "2026-09-01", "2026-09-05",
                    aliases[i], pageable).getBody());
            verify(service).getPaymentHistoryForPatient(profileId, LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 5), expected[i], pageable);
            clearInvocations(service);
        }
        assertNotNull(controller.getPaymentHistory(null, null, "", null, pageable).getBody());
        verify(service).getPaymentHistoryForPatient(profileId, null, null, null, pageable);
    }

    @Test
    void paymentHistoryReturnsEmptyWhenResolvedProfileHasNoId() {
        when(authService.currentAccount()).thenReturn(Account.builder().accountId(accountId).build());
        when(familyAccessService.resolveReadableProfile(accountId, null))
                .thenReturn(Profile.builder().profileId(null).build());
        var response = controller.getPaymentHistory(null, null, null, null, PageRequest.of(0, 10));
        assertNotNull(response.getBody());
        assertTrue(response.getBody().items().isEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void receiptDetailUsesReadableFamilyProfile() {
        UUID invoiceId = UUID.randomUUID();
        ReceiptDetailResponse receipt = mock(ReceiptDetailResponse.class);
        when(authService.currentAccount()).thenReturn(Account.builder().accountId(accountId).build());
        when(familyAccessService.resolveReadableProfile(accountId, profileId))
                .thenReturn(Profile.builder().profileId(profileId).build());
        when(service.getReceiptDetail(invoiceId, profileId)).thenReturn(receipt);
        assertSame(receipt, controller.getReceiptDetail(invoiceId, profileId).getBody());
    }
}
