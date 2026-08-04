package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.medicalRecord.MedicineCatalogResponse;
import org.example.doansummer2026.repository.MedicineCatalogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/medicines")
@RequiredArgsConstructor
public class MedicineCatalogController {
    private final MedicineCatalogRepository repository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public List<MedicineCatalogResponse> search(@RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        return repository.searchActive(keyword == null ? "" : keyword.trim(), PageRequest.of(0, safeSize))
                .stream().map(MedicineCatalogResponse::from).toList();
    }
}
