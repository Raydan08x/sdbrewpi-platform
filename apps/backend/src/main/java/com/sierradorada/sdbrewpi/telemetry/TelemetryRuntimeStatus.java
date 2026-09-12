package com.sierradorada.sdbrewpi.telemetry;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class TelemetryRuntimeStatus {
    private final boolean enabled;
    private final AtomicBoolean connected = new AtomicBoolean();
    private final AtomicReference<Instant> lastMessageAt = new AtomicReference<>();
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicReference<String> detail;

    public TelemetryRuntimeStatus(MqttProperties properties) {
        this.enabled = properties.enabled();
        this.detail = new AtomicReference<>(enabled ? "Esperando conexión MQTT" : "MQTT deshabilitado");
    }

    public void connected() { connected.set(true); detail.set("Suscripción MQTT activa"); }
    public void disconnected(String reason) { connected.set(false); detail.set(reason); }
    public void accepted(Instant at) { lastMessageAt.set(at); accepted.incrementAndGet(); }
    public void rejected(Instant at) { lastMessageAt.set(at); rejected.incrementAndGet(); }

    public TelemetryStatusView view() {
        return new TelemetryStatusView(enabled, connected.get(), lastMessageAt.get(), accepted.get(), rejected.get(), detail.get());
    }
}
