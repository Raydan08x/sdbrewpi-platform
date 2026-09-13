package com.sierradorada.sdbrewpi.fermentation;

import com.sierradorada.sdbrewpi.plc.PlcDemoProperties;
import com.sierradorada.sdbrewpi.plc.PlcDemoRuntimeStatus;
import com.sierradorada.sdbrewpi.telemetry.MqttProperties;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FermentationAlarmService {
    private static final int LOW_BATTERY_PERCENT = 20;
    private final FermentationRepository repository;
    private final PlcDemoProperties plcProperties;
    private final PlcDemoRuntimeStatus plcStatus;
    private final MqttProperties mqttProperties;

    public FermentationAlarmService(FermentationRepository repository, PlcDemoProperties plcProperties,
            PlcDemoRuntimeStatus plcStatus, MqttProperties mqttProperties) {
        this.repository = repository;
        this.plcProperties = plcProperties;
        this.plcStatus = plcStatus;
        this.mqttProperties = mqttProperties;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void evaluate() {
        Instant now = Instant.now();
        for (TankView tank : repository.findTanks()) {
            boolean fieldSource = !"SIMULATION".equals(tank.pillSource());
            boolean stale = fieldSource && tank.pillReceivedAt() != null
                && tank.pillReceivedAt().isBefore(now.minusSeconds(sourceFreshnessSeconds(tank)));
            repository.reconcileAlarm(tank.id() + ":SENSOR_STALE", tank.id(), "SENSOR_STALE", "CRITICAL",
                "No se reciben datos recientes de " + tank.name(), tank.pillSource(), stale, now);
            boolean lowBattery = tank.pillBatteryPct() != null && tank.pillBatteryPct() <= LOW_BATTERY_PERCENT;
            repository.reconcileAlarm(tank.id() + ":BATTERY_LOW", tank.id(), "BATTERY_LOW", "WARNING",
                "La batería de " + tank.pillId() + " está en " + tank.pillBatteryPct() + "%", tank.pillSource(),
                lowBattery, now);
        }

        boolean plcLost = plcProperties.enabled() && (!plcStatus.view().connected()
            || plcStatus.view().lastMessageAt() == null
            || plcStatus.view().lastMessageAt().isBefore(now.minusSeconds(plcProperties.freshnessSeconds())));
        repository.reconcileAlarm("PLC-DEMO:CONNECTION_LOST", "PLC-DEMO", "PLC_CONNECTION_LOST", "CRITICAL",
            "No hay datos recientes del PLC de demostración en " + plcProperties.port(), "PLC_DEMO_SERIAL", plcLost, now);
    }

    @Transactional
    public void reconcileDemoFlags(boolean high, boolean low, Instant now) {
        repository.reconcileAlarm("PLC-DEMO:TEMP_HIGH", "PLC-DEMO", "PLC_TEMPERATURE_HIGH", "CRITICAL",
            "El firmware demo reporta alarma de temperatura alta", "PLC_DEMO_SERIAL", high, now);
        repository.reconcileAlarm("PLC-DEMO:TEMP_LOW", "PLC-DEMO", "PLC_TEMPERATURE_LOW", "CRITICAL",
            "El firmware demo reporta alarma de temperatura baja", "PLC_DEMO_SERIAL", low, now);
    }

    private long sourceFreshnessSeconds(TankView tank) {
        return "PLC_DEMO_SERIAL".equals(tank.pillSource()) ? plcProperties.freshnessSeconds()
            : mqttProperties.freshnessSeconds();
    }
}
