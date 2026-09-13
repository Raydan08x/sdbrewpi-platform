package com.sierradorada.sdbrewpi.production;

public record ProfileStepView(
        int order,
        String name,
        double targetTemperatureC,
        int durationHours,
        Double rampRateCPerHour) {}
