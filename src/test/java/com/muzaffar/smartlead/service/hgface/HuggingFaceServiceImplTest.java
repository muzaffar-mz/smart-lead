package com.muzaffar.smartlead.service.hgface;

import com.muzaffar.smartlead.domain.entity.HfCompletion;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import com.muzaffar.smartlead.domain.repository.HfCompletionRepository;
import com.muzaffar.smartlead.exceptions.HFException;
import com.muzaffar.smartlead.exceptions.RateLimitInfo;
import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.service.hgface.client.HuggingFaceClient;
import com.muzaffar.smartlead.service.hgface.impl.HuggingFaceServiceImpl;
import com.muzaffar.smartlead.service.hgface.model.RawLeadClassificationDTO;
import com.muzaffar.smartlead.service.hgface.model.request.CompletionRequest;
import com.muzaffar.smartlead.service.hgface.model.response.Choice;
import com.muzaffar.smartlead.service.hgface.model.response.CompletionResponse;
import com.muzaffar.smartlead.service.hgface.model.response.HFErrorResponse;
import com.muzaffar.smartlead.service.hgface.model.response.Message;
import com.muzaffar.smartlead.service.hgface.model.response.Usage;
import com.muzaffar.smartlead.service.lead.LeadService;
import com.muzaffar.smartlead.service.message.model.MessageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HuggingFaceServiceImplTest {

    private static final String LEAD_JSON =
            "{\"is_lead\":true,\"type\":\"demo-request\",\"title\":\"Demo\",\"urgency\":\"high\",\"summary\":\"Wants a demo\"}";

    @Mock
    private HuggingFaceClient client;
    @Mock
    private HfCompletionRepository completionRepository;
    @Mock
    private LeadService leadService;
    @Mock
    private HuggingFaceRateLimiter rateLimiter;

    private HuggingFaceServiceImpl underTest;

    @BeforeEach
    void setUp() {
        underTest = new HuggingFaceServiceImpl(client, completionRepository, JsonMapper.builder().build(), leadService, rateLimiter);
        ReflectionTestUtils.setField(underTest, "batchSize", 30);
        ReflectionTestUtils.setField(underTest, "permanentStatuses", Set.of(400, 413, 422));
        ReflectionTestUtils.setField(underTest, "model", "test-model");
        ReflectionTestUtils.setField(underTest, "systemPrompt", "classify this");
    }

    @Test
    void submitMessageAsyncSavesAPendingCompletion() {
        //given
        var id = 11L;
        var email = "john@acme.com";
        var ip = "10.0.0.1";
        var message = "Need a demo";
        var dto = new MessageDTO(id, email, null, ip, message, LocalDateTime.now(), LocalDateTime.now());

        //when
        underTest.submitMessageAsync(dto);

        //then
        var captor = ArgumentCaptor.forClass(HfCompletion.class);
        verify(completionRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageId()).isEqualTo(id);
        assertThat(captor.getValue().getStatus()).isEqualTo(CompletionStatus.PENDING);
        assertThat(captor.getValue().getMessage()).isEqualTo(message);
    }

    @Test
    void submitMessageAsyncSwallowsRepositoryFailures() {
        //given
        var id = 11L;
        var email = "john@acme.com";
        var ip = "10.0.0.1";
        var message = "Need a demo";
        var dto = new MessageDTO(id, email, null, ip, message, LocalDateTime.now(), LocalDateTime.now());

        when(completionRepository.save(any())).thenThrow(new IllegalStateException("db down"));

        //when
        //then
        assertThatCode(() -> underTest.submitMessageAsync(dto)).doesNotThrowAnyException();
    }

    @Test
    void processPendingDoesNothingWhileTheDailyLimitIsExhausted() {
        //given
        when(rateLimiter.canSendRequests()).thenReturn(false);
        when(rateLimiter.getBlockedUntil()).thenReturn(LocalDateTime.now().plusHours(1));

        //when
        underTest.processPending();

        //then
        verifyNoInteractions(completionRepository, client, leadService);
    }

    @Test
    void processPendingDoesNothingWhenNothingIsPending() {
        //given
        when(rateLimiter.canSendRequests()).thenReturn(true);
        when(completionRepository.findByStatusOrderByIdAsc(eq(CompletionStatus.PENDING), any()))
                .thenReturn(List.of());

        //when
        underTest.processPending();

        //then
        verifyNoInteractions(client, leadService);
    }

    @Test
    void processPendingFetchesOneBatchOfPendingCompletions() {
        //given
        when(rateLimiter.canSendRequests()).thenReturn(true);
        when(completionRepository.findByStatusOrderByIdAsc(eq(CompletionStatus.PENDING), any()))
                .thenReturn(List.of());

        //when
        underTest.processPending();

        //then
        verify(completionRepository).findByStatusOrderByIdAsc(CompletionStatus.PENDING, Limit.of(30));
    }

    @Test
    void processPendingMarksSuccessAndSubmitsTheLead() {
        //given
        var completion = pending(1, 100, "Need a demo");
        when(rateLimiter.canSendRequests()).thenReturn(true);
        when(completionRepository.findByStatusOrderByIdAsc(eq(CompletionStatus.PENDING), any(Limit.class)))
                .thenReturn(List.of(completion));
        when(client.completion(any())).thenReturn(response(LEAD_JSON));


        //when
        underTest.processPending();

        //then
        assertThat(completion.getStatus()).isEqualTo(CompletionStatus.SUCCESS);
        assertThat(completion.getModel()).isEqualTo("test-model");
        assertThat(completion.getRole()).isEqualTo("assistant");
        assertThat(completion.getContent()).isEqualTo(LEAD_JSON);
        assertThat(completion.getReasoning()).isEqualTo("thinking");
        assertThat(completion.getUsage().getTotalTokens()).isEqualTo(30);
        assertThat(completion.getUsage().getReasoningTokens()).isEqualTo(5);

        var order = inOrder(completionRepository, leadService);
        order.verify(completionRepository).saveAndFlush(completion);
        var leadCaptor = ArgumentCaptor.forClass(RawLeadClassificationDTO.class);
        order.verify(leadService).submitLeadAsync(eq(100L), leadCaptor.capture());
        assertThat(leadCaptor.getValue().isLead()).isTrue();
        assertThat(leadCaptor.getValue().type()).isEqualTo("demo-request");
        assertThat(leadCaptor.getValue().urgency()).isEqualTo("high");
    }

    @Test
    void processPendingSendsTheSystemPromptAndTheMessageToTheModel() {
        //given
        var completion = pending(1, 100, "Need a demo");
        when(rateLimiter.canSendRequests()).thenReturn(true);
        when(completionRepository.findByStatusOrderByIdAsc(eq(CompletionStatus.PENDING), any(Limit.class)))
                .thenReturn(List.of(completion));
        when(client.completion(any())).thenReturn(response(LEAD_JSON));

        //when
        underTest.processPending();

        //then
        var captor = ArgumentCaptor.forClass(CompletionRequest.class);
        verify(client).completion(captor.capture());
        var request = captor.getValue();
        assertThat(request.model()).isEqualTo("test-model");
        assertThat(request.messages()).extracting("role", "content")
                .containsExactly(
                        tuple("system", "classify this"),
                        tuple("user", "Need a demo"));
    }

    @Test
    void processPendingMarksPermanentFailuresAsErrorAndCarriesOn() {
        //given
        var rejected = pending(1, 100, "bad input");
        var good = pending(2, 200, "Need a demo");
        when(rateLimiter.canSendRequests()).thenReturn(true);
        when(completionRepository.findByStatusOrderByIdAsc(eq(CompletionStatus.PENDING), any(Limit.class)))
                .thenReturn(List.of(rejected, good));
        when(client.completion(any()))
                .thenThrow(hfException(400, null))
                .thenReturn(response(LEAD_JSON));

        //when
        underTest.processPending();

        //then
        assertThat(rejected.getStatus()).isEqualTo(CompletionStatus.ERROR);
        assertThat(rejected.getMessage()).isEqualTo("boom");
        verify(completionRepository).saveAndFlush(rejected);
        assertThat(good.getStatus()).isEqualTo(CompletionStatus.SUCCESS);
        verify(completionRepository).saveAndFlush(good);
        verify(leadService).submitLeadAsync(eq(200L), any());
        verify(leadService, never()).submitLeadAsync(eq(100L), any());
    }

    @Test
    void processPendingMarksUnparseableModelAnswersAsError() {
        //given
        var completion = pending(1, 100, "Need a demo");
        when(rateLimiter.canSendRequests()).thenReturn(true);
        when(completionRepository.findByStatusOrderByIdAsc(eq(CompletionStatus.PENDING), any(Limit.class)))
                .thenReturn(List.of(completion));

        when(client.completion(any())).thenReturn(response("this is not json"));

        //when
        underTest.processPending();

        //then
        assertThat(completion.getStatus()).isEqualTo(CompletionStatus.ERROR);
        verify(completionRepository).saveAndFlush(completion);
        verifyNoInteractions(leadService);
    }

    @Test
    void processPendingStopsOnTransientFailureAndLeavesTheRestPending() {

        //given
        var first = pending(1, 100, "one");
        var second = pending(2, 200, "two");
        var third = pending(3, 300, "three");

        when(rateLimiter.canSendRequests()).thenReturn(true);
        when(completionRepository.findByStatusOrderByIdAsc(eq(CompletionStatus.PENDING), any(Limit.class)))
                .thenReturn(List.of(first, second, third));

        var rateLimit = new RateLimitInfo(60_000L, null, null, 0L, null, null, null);
        when(client.completion(any()))
                .thenReturn(response(LEAD_JSON))
                .thenThrow(hfException(429, rateLimit));

        //when
        underTest.processPending();

        //then
        assertThat(first.getStatus()).isEqualTo(CompletionStatus.SUCCESS);
        assertThat(second.getStatus()).isEqualTo(CompletionStatus.PENDING);
        assertThat(third.getStatus()).isEqualTo(CompletionStatus.PENDING);
        verify(client, times(2)).completion(any());
        verify(completionRepository).saveAndFlush(first);
        verify(leadService).submitLeadAsync(eq(100L), any());
        verify(completionRepository, never()).saveAndFlush(second);
        verify(leadService, never()).submitLeadAsync(eq(200L), any());
        verify(completionRepository, never()).saveAndFlush(third);
        verify(leadService, never()).submitLeadAsync(eq(300L), any());
    }

    @Test
    void processPendingReportsTooManyRequestsToTheRateLimiter() {
        //given
        var completion = pending(1, 100, "one");
        when(rateLimiter.canSendRequests()).thenReturn(true);
        when(completionRepository.findByStatusOrderByIdAsc(eq(CompletionStatus.PENDING), any(Limit.class)))
                .thenReturn(List.of(completion));
        var rateLimit = new RateLimitInfo(60_000L, null, null, 0L, null, null, null);
        when(client.completion(any(CompletionRequest.class))).thenThrow(hfException(429, rateLimit));

        //when
        underTest.processPending();

        //then
        verify(rateLimiter).registerFailure(rateLimit);
        verify(completionRepository, never()).saveAndFlush(completion);
        verify(leadService, never()).submitLeadAsync(eq(100L), any());

    }

    @Test
    void processPendingTreatsServerErrorsAsTransient() {
        //given
        var completion = pending(1, 100, "one");
        when(rateLimiter.canSendRequests()).thenReturn(true);
        when(completionRepository.findByStatusOrderByIdAsc(eq(CompletionStatus.PENDING), any(Limit.class)))
                .thenReturn(List.of(completion));
        when(client.completion(any(CompletionRequest.class))).thenThrow(hfException(503, null));

        //when
        underTest.processPending();

        //then
        assertThat(completion.getStatus()).isEqualTo(CompletionStatus.PENDING);
        verify(rateLimiter, never()).registerFailure(any());
        verify(completionRepository, never()).saveAndFlush(any());
        verify(leadService, never()).submitLeadAsync(eq(100L), any());
    }

    private HfCompletion pending(long id, long messageId, String text) {
        var completion = new HfCompletion();
        completion.setId(id);
        completion.setMessageId(messageId);
        completion.setStatus(CompletionStatus.PENDING);
        completion.setMessage(text);
        return completion;
    }

    private static CompletionResponse response(String content) {
        return new CompletionResponse(
                List.of(new Choice(new Message("assistant", content, "thinking"))),
                new Usage(0.1, 10, 0.2, 20, 0.3, 30, 0.6, new Usage.CompletionTokensDetails(5)));
    }


    private static HFException hfException(int status, RateLimitInfo rateLimit) {
        return new HFException(ErrorConstant.HUGGING_FACE_EXCEPTION,
                new HFErrorResponse(new HFErrorResponse.HFErrorBody("boom", "error", "code")), status, rateLimit);
    }
}
