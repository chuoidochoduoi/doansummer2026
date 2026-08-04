package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.department.DepartmentCreateRequest;
import org.example.doansummer2026.dto.department.DepartmentResponse;
import org.example.doansummer2026.dto.department.DepartmentUpdateRequest;
import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.SpecializationRepository;
import org.example.doansummer2026.repository.ServiceCapabilityRepository;
import org.example.doansummer2026.repository.StaffCapabilityRepository;
import org.example.doansummer2026.enums.StaffCapabilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.DepartmentServiceInterface;
import org.example.doansummer2026.service.AuthService;
import org.example.doansummer2026.model.Account;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentService implements DepartmentServiceInterface {

    private final DepartmentRepository repo;
    private final StaffInfoRepository staffRepo;
    private final SpecializationRepository specializationRepo;
    private final ServiceCapabilityRepository capabilityRepo;
    private final StaffCapabilityRepository staffCapabilityRepo;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> listAll(Pageable pageable) {
        Page<Department> page = repo.findAllWithHeadDoctor(pageable);
        return PageResponse.from(page, DepartmentResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> list(DepartmentType departmentType, Pageable pageable) {
        Page<Department> page = repo.findAllByDepartmentType(departmentType, pageable);
        return PageResponse.from(page, DepartmentResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> listMultiple(Pageable pageable, List<DepartmentType> departmentTypes) {
        Page<Department> page = repo.findAllByDepartmentTypeIn(departmentTypes, pageable);
        return PageResponse.from(page, DepartmentResponse::from);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(UUID id) {
        return DepartmentResponse.from(findById(id));
    }

    public DepartmentResponse create(DepartmentCreateRequest req) {
        if (repo.existsByRoomCode(req.roomCode())) {
            throw new ConflictException("Ma phong da ton tai: " + req.roomCode());
        }
        if (repo.existsByName(req.name())) {
            throw new ConflictException("Ten khoa da ton tai: " + req.name());
        }
        DepartmentType departmentType = req.departmentType() != null ? req.departmentType().normalized() : DepartmentType.EXAMINATION;
        if (departmentType == DepartmentType.EXAMINATION && req.specializationId() == null) {
            throw new BadRequestException("Vui long chon chuyen khoa cho phong kham");
        }
        Department.DepartmentBuilder builder = Department.builder()
                .roomCode(req.roomCode())
                .name(req.name())
                .status(req.status() != null ? req.status() : DepartmentStatus.AVAILABLE)
                .departmentType(departmentType)
                .description(req.description());

        if (departmentType == DepartmentType.EXAMINATION && req.specializationId() != null) {
            builder.specialization(specializationRepo.findById(req.specializationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chuyen khoa khong ton tai: " + req.specializationId())));
        }

        if (req.headDoctorId() != null) {
            StaffInfo headDoctor = staffRepo.findById(req.headDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff khong ton tai: " + req.headDoctorId()));
            // Kiem tra bac si chua duoc gianh cho phong khac
            if (repo.existsByHeadDoctor_StaffId(req.headDoctorId())) {
                throw new ConflictException("Bac si nay da phu trach phong khac: " + req.headDoctorId());
            }
            builder.headDoctor(headDoctor);
        }

        Department department = builder.build();
        if (req.capabilityIds() != null) {
            department.setCapabilities(new java.util.HashSet<>(capabilityRepo.findAllById(req.capabilityIds())));
            if (department.getCapabilities().size() != req.capabilityIds().stream().distinct().count()) {
                throw new ResourceNotFoundException("Co nang luc thuc hien khong ton tai");
            }
        }
        validateHeadDoctorCapabilities(department);
        Department saved = repo.save(department);

        if (req.nurseIds() != null && !req.nurseIds().isEmpty()) {
            for (UUID nurseId : req.nurseIds()) {
                StaffInfo nurse = staffRepo.findById(nurseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Y ta khong ton tai: " + nurseId));
                if (nurse.getDepartment() != null) {
                    throw new ConflictException("Y ta nay da duoc chi dinh cho phong khac: " + nurseId);
                }
                nurse.setDepartment(saved);
                staffRepo.save(nurse);
            }
        }

        // Fetch again to ensure nurses are loaded in response
        return DepartmentResponse.from(repo.findById(saved.getDepartmentId()).get());
    }

    public DepartmentResponse update(UUID id, DepartmentUpdateRequest req) {
        Department d = findById(id);
        if (req.roomCode() != null && !req.roomCode().equals(d.getRoomCode())) {
            if (repo.existsByRoomCode(req.roomCode())) {
                throw new ConflictException("Ma phong da ton tai: " + req.roomCode());
            }
            d.setRoomCode(req.roomCode());
        }
        if (req.name() != null && !req.name().equals(d.getName())) {
            if (repo.existsByName(req.name())) {
                throw new ConflictException("Ten khoa da ton tai: " + req.name());
            }
            d.setName(req.name());
        }
        if (req.status() != null) d.setStatus(req.status());
        if (req.departmentType() != null) {
            d.setDepartmentType(req.departmentType().normalized());
            if (req.departmentType().normalized() != DepartmentType.EXAMINATION) {
                d.setSpecialization(null);
            }
        }
        if (d.getDepartmentType() == DepartmentType.EXAMINATION && req.specializationId() != null) {
            d.setSpecialization(specializationRepo.findById(req.specializationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chuyen khoa khong ton tai: " + req.specializationId())));
        }
        if (req.capabilityIds() != null) {
            d.setCapabilities(new java.util.HashSet<>(capabilityRepo.findAllById(req.capabilityIds())));
            if (d.getCapabilities().size() != req.capabilityIds().stream().distinct().count()) {
                throw new ResourceNotFoundException("Co nang luc thuc hien khong ton tai");
            }
        }
        if (req.description() != null) d.setDescription(req.description());
        if (req.headDoctorId() != null) {
            // Kiem tra bac si moi chua duoc gianh cho phong khac (tru phong hien tai)
            if (repo.existsByHeadDoctor_StaffId(req.headDoctorId())
                    && (d.getHeadDoctor() == null || !d.getHeadDoctor().getStaffId().equals(req.headDoctorId()))) {
                throw new ConflictException("Bac si nay da phu trach phong khac: " + req.headDoctorId());
            }
            StaffInfo headDoctor = staffRepo.findById(req.headDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff khong ton tai: " + req.headDoctorId()));
            d.setHeadDoctor(headDoctor);
        }

        if (req.nurseIds() != null) {
            // Clear old nurses
            List<StaffInfo> currentNurses = staffRepo.findByDepartment_DepartmentId(d.getDepartmentId());
            for (StaffInfo oldNurse : currentNurses) {
                if (!req.nurseIds().contains(oldNurse.getStaffId())) {
                    oldNurse.setDepartment(null);
                    staffRepo.save(oldNurse);
                }
            }

            // Set new nurses
            for (UUID nurseId : req.nurseIds()) {
                StaffInfo nurse = staffRepo.findById(nurseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Y ta khong ton tai: " + nurseId));
                if (nurse.getDepartment() != null && !nurse.getDepartment().getDepartmentId().equals(d.getDepartmentId())) {
                    throw new ConflictException("Y ta nay da duoc chi dinh cho phong khac: " + nurseId);
                }
                nurse.setDepartment(d);
                staffRepo.save(nurse);
            }
        }
        
        validateHeadDoctorCapabilities(d);
        Department saved = repo.save(d);
        // Ensure nurses collection is up to date for the response mapping
        return DepartmentResponse.from(repo.findById(saved.getDepartmentId()).get());
    }

    public DepartmentResponse updateStatus(UUID id, DepartmentStatus status) {
        Department d = findById(id);
        if (status != null) {
            d.setStatus(status);
        }
        return DepartmentResponse.from(repo.save(d));
    }

    private void validateHeadDoctorCapabilities(Department department) {
        if (department.getHeadDoctor() == null || department.getCapabilities() == null
                || department.getCapabilities().isEmpty()) return;
        boolean matches = department.getCapabilities().stream().anyMatch(capability ->
                staffCapabilityRepo.existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(
                        department.getHeadDoctor().getStaffId(), capability.getCapabilityId(), StaffCapabilityStatus.ACTIVE));
        if (!matches) throw new BadRequestException("Bác sĩ phụ trách chưa có kỹ thuật được cấp phép đang hiệu lực phù hợp với phòng");
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Khoa khong ton tai: " + id);
        }
        repo.deleteById(id);
    }

    public Department findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khoa khong ton tai: " + id));
    }

    @Transactional(readOnly = true)
    public Department findByIdWithHeadDoctor(UUID id) {
        return repo.findWithHeadDoctorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khoa khong ton tai: " + id));
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getMyDepartment() {
        Account acc = authService.currentAccount();
        StaffInfo staff = staffRepo.findFirstByProfile_Account_Username(acc.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Ban khong phai la nhan vien"));
        // Tim phong theo bac si phu trach truoc
        Optional<Department> dept = repo.findByHeadDoctor_StaffId(staff.getStaffId());
        if (dept.isEmpty()) {
            // Neu la y ta, tim phong duoc phan cong
            dept = repo.findFirstByNurses_StaffId(staff.getStaffId());
        }
        return DepartmentResponse.from(
            dept.orElseThrow(() -> new ResourceNotFoundException("Chua duoc chi dinh phong"))
        );
    }
}
