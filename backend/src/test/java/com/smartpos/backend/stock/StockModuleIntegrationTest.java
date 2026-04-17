package com.smartpos.backend.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartpos.backend.support.AuthHelper;
import com.smartpos.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockModuleIntegrationTest extends IntegrationTestBase {

    @Autowired private StockLedgerService stockLedger;

    private String ownerToken;
    private String warehouseToken;
    private String cashierToken;

    private String productAId;
    private String productBId;
    private String categoryId;
    private String supplierId;

    private void bootstrap() throws Exception {
        ownerToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, OWNER_EMAIL, DEFAULT_PASSWORD);
        warehouseToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, WAREHOUSE_EMAIL, DEFAULT_PASSWORD);
        cashierToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, CASHIER_EMAIL, DEFAULT_PASSWORD);

        categoryId = objectMapper.readTree(mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", "Food"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        supplierId = objectMapper.readTree(mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", "PT Supplier"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        productAId = createProduct("SKU-A", "Alpha", 5);
        productBId = createProduct("SKU-B", "Beta",  0);

        // Seed stock: A=10, B=0
        seedStockViaPurchase(productAId, 10);
    }

    private String createProduct(String sku, String name, int lowStockThreshold) throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("sku", sku);
        p.put("name", name);
        p.put("categoryId", categoryId);
        p.put("unit", "pcs");
        p.put("cost", 1000);
        p.put("price", 1500);
        p.put("barcode", null);
        p.put("lowStockThreshold", lowStockThreshold);
        p.put("active", true);
        return objectMapper.readTree(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(p)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    private void seedStockViaPurchase(String productId, int qty) throws Exception {
        Map<String, Object> create = Map.of("supplierId", supplierId,
                "items", List.of(Map.of("productId", productId, "qtyOrdered", qty, "cost", 1000)));
        String body = mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(create)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String purchaseId = objectMapper.readTree(body).get("id").asText();
        mockMvc.perform(post("/api/v1/purchases/" + purchaseId + "/receive")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("items",
                                List.of(Map.of("productId", productId, "qtyReceived", qty, "cost", 1000))))))
                .andExpect(status().isOk());
    }

    @Test
    void onHandListsAllProductsWithCurrentQty() throws Exception {
        bootstrap();

        JsonNode body = objectMapper.readTree(mockMvc.perform(get("/api/v1/stock/on-hand")
                        .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(2, body.get("totalElements").asInt());
        for (JsonNode row : body.get("content")) {
            if (row.get("productId").asText().equals(productAId)) {
                assertEquals(10, row.get("onHand").asInt());
                assertEquals(false, row.get("lowStock").asBoolean());
            } else if (row.get("productId").asText().equals(productBId)) {
                assertEquals(0, row.get("onHand").asInt());
                assertEquals(true, row.get("lowStock").asBoolean());
            }
        }
    }

    @Test
    void onHandLowOnlyFiltersToThresholdBreachers() throws Exception {
        bootstrap();

        JsonNode body = objectMapper.readTree(mockMvc.perform(get("/api/v1/stock/on-hand?lowOnly=true")
                        .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(1, body.get("totalElements").asInt());
        assertEquals(productBId, body.get("content").get(0).get("productId").asText());
    }

    @Test
    void movementsListReturnsChronologicalDescByDefault() throws Exception {
        bootstrap();

        JsonNode body = objectMapper.readTree(mockMvc.perform(get("/api/v1/stock/movements")
                        .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertTrue(body.get("totalElements").asInt() >= 1);
        assertEquals("PURCHASE_RECEIVE", body.get("content").get(0).get("type").asText());
        assertEquals("SKU-A", body.get("content").get(0).get("productSku").asText());
    }

    @Test
    void positiveAdjustmentIncreasesOnHandAndAppendsMovement() throws Exception {
        bootstrap();

        mockMvc.perform(post("/api/v1/stock/adjustments")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "productId", productAId,
                                "qtyDelta", 3,
                                "note", "Stock count correction"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.qtyDelta").value(3))
                .andExpect(jsonPath("$.onHandAfter").value(13));

        assertEquals(13, stockLedger.onHand(UUID.fromString(productAId)));
    }

    @Test
    void negativeAdjustmentBeyondStockIsRejected() throws Exception {
        bootstrap();

        mockMvc.perform(post("/api/v1/stock/adjustments")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "productId", productAId,
                                "qtyDelta", -999,
                                "note", "Try to go negative"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        assertEquals(10, stockLedger.onHand(UUID.fromString(productAId)));
    }

    @Test
    void cashierCanViewOnHandButCannotAdjust() throws Exception {
        bootstrap();

        mockMvc.perform(get("/api/v1/stock/on-hand").header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/stock/adjustments")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "productId", productAId, "qtyDelta", 1, "note", "hi"))))
                .andExpect(status().isForbidden());
    }
}
