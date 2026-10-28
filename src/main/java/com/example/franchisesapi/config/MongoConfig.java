package com.example.franchisesapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Activa la auditoría de MongoDB (@CreatedDate / @LastModifiedDate) y expone un Clock
 * común, de modo que todas las fechas de la aplicación salen del mismo reloj y los tests
 * pueden fijarlo.
 */
@Configuration(proxyBeanMethods = false)
@EnableMongoAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class MongoConfig {

    /**
     * MongoDB guarda las fechas con precisión de milisegundos: el reloj avanza de milisegundo
     * en milisegundo para que una fecha recién asignada sea igual a la que se lee después.
     */
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofMillis(1));
    }

    @Bean
    DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(Instant.now(clock));
    }

}
