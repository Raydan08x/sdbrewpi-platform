package com.sierradorada.sdbrewpi.plant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
