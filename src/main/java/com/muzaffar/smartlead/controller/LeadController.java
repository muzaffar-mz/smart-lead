package com.muzaffar.smartlead.controller;

import com.muzaffar.smartlead.model.BaseResponse;
import com.muzaffar.smartlead.model.PageResponse;
import com.muzaffar.smartlead.service.lead.LeadService;
import com.muzaffar.smartlead.service.lead.LeadDTO;
import com.muzaffar.smartlead.service.lead.LeadFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/leads")
public class LeadController {

    private final LeadService leadService;

    @GetMapping("/{id}")
    public BaseResponse<LeadDTO> get(@PathVariable Long id) {
        return BaseResponse.payload(leadService.getById(id));
    }

    @GetMapping
    public BaseResponse<PageResponse<LeadDTO>> find(@Valid @ModelAttribute LeadFilter filter) {
        var result = leadService.find(filter);
        return BaseResponse.payload(PageResponse.from(result));
    }

}
