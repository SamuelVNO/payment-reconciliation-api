package io.github.samuelvno.paymentreconciliation.shared.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    // FOR UPDATE SKIP LOCKED: cada instância da API trava um lote diferente e pula as linhas
    // que outra instância já travou. Vários relays em paralelo, nenhum evento publicado duas vezes
    // por concorrência (e sem precisar de ShedLock).
    @Query(value = """
            select * from outbox_events
            where published_at is null
            order by created_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch(@Param("limit") int limit);

    long countByPublishedAtIsNull();
}
