package com.notifsvc.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifsvc.support.Awaiter;
import com.notifsvc.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tenantAdminToken;
    private long templateId;

    private void setUpTenantWithTemplate(String tenantName, String adminUsername) throws Exception {
        String adminToken = TestAuth.login(mockMvc, objectMapper, "admin", "ChangeMe123!");

        String tenantResponse = mockMvc.perform(post("/api/tenants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", tenantName, "globalRateLimitPerMinute", 600))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long tenantId = objectMapper.readTree(tenantResponse).get("id").asLong();

        mockMvc.perform(post("/api/tenants/" + tenantId + "/admins")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("username", adminUsername, "password", "Password123"))))
                .andExpect(status().isCreated());

        tenantAdminToken = TestAuth.login(mockMvc, objectMapper, adminUsername, "Password123");

        mockMvc.perform(put("/api/channels")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("channelType", "IN_APP", "enabled", true, "senderIdentity", "app"))))
                .andExpect(status().isOk());

        String templateResponse = mockMvc.perform(post("/api/templates")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "ping-" + tenantName,
                                "channelType", "IN_APP",
                                "body", "Hi {{name}}!"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        templateId = objectMapper.readTree(templateResponse).get("id").asLong();
    }

    @Test
    void immediateNotificationEventuallyReachesDelivered() throws Exception {
        setUpTenantWithTemplate("DeliverCo", "deliverco_admin");

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Alice");
        String createBody = objectMapper.writeValueAsString(Map.of(
                "templateId", templateId,
                "recipient", "user-1",
                "variables", variables,
                "idempotencyKey", "notif-key-1"));

        String createResponse = mockMvc.perform(post("/api/notifications")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long notificationId = objectMapper.readTree(createResponse).get("id").asLong();

        Awaiter.await(Duration.ofSeconds(10), () -> {
            try {
                String response = mockMvc.perform(get("/api/notifications/" + notificationId)
                                .header("Authorization", "Bearer " + tenantAdminToken))
                        .andReturn().getResponse().getContentAsString();
                String status = objectMapper.readTree(response).get("status").asText();
                return "DELIVERED".equals(status);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        String attemptsResponse = mockMvc.perform(get("/api/notifications/" + notificationId + "/attempts")
                        .header("Authorization", "Bearer " + tenantAdminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode attempts = objectMapper.readTree(attemptsResponse);
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).get("status").asText()).isEqualTo("SUCCESS");
    }

    @Test
    void duplicateIdempotencyKeyDoesNotCreateASecondNotification() throws Exception {
        setUpTenantWithTemplate("IdemCo", "idemco_admin");

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Carl");
        String createBody = objectMapper.writeValueAsString(Map.of(
                "templateId", templateId,
                "recipient", "user-2",
                "variables", variables,
                "idempotencyKey", "shared-key"));

        String firstResponse = mockMvc.perform(post("/api/notifications")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long firstId = objectMapper.readTree(firstResponse).get("id").asLong();

        String secondResponse = mockMvc.perform(post("/api/notifications")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long secondId = objectMapper.readTree(secondResponse).get("id").asLong();

        assertThat(secondId).isEqualTo(firstId);

        String listResponse = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tenantAdminToken))
                .andReturn().getResponse().getContentAsString();
        long matchingCount = 0;
        for (JsonNode node : objectMapper.readTree(listResponse)) {
            if (node.get("idempotencyKey").asText().equals("shared-key")) {
                matchingCount++;
            }
        }
        assertThat(matchingCount).isEqualTo(1);
    }

    @Test
    void scheduledNotificationCanBeCancelledBeforeDispatch() throws Exception {
        setUpTenantWithTemplate("CancelCo", "cancelco_admin");

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Dee");
        String createBody = objectMapper.writeValueAsString(Map.of(
                "templateId", templateId,
                "recipient", "user-3",
                "variables", variables,
                "idempotencyKey", "cancel-key",
                "scheduledAt", java.time.Instant.now().plusSeconds(3600).toString()));

        String createResponse = mockMvc.perform(post("/api/notifications")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long notificationId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(patch("/api/notifications/" + notificationId + "/cancel")
                        .header("Authorization", "Bearer " + tenantAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // cancelling an already-cancelled notification is a conflict, not a silent success
        mockMvc.perform(patch("/api/notifications/" + notificationId + "/cancel")
                        .header("Authorization", "Bearer " + tenantAdminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void missingTemplateVariableIsRejectedAtCreationTime() throws Exception {
        setUpTenantWithTemplate("ValidateCo", "validateco_admin");

        String createBody = objectMapper.writeValueAsString(Map.of(
                "templateId", templateId,
                "recipient", "user-4",
                "variables", Map.of(),
                "idempotencyKey", "missing-var-key"));

        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest());
    }
}
