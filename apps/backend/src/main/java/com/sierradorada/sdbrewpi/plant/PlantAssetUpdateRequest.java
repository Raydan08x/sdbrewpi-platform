package com.sierradorada.sdbrewpi.plant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlantAssetUpdateRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 40) String assetType,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 100) String manufacturer,
        @Size(max = 140) String model,
        @DecimalMin("0.0") @DecimalMax("10000000.0") Double capacityL,
        @Size(max = 300) String electricalSpec,
        @Size(max = 160) String communicationProtocol,
        @Size(max = 160) String deviceIdentifier,
        @Size(max = 120) String firmwareProfile,
        @NotBlank @Size(max = 30) String status,
        boolean controllable,
        @Size(max = 1000) String notes,
        @Min(0) long expectedRevision) {}
