package com.sierradorada.sdbrewpi.production;

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
class ProductionControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void exposesDemoRecipeAndActiveBatch() throws Exception {
        mvc.perform(get("/api/v1/production/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recipes[0].code").value("DEMO-PALE-ALE"))
            .andExpect(jsonPath("$.recipes[0].steps.length()").value(3))
            .andExpect(jsonPath("$.activeBatches[0].code").value("SIM-LOTE-001"))
            .andExpect(jsonPath("$.activeBatches[0].tankId").value("TANK-01"));
    }

    @Test
    void createsVersionedRecipeAndAssignsBatchToFreeTank() throws Exception {
        String recipeBody = """
            {"code":"qa-lager","name":"Lager QA","originalGravity":1.048,"targetFinalGravity":1.010,
             "defaultVolumeL":180,"notes":"Prueba","steps":[
               {"name":"Principal","targetTemperatureC":12,"durationHours":168},
               {"name":"Maduración","targetTemperatureC":4,"durationHours":96}]}
            """;
        String recipeJson = mvc.perform(post("/api/v1/production/recipes")
                .header("X-Actor", "qa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(recipeBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("QA-LAGER"))
            .andExpect(jsonPath("$.version").value(1))
            .andReturn().getResponse().getContentAsString();
        JsonNode recipe = objectMapper.readTree(recipeJson);

        String batchBody = "{\"code\":\"qa-lote-002\",\"recipeVersionId\":\"" + recipe.get("id").asText()
            + "\",\"tankId\":\"TANK-02\",\"volumeL\":175}";
        mvc.perform(post("/api/v1/production/batches")
                .header("X-Actor", "qa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("QA-LOTE-002"))
            .andExpect(jsonPath("$.recipeName").value("Lager QA"))
            .andExpect(jsonPath("$.tankId").value("TANK-02"))
            .andExpect(jsonPath("$.profile.length()").value(2));
    }

    @Test
    void rejectsSecondActiveBatchForSameTank() throws Exception {
        mvc.perform(post("/api/v1/production/batches")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CONFLICT\",\"recipeVersionId\":\"00000000-0000-0000-0000-000000000101\",\"tankId\":\"TANK-01\",\"volumeL\":200}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }

    @Test
    void rejectsRecipeWithImpossibleGravityTarget() throws Exception {
        mvc.perform(post("/api/v1/production/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"BAD\",\"name\":\"Inválida\",\"originalGravity\":1.040,\"targetFinalGravity\":1.050,\"defaultVolumeL\":100,\"steps\":[{\"name\":\"Paso\",\"targetTemperatureC\":18,\"durationHours\":24}]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_COMMAND"));
    }

    @Test
    void completingBatchReleasesTankForAnotherBatch() throws Exception {
        mvc.perform(put("/api/v1/production/batches/00000000-0000-0000-0000-000000000301/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(post("/api/v1/production/batches")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"SIM-LOTE-002\",\"recipeVersionId\":\"00000000-0000-0000-0000-000000000101\",\"tankId\":\"TANK-01\",\"volumeL\":200}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tankId").value("TANK-01"));
    }
}
