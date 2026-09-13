package com.sierradorada.sdbrewpi.plc;

import java.time.Instant;

public record PlcDemoStatusView(
    boolean enabled,
    boolean connected,
    String port,
    Instant lastMessageAt,
    long acceptedMessages,
    long rejectedLines,
    String detail
) {}
