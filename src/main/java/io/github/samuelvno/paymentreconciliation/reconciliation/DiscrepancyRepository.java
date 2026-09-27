package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.samuelvno.paymentreconciliation.payout.Payout;

public interface DiscrepancyRepository extends JpaRepository<Discrepancy, UUID> {

    Optional<Discrepancy> findByIdAndStoreId(UUID id, UUID storeId);

    // Uma query com LEFT JOIN no lugar de 1 + N (uma por pedido e outra por repasse).
    // countQuery explícita: o count derivado automaticamente levaria os JOINs sem necessidade.
    @Query(value = """
            select new io.github.samuelvno.paymentreconciliation.reconciliation.DiscrepancyView(
                d.id, d.type, d.status, o.externalId, p.externalId, d.expectedAmount, d.actualAmount,
                d.difference, d.description, d.resolutionNote, d.createdAt, d.resolvedAt)
            from Discrepancy d
            left join d.order o
            left join d.payout p
            where d.store.id = :storeId
              and (:type is null or d.type = :type)
              and (:status is null or d.status = :status)
            """,
            countQuery = """
            select count(d) from Discrepancy d
            where d.store.id = :storeId
              and (:type is null or d.type = :type)
              and (:status is null or d.status = :status)
            """)
    Page<DiscrepancyView> findViews(@Param("storeId") UUID storeId,
                                    @Param("type") DiscrepancyType type,
                                    @Param("status") DiscrepancyStatus status,
                                    Pageable pageable);

    // Repasses sem pedido correspondente que ainda não viraram divergência.
    @Query("""
            select p from Payout p
            where p.store.id = :storeId
              and not exists (select 1 from Order o
                              where o.store.id = :storeId and o.externalId = p.orderExternalId)
              and not exists (select 1 from Discrepancy d
                              where d.payout = p and d.type = ORPHAN_PAYOUT)
            """)
    List<Payout> findUnflaggedOrphanPayouts(@Param("storeId") UUID storeId);

    // UPDATE em massa: não passa pelo contexto de persistência nem pelo @Version.
    @Modifying
    @Query("""
            update Discrepancy d
            set d.status = RESOLVED, d.resolutionNote = :note, d.resolvedAt = :now, d.version = d.version + 1
            where d.payout.id in :payoutIds and d.type = ORPHAN_PAYOUT and d.status = OPEN
            """)
    int resolveOrphans(@Param("payoutIds") Collection<UUID> payoutIds,
                       @Param("note") String note,
                       @Param("now") Instant now);

    @Query("""
            select new io.github.samuelvno.paymentreconciliation.reconciliation.ReconciliationReport$TypeSummary(
                d.type, count(d), sum(d.difference))
            from Discrepancy d
            where d.store.id = :storeId and d.createdAt >= :from and d.createdAt < :to
            group by d.type
            order by d.type
            """)
    List<ReconciliationReport.TypeSummary> summarizeByType(@Param("storeId") UUID storeId,
                                                           @Param("from") Instant from,
                                                           @Param("to") Instant to);

    long countByStoreIdAndStatus(UUID storeId, DiscrepancyStatus status);
}
