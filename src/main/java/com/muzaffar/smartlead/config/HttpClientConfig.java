package com.muzaffar.smartlead.config;

import com.muzaffar.smartlead.service.hgface.client.HuggingFaceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ImportHttpServices(group = "huggingFace", types = HuggingFaceClient.class)
public class HttpClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer groupConfigurer(
            @Value("${hugging-face.token}") String token,
            @Value("${hugging-face.url}") String url,
            JsonMapper jsonMapper
    ) {

        var errorHandler = new HuggingFaceErrorHandler(jsonMapper);

        return groups -> {
            groups
                    .filterByName("huggingFace")
                    .forEachClient((g, builder) ->
                            builder
                                    .baseUrl(url)
                                    .defaultHeaders(h -> h.setBearerAuth(token))
                                    .defaultStatusHandler(HttpStatusCode::isError, errorHandler));
        };
    }

}
