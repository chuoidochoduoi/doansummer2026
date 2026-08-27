package org.example.doansummer2026.dto.customerVisit;

import org.example.doansummer2026.enums.VisitStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerVisitUpdateRequest(
        VisitStatus status,
        LocalDateTime checkOutTime
) {}



