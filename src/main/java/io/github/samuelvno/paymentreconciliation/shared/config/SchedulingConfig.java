package io.github.samuelvno.paymentreconciliation.shared.config;

import javax.sql.DataSource;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga os jobs agendados. Com 2+ instâncias da API, o @Scheduled rodaria em todas ao mesmo tempo:
 * o ShedLock grava um lock na tabela "shedlock" e só uma instância executa cada job.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {

    @Bean
    LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                // relógio do banco, não da máquina: instâncias com relógios diferentes não quebram o lock
                .usingDbTime()
                .build());
    }
}
