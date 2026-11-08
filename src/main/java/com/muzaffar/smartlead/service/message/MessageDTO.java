package com.muzaffar.smartlead.service.message;

import com.muzaffar.smartlead.domain.entity.Message;
import com.muzaffar.smartlead.domain.entity.enums.MessageStatus;

import java.time.LocalDateTime;

public record MessageDTO(
        Long id,
        String email,
        String phone,
        String ip,
        String message,
        MessageStatus status,
        LocalDateTime dateCreated,
        LocalDateTime dateUpdated
) {

    public static MessageDTO from(Message entity) {
        return new MessageDTO(
                entity.getId(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getIp(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getDateCreated(),
                entity.getDateUpdated());
    }
}
