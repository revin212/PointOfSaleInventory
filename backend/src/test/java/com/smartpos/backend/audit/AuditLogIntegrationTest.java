package com.smartpos.backend.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartpos.backend.support.AuthHelper;
import com.smartpos.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditLogIntegrationTest extends IntegrationTestBase {

    private String ownerToken;
    private String cashierToken;
    private String warehouseToken;

    private void initTokens() throws Exception {
        ownerToken     = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, OWNER_EMAIL,     DEFAULT_PASSWORD);
        cashierToken   = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, CASHIER_EMAIL,   DEFAULT_PASSWORD);
        warehouseToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, WAREHOUSE_EMAIL, DEFAULT_PASSWORD);
    }

    @Test
    void writeFlowsEmitAuditLogsVisibleToOwner() throws Exception {
        initTokens();

        // Seed catalog + purchase + sale + cancel to exercise several actions
        String catId = objectMapper.readTree(mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", "Food"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        String supId = objectMapper.readTree(mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", "PT Supplier"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        Map<String, Object> prod = new HashMap<>();
        prod.put("sku", "SKU-1"); prod.put("name", "Item 1"); prod.put("categoryId", catId);
        prod.put("unit", "pcs"); prod.put("cost", 1000); prod.put("price", 1500);
        prod.put("barcode", null); prod.put("lowStockThreshold", 5); prod.put("active", true);
        String prodId = objectMapper.readTree(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(prod)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        Map<String, Object> purchase = Map.of("supplierId", supId,
                "items", List.of(Map.of("productId", prodId, "qtyOrdered", 10, "cost", 1000)));
        String purchaseId = objectMapper.readTree(mockMvc.perform(post("/api/v1/purchases")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(purchase)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/v1/purchases/" + purchaseId + "/receive")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("items",
                                List.of(Map.of("productId", prodId, "qtyReceived", 10, "cost", 1000))))))
                .andExpect(status().isOk());

        String saleId = objectMapper.readTree(mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "items", List.of(Map.of("productId", prodId, "qty", 1, "unitPrice", 1500, "lineDiscount", 0)),
                                "discount", 0, "paymentMethod", "CASH", "paidAmount", 2000))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/sales/" + saleId + "/cancel")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "test"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/stock/adjustments")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "productId", prodId, "qtyDelta", 2, "note", "correction"))))
                .andExpect(status().isOk());

        JsonNode body = objectMapper.readTree(mockMvc.perform(
                        get("/api/v1/audit-logs?size=100")
                                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        Set<String> actions = new HashSet<>();
        for (JsonNode row : body.get("content")) {
            actions.add(row.get("action").asText());
        }
        assertTrue(actions.contains("PURCHASE_CREATE"),  "expected PURCHASE_CREATE in " + actions);
        assertTrue(actions.contains("PURCHASE_RECEIVE"), "expected PURCHASE_RECEIVE in " + actions);
        assertTrue(actions.contains("SALE_CREATE"),      "expected SALE_CREATE in " + actions);
        assertTrue(actions.contains("SALE_CANCEL"),      "expected SALE_CANCEL in " + actions);
        assertTrue(actions.contains("STOCK_ADJUSTMENT"), "expected STOCK_ADJUSTMENT in " + actions);
    }

    @Test
    void auditLogEndpointIsOwnerOnly() throws Exception {
        initTokens();
        mockMvc.perform(get("/api/v1/audit-logs").header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit-logs").header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit-logs").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    void auditLogFilterByEntityTypeAndAction() throws Exception {
        initTokens();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "New Cashier",
                                "email", "newcashier@smartpos.test",
                                "password", "Password123!",
                                "role", "CASHIER",
                                "active", true))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/audit-logs?entityType=USER&action=USER_CREATE")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].entityType").value("USER"))
                .andExpect(jsonPath("$.content[0].action").value("USER_CREATE"));
    }
}
