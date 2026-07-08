package org.example.doansummer2026.dto.medicalRecord;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MedicalRecordCreateRequest(
        @NotNull UUID visitId,
        @NotNull UUID doctorId,
        @Size(max = 2000) String chiefComplaint
) {}
