package org.example.doansummer2026.enums;

/**
 * Nhom mau - luu duoi dang chuan (A+, AB-, ...).
 */
public enum BloodType {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String display;

    BloodType(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}