package com.muzaffar.smartlead.config;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class AiConfig {

    @Bean
    ChatClient leadChatClient(
            ChatClient.Builder builder,
            @Value("classpath:hgface/lead-classification-schema.json") Resource schema,
            @Value("classpath:hgface/lead-system-prompt.txt") Resource systemPromptResource
                              ) throws IOException {
        var jsonSchema = schema.getContentAsString(StandardCharsets.UTF_8);
        var responseFormat = OpenAiChatModel.ResponseFormat.builder()
                .type(OpenAiChatModel.ResponseFormat.Type.JSON_SCHEMA)
                .jsonSchema(jsonSchema)
                .strict(true)
                .build();

        var options = OpenAiChatOptions.builder()
                .responseFormat(responseFormat);

        return builder.defaultSystem(systemPromptResource).defaultOptions(options).build();
    }

}
