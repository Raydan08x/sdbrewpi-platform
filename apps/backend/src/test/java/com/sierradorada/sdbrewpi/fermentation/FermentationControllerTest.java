package com.sierradorada.sdbrewpi.fermentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FermentationControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void exposesTwoTanksAndSimulationBanner() throws Exception {
        mvc.perform(get("/api/v1/fermentation/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.environment").value("SIMULATION"))
            .andExpect(jsonPath("$.tanks.length()").value(2))
            .andExpect(jsonPath("$.telemetry.enabled").value(false))
            .andExpect(jsonPath("$.plcDemo.enabled").value(false))
            .andExpect(jsonPath("$.alarms").isArray())
            .andExpect(jsonPath("$.chiller.hardwareEnabled").value(false));
    }

    @Test
    void exposesBoundedTankHistory() throws Exception {
        mvc.perform(get("/api/v1/fermentation/tanks/TANK-01/history")
                .param("hours", "99999").param("limit", "99999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tankId").value("TANK-01"))
            .andExpect(jsonPath("$.samples").isArray());
    }

    @Test
    void rejectsUnsafeSetpoint() throws Exception {
        mvc.perform(put("/api/v1/fermentation/tanks/TANK-01/setpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"setpointC\":-2,\"expectedRevision\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fields.setpointC").exists());
    }

    @Test
    void acceptsValidModeAndAuditsThroughService() throws Exception {
        mvc.perform(put("/api/v1/fermentation/tanks/TANK-01/mode")
                .header("X-Actor", "qa")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"AUTO\",\"expectedRevision\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.state.mode").value("AUTO"));
    }

    @Test
    void rejectsStaleRevisionAsConflict() throws Exception {
        mvc.perform(put("/api/v1/fermentation/tanks/TANK-01/mode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"OFF\",\"expectedRevision\":999}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("REVISION_CONFLICT"));
    }

    @Test
    void allowsCommandsFromLoopbackDevelopmentOrigin() throws Exception {
        mvc.perform(options("/api/v1/fermentation/tanks/TANK-01/mode")
                .header("Origin", "http://127.0.0.1:5173")
                .header("Access-Control-Request-Method", "PUT"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
    }
}
