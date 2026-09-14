package com.sierradorada.sdbrewpi.production;

import jakarta.validation.constraints.Min;

public record BatchTransitionRequest(@Min(0) long expectedRevision) {}
