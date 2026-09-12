package com.sierradorada.sdbrewpi.fermentation;

import java.time.Instant;

public record CommandResult(String commandId, String status, String reason, Instant acceptedAt, TankView state) {}

