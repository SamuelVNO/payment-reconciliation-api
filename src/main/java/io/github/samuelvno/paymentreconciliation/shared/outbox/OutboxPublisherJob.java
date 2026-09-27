package io.github.samuelvno.paymentreconciliation.shared.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fica separado do OutboxRelay de propósito: chamar um método @Transactional da própria
 * classe (this.metodo()) não passa pelo proxy do Spring e a transação não abre.
 */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
class OutboxPublisherJob {

    private final OutboxRelay relay;

    OutboxPublisherJob(OutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval}")
    void publish() {
        relay.publishPending();
    }
}
