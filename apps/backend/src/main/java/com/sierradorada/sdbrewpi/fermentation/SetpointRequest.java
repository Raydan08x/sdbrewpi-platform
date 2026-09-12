package com.sierradorada.sdbrewpi.fermentation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SetpointRequest(
    @NotNull(message = "El setpoint es obligatorio")
    @DecimalMin(value = "0.0", message = "El setpoint mínimo es 0 °C")
    @DecimalMax(value = "35.0", message = "El setpoint máximo es 35 °C")
    Double setpointC,
    long expectedRevision
) {}

