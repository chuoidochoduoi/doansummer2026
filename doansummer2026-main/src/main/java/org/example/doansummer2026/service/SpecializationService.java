package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.specialization.SpecializationCreateRequest;
import org.example.doansummer2026.dto.specialization.SpecializationResponse;
import org.example.doansummer2026.dto.specialization.SpecializationUpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Specialization;
import org.example.doansummer2026.repository.SpecializationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.SpecializationServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SpecializationService implements SpecializationServiceInterface {

    private final SpecializationRepository repo;

    @Transactional(readOnly = true)
    public PageResponse<SpecializationResponse> list(Pageable pageable) {
        Page<Specialization> page = repo.findAll(pageable);
        return PageResponse.from(page, SpecializationResponse::from);
    }

    @Transactional(readOnly = true)
    public SpecializationResponse get(UUID id) {
        return SpecializationResponse.from(findById(id));
    }

    public SpecializationResponse create(SpecializationCreateRequest req) {
        String normalizedName = normalizeName(req.name());
        if (repo.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Tên chuyên khoa đã tồn tại: " + normalizedName);
        }
        Specialization s = Specialization.builder()
                .name(normalizedName)
                .description(normalizeOptional(req.description()))
                .active(req.active() == null || req.active())
                .build();
        return SpecializationResponse.from(repo.save(s));
    }

    public SpecializationResponse update(UUID id, SpecializationUpdateRequest req) {
        Specialization s = findById(id);
        if (req.name() != null) {
            String normalizedName = normalizeName(req.name());
            if (repo.existsByNameIgnoreCaseAndSpecializationIdNot(normalizedName, id)) {
                throw new ConflictException("Tên chuyên khoa đã tồn tại: " + normalizedName);
            }
            s.setName(normalizedName);
        }
        if (req.description() != null) s.setDescription(normalizeOptional(req.description()));
        if (req.active() != null && req.active() != !Boolean.FALSE.equals(s.getActive())) {
            if (!req.active() && repo.countActiveReferences(id) > 0) {
                throw new ConflictException(
                        "Không thể ngừng sử dụng chuyên khoa khi vẫn còn phòng, dịch vụ hoặc nhân sự đang hoạt động. "
                                + "Vui lòng ngừng hoặc chuyển các cấu hình liên quan trước."
                );
            }
            s.setActive(req.active());
        }
        return SpecializationResponse.from(repo.save(s));
    }

    private String normalizeName(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replaceAll("\\s+", " ");
    }

    public void delete(UUID id) {
        Specialization specialization = findById(id);
        if (repo.countAllReferences(id) > 0) {
            throw new ConflictException(
                    "Không thể xóa chuyên khoa đã được sử dụng. Hãy chuyển sang trạng thái ngừng hoạt động."
            );
        }
        repo.delete(specialization);
    }

    public Specialization findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chuyên khoa không tồn tại: " + id));
    }

    public Specialization findActiveById(UUID id) {
        Specialization specialization = findById(id);
        if (Boolean.FALSE.equals(specialization.getActive())) {
            throw new ConflictException("Chuyên khoa đã ngừng hoạt động và không thể dùng cho cấu hình mới");
        }
        return specialization;
    }
}



