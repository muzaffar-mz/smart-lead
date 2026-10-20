package com.muzaffar.smartlead.controller;

import com.muzaffar.smartlead.model.BaseResponse;
import com.muzaffar.smartlead.model.PageResponse;
import com.muzaffar.smartlead.service.completion.CompletionService;
import com.muzaffar.smartlead.service.completion.model.CompletionDTO;
import com.muzaffar.smartlead.service.completion.model.CompletionFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/completions")
public class CompletionController {

    private final CompletionService completionService;

    @GetMapping("/{id}")
    public BaseResponse<CompletionDTO> get(@PathVariable Long id) {
        return BaseResponse.payload(completionService.getById(id));
    }

    @GetMapping("/message/{messageId}")
    public BaseResponse<CompletionDTO> getByMessageId(@PathVariable Long messageId) {
        return BaseResponse.payload(completionService.getByMessageId(messageId));
    }

    @GetMapping
    public BaseResponse<PageResponse<CompletionDTO>> find(@Valid @ModelAttribute CompletionFilter filter) {
        var result = completionService.find(filter);
        return BaseResponse.payload(PageResponse.from(result));
    }

}
