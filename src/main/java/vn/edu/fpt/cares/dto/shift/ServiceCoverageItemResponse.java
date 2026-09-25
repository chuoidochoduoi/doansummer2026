package vn.edu.fpt.cares.dto.shift;

import vn.edu.fpt.cares.enums.ShiftUnavailableReason;
import java.util.*;

public record ServiceCoverageItemResponse(
        UUID serviceId, String serviceCode, String serviceName,
        boolean available, ShiftUnavailableReason reason,
        List<UUID> eligibleStaffIds, List<String> eligibleStaffNames
) {}
