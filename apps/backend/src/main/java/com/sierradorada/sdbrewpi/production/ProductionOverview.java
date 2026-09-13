package com.sierradorada.sdbrewpi.production;

import java.time.Instant;
import java.util.List;

public record ProductionOverview(
        Instant generatedAt,
        List<RecipeView> recipes,
        List<BatchView> activeBatches,
        List<ProductionStageView> processStages) {}
