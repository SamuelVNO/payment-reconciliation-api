package io.github.samuelvno.paymentreconciliation.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndStoreId(UUID id, UUID storeId);

    Page<Order> findByStoreId(UUID storeId, Pageable pageable);

    Page<Order> findByStoreIdAndReconciliationStatus(UUID storeId, ReconciliationStatus status, Pageable pageable);

    boolean existsByStoreIdAndExternalId(UUID storeId, String externalId);
}
