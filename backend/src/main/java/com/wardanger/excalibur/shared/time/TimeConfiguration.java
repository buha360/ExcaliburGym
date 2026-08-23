package com.wardanger.excalibur.shared.time;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {

    public static final ZoneId APPLICATION_ZONE = ZoneId.of("Europe/Budapest");

    @Bean
    Clock applicationClock() {
        return Clock.system(APPLICATION_ZONE);
    }
}
