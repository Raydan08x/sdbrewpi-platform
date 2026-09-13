package com.sierradorada.sdbrewpi.plant;

import jakarta.validation.constraints.Min;

public record PlantAssetRetireRequest(@Min(0) long expectedRevision) {}
