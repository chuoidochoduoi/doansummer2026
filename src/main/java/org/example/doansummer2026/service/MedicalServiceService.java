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
            throw new BadRequestException("He thong chi ho tro gioi tinh MALE hoac FEMALE");
        }
        if (repo.existsByName(req.name())) {
            throw new ConflictException("Ten dich vu da ton tai: " + req.name());
        }
        if (repo.existsByServiceCode(req.serviceCode())) {
            throw new ConflictException("Ma dich vu da ton tai: " + req.serviceCode());
        }

        DepartmentType departmentType = req.departmentType().normalized();
        if (departmentType == DepartmentType.EXAMINATION && req.requiredSpecializationId() == null) {
            throw new BadRequestException("Dich vu phong kham bat buoc chon chuyen khoa phuc vu");
        }
        if (departmentType != DepartmentType.EXAMINATION && req.requiredCapabilityId() == null) {
            throw new BadRequestException("Dich vu can lam sang bat buoc chon nang luc thuc hien");
        }
        Specialization spec = null;
        if (departmentType == DepartmentType.EXAMINATION && req.requiredSpecializationId() != null) {
            spec = specializationRepo.findById(req.requiredSpecializationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Chuyen khoa khong ton tai: " + req.requiredSpecializationId()));
        }
        MedicalService s = MedicalService.builder()
                .serviceCode(req.serviceCode())
                .name(req.name())
                .description(req.description())
                .departmentType(departmentType)
                .price(req.price() != null ? req.price() : BigDecimal.ZERO)
                .status(req.status() != null ? req.status() : ServiceStatus.DRAFT)
                .isPointOfCare(req.isPointOfCare() != null ? req.isPointOfCare() : false)
                .durationMinutes(req.durationMinutes() != null ? req.durationMinutes() : 15)
                .workflowPriority(req.workflowPriority() != null ? req.workflowPriority() : 1)
                .requiresDoctorOrder(Boolean.TRUE.equals(req.requiresDoctorOrder()))
                .requiresReturnToDoctor(Boolean.TRUE.equals(req.requiresReturnToDoctor()))
                .resultWaitMinutes(req.resultWaitMinutes() != null ? req.resultWaitMinutes() : 0)
                .allowCustomerBooking(req.allowCustomerBooking() != null ? req.allowCustomerBooking() : true)
                .minimumAge(req.minimumAge() != null ? req.minimumAge() : 0)
                .maximumAge(req.maximumAge() != null ? req.maximumAge() : 120)
                .allowedGender(req.allowedGender())
                .department(null)
                .requiredSpecialization(spec)
                .requiredCapability(req.requiredCapabilityId() != null
                        ? capabilityRepo.findById(req.requiredCapabilityId()).orElseThrow(() ->
                        new ResourceNotFoundException("Nang luc thuc hien khong ton tai: " + req.requiredCapabilityId())) : null)
                .build();
        return MedicalServiceResponse.from(repo.save(s));
    }

    /**
     * Cap nhat dich vu. Chi cap nhat khi status = DRAFT hoac ACTIVE.
     */
    public MedicalServiceResponse update(UUID id, MedicalServiceUpdateRequest req) {
        validateDemographicRules(req.minimumAge(), req.maximumAge());
        if (req.allowedGender() == org.example.doansummer2026.enums.Gender.OTHER) {
            throw new BadRequestException("He thong chi ho tro gioi tinh MALE hoac FEMALE");
        }
        MedicalService s = findById(id);
        if (s.getStatus() == ServiceStatus.INACTIVE) {
            throw new ConflictException("Khong the chinh sua dich vu da ngung hoat dong");
        }
        if (req.name() != null && !req.name().equals(s.getName())) {
            if (repo.existsByName(req.name())) {
                throw new ConflictException("Ten dich vu da ton tai: " + req.name());
            }
            s.setName(req.name());
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
        if (req.resultWaitMinutes() != null) s.setResultWaitMinutes(req.resultWaitMinutes());
        if (req.allowCustomerBooking() != null) s.setAllowCustomerBooking(req.allowCustomerBooking());
        if (req.minimumAge() != null || req.maximumAge() != null || req.allowedGender() != null) {
            s.setMinimumAge(req.minimumAge() != null ? req.minimumAge() : 0);
            s.setMaximumAge(req.maximumAge() != null ? req.maximumAge() : 120);
            s.setAllowedGender(req.allowedGender());
        }
        // Phong cu the se do bo dieu phoi queue chon khi check-in.
        s.setDepartment(null);
        if (s.getDepartmentType() == DepartmentType.EXAMINATION && req.requiredSpecializationId() != null) {
            s.setRequiredSpecialization(specializationRepo.findById(req.requiredSpecializationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Chuyen khoa khong ton tai: " + req.requiredSpecializationId())));
        } else if (s.getDepartmentType() != DepartmentType.EXAMINATION) {
            s.setRequiredSpecialization(null);
        }
        if (s.getDepartmentType() == DepartmentType.EXAMINATION && s.getRequiredSpecialization() == null) {
            throw new BadRequestException("Dich vu phong kham bat buoc chon chuyen khoa phuc vu");
        }
        if (s.getDepartmentType() != DepartmentType.EXAMINATION && s.getRequiredCapability() == null) {
            throw new BadRequestException("Dich vu can lam sang bat buoc chon nang luc thuc hien");
        }
        if (req.requiredCapabilityId() != null) {
            s.setRequiredCapability(capabilityRepo.findById(req.requiredCapabilityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Nang luc thuc hien khong ton tai: " + req.requiredCapabilityId())));
        }

        return MedicalServiceResponse.from(repo.save(s));
    }

    /**
     * Xoa dich vu - chi cho phep xoa khi status = DRAFT.
     */
    public void delete(UUID id) {
        MedicalService s = findById(id);
        if (s.getStatus() != ServiceStatus.DRAFT) {
            throw new ConflictException("Chi duoc xoa dich vu o trang thai DRAFT");
        }
        repo.deleteById(id);
    }

    /**
     * Ngung hoat dong dich vu - chuyen tu ACTIVE sang INACTIVE.
     */
    public MedicalServiceResponse deactivate(UUID id) {
        MedicalService s = findById(id);
        if (s.getStatus() != ServiceStatus.ACTIVE) {
            throw new ConflictException("Chi duoc ngung dich vu o trang thai ACTIVE");
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
            throw new ConflictException("Chi duoc phat hanh dich vu o trang thai DRAFT");
        }
        if (s.getDepartmentType() == DepartmentType.EXAMINATION && s.getRequiredSpecialization() == null) {
            throw new BadRequestException("Khong the phat hanh dich vu phong kham chua co chuyen khoa phuc vu");
        }
        if (s.getDepartmentType() != DepartmentType.EXAMINATION && s.getRequiredCapability() == null) {
            throw new BadRequestException("Khong the phat hanh dich vu chua co nang luc thuc hien");
        }
        s.setStatus(ServiceStatus.ACTIVE);
        return MedicalServiceResponse.from(repo.save(s));
    }

    public MedicalService findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + id));
    }

    private void validateDemographicRules(Integer minimumAge, Integer maximumAge) {
        if (minimumAge != null && maximumAge != null && minimumAge > maximumAge) {
            throw new BadRequestException("Tuoi toi thieu khong duoc lon hon tuoi toi da");
        }
    }
}
