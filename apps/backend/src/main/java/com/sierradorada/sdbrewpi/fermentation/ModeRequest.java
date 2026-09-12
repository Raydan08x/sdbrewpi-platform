package com.sierradorada.sdbrewpi.fermentation;

import jakarta.validation.constraints.NotNull;

public record ModeRequest(@NotNull(message = "El modo es obligatorio") ControlMode mode, long expectedRevision) {}

