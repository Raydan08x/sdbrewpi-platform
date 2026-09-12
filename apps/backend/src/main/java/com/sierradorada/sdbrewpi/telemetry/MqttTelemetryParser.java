package com.sierradorada.sdbrewpi.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class MqttTelemetryParser {
    private static final int MAX_PAYLOAD_LENGTH = 2000;
    private final ObjectMapper objectMapper;

    public MqttTelemetryParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PillTelemetrySample parse(String topic, String payload, boolean retained, Instant receivedAt) {
        if (topic == null || topic.isBlank() || topic.length() > 200) throw new IllegalArgumentException("Tópico MQTT inválido");
        if (payload == null || payload.isBlank() || payload.length() > MAX_PAYLOAD_LENGTH) throw new IllegalArgumentException("Payload MQTT vacío o demasiado grande");
        if (topic.endsWith("/history") || topic.endsWith("/telemetry")) return parseJson(topic, payload, retained, receivedAt);
        return parseScalar(topic, payload, retained, receivedAt);
    }

    private PillTelemetrySample parseJson(String topic, String payload, boolean retained, Instant receivedAt) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            boolean historical = topic.endsWith("/history");
            boolean canonical = topic.startsWith("sdbrewpi/v1/");
            JsonNode metrics = root.path("metrics");
            Instant capturedAt = parseTimestamp(first(root, "capturedAt", "timestamp", "ts"));
            boolean verifiedTime = capturedAt != null;
            if (!verifiedTime) capturedAt = receivedAt;
            String pillId = normalizePillId(text(first(root, "pillId", "pill")), topic);
            Double temperature = number(first(metrics, "temperatureC", "temperature", "temp"));
            if (temperature == null) temperature = number(first(root, "temperatureC", "temperature", "temp"));
            Double gravity = number(first(metrics, "gravity", "sg"));
            if (gravity == null) gravity = number(first(root, "gravity", "sg"));
            Integer battery = integer(first(metrics, "batteryPct", "battery", "bat"));
            if (battery == null) battery = integer(first(root, "batteryPct", "battery", "bat"));
            Integer rssi = integer(first(metrics, "rssiDbm", "rssi"));
            if (rssi == null) rssi = integer(first(root, "rssiDbm", "rssi"));
            Long sequence = longValue(root.get("sequence"));
            validate(temperature, gravity, battery, rssi);
            String quality = historical ? "HISTORICAL" : verifiedTime ? quality(root) : "UNVERIFIED_TIME";
            String messageId = messageId(text(root.get("messageId")), topic + "\n" + capturedAt + "\n" + payload);
            return new PillTelemetrySample(messageId, pillId, topic, canonical ? "CANONICAL_V1" : historical ? "LEGACY_HISTORY" : "LEGACY_JSON", sequence,
                capturedAt, receivedAt, temperature, gravity, battery, rssi, quality, historical, retained, payload);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("JSON MQTT inválido");
        }
    }

    private PillTelemetrySample parseScalar(String topic, String payload, boolean retained, Instant receivedAt) {
        String metric = topic.substring(topic.lastIndexOf('/') + 1);
        double value;
        try { value = Double.parseDouble(payload.trim()); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Valor MQTT no numérico"); }
        Double temperature = "temperature".equals(metric) ? value : null;
        Double gravity = "gravity".equals(metric) ? value : null;
        Integer battery = "battery".equals(metric) ? (int) Math.round(value) : null;
        Integer rssi = "rssi".equals(metric) ? (int) Math.round(value) : null;
        if (temperature == null && gravity == null && battery == null && rssi == null) throw new IllegalArgumentException("Métrica MQTT no soportada");
        validate(temperature, gravity, battery, rssi);
        String identity = retained ? topic + "\n" + payload : topic + "\n" + receivedAt + "\n" + payload;
        return new PillTelemetrySample(digest(identity), normalizePillId(null, topic), topic, "LEGACY_SCALAR", null,
            receivedAt, receivedAt, temperature, gravity, battery, rssi, "UNVERIFIED_TIME", false, retained, payload);
    }

    private JsonNode first(JsonNode node, String... names) {
        if (node == null || node.isMissingNode()) return null;
        for (String name : names) if (node.hasNonNull(name)) return node.get(name);
        return null;
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private Double number(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isNumber()) return node.doubleValue();
        try { return Double.valueOf(node.asText()); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Métrica MQTT no numérica"); }
    }

    private Integer integer(JsonNode node) {
        Double value = number(node);
        return value == null ? null : (int) Math.round(value);
    }

    private Long longValue(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (!node.canConvertToLong()) throw new IllegalArgumentException("Secuencia MQTT inválida");
        long value = node.longValue();
        if (value < 0) throw new IllegalArgumentException("Secuencia MQTT inválida");
        return value;
    }

    private Instant parseTimestamp(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isNumber()) {
            long value = node.longValue();
            return value > 100_000_000_000L ? Instant.ofEpochMilli(value) : Instant.ofEpochSecond(value);
        }
        String value = node.asText();
        if (value.isBlank()) return null;
        try { return Instant.parse(value); }
        catch (DateTimeParseException exception) { throw new IllegalArgumentException("Timestamp MQTT inválido"); }
    }

    private String quality(JsonNode root) {
        JsonNode node = root.get("quality");
        if (node == null || node.isNull()) return "GOOD";
        if (node.isTextual()) return normalizeQuality(node.asText());
        JsonNode status = first(node, "status", "overall");
        return status == null ? "GOOD" : normalizeQuality(status.asText());
    }

    private String normalizePillId(String candidate, String topic) {
        String source = ((candidate == null ? "" : candidate) + " " + topic).toLowerCase();
        if (source.contains("4d4c") || source.contains("4d4e") || source.contains("amarilla")) return "PILL-4D4C";
        if (source.contains("3ba4") || source.contains("3ba6") || source.contains("roja")) return "PILL-3BA4";
        if (candidate != null && !candidate.isBlank() && candidate.length() <= 64) return candidate.trim().toUpperCase();
        throw new IllegalArgumentException("Pill MQTT desconocida");
    }

    private void validate(Double temperature, Double gravity, Integer battery, Integer rssi) {
        if (temperature != null && (!Double.isFinite(temperature) || temperature < -10 || temperature > 50)) throw new IllegalArgumentException("Temperatura MQTT fuera de rango");
        if (gravity != null && (!Double.isFinite(gravity) || gravity < 0.8 || gravity > 1.5)) throw new IllegalArgumentException("Gravedad MQTT fuera de rango");
        if (battery != null && (battery < 0 || battery > 100)) throw new IllegalArgumentException("Batería MQTT fuera de rango");
        if (rssi != null && (rssi < -127 || rssi > 0)) throw new IllegalArgumentException("RSSI MQTT fuera de rango");
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo identificar la telemetría", exception);
        }
    }

    private String messageId(String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) return digest(fallback);
        String value = candidate.trim();
        return value.matches("[A-Za-z0-9._:-]{1,64}") ? value : digest(value);
    }

    private String normalizeQuality(String value) {
        String normalized = value == null ? "UNKNOWN" : value.trim().toUpperCase();
        return switch (normalized) {
            case "GOOD", "DEGRADED", "BAD", "STALE" -> normalized;
            default -> "UNKNOWN";
        };
    }
}
