package com.muzaffar.smartlead.controller;

import com.muzaffar.smartlead.AbstractIntegrationTest;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessageControllerIntegrationTest extends AbstractIntegrationTest {

    @Nested
    class Create {

        @Test
        void anEmailContactIsStoredAndThanked() throws Exception {
            mockMvc.perform(authorizedPost("/api/v1/messages",
                            "{\"contact\":\"john@acme.com\",\"message\":\"I would like a demo\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(201))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Thank you")));

            var stored = messageRepository.findAll();
            assertThat(stored).hasSize(1);
            assertThat(stored.getFirst().getEmail()).isEqualTo("john@acme.com");
            assertThat(stored.getFirst().getPhone()).isNull();
            assertThat(stored.getFirst().getMessage()).isEqualTo("I would like a demo");
            assertThat(stored.getFirst().getIp()).isEqualTo("127.0.0.1");
        }

        @Test
        void aPhoneContactIsStoredNormalized() throws Exception {
            mockMvc.perform(authorizedPost("/api/v1/messages",
                            "{\"contact\":\"+1 (555) 014-2000\",\"message\":\"Call me\"}"))
                    .andExpect(status().isCreated());

            var stored = messageRepository.findAll().getFirst();
            assertThat(stored.getPhone()).isEqualTo("+15550142000");
            assertThat(stored.getEmail()).isNull();
        }

        @Test
        void aPendingCompletionIsQueuedForTheMessage() throws Exception {
            mockMvc.perform(authorizedPost("/api/v1/messages",
                            "{\"contact\":\"john@acme.com\",\"message\":\"I would like a demo\"}"))
                    .andExpect(status().isCreated());

            var messageId = messageRepository.findAll().getFirst().getId();
            // the completion is saved by an async task
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var completions = completionRepository.findAll();
                assertThat(completions).hasSize(1);
                assertThat(completions.getFirst().getMessageId()).isEqualTo(messageId);
                assertThat(completions.getFirst().getMessage()).isEqualTo("I would like a demo");
                assertThat(completions.getFirst().getStatus()).isIn(CompletionStatus.PENDING, CompletionStatus.SUCCESS,
                        CompletionStatus.ERROR);
            });
        }

        @Test
        void aBlankContactIsRejected() throws Exception {
            mockMvc.perform(authorizedPost("/api/v1/messages", "{\"contact\":\"  \",\"message\":\"hello\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(-20))
                    .andExpect(jsonPath("$.error_message").value(org.hamcrest.Matchers.containsString("Contact details cannot be blank")));

            assertThat(messageRepository.count()).isZero();
        }

        @Test
        void aContactThatIsNeitherEmailNorPhoneIsRejected() throws Exception {
            mockMvc.perform(authorizedPost("/api/v1/messages", "{\"contact\":\"john@@acme\",\"message\":\"hello\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_message").value("Contact must be a valid email address or phone number"));

            assertThat(messageRepository.count()).isZero();
        }

        @Test
        void aBlankMessageIsRejected() throws Exception {
            mockMvc.perform(authorizedPost("/api/v1/messages", "{\"contact\":\"john@acme.com\",\"message\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_message").value("Message cannot be blank"));

            assertThat(messageRepository.count()).isZero();
        }

        @Test
        void bothProblemsAreReportedTogether() throws Exception {
            mockMvc.perform(authorizedPost("/api/v1/messages", "{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_message").value(org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.containsString("Contact details cannot be blank"),
                            org.hamcrest.Matchers.containsString("Message cannot be blank"))));
        }

        @Test
        void aMissingBodyIsRejected() throws Exception {
            mockMvc.perform(post("/api/v1/messages").header(API_KEY_HEADER, API_KEY).contentType("application/json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_message").value("Request body is missing or malformed"));
        }

        @Test
        void malformedJsonIsRejected() throws Exception {
            mockMvc.perform(authorizedPost("/api/v1/messages", "{\"contact\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_message").value("Request body is missing or malformed"));
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsTheMessage() throws Exception {
            var saved = saveMessage("john@acme.com", null, "10.0.0.1", "hello");

            mockMvc.perform(authorizedGet("/api/v1/messages/{id}", saved.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.payload.id").value(saved.getId()))
                    .andExpect(jsonPath("$.payload.email").value("john@acme.com"))
                    .andExpect(jsonPath("$.payload.ip").value("10.0.0.1"))
                    .andExpect(jsonPath("$.payload.message").value("hello"))
                    .andExpect(jsonPath("$.payload.dateCreated").exists());
        }

        @Test
        void unknownIdIsNotFound() throws Exception {
            mockMvc.perform(authorizedGet("/api/v1/messages/{id}", 999_999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(-10))
                    .andExpect(jsonPath("$.error_message").value(org.hamcrest.Matchers.containsString("999999")));
        }

        @Test
        void aNonNumericIdIsABadRequest() throws Exception {
            mockMvc.perform(authorizedGet("/api/v1/messages/{id}", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(-20));
        }
    }

    @Nested
    class Find {

        @Test
        void withoutFiltersReturnsTheLast30DaysInAPage() throws Exception {
            var recent = saveMessage("recent@acme.com", null, "10.0.0.1", "recent");
            var old = saveMessage("old@acme.com", null, "10.0.0.2", "old");
            setDateCreated("messages", old.getId(), LocalDateTime.now().minusDays(45));

            mockMvc.perform(authorizedGet("/api/v1/messages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.content.length()").value(1))
                    .andExpect(jsonPath("$.payload.content[0].id").value(recent.getId()))
                    .andExpect(jsonPath("$.payload.page").value(0))
                    .andExpect(jsonPath("$.payload.size").value(100))
                    .andExpect(jsonPath("$.payload.total_elements").value(1))
                    .andExpect(jsonPath("$.payload.total_pages").value(1));
        }

        @Test
        void anExplicitFromReachesOlderMessages() throws Exception {
            saveMessage("recent@acme.com", null, "10.0.0.1", "recent");
            var old = saveMessage("old@acme.com", null, "10.0.0.2", "old");
            setDateCreated("messages", old.getId(), LocalDateTime.now().minusDays(45));
            var from = LocalDateTime.now().minusDays(60).withNano(0);

            mockMvc.perform(authorizedGet("/api/v1/messages").param("from", from.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.total_elements").value(2));
        }

        @Test
        void filtersByEmailPartiallyIgnoringCase() throws Exception {
            saveMessage("John.Smith@Acme.com", null, "10.0.0.1", "one");
            saveMessage("maria@globex.io", null, "10.0.0.2", "two");

            mockMvc.perform(authorizedGet("/api/v1/messages").param("email", "SMITH@acme"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.content.length()").value(1))
                    .andExpect(jsonPath("$.payload.content[0].email").value("John.Smith@Acme.com"));
        }

        @Test
        void filtersByPhoneAndIpAndId() throws Exception {
            var wanted = saveMessage(null, "+15550142000", "192.168.1.5", "one");
            saveMessage(null, "+442071234567", "10.0.0.2", "two");

            mockMvc.perform(authorizedGet("/api/v1/messages").param("phone", "(555) 014"))
                    .andExpect(jsonPath("$.payload.content.length()").value(1))
                    .andExpect(jsonPath("$.payload.content[0].id").value(wanted.getId()));
            mockMvc.perform(authorizedGet("/api/v1/messages").param("ip", "192.168"))
                    .andExpect(jsonPath("$.payload.content.length()").value(1))
                    .andExpect(jsonPath("$.payload.content[0].id").value(wanted.getId()));
            mockMvc.perform(authorizedGet("/api/v1/messages").param("id", String.valueOf(wanted.getId())))
                    .andExpect(jsonPath("$.payload.content.length()").value(1));
        }

        @Test
        void pagesAndSorts() throws Exception {
            for (int i = 1; i <= 5; i++) {
                saveMessage("user" + i + "@acme.com", null, "10.0.0." + i, "message " + i);
            }

            mockMvc.perform(authorizedGet("/api/v1/messages")
                            .param("page", "1").param("size", "2").param("sortBy", "email").param("direction", "desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.content.length()").value(2))
                    .andExpect(jsonPath("$.payload.content[0].email").value("user3@acme.com"))
                    .andExpect(jsonPath("$.payload.content[1].email").value("user2@acme.com"))
                    .andExpect(jsonPath("$.payload.page").value(1))
                    .andExpect(jsonPath("$.payload.size").value(2))
                    .andExpect(jsonPath("$.payload.total_elements").value(5))
                    .andExpect(jsonPath("$.payload.total_pages").value(3));
        }

        @Test
        void invalidParametersAreABadRequest() throws Exception {
            mockMvc.perform(authorizedGet("/api/v1/messages").param("size", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(-20))
                    .andExpect(jsonPath("$.error_message").value("size must be between 1 and 5000"));
            mockMvc.perform(authorizedGet("/api/v1/messages").param("page", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_message").value("page must be 0 or greater"));
            mockMvc.perform(authorizedGet("/api/v1/messages").param("direction", "sideways"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_message").value("direction must be asc or desc"));
            mockMvc.perform(authorizedGet("/api/v1/messages").param("sortBy", "message"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(authorizedGet("/api/v1/messages").param("email", "x".repeat(256)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_message").value("email must be at most 255 characters"));
        }
    }
}
