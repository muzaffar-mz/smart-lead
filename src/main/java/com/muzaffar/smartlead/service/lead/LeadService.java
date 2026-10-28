package com.muzaffar.smartlead.service.lead;


import com.muzaffar.smartlead.domain.entity.Lead;
import com.muzaffar.smartlead.domain.entity.enums.LeadType;
import com.muzaffar.smartlead.domain.entity.enums.LeadUrgency;
import com.muzaffar.smartlead.domain.repository.LeadRepository;
import com.muzaffar.smartlead.exceptions.NotFoundException;
import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.service.hgface.model.RawLeadClassificationDTO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;

    @Async("asyncExecutor")
    public void submitLeadAsync(Long messageId, RawLeadClassificationDTO classification) {
        if (!classification.isLead()) {
            log.debug("Message {} is not a lead, nothing to save", messageId);
            return;
        }
        try {
            var lead = new Lead();
            lead.setMessageId(messageId);
            lead.setTitle(classification.title());
            lead.setType(LeadType.fromValue(classification.type()));
            lead.setUrgency(LeadUrgency.fromValue(classification.urgency()));
            lead.setSummary(classification.summary());
            leadRepository.save(lead);
        } catch (Exception e) {
            log.error("Failed to save lead for message {}: {}", messageId, e.getMessage(), e);
        }
    }

    public LeadDTO getById(Long id) {
        return leadRepository.findById(id)
                .map(LeadDTO::from)
                .orElseThrow(() -> new NotFoundException(
                        ErrorConstant.NOT_FOUND,
                        "Lead with id %d not found".formatted(id)));
    }

    public Page<LeadDTO> find(LeadFilter filter) {
        return leadRepository.findAll(toSpecification(filter), filter.toPageable()).map(LeadDTO::from);
    }

    private Specification<Lead> toSpecification(LeadFilter filter) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("type"), filter.type()));
            }
            if (filter.urgency() != null) {
                predicates.add(cb.equal(root.get("urgency"), filter.urgency()));
            }
            if (filter.messageId() != null) {
                predicates.add(cb.equal(root.get("messageId"), filter.messageId()));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateCreated"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateCreated"), filter.to()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
