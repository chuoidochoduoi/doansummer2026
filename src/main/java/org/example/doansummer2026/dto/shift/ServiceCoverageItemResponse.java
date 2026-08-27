package org.example.doansummer2026.dto.shift;

import org.example.doansummer2026.enums.ShiftUnavailableReason;
import java.util.*;

public record ServiceCoverageItemResponse(
        UUID serviceId, String serviceCode, String serviceName,
        boolean available, ShiftUnavailableReason reason,
        List<UUID> eligibleStaffIds, List<String> eligibleStaffNames
) {}
