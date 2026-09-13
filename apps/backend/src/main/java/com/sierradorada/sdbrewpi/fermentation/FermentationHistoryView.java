package com.sierradorada.sdbrewpi.fermentation;

import java.time.Instant;
import java.util.List;

public record FermentationHistoryView(
    String tankId,
    Instant from,
    Instant generatedAt,
    List<FermentationMeasurementView> samples
) {}
