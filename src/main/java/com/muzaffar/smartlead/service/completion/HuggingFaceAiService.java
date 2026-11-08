package com.muzaffar.smartlead.service.completion;

import com.muzaffar.smartlead.domain.entity.HfCompletion;
import com.muzaffar.smartlead.domain.entity.UsageDetails;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import com.muzaffar.smartlead.domain.repository.HfCompletionRepository;
import com.muzaffar.smartlead.exceptions.RateLimitInfo;
import com.muzaffar.smartlead.service.completion.hgfacerest.HuggingFaceRateLimiter;
import com.muzaffar.smartlead.service.completion.hgfacerest.model.RawLeadClassificationDTO;
import com.muzaffar.smartlead.service.completion.hgfacerest.model.response.LeadClassificationResult;
import com.muzaffar.smartlead.service.completion.hgfacerest.model.response.Usage;
import com.muzaffar.smartlead.service.lead.LeadService;
import com.openai.core.JsonValue;
import com.openai.core.http.Headers;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.completions.CompletionUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class HuggingFaceAiService {

    private static final String REASONING_KEY = "reasoningContent";
    private LocalDateTime blockedUntil;

    private final ChatClient chatClient;
    private final HfCompletionRepository completionRepository;
    private final JsonMapper jsonMapper;
    private final LeadService leadService;
    private final Executor hfExecutor;

    private final AtomicBoolean runQueued = new AtomicBoolean();

    @Value("${hugging-face.batch-size}")
    private Integer batchSize;

    @Value("${hugging-face.permanent-statuses}")
    private Set<Integer> permanentStatuses;


    @Async("hfExecutor")
    public void processAsync(Long completionId) {
        var list = new LinkedHashSet<String>();
        list.
        try {
            if (!canSendRequests()) {
                return;
            }
            completionRepository.findById(completionId)
                    .filter(c -> c.getStatus() == CompletionStatus.PENDING)
                    .ifPresent(this::process);
        } catch (Exception e) {
            log.error("Async processing of completion {} failed: {}", completionId, e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void processPending() {
        // hand the run to the single hf thread; skip this tick if the previous run is still queued or running
        if (!runQueued.compareAndSet(false, true)) {
            return;
        }
        try {
            hfExecutor.execute(this::runPending);
        } catch (RuntimeException e) {
            runQueued.set(false);
            log.error("Could not queue the pending completions run: {}", e.getMessage(), e);
        }
    }

    private void runPending() {
        try {
            processBatch();
        } catch (Exception e) {
            log.error("Pending completions run failed: {}", e.getMessage(), e);
        } finally {
            runQueued.set(false);
        }
    }

    private void processBatch() {
        if (!this.canSendRequests()) {
            log.info("Daily request limit exhausted, skipping run until {}", this.getBlockedUntil());
            return;
        }
        var pending = completionRepository.findByStatusOrderByIdAsc(CompletionStatus.PENDING, Limit.of(batchSize));
        if (pending.isEmpty()) {
            return;
        }
        log.info("Processing {} pending completions", pending.size());

        int processed = 0;
        for (var completion : pending) {
            if (!process(completion)) {
                log.warn("Stopping run after {} of {} completions", processed, pending.size());
                return;
            }
            processed++;
        }
        log.info("Processed all {} pending completions", processed);
    }

    private boolean process(HfCompletion completion) {

        LeadClassificationResult result;
        RawLeadClassificationDTO lead;
        try {
            result = classifyLead(completion.getMessage());
            lead = result.classification();
        } catch (Exception e) {
            if (isPermanent(e)) {
                log.error("Completion {} failed permanently: {}", completion.getId(), e.getMessage(), e);
                markError(completion, Objects.toString(e.getMessage(), e.getClass().getSimpleName()));
                return true;
            }
            log.warn("Completion {} failed, leaving it PENDING: {}", completion.getId(), e.getMessage(), e);
            return false;
        }

        markSuccess(completion, result);
        leadService.submitLeadAsync(completion.getMessageId(), lead);
        return true;
    }

    public LeadClassificationResult classifyLead(String userMessage) {
        ChatResponse response = chatClient.prompt()
                .user(userMessage)
                .call()
                .chatResponse();

        var output = response.getResult().getOutput();
        String content = output.getText();

        RawLeadClassificationDTO classification = jsonMapper.readValue(content, RawLeadClassificationDTO.class);

        return new LeadClassificationResult(
                classification,
                reasoningOf(output.getMetadata().get(REASONING_KEY)),
                toUsage(response.getMetadata().getUsage().getNativeUsage()),
                response.getMetadata().getModel(), "assistant", content);
    }

    private static String reasoningOf(Object reasoning) {
        return reasoning instanceof String s && !s.isBlank() ? s : null;
    }

    private static Usage toUsage(Object nativeUsage) {
        if (!(nativeUsage instanceof CompletionUsage usage)) {
            return null;
        }
        var extra = usage._additionalProperties();
        Integer reasoningTokens = usage.completionTokensDetails()
                .flatMap(d -> d.reasoningTokens())
                .map(Long::intValue)
                .orElse(null);

        return new Usage(
                number(extra.get("queue_time")),
                (int) usage.promptTokens(),
                number(extra.get("prompt_time")),
                (int) usage.completionTokens(),
                number(extra.get("completion_time")),
                (int) usage.totalTokens(),
                number(extra.get("total_time")),
                new Usage.CompletionTokensDetails(reasoningTokens));
    }

    private static Double number(JsonValue value) {
        return value != null && value.asNumber().orElse(null) instanceof Number n ? n.doubleValue() : null;
    }



    private void markSuccess(HfCompletion completion, LeadClassificationResult result) {
        completion.setStatus(CompletionStatus.SUCCESS);
        completion.setModel(result.model());
        completion.setRole(result.role());
        completion.setContent(result.content());
        completion.setReasoning(result.reasoning());
        completion.setUsage(toUsageDetails(result.usage()));

        completionRepository.saveAndFlush(completion);
    }

    private void markError(HfCompletion completion, String errorMessage) {
        completion.setStatus(CompletionStatus.ERROR);
        completion.setMessage(errorMessage);
        completionRepository.saveAndFlush(completion);
    }

    private UsageDetails toUsageDetails(Usage usage) {
        if (usage == null) {
            return null;
        }
        var details = new UsageDetails();
        details.setQueueTime(usage.queueTime());
        details.setPromptTokens(usage.promptTokens());
        details.setPromptTime(usage.promptTime());
        details.setCompletionTokens(usage.completionTokens());
        details.setCompletionTime(usage.completionTime());
        details.setTotalTokens(usage.totalTokens());
        details.setTotalTime(usage.totalTime());
        if (usage.completionTokensDetails() != null) {
            details.setReasoningTokens(usage.completionTokensDetails().reasoningTokens());
        }
        return details;
    }

    public synchronized boolean canSendRequests() {
        if (blockedUntil == null) {
            return true;
        }

        return !LocalDateTime.now().isBefore(blockedUntil);
    }

    public synchronized LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }

    public synchronized void registerFailure(RateLimitInfo rateLimit) {
        //we only care about daily reate limit exhaustion
        if (rateLimit == null || rateLimit.remainingRequestsPerDay() == null || rateLimit.remainingRequestsPerDay() > 0) {
            return;
        }

        Long waitMillis = rateLimit.retryAfterMillis() != null
                ? rateLimit.retryAfterMillis()
                : rateLimit.resetRequestsPerDayMillis();
        if (waitMillis == null) {
            log.warn("Daily request limit exhausted but no retry-after or reset header was sent");
            return;
        }
        blockedUntil = LocalDateTime.now().plus(Duration.ofMillis(waitMillis));
        log.warn("Daily request limit exhausted, no requests until {} (in {})",
                blockedUntil, Duration.ofMillis(waitMillis));
    }



    private boolean isPermanent(Exception e) {
        var serviceException = findServiceException(e);
        if (serviceException != null) {
            int status = serviceException.statusCode();
            if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
                this.registerFailure(RateLimitInfo.from(toHttpHeaders(serviceException.headers())));
            }
            return permanentStatuses.contains(status);
        }
        // the model replied, but with something we cannot map
        return e instanceof JacksonException || e instanceof IllegalArgumentException;
    }

    private static OpenAIServiceException findServiceException(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof OpenAIServiceException s) {
                return s;
            }
        }
        return null;
    }

    private static HttpHeaders toHttpHeaders(Headers headers) {
        var result = new HttpHeaders();
        headers.names().forEach(name -> result.addAll(name, headers.values(name)));
        return result;
    }


}
