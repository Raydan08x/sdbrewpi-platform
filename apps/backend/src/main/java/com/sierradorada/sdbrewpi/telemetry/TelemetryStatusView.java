package com.sierradorada.sdbrewpi.telemetry;

import java.time.Instant;

public record TelemetryStatusView(
    boolean enabled,
    boolean connected,
    Instant lastMessageAt,
    long acceptedMessages,
    long rejectedMessages,
    String detail
) {}
