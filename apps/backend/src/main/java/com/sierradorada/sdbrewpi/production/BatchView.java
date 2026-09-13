package com.sierradorada.sdbrewpi.production;

import java.time.Instant;
import java.util.List;

public record BatchView(
        String id,
        String code,
        String recipeVersionId,
        String recipeCode,
        String recipeName,
        int recipeVersion,
        String tankId,
        double volumeL,
        String status,
        int currentStep,
        Instant startedAt,
        Instant expectedCompleteAt,
        Instant completedAt,
        long revision,
        List<ProfileStepView> profile) {}
