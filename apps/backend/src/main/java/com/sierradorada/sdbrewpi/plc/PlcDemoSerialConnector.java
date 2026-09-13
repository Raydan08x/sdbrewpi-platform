package com.sierradorada.sdbrewpi.plc;

import com.fazecast.jSerialComm.SerialPort;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class PlcDemoSerialConnector implements SmartLifecycle {
    private static final int MAX_LINE_LENGTH = 512;
    private final PlcDemoProperties properties;
    private final PlcDemoIngestionService ingestion;
    private final PlcDemoRuntimeStatus status;
    private volatile boolean running;
    private volatile SerialPort activePort;
    private Thread worker;

    public PlcDemoSerialConnector(PlcDemoProperties properties, PlcDemoIngestionService ingestion,
            PlcDemoRuntimeStatus status) {
        this.properties = properties;
        this.ingestion = ingestion;
        this.status = status;
    }

    @Override
    public void start() {
        if (!properties.enabled() || running) return;
        running = true;
        worker = Thread.ofPlatform().daemon().name("plc-demo-serial-reader").start(this::readLoop);
    }

    private void readLoop() {
        while (running) {
            SerialPort port = null;
            try {
                validateConfiguration();
                port = SerialPort.getCommPort(properties.port());
                port.setComPortParameters(properties.baudRate(), 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
                port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
                if (!port.openPort()) throw new IllegalStateException("No se pudo abrir " + properties.port());
                activePort = port;
                port.clearDTR();
                port.clearRTS();
                status.connected();
                readLines(port);
            } catch (Exception exception) {
                status.disconnected(safeMessage(exception));
            } finally {
                activePort = null;
                if (port != null && port.isOpen()) port.closePort();
            }
            pauseBeforeRetry();
        }
    }

    private void readLines(SerialPort port) {
        StringBuilder line = new StringBuilder();
        byte[] buffer = new byte[256];
        while (running) {
            int count = port.readBytes(buffer, buffer.length);
            if (count < 0) throw new IllegalStateException("Error leyendo " + properties.port());
            for (int index = 0; index < count; index++) {
                int next = buffer[index] & 0xff;
                if (next == '\n') {
                    process(line.toString());
                    line.setLength(0);
                } else if (next != '\r') {
                    if (line.length() >= MAX_LINE_LENGTH) {
                        line.setLength(0);
                        status.rejected();
                    } else {
                        line.append((char) next);
                    }
                }
            }
        }
    }

    private void process(String line) {
        if (line.isBlank() || !line.startsWith("[DEMO]")) return;
        try {
            ingestion.ingest(line, Instant.now());
        } catch (IllegalArgumentException exception) {
            status.rejected();
        }
    }

    private void validateConfiguration() {
        if (properties.port() == null || properties.port().isBlank()) {
            throw new IllegalStateException("Puerto serie de demo no configurado");
        }
        if (properties.baudRate() < 1200 || properties.baudRate() > 3_000_000) {
            throw new IllegalStateException("Velocidad serie de demo fuera de rango");
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "PLC demo serie desconectado";
        return message.length() > 160 ? message.substring(0, 160) : message;
    }

    private void pauseBeforeRetry() {
        if (!running) return;
        try { Thread.sleep(2000); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }

    @Override
    @PreDestroy
    public void stop() {
        running = false;
        SerialPort port = activePort;
        if (port != null && port.isOpen()) port.closePort();
        if (worker != null) worker.interrupt();
        status.disconnected("Conector de demo PLC detenido");
    }

    @Override public boolean isRunning() { return running; }
    @Override public boolean isAutoStartup() { return true; }
    @Override public int getPhase() { return Integer.MAX_VALUE; }
    @Override public void stop(Runnable callback) { stop(); callback.run(); }
}
