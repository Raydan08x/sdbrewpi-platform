package com.sierradorada.sdbrewpi.telemetry;

import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryIngestionService {
    private final MqttTelemetryParser parser;
    private final TelemetryRepository repository;
    private final TelemetryRuntimeStatus status;
    private final MqttProperties properties;

    public TelemetryIngestionService(MqttTelemetryParser parser, TelemetryRepository repository,
            TelemetryRuntimeStatus status, MqttProperties properties) {
        this.parser = parser;
        this.repository = repository;
        this.status = status;
        this.properties = properties;
    }

    @Transactional
    public TelemetryIngestionResult ingest(String topic, String payload, boolean retained, Instant receivedAt) {
        try {
            PillTelemetrySample sample = parser.parse(topic, payload, retained, receivedAt);
            boolean inserted = repository.insert(sample);
            if (!inserted) return new TelemetryIngestionResult(false, true, false, "Mensaje duplicado");
            boolean eligible = sample.eligibleForLiveState(receivedAt, Duration.ofSeconds(properties.freshnessSeconds()));
            boolean updated = eligible && repository.updateCurrentTank(sample) > 0;
            status.accepted(receivedAt);
            return new TelemetryIngestionResult(true, false, updated,
                eligible ? updated ? "Telemetría actual aplicada" : "Pill sin tanque asociado o muestra anterior" : "Telemetría almacenada solo como histórico");
        } catch (IllegalArgumentException exception) {
            repository.reject(topic, exception.getMessage(), receivedAt);
            status.rejected(receivedAt);
            return new TelemetryIngestionResult(false, false, false, exception.getMessage());
        }
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void markStaleTelemetry() {
        if (!properties.enabled()) return;
        repository.markStale(Instant.now().minusSeconds(properties.freshnessSeconds()));
    }
}
