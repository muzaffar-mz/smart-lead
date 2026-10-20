package com.muzaffar.smartlead.controller;

import com.muzaffar.smartlead.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TestControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String URL = "/api/v1/test/seed-messages";

    @Test
    void withoutAnyKeyIsUnauthorized() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(-30));
    }

    @Test
    void onlyTheTestKeyIsUnauthorized() throws Exception {
        mockMvc.perform(get(URL).header(TEST_KEY_HEADER, TEST_KEY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyTheApiKeyIsForbidden() throws Exception {
        mockMvc.perform(authorizedGet(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(-30))
                .andExpect(jsonPath("$.error_message").value("Missing or invalid X-TEST-KEY header"));
        assertThat(messageRepository.count()).isZero();
    }

    @Test
    void wrongTestKeyIsForbidden() throws Exception {
        mockMvc.perform(authorizedGet(URL).header(TEST_KEY_HEADER, "wrong"))
                .andExpect(status().isForbidden());
        assertThat(messageRepository.count()).isZero();
    }

    @Test
    void bothKeysSubmitTheRequestedNumberOfMessages() throws Exception {
        mockMvc.perform(authorizedGet(URL).param("count", "3").header(TEST_KEY_HEADER, TEST_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value(3));

        assertThat(messageRepository.count()).isEqualTo(3);
    }

    @Test
    void countDefaultsToTen() throws Exception {
        mockMvc.perform(authorizedGet(URL).header(TEST_KEY_HEADER, TEST_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value(10));

        assertThat(messageRepository.count()).isEqualTo(10);
    }

    @Test
    void invalidCountIsABadRequest() throws Exception {
        mockMvc.perform(authorizedGet(URL).param("count", "0").header(TEST_KEY_HEADER, TEST_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_message").value("count must be between 1 and 1000"));
        mockMvc.perform(authorizedGet(URL).param("count", "1001").header(TEST_KEY_HEADER, TEST_KEY))
                .andExpect(status().isBadRequest());
        mockMvc.perform(authorizedGet(URL).param("count", "abc").header(TEST_KEY_HEADER, TEST_KEY))
                .andExpect(status().isBadRequest());
        assertThat(messageRepository.count()).isZero();
    }
}
