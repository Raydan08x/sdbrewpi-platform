package com.sierradorada.sdbrewpi.production;

public record ProductionStageView(
        String code,
        String phase,
        int order,
        String name,
        String description,
        boolean optional,
        String variant) {}
