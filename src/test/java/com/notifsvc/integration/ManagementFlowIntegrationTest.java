package com.notifsvc.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifsvc.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ManagementFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void platformAdminCreatesTenantAndTenantAdminManagesTemplatesAndChannels() throws Exception {
        String adminToken = TestAuth.login(mockMvc, objectMapper, "admin", "ChangeMe123!");

        String tenantResponse = mockMvc.perform(post("/api/tenants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Globex", "globalRateLimitPerMinute", 100))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        long tenantId = objectMapper.readTree(tenantResponse).get("id").asLong();

        mockMvc.perform(post("/api/tenants/" + tenantId + "/admins")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("username", "globex_admin", "password", "GlobexPass123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TENANT_ADMIN"));

        String tenantAdminToken = TestAuth.login(mockMvc, objectMapper, "globex_admin", "GlobexPass123");

        // tenant admin cannot manage tenants
        mockMvc.perform(post("/api/tenants")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "ShouldFail"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/channels")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("channelType", "EMAIL", "enabled", true, "senderIdentity", "noreply@globex.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channelType").value("EMAIL"));

        String templateResponse = mockMvc.perform(post("/api/templates")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "welcome",
                                "channelType", "EMAIL",
                                "subject", "Hi {{firstName}}",
                                "body", "Welcome {{firstName}}!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1))
                .andReturn().getResponse().getContentAsString();
        long templateId = objectMapper.readTree(templateResponse).get("id").asLong();

        String revisedResponse = mockMvc.perform(put("/api/templates/" + templateId + "/revise")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("subject", "Hey {{firstName}}", "body", "Welcome aboard {{firstName}}!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andReturn().getResponse().getContentAsString();

        // listing active templates shows only the latest version
        String listResponse = mockMvc.perform(get("/api/templates")
                        .header("Authorization", "Bearer " + tenantAdminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode list = objectMapper.readTree(listResponse);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("version").asInt()).isEqualTo(2);

        String historyResponse = mockMvc.perform(get("/api/templates/welcome/history")
                        .header("Authorization", "Bearer " + tenantAdminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(historyResponse)).hasSize(2);
    }

    @Test
    void creatingTenantWithDuplicateNameConflicts() throws Exception {
        String adminToken = TestAuth.login(mockMvc, objectMapper, "admin", "ChangeMe123!");

        mockMvc.perform(post("/api/tenants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "DupeCo"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tenants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "DupeCo"))))
                .andExpect(status().isConflict());
    }
}
