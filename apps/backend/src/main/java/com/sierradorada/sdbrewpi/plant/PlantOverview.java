package com.sierradorada.sdbrewpi.plant;

import java.time.Instant;
import java.util.List;

public record PlantOverview(
        Instant generatedAt,
        PlantProfileView site,
        List<PlantAssetView> assets,
        List<PlantWarehouseView> warehouses,
        DeviceOnboardingView onboarding,
        int assetsNeedingData) {}
