package org.example.doansummer2026.enums;

public enum InvoiceStatus {
    DRAFT,           // Moi tao, chua xuat
    ISSUED,          // Da xuat cho benh nhan
    PARTIALLY_PAID,  // Thanh toan mot phan
    PAID,            // Da thanh toan du
    CANCELLED,       // Huy (do sai thong tin, benh nhan khong den...)
    REFUNDED         // Hoan tien (da thanh toan nhung huy sau)
}