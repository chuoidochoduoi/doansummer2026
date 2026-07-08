package org.example.doansummer2026.enums;

/**
 * Trang thai yeu cau xet nghiem/CDHA.
 * - PENDING: moi tao, cho ky thuat vien thuc hien
 * - IN_PROGRESS: dang lay mau/thuc hien
 * - COMPLETED: co ket qua
 * - CANCELLED: huy (sau khi IN_PROGRESS, can ghi ly do)
 */
public enum TestRequestStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED
}