package com.sierradorada.sdbrewpi.production;

import java.time.Instant;

public record BatchStageView(
        String code,
        String phase,
        int order,
        String name,
        String description,
        boolean optional,
        String variant,
        String status,
        Instant startedAt,
        String startedBy,
        Instant completedAt,
        String completedBy,
        String notes,
        Double measuredValue,
        String unit,
        long revision) {}
