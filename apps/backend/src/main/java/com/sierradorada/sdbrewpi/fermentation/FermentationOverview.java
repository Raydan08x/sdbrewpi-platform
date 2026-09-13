package com.sierradorada.sdbrewpi.fermentation;

import java.time.Instant;
import java.util.List;
import com.sierradorada.sdbrewpi.plc.PlcDemoStatusView;
import com.sierradorada.sdbrewpi.telemetry.TelemetryStatusView;

public record FermentationOverview(String environment, Instant generatedAt, List<TankView> tanks, ChillerView chiller,
    TelemetryStatusView telemetry, PlcDemoStatusView plcDemo, List<AlarmView> alarms) {}
