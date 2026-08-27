package org.example.doansummer2026.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.service.InvoiceService;
import org.example.doansummer2026.service.PayOSService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.model.webhooks.WebhookData;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payos")
@RequiredArgsConstructor
public class PayOSWebhookController {

    private final PayOSService payOSService;
    private final InvoiceService invoiceService;

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody String webhookBodyStr) {
        log.info("Received PayOS webhook string: {}", webhookBodyStr);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ObjectNode webhookBody = (ObjectNode) mapper.readTree(webhookBodyStr);
            
            WebhookData data = payOSService.verifyWebhook(webhookBody);
            log.info("Webhook verified, orderCode: {}, amount: {}", data.getOrderCode(), data.getAmount());
            
            UUID invoiceId = payOSService.getInvoiceIdByOrderCode(data.getOrderCode());
            if (invoiceId != null) {
                // Call pay method (null for receivedById since it's online payment)
                invoiceService.pay(invoiceId, null);
                log.info("Invoice {} successfully marked as PAID via PayOS", invoiceId);
            } else {
                log.warn("Cannot find invoiceId for PayOS orderCode: {}", data.getOrderCode());
            }

            return RestResponses.ok(Map.of("success", true, "message", "Webhook received"));
        } catch (Exception e) {
            log.error("Failed to process PayOS webhook: ", e);
            return RestResponses.ok(Map.of("success", false, "message", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
}
