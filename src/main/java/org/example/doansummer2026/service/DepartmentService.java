package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.department.DepartmentCreateRequest;
import org.example.doansummer2026.dto.department.DepartmentResponse;
import org.example.doansummer2026.dto.department.DepartmentUpdateRequest;
import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.DepartmentServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentService implements DepartmentServiceInterface {

    private final DepartmentRepository repo;
    private final StaffInfoRepository staffRepo;

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
        Department.DepartmentBuilder builder = Department.builder()
                .roomCode(req.roomCode())
                .name(req.name())
                .status(req.status() != null ? req.status() : DepartmentStatus.AVAILABLE)
                .departmentType(req.departmentType() != null ? req.departmentType() : DepartmentType.EXAMINATION)
                .description(req.description());

        if (req.headDoctorId() != null) {
            StaffInfo headDoctor = staffRepo.findById(req.headDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff khong ton tai: " + req.headDoctorId()));
            // Kiem tra bac si chua duoc gianh cho phong khac
            if (repo.existsByHeadDoctor_StaffId(req.headDoctorId())) {
                throw new ConflictException("Bac si nay da phu trach phong khac: " + req.headDoctorId());
            }
            builder.headDoctor(headDoctor);
        }

        return DepartmentResponse.from(repo.save(builder.build()));
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
        if (req.departmentType() != null) d.setDepartmentType(req.departmentType());
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
        return DepartmentResponse.from(repo.save(d));
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
}



