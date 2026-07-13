package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.model.MedicalRecord;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/** Service interface for MedicalRecord management. */
public interface MedicalRecordServiceInterface {
    PageResponse<MedicalRecordResponse> search(UUID doctorId, org.example.doansummer2026.enums.MedicalRecordStatus status,
                                                LocalDateTime from, LocalDateTime to, Pageable pageable);
    MedicalRecordResponse get(UUID id);
    MedicalRecordResponse create(MedicalRecordCreateRequest req);
    MedicalRecordResponse update(UUID id, MedicalRecordUpdateRequest req);
    MedicalRecordResponse complete(UUID id);
    MedicalRecordResponse complete(UUID id, MedicalRecordUpdateRequest req);
    void delete(UUID id);
    MedicalRecord findById(UUID id);
}