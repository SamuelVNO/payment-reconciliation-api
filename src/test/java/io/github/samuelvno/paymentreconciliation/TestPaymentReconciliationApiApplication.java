package io.github.samuelvno.paymentreconciliation;

import org.springframework.boot.SpringApplication;

/**
 * Roda a app localmente com os containers de teste: ./mvnw spring-boot:test-run
 */
public class TestPaymentReconciliationApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(PaymentReconciliationApiApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
