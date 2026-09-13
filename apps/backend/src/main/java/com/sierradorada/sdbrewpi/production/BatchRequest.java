package com.sierradorada.sdbrewpi.production;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BatchRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank String recipeVersionId,
        @NotBlank String tankId,
        @DecimalMin("1.0") @DecimalMax("10000.0") double volumeL) {}
