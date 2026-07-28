package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.medicalService.MedicalServiceResponse;
import org.example.doansummer2026.dto.medicalService.MedicalServiceCreateRequest;
import org.example.doansummer2026.dto.medicalService.MedicalServiceUpdateRequest;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.enums.ServiceStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Service interface for MedicalService management. */
public interface MedicalServiceServiceInterface {
    PageResponse<MedicalServiceResponse> search(String keyword, UUID categoryId,
                                                 org.example.doansummer2026.enums.ServiceType serviceType,
                                                 ServiceStatus status, Pageable pageable);

    /** API cho khach hang/benh nhan xem dich vu dang hoat dong. */
    PageResponse<MedicalServiceResponse> listAvailable(String keyword, UUID categoryId,
                                                       org.example.doansummer2026.enums.ServiceType serviceType,
                                                       Pageable pageable);

    MedicalServiceResponse get(UUID id);
    MedicalServiceResponse create(MedicalServiceCreateRequest req);
    MedicalServiceResponse update(UUID id, MedicalServiceUpdateRequest req);
    void delete(UUID id);
    MedicalServiceResponse deactivate(UUID id);
    MedicalServiceResponse publish(UUID id);
    MedicalService findById(UUID id);
}



