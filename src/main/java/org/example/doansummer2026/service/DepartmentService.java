package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.department.DepartmentCreateRequest;
import org.example.doansummer2026.dto.department.DepartmentResponse;
import org.example.doansummer2026.dto.department.DepartmentUpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.DepartmentServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentService implements DepartmentServiceInterface {

    private final DepartmentRepository repo;

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> list(Pageable pageable) {
        Page<Department> page = repo.findAll(pageable);
        return PageResponse.from(page, DepartmentResponse::from);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(UUID id) {
        return DepartmentResponse.from(findById(id));
    }

    public DepartmentResponse create(DepartmentCreateRequest req) {
        if (repo.existsByName(req.name())) {
            throw new ConflictException("Ten khoa da ton tai: " + req.name());
        }
        Department d = Department.builder()
                .name(req.name())
                .description(req.description())
                .build();
        return DepartmentResponse.from(repo.save(d));
    }

    public DepartmentResponse update(UUID id, DepartmentUpdateRequest req) {
        Department d = findById(id);
        if (req.name() != null && !req.name().equals(d.getName())) {
            if (repo.existsByName(req.name())) {
                throw new ConflictException("Ten khoa da ton tai: " + req.name());
            }
            d.setName(req.name());
        }
        if (req.description() != null) d.setDescription(req.description());
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
}