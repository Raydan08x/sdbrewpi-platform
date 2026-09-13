package com.sierradorada.sdbrewpi.plc;

import com.sierradorada.sdbrewpi.fermentation.FermentationAlarmService;
import com.sierradorada.sdbrewpi.fermentation.FermentationRepository;
import com.sierradorada.sdbrewpi.fermentation.TankView;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlcDemoIngestionService {
    private final PlcDemoParser parser;
    private final PlcDemoProperties properties;
    private final PlcDemoRuntimeStatus status;
    private final FermentationRepository repository;
    private final FermentationAlarmService alarmService;

    public PlcDemoIngestionService(PlcDemoParser parser, PlcDemoProperties properties, PlcDemoRuntimeStatus status,
            FermentationRepository repository, FermentationAlarmService alarmService) {
        this.parser = parser;
        this.properties = properties;
        this.status = status;
        this.repository = repository;
        this.alarmService = alarmService;
    }

    @Transactional
    public PlcDemoSample ingest(String line, Instant receivedAt) {
        PlcDemoSample sample = parser.parse(line, receivedAt);
        updateTank("TANK-01", sample.fermenter1TemperatureC(), receivedAt);
        updateTank("TANK-02", sample.fermenter2TemperatureC(), receivedAt);
        if (sample.pumpReportedOn() != null && sample.chillerReportedOn() != null) {
            repository.updateChillerFromPlcDemo(sample.pumpReportedOn(), sample.chillerReportedOn());
        }
        alarmService.reconcileDemoFlags(sample.highTemperatureAlarm(), sample.lowTemperatureAlarm(), receivedAt);
        status.accepted(receivedAt);
        return sample;
    }

    private void updateTank(String tankId, double temperature, Instant receivedAt) {
        repository.updateFromPlcDemo(tankId, temperature, receivedAt);
        TankView tank = repository.findTank(tankId).orElseThrow();
        repository.recordMeasurementIfDue(tankId, "PLC_DEMO_SERIAL", receivedAt, temperature, tank.gravity(), "GOOD",
            receivedAt.minusSeconds(Math.max(1, properties.historyIntervalSeconds())));
    }
}
