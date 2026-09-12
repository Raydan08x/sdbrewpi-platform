package com.sierradorada.sdbrewpi.telemetry;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sdbrewpi.mqtt")
public record MqttProperties(
    boolean enabled,
    String brokerUri,
    String clientId,
    String username,
    String password,
    List<String> topics,
    long freshnessSeconds
) {}
