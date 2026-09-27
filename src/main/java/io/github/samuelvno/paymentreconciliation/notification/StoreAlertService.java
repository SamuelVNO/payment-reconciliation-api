package io.github.samuelvno.paymentreconciliation.notification;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.samuelvno.paymentreconciliation.reconciliation.DiscrepancyDetectedEvent;

@Service
public class StoreAlertService {

    private static final Logger log = LoggerFactory.getLogger(StoreAlertService.class);
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final StoreAlertRepository repository;

    public StoreAlertService(StoreAlertRepository repository) {
        this.repository = repository;
    }

    /**
     * Consumidor idempotente: o Kafka entrega "pelo menos uma vez", então o mesmo evento pode
     * chegar de novo (rebalance, retry do relay). O event_id único garante um alerta por evento.
     */
    @Transactional
    public boolean register(UUID eventId, DiscrepancyDetectedEvent event) {
        if (repository.existsByEventId(eventId)) {
            log.info("Event {} already processed, skipping", eventId);
            return false;
        }
        String message = "%s: %s (diferença de %s)".formatted(event.type(), reference(event),
                NumberFormat.getCurrencyInstance(PT_BR).format(event.difference()));
        repository.save(new StoreAlert(event.storeId(), eventId, event.discrepancyId(), event.type().name(),
                event.difference(), message));
        log.info("Alert for store {}: {}", event.storeId(), message);
        return true;
    }

    @Transactional(readOnly = true)
    public Page<StoreAlertResponse> findByStore(UUID storeId, Pageable pageable) {
        return repository.findByStoreId(storeId, pageable).map(StoreAlertResponse::from);
    }

    private static String reference(DiscrepancyDetectedEvent event) {
        return event.orderExternalId() != null
                ? "pedido " + event.orderExternalId()
                : "repasse " + event.payoutExternalId();
    }
}
