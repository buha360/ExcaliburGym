package com.wardanger.excalibur.solarium.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SolariumClientConfiguration {
    @Bean
    RestClient solariumRestClient(@Value("${excalibur.solarium.base-url}") String baseUrl, @Value("${excalibur.solarium.internal-api-key}") String apiKey) {
        return RestClient.builder().baseUrl(baseUrl).defaultHeader("X-Internal-Api-Key", apiKey).build();
    }
}