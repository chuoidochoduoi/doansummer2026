package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.medicalService.MedicalServiceCreateRequest;
import org.example.doansummer2026.dto.medicalService.MedicalServiceResponse;
import org.example.doansummer2026.dto.medicalService.MedicalServiceUpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.ServiceCategory;
import org.example.doansummer2026.model.Specialization;
import org.example.doansummer2026.enums.ServiceStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ServiceCategoryRepository;
import org.example.doansummer2026.repository.SpecializationRepository;
import org.example.doansummer2026.repository.ServiceCapabilityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.MedicalServiceServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@Service
@Transactional
@RequiredArgsConstructor
public class MedicalServiceService implements MedicalServiceServiceInterface {

    private final MedicalServiceRepository repo;
    private final ServiceCategoryRepository categoryRepo;
    private final SpecializationRepository specializationRepo;
    private final ServiceCapabilityRepository capabilityRepo;

    @Transactional(readOnly = true)
    public PageResponse<MedicalServiceResponse> search(String keyword, DepartmentType departmentType,
                                                        ServiceStatus status, UUID specializationId,
                                                        Pageable pageable) {
        Page<MedicalService> page = repo.search(keyword, departmentType, status, specializationId, pageable);
        return PageResponse.from(page, s -> MedicalServiceResponse.from(s));
    }

    /**
     * API cho khach hang/benh nhan xem dich vu dang hoat dong.
     * Chi tra ve cac dich vu co status = ACTIVE.
     */
    @Transactional(readOnly = true)
    public PageResponse<MedicalServiceResponse> listAvailable(String keyword, DepartmentType departmentType,
                                                               Pageable pageable) {
        Page<MedicalService> page = repo.searchCustomerBookable(keyword, departmentType, pageable);
        return PageResponse.from(page, s -> MedicalServiceResponse.from(s));
    }

    @Transactional(readOnly = true)
    public MedicalServiceResponse get(UUID id) {
        return MedicalServiceResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", repo.count());
        stats.put("active", repo.count((root, query, cb) -> cb.equal(root.get("status"), ServiceStatus.ACTIVE)));
        stats.put("suspended", repo.count((root, query, cb) -> cb.equal(root.get("status"), ServiceStatus.INACTIVE)));
        stats.put("draft", repo.count((root, query, cb) -> cb.equal(root.get("status"), ServiceStatus.DRAFT)));
        return stats;
    }

    /**
     * Tao dich vu moi - mac dinh la DRAFT.
     */
    public MedicalServiceResponse create(MedicalServiceCreateRequest req) {
        validateDemographicRules(req.minimumAge(), req.maximumAge());
        if (req.allowedGender() == org.example.doansummer2026.enums.Gender.OTHER) {
            throw new BadRequestException("Hệ thống chỉ hỗ trợ giới tính Nam hoặc Nữ");
        }
        String normalizedName = normalizeName(req.name());
        if (repo.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Tên dịch vụ đã tồn tại: " + normalizedName);
        }
        if (repo.existsByServiceCode(req.serviceCode())) {
            throw new ConflictException("Mã dịch vụ đã tồn tại: " + req.serviceCode());
        }

        DepartmentType departmentType = req.departmentType().normalized();
        if (departmentType == DepartmentType.EXAMINATION && req.requiredSpecializationId() == null) {
            throw new BadRequestException("Dịch vụ khám bệnh bắt buộc chọn chuyên khoa phục vụ");
        }
        if (departmentType != DepartmentType.EXAMINATION && req.requiredCapabilityId() == null) {
            throw new BadRequestException("Dịch vụ cận lâm sàng bắt buộc chọn danh mục kỹ thuật");
        }
        Specialization spec = null;
        if (departmentType == DepartmentType.EXAMINATION && req.requiredSpecializationId() != null) {
            spec = specializationRepo.findById(req.requiredSpecializationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Chuyên khoa không tồn tại: " + req.requiredSpecializationId()));
            if (Boolean.FALSE.equals(spec.getActive())) {
                throw new ConflictException("Chuyên khoa đã ngừng hoạt động và không thể gán cho dịch vụ mới");
            }
        }
        MedicalService s = MedicalService.builder()
                .serviceCode(req.serviceCode())
                .name(normalizedName)
                .description(req.description())
                .departmentType(departmentType)
                .price(req.price() != null ? req.price() : BigDecimal.ZERO)
                .status(ServiceStatus.DRAFT)
                .isPointOfCare(req.isPointOfCare() != null ? req.isPointOfCare() : false)
                .durationMinutes(req.durationMinutes() != null ? req.durationMinutes() : 15)
                .workflowPriority(req.workflowPriority() != null ? req.workflowPriority() : 1)
                .requiresDoctorOrder(Boolean.TRUE.equals(req.requiresDoctorOrder()))
                .requiresReturnToDoctor(Boolean.TRUE.equals(req.requiresReturnToDoctor()))
                .requiresSpecimen(departmentType != DepartmentType.EXAMINATION
                        && Boolean.TRUE.equals(req.requiresSpecimen()))
                .resultWaitMinutes(req.resultWaitMinutes() != null ? req.resultWaitMinutes() : 0)
                .allowCustomerBooking(req.allowCustomerBooking() != null ? req.allowCustomerBooking() : true)
                .minimumAge(req.minimumAge() != null ? req.minimumAge() : 0)
                .maximumAge(req.maximumAge() != null ? req.maximumAge() : 120)
                .allowedGender(req.allowedGender())
                .department(null)
                .requiredSpecialization(spec)
                .requiredCapability(departmentType != DepartmentType.EXAMINATION && req.requiredCapabilityId() != null
                        ? capabilityRepo.findById(req.requiredCapabilityId()).orElseThrow(() ->
                        new ResourceNotFoundException("Danh mục kỹ thuật không tồn tại: " + req.requiredCapabilityId())) : null)
                .build();
        if (s.getRequiredCapability() != null && Boolean.FALSE.equals(s.getRequiredCapability().getActive())) {
            throw new ConflictException("Danh mục kỹ thuật đã ngừng hoạt động và không thể gán cho dịch vụ mới");
        }
        return MedicalServiceResponse.from(repo.save(s));
    }

    /** Cap nhat dich vu va chi cho phep chuyen trang thai theo workflow da chot. */
    public MedicalServiceResponse update(UUID id, MedicalServiceUpdateRequest req) {
        validateDemographicRules(req.minimumAge(), req.maximumAge());
        if (req.allowedGender() == org.example.doansummer2026.enums.Gender.OTHER) {
            throw new BadRequestException("Hệ thống chỉ hỗ trợ giới tính Nam hoặc Nữ");
        }
        MedicalService s = findById(id);
        validateStatusTransition(s.getStatus(), req.status());
        if (req.name() != null) {
            String normalizedName = normalizeName(req.name());
            if (repo.existsByNameIgnoreCaseAndServiceIdNot(normalizedName, id)) {
                throw new ConflictException("Tên dịch vụ đã tồn tại: " + normalizedName);
            }
            s.setName(normalizedName);
        }
        if (req.description() != null) s.setDescription(req.description());
        if (req.departmentType() != null) s.setDepartmentType(req.departmentType().normalized());
        if (req.price() != null) s.setPrice(req.price());
        if (req.status() != null) s.setStatus(req.status());
        if (req.isPointOfCare() != null) s.setIsPointOfCare(req.isPointOfCare());
        if (req.durationMinutes() != null) s.setDurationMinutes(req.durationMinutes());
        if (req.workflowPriority() != null) s.setWorkflowPriority(req.workflowPriority());
        if (req.requiresDoctorOrder() != null) s.setRequiresDoctorOrder(req.requiresDoctorOrder());
        if (req.requiresReturnToDoctor() != null) s.setRequiresReturnToDoctor(req.requiresReturnToDoctor());
        if (req.requiresSpecimen() != null) s.setRequiresSpecimen(req.requiresSpecimen());
        if (req.resultWaitMinutes() != null) s.setResultWaitMinutes(req.resultWaitMinutes());
        if (req.allowCustomerBooking() != null) s.setAllowCustomerBooking(req.allowCustomerBooking());
        if (req.minimumAge() != null || req.maximumAge() != null || req.allowedGender() != null) {
            s.setMinimumAge(req.minimumAge() != null ? req.minimumAge() : 0);
            s.setMaximumAge(req.maximumAge() != null ? req.maximumAge() : 120);
            s.setAllowedGender(req.allowedGender());
        }
        // Phong cu the se do bo dieu phoi queue chon khi check-in.
        s.setDepartment(null);
        if (s.getDepartmentType() == DepartmentType.EXAMINATION) {
            // Dich vu kham chi dung chuyen khoa; khong duoc giu cau hinh CLS cu.
            s.setRequiredCapability(null);
            s.setRequiresSpecimen(false);
            if (req.requiredSpecializationId() != null) {
                Specialization specialization = specializationRepo.findById(req.requiredSpecializationId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Chuyên khoa không tồn tại: " + req.requiredSpecializationId()));
                if (Boolean.FALSE.equals(specialization.getActive())) {
                    throw new ConflictException("Chuyên khoa đã ngừng hoạt động và không thể gán cho dịch vụ");
                }
                s.setRequiredSpecialization(specialization);
            }
            if (s.getRequiredSpecialization() == null) {
                throw new BadRequestException("Dịch vụ khám bệnh bắt buộc chọn chuyên khoa phục vụ");
            }
        } else {
            // Dich vu CLS chi dung nang luc; gan nang luc moi truoc khi kiem tra bat buoc.
            s.setRequiredSpecialization(null);
            if (req.requiredCapabilityId() != null) {
                org.example.doansummer2026.model.ServiceCapability capability = capabilityRepo.findById(req.requiredCapabilityId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Danh mục kỹ thuật không tồn tại: " + req.requiredCapabilityId()));
                if (Boolean.FALSE.equals(capability.getActive())) {
                    throw new ConflictException("Danh mục kỹ thuật đã ngừng hoạt động và không thể gán cho dịch vụ");
                }
                s.setRequiredCapability(capability);
            }
            if (s.getRequiredCapability() == null) {
                throw new BadRequestException("Dịch vụ cận lâm sàng bắt buộc chọn danh mục kỹ thuật");
            }
        }

        return MedicalServiceResponse.from(repo.save(s));
    }

    /**
     * Xoa dich vu - chi cho phep xoa khi status = DRAFT.
     */
    public void delete(UUID id) {
        MedicalService s = findById(id);
        if (s.getStatus() != ServiceStatus.DRAFT) {
            throw new ConflictException("Chỉ được xóa dịch vụ ở trạng thái bản nháp");
        }
        long operationalReferences = repo.countOperationalReferences(id);
        if (operationalReferences > 0) {
            throw new ConflictException(
                    "Không thể xóa dịch vụ đã phát sinh lịch hẹn, hóa đơn, hàng chờ hoặc yêu cầu cận lâm sàng (" +
                    operationalReferences + " liên kết). Hãy chuyển dịch vụ sang trạng thái ngừng hoạt động.");
        }
        repo.deleteById(id);
    }

    /**
     * Ngung hoat dong dich vu - chuyen tu ACTIVE sang INACTIVE.
     */
    public MedicalServiceResponse deactivate(UUID id) {
        MedicalService s = findById(id);
        if (s.getStatus() != ServiceStatus.ACTIVE) {
            throw new ConflictException("Chỉ được tạm ngừng dịch vụ đang áp dụng");
        }
        s.setStatus(ServiceStatus.INACTIVE);
        return MedicalServiceResponse.from(repo.save(s));
    }

    /**
     * Phat hanh dich vu - chuyen tu DRAFT sang ACTIVE.
     */
    public MedicalServiceResponse publish(UUID id) {
        MedicalService s = findById(id);
        if (s.getStatus() != ServiceStatus.DRAFT) {
            throw new ConflictException("Chỉ được phát hành dịch vụ ở trạng thái bản nháp");
        }
        if (s.getDepartmentType() == DepartmentType.EXAMINATION && s.getRequiredSpecialization() == null) {
            throw new BadRequestException("Không thể phát hành dịch vụ khám bệnh chưa có chuyên khoa phục vụ");
        }
        if (s.getDepartmentType() != DepartmentType.EXAMINATION && s.getRequiredCapability() == null) {
            throw new BadRequestException("Không thể phát hành dịch vụ chưa có danh mục kỹ thuật");
        }
        s.setStatus(ServiceStatus.ACTIVE);
        return MedicalServiceResponse.from(repo.save(s));
    }

    public MedicalService findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + id));
    }

    private void validateDemographicRules(Integer minimumAge, Integer maximumAge) {
        if (minimumAge != null && maximumAge != null && minimumAge > maximumAge) {
            throw new BadRequestException("Tuổi tối thiểu không được lớn hơn tuổi tối đa");
        }
    }

    private String normalizeName(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    /** DRAFT chi co the phat hanh sang ACTIVE; ACTIVE va INACTIVE co the chuyen qua lai. */
    private void validateStatusTransition(ServiceStatus current, ServiceStatus requested) {
        if (requested == null || requested == current) return;
        if (current == ServiceStatus.DRAFT && requested != ServiceStatus.ACTIVE) {
            throw new ConflictException("Dịch vụ bản nháp chỉ có thể chuyển sang trạng thái đang áp dụng");
        }
        if (current != ServiceStatus.DRAFT && requested == ServiceStatus.DRAFT) {
            throw new ConflictException("Dịch vụ đã áp dụng hoặc tạm ngừng không thể quay lại bản nháp");
        }
    }
}
