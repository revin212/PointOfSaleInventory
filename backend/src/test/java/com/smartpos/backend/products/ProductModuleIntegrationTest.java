package com.smartpos.backend.products;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartpos.backend.support.AuthHelper;
import com.smartpos.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductModuleIntegrationTest extends IntegrationTestBase {

    private Map<String, Object> productPayload(String sku, String name, String categoryId) {
        Map<String, Object> m = new HashMap<>();
        m.put("sku", sku);
        m.put("name", name);
        m.put("categoryId", categoryId);
        m.put("unit", "pcs");
        m.put("cost", 10000);
        m.put("price", 15000);
        m.put("barcode", null);
        m.put("lowStockThreshold", 5);
        m.put("active", true);
        return m;
    }

    private String createCategoryAsOwner(String ownerToken, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    @Test
    void ownerCreateProductSucceedsAndDuplicateSkuConflicts() throws Exception {
        String token = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, OWNER_EMAIL, DEFAULT_PASSWORD);
        String categoryId = createCategoryAsOwner(token, "Food");

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(productPayload("SKU-001", "Nasi Goreng", categoryId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.categoryName").value("Food"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(productPayload("sku-001", "Other", categoryId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void listProductsSupportsFilters() throws Exception {
        String token = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, OWNER_EMAIL, DEFAULT_PASSWORD);
        String food = createCategoryAsOwner(token, "Food");
        String drink = createCategoryAsOwner(token, "Drink");

        mockMvc.perform(post("/api/v1/products").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(productPayload("F-1", "Nasi Uduk", food))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/products").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(productPayload("D-1", "Teh Manis", drink))))
                .andExpect(status().isCreated());
        Map<String, Object> inactive = productPayload("D-2", "Kopi Hitam", drink);
        inactive.put("active", false);
        mockMvc.perform(post("/api/v1/products").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(inactive)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/v1/products?active=true").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/products?categoryId=" + drink)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/products?query=uduk")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("F-1"));
    }

    @Test
    void cashierCanListButCannotCreateProduct() throws Exception {
        String ownerToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, OWNER_EMAIL, DEFAULT_PASSWORD);
        String cashierToken = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, CASHIER_EMAIL, DEFAULT_PASSWORD);
        String cat = createCategoryAsOwner(ownerToken, "Food");

        mockMvc.perform(get("/api/v1/products").header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/products").header("Authorization", "Bearer " + cashierToken)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(productPayload("X-1", "Forbidden", cat))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProductSoftRetires() throws Exception {
        String token = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, OWNER_EMAIL, DEFAULT_PASSWORD);
        String cat = createCategoryAsOwner(token, "Food");

        String body = mockMvc.perform(post("/api/v1/products").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(productPayload("A-1", "Something", cat))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(delete("/api/v1/products/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        JsonNode after = objectMapper.readTree(mockMvc.perform(get("/api/v1/products/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assert !after.get("active").asBoolean();
    }
}
