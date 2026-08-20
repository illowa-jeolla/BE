package com.example.travel.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableAsync
public class TimeConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
