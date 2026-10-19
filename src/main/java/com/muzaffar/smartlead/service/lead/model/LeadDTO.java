package com.muzaffar.smartlead.service.lead.model;

import com.muzaffar.smartlead.domain.entity.Lead;
import com.muzaffar.smartlead.domain.entity.enums.LeadType;
import com.muzaffar.smartlead.domain.entity.enums.LeadUrgency;

import java.time.LocalDateTime;

public record LeadDTO(
        Long id,
        String title,
        LeadType type,
        LeadUrgency urgency,
        String summary,
        Long messageId,
        LocalDateTime dateCreated,
        LocalDateTime dateUpdated
) {

    public static LeadDTO from(Lead entity) {
        return new LeadDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getType(),
                entity.getUrgency(),
                entity.getSummary(),
                entity.getMessageId(),
                entity.getDateCreated(),
                entity.getDateUpdated());
    }
}
