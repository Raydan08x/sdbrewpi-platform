package com.sierradorada.sdbrewpi.production;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProductionOrderReleaseRequest(
    @NotBlank String recipeVersionId,
    @DecimalMin("1.0") @DecimalMax("10000.0") double plannedVolumeL,
    @NotBlank @Pattern(regexp = "TEST|PILOT|COMMERCIAL") String batchKind,
    @NotBlank @Pattern(regexp = "CERV|HSEL") String productCode
) {}
