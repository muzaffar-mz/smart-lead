package com.muzaffar.smartlead.controller;

import com.muzaffar.smartlead.AbstractIntegrationTest;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import com.muzaffar.smartlead.exceptions.HFException;
import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.service.hgface.impl.HuggingFaceServiceImpl;
import com.muzaffar.smartlead.service.hgface.model.request.CompletionRequest;
import com.muzaffar.smartlead.service.hgface.model.response.Choice;
import com.muzaffar.smartlead.service.hgface.model.response.CompletionResponse;
import com.muzaffar.smartlead.service.hgface.model.response.HFErrorResponse;
import com.muzaffar.smartlead.service.hgface.model.response.Message;
import com.muzaffar.smartlead.service.hgface.model.response.Usage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole path through the application: POST a message, let the scheduled job classify it (the job is called
 * directly instead of waiting a minute), and read the lead back through the API.
 */
class MessageToLeadFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String LEAD_JSON = "{\"is_lead\":true,\"type\":\"pricing-inquiry\",\"title\":\"Pricing question\","
            + "\"urgency\":\"high\",\"summary\":\"Wants a quote for 50 seats\"}";
    private static final String NOT_A_LEAD_JSON = "{\"is_lead\":false,\"type\":\"other\",\"title\":\"Spam\","
            + "\"urgency\":\"low\",\"summary\":\"Not a real inquiry\"}";

    @Autowired
    private HuggingFaceServiceImpl huggingFaceService;

    private static CompletionResponse modelAnswer(String content) {
        return new CompletionResponse(
                List.of(new Choice(new Message("assistant", content, "thinking"))),
                new Usage(0.1, 10, 0.2, 20, 0.3, 30, 0.6, new Usage.CompletionTokensDetails(5)));
    }

    private long postMessageAndWaitForItsCompletion(String body) throws Exception {
        mockMvc.perform(authorizedPost("/api/v1/messages", body)).andExpect(status().isCreated());
        var messageId = messageRepository.findAll().getFirst().getId();
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(completionRepository.findAll()).hasSize(1));
        return messageId;
    }

    @Test
    void aMessageThatIsALeadEndsUpAsALeadInTheApi() throws Exception {
        when(huggingFaceClient.completion(any(CompletionRequest.class))).thenReturn(modelAnswer(LEAD_JSON));
        var messageId = postMessageAndWaitForItsCompletion(
                "{\"contact\":\"john@acme.com\",\"message\":\"How much is it for 50 seats?\"}");

        huggingFaceService.processPending();

        var completion = completionRepository.findAll().getFirst();
        assertThat(completion.getStatus()).isEqualTo(CompletionStatus.SUCCESS);
        assertThat(completion.getContent()).isEqualTo(LEAD_JSON);
        assertThat(completion.getUsage().getTotalTokens()).isEqualTo(30);

        // the lead is saved by an async task
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(leadRepository.count()).isEqualTo(1));
        mockMvc.perform(authorizedGet("/api/v1/leads").param("messageId", String.valueOf(messageId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.total_elements").value(1))
                .andExpect(jsonPath("$.payload.content[0].title").value("Pricing question"))
                .andExpect(jsonPath("$.payload.content[0].type").value("PRICING_INQUIRY"))
                .andExpect(jsonPath("$.payload.content[0].urgency").value("HIGH"))
                .andExpect(jsonPath("$.payload.content[0].summary").value("Wants a quote for 50 seats"));
    }

    @Test
    void aMessageThatIsNotALeadIsCompletedButNoLeadIsCreated() throws Exception {
        when(huggingFaceClient.completion(any(CompletionRequest.class))).thenReturn(modelAnswer(NOT_A_LEAD_JSON));
        postMessageAndWaitForItsCompletion("{\"contact\":\"+15550142000\",\"message\":\"buy cheap watches\"}");

        huggingFaceService.processPending();

        assertThat(completionRepository.findAll().getFirst().getStatus()).isEqualTo(CompletionStatus.SUCCESS);
        // give the async lead task a moment, then make sure nothing was created
        Thread.sleep(500);
        assertThat(leadRepository.count()).isZero();
    }

    @Test
    void aMessageTheModelRejectsIsMarkedAsErrorAndNoLeadIsCreated() throws Exception {
        var rejected = new HFException(ErrorConstant.HUGGING_FACE_EXCEPTION,
                new HFErrorResponse(new HFErrorResponse.HFErrorBody("input too long", "invalid_request", "400")), 400, null);
        when(huggingFaceClient.completion(any(CompletionRequest.class))).thenThrow(rejected);
        postMessageAndWaitForItsCompletion("{\"contact\":\"john@acme.com\",\"message\":\"hello\"}");

        huggingFaceService.processPending();

        var completion = completionRepository.findAll().getFirst();
        assertThat(completion.getStatus()).isEqualTo(CompletionStatus.ERROR);
        assertThat(completion.getMessage()).isEqualTo("input too long");
        assertThat(leadRepository.count()).isZero();
    }

    @Test
    void aTemporaryFailureLeavesTheMessagePendingForTheNextRun() throws Exception {
        var unavailable = new HFException(ErrorConstant.HUGGING_FACE_EXCEPTION,
                new HFErrorResponse(new HFErrorResponse.HFErrorBody("try later", "server_error", "503")), 503, null);
        when(huggingFaceClient.completion(any(CompletionRequest.class))).thenThrow(unavailable).thenReturn(modelAnswer(LEAD_JSON));
        postMessageAndWaitForItsCompletion("{\"contact\":\"john@acme.com\",\"message\":\"hello\"}");

        huggingFaceService.processPending();
        assertThat(completionRepository.findAll().getFirst().getStatus()).isEqualTo(CompletionStatus.PENDING);

        huggingFaceService.processPending();
        assertThat(completionRepository.findAll().getFirst().getStatus()).isEqualTo(CompletionStatus.SUCCESS);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(leadRepository.count()).isEqualTo(1));
    }
}
