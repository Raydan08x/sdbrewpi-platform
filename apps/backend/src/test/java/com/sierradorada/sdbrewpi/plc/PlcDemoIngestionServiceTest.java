package com.sierradorada.sdbrewpi.plc;

import static org.assertj.core.api.Assertions.assertThat;

import com.sierradorada.sdbrewpi.fermentation.FermentationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PlcDemoIngestionServiceTest {
    @Autowired PlcDemoIngestionService ingestion;
    @Autowired FermentationService fermentation;

    @Test
    void appliesDemoTemperaturesAndPersistsHistoryWithoutEnablingHardware() {
        Instant now = Instant.now();
        ingestion.ingest("[DEMO] F1=17.8 F2=19.6 | AlarmaCalor=0 AlarmaFrio=0 | Chiller=OFF Bomba=OFF", now);

        assertThat(fermentation.tank("TANK-01").productTemperatureC()).isEqualTo(17.8);
        assertThat(fermentation.tank("TANK-02").productTemperatureC()).isEqualTo(19.6);
        assertThat(fermentation.tank("TANK-01").pillSource()).isEqualTo("PLC_DEMO_SERIAL");
        assertThat(fermentation.history("TANK-01", 1, 20).samples()).isNotEmpty();
        assertThat(fermentation.overview().chiller().hardwareEnabled()).isFalse();
    }

    @Test
    void opensAndClearsReportedDemoAlarm() {
        Instant now = Instant.now();
        ingestion.ingest("[DEMO] F1=24.0 F2=19.6 | AlarmaCalor=1 AlarmaFrio=0 | Chiller=ON Bomba=ON", now);
        assertThat(fermentation.overview().alarms()).anyMatch(alarm -> "PLC_TEMPERATURE_HIGH".equals(alarm.code()));

        ingestion.ingest("[DEMO] F1=18.0 F2=19.6 | AlarmaCalor=0 AlarmaFrio=0 | Chiller=OFF Bomba=OFF", now.plusSeconds(2));
        assertThat(fermentation.overview().alarms()).noneMatch(alarm -> "PLC_TEMPERATURE_HIGH".equals(alarm.code()));
    }
}
