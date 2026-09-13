package com.sierradorada.sdbrewpi.plant;

import java.time.Instant;
import java.util.List;

public record PlantWarehouseView(
        String id,
        String siteId,
        String code,
        String name,
        String purpose,
        boolean temperatureControlled,
        String notes,
        List<String> allowedCategories,
        List<PlantStorageLocationView> locations,
        long revision,
        boolean active,
        Instant updatedAt) {}
