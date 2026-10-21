package com.muzaffar.smartlead.controller;

import com.muzaffar.smartlead.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicControllerIntegrationTest extends AbstractIntegrationTest {

    private static String login(String user, String password) {
        return "{\"login\":\"" + user + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void theSwaggerLoginGivesTheApiKey() throws Exception {
        mockMvc.perform(post("/api/public/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login(SWAGGER_USER, SWAGGER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.apiKey").value(API_KEY));
    }

    @Test
    void aWrongLoginOrPasswordGivesNothing() throws Exception {
        mockMvc.perform(post("/api/public/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login(SWAGGER_USER, "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.payload").doesNotExist());
        mockMvc.perform(post("/api/public/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("wrong", SWAGGER_PASSWORD)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/public/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
