package com.sierradorada.sdbrewpi.fermentation;

public record ChillerView(
    String mode,
    Double reservoirTemperatureC,
    String reservoirQuality,
    boolean pumpOn,
    boolean compressorRequest,
    String environment,
    boolean hardwareEnabled,
    long revision
) {}

