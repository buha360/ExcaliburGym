package com.wardanger.excalibur.sauna.shared;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Budapest"));
    }
}
