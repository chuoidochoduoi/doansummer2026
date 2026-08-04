package org.example.doansummer2026.enums;

/**
 * Vai tro nghiep vu (business role) cua nhan vien - phan quyen chi tiet hon Role.
 * - DOCTOR: bac si; pham vi kham duoc xac dinh boi specialization
 * - NURSE: y ta
 * - RECEPTIONIST: le tan
 * - CASHIER: thu ngan
 * - CLINIC_MANAGER: quan ly phong kham
 * - ADMIN: quan tri vien he thong
 */
public enum SystemRole {
    DOCTOR,
    /** @deprecated Chi giu de doc du lieu cu; du lieu moi phai dung DOCTOR. */
    @Deprecated
    GENERAL_DOCTOR,
    /** @deprecated Chi giu de doc du lieu cu; du lieu moi phai dung DOCTOR. */
    @Deprecated
    SPECIALIST_DOCTOR,
    NURSE,
    RECEPTIONIST,
    CASHIER,
    CLINIC_MANAGER,
    ADMIN;

    public boolean isDoctor() {
        return this == DOCTOR || this == GENERAL_DOCTOR || this == SPECIALIST_DOCTOR;
    }

    public SystemRole normalized() {
        return isDoctor() ? DOCTOR : this;
    }
}


