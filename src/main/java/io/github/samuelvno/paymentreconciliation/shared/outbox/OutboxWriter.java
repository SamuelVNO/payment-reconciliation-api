package io.github.samuelvno.paymentreconciliation.shared.outbox;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxWriter {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * MANDATORY: lança exceção se não houver transação aberta. O evento só faz sentido
     * se for gravado junto (commit ou rollback) com a mudança de negócio que o gerou.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String aggregateType, UUID aggregateId, String eventType, String topic, String key,
                        Object payload) {
        try {
            repository.save(new OutboxEvent(aggregateType, aggregateId, eventType, topic, key,
                    objectMapper.writeValueAsString(payload)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize event " + eventType, ex);
        }
    }
}
