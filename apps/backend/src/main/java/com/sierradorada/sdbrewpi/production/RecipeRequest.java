package com.sierradorada.sdbrewpi.production;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RecipeRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String name,
        @DecimalMin("0.8000") @DecimalMax("1.5000") double originalGravity,
        @DecimalMin("0.8000") @DecimalMax("1.5000") double targetFinalGravity,
        @DecimalMin("1.0") @DecimalMax("10000.0") double defaultVolumeL,
        @Size(max = 1000) String notes,
        @NotEmpty @Size(max = 20) List<@NotNull @Valid ProfileStepRequest> steps) {}
