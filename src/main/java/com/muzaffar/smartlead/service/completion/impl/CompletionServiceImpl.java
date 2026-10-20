package com.muzaffar.smartlead.service.completion.impl;

import com.muzaffar.smartlead.domain.entity.HfCompletion;
import com.muzaffar.smartlead.domain.repository.HfCompletionRepository;
import com.muzaffar.smartlead.exceptions.NotFoundException;
import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.service.completion.CompletionService;
import com.muzaffar.smartlead.service.completion.model.CompletionDTO;
import com.muzaffar.smartlead.service.completion.model.CompletionFilter;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CompletionServiceImpl implements CompletionService {

    private final HfCompletionRepository completionRepository;

    @Override
    public CompletionDTO getById(Long id) {
        return completionRepository.findById(id)
                .map(CompletionDTO::from)
                .orElseThrow(() -> new NotFoundException(
                        ErrorConstant.NOT_FOUND,
                        "Completion with id %d not found".formatted(id)));
    }

    @Override
    public CompletionDTO getByMessageId(Long messageId) {
        return completionRepository.findByMessageId(messageId)
                .map(CompletionDTO::from)
                .orElseThrow(() -> new NotFoundException(
                        ErrorConstant.NOT_FOUND,
                        "Completion for message id %d not found".formatted(messageId)));
    }

    @Override
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
