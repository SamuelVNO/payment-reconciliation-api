package io.github.samuelvno.paymentreconciliation.shared.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OutboxMetrics {

    // Fila do outbox crescendo = Kafka fora do ar ou relay travado. É o primeiro alerta a configurar.
    @Bean
    Gauge outboxPendingGauge(MeterRegistry registry, OutboxRepository repository) {
        return Gauge.builder("outbox.pending", repository, OutboxRepository::countByPublishedAtIsNull)
                .description("Outbox events waiting to be published")
                .register(registry);
    }
}
