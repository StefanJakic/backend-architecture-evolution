package dev.stefanjakic.reservation.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ReservationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
