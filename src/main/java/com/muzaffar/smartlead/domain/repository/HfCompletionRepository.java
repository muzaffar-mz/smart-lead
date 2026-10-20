package com.muzaffar.smartlead.domain.repository;

import com.muzaffar.smartlead.domain.entity.HfCompletion;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface HfCompletionRepository extends JpaRepository<HfCompletion, Long>, JpaSpecificationExecutor<HfCompletion> {

    List<HfCompletion> findByStatusOrderByIdAsc(CompletionStatus status, Limit limit);

    Optional<HfCompletion> findByMessageId(Long messageId);
}
