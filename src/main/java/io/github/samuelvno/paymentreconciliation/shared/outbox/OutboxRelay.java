package io.github.samuelvno.paymentreconciliation.shared.outbox;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lê eventos pendentes do outbox e publica no Kafka.
 * Garantia: at-least-once. Se a app cair depois do send e antes do commit, o evento sai
 * de novo no próximo ciclo; por isso o consumidor precisa ser idempotente.
 */
@Component
public class OutboxRelay {

    public static final String EVENT_ID_HEADER = "eventId";
    public static final String EVENT_TYPE_HEADER = "eventType";

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties properties;
    private final Clock clock;
    private final Counter publishedCounter;
    private final Counter failedCounter;

    public OutboxRelay(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate,
                       OutboxProperties properties, Clock clock, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.clock = clock;
        this.publishedCounter = Counter.builder("outbox.published").register(meterRegistry);
        this.failedCounter = Counter.builder("outbox.publish.failures").register(meterRegistry);
    }

    @Transactional
    public int publishPending() {
        List<OutboxEvent> events = repository.lockNextBatch(properties.batchSize());
        int published = 0;
        for (OutboxEvent event : events) {
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(event.getTopic(), event.getMessageKey(), event.getPayload());
            record.headers().add(EVENT_ID_HEADER, event.getId().toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add(EVENT_TYPE_HEADER, event.getEventType().getBytes(StandardCharsets.UTF_8));
            try {
                // Espera o ack do broker: só marca como publicado o que o Kafka confirmou.
                kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
                event.markPublished(Instant.now(clock));
                publishedCounter.increment();
                published++;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                event.markFailed("interrupted");
                break;
            } catch (ExecutionException | TimeoutException ex) {
                // Broker fora do ar: para o lote e tenta de novo no próximo ciclo (ordem preservada).
                event.markFailed(ex.getMessage());
                failedCounter.increment();
                log.warn("Could not publish outbox event {} (attempt {}): {}", event.getId(), event.getAttempts(),
                        ex.getMessage());
                break;
            }
        }
        if (published > 0) {
            log.info("Published {} outbox event(s)", published);
        }
        return published;
    }
}
