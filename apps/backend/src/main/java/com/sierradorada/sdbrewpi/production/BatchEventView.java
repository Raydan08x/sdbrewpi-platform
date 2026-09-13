package com.sierradorada.sdbrewpi.production;

import java.time.Instant;

public record BatchEventView(
    String id,
    Instant occurredAt,
    String eventType,
    Integer stepOrder,
    String actor,
    String message,
    String materialName,
    Double quantity,
    String unit
) {}
