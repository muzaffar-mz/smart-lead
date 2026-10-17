package com.muzaffar.smartlead.domain.repository;

import com.muzaffar.smartlead.domain.entity.HfCompletion;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HfCompletionRepository extends JpaRepository<HfCompletion, Long> {

    List<HfCompletion> findByStatusOrderByIdAsc(CompletionStatus status, Limit limit);
}
