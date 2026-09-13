package com.sierradorada.sdbrewpi.plant;

public record PlantAssetView(
        String id,
        String siteId,
        String code,
        String assetType,
        String name,
        String manufacturer,
        String model,
        Double capacityL,
        String electricalSpec,
        String communicationProtocol,
        String deviceIdentifier,
        String firmwareProfile,
        String status,
        boolean controllable,
        String notes,
        long revision) {}
