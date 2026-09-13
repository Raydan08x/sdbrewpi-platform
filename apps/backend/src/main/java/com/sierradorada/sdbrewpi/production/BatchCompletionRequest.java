package com.sierradorada.sdbrewpi.production;

import jakarta.validation.constraints.Min;

public record BatchCompletionRequest(@Min(0) long expectedRevision) {}
