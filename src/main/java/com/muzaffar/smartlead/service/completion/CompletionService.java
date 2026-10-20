package com.muzaffar.smartlead.service.completion;

import com.muzaffar.smartlead.service.completion.model.CompletionDTO;
import com.muzaffar.smartlead.service.completion.model.CompletionFilter;
import org.springframework.data.domain.Page;

public interface CompletionService {

    CompletionDTO getById(Long id);

    CompletionDTO getByMessageId(Long messageId);

    Page<CompletionDTO> find(CompletionFilter filter);
}
