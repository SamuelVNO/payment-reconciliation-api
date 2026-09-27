package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.samuelvno.paymentreconciliation.order.Order;
import io.github.samuelvno.paymentreconciliation.order.OrderRepository;
import io.github.samuelvno.paymentreconciliation.order.OrderStatusCount;
import io.github.samuelvno.paymentreconciliation.order.ReconciliationStatus;
import io.github.samuelvno.paymentreconciliation.payout.Payout;
import io.github.samuelvno.paymentreconciliation.payout.PayoutRepository;
import io.github.samuelvno.paymentreconciliation.shared.config.TimeConfig;
import io.github.samuelvno.paymentreconciliation.shared.error.InvalidRequestException;
import io.github.samuelvno.paymentreconciliation.shared.error.ResourceNotFoundException;
import io.github.samuelvno.paymentreconciliation.shared.outbox.OutboxWriter;
import io.github.samuelvno.paymentreconciliation.store.Store;
import io.github.samuelvno.paymentreconciliation.store.StoreService;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    // Menor UUID possível: ponto de partida do cursor da paginação keyset.
    private static final UUID FIRST_ID = new UUID(0L, 0L);

    private final StoreService storeService;
    private final OrderRepository orderRepository;
    private final PayoutRepository payoutRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final ReconciliationRunRepository runRepository;
    private final ReconciliationProperties properties;
    private final ReconciliationRules rules;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final OutboxWriter outbox;
    private final String discrepanciesTopic;

    public ReconciliationService(StoreService storeService, OrderRepository orderRepository,
                                 PayoutRepository payoutRepository, DiscrepancyRepository discrepancyRepository,
                                 ReconciliationRunRepository runRepository, ReconciliationProperties properties,
                                 PlatformTransactionManager transactionManager, Clock clock, OutboxWriter outbox,
                                 @Value("${app.kafka.topics.discrepancies}") String discrepanciesTopic) {
        this.storeService = storeService;
        this.orderRepository = orderRepository;
        this.payoutRepository = payoutRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.runRepository = runRepository;
        this.properties = properties;
        this.rules = new ReconciliationRules(properties.settlementDays(), properties.feeTolerance());
        this.tx = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.outbox = outbox;
        this.discrepanciesTopic = discrepanciesTopic;
    }

    /**
     * Sem @Transactional aqui de propósito: cada lote roda na sua própria transação
     * (TransactionTemplate). Uma loja com 1 milhão de pedidos não abre uma transação gigante,
     * não acumula 1 milhão de entidades no contexto de persistência e, se cair no meio,
     * os lotes já concluídos ficam salvos e o próximo run continua de onde parou.
     */
    public ReconciliationRunResponse reconcile(UUID storeId, TriggerSource trigger) {
        Store store = storeService.getStore(storeId);
        LocalDate today = LocalDate.now(clock);
        ReconciliationRun run = tx.execute(status -> runRepository.save(new ReconciliationRun(store, trigger, today)));

        RunTotals totals = RunTotals.EMPTY;
        UUID cursor = FIRST_ID;
        while (true) {
            UUID after = cursor;
            BatchResult batch = tx.execute(status -> processBatch(store, run, after, today));
            totals = totals.plus(batch.totals());
            if (batch.lastOrderId() == null || batch.totals().ordersChecked() < properties.batchSize()) {
                break;
            }
            cursor = batch.lastOrderId();
        }

        int orphans = tx.execute(status -> flagOrphanPayouts(store, run));
        RunTotals finalTotals = totals.plus(new RunTotals(0, 0, 0, 0, orphans));

        ReconciliationRunResponse response = tx.execute(status -> {
            ReconciliationRun managed = runRepository.findById(run.getId()).orElseThrow();
            managed.finish(finalTotals, Instant.now(clock));
            return ReconciliationRunResponse.from(managed);
        });
        log.info("Reconciliation run {} for store {} finished: checked={} reconciled={} divergent={} pending={} discrepancies={}",
                run.getId(), storeId, finalTotals.ordersChecked(), finalTotals.reconciled(),
                finalTotals.divergent(), finalTotals.stillPending(), finalTotals.discrepancies());
        return response;
    }

    private record BatchResult(RunTotals totals, UUID lastOrderId) {
    }

    private BatchResult processBatch(Store store, ReconciliationRun run, UUID afterId, LocalDate today) {
        List<Order> orders = orderRepository.findPendingBatch(store.getId(), afterId, Limit.of(properties.batchSize()));
        if (orders.isEmpty()) {
            return new BatchResult(RunTotals.EMPTY, null);
        }

        // 1 SELECT para os repasses do lote inteiro. Buscar por pedido dentro do for seria N+1.
        Map<String, List<Payout>> payoutsByOrder = payoutRepository
                .findByStoreIdAndOrderExternalIdIn(store.getId(), orders.stream().map(Order::getExternalId).toList())
                .stream()
                .collect(Collectors.groupingBy(Payout::getOrderExternalId));

        List<Discrepancy> discrepancies = new ArrayList<>();
        List<UUID> matchedPayoutIds = new ArrayList<>();
        int reconciled = 0;
        int divergent = 0;
        int pending = 0;
        for (Order order : orders) {
            List<Payout> payouts = payoutsByOrder.getOrDefault(order.getExternalId(), List.of());
            ReconciliationRules.Evaluation evaluation = rules.evaluate(store, order, payouts, today);
            // Sem save(): o pedido está "managed" e o dirty checking gera o UPDATE no commit do lote.
            order.markAs(evaluation.status());
            switch (evaluation.status()) {
                case RECONCILED -> reconciled++;
                case DIVERGENT -> divergent++;
                case PENDING -> pending++;
            }
            evaluation.findings().forEach(finding -> discrepancies.add(Discrepancy.of(store, run, order, finding)));
            payouts.forEach(payout -> matchedPayoutIds.add(payout.getId()));
        }
        discrepancyRepository.saveAll(discrepancies);
        enqueueEvents(store, discrepancies);

        // Repasse que chegou antes do pedido ser cadastrado: era órfão, agora casou.
        if (!matchedPayoutIds.isEmpty()) {
            discrepancyRepository.resolveOrphans(matchedPayoutIds, "Matched with an order registered later",
                    Instant.now(clock));
        }
        RunTotals totals = new RunTotals(orders.size(), reconciled, divergent, pending, discrepancies.size());
        return new BatchResult(totals, orders.getLast().getId());
    }

    private int flagOrphanPayouts(Store store, ReconciliationRun run) {
        List<Payout> orphans = discrepancyRepository.findUnflaggedOrphanPayouts(store.getId());
        List<Discrepancy> discrepancies = discrepancyRepository.saveAll(
                orphans.stream().map(payout -> Discrepancy.orphan(store, run, payout)).toList());
        enqueueEvents(store, discrepancies);
        return orphans.size();
    }

    // Mesma transação do lote: ou grava a divergência E o evento, ou nenhum dos dois.
    private void enqueueEvents(Store store, List<Discrepancy> discrepancies) {
        for (Discrepancy discrepancy : discrepancies) {
            outbox.enqueue("Discrepancy", discrepancy.getId(), DiscrepancyDetectedEvent.EVENT_TYPE,
                    discrepanciesTopic, store.getId().toString(),
                    DiscrepancyDetectedEvent.from(store.getId(), discrepancy));
        }
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationRunResponse> findRuns(UUID storeId, Pageable pageable) {
        return runRepository.findByStoreId(storeId, pageable).map(ReconciliationRunResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<DiscrepancyView> findDiscrepancies(UUID storeId, DiscrepancyType type, DiscrepancyStatus status,
                                                   Pageable pageable) {
        return discrepancyRepository.findViews(storeId, type, status, pageable);
    }

    @Transactional
    public void resolve(UUID storeId, UUID discrepancyId, String note) {
        Discrepancy discrepancy = discrepancyRepository.findByIdAndStoreId(discrepancyId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Discrepancy", discrepancyId));
        discrepancy.resolve(note, Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public ReconciliationReport report(UUID storeId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidRequestException("'from' must be before or equal to 'to'");
        }
        storeService.getStore(storeId);
        Map<ReconciliationStatus, Long> ordersByStatus = new EnumMap<>(ReconciliationStatus.class);
        for (ReconciliationStatus status : ReconciliationStatus.values()) {
            ordersByStatus.put(status, 0L);
        }
        for (OrderStatusCount count : orderRepository.countByStatus(storeId, from, to)) {
            ordersByStatus.put(count.status(), count.total());
        }
        Instant start = from.atStartOfDay(TimeConfig.BUSINESS_ZONE).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(TimeConfig.BUSINESS_ZONE).toInstant();
        return new ReconciliationReport(from, to, ordersByStatus,
                discrepancyRepository.summarizeByType(storeId, start, end),
                discrepancyRepository.countByStoreIdAndStatus(storeId, DiscrepancyStatus.OPEN));
    }
}
