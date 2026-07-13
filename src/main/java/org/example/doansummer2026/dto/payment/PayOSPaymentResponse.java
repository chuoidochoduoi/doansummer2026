package org.example.doansummer2026.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock PayOS payment response.
 * Trong thực tế sẽ gọi API PayOS để tạo payment link.
 */
public record PayOSPaymentResponse(
        UUID invoiceId,
        String invoiceCode,
        BigDecimal amount,
        String paymentLink,
        String qrCodeUrl,
        String status,
        String message
) {
    public static PayOSPaymentResponse pending(UUID invoiceId, String invoiceCode, BigDecimal amount) {
        String mockPaymentLink = "https://payos.mock/checkout?orderId=" + UUID.randomUUID() + "&amount=" + amount.longValue();
        String mockQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + mockPaymentLink;
        return new PayOSPaymentResponse(invoiceId, invoiceCode, amount, mockPaymentLink, mockQrUrl, "PENDING", "Chờ thanh toán");
    }

    public static PayOSPaymentResponse paid(UUID invoiceId, String invoiceCode, BigDecimal amount) {
        return new PayOSPaymentResponse(invoiceId, invoiceCode, amount, null, null, "PAID", "Đã thanh toán");
    }
}