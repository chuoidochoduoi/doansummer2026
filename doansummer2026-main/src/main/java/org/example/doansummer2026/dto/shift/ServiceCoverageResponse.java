package org.example.doansummer2026.dto.shift;

import java.time.LocalDate;
import java.util.*;

public record ServiceCoverageResponse(
        LocalDate date, UUID shiftId, int totalActiveServices,
        int coveredServices, int uncoveredServices,
        List<ServiceCoverageItemResponse> services
) {}
