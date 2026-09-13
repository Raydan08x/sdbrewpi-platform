package com.sierradorada.sdbrewpi.plc;

import java.time.Instant;

public record PlcDemoSample(
    double fermenter1TemperatureC,
    double fermenter2TemperatureC,
    boolean highTemperatureAlarm,
    boolean lowTemperatureAlarm,
    Boolean chillerReportedOn,
    Boolean pumpReportedOn,
    Instant capturedAt
) {}
