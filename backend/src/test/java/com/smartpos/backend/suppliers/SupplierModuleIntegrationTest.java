package com.smartpos.backend.suppliers;

import com.smartpos.backend.support.AuthHelper;
import com.smartpos.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SupplierModuleIntegrationTest extends IntegrationTestBase {

    @Test
    void ownerCanCrudSupplier() throws Exception {
        String token = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, OWNER_EMAIL, DEFAULT_PASSWORD);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "PT Sumber Rejeki");
        payload.put("phone", "081234567890");
        payload.put("address", "Jl. Merdeka No. 1");

        String createBody = mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("PT Sumber Rejeki"))
                .andExpect(jsonPath("$.phone").value("081234567890"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createBody).get("id").asText();

        payload.put("name", "PT Sumber Rejeki Abadi");
        mockMvc.perform(put("/api/v1/suppliers/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PT Sumber Rejeki Abadi"));

        mockMvc.perform(get("/api/v1/suppliers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(delete("/api/v1/suppliers/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cashierCannotAccessSuppliers() throws Exception {
        String token = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, CASHIER_EMAIL, DEFAULT_PASSWORD);
        mockMvc.perform(get("/api/v1/suppliers").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void supplierMissingRequiredFieldFailsValidation() throws Exception {
        String token = AuthHelper.loginAndGetAccessToken(mockMvc, objectMapper, WAREHOUSE_EMAIL, DEFAULT_PASSWORD);
        mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("phone", "0812"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
