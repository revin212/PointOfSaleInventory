package com.smartpos.backend.categories;

import com.smartpos.backend.support.AuthHelper;
import com.smartpos.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryModuleIntegrationTest extends IntegrationTestBase {

    @Test
    void warehouseCanCrudCategory_andDuplicateNameConflicts() throws Exception {
        String token = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, WAREHOUSE_EMAIL, DEFAULT_PASSWORD);

        String createBody = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", "Beverages"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Beverages"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createBody).get("id").asText();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", "beverages"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        mockMvc.perform(put("/api/v1/categories/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", "Drinks"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Drinks"));

        mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(delete("/api/v1/categories/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cashierCannotCreateCategoryButCanList() throws Exception {
        String token = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, CASHIER_EMAIL, DEFAULT_PASSWORD);

        mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", "Snacks"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createCategoryValidationFailsOnBlankName() throws Exception {
        String token = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, OWNER_EMAIL, DEFAULT_PASSWORD);

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
