package com.sierradorada.sdbrewpi.plant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlantControllerTest {
    private static final String SITE_ID = "00000000-0000-0000-0000-000000000401";
    private static final String VALID_PROFILE = """
        {"name":"Planta piloto","companyName":"Cervecería de prueba","legalName":"",
         "taxId":"","timezone":"America/Bogota","currency":"COP","nominalBatchCapacityL":400,
         "plannedFermenters":2,"pipingDeadVolumeL":12.5,"logoUrl":"","expectedRevision":0}
        """;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void exposesPlantAssetsAndSafeOnboardingState() throws Exception {
        mvc.perform(get("/api/v1/plant/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.site.code").value("MAIN"))
            .andExpect(jsonPath("$.assets.length()").value(13))
            .andExpect(jsonPath("$.onboarding.status").value("WAITING_FOR_CONNECTION"))
            .andExpect(jsonPath("$.onboarding.scannerEnabled").value(false))
            .andExpect(jsonPath("$.onboarding.hardwareOutputsEnabled").value(false));
    }

    @Test
    void updatesPlantProfileWithOptimisticRevision() throws Exception {
        mvc.perform(put("/api/v1/plant/sites/" + SITE_ID)
                .header("X-Actor", "qa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PROFILE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Planta piloto"))
            .andExpect(jsonPath("$.nominalBatchCapacityL").value(400.0))
            .andExpect(jsonPath("$.pipingDeadVolumeL").value(12.5))
            .andExpect(jsonPath("$.revision").value(1));
    }

    @Test
    void rejectsStalePlantRevision() throws Exception {
        mvc.perform(put("/api/v1/plant/sites/" + SITE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PROFILE.replace("\"expectedRevision\":0", "\"expectedRevision\":99")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("REVISION_CONFLICT"));
    }

    @Test
    void rejectsUnknownTimezone() throws Exception {
        mvc.perform(put("/api/v1/plant/sites/" + SITE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PROFILE.replace("America/Bogota", "Zona/Inventada")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_COMMAND"));
    }

    @Test
    void rejectsUnsafeLogoUrl() throws Exception {
        mvc.perform(put("/api/v1/plant/sites/" + SITE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PROFILE.replace("\"logoUrl\":\"\"", "\"logoUrl\":\"javascript:alert(1)\"")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_COMMAND"));
    }

    @Test
    void createsUpdatesAndRetiresPlantAsset() throws Exception {
        String createdJson = mvc.perform(post("/api/v1/plant/sites/" + SITE_ID + "/assets")
                .header("X-Actor", "qa")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"mill-01","assetType":"MILL","name":"Molino principal",
                     "manufacturer":"QA","model":"M1","capacityL":80,"electricalSpec":"110 V",
                     "communicationProtocol":"Sin comunicación","deviceIdentifier":"","firmwareProfile":"",
                     "status":"AVAILABLE","controllable":false,"notes":"Prueba"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("MILL-01"))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(createdJson);
        String id = created.get("id").asText();

        mvc.perform(put("/api/v1/plant/assets/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"MILL-01","assetType":"MILL","name":"Molino de malta",
                     "manufacturer":"QA","model":"M1","capacityL":90,"electricalSpec":"110 V",
                     "communicationProtocol":"Sin comunicación","deviceIdentifier":"","firmwareProfile":"",
                     "status":"MAINTENANCE","controllable":false,"notes":"Ajustado","expectedRevision":0}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Molino de malta"))
            .andExpect(jsonPath("$.capacityL").value(90.0))
            .andExpect(jsonPath("$.revision").value(1));

        mvc.perform(put("/api/v1/plant/assets/" + id + "/retire")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RETIRED"))
            .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/v1/plant/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assets.length()").value(13));
    }

    @Test
    void rejectsUnknownAssetType() throws Exception {
        mvc.perform(post("/api/v1/plant/sites/" + SITE_ID + "/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"BAD-01","assetType":"DESCONOCIDO","name":"Equipo inválido",
                     "status":"AVAILABLE","controllable":false}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_COMMAND"));
    }
}
