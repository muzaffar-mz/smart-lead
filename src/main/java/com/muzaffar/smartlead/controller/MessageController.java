package com.muzaffar.smartlead.controller;

import com.muzaffar.smartlead.model.BaseResponse;
import com.muzaffar.smartlead.model.request.MessageRequest;
import com.muzaffar.smartlead.service.message.MessageService;
import com.muzaffar.smartlead.service.message.model.MessageDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<Void> create(@Valid @RequestBody MessageRequest request, HttpServletRequest httpRequest) {
        messageService.createMessage(request, httpRequest.getRemoteAddr());
        return BaseResponse.created("Thank you for your interest in our product. We will get in touch with you soon.");
    }

    @GetMapping("/{id}")
    public BaseResponse<MessageDTO> get(@PathVariable Long id) {
        var result = messageService.get(id);
        return BaseResponse.payload(result);
    }

}
