package vn.edu.fpt.cares.dto.customervisit;

import vn.edu.fpt.cares.enums.VisitStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerVisitUpdateRequest(
        VisitStatus status,
        LocalDateTime checkOutTime
) {}



