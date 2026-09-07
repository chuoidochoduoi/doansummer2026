package org.example.doansummer2026.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.doansummer2026.dto.payment.PayOSPaymentResponse;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;
import vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService;
import vn.payos.service.blocking.webhooks.WebhooksService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayOSServiceTest {
    @Mock PayOS payOS;
    @Mock InvoiceRepository invoiceRepository;
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> values;
    @Mock PaymentRequestsService paymentRequests;
    @Mock WebhooksService webhooks;
    private PayOSService service;
    private UUID invoiceId;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        service = new PayOSService(payOS, invoiceRepository, redisTemplate);
        ReflectionTestUtils.setField(service, "allowedOrigins", new String[]{"https://clinic.example,https://admin.example"});
        invoiceId = UUID.randomUUID();
        invoice = Invoice.builder().invoiceId(invoiceId).invoiceCode("INV-001")
                .totalAmount(new BigDecimal("150000")).status(InvoiceStatus.PENDING).build();
        lenient().when(redisTemplate.opsForValue()).thenReturn(values);
    }

    @Test
    void missingInvoiceIsRejected() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.createPaymentLink(invoiceId));
        verifyNoInteractions(payOS);
    }

    @Test
    void paidInvoiceReturnsPaidWithoutRedisOrProvider() {
        invoice.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        PayOSPaymentResponse result = service.createPaymentLink(invoiceId);
        assertEquals("PAID", result.status());
        assertNull(result.paymentLink());
        verify(redisTemplate, never()).opsForValue();
        verifyNoInteractions(payOS);
    }

    @Test
    void validCachedLinkIsReturnedWithoutCreatingNewLink() throws Exception {
        PayOSPaymentResponse cached = new PayOSPaymentResponse(invoiceId, "INV-001", new BigDecimal("150000"),
                "https://cached", "qr", "PENDING", "Chờ thanh toán");
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(values.get("invoice_payos_link:" + invoiceId)).thenReturn(new ObjectMapper().writeValueAsString(cached));
        PayOSPaymentResponse result = service.createPaymentLink(invoiceId);
        assertEquals("https://cached", result.paymentLink());
        verifyNoInteractions(payOS);
    }

    @Test
    void invalidCacheCreatesAndCachesFreshPaymentLink() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(values.get("invoice_payos_link:" + invoiceId)).thenReturn("not-json");
        when(payOS.paymentRequests()).thenReturn(paymentRequests);
        CreatePaymentLinkResponse providerResponse = mock(CreatePaymentLinkResponse.class);
        when(providerResponse.getCheckoutUrl()).thenReturn("https://payos/checkout");
        when(providerResponse.getQrCode()).thenReturn("qr-data");
        when(paymentRequests.create(any(CreatePaymentLinkRequest.class))).thenReturn(providerResponse);

        PayOSPaymentResponse result = service.createPaymentLink(invoiceId);

        assertEquals("https://payos/checkout", result.paymentLink());
        assertEquals("qr-data", result.qrCodeUrl());
        ArgumentCaptor<CreatePaymentLinkRequest> request = ArgumentCaptor.forClass(CreatePaymentLinkRequest.class);
        verify(paymentRequests).create(request.capture());
        assertEquals(150000L, request.getValue().getAmount());
        verify(values).set(startsWith("payos_order:"), eq(invoiceId.toString()), eq(Duration.ofMinutes(5)));
        verify(values).set(eq("invoice_payos_link:" + invoiceId), contains("payos/checkout"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void providerFailureBecomesBadRequest() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(values.get(anyString())).thenReturn(null);
        when(payOS.paymentRequests()).thenThrow(new IllegalStateException("provider offline"));
        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.createPaymentLink(invoiceId));
        assertTrue(error.getMessage().contains("provider offline"));
    }

    @Test
    void webhookIsVerifiedAndProviderFailureIsRejected() {
        ObjectNode body = new ObjectMapper().createObjectNode();
        WebhookData verified = mock(WebhookData.class);
        when(payOS.webhooks()).thenReturn(webhooks);
        when(webhooks.verify(any())).thenReturn(verified);
        assertSame(verified, service.verifyWebhook(body));

        when(webhooks.verify(any())).thenThrow(new IllegalArgumentException("bad signature"));
        BadRequestException error = assertThrows(BadRequestException.class, () -> service.verifyWebhook(body));
        assertTrue(error.getMessage().contains("bad signature"));
    }

    @Test
    void orderCodeLookupHandlesPresentAbsentAndMalformedValues() {
        UUID expected = UUID.randomUUID();
        when(values.get("payos_order:10")).thenReturn(expected.toString());
        when(values.get("payos_order:11")).thenReturn(null);
        when(values.get("payos_order:12")).thenReturn("bad-id");
        assertEquals(expected, service.getInvoiceIdByOrderCode(10));
        assertNull(service.getInvoiceIdByOrderCode(11));
        assertThrows(IllegalArgumentException.class, () -> service.getInvoiceIdByOrderCode(12));
    }
}
