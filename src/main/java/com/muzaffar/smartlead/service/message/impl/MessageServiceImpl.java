package com.muzaffar.smartlead.service.message.impl;


import com.muzaffar.smartlead.domain.entity.Message;
import com.muzaffar.smartlead.domain.repository.MessageRepository;
import com.muzaffar.smartlead.exceptions.NotFoundException;
import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.model.request.MessageRequest;
import com.muzaffar.smartlead.service.hgface.HuggingFaceService;
import com.muzaffar.smartlead.service.message.MessageService;
import com.muzaffar.smartlead.service.message.model.MessageDTO;
import com.muzaffar.smartlead.validation.ContactUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final HuggingFaceService hfService;

    @Override
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

    @Override
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
}
