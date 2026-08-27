package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.icd.ICD10Response;
import org.example.doansummer2026.dto.icd.ICD10CreateRequest;
import org.example.doansummer2026.dto.icd.ICD10UpdateRequest;
import org.example.doansummer2026.model.Icd10Code;
import org.springframework.data.domain.Pageable;

/** Service interface for ICD-10 code management. */
public interface Icd10CodeServiceInterface {
    PageResponse<ICD10Response> search(String keyword, String category, Pageable pageable);
    ICD10Response get(String code);
    ICD10Response create(ICD10CreateRequest req);
    ICD10Response update(String code, ICD10UpdateRequest req);
    void delete(String code);
    Icd10Code findByCode(String code);
}