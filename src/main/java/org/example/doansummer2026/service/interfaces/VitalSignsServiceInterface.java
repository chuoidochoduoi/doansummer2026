package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.dto.vitalSigns.VitalSignsResponse;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsCreateRequest;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsUpdateRequest;
import org.example.doansummer2026.model.VitalSigns;

import java.util.UUID;

/** Service interface for VitalSigns management. */
public interface VitalSignsServiceInterface {
    VitalSignsResponse get(UUID id);
    VitalSignsResponse create(VitalSignsCreateRequest req);
    VitalSignsResponse update(UUID id, VitalSignsUpdateRequest req);
    void delete(UUID id);
    VitalSigns findById(UUID id);
}