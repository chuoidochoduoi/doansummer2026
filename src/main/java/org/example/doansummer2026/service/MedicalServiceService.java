package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.medicalService.MedicalServiceCreateRequest;
import org.example.doansummer2026.dto.medicalService.MedicalServiceResponse;
import org.example.doansummer2026.dto.medicalService.MedicalServiceUpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.ServiceCategory;
import org.example.doansummer2026.enums.ServiceType;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ServiceCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.MedicalServiceServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MedicalServiceService implements MedicalServiceServiceInterface {

    private final MedicalServiceRepository repo;
    private final ServiceCategoryRepository categoryRepo;
    private final DepartmentRepository departmentRepo;

    @Transactional(readOnly = true)
    public PageResponse<MedicalServiceResponse> search(String keyword, UUID categoryId,
                                                        ServiceType serviceType, Boolean isActive,
                                                        Pageable pageable) {
        Page<MedicalService> page = repo.search(keyword, categoryId, serviceType, isActive, pageable);
        return PageResponse.from(page, s -> MedicalServiceResponse.from(s));
    }

    /**
     * API cho khach hang/benh nhan xem dich vu dang hoat dong.
     * Chi tra ve cac dich vu co isActive = true.
     */
    @Transactional(readOnly = true)
    public PageResponse<MedicalServiceResponse> listAvailable(String keyword, UUID categoryId,
                                                               ServiceType serviceType,
                                                               Pageable pageable) {
        Page<MedicalService> page = repo.search(keyword, categoryId, serviceType, true, pageable);
        return PageResponse.from(page, s -> MedicalServiceResponse.from(s));
    }

    @Transactional(readOnly = true)
    public MedicalServiceResponse get(UUID id) {
        return MedicalServiceResponse.from(findById(id));
    }

    public MedicalServiceResponse create(MedicalServiceCreateRequest req) {
        if (repo.existsByName(req.name())) {
            throw new ConflictException("Ten dich vu da ton tai: " + req.name());
        }
        ServiceCategory category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Danh muc khong ton tai: " + req.categoryId()));
        Department dept = null;
        if (req.departmentId() != null) {
            dept = departmentRepo.findById(req.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Khoa khong ton tai: " + req.departmentId()));
        }
        MedicalService s = MedicalService.builder()
                .name(req.name())
                .description(req.description())
                .serviceType(req.serviceType())
                .durationMinutes(req.durationMinutes())
                .price(req.price() != null ? req.price() : BigDecimal.ZERO)
                .isActive(req.isActive() != null ? req.isActive() : true)
                .isPointOfCare(req.isPointOfCare() != null ? req.isPointOfCare() : false)
                .category(category)
                .department(dept)
                .build();
        return MedicalServiceResponse.from(repo.save(s));
    }

    public MedicalServiceResponse update(UUID id, MedicalServiceUpdateRequest req) {
        MedicalService s = findById(id);
        if (req.name() != null && !req.name().equals(s.getName())) {
            if (repo.existsByName(req.name())) {
                throw new ConflictException("Ten dich vu da ton tai: " + req.name());
            }
            s.setName(req.name());
        }
        if (req.description() != null) s.setDescription(req.description());
        if (req.serviceType() != null) s.setServiceType(req.serviceType());
        if (req.durationMinutes() != null) s.setDurationMinutes(req.durationMinutes());
        if (req.price() != null) s.setPrice(req.price());
        if (req.isActive() != null) s.setIsActive(req.isActive());
        if (req.isPointOfCare() != null) s.setIsPointOfCare(req.isPointOfCare());
        if (req.categoryId() != null) {
            s.setCategory(categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Danh muc khong ton tai: " + req.categoryId())));
        }
        if (req.departmentId() != null) {
            s.setDepartment(departmentRepo.findById(req.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Khoa khong ton tai: " + req.departmentId())));
        }

        return MedicalServiceResponse.from(repo.save(s));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Dich vu khong ton tai: " + id);
        }
        repo.deleteById(id);
    }

    public MedicalService findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + id));
    }
}
