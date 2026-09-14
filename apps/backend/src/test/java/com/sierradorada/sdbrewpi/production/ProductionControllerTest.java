package com.sierradorada.sdbrewpi.production;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void releasesMasterLotThenAssignsItsFermentationSnapshot() throws Exception {
        String releasedJson = mvc.perform(post("/api/v1/production/orders/release")
                .header("X-Actor", "qa-planner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"recipeVersionId":"00000000-0000-0000-0000-000000000101","plannedVolumeL":175,
                     "batchKind":"PILOT","productCode":"CERV"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("CERV-[0-9]{4}-P001")))
            .andExpect(jsonPath("$.status").value("RELEASED"))
            .andExpect(jsonPath("$.tankId").isEmpty())
            .andExpect(jsonPath("$.batchRecordOpenedAt").isNotEmpty())
            .andExpect(jsonPath("$.releasedBy").value("qa-planner"))
            .andExpect(jsonPath("$.batchKind").value("PILOT"))
            .andExpect(jsonPath("$.productName").value("Cerveza"))
            .andExpect(jsonPath("$.executionStages.length()").value(43))
            .andExpect(jsonPath("$.executionStages[0].code").value("ORDER_RELEASE"))
            .andExpect(jsonPath("$.executionStages[0].status").value("COMPLETED"))
            .andExpect(jsonPath("$.executionStages[1].code").value("WEIGHING_KIT"))
            .andExpect(jsonPath("$.executionStages[1].status").value("PENDING"))
            .andReturn().getResponse().getContentAsString();
        JsonNode released = objectMapper.readTree(releasedJson);
        String batchId = released.get("id").asText();
        BatchView ready = advancePreparation(batchId);
        assertEquals("READY_FOR_FERMENTATION", ready.status());

        mvc.perform(put("/api/v1/production/batches/" + batchId + "/fermentation-assignment")
                .header("X-Actor", "qa-cellar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tankId\":\"TANK-02\",\"transferredVolumeL\":170,\"expectedRevision\":" + ready.revision() + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FERMENTING"))
            .andExpect(jsonPath("$.tankId").value("TANK-02"))
            .andExpect(jsonPath("$.tankNameSnapshot").value("Fermentador 2"))
            .andExpect(jsonPath("$.pillIdSnapshot").value("PILL-3BA4"))
            .andExpect(jsonPath("$.pillSourceSnapshot").isNotEmpty())
            .andExpect(jsonPath("$.fermentationVolumeL").value(170))
            .andExpect(jsonPath("$.fermentationAssignedAt").isNotEmpty())
            .andExpect(jsonPath("$.fermentationAssignedBy").value("qa-cellar"));

        mvc.perform(get("/api/v1/production/batches/" + batchId + "/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventType").value("FERMENTATION_ASSIGNED"));
    }

    @Test
    void keepsIndependentCountersForTestPilotAndCommercialLots() throws Exception {
        String base = "{\"recipeVersionId\":\"00000000-0000-0000-0000-000000000101\",\"plannedVolumeL\":100,";
        mvc.perform(post("/api/v1/production/orders/release").contentType(MediaType.APPLICATION_JSON)
                .content(base + "\"batchKind\":\"TEST\",\"productCode\":\"CERV\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("CERV-[0-9]{4}-T001")));
        mvc.perform(post("/api/v1/production/orders/release").contentType(MediaType.APPLICATION_JSON)
                .content(base + "\"batchKind\":\"PILOT\",\"productCode\":\"CERV\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("CERV-[0-9]{4}-P001")));
        mvc.perform(post("/api/v1/production/orders/release").contentType(MediaType.APPLICATION_JSON)
                .content(base + "\"batchKind\":\"COMMERCIAL\",\"productCode\":\"HSEL\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("HSEL-[0-9]{4}-L001")));
    }

    @Test
    void rejectsFermenterAssignmentBeforeLotIsReady() throws Exception {
        String releasedJson = mvc.perform(post("/api/v1/production/orders/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"recipeVersionId":"00000000-0000-0000-0000-000000000101","plannedVolumeL":100,
                     "batchKind":"TEST","productCode":"CERV"}
                    """))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String batchId = objectMapper.readTree(releasedJson).get("id").asText();

        mvc.perform(put("/api/v1/production/batches/" + batchId + "/fermentation-assignment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tankId\":\"TANK-02\",\"transferredVolumeL\":95,\"expectedRevision\":0}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }

    @Test
    void enforcesStageOrderAndRequiredSteps() throws Exception {
        String releasedJson = mvc.perform(post("/api/v1/production/orders/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"recipeVersionId":"00000000-0000-0000-0000-000000000101","plannedVolumeL":100,
                     "batchKind":"TEST","productCode":"CERV"}
                    """))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String batchId = objectMapper.readTree(releasedJson).get("id").asText();

        mvc.perform(put("/api/v1/production/batches/" + batchId + "/stages/MILLING")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"START\",\"expectedBatchRevision\":0,\"expectedStageRevision\":0}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Primero debes resolver la etapa Armar kit y realizar pesajes"));

        mvc.perform(put("/api/v1/production/batches/" + batchId + "/stages/WEIGHING_KIT")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"SKIP\",\"expectedBatchRevision\":0,\"expectedStageRevision\":0}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Una etapa obligatoria no se puede omitir"));
    }

    @Test
    void createsVersionedRecipe() throws Exception {
        String recipeBody = """
            {"code":"qa-lager","name":"Lager QA","originalGravity":1.048,"targetFinalGravity":1.010,
             "defaultVolumeL":180,"notes":"Prueba","steps":[
               {"name":"Principal","targetTemperatureC":12,"durationHours":168},
               {"name":"Maduración","targetTemperatureC":4,"durationHours":96}]}
            """;
        mvc.perform(post("/api/v1/production/recipes")
                .header("X-Actor", "qa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(recipeBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("QA-LAGER"))
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.steps.length()").value(2));
    }

    @Test
    void rejectsSecondActiveBatchForSameTank() throws Exception {
        String released = mvc.perform(post("/api/v1/production/orders/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipeVersionId\":\"00000000-0000-0000-0000-000000000101\",\"plannedVolumeL\":200,\"batchKind\":\"TEST\",\"productCode\":\"CERV\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String batchId = objectMapper.readTree(released).get("id").asText();
        BatchView ready = advancePreparation(batchId);
        mvc.perform(put("/api/v1/production/batches/" + batchId + "/fermentation-assignment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tankId\":\"TANK-01\",\"transferredVolumeL\":200,\"expectedRevision\":" + ready.revision() + "}"))
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

        String released = mvc.perform(post("/api/v1/production/orders/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipeVersionId\":\"00000000-0000-0000-0000-000000000101\",\"plannedVolumeL\":200,\"batchKind\":\"TEST\",\"productCode\":\"CERV\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String batchId = objectMapper.readTree(released).get("id").asText();
        BatchView ready = advancePreparation(batchId);
        mvc.perform(put("/api/v1/production/batches/" + batchId + "/fermentation-assignment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tankId\":\"TANK-01\",\"transferredVolumeL\":200,\"expectedRevision\":" + ready.revision() + "}"))
            .andExpect(status().isOk())
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

    private BatchView advancePreparation(String batchId) {
        BatchView batch = service.overview().activeBatches().stream().filter(item -> item.id().equals(batchId))
            .findFirst().orElseThrow();
        for (BatchStageView stage : batch.executionStages().stream().filter(item -> item.order() <= 180).toList()) {
            if ("COMPLETED".equals(stage.status()) || "SKIPPED".equals(stage.status())) continue;
            batch = service.commandStage(batchId, stage.code(),
                new StageCommandRequest("START", batch.revision(), stage.revision(), "", null, null), "qa-process");
            BatchStageView started = batch.executionStages().stream().filter(item -> item.code().equals(stage.code()))
                .findFirst().orElseThrow();
            batch = service.commandStage(batchId, stage.code(),
                new StageCommandRequest("COMPLETE", batch.revision(), started.revision(), "QA", null, null), "qa-process");
        }
        return batch;
    }
}
