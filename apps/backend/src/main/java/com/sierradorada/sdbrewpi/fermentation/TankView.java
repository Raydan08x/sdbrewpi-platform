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
    long pillAgeSeconds,
    boolean coolingDemand,
    long revision
) {}

