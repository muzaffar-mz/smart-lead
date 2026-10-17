package com.muzaffar.smartlead.service.hgface;


import com.muzaffar.smartlead.service.message.model.MessageDTO;

public interface HuggingFaceService {

    void submitMessageAsync(MessageDTO message);
}
