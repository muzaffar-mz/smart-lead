package com.muzaffar.smartlead.service.lead;


import com.muzaffar.smartlead.service.hgface.model.RawLeadClassificationDTO;
import com.muzaffar.smartlead.service.lead.model.LeadDTO;
import com.muzaffar.smartlead.service.lead.model.LeadFilter;
import org.springframework.data.domain.Page;

public interface LeadService {

    void submitLeadAsync(Long messageId, RawLeadClassificationDTO classification);

    LeadDTO getById(Long id);

    Page<LeadDTO> find(LeadFilter filter);
}
