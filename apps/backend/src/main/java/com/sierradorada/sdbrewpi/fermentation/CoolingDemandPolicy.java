package com.sierradorada.sdbrewpi.fermentation;

final class CoolingDemandPolicy {
    private CoolingDemandPolicy() {}

    static boolean decide(ControlMode mode, double temperatureC, double setpointC, double hysteresisC, boolean previousDemand) {
        if (mode != ControlMode.AUTO) return false;
        if (temperatureC > setpointC + hysteresisC) return true;
        if (temperatureC < setpointC - hysteresisC) return false;
        return previousDemand;
    }
}

