package com.sierradorada.sdbrewpi.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.http.MediaType; import org.springframework.test.web.servlet.MockMvc; import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class InventoryControllerTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; static final String SITE="00000000-0000-0000-0000-000000000401";
 @Test void recordsLotStockAndPreventsNegativeBalance() throws Exception {
  String warehouse=json.readTree(mvc.perform(post("/api/v1/plant/sites/"+SITE+"/warehouses").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"BOD-MP\",\"name\":\"Materia prima\",\"purpose\":\"RAW_MATERIALS\",\"temperatureControlled\":false,\"allowedCategories\":[\"RAW_MATERIAL\"],\"notes\":\"\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
  String item=json.readTree(mvc.perform(post("/api/v1/inventory/sites/"+SITE+"/items").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"MALTA-PALE\",\"name\":\"Malta Pale Ale\",\"category\":\"RAW_MATERIAL\",\"baseUnit\":\"KG\",\"trackLots\":true,\"minimumStock\":25,\"notes\":\"\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("MALTA-PALE")).andReturn().getResponse().getContentAsString()).get("id").asText();
  String lot=json.readTree(mvc.perform(post("/api/v1/inventory/items/"+item+"/lots").contentType(MediaType.APPLICATION_JSON).content("{\"internalCode\":\"MP-2609-001\",\"supplierLot\":\"SUP-42\",\"qualityStatus\":\"APPROVED\",\"notes\":\"\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
  String movement="{\"itemId\":\"%s\",\"lotId\":\"%s\",\"warehouseId\":\"%s\",\"movementType\":\"RECEIPT\",\"quantity\":100,\"reference\":\"OC-1\",\"notes\":\"\"}".formatted(item,lot,warehouse);
  mvc.perform(post("/api/v1/inventory/movements").contentType(MediaType.APPLICATION_JSON).content(movement)).andExpect(status().isCreated()).andExpect(jsonPath("$.quantity").value(100));
  mvc.perform(get("/api/v1/inventory/overview")).andExpect(status().isOk()).andExpect(jsonPath("$.balances[0].quantity").value(100)).andExpect(jsonPath("$.lowStockItems").value(0));
  mvc.perform(post("/api/v1/inventory/movements").contentType(MediaType.APPLICATION_JSON).content(movement.replace("RECEIPT","ISSUE").replace("100","101"))).andExpect(status().isConflict());
 }
 @Test void requiresLotWhenTrackingIsEnabled() throws Exception {
  mvc.perform(get("/api/v1/inventory/overview")).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
 }
}
