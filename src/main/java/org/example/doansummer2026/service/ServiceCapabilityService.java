package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.capability.*;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.ServiceCapability;
import org.example.doansummer2026.repository.ServiceCapabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional
public class ServiceCapabilityService {
    private final ServiceCapabilityRepository repository;

    @Transactional(readOnly = true)
    public List<ServiceCapabilityResponse> list() {
        return repository.findAllByOrderByNameAsc().stream().map(ServiceCapabilityResponse::from).toList();
    }

    public ServiceCapabilityResponse create(ServiceCapabilityRequest request) {
        if (repository.existsByCodeIgnoreCase(request.code())) throw new ConflictException("Ma nang luc da ton tai");
        if (repository.existsByNameIgnoreCase(request.name())) throw new ConflictException("Ten nang luc da ton tai");
        return ServiceCapabilityResponse.from(repository.save(ServiceCapability.builder()
                .code(request.code().trim().toUpperCase()).name(request.name().trim())
                .description(request.description()).active(request.active() == null || request.active()).build()));
    }

    public ServiceCapabilityResponse update(UUID id, ServiceCapabilityRequest request) {
        ServiceCapability value = find(id);
        value.setCode(request.code().trim().toUpperCase());
        value.setName(request.name().trim());
        value.setDescription(request.description());
        if (request.active() != null) value.setActive(request.active());
        return ServiceCapabilityResponse.from(repository.save(value));
    }

    public void delete(UUID id) { repository.delete(find(id)); }

    public ServiceCapability find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Nang luc khong ton tai: " + id));
    }
}
