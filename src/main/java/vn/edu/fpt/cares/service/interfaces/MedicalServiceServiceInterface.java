package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.medicalservice.MedicalServiceResponse;
import vn.edu.fpt.cares.dto.medicalservice.MedicalServiceCreateRequest;
import vn.edu.fpt.cares.dto.medicalservice.MedicalServiceUpdateRequest;
import vn.edu.fpt.cares.model.MedicalService;
import vn.edu.fpt.cares.enums.ServiceStatus;
import vn.edu.fpt.cares.enums.DepartmentType;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

/** Service interface for MedicalService management. */
public interface MedicalServiceServiceInterface {
    PageResponse<MedicalServiceResponse> search(String keyword, DepartmentType departmentType,
                                                 ServiceStatus status, UUID specializationId, Pageable pageable);
    PageResponse<MedicalServiceResponse> search(String keyword, DepartmentType departmentType,
                                                 ServiceStatus status, UUID specializationId,
                                                 boolean primaryOnly, Pageable pageable);

    /** API cho khach hang/benh nhan xem dich vu dang hoat dong. */
    PageResponse<MedicalServiceResponse> listAvailable(String keyword, DepartmentType departmentType,
                                                       Pageable pageable);

    MedicalServiceResponse get(UUID id);
    Map<String, Long> getStats();
    Map<String, Long> getStats(boolean primaryOnly);
    MedicalServiceResponse create(MedicalServiceCreateRequest req);
    MedicalServiceResponse update(UUID id, MedicalServiceUpdateRequest req);
    void delete(UUID id);
    MedicalServiceResponse deactivate(UUID id);
    MedicalServiceResponse publish(UUID id);
    MedicalService findById(UUID id);
}

