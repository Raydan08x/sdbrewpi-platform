package com.sierradorada.sdbrewpi.production;

import jakarta.validation.constraints.Min;

public record ProfileCommandRequest(@Min(0) long expectedRevision) {}
