package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.samuelvno.paymentreconciliation.TestcontainersConfiguration;
import io.github.samuelvno.paymentreconciliation.order.Order;
import io.github.samuelvno.paymentreconciliation.order.OrderRepository;
import io.github.samuelvno.paymentreconciliation.payout.PayoutImportService;
import io.github.samuelvno.paymentreconciliation.store.Store;
import io.github.samuelvno.paymentreconciliation.store.StoreRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contando os SQL executados, a diferença entre navegar por entidades lazy (N+1)
 * e usar uma projeção com JOIN (quantidade fixa de queries).
 */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"
})
@Import(TestcontainersConfiguration.class)
class DiscrepancyQueryIT {

    private static final int ORDERS = 20;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PayoutImportService payoutImportService;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private DiscrepancyRepository discrepancyRepository;

    @Autowired
    private TransactionTemplate tx;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private UUID storeId;

    @BeforeEach
    void setUp() {
        Store store = storeRepository.save(new Store("Loja N+1", String.valueOf(System.nanoTime()).substring(0, 14),
                new BigDecimal("0.0299")));
        storeId = store.getId();
        StringBuilder csv = new StringBuilder("payout_id,order_id,gross_amount,fee_amount,net_amount,paid_at\n");
        for (int i = 1; i <= ORDERS; i++) {
            orderRepository.save(new Order(store, "PED-" + i, new BigDecimal("100.00"), LocalDate.now().minusDays(10)));
            // taxa de 5.00 em vez de 2.99: todo pedido vira FEE_OVERCHARGED
            csv.append("PAY-%d,PED-%d,100.00,5.00,95.00,%s%n".formatted(i, i, LocalDate.now().minusDays(9)));
        }
        payoutImportService.importCsv(storeId, "n1-" + storeId, "n1.csv", new ByteArrayInputStream(csv.toString().getBytes()));
        reconciliationService.reconcile(storeId, TriggerSource.MANUAL);
    }

    @Test
    void navigatingLazyAssociationsTriggersNPlusOne() {
        Statistics stats = statistics();

        tx.executeWithoutResult(status -> discrepancyRepository.findAll().stream()
                .filter(d -> d.getOrder() != null && d.getOrder().getStore().getId().equals(storeId))
                .forEach(d -> d.getOrder().getExternalId()));

        // 1 SELECT das divergências + 1 SELECT por pedido acessado
        assertThat(stats.getPrepareStatementCount()).isGreaterThanOrEqualTo(1 + ORDERS);
    }

    @Test
    void projectionLoadsPageWithFixedNumberOfQueries() {
        Statistics stats = statistics();

        var page = reconciliationService.findDiscrepancies(storeId, null, null, Pageable.ofSize(50));

        assertThat(page.getContent()).hasSize(ORDERS)
                .allSatisfy(view -> assertThat(view.orderExternalId()).startsWith("PED-"));
        // 1 SELECT com JOIN (+ count só quando a página vem cheia), independente de quantas linhas
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    private Statistics statistics() {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        return stats;
    }
}
