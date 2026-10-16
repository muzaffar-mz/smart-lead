package com.muzaffar.smartlead.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    private String email;

    private String phone;

    private String ip;

    @Column(columnDefinition = "text")
    private String message;
}
