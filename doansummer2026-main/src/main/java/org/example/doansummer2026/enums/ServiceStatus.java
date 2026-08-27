package org.example.doansummer2026.enums;

/**
 * Trang thai dich vu y te.
 * - DRAFT: ban thao (chua duoc phe duyet)
 * - ACTIVE: dang van hanh (duoc phe duyet, co the su dung)
 * - INACTIVE: ngung hoat dong (khong cho su dung nhung khong xoa)
 */
public enum ServiceStatus {
    DRAFT,      // bản nháp
    ACTIVE,     // đang vận hành
    INACTIVE    // ngưng hoạt động
}