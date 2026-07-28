package org.example.doansummer2026.enums;

public enum PaymentMethod {
    CASH,            // Tien mat
    CARD,            // The tin dung/ghi no
    BANK_TRANSFER,   // Chuyen khoan
    MOMO,            // Vi MoMo
    VNPAY,           // Cong thanh toan VNPay
    ZALOPAY,         // Vi ZaloPay
    INSURANCE,       // Bao hiem (chi mot phan)
    OTHER;           // Khac

    public String getDisplayName() {
        return switch (this) {
            case CASH -> "Cash";
            case CARD -> "Card";
            case BANK_TRANSFER -> "Bank Transfer";
            case MOMO -> "MoMo";
            case VNPAY -> "VNPay";
            case ZALOPAY -> "ZaloPay";
            case INSURANCE -> "Insurance";
            default -> "Other";
        };
    }
}



