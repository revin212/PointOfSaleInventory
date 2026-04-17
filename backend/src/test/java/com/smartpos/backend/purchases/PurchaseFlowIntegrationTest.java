package com.smartpos.backend.purchases;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartpos.backend.stock.StockLedgerService;
import com.smartpos.backend.support.AuthHelper;
import com.smartpos.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PurchaseFlowIntegrationTest extends IntegrationTestBase {

    @Autowired private StockLedgerService stockLedger;

    private String ownerToken;
    private String warehouseToken;
    private String cashierToken;

    private String categoryId;
    private String supplierId;
    private String productAId;
    private String productBId;

    private void initTokens() throws Exception {
        ownerToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, OWNER_EMAIL, DEFAULT_PASSWORD);
        warehouseToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, WAREHOUSE_EMAIL, DEFAULT_PASSWORD);
        cashierToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, CASHIER_EMAIL, DEFAULT_PASSWORD);
    }

    private void seedMasters() throws Exception {
        String catBody = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", "Food"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        categoryId = objectMapper.readTree(catBody).get("id").asText();

        String supBody = mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", "PT Supplier"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        supplierId = objectMapper.readTree(supBody).get("id").asText();

        productAId = createProduct("SKU-A", "Product A");
        productBId = createProduct("SKU-B", "Product B");
    }

    private String createProduct(String sku, String name) throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("sku", sku);
        p.put("name", name);
        p.put("categoryId", categoryId);
        p.put("unit", "pcs");
        p.put("cost", 1000);
        p.put("price", 1500);
        p.put("barcode", null);
        p.put("lowStockThreshold", 5);
        p.put("active", true);
        String body = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(p)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    @Test
    void happyPath_createPartialReceiveThenFullReceive_updatesStatusAndStock() throws Exception {
        initTokens();
        seedMasters();

        // Create purchase: A x10 @1000, B x5 @2000
        Map<String, Object> createReq = Map.of(
                "supplierId", supplierId,
                "items", List.of(
                        Map.of("productId", productAId, "qtyOrdered", 10, "cost", 1000),
                        Map.of("productId", productBId, "qtyOrdered", 5,  "cost", 2000)
                ));
        String createBody = mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        String purchaseId = objectMapper.readTree(createBody).get("id").asText();

        // Partial receive: A x6, B x5
        Map<String, Object> recv1 = Map.of("items", List.of(
                Map.of("productId", productAId, "qtyReceived", 6, "cost", 1000),
                Map.of("productId", productBId, "qtyReceived", 5, "cost", 2000)
        ));
        mockMvc.perform(post("/api/v1/purchases/" + purchaseId + "/receive")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(recv1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("PARTIALLY_RECEIVED"));

        assertEquals(6, stockLedger.onHand(UUID.fromString(productAId)));
        assertEquals(5, stockLedger.onHand(UUID.fromString(productBId)));

        // Receive remaining: A x4
        Map<String, Object> recv2 = Map.of("items", List.of(
                Map.of("productId", productAId, "qtyReceived", 4, "cost", 1000)
        ));
        mockMvc.perform(post("/api/v1/purchases/" + purchaseId + "/receive")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(recv2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        assertEquals(10, stockLedger.onHand(UUID.fromString(productAId)));
        assertEquals(5,  stockLedger.onHand(UUID.fromString(productBId)));

        // Detail shows fully received
        mockMvc.perform(get("/api/v1/purchases/" + purchaseId)
                        .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.items[?(@.productId == '" + productAId + "')].qtyOutstanding").value(0))
                .andExpect(jsonPath("$.items[?(@.productId == '" + productBId + "')].qtyOutstanding").value(0));
    }

    @Test
    void overReceiveFailsWithBusinessRuleEnvelope() throws Exception {
        initTokens();
        seedMasters();

        Map<String, Object> createReq = Map.of(
                "supplierId", supplierId,
                "items", List.of(Map.of("productId", productAId, "qtyOrdered", 3, "cost", 1000)));
        String body = mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String purchaseId = objectMapper.readTree(body).get("id").asText();

        Map<String, Object> recv = Map.of("items", List.of(
                Map.of("productId", productAId, "qtyReceived", 5, "cost", 1000)));
        mockMvc.perform(post("/api/v1/purchases/" + purchaseId + "/receive")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(recv)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        // No stock movement should have been written
        assertEquals(0, stockLedger.onHand(UUID.fromString(productAId)));
    }

    @Test
    void cashierCannotCreateOrReceivePurchase() throws Exception {
        initTokens();
        seedMasters();

        Map<String, Object> createReq = Map.of(
                "supplierId", supplierId,
                "items", List.of(Map.of("productId", productAId, "qtyOrdered", 1, "cost", 1000)));
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listPurchasesReturnsSummaryWithTotals() throws Exception {
        initTokens();
        seedMasters();

        Map<String, Object> createReq = Map.of(
                "supplierId", supplierId,
                "items", List.of(
                        Map.of("productId", productAId, "qtyOrdered", 2, "cost", 1000),
                        Map.of("productId", productBId, "qtyOrdered", 3, "cost", 2000)));
        mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createReq)))
                .andExpect(status().isCreated());

        JsonNode resp = objectMapper.readTree(mockMvc.perform(get("/api/v1/purchases")
                        .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(1, resp.get("totalElements").asInt());
        assertEquals("PT Supplier", resp.get("content").get(0).get("supplierName").asText());
        assertEquals(2, resp.get("content").get(0).get("itemCount").asInt());
        assertEquals(8000, resp.get("content").get(0).get("totalCost").asInt()); // 2*1000 + 3*2000
    }
}
