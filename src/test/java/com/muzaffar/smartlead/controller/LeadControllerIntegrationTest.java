package com.muzaffar.smartlead.controller;

import com.muzaffar.smartlead.AbstractIntegrationTest;
import com.muzaffar.smartlead.domain.entity.enums.LeadType;
import com.muzaffar.smartlead.domain.entity.enums.LeadUrgency;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeadControllerIntegrationTest extends AbstractIntegrationTest {

    private long saveMessageId(String email) {
        return saveMessage(email, null, "10.0.0.1", "text for " + email).getId();
    }

    @Nested
    class GetById {

        @Test
        void returnsTheLead() throws Exception {
            var messageId = saveMessageId("john@acme.com");
            var lead = saveLead(messageId, LeadType.PRICING_INQUIRY, LeadUrgency.HIGH);

            mockMvc.perform(authorizedGet("/api/v1/leads/{id}", lead.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.id").value(lead.getId()))
                    .andExpect(jsonPath("$.payload.type").value("PRICING_INQUIRY"))
                    .andExpect(jsonPath("$.payload.urgency").value("HIGH"))
                    .andExpect(jsonPath("$.payload.messageId").value(messageId))
                    .andExpect(jsonPath("$.payload.title").value("Lead for message " + messageId));
        }

        @Test
        void unknownIdIsNotFound() throws Exception {
            mockMvc.perform(authorizedGet("/api/v1/leads/{id}", 999_999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(-10))
                    .andExpect(jsonPath("$.error_message").value(org.hamcrest.Matchers.containsString("999999")));
        }
    }

    @Nested
    class Find {

        @Test
        void withoutFiltersReturnsEveryLead() throws Exception {
            saveLead(saveMessageId("a@acme.com"), LeadType.SUPPORT, LeadUrgency.LOW);
            saveLead(saveMessageId("b@acme.com"), LeadType.OTHER, LeadUrgency.HIGH);

            mockMvc.perform(authorizedGet("/api/v1/leads"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.total_elements").value(2))
                    .andExpect(jsonPath("$.payload.size").value(100));
        }

        @Test
        void filtersByTypeUrgencyAndMessageId() throws Exception {
            var m1 = saveMessageId("a@acme.com");
            var m2 = saveMessageId("b@acme.com");
            var m3 = saveMessageId("c@acme.com");
            saveLead(m1, LeadType.SUPPORT, LeadUrgency.LOW);
            saveLead(m2, LeadType.SUPPORT, LeadUrgency.HIGH);
            saveLead(m3, LeadType.OTHER, LeadUrgency.HIGH);

            mockMvc.perform(authorizedGet("/api/v1/leads").param("type", "SUPPORT"))
                    .andExpect(jsonPath("$.payload.total_elements").value(2));
            mockMvc.perform(authorizedGet("/api/v1/leads").param("urgency", "HIGH"))
                    .andExpect(jsonPath("$.payload.total_elements").value(2));
            mockMvc.perform(authorizedGet("/api/v1/leads").param("type", "SUPPORT").param("urgency", "HIGH"))
                    .andExpect(jsonPath("$.payload.total_elements").value(1))
                    .andExpect(jsonPath("$.payload.content[0].messageId").value(m2));
            mockMvc.perform(authorizedGet("/api/v1/leads").param("messageId", String.valueOf(m3)))
                    .andExpect(jsonPath("$.payload.total_elements").value(1))
                    .andExpect(jsonPath("$.payload.content[0].type").value("OTHER"));
        }

        @Test
        void filtersByDateRange() throws Exception {
            saveLead(saveMessageId("recent@acme.com"), LeadType.SUPPORT, LeadUrgency.LOW);
            var old = saveLead(saveMessageId("old@acme.com"), LeadType.SUPPORT, LeadUrgency.LOW);
            setDateCreated("leads", old.getId(), LocalDateTime.now().minusDays(10));
            var from = LocalDateTime.now().minusDays(1).withNano(0).toString();
            var to = LocalDateTime.now().minusDays(5).withNano(0).toString();

            mockMvc.perform(authorizedGet("/api/v1/leads").param("from", from))
                    .andExpect(jsonPath("$.payload.total_elements").value(1));
            mockMvc.perform(authorizedGet("/api/v1/leads").param("to", to))
                    .andExpect(jsonPath("$.payload.total_elements").value(1))
                    .andExpect(jsonPath("$.payload.content[0].id").value(old.getId()));
        }

        @Test
        void pagesAndSorts() throws Exception {
            for (int i = 1; i <= 5; i++) {
                saveLead(saveMessageId("user" + i + "@acme.com"), LeadType.SUPPORT, LeadUrgency.LOW);
            }

            mockMvc.perform(authorizedGet("/api/v1/leads")
                            .param("page", "2").param("size", "2").param("sortBy", "id").param("direction", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.content.length()").value(1))
                    .andExpect(jsonPath("$.payload.page").value(2))
                    .andExpect(jsonPath("$.payload.total_elements").value(5))
                    .andExpect(jsonPath("$.payload.total_pages").value(3));
        }

        @Test
        void invalidParametersAreABadRequest() throws Exception {
            mockMvc.perform(authorizedGet("/api/v1/leads").param("size", "5001"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_message").value("size must be between 1 and 5000"));
            mockMvc.perform(authorizedGet("/api/v1/leads").param("direction", "sideways"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(authorizedGet("/api/v1/leads").param("sortBy", "summary"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(authorizedGet("/api/v1/leads").param("type", "NOT_A_TYPE"))
                    .andExpect(status().isBadRequest());
        }
    }
}
