package org.example.doansummer2026.enums;

/**
 * Loai phong ban/khoa phong:
 * - EXAMINATION: Kham benh (bac si kham)
 * - LABORATORY: Xet nghiem (phong xet nghiem)
 * - IMAGING: Chan doan hinh anh (XQ, SA, MRI)
 */
public enum DepartmentType {
    EXAMINATION,
    PARACLINICAL,
    @Deprecated LABORATORY,
    @Deprecated IMAGING;

    public boolean isParaclinical() {
        return this == PARACLINICAL || this == LABORATORY || this == IMAGING;
    }

    public DepartmentType normalized() {
        return isParaclinical() ? PARACLINICAL : this;
    }
}


