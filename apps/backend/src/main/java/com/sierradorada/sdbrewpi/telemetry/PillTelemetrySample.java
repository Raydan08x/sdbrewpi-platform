package com.sierradorada.sdbrewpi.telemetry;

import java.time.Duration;
import java.time.Instant;

public record PillTelemetrySample(
    String messageId,
    String pillId,
    String sourceTopic,
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
    boolean retained,
    String rawPayload
) {
    public boolean eligibleForLiveState(Instant now, Duration freshness) {
        return !historical && !retained && "GOOD".equals(quality)
            && temperatureC != null && gravity != null
            && !capturedAt.isAfter(now.plusSeconds(5))
            && !capturedAt.isBefore(now.minus(freshness));
    }
}
