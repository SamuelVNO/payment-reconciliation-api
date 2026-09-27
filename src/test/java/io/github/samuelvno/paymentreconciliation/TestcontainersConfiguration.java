package io.github.samuelvno.paymentreconciliation;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Sobe Postgres e Kafka reais em Docker para os testes. @ServiceConnection injeta
 * url/usuário/senha/bootstrap-servers no Spring, sem @DynamicPropertySource.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }

    // Imagem "native" (GraalVM): sobe em ~1s, ótima para teste. Em produção usamos apache/kafka.
    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer("apache/kafka-native:3.9.1");
    }
}
