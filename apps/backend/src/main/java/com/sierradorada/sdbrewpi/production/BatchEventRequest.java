package com.sierradorada.sdbrewpi.production;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BatchEventRequest(
    @NotBlank(message = "El tipo de evento es obligatorio") String eventType,
    @Min(value = 1, message = "La fase debe ser mayor que cero")
    @Max(value = 999, message = "La fase no es válida") Integer stepOrder,
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 300, message = "La descripción debe tener máximo 300 caracteres") String message,
    @Size(max = 120, message = "El material debe tener máximo 120 caracteres") String materialName,
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero") Double quantity,
    @Size(max = 20, message = "La unidad debe tener máximo 20 caracteres") String unit
) {}
