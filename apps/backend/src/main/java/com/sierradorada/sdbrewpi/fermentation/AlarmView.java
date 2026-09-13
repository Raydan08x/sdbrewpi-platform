package com.sierradorada.sdbrewpi.fermentation;

import java.time.Instant;

public record AlarmView(
    String id,
    String targetId,
    String code,
    String severity,
    String status,
    String message,
    String source,
    Instant openedAt,
    Instant lastSeenAt,
    Instant clearedAt,
    Instant acknowledgedAt,
    String acknowledgedBy,
    String acknowledgmentNote,
    long revision
) {}
