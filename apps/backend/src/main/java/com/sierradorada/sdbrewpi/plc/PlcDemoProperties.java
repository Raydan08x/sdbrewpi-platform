package com.sierradorada.sdbrewpi.plc;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sdbrewpi.plc-demo")
public record PlcDemoProperties(
    boolean enabled,
    String port,
    int baudRate,
    int freshnessSeconds,
    int historyIntervalSeconds
) {}
