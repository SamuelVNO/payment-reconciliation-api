package io.github.samuelvno.paymentreconciliation.shared.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import io.github.samuelvno.paymentreconciliation.shared.messaging.InvalidEventException;

@Configuration
public class KafkaConfig {

    public static final String DLT_SUFFIX = "-dlt";

    private static final int PARTITIONS = 3;

    @Bean
    NewTopic discrepanciesTopic(@Value("${app.kafka.topics.discrepancies}") String topic) {
        return TopicBuilder.name(topic).partitions(PARTITIONS).replicas(1).build();
    }

    // A mensagem vai para a MESMA partição no tópico -dlt: ele precisa ter as mesmas partições.
    @Bean
    NewTopic discrepanciesDeadLetterTopic(@Value("${app.kafka.topics.discrepancies}") String topic) {
        return TopicBuilder.name(topic + DLT_SUFFIX).partitions(PARTITIONS).replicas(1).build();
    }

    /**
     * Mensagem que falha: tenta 3 vezes com 1s de intervalo e depois vai para o tópico -dlt,
     * em vez de travar a partição para sempre (poison message). Payload inválido não adianta
     * repetir: vai direto para a DLT.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // Destino explícito: o sufixo padrão já mudou entre versões do spring-kafka (.DLT -> -dlt)
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + DLT_SUFFIX, record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 2));
        handler.addNotRetryableExceptions(InvalidEventException.class);
        return handler;
    }
}
