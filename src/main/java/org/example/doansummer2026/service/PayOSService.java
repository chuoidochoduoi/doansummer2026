package org.example.doansummer2026.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.doansummer2026.dto.payment.PayOSPaymentResponse;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSService {

    private final PayOS payOS;
    private final InvoiceRepository invoiceRepository;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    public PayOSPaymentResponse createPaymentLink(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn không tồn tại"));

        if (invoice.getStatus() == org.example.doansummer2026.enums.InvoiceStatus.PAID) {
            return PayOSPaymentResponse.paid(invoice.getInvoiceId(), invoice.getInvoiceCode(), invoice.getTotalAmount());
        }

        // Check if an active payment link already exists
        String cacheKey = "invoice_payos_link:" + invoiceId;
        String cachedLink = redisTemplate.opsForValue().get(cacheKey);
        if (cachedLink != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(cachedLink, PayOSPaymentResponse.class);
            } catch (Exception e) {
                log.warn("Failed to parse cached payment link", e);
            }
        }

        // Generate a random orderCode up to 9007199254740991 (Safe Integer)
        long orderCode = System.currentTimeMillis() % 10000000000L * 100 + ThreadLocalRandom.current().nextInt(100);
        
        // Save orderCode to Redis for webhook lookup
        redisTemplate.opsForValue().set("payos_order:" + orderCode, invoiceId.toString(), Duration.ofMinutes(5));

        String returnUrl = allowedOrigins[0].split(",")[0] + "/payment-success";
        String cancelUrl = allowedOrigins[0].split(",")[0] + "/payment-cancel";

        PaymentLinkItem item = PaymentLinkItem.builder()
                .name("Thanh toan hoa don " + invoice.getInvoiceCode())
                .quantity(1)
                .price(invoice.getTotalAmount().longValue())
                .build();

        // Expire in 5 minutes
        long expiredAt = System.currentTimeMillis() / 1000L + 5 * 60;

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(invoice.getTotalAmount().longValue())
                .description("HD " + invoice.getInvoiceCode())
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .expiredAt(expiredAt)
                .items(Collections.singletonList(item))
                .build();

        try {
            CreatePaymentLinkResponse data = payOS.paymentRequests().create(paymentData);
            PayOSPaymentResponse response = new PayOSPaymentResponse(
                    invoice.getInvoiceId(),
                    invoice.getInvoiceCode(),
                    invoice.getTotalAmount(),
                    data.getCheckoutUrl(),
                    data.getQrCode(),
                    "PENDING",
                    "Chờ thanh toán"
            );
            
            // Cache the generated link for 5 minutes
            ObjectMapper mapper = new ObjectMapper();
            redisTemplate.opsForValue().set(cacheKey, mapper.writeValueAsString(response), Duration.ofMinutes(5));
            
            return response;
        } catch (Exception e) {
            log.error("PayOS Error: ", e);
            throw new BadRequestException("Không thể tạo link thanh toán PayOS: " + e.getMessage());
        }
    }
    
    public WebhookData verifyWebhook(ObjectNode webhookBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Webhook webhook = mapper.treeToValue(webhookBody, Webhook.class);
            return payOS.webhooks().verify(webhook);
        } catch (Exception e) {
            throw new BadRequestException("Webhook không hợp lệ: " + e.getMessage());
        }
    }

    public UUID getInvoiceIdByOrderCode(long orderCode) {
        String invoiceIdStr = redisTemplate.opsForValue().get("payos_order:" + orderCode);
        if (invoiceIdStr != null) {
            return UUID.fromString(invoiceIdStr);
        }
        return null;
    }
}
