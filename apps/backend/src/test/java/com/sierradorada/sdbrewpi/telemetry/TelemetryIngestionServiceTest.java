package com.sierradorada.sdbrewpi.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sierradorada.sdbrewpi.fermentation.FermentationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TelemetryIngestionServiceTest {
    @Autowired TelemetryIngestionService ingestion;
    @Autowired TelemetryRepository repository;
    @Autowired FermentationService fermentation;
    @Autowired MockMvc mvc;

    @Test
    void persistsFreshTelemetryAndUpdatesAssociatedTank() {
        Instant now = Instant.now();
        String payload = "{\"messageId\":\"integration-fresh\",\"capturedAt\":\"" + now.minusSeconds(2)
            + "\",\"temperature\":17.25,\"sg\":1.033,\"battery\":68,\"rssi\":-64}";

        TelemetryIngestionResult result = ingestion.ingest("sierra/rapt/amarilla/telemetry", payload, false, now);

        assertThat(result.accepted()).isTrue();
        assertThat(result.liveStateUpdated()).isTrue();
        assertThat(repository.recent("PILL-4D4C", 10)).hasSize(1);
        assertThat(fermentation.tank("TANK-01").productTemperatureC()).isEqualTo(17.25);
        assertThat(fermentation.tank("TANK-01").pillBatteryPct()).isEqualTo(68);
    }

    @Test
    void persistsHistoryWithoutReplacingCurrentTankValue() {
        double before = fermentation.tank("TANK-02").productTemperatureC();
        TelemetryIngestionResult result = ingestion.ingest("rapt/pill/3ba4/history",
            "{\"ts\":1700000000,\"sg\":1.010,\"temp\":2.0,\"bat\":50,\"rssi\":-80,\"pill\":\"3ba4\"}", false, Instant.now());

        assertThat(result.accepted()).isTrue();
        assertThat(result.liveStateUpdated()).isFalse();
        assertThat(fermentation.tank("TANK-02").productTemperatureC()).isEqualTo(before);
    }

    @Test
    void recordsInvalidPayloadWithoutExposingIt() {
        int before = repository.rejectionCount();
        TelemetryIngestionResult result = ingestion.ingest("rapt/pill/4d4c/gravity", "incorrecto", true, Instant.now());

        assertThat(result.accepted()).isFalse();
        assertThat(result.reason()).contains("no numérico");
        assertThat(repository.rejectionCount()).isEqualTo(before + 1);
    }

    @Test
    void exposesConnectorStatusAndBoundedHistory() throws Exception {
        mvc.perform(get("/api/v1/telemetry/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false))
            .andExpect(jsonPath("$.connected").value(false));

        mvc.perform(get("/api/v1/telemetry/pills/PILL-4D4C").param("limit", "5000"))
            .andExpect(status().isOk());
    }
}
