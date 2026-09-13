package com.sierradorada.sdbrewpi.fermentation;

import java.time.Instant;

public record FermentationMeasurementView(
    Instant capturedAt,
    Instant receivedAt,
    Double temperatureC,
    Double gravity,
    String quality,
    String source
) {}
