package org.example.doansummer2026.dto.medicalRecord;

import jakarta.validation.constraints.Size;
import org.example.doansummer2026.dto.icd.ICD10SelectionCreateRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MedicalRecordUpdateRequest(
        @Size(max = 2000) String chiefComplaint,
        @Size(max = 2000) String clinicalFindings,
        @Size(max = 2000) String diagnosis,
        @Size(max = 2000) String prescriptionNote,
        @Size(max = 2000) String conclusion,
        @Size(max = 2000) String patientInstruction,
        // Vital signs - cap nhat khi luu nham/ket luuan
        String bloodPressure,
        Integer heartRate,
        BigDecimal temperature,
        BigDecimal weight,
        BigDecimal height,
        // Danh sach thuoc trong don thuoc (thay the ca danh sach cu khi cap nhat)
        List<PrescriptionItemCreateRequest> prescriptionItems,
        // Danh sach benh chuan doan ICD-10 (thay the ca danh sach cu khi cap nhat)
        List<ICD10SelectionCreateRequest> icdSelections
) {}




