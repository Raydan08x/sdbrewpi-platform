package com.sierradorada.sdbrewpi.plc;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PlcDemoParser {
    private static final Pattern DEMO_LINE = Pattern.compile(
        "^\\[DEMO\\]\\s+F1=(-?\\d+(?:\\.\\d+)?)\\s+F2=(-?\\d+(?:\\.\\d+)?)\\s+\\|\\s+"
            + "AlarmaCalor=([01])\\s+AlarmaFrio=([01])(?:\\s+\\|\\s+(.+))?\\s*$");
    private static final Pattern OUTPUT_STATE = Pattern.compile("^Chiller=(ON|OFF)\\s+Bomba=(ON|OFF)$");

    public PlcDemoSample parse(String line, Instant receivedAt) {
        Matcher matcher = DEMO_LINE.matcher(line == null ? "" : line.trim());
        if (!matcher.matches()) throw new IllegalArgumentException("Línea de demo PLC no reconocida");
        double f1 = Double.parseDouble(matcher.group(1));
        double f2 = Double.parseDouble(matcher.group(2));
        validateTemperature(f1);
        validateTemperature(f2);
        Boolean chiller = null;
        Boolean pump = null;
        String extra = matcher.group(5);
        if (extra != null) {
            Matcher output = OUTPUT_STATE.matcher(extra.trim());
            if (output.matches()) {
                chiller = "ON".equals(output.group(1));
                pump = "ON".equals(output.group(2));
            } else if (!extra.trim().matches("Delta=-?\\d+(?:\\.\\d+)?")) {
                throw new IllegalArgumentException("Estado adicional de demo PLC no reconocido");
            }
        }
        return new PlcDemoSample(f1, f2, "1".equals(matcher.group(3)), "1".equals(matcher.group(4)),
            chiller, pump, receivedAt);
    }

    private void validateTemperature(double value) {
        if (!Double.isFinite(value) || value < -10 || value > 50) {
            throw new IllegalArgumentException("Temperatura de demo PLC fuera de rango");
        }
    }
}
