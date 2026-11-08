package com.muzaffar.smartlead.service.completion.hgfacerest.client;


import com.muzaffar.smartlead.service.completion.hgfacerest.model.request.CompletionRequest;
import com.muzaffar.smartlead.service.completion.hgfacerest.model.response.CompletionResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("${hugging-face.completions}")
public interface HuggingFaceClient {

    @PostExchange
    CompletionResponse completion(@RequestBody CompletionRequest request);
}
