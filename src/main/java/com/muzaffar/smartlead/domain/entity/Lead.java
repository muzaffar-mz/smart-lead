package com.muzaffar.smartlead.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import com.muzaffar.smartlead.domain.entity.enums.LeadType;
import com.muzaffar.smartlead.domain.entity.enums.LeadUrgency;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "leads")
public class Lead extends BaseEntity {

    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadUrgency urgency;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "message_id", nullable = false)
    private Long messageId;
}
