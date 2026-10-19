package com.muzaffar.smartlead.config;


import com.muzaffar.smartlead.exceptions.HFException;
import com.muzaffar.smartlead.exceptions.RateLimitInfo;
import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.service.hgface.model.response.HFErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class HuggingFaceErrorHandler implements RestClient.ResponseSpec.ErrorHandler {

    private final JsonMapper jsonMapper;


    @Override
    public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        byte[] body = response.getBody().readAllBytes();
        String bodyString = new String(body, StandardCharsets.UTF_8);

        log.error("HuggingFace error. {} {} -> status={}, body={}",
                request.getMethod(), request.getURI(), status.value(), bodyString);

        if (!bodyString.isBlank()) {
            try {
                HFErrorResponse error = jsonMapper.readValue(bodyString, HFErrorResponse.class);
                if (error != null) {
                    throw new HFException(ErrorConstant.HUGGING_FACE_EXCEPTION, error, status.value(),
                            RateLimitInfo.from(response.getHeaders()));
                }
            } catch (JacksonException e) {
                log.error("HuggingFace error body could not be parsed: {}", e.getMessage(), e);
            }
        }

        throw new RestClientResponseException(
                "HuggingFace call failed with status " + status.value(),
                status, response.getStatusText(), response.getHeaders(),
                body, StandardCharsets.UTF_8);
    }
}
