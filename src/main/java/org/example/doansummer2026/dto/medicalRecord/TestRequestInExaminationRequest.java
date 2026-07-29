package org.example.doansummer2026.dto.medicalRecord;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Du lieu yeu cau xet nghiem trong payload khi hoan thien kham.
 * Duoc tao thanh TestRequest khi API complete duoc goi.
 * - serviceId: dich vu xet nghiem (LAB_TEST, IMAGING, PROCEDURE).
 * - notes: ghi chu cho yeu cau xet nghiem.
 */
public record TestRequestInExaminationRequest(
        @NotNull UUID serviceId,
        String notes
) {}
