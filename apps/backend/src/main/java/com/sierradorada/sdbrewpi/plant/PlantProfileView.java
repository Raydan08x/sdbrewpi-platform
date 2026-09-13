package com.sierradorada.sdbrewpi.plant;

import java.time.Instant;

public record PlantProfileView(
        String id,
        String code,
        String name,
        String companyName,
        String legalName,
        String taxId,
        String timezone,
        String currency,
        Double nominalBatchCapacityL,
        int plannedFermenters,
        double pipingDeadVolumeL,
        String logoUrl,
        long revision,
        Instant updatedAt) {}
