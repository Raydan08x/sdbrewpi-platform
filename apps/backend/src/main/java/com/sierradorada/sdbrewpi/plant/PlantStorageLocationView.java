package com.sierradorada.sdbrewpi.plant;

import java.time.Instant;

public record PlantStorageLocationView(
        String id,
        String warehouseId,
        String code,
        String name,
        String locationType,
        String notes,
        long revision,
        boolean active,
        Instant updatedAt) {}
