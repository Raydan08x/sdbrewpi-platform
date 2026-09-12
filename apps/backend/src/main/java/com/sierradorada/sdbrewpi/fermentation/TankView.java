package com.sierradorada.sdbrewpi.fermentation;

import java.time.Instant;

public record TankView(
    String id,
    String name,
    ControlMode mode,
    double setpointC,
    double productTemperatureC,
    double gravity,
    String pillId,
    String pillQuality,
    Instant pillCapturedAt,
    Instant pillReceivedAt,
    long pillAgeSeconds,
    Integer pillBatteryPct,
    Integer pillRssiDbm,
    String pillSource,
    boolean coolingDemand,
    long revision
) {}
