package com.sierradorada.sdbrewpi.production;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileStepRequest(
        @NotBlank @Size(max = 80) String name,
        @DecimalMin("0.0") @DecimalMax("35.0") double targetTemperatureC,
        @Min(1) @Max(1440) int durationHours) {}
