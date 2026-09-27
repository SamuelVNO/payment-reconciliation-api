package io.github.samuelvno.paymentreconciliation.shared.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "Hoje" vem de um Clock injetado, nunca de LocalDate.now() solto no código:
 * nos testes trocamos por um Clock fixo e as regras de prazo ficam determinísticas.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    @Bean
    Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
