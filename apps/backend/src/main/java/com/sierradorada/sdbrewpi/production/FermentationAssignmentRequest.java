package com.sierradorada.sdbrewpi.production;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record FermentationAssignmentRequest(
    @NotBlank String tankId,
    @DecimalMin("1.0") @DecimalMax("10000.0") double transferredVolumeL,
    @Min(0) long expectedRevision
) {}
