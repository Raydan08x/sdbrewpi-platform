package com.sierradorada.sdbrewpi.plant;

import java.util.List;

public record DeviceOnboardingView(
        String status,
        boolean scannerEnabled,
        boolean hardwareOutputsEnabled,
        String detail,
        List<String> supportedFirmwareProfiles,
        List<String> requiredSteps) {}
