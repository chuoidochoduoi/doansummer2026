package vn.edu.fpt.cares.dto.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InvoiceInsuranceRequest(
        @NotNull UUID insuranceId,
        @NotBlank String bhytCode
) {}
