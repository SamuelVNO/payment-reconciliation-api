package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.util.UUID;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.github.samuelvno.paymentreconciliation.store.StoreRepository;

/** Conciliação noturna de todas as lojas (03:00, horário de Brasília). */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);

    private final ReconciliationService reconciliationService;
    private final StoreRepository storeRepository;

    public ReconciliationJob(ReconciliationService reconciliationService, StoreRepository storeRepository) {
        this.reconciliationService = reconciliationService;
        this.storeRepository = storeRepository;
    }

    @Scheduled(cron = "${app.reconciliation.cron}", zone = "America/Sao_Paulo")
    @SchedulerLock(name = "reconciliation-nightly", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
    public void run() {
        for (UUID storeId : storeRepository.findAllIds()) {
            try {
                reconciliationService.reconcile(storeId, TriggerSource.SCHEDULED);
            } catch (RuntimeException ex) {
                // Falha de uma loja não pode impedir as outras de conciliar.
                log.error("Scheduled reconciliation failed for store {}", storeId, ex);
            }
        }
    }
}
