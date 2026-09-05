package com.wardanger.excalibur.sauna.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SaunaClientConfiguration {
    @Bean
    RestClient saunaRestClient(
            @Value("${excalibur.sauna.base-url}") String baseUrl,
            @Value("${excalibur.sauna.internal-api-key}") String internalApiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .build();
    }
}
