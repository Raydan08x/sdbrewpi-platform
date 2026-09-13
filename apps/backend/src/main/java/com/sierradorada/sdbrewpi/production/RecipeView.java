package com.sierradorada.sdbrewpi.production;

import java.time.Instant;
import java.util.List;

public record RecipeView(
        String id,
        String code,
        String name,
        int version,
        double originalGravity,
        double targetFinalGravity,
        double defaultVolumeL,
        String notes,
        Instant createdAt,
        List<ProfileStepView> steps) {}
