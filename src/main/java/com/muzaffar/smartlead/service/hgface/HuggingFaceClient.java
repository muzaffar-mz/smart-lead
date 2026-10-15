package com.muzaffar.smartlead.service.hgface;


import com.muzaffar.smartlead.service.hgface.model.request.CompletionRequest;
import com.muzaffar.smartlead.service.hgface.model.response.CompletionResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("${hugging-face.completions}")
public interface HuggingFaceClient {

    @PostExchange
    CompletionResponse completion(@RequestBody CompletionRequest request);
}
