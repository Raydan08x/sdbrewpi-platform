package com.sierradorada.sdbrewpi.plc;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class PlcDemoRuntimeStatus {
    private final PlcDemoProperties properties;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicReference<Instant> lastMessageAt = new AtomicReference<>();
    private final AtomicReference<String> detail = new AtomicReference<>("Conector de demo PLC deshabilitado");

    public PlcDemoRuntimeStatus(PlcDemoProperties properties) { this.properties = properties; }

    public void connected() {
        connected.set(true);
        detail.set("Leyendo telemetría demo por serie; no se envían comandos");
    }

    public void disconnected(String reason) {
        connected.set(false);
        detail.set(reason);
    }

    public void accepted(Instant receivedAt) {
        accepted.incrementAndGet();
        lastMessageAt.set(receivedAt);
    }

    public void rejected() { rejected.incrementAndGet(); }

    public PlcDemoStatusView view() {
        Instant last = lastMessageAt.get();
        boolean portOpen = connected.get();
        boolean fresh = portOpen && last != null
            && !last.isBefore(Instant.now().minusSeconds(Math.max(1, properties.freshnessSeconds())));
        String currentDetail = !properties.enabled() ? "Conector de demo PLC deshabilitado"
            : !portOpen ? detail.get()
            : last == null ? "Puerto serie abierto; esperando la primera muestra"
            : !fresh ? "Puerto serie abierto; la demo dejó de emitir muestras recientes"
            : detail.get();
        return new PlcDemoStatusView(properties.enabled(), fresh, properties.port(), last,
            accepted.get(), rejected.get(), currentDetail);
    }
}
