package com.muzaffar.smartlead.service.message.model;

import com.muzaffar.smartlead.domain.entity.Message;

import java.time.LocalDateTime;

public record MessageDTO(
        Long id,
        String email,
        String phone,
        String ip,
        String message,
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
                entity.getDateCreated(),
                entity.getDateUpdated());
    }
}
