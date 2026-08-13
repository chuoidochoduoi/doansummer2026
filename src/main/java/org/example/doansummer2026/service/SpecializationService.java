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
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Chuyên khoa không tồn tại: " + id);
        }
        repo.deleteById(id);
    }

    public Specialization findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chuyên khoa không tồn tại: " + id));
    }
}



