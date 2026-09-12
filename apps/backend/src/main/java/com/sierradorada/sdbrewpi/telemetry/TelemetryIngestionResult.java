package com.sierradorada.sdbrewpi.telemetry;

public record TelemetryIngestionResult(boolean accepted, boolean duplicate, boolean liveStateUpdated, String reason) {}
