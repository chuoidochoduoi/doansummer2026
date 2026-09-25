package vn.edu.fpt.cares.dto.clinicalform;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ClinicalFormBindingRequest(@NotNull List<UUID> serviceIds) {}
