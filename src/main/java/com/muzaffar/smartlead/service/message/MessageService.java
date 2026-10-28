package com.muzaffar.smartlead.service.message;


import com.muzaffar.smartlead.domain.entity.Message;
import com.muzaffar.smartlead.domain.repository.MessageRepository;
import com.muzaffar.smartlead.exceptions.NotFoundException;
import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.model.request.MessageRequest;
import com.muzaffar.smartlead.service.hgface.HuggingFaceService;
import com.muzaffar.smartlead.validation.ContactUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
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
public class MessageService {

    private final MessageRepository messageRepository;
    private final HuggingFaceService hfService;

    public void createMessage(MessageRequest request, String remoteAddr) {
        log.info("Creating message from ip: {}", remoteAddr);

        var entity = new Message();
        var contact = request.contact().trim();
        if (ContactUtils.isEmail(contact)) {
            entity.setEmail(contact);
        } else {
            entity.setPhone(ContactUtils.normalizePhone(contact));
        }
        entity.setIp(remoteAddr);
        entity.setMessage(request.message());
        entity = messageRepository.save(entity);

        hfService.submitMessageAsync(MessageDTO.from(entity));
    }

    public MessageDTO get(Long id) {
        return messageRepository.findById(id)
                .map(MessageDTO::from)
                .orElseThrow(
                        () -> new NotFoundException(
                                ErrorConstant.NOT_FOUND,
                                "Message with id %d not found".formatted(id)
                        )
                );
    }

    public Page<MessageDTO> find(MessageFilter filter) {
        return messageRepository.findAll(toSpecification(filter), filter.toPageable()).map(MessageDTO::from);
    }

    private Specification<Message> toSpecification(MessageFilter filter) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (filter.id() != null) {
                predicates.add(cb.equal(root.get("id"), filter.id()));
            }
            if (filter.email() != null && !filter.email().isBlank()) {
                predicates.add(contains(cb, root.get("email"), filter.email().trim().toLowerCase(), true));
            }
            if (filter.phone() != null && !filter.phone().isBlank()) {
                predicates.add(contains(cb, root.get("phone"), ContactUtils.normalizePhone(filter.phone()), false));
            }
            if (filter.ip() != null && !filter.ip().isBlank()) {
                predicates.add(contains(cb, root.get("ip"), filter.ip().trim(), false));
            }
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateCreated"), filter.from()));
            predicates.add(cb.lessThanOrEqualTo(root.get("dateCreated"), filter.to()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** LIKE %value% with the LIKE wildcards in the value escaped, so "%" and "_" match literally. */
    private static Predicate contains(CriteriaBuilder cb, Expression<String> column, String value, boolean ignoreCase) {
        var escaped = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        var pattern = "%" + escaped + "%";
        return cb.like(ignoreCase ? cb.lower(column) : column, pattern, '\\');
    }
}
