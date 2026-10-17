package com.muzaffar.smartlead.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hf_completions")
public class HfCompletion extends BaseEntity {

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompletionStatus status;

    @Column(columnDefinition = "text", nullable = false)
    private String message;

    private String model;

    private String role;

    @Column(columnDefinition = "text")
    private String content;

    @Column(columnDefinition = "text")
    private String reasoning;

    @Embedded
    private UsageDetails usage;
}
