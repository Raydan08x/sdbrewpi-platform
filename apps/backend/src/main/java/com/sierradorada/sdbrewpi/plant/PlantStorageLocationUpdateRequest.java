package com.sierradorada.sdbrewpi.plant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlantStorageLocationUpdateRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 30) String locationType,
        @Size(max = 500) String notes,
        @Min(0) long expectedRevision) {}
