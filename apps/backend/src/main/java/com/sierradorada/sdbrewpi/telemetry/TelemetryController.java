package com.sierradorada.sdbrewpi.telemetry;

import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {
    private final TelemetryRuntimeStatus status;
    private final TelemetryRepository repository;

    public TelemetryController(TelemetryRuntimeStatus status, TelemetryRepository repository) {
        this.status = status;
        this.repository = repository;
    }

    @GetMapping("/status")
    ResponseEntity<TelemetryStatusView> status() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(status.view());
    }

    @GetMapping("/pills/{pillId}")
    List<TelemetryRecordView> recent(@PathVariable String pillId,
            @RequestParam(defaultValue = "50") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return repository.recent(pillId.toUpperCase(), safeLimit);
    }
}
