package com.sierradorada.sdbrewpi.fermentation;

import java.time.Instant;
import java.util.List;

public record AlarmHistoryView(
    Instant from,
    Instant generatedAt,
    List<AlarmView> alarms
) {}
