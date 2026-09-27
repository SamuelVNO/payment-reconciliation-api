package io.github.samuelvno.paymentreconciliation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApplicationIT {

    @Test
    void contextLoadsAndMigrationsRun() {
        // Sobe o contexto inteiro contra um Postgres real: Flyway migra e o Hibernate valida o schema.
    }
}
