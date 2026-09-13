package com.sierradorada.sdbrewpi.plc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlcDemoParserTest {
    private final PlcDemoParser parser = new PlcDemoParser();

    @Test
    void parsesObservedWaveshareDemoLine() {
        Instant now = Instant.parse("2026-09-13T05:00:00Z");
        PlcDemoSample sample = parser.parse(
            "[DEMO] F1=17.8 F2=19.6 | AlarmaCalor=0 AlarmaFrio=1 | Chiller=OFF Bomba=ON", now);

        assertThat(sample.fermenter1TemperatureC()).isEqualTo(17.8);
        assertThat(sample.fermenter2TemperatureC()).isEqualTo(19.6);
        assertThat(sample.lowTemperatureAlarm()).isTrue();
        assertThat(sample.highTemperatureAlarm()).isFalse();
        assertThat(sample.pumpReportedOn()).isTrue();
        assertThat(sample.chillerReportedOn()).isFalse();
        assertThat(sample.capturedAt()).isEqualTo(now);
    }

    @Test
    void rejectsUnknownSerialText() {
        assertThatThrownBy(() -> parser.parse("[HEARTBEAT] Chiller=0", Instant.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no reconocida");
    }

    @Test
    void acceptsObservedDeltaVariantWithoutInventingOutputState() {
        PlcDemoSample sample = parser.parse(
            "[DEMO] F1=18.0 F2=19.7 | AlarmaCalor=0 AlarmaFrio=0 | Delta=1.5", Instant.now());

        assertThat(sample.chillerReportedOn()).isNull();
        assertThat(sample.pumpReportedOn()).isNull();
    }

    @Test
    void rejectsTemperatureOutsideAcceptedRange() {
        assertThatThrownBy(() -> parser.parse(
            "[DEMO] F1=99.0 F2=19.6 | AlarmaCalor=0 AlarmaFrio=0 | Chiller=OFF Bomba=OFF", Instant.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fuera de rango");
    }
}
