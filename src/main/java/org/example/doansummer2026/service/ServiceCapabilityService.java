package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.capability.*;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.ServiceCapability;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ServiceCapabilityRepository;
import org.example.doansummer2026.repository.StaffCapabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional
public class ServiceCapabilityService {
    private final ServiceCapabilityRepository repository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final DepartmentRepository departmentRepository;
    private final StaffCapabilityRepository staffCapabilityRepository;

    @Transactional(readOnly = true)
    public List<ServiceCapabilityResponse> list() {
        return repository.findAllByOrderByNameAsc().stream().map(ServiceCapabilityResponse::from).toList();
    }

    public ServiceCapabilityResponse create(ServiceCapabilityRequest request) {
        if (repository.existsByCodeIgnoreCase(request.code())) throw new ConflictException("Mã danh mục kỹ thuật đã tồn tại");
        if (repository.existsByNameIgnoreCase(request.name())) throw new ConflictException("Tên danh mục kỹ thuật đã tồn tại");
        return ServiceCapabilityResponse.from(repository.save(ServiceCapability.builder()
                .code(request.code().trim().toUpperCase()).name(request.name().trim())
                .description(request.description()).active(request.active() == null || request.active()).build()));
    }

    public ServiceCapabilityResponse update(UUID id, ServiceCapabilityRequest request) {
        ServiceCapability value = find(id);
        value.setCode(request.code().trim().toUpperCase());
        value.setName(request.name().trim());
        value.setDescription(request.description());
        if (request.active() != null) {
            if (!request.active() && !Boolean.FALSE.equals(value.getActive())) {
                long serviceCount = medicalServiceRepository.countActiveReferencesToCapability(id);
                long departmentCount = departmentRepository.countReferencesToCapability(id);
                long staffCount = staffCapabilityRepository.countActiveReferencesToCapability(id);
                if (serviceCount > 0 || departmentCount > 0 || staffCount > 0) {
                    throw new ConflictException(
                            "Không thể ngừng danh mục kỹ thuật khi vẫn còn dịch vụ, phòng hoặc nhân sự đang sử dụng"
                    );
                }
            }
            value.setActive(request.active());
        }
        return ServiceCapabilityResponse.from(repository.save(value));
    }

    /**
     * Khong soft-delete danh muc dang duoc tham chieu. @SQLRestriction se an
     * capability da xoa, trong khi medical_service van giu khoa ngoai; luc do
     * Hibernate tao proxy nhung khong tai duoc dich va nem EntityNotFoundException.
     */
    public void delete(UUID id) {
        ServiceCapability value = find(id);
        long serviceCount = medicalServiceRepository.countActiveReferencesToCapability(id);
        long departmentCount = departmentRepository.countReferencesToCapability(id);
        long staffCount = staffCapabilityRepository.countActiveReferencesToCapability(id);

        if (serviceCount > 0 || departmentCount > 0 || staffCount > 0) {
            throw new ConflictException(
                    "Không thể xóa danh mục kỹ thuật đang được sử dụng " +
                    "(" + serviceCount + " dịch vụ, " + departmentCount +
                    " phòng, " + staffCount + " nhân sự). " +
                    "Hãy chuyển sang trạng thái ngừng hoạt động.");
        }

        repository.delete(value);
    }

    public ServiceCapability find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Danh mục kỹ thuật không tồn tại: " + id));
    }

    public ServiceCapability findActive(UUID id) {
        ServiceCapability capability = find(id);
        if (Boolean.FALSE.equals(capability.getActive())) {
            throw new ConflictException("Danh mục kỹ thuật đã ngừng hoạt động và không thể dùng cho cấu hình mới");
        }
        return capability;
    }
}
