package com.muzaffar.smartlead.controller;

import com.muzaffar.smartlead.AbstractIntegrationTest;
import com.muzaffar.smartlead.domain.entity.UsageDetails;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompletionControllerIntegrationTest extends AbstractIntegrationTest {

    private long saveMessageId(String email) {
        return saveMessage(email, null, "10.0.0.1", "text for " + email).getId();
    }

    @Nested
    class GetById {

        @Test
        void returnsTheCompletion() throws Exception {
            var messageId = saveMessageId("john@acme.com");
            var completion = saveCompletion(messageId, CompletionStatus.SUCCESS, "Need a demo");
            completion.setModel("openai/gpt-oss-120b:groq");
            completion.setContent("{\"isLead\":true}");
            var usage = new UsageDetails();
            usage.setPromptTokens(10);
            usage.setTotalTokens(30);
            completion.setUsage(usage);
            completionRepository.save(completion);

            mockMvc.perform(authorizedGet("/api/v1/completions/{id}", completion.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.id").value(completion.getId()))
                    .andExpect(jsonPath("$.payload.messageId").value(messageId))
                    .andExpect(jsonPath("$.payload.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.payload.message").value("Need a demo"))
                    .andExpect(jsonPath("$.payload.model").value("openai/gpt-oss-120b:groq"))
                    .andExpect(jsonPath("$.payload.content").value("{\"isLead\":true}"))
                    .andExpect(jsonPath("$.payload.usage.promptTokens").value(10))
                    .andExpect(jsonPath("$.payload.usage.totalTokens").value(30));
        }

        @Test
        void unknownIdIsNotFound() throws Exception {
            mockMvc.perform(authorizedGet("/api/v1/completions/{id}", 999_999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(-10))
                    .andExpect(jsonPath("$.error_message").value(org.hamcrest.Matchers.containsString("999999")));
        }
    }

    @Nested
    class GetByMessageId {

        @Test
        void returnsTheCompletionOfThatMessage() throws Exception {
            var other = saveMessageId("other@acme.com");
            var messageId = saveMessageId("john@acme.com");
            saveCompletion(other, CompletionStatus.ERROR, "other text");
            var completion = saveCompletion(messageId, CompletionStatus.PENDING, "Need a demo");

            mockMvc.perform(authorizedGet("/api/v1/completions/message/{messageId}", messageId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.id").value(completion.getId()))
                    .andExpect(jsonPath("$.payload.messageId").value(messageId))
                    .andExpect(jsonPath("$.payload.status").value("PENDING"));
        }

        @Test
        void messageWithoutCompletionIsNotFound() throws Exception {
            var messageId = saveMessageId("john@acme.com");

            mockMvc.perform(authorizedGet("/api/v1/completions/message/{messageId}", messageId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(-10))
                    .andExpect(jsonPath("$.error_message").value(org.hamcrest.Matchers.containsString(String.valueOf(messageId))));
        }
    }

    @Nested
    class Find {

        @Test
        void withoutFiltersReturnsEveryCompletion() throws Exception {
            saveCompletion(saveMessageId("a@acme.com"), CompletionStatus.PENDING, "one");
            saveCompletion(saveMessageId("b@acme.com"), CompletionStatus.SUCCESS, "two");

            mockMvc.perform(authorizedGet("/api/v1/completions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.total_elements").value(2))
                    .andExpect(jsonPath("$.payload.size").value(100));
        }

        @Test
        void filtersByStatusAndMessageId() throws Exception {
            var m1 = saveMessageId("a@acme.com");
            var m2 = saveMessageId("b@acme.com");
            var m3 = saveMessageId("c@acme.com");
            saveCompletion(m1, CompletionStatus.PENDING, "one");
            saveCompletion(m2, CompletionStatus.SUCCESS, "two");
            saveCompletion(m3, CompletionStatus.SUCCESS, "three");

            mockMvc.perform(authorizedGet("/api/v1/completions").param("status", "SUCCESS"))
                    .andExpect(jsonPath("$.payload.total_elements").value(2));
            mockMvc.perform(authorizedGet("/api/v1/completions").param("status", "ERROR"))
                    .andExpect(jsonPath("$.payload.total_elements").value(0));
            mockMvc.perform(authorizedGet("/api/v1/completions").param("messageId", String.valueOf(m3)))
                    .andExpect(jsonPath("$.payload.total_elements").value(1))
                    .andExpect(jsonPath("$.payload.content[0].message").value("three"));
            mockMvc.perform(authorizedGet("/api/v1/completions")
                            .param("status", "SUCCESS").param("messageId", String.valueOf(m1)))
                    .andExpect(jsonPath("$.payload.total_elements").value(0));
        }

        @Test
        void filtersByDateRange() throws Exception {
            saveCompletion(saveMessageId("recent@acme.com"), CompletionStatus.PENDING, "recent");
            var old = saveCompletion(saveMessageId("old@acme.com"), CompletionStatus.PENDING, "old");
            setDateCreated("hf_completions", old.getId(), LocalDateTime.now().minusDays(10));
            var from = LocalDateTime.now().minusDays(1).withNano(0).toString();
            var to = LocalDateTime.now().minusDays(5).withNano(0).toString();

            mockMvc.perform(authorizedGet("/api/v1/completions").param("from", from))
                    .andExpect(jsonPath("$.payload.total_elements").value(1));
            mockMvc.perform(authorizedGet("/api/v1/completions").param("to", to))
                    .andExpect(jsonPath("$.payload.total_elements").value(1))
                    .andExpect(jsonPath("$.payload.content[0].id").value(old.getId()));
        }

        @Test
        void pagesAndSorts() throws Exception {
            for (int i = 1; i <= 5; i++) {
                saveCompletion(saveMessageId("user" + i + "@acme.com"), CompletionStatus.PENDING, "text " + i);
            }

            mockMvc.perform(authorizedGet("/api/v1/completions")
                            .param("page", "2").param("size", "2").param("sortBy", "id").param("direction", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.content.length()").value(1))
                    .andExpect(jsonPath("$.payload.page").value(2))
                    .andExpect(jsonPath("$.payload.total_elements").value(5))
                    .andExpect(jsonPath("$.payload.total_pages").value(3));
        }

        @Test
        void invalidParametersAreABadRequest() throws Exception {
            mockMvc.perform(authorizedGet("/api/v1/completions").param("size", "5001"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_message").value("size must be between 1 and 5000"));
            mockMvc.perform(authorizedGet("/api/v1/completions").param("direction", "sideways"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(authorizedGet("/api/v1/completions").param("sortBy", "content"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(authorizedGet("/api/v1/completions").param("status", "NOT_A_STATUS"))
                    .andExpect(status().isBadRequest());
        }
    }
}
