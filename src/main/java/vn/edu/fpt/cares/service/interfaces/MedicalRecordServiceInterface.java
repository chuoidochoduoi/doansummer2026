package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.medicalrecord.MedicalRecordResponse;
import vn.edu.fpt.cares.dto.medicalrecord.MedicalRecordCreateRequest;
import vn.edu.fpt.cares.dto.medicalrecord.MedicalRecordUpdateRequest;
import vn.edu.fpt.cares.model.MedicalRecord;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/** Service interface for MedicalRecord management. */
public interface MedicalRecordServiceInterface {
    PageResponse<MedicalRecordResponse> search(UUID doctorId, vn.edu.fpt.cares.enums.MedicalRecordStatus status,
                                                LocalDateTime from, LocalDateTime to, Pageable pageable);
    MedicalRecordResponse get(UUID id);
    MedicalRecordResponse create(MedicalRecordCreateRequest req);
    MedicalRecordResponse update(UUID id, MedicalRecordUpdateRequest req);
    MedicalRecordResponse complete(UUID id);
    MedicalRecordResponse complete(UUID id, MedicalRecordUpdateRequest req);
    void delete(UUID id);
    MedicalRecord findById(UUID id);

    /** Danh gia phong kham (1-5 sao). Chi ap dung cho EXAMINATION da hoan thanh. */
    MedicalRecordResponse rate(UUID id, int ratingScore);
}



