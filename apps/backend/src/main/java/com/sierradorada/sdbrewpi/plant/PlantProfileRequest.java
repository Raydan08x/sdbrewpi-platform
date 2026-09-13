package com.sierradorada.sdbrewpi.plant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlantProfileRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 160) String companyName,
        @Size(max = 180) String legalName,
        @Size(max = 40) String taxId,
        @NotBlank @Size(max = 80) String timezone,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @DecimalMin("1.0") @DecimalMax("10000000.0") Double nominalBatchCapacityL,
        @Min(1) @Max(1000) int plannedFermenters,
        @DecimalMin("0.0") @DecimalMax("100000.0") double pipingDeadVolumeL,
        @Size(max = 500) String logoUrl,
        @Min(0) long expectedRevision) {}
