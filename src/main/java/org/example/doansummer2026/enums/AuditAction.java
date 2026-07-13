package org.example.doansummer2026.enums;

public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,
    EXPORT,
    IMPORT,
    VIEW,           // Truy cap du lieu nhay cam
    STATUS_CHANGE   // Doi trang thai (VD: Invoice PENDING -> PAID)
}