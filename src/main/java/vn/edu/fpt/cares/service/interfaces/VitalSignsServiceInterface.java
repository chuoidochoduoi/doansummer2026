package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.dto.vitalsigns.VitalSignsResponse;
import vn.edu.fpt.cares.dto.vitalsigns.VitalSignsCreateRequest;
import vn.edu.fpt.cares.dto.vitalsigns.VitalSignsUpdateRequest;
import vn.edu.fpt.cares.model.VitalSigns;

import java.util.UUID;

/** Service interface for VitalSigns management. */
public interface VitalSignsServiceInterface {
    VitalSignsResponse get(UUID id);
    VitalSignsResponse create(VitalSignsCreateRequest req);
    VitalSignsResponse update(UUID id, VitalSignsUpdateRequest req);
    void delete(UUID id);
    VitalSigns findById(UUID id);
}



