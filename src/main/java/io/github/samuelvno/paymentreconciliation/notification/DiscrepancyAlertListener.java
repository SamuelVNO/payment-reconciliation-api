package io.github.samuelvno.paymentreconciliation.notification;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import io.github.samuelvno.paymentreconciliation.reconciliation.DiscrepancyDetectedEvent;
import io.github.samuelvno.paymentreconciliation.shared.messaging.InvalidEventException;
import io.github.samuelvno.paymentreconciliation.shared.outbox.OutboxRelay;

@Component
class DiscrepancyAlertListener {

    private final StoreAlertService alertService;
    private final ObjectMapper objectMapper;

    DiscrepancyAlertListener(StoreAlertService alertService, ObjectMapper objectMapper) {
        this.alertService = alertService;
        this.objectMapper = objectMapper;
    }

    // Se o método lançar exceção, o offset não é confirmado: o DefaultErrorHandler tenta de novo
    // e, esgotadas as tentativas, publica no tópico -dlt.
    @KafkaListener(topics = "${app.kafka.topics.discrepancies}", groupId = "store-alerts")
    void onDiscrepancyDetected(ConsumerRecord<String, String> record) {
        Header eventIdHeader = record.headers().lastHeader(OutboxRelay.EVENT_ID_HEADER);
        if (eventIdHeader == null) {
            throw new InvalidEventException("Missing eventId header", null);
        }
        try {
            UUID eventId = UUID.fromString(new String(eventIdHeader.value(), StandardCharsets.UTF_8));
            DiscrepancyDetectedEvent event = objectMapper.readValue(record.value(), DiscrepancyDetectedEvent.class);
            alertService.register(eventId, event);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new InvalidEventException("Unreadable DiscrepancyDetected event", ex);
        }
    }
}
