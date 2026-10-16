package com.muzaffar.smartlead.service.message;


import com.muzaffar.smartlead.model.request.MessageRequest;
import com.muzaffar.smartlead.service.message.model.MessageDTO;


public interface MessageService {

    void createMessage(MessageRequest request, String remoteAddr);
    MessageDTO get(Long id);

}
