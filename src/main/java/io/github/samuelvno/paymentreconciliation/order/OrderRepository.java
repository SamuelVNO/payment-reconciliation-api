package io.github.samuelvno.paymentreconciliation.order;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndStoreId(UUID id, UUID storeId);

    Page<Order> findByStoreId(UUID storeId, Pageable pageable);

    Page<Order> findByStoreIdAndReconciliationStatus(UUID storeId, ReconciliationStatus status, Pageable pageable);

    boolean existsByStoreIdAndExternalId(UUID storeId, String externalId);

    // Paginação por cursor (keyset): "id > último id visto" usa o índice e custa o mesmo na
    // página 1 ou na 10.000. OFFSET faria o banco ler e descartar todas as linhas anteriores,
    // e ainda pularia pedidos, porque o motor muda o status deles no meio da varredura.
    @Query("""
            select o from Order o
            where o.store.id = :storeId and o.reconciliationStatus = PENDING and o.id > :afterId
            order by o.id
            """)
    List<Order> findPendingBatch(@Param("storeId") UUID storeId, @Param("afterId") UUID afterId, Limit limit);

    @Query("""
            select new io.github.samuelvno.paymentreconciliation.order.OrderStatusCount(o.reconciliationStatus, count(o))
            from Order o
            where o.store.id = :storeId and o.orderDate between :from and :to
            group by o.reconciliationStatus
            """)
    List<OrderStatusCount> countByStatus(@Param("storeId") UUID storeId,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);
}
