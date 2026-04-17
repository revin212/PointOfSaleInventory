package com.smartpos.backend.sales;

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

class SaleFlowIntegrationTest extends IntegrationTestBase {

    @Autowired private StockLedgerService stockLedger;

    private String ownerToken;
    private String cashierToken;
    private String warehouseToken;

    private String categoryId;
    private String supplierId;
    private String productAId;
    private String productBId;

    private void initTokens() throws Exception {
        ownerToken    = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, OWNER_EMAIL,    DEFAULT_PASSWORD);
        cashierToken  = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, CASHIER_EMAIL,  DEFAULT_PASSWORD);
        warehouseToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, WAREHOUSE_EMAIL, DEFAULT_PASSWORD);
    }

    private void seedCatalogAndStock(int qtyA, int qtyB) throws Exception {
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

        productAId = createProduct("SKU-A", "Product A", 1000, 1500);
        productBId = createProduct("SKU-B", "Product B", 2000, 3000);

        if (qtyA > 0 || qtyB > 0) {
            // Create a purchase and receive it fully to seed stock
            Map<String, Object> createReq = Map.of(
                    "supplierId", supplierId,
                    "items", List.of(
                            Map.of("productId", productAId, "qtyOrdered", Math.max(qtyA, 1), "cost", 1000),
                            Map.of("productId", productBId, "qtyOrdered", Math.max(qtyB, 1), "cost", 2000)
                    ));
            String purchaseBody = mockMvc.perform(post("/api/v1/purchases")
                            .header("Authorization", "Bearer " + warehouseToken)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(createReq)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            String purchaseId = objectMapper.readTree(purchaseBody).get("id").asText();
            Map<String, Object> recv = Map.of("items", List.of(
                    Map.of("productId", productAId, "qtyReceived", Math.max(qtyA, 1), "cost", 1000),
                    Map.of("productId", productBId, "qtyReceived", Math.max(qtyB, 1), "cost", 2000)));
            mockMvc.perform(post("/api/v1/purchases/" + purchaseId + "/receive")
                            .header("Authorization", "Bearer " + warehouseToken)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(recv)))
                    .andExpect(status().isOk());
        }
    }

    private String createProduct(String sku, String name, int cost, int price) throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("sku", sku);
        p.put("name", name);
        p.put("categoryId", categoryId);
        p.put("unit", "pcs");
        p.put("cost", cost);
        p.put("price", price);
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
    void cashierCreatesSaleDecreasesStockAndEmitsTotals() throws Exception {
        initTokens();
        seedCatalogAndStock(10, 5);

        Map<String, Object> saleReq = Map.of(
                "items", List.of(
                        Map.of("productId", productAId, "qty", 2, "unitPrice", 1500, "lineDiscount", 0),
                        Map.of("productId", productBId, "qty", 1, "unitPrice", 3000, "lineDiscount", 500)
                ),
                "discount", 0,
                "paymentMethod", "CASH",
                "paidAmount", 10000
        );

        mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(saleReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.invoiceNo").exists())
                .andExpect(jsonPath("$.totals.subtotal").value(5500))
                .andExpect(jsonPath("$.totals.total").value(5500))
                .andExpect(jsonPath("$.totals.paidAmount").value(10000))
                .andExpect(jsonPath("$.totals.changeAmount").value(4500));

        assertEquals(8, stockLedger.onHand(UUID.fromString(productAId)));
        assertEquals(4, stockLedger.onHand(UUID.fromString(productBId)));
    }

    @Test
    void insufficientStockRollsBackEntireSale() throws Exception {
        initTokens();
        seedCatalogAndStock(1, 1); // only 1 of each in stock

        Map<String, Object> saleReq = Map.of(
                "items", List.of(
                        Map.of("productId", productAId, "qty", 1, "unitPrice", 1500, "lineDiscount", 0),
                        Map.of("productId", productBId, "qty", 5, "unitPrice", 3000, "lineDiscount", 0)
                ),
                "discount", 0,
                "paymentMethod", "CASH",
                "paidAmount", 20000
        );

        mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(saleReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        // No partial stock decrement, no sale row written
        assertEquals(1, stockLedger.onHand(UUID.fromString(productAId)));
        assertEquals(1, stockLedger.onHand(UUID.fromString(productBId)));

        mockMvc.perform(get("/api/v1/sales").header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void paidAmountLessThanTotalFailsValidation() throws Exception {
        initTokens();
        seedCatalogAndStock(5, 0);
        Map<String, Object> saleReq = Map.of(
                "items", List.of(Map.of("productId", productAId, "qty", 2, "unitPrice", 1500, "lineDiscount", 0)),
                "discount", 0,
                "paymentMethod", "CASH",
                "paidAmount", 1000
        );
        mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(saleReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void ownerCanCancelSale_writesReversalMovements() throws Exception {
        initTokens();
        seedCatalogAndStock(10, 0);

        Map<String, Object> saleReq = Map.of(
                "items", List.of(Map.of("productId", productAId, "qty", 3, "unitPrice", 1500, "lineDiscount", 0)),
                "discount", 0,
                "paymentMethod", "CASH",
                "paidAmount", 5000
        );
        String createBody = mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(saleReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String saleId = objectMapper.readTree(createBody).get("id").asText();

        assertEquals(7, stockLedger.onHand(UUID.fromString(productAId)));

        mockMvc.perform(post("/api/v1/sales/" + saleId + "/cancel")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "Customer canceled"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertEquals(10, stockLedger.onHand(UUID.fromString(productAId)));

        mockMvc.perform(get("/api/v1/sales/" + saleId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("Customer canceled"));
    }

    @Test
    void cashierCannotCancelSale() throws Exception {
        initTokens();
        seedCatalogAndStock(5, 0);

        Map<String, Object> saleReq = Map.of(
                "items", List.of(Map.of("productId", productAId, "qty", 1, "unitPrice", 1500, "lineDiscount", 0)),
                "discount", 0,
                "paymentMethod", "CASH",
                "paidAmount", 1500
        );
        String body = mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(saleReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String saleId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(post("/api/v1/sales/" + saleId + "/cancel")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void warehouseCannotAccessSalesList() throws Exception {
        initTokens();
        mockMvc.perform(get("/api/v1/sales").header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isForbidden());
    }
}
