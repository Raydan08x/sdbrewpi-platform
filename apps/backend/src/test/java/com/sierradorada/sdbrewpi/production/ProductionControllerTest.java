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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductionControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductionService service;

    @Test
    void exposesDemoRecipeAndActiveBatch() throws Exception {
        mvc.perform(get("/api/v1/production/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recipes[0].code").value("DEMO-PALE-ALE"))
            .andExpect(jsonPath("$.recipes[0].steps.length()").value(3))
            .andExpect(jsonPath("$.recipes[0].steps[1].rampRateCPerHour").value(0.5))
            .andExpect(jsonPath("$.activeBatches[0].code").value("SIM-LOTE-001"))
            .andExpect(jsonPath("$.activeBatches[0].tankId").value("TANK-01"))
            .andExpect(jsonPath("$.activeBatches[0].profileState").value("NOT_STARTED"))
            .andExpect(jsonPath("$.processStages.length()").value(43))
            .andExpect(jsonPath("$.processStages[0].code").value("ORDER_RELEASE"))
            .andExpect(jsonPath("$.processStages[24].code").value("FERMENTATION"))
            .andExpect(jsonPath("$.processStages[35].variant").value("KEG"));
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
    void rejectsThermalRampThatCannotReachItsTarget() throws Exception {
        mvc.perform(post("/api/v1/production/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"BAD-RAMP","name":"Rampa inválida","originalGravity":1.050,
                     "targetFinalGravity":1.010,"defaultVolumeL":100,"steps":[
                       {"name":"Principal","targetTemperatureC":20,"durationHours":24},
                       {"name":"Cold crash","targetTemperatureC":4,"durationHours":2,"rampRateCPerHour":1}]}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("La rampa de la fase Cold crash no alcanza su temperatura objetivo dentro de la duración configurada"));
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

        mvc.perform(get("/api/v1/fermentation/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tanks[0].mode").value("OFF"));
    }

    @Test
    void startsPausesAndResumesProfileWithAuditEvents() throws Exception {
        String batchId = "00000000-0000-0000-0000-000000000301";
        mvc.perform(put("/api/v1/production/batches/" + batchId + "/profile/start")
                .header("X-Actor", "qa-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profileState").value("RUNNING"))
            .andExpect(jsonPath("$.stepStartedAt").isNotEmpty())
            .andExpect(jsonPath("$.stepExpectedCompleteAt").isNotEmpty());

        mvc.perform(put("/api/v1/fermentation/tanks/TANK-01/setpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"setpointC\":17.5,\"expectedRevision\":1}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("El perfil en ejecución controla el tanque; páusalo antes de intervenir"));

        mvc.perform(put("/api/v1/fermentation/tanks/TANK-01/mode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"OFF\",\"expectedRevision\":1}"))
            .andExpect(status().isConflict());

        mvc.perform(put("/api/v1/production/batches/" + batchId + "/profile/pause")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profileState").value("PAUSED"));

        mvc.perform(get("/api/v1/fermentation/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tanks[0].mode").value("MANUAL"));

        Long manualRevision = jdbc.queryForObject(
            "SELECT revision FROM fermentation_tank WHERE id = 'TANK-01'", Long.class);
        mvc.perform(put("/api/v1/fermentation/tanks/TANK-01/setpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"setpointC\":17.5,\"expectedRevision\":" + manualRevision + "}"))
            .andExpect(status().isOk());

        mvc.perform(put("/api/v1/production/batches/" + batchId + "/profile/resume")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profileState").value("RUNNING"));

        mvc.perform(get("/api/v1/production/batches/" + batchId + "/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));

        mvc.perform(get("/api/v1/fermentation/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tanks[0].mode").value("AUTO"))
            .andExpect(jsonPath("$.tanks[0].setpointC").value(18.0));
    }

    @Test
    void recordsStructuredIngredientAdditionInBatchLog() throws Exception {
        String batchId = "00000000-0000-0000-0000-000000000301";
        mvc.perform(post("/api/v1/production/batches/" + batchId + "/events")
                .header("X-Actor", "qa-cellar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventType":"INGREDIENT_ADDITION","message":"Adición de lúpulo en fermentación",
                     "materialName":"Citra","quantity":0.75,"unit":"kg"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.eventType").value("INGREDIENT_ADDITION"))
            .andExpect(jsonPath("$.stepOrder").value(1))
            .andExpect(jsonPath("$.actor").value("qa-cellar"))
            .andExpect(jsonPath("$.materialName").value("Citra"))
            .andExpect(jsonPath("$.quantity").value(0.75))
            .andExpect(jsonPath("$.unit").value("kg"));

        mvc.perform(get("/api/v1/production/batches/" + batchId + "/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].materialName").value("Citra"));
    }

    @Test
    void rejectsIncompleteIngredientAddition() throws Exception {
        mvc.perform(post("/api/v1/production/batches/00000000-0000-0000-0000-000000000301/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventType\":\"INGREDIENT_ADDITION\",\"message\":\"Adición incompleta\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Una adición requiere material, cantidad y unidad"));
    }

    @Test
    void advancesOverdueProfileStepAndUpdatesSetpoint() throws Exception {
        String batchId = "00000000-0000-0000-0000-000000000301";
        jdbc.update("UPDATE production_batch SET profile_state = 'RUNNING', step_started_at = ?, revision = 1 WHERE id = ?",
            java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(121 * 3600L)), batchId);
        jdbc.update("UPDATE fermentation_tank SET mode = 'AUTO', setpoint_c = 18 WHERE id = 'TANK-01'");

        service.advanceProfiles();

        mvc.perform(get("/api/v1/production/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeBatches[0].currentStep").value(2));
        mvc.perform(get("/api/v1/fermentation/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tanks[0].setpointC").value(18.5));
    }
}
