package com.muzaffar.smartlead.service.completion;

import com.muzaffar.smartlead.domain.entity.HfCompletion;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import com.muzaffar.smartlead.domain.repository.HfCompletionRepository;
import com.muzaffar.smartlead.exceptions.NotFoundException;
import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.service.message.MessageDTO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompletionService {

    private final HfCompletionRepository completionRepository;
    private final HuggingFaceAiService aiService;

    public boolean submitMessage(MessageDTO message) {
        try {
            var completion = new HfCompletion();
            completion.setMessageId(message.id());
            completion.setStatus(CompletionStatus.PENDING);
            completion.setMessage(message.message());
            completion = completionRepository.save(completion);
            aiService.processAsync(completion.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to save pending completion for message {}: {}", message.id(), e.getMessage(), e);
            return false;
        }
    }

    public CompletionDTO getById(Long id) {
        return completionRepository.findById(id)
                .map(CompletionDTO::from)
                .orElseThrow(() -> new NotFoundException(
                        ErrorConstant.NOT_FOUND,
                        "Completion with id %d not found".formatted(id)));
    }

    public CompletionDTO getByMessageId(Long messageId) {
        return completionRepository.findByMessageId(messageId)
                .map(CompletionDTO::from)
                .orElseThrow(() -> new NotFoundException(
                        ErrorConstant.NOT_FOUND,
                        "Completion for message id %d not found".formatted(messageId)));
    }

    public Page<CompletionDTO> find(CompletionFilter filter) {
        return completionRepository.findAll(toSpecification(filter), filter.toPageable()).map(CompletionDTO::from);
    }

    private Specification<HfCompletion> toSpecification(CompletionFilter filter) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
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
