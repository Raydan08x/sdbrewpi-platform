package com.sierradorada.sdbrewpi.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MqttTelemetryParserTest {
    private final MqttTelemetryParser parser = new MqttTelemetryParser(new ObjectMapper());

    @Test
    void parsesTimestampedLegacyTelemetryForYellowPill() {
        Instant received = Instant.parse("2026-09-12T18:00:10Z");
        PillTelemetrySample sample = parser.parse("sierra/rapt/amarilla/telemetry",
            "{\"timestamp\":\"2026-09-12T18:00:00Z\",\"temperature\":18.4,\"sg\":1.042,\"battery\":76,\"rssi\":-61}", false, received);

        assertThat(sample.pillId()).isEqualTo("PILL-4D4C");
        assertThat(sample.sourceFormat()).isEqualTo("LEGACY_JSON");
        assertThat(sample.eligibleForLiveState(received, Duration.ofSeconds(90))).isTrue();
    }

    @Test
    void storesOfflineHistoryButNeverUsesItAsCurrentState() {
        Instant received = Instant.parse("2026-09-12T18:00:10Z");
        PillTelemetrySample sample = parser.parse("rapt/pill/3ba4/history",
            "{\"ts\":1757699900,\"sg\":1.031,\"temp\":17.8,\"bat\":63,\"rssi\":-70,\"pill\":\"3ba4\"}", false, received);

        assertThat(sample.historical()).isTrue();
        assertThat(sample.quality()).isEqualTo("HISTORICAL");
        assertThat(sample.eligibleForLiveState(received, Duration.ofSeconds(90))).isFalse();
    }

    @Test
    void retainedScalarWithoutTimestampCannotDriveControl() {
        Instant received = Instant.parse("2026-09-12T18:00:10Z");
        PillTelemetrySample sample = parser.parse("rapt/pill/4d4e/temperature", "19.25", true, received);

        assertThat(sample.temperatureC()).isEqualTo(19.25);
        assertThat(sample.quality()).isEqualTo("UNVERIFIED_TIME");
        assertThat(sample.eligibleForLiveState(received, Duration.ofSeconds(90))).isFalse();
    }

    @Test
    void parsesCanonicalContractWithNestedMetrics() {
        Instant received = Instant.parse("2026-09-12T18:00:10Z");
        PillTelemetrySample sample = parser.parse("sdbrewpi/v1/main/pill-4d4c/telemetry",
            "{\"messageId\":\"m-42\",\"sequence\":42,\"pillId\":\"PILL-4D4C\",\"capturedAt\":\"2026-09-12T18:00:05Z\",\"quality\":\"GOOD\",\"metrics\":{\"temperatureC\":18.1,\"gravity\":1.038,\"batteryPct\":71,\"rssiDbm\":-58}}", false, received);

        assertThat(sample.messageId()).isEqualTo("m-42");
        assertThat(sample.sequence()).isEqualTo(42);
        assertThat(sample.sourceFormat()).isEqualTo("CANONICAL_V1");
        assertThat(sample.eligibleForLiveState(received, Duration.ofSeconds(90))).isTrue();
    }

    @Test
    void rejectsUnsafeMetricRanges() {
        assertThatThrownBy(() -> parser.parse("rapt/pill/4d4c/gravity", "4.2", true, Instant.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Gravedad");
    }
}
