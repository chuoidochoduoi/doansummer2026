package vn.edu.fpt.cares.dto.medicalservice;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ServiceSelectionResolveRequest(
        @NotEmpty(message = "Vui lòng chọn ít nhất một dịch vụ") List<UUID> serviceIds
) {}
