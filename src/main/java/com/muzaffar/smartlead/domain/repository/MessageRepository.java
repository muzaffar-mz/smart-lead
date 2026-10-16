package com.muzaffar.smartlead.domain.repository;

import com.muzaffar.smartlead.domain.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
