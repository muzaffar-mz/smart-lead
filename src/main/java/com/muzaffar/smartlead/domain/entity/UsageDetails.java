package com.muzaffar.smartlead.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class UsageDetails {

    @Column(name = "queue_time")
    private Double queueTime;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "prompt_time")
    private Double promptTime;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "completion_time")
    private Double completionTime;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "total_time")
    private Double totalTime;

    @Column(name = "reasoning_tokens")
    private Integer reasoningTokens;
}
