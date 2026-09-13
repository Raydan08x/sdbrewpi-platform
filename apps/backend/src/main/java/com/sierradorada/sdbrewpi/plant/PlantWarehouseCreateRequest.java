package com.sierradorada.sdbrewpi.plant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PlantWarehouseCreateRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String purpose,
        boolean temperatureControlled,
        @NotEmpty @Size(max = 6) List<@NotBlank @Size(max = 40) String> allowedCategories,
        @Size(max = 1000) String notes) {}
