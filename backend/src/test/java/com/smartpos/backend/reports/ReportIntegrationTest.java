package com.smartpos.backend.reports;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartpos.backend.support.AuthHelper;
import com.smartpos.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportIntegrationTest extends IntegrationTestBase {

    private String ownerToken;
    private String cashierToken;
    private String warehouseToken;
    private String categoryId;
    private String supplierId;
    private String productAId;
    private String productBId;

    private void bootstrap() throws Exception {
        ownerToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, OWNER_EMAIL, DEFAULT_PASSWORD);
        cashierToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, CASHIER_EMAIL, DEFAULT_PASSWORD);
        warehouseToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, WAREHOUSE_EMAIL, DEFAULT_PASSWORD);

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

        productAId = createProduct("SKU-A", "Alpha");
        productBId = createProduct("SKU-B", "Beta");
        seedStockViaPurchase(productAId, 50);
        seedStockViaPurchase(productBId, 50);
    }

    private String createProduct(String sku, String name) throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("sku", sku);
        p.put("name", name);
        p.put("categoryId", categoryId);
        p.put("unit", "pcs");
        p.put("cost", 1000);
        p.put("price", 2000);
        p.put("barcode", null);
        p.put("lowStockThreshold", 5);
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

    private String createSale(int qtyA, int qtyB, String paymentMethod, int paid) throws Exception {
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        if (qtyA > 0) items.add(Map.of("productId", productAId, "qty", qtyA, "unitPrice", 2000, "lineDiscount", 0));
        if (qtyB > 0) items.add(Map.of("productId", productBId, "qty", qtyB, "unitPrice", 2000, "lineDiscount", 0));
        Map<String, Object> sale = Map.of(
                "items", items,
                "discount", 0,
                "paymentMethod", paymentMethod,
                "paidAmount", paid
        );
        String body = mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(sale)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    @Test
    void dailySummaryAggregatesCompletedSalesAndBreaksDownByPaymentMethod() throws Exception {
        bootstrap();

        createSale(2, 1, "CASH", 6000);       // 6000
        createSale(1, 0, "CASH", 2000);       // 2000
        createSale(0, 3, "TRANSFER", 6000);   // 6000

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        JsonNode body = objectMapper.readTree(mockMvc.perform(
                        get("/api/v1/reports/daily-summary?date=" + today)
                                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesCount").value(3))
                .andExpect(jsonPath("$.totalRevenue").value(14000))
                .andExpect(jsonPath("$.totalItemsSold").value(7))
                .andReturn().getResponse().getContentAsString());

        Map<String, Long> countByMethod = new HashMap<>();
        Map<String, Long> totalByMethod = new HashMap<>();
        for (JsonNode row : body.get("byPaymentMethod")) {
            countByMethod.put(row.get("paymentMethod").asText(), row.get("count").asLong());
            totalByMethod.put(row.get("paymentMethod").asText(), row.get("total").asLong());
        }
        assertEquals(2L, countByMethod.get("CASH"));
        assertEquals(8000L, totalByMethod.get("CASH"));
        assertEquals(1L, countByMethod.get("TRANSFER"));
        assertEquals(6000L, totalByMethod.get("TRANSFER"));
    }

    @Test
    void cancelledSalesShowUnderCancelledTotalsAndNotInRevenue() throws Exception {
        bootstrap();

        String saleId = createSale(2, 0, "CASH", 4000);
        mockMvc.perform(post("/api/v1/sales/" + saleId + "/cancel")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "test"))))
                .andExpect(status().isOk());

        createSale(1, 0, "CASH", 2000);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        mockMvc.perform(get("/api/v1/reports/daily-summary?date=" + today)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesCount").value(1))
                .andExpect(jsonPath("$.totalRevenue").value(2000))
                .andExpect(jsonPath("$.cancelledCount").value(1))
                .andExpect(jsonPath("$.cancelledAmount").value(4000));
    }

    @Test
    void topProductsOrdersByQtySoldDesc() throws Exception {
        bootstrap();

        createSale(5, 1, "CASH", 12000);
        createSale(2, 4, "CASH", 12000);
        // A sold 7, B sold 5

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        JsonNode body = objectMapper.readTree(mockMvc.perform(
                        get("/api/v1/reports/top-products?from=" + today + "&to=" + today + "&limit=5")
                                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(2))
                .andReturn().getResponse().getContentAsString());

        assertEquals(productAId, body.get("rows").get(0).get("productId").asText());
        assertEquals(7L, body.get("rows").get(0).get("qtySold").asLong());
        assertEquals(productBId, body.get("rows").get(1).get("productId").asText());
        assertEquals(5L, body.get("rows").get(1).get("qtySold").asLong());
    }

    @Test
    void reportsAreOwnerOnly() throws Exception {
        bootstrap();
        mockMvc.perform(get("/api/v1/reports/daily-summary")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reports/daily-summary")
                        .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void topProductsRejectsInvertedDateRange() throws Exception {
        bootstrap();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);
        mockMvc.perform(get("/api/v1/reports/top-products?from=" + today + "&to=" + yesterday)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }
}
