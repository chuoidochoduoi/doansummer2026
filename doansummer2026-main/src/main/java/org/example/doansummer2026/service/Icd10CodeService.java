package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.icd.ICD10CreateRequest;
import org.example.doansummer2026.dto.icd.ICD10Response;
import org.example.doansummer2026.dto.icd.ICD10UpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Icd10Code;
import org.example.doansummer2026.repository.Icd10CodeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.Icd10CodeServiceInterface;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class Icd10CodeService implements Icd10CodeServiceInterface {

    private final Icd10CodeRepository repo;

    @Transactional(readOnly = true)
    public PageResponse<ICD10Response> search(String keyword, String category, Pageable pageable) {
        Page<Icd10Code> page = repo.search(keyword, category, pageable);
        return PageResponse.from(page, ICD10Response::from);
    }

    @Transactional(readOnly = true)
    public ICD10Response get(String code) {
        return ICD10Response.from(findByCode(code));
    }

    public ICD10Response create(ICD10CreateRequest req) {
        if (repo.existsByCode(req.code())) {
            throw new ConflictException("Mã ICD-10 đã tồn tại: " + req.code());
        }
        Icd10Code c = Icd10Code.builder()
                .code(req.code())
                .name(req.name())
                .description(req.description())
                .category(req.category())
                .build();
        return ICD10Response.from(repo.save(c));
    }

    public ICD10Response update(String code, ICD10UpdateRequest req) {
        Icd10Code c = findByCode(code);
        if (req.name() != null) {
            c.setName(req.name());
        }
        if (req.description() != null) {
            c.setDescription(req.description());
        }
        if (req.category() != null) {
            c.setCategory(req.category());
        }
        return ICD10Response.from(repo.save(c));
    }

    public void delete(String code) {
        if (!repo.existsById(code)) {
            throw new ResourceNotFoundException("Mã ICD-10 không tồn tại: " + code);
        }
        repo.deleteById(code);
    }

    public Icd10Code findByCode(String code) {
        return repo.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Mã ICD-10 không tồn tại: " + code));
    }
}
