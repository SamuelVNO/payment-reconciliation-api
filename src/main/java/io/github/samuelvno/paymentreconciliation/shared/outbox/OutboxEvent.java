package io.github.samuelvno.paymentreconciliation.shared.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.samuelvno.paymentreconciliation.shared.persistence.BaseEntity;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent extends BaseEntity {

    @Column(nullable = false, length = 50, updatable = false)
    private String aggregateType;

    @Column(nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 80, updatable = false)
    private String eventType;

    @Column(nullable = false, length = 120, updatable = false)
    private String topic;

    // Chave da mensagem = partição no Kafka. Mesma loja -> mesma partição -> ordem preservada.
    @Column(nullable = false, length = 80, updatable = false)
    private String messageKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb", updatable = false)
    private String payload;

    private int attempts;

    @Column(length = 500)
    private String lastError;

    private Instant publishedAt;

    protected OutboxEvent() {
        // exigido pelo JPA
    }

    OutboxEvent(String aggregateType, UUID aggregateId, String eventType, String topic, String messageKey,
                String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
    }

    void markPublished(Instant when) {
        this.attempts++;
        this.publishedAt = when;
        this.lastError = null;
    }

    void markFailed(String error) {
        this.attempts++;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
    }

    public String getEventType() {
        return eventType;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getPayload() {
        return payload;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
