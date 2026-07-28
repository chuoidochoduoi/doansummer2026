package org.example.doansummer2026.dto.appointment;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/** Service info in appointment response */
public record ServiceInfo(
        UUID serviceId,
        String serviceName,
        BigDecimal servicePrice
) implements Serializable {
}



