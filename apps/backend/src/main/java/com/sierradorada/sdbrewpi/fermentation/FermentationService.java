package com.sierradorada.sdbrewpi.fermentation;

import com.sierradorada.sdbrewpi.shared.TankNotFoundException;
import com.sierradorada.sdbrewpi.shared.RevisionConflictException;
import com.sierradorada.sdbrewpi.telemetry.MqttProperties;
import com.sierradorada.sdbrewpi.telemetry.TelemetryRuntimeStatus;
import com.sierradorada.sdbrewpi.plc.PlcDemoProperties;
import com.sierradorada.sdbrewpi.plc.PlcDemoRuntimeStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FermentationService {
    private static final double HYSTERESIS_C = 0.35;
    private final FermentationRepository repository;
    private final boolean hardwareEnabled;
    private final boolean simulationEnabled;
    private final MqttProperties mqttProperties;
    private final TelemetryRuntimeStatus telemetryStatus;
    private final PlcDemoProperties plcProperties;
    private final PlcDemoRuntimeStatus plcStatus;

    public FermentationService(FermentationRepository repository,
            @Value("${sdbrewpi.hardware.enabled:false}") boolean hardwareEnabled,
            @Value("${sdbrewpi.simulation.enabled:true}") boolean simulationEnabled,
            MqttProperties mqttProperties, TelemetryRuntimeStatus telemetryStatus,
            PlcDemoProperties plcProperties, PlcDemoRuntimeStatus plcStatus) {
        this.repository = repository;
        this.hardwareEnabled = hardwareEnabled;
        this.simulationEnabled = simulationEnabled;
        this.mqttProperties = mqttProperties;
        this.telemetryStatus = telemetryStatus;
        this.plcProperties = plcProperties;
        this.plcStatus = plcStatus;
    }

    public FermentationOverview overview() {
        String environment = hardwareEnabled ? "HARDWARE" : plcProperties.enabled() ? "PLC_DEMO_READ_ONLY"
            : mqttProperties.enabled() ? "LIVE_READ_ONLY" : "SIMULATION";
        return new FermentationOverview(environment, Instant.now(), repository.findTanks(), repository.findChiller(hardwareEnabled),
            telemetryStatus.view(), plcStatus.view(), repository.findActiveAlarms());
    }

    public FermentationHistoryView history(String id, int hours, int limit) {
        tank(id);
        int safeHours = Math.max(1, Math.min(hours, 24 * 30));
        int safeLimit = Math.max(1, Math.min(limit, 2000));
        Instant from = Instant.now().minus(Duration.ofHours(safeHours));
        List<FermentationMeasurementView> samples = repository.findHistory(id, from, safeLimit);
        return new FermentationHistoryView(id, from, Instant.now(), samples.reversed());
    }

    public AlarmHistoryView alarmHistory(int hours, int limit) {
        int safeHours = Math.max(1, Math.min(hours, 24 * 90));
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        Instant from = Instant.now().minus(Duration.ofHours(safeHours));
        return new AlarmHistoryView(from, Instant.now(), repository.findAlarms(from, safeLimit));
    }

    @Transactional
    public AlarmView acknowledgeAlarm(String id, AlarmAcknowledgementRequest request, String actor) {
        AlarmView alarm = repository.findAlarm(id)
            .orElseThrow(() -> new IllegalArgumentException("La alarma no existe"));
        if (!"OPEN".equals(alarm.status())) throw new IllegalStateException("La alarma ya no está activa");
        if (alarm.acknowledgedAt() != null) throw new IllegalStateException("La alarma ya fue reconocida");
        String safeActor = safeActor(actor);
        String note = request.note() == null ? "" : request.note().trim();
        Instant now = Instant.now();
        if (repository.acknowledgeAlarm(id, request.expectedRevision(), safeActor, note, now) == 0) {
            throw new RevisionConflictException();
        }
        repository.audit(UUID.randomUUID().toString(), safeActor, id, "ACKNOWLEDGE_ALARM", alarm.code(),
            "ACCEPTED", "Alarma reconocida por el operador");
        return repository.findAlarm(id).orElseThrow();
    }

    @Transactional
    public CommandResult setpoint(String id, SetpointRequest request, String actor) {
        tank(id);
        if (repository.updateSetpoint(id, request.setpointC(), request.expectedRevision()) == 0) {
            throw new RevisionConflictException();
        }
        return accepted(actor, id, "SET_SETPOINT", Double.toString(request.setpointC()),
            "Setpoint guardado; las salidas físicas están deshabilitadas");
    }

    @Transactional
    public CommandResult mode(String id, ModeRequest request, String actor) {
        tank(id);
        if (repository.updateMode(id, request.mode(), request.expectedRevision()) == 0) {
            throw new RevisionConflictException();
        }
        return accepted(actor, id, "SET_MODE", request.mode().name(), "Modo actualizado; la actuación física está deshabilitada");
    }

    public TankView tank(String id) {
        return repository.findTank(id).orElseThrow(() -> new TankNotFoundException(id));
    }

    private CommandResult accepted(String actor, String id, String type, String payload, String reason) {
        String commandId = UUID.randomUUID().toString();
        repository.audit(commandId, safeActor(actor), id, type, payload, "ACCEPTED", reason);
        return new CommandResult(commandId, "ACCEPTED", reason, Instant.now(), tank(id));
    }

    private String safeActor(String actor) {
        if (actor == null || actor.isBlank()) return "local-webapp";
        String trimmed = actor.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void simulate() {
        if (!simulationEnabled || hardwareEnabled || mqttProperties.enabled() || plcProperties.enabled()) return;
        boolean anyDemand = false;
        Instant now = Instant.now();
        for (TankView tank : repository.findTanks()) {
            boolean demand = CoolingDemandPolicy.decide(tank.mode(), tank.productTemperatureC(), tank.setpointC(), HYSTERESIS_C, tank.coolingDemand());
            double ambientEffect = (22.0 - tank.productTemperatureC()) * 0.012;
            double coolingEffect = demand ? -0.075 : 0.0;
            double temperature = clamp(tank.productTemperatureC() + ambientEffect + coolingEffect, 0, 35);
            double gravity = Math.max(0.998, tank.gravity() - 0.00002);
            repository.simulate(tank.id(), temperature, gravity, demand, now);
            repository.recordMeasurementIfDue(tank.id(), "SIMULATION", now, temperature, gravity, "GOOD",
                now.minusSeconds(10));
            anyDemand |= demand;
        }
        repository.simulateChiller(anyDemand);
    }

    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
