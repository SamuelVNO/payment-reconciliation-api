package io.github.samuelvno.paymentreconciliation.notification;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import io.github.samuelvno.paymentreconciliation.TestcontainersConfiguration;
import io.github.samuelvno.paymentreconciliation.order.Order;
import io.github.samuelvno.paymentreconciliation.order.OrderRepository;
import io.github.samuelvno.paymentreconciliation.payout.PayoutImportService;
import io.github.samuelvno.paymentreconciliation.reconciliation.ReconciliationService;
import io.github.samuelvno.paymentreconciliation.reconciliation.TriggerSource;
import io.github.samuelvno.paymentreconciliation.shared.config.KafkaConfig;
import io.github.samuelvno.paymentreconciliation.shared.outbox.OutboxRelay;
import io.github.samuelvno.paymentreconciliation.shared.outbox.OutboxRepository;
import io.github.samuelvno.paymentreconciliation.store.Store;
import io.github.samuelvno.paymentreconciliation.store.StoreRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DiscrepancyEventsIT {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PayoutImportService payoutImportService;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private StoreAlertRepository alertRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Value("${app.kafka.topics.discrepancies}")
    private String topic;

    private UUID storeId;

    @BeforeEach
    void setUp() {
        Store store = storeRepository.save(new Store("Loja Kafka", String.valueOf(System.nanoTime()).substring(0, 14),
                new BigDecimal("0.0299")));
        storeId = store.getId();
        LocalDate orderDate = LocalDate.now().minusDays(10);
        orderRepository.save(new Order(store, "PED-1", new BigDecimal("89.90"), orderDate));
        orderRepository.save(new Order(store, "PED-2", new BigDecimal("75.00"), orderDate));
        String csv = """
                payout_id,order_id,gross_amount,fee_amount,net_amount,paid_at
                PAY-1,PED-1,89.90,3.50,86.40,%s
                """.formatted(orderDate.plusDays(1));
        payoutImportService.importCsv(storeId, "kafka-" + storeId, "k.csv",
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void discrepanciesFlowFromOutboxThroughKafkaToAlertsExactlyOnce() {
        reconciliationService.reconcile(storeId, TriggerSource.MANUAL);
        // FEE_OVERCHARGED (PED-1) + MISSING_PAYOUT (PED-2): 2 eventos esperando no outbox
        assertThat(outboxRepository.countByPublishedAtIsNull()).isGreaterThanOrEqualTo(2);

        outboxRelay.publishPending();

        assertThat(outboxRepository.countByPublishedAtIsNull()).isZero();
        await().atMost(Duration.ofSeconds(20)).until(() -> alertRepository.countByStoreId(storeId) == 2);

        // Reentrega do mesmo evento (o que acontece num retry do relay): nenhum alerta novo.
        StoreAlert alert = alertRepository.findByStoreId(storeId, Pageable.ofSize(1))
                .getContent().getFirst();
        ProducerRecord<String, String> duplicate = new ProducerRecord<>(topic, storeId.toString(), """
                {"discrepancyId":"%s","storeId":"%s","type":"%s","difference":%s}
                """.formatted(alert.getDiscrepancyId(), storeId, alert.getType(), alert.getDifference()));
        duplicate.headers().add(OutboxRelay.EVENT_ID_HEADER, alert.getEventId().toString().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(duplicate).join();

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .until(() -> alertRepository.countByStoreId(storeId) == 2);
    }

    @Test
    void poisonMessageGoesToDeadLetterTopic() {
        String eventId = UUID.randomUUID().toString();
        ProducerRecord<String, String> poison = new ProducerRecord<>(topic, storeId.toString(), "isto não é json");
        poison.headers().add(OutboxRelay.EVENT_ID_HEADER, eventId.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(poison).join();

        try (Consumer<String, String> consumer = consumerFactory.createConsumer("dlt-test-" + eventId, "dlt")) {
            consumer.subscribe(List.of(topic + KafkaConfig.DLT_SUFFIX));
            AtomicReference<ConsumerRecord<String, String>> deadLetter = new AtomicReference<>();
            await().atMost(Duration.ofSeconds(20)).until(() -> {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if ("isto não é json".equals(record.value())) {
                        deadLetter.set(record);
                    }
                }
                return deadLetter.get() != null;
            });
            // exception-fqcn traz o wrapper (ListenerExecutionFailedException); a causa real vem em cause-fqcn
            assertThat(header(deadLetter.get(), "kafka_dlt-exception-cause-fqcn")).endsWith("InvalidEventException");
            assertThat(header(deadLetter.get(), "kafka_dlt-original-topic")).isEqualTo(topic);
        }
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }
}
