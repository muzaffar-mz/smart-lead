package com.muzaffar.smartlead.service.message;


import com.muzaffar.smartlead.model.request.MessageRequest;
import com.muzaffar.smartlead.service.message.model.MessageDTO;
import com.muzaffar.smartlead.service.message.model.MessageFilter;
import org.springframework.data.domain.Page;


public interface MessageService {

    void createMessage(MessageRequest request, String remoteAddr);
    MessageDTO get(Long id);

    Page<MessageDTO> find(MessageFilter filter);

}
