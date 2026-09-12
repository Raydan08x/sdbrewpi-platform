package com.sierradorada.sdbrewpi.telemetry;

import java.time.Instant;

public record TelemetryRecordView(
    String messageId,
    String pillId,
    String sourceFormat,
    Long sequence,
    Instant capturedAt,
    Instant receivedAt,
    Double temperatureC,
    Double gravity,
    Integer batteryPct,
    Integer rssiDbm,
    String quality,
    boolean historical,
    boolean retained
) {}
