package com.sierradorada.sdbrewpi.production;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StageCommandRequest(
    @NotBlank @Pattern(regexp = "START|COMPLETE|SKIP") String action,
    @Min(0) long expectedBatchRevision,
    @Min(0) long expectedStageRevision,
    @Size(max = 1000) String notes,
    @DecimalMin("-1000000.0") @DecimalMax("1000000.0") Double measuredValue,
    @Size(max = 20) String unit
) {}
