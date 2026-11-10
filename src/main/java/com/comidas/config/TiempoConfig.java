package com.comidas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj de la aplicación. Los services piden la hora a este bean (nunca a {@code Instant.now()})
 * para que los tests puedan fijarla.
 */
@Configuration
public class TiempoConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
