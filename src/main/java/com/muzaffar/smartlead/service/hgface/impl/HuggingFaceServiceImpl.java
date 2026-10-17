package com.muzaffar.smartlead.service.hgface.impl;

import com.muzaffar.smartlead.domain.entity.HfCompletion;
import com.muzaffar.smartlead.domain.entity.UsageDetails;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import com.muzaffar.smartlead.domain.repository.HfCompletionRepository;
import com.muzaffar.smartlead.exceptions.HFException;
import com.muzaffar.smartlead.service.hgface.HuggingFaceClient;
import com.muzaffar.smartlead.service.hgface.HuggingFaceService;
import com.muzaffar.smartlead.service.hgface.model.RawLeadClassificationDTO;
import com.muzaffar.smartlead.service.hgface.model.request.CompletionRequest;
import com.muzaffar.smartlead.service.hgface.model.request.JsonSchema;
import com.muzaffar.smartlead.service.hgface.model.request.RequestMessage;
import com.muzaffar.smartlead.service.hgface.model.request.ResponseFormat;
import com.muzaffar.smartlead.service.hgface.model.response.LeadClassificationResult;
import com.muzaffar.smartlead.service.hgface.model.response.Message;
import com.muzaffar.smartlead.service.hgface.model.response.Usage;
import com.muzaffar.smartlead.service.message.model.MessageDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class HuggingFaceServiceImpl implements HuggingFaceService {

    private final HuggingFaceClient client;
    private final HfCompletionRepository completionRepository;
    private final JsonMapper jsonMapper;


    @Value("${hugging-face.batch-size}")
    private Integer batchSize;

    @Value("${hugging-face.permanent-statuses}")
    private Set<Integer> permanentStatuses;

    @Value("${hugging-face.model}")
    private String model;

    @Value("${hugging-face.temperature}")
    private double temperature;

    @Value("${hugging-face.max-tokens}")
    private int maxTokens;

    @Value("${hugging-face.reasoning-effort}")
    private String reasoningEffort;

    @Value("${hugging-face.stream}")
    private boolean stream;

    @Value("classpath:hgface/lead-classification-schema.json")
    private Resource schemaResource;

    @Value("classpath:hgface/lead-system-prompt.txt")
    private Resource systemPromptResource;

    private ResponseFormat responseFormat;

    private String systemPrompt;


    @PostConstruct
    void init() throws IOException {
        try (InputStream in = schemaResource.getInputStream()) {
            Map<String, Object> schema = jsonMapper.readValue(in, new TypeReference<>() {
            });
            responseFormat = new ResponseFormat("json_schema", new JsonSchema("lead_classification", true, schema));
        }
        systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    @Override
    @Async("hfExecutor")
    public void submitMessageAsync(MessageDTO message) {
        try {
            var completion = new HfCompletion();
            completion.setMessageId(message.id());
            completion.setStatus(CompletionStatus.PENDING);
            completion.setMessage(message.message());
            completionRepository.save(completion);
        } catch (Exception e) {
            log.error("Failed to save pending completion for message {}: {}", message.id(), e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void processPending() {
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
        //TODO lead service class async method
        return true;
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

    private void markError(HfCompletion completion, String errorMessage) {
        completion.setStatus(CompletionStatus.ERROR);
        completion.setMessage(errorMessage);
        completionRepository.saveAndFlush(completion);
    }

    private boolean isPermanent(Exception e) {
        if (e instanceof HFException hf) {
            return permanentStatuses.contains(hf.getHttpStatus());
        }
        if (e instanceof RestClientResponseException r) {
            return permanentStatuses.contains(r.getStatusCode().value());
        }
        // the model replied, but with something we cannot map
        return e instanceof JacksonException || e instanceof IllegalArgumentException;
    }

    private LeadClassificationResult classifyLead(String userMessage) {
        CompletionRequest request = new CompletionRequest(
                model,
                List.of(new RequestMessage("system", systemPrompt),
                        new RequestMessage("user", userMessage)),
                temperature,
                maxTokens,
                reasoningEffort,
                stream,
                responseFormat);

        var result = client.completion(request);

        Message message = result.choices().getFirst().message();

        RawLeadClassificationDTO classification = jsonMapper.readValue(message.content(), RawLeadClassificationDTO.class);
        return new LeadClassificationResult(classification, message.reasoning(), result.usage(),
                model, message.role(), message.content());
    }
}
