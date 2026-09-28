package io.github.samuelvno.paymentreconciliation.notification;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.samuelvno.paymentreconciliation.reconciliation.DiscrepancyDetectedEvent;
import io.github.samuelvno.paymentreconciliation.reconciliation.DiscrepancyType;

@Service
public class StoreAlertService {

    private static final Logger log = LoggerFactory.getLogger(StoreAlertService.class);
    private static final Locale PT_BR = Locale.of("pt", "BR");

    // O alerta é lido por gente da loja (painel, e-mail): texto em português, sem código técnico
    private static final Map<DiscrepancyType, String> LABELS = Map.of(
            DiscrepancyType.MISSING_PAYOUT, "Repasse faltando",
            DiscrepancyType.AMOUNT_MISMATCH, "Valor divergente",
            DiscrepancyType.FEE_OVERCHARGED, "Taxa acima da contratada",
            DiscrepancyType.DUPLICATE_PAYOUT, "Pedido pago em dobro",
            DiscrepancyType.ORPHAN_PAYOUT, "Repasse sem pedido");

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
        String message = "%s: %s, diferença de %s".formatted(LABELS.get(event.type()), reference(event),
                NumberFormat.getCurrencyInstance(PT_BR).format(event.difference().abs()));
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
