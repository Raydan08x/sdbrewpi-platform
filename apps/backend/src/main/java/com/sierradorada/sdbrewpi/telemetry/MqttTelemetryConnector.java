package com.sierradorada.sdbrewpi.telemetry;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sdbrewpi.mqtt.enabled", havingValue = "true")
public class MqttTelemetryConnector implements MqttCallbackExtended {
    private static final Logger log = LoggerFactory.getLogger(MqttTelemetryConnector.class);
    private final MqttProperties properties;
    private final TelemetryIngestionService ingestion;
    private final TelemetryRuntimeStatus status;
    private final AtomicBoolean connectionFailureLogged = new AtomicBoolean();
    private MqttClient client;

    public MqttTelemetryConnector(MqttProperties properties, TelemetryIngestionService ingestion,
            TelemetryRuntimeStatus status) {
        this.properties = properties;
        this.ingestion = ingestion;
        this.status = status;
    }

    @Scheduled(initialDelay = 1000, fixedDelay = 10000)
    public synchronized void connectIfNeeded() {
        if (client != null && client.isConnected()) return;
        try {
            if (client == null) {
                client = new MqttClient(properties.brokerUri(), properties.clientId(), new MemoryPersistence());
                client.setCallback(this);
            }
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(false);
            options.setConnectionTimeout(5);
            options.setKeepAliveInterval(30);
            if (properties.username() != null && !properties.username().isBlank()) options.setUserName(properties.username());
            if (properties.password() != null && !properties.password().isBlank()) options.setPassword(properties.password().toCharArray());
            client.connect(options);
            subscribe();
            status.connected();
            connectionFailureLogged.set(false);
        } catch (MqttException exception) {
            status.disconnected("Broker MQTT no disponible");
            if (connectionFailureLogged.compareAndSet(false, true)) {
                log.warn("No fue posible conectar el lector MQTT: {}", exception.getReasonCode());
            } else {
                log.debug("El lector MQTT continúa desconectado: {}", exception.getReasonCode());
            }
        }
    }

    private void subscribe() throws MqttException {
        List<String> topics = properties.topics().stream().map(String::trim).filter(topic -> !topic.isBlank()).toList();
        client.subscribe(topics.toArray(String[]::new), topics.stream().mapToInt(topic -> 1).toArray());
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        status.connected();
        connectionFailureLogged.set(false);
        if (reconnect) {
            try { subscribe(); }
            catch (MqttException exception) { status.disconnected("Reconexión MQTT sin suscripción"); }
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        status.disconnected("Conexión MQTT perdida");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        ingestion.ingest(topic, new String(message.getPayload(), StandardCharsets.UTF_8), message.isRetained(), Instant.now());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Este cliente solo consume telemetría.
    }

    @PreDestroy
    public synchronized void disconnect() {
        if (client == null) return;
        try {
            if (client.isConnected()) client.disconnect();
            client.close();
        } catch (MqttException exception) {
            log.debug("Cierre MQTT incompleto: {}", exception.getReasonCode());
        }
    }
}
