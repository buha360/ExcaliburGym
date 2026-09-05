package com.wardanger.excalibur.statistics.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "excalibur.reporting.mode", havingValue = "remote")
public class ReportingClientConfiguration {

    @Bean
    RestClient reportingRestClient(
            @Value("${excalibur.reporting.base-url}") String baseUrl,
            @Value("${excalibur.reporting.internal-api-key}") String internalApiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .build();
    }
}
