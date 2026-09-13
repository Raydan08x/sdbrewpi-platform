package com.sierradorada.sdbrewpi.fermentation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AlarmAcknowledgementRequest(
    @Min(value = 0, message = "La revisión esperada no puede ser negativa") long expectedRevision,
    @Size(max = 300, message = "La nota debe tener máximo 300 caracteres") String note
) {}
