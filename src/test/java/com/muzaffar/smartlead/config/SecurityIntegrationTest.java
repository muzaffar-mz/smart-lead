package com.muzaffar.smartlead.config;

import com.muzaffar.smartlead.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends AbstractIntegrationTest {

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }

    @Test
    void apiCallWithoutKeyIsRejectedWithOurErrorBody() throws Exception {
        mockMvc.perform(get("/api/v1/messages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(-30))
                .andExpect(jsonPath("$.error_message").value("Missing or invalid X-API-KEY header"));
    }

    @Test
    void apiCallWithWrongKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/messages").header(API_KEY_HEADER, "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void everyApiEndpointNeedsTheKey() throws Exception {
        mockMvc.perform(get("/api/v1/messages/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/leads")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/leads/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/messages")
                        .contentType("application/json")
                        .content("{\"contact\":\"john@acme.com\",\"message\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiCallWithTheKeyIsAllowed() throws Exception {
        mockMvc.perform(authorizedGet("/api/v1/messages")).andExpect(status().isOk());
    }

    @Test
    void healthCheckNeedsNoKey() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void swaggerNeedsALogin() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerRejectsTheApiKeyAndWrongPasswords() throws Exception {
        mockMvc.perform(get("/v3/api-docs").header(API_KEY_HEADER, API_KEY)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs").header("Authorization", basic(SWAGGER_USER, "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerLoginDoesNotOpenTheApi() throws Exception {
        mockMvc.perform(get("/api/v1/messages").header("Authorization", basic(SWAGGER_USER, SWAGGER_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiDocsAreAvailableWithTheSwaggerLoginAndDescribeTheApiKeyScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs").header("Authorization", basic(SWAGGER_USER, SWAGGER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.components.securitySchemes.apiKey.type").value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.apiKey.in").value("header"))
                .andExpect(jsonPath("$.components.securitySchemes.apiKey.name").value("X-API-KEY"))
                .andExpect(jsonPath("$.paths['/api/v1/messages']").exists());
    }
}
